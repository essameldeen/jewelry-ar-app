import { RingColors, RING_STYLE_COLORS, RingStyle } from '../data/products';
import { HandLandmarks } from '../hooks/useHandTracking';
import { PoseLandmarks } from '../hooks/usePoseTracking';

export function drawOverlayImage(
  ctx: CanvasRenderingContext2D,
  img: HTMLImageElement,
  landmarks: HandLandmarks | null,
  poseLandmarks: PoseLandmarks | null,
  category: string,
  canvasWidth: number,
  canvasHeight: number
) {
  const aspect = img.naturalHeight / img.naturalWidth;

  if (category === 'necklace' || category === 'luxury') {
    if (poseLandmarks?.isDetected) {
      const noseY   = poseLandmarks.nose.y          * canvasHeight;
      const lsX     = poseLandmarks.leftShoulder.x  * canvasWidth;
      const rsX     = poseLandmarks.rightShoulder.x  * canvasWidth;
      const lsY     = poseLandmarks.leftShoulder.y  * canvasHeight;
      const rsY     = poseLandmarks.rightShoulder.y  * canvasHeight;

      const centerX      = (lsX + rsX) / 2;
      const shoulderWidth = Math.abs(rsX - lsX);
      const shoulderMidY  = (lsY + rsY) / 2;

      // Collarbone sits ~65 % of the way from nose down to shoulder midpoint
      const clavicleY = noseY + (shoulderMidY - noseY) * 0.65;

      // Width slightly wider than shoulders so chain fills the neck area naturally
      const drawW = shoulderWidth * 1.05;
      const drawH = aspect * drawW;

      // Anchor: top of image at the throat/clavicle line
      ctx.drawImage(img, centerX - drawW / 2, clavicleY, drawW, drawH);
    } else {
      // Fallback: fixed center position while waiting for detection
      const drawW = canvasWidth * 0.55;
      const drawH = aspect * drawW;
      ctx.drawImage(img, (canvasWidth - drawW) / 2, canvasHeight * 0.12, drawW, drawH);
    }
    return;
  }

  if (!landmarks?.isDetected) return;

  const wristX = landmarks.wrist.x * canvasWidth;
  const wristY = landmarks.wrist.y * canvasHeight;
  const midX = landmarks.middleFingerTip.x * canvasWidth;
  const midY = landmarks.middleFingerTip.y * canvasHeight;
  const handSize = Math.sqrt((midX - wristX) ** 2 + (midY - wristY) ** 2);
  const scale = Math.max(0.4, Math.min(1.8, handSize / 200));

  if (category === 'bracelet') {
    const bx = wristX;
    const by = wristY;
    const drawW = 90 * scale;
    const drawH = aspect * drawW;
    const baseX = landmarks.ringFingerBase.x * canvasWidth;
    const baseY = landmarks.ringFingerBase.y * canvasHeight;
    const angle = Math.atan2(baseY - by, baseX - bx) - Math.PI / 2;
    ctx.save();
    ctx.translate(bx, by);
    ctx.rotate(angle);
    ctx.drawImage(img, -drawW / 2, -drawH / 2, drawW, drawH);
    ctx.restore();
    return;
  }

  // ring / engagement / wedding / fashion
  const ringX = landmarks.ringFingerBase.x * canvasWidth;
  const ringY = landmarks.ringFingerBase.y * canvasHeight;
  const tipX = landmarks.ringFingerTip.x * canvasWidth;
  const tipY = landmarks.ringFingerTip.y * canvasHeight;
  const angle = Math.atan2(tipY - ringY, tipX - ringX) - Math.PI / 2;
  const drawW = 65 * scale;
  const drawH = aspect * drawW;

  ctx.save();
  ctx.translate(ringX, ringY);
  ctx.rotate(angle);
  ctx.drawImage(img, -drawW / 2, -drawH / 2, drawW, drawH);
  ctx.restore();
}

/**
 * Draws a realistic 3D-looking ring on a canvas at the detected finger position.
 */
export function drawRing(
  ctx: CanvasRenderingContext2D,
  landmarks: HandLandmarks,
  ringStyle: RingStyle,
  canvasWidth: number,
  canvasHeight: number
) {
  if (!landmarks.isDetected) return;

  const colors = RING_STYLE_COLORS[ringStyle];

  // Ring finger base position (landmark 13) - where a ring naturally sits
  const ringX = landmarks.ringFingerBase.x * canvasWidth;
  const ringY = landmarks.ringFingerBase.y * canvasHeight;

  // Calculate ring size based on hand distance (wrist to middle finger)
  const wristX = landmarks.wrist.x * canvasWidth;
  const wristY = landmarks.wrist.y * canvasHeight;
  const midX = landmarks.middleFingerTip.x * canvasWidth;
  const midY = landmarks.middleFingerTip.y * canvasHeight;
  const handSize = Math.sqrt((midX - wristX) ** 2 + (midY - wristY) ** 2);

  const scale = Math.max(0.4, Math.min(1.8, handSize / 200));
  const ringWidth = 38 * scale;
  const ringHeight = 50 * scale;
  const bandThickness = 14 * scale;

  ctx.save();
  ctx.translate(ringX, ringY);

  // Calculate finger angle for rotation
  const tipX = landmarks.ringFingerTip.x * canvasWidth;
  const tipY = landmarks.ringFingerTip.y * canvasHeight;
  const angle = Math.atan2(tipY - ringY, tipX - ringX) - Math.PI / 2;
  ctx.rotate(angle);

  // === Shadow ===
  ctx.shadowColor = 'rgba(0,0,0,0.4)';
  ctx.shadowBlur = 12 * scale;
  ctx.shadowOffsetX = 3 * scale;
  ctx.shadowOffsetY = 4 * scale;

  // === Outer ring (main band) ===
  ctx.beginPath();
  ctx.ellipse(0, 0, ringWidth, ringHeight, 0, 0, Math.PI * 2);
  const outerGrad = ctx.createRadialGradient(
    -ringWidth * 0.3,
    -ringHeight * 0.3,
    0,
    0,
    0,
    ringWidth * 1.2
  );
  outerGrad.addColorStop(0, colors.highlight);
  outerGrad.addColorStop(0.4, colors.primary);
  outerGrad.addColorStop(0.7, colors.primary);
  outerGrad.addColorStop(1, colors.shadow);
  ctx.fillStyle = outerGrad;
  ctx.fill();

  // Reset shadow for inner parts
  ctx.shadowColor = 'transparent';
  ctx.shadowBlur = 0;
  ctx.shadowOffsetX = 0;
  ctx.shadowOffsetY = 0;

  // === Inner hole (cut-out for the finger) ===
  ctx.beginPath();
  ctx.ellipse(0, 0, ringWidth - bandThickness, ringHeight - bandThickness, 0, 0, Math.PI * 2);
  ctx.globalCompositeOperation = 'destination-out';
  ctx.fillStyle = 'black';
  ctx.fill();
  ctx.globalCompositeOperation = 'source-over';

  // === Specular highlight (top-left shine) ===
  ctx.beginPath();
  ctx.ellipse(
    -ringWidth * 0.25,
    -ringHeight * 0.3,
    ringWidth * 0.35,
    ringHeight * 0.2,
    -0.4,
    0,
    Math.PI * 2
  );
  const specGrad = ctx.createRadialGradient(
    -ringWidth * 0.25,
    -ringHeight * 0.3,
    0,
    -ringWidth * 0.25,
    -ringHeight * 0.3,
    ringWidth * 0.35
  );
  specGrad.addColorStop(0, 'rgba(255,255,255,0.7)');
  specGrad.addColorStop(0.5, 'rgba(255,255,255,0.2)');
  specGrad.addColorStop(1, 'rgba(255,255,255,0)');
  ctx.fillStyle = specGrad;
  ctx.fill();

  // === Secondary highlight (bottom-right) ===
  ctx.beginPath();
  ctx.ellipse(
    ringWidth * 0.2,
    ringHeight * 0.25,
    ringWidth * 0.2,
    ringHeight * 0.12,
    0.5,
    0,
    Math.PI * 2
  );
  const sec = ctx.createRadialGradient(
    ringWidth * 0.2,
    ringHeight * 0.25,
    0,
    ringWidth * 0.2,
    ringHeight * 0.25,
    ringWidth * 0.2
  );
  sec.addColorStop(0, 'rgba(255,255,255,0.3)');
  sec.addColorStop(1, 'rgba(255,255,255,0)');
  ctx.fillStyle = sec;
  ctx.fill();

  // === Diamond / gem for DIAMOND style ===
  if (ringStyle === 'DIAMOND') {
    drawDiamond(ctx, 0, -ringHeight + bandThickness * 0.3, bandThickness * 1.2, scale);
  }

  // === Accent line (inner edge) ===
  ctx.beginPath();
  ctx.ellipse(0, 0, ringWidth - bandThickness + 2, ringHeight - bandThickness + 2, 0, 0, Math.PI * 2);
  ctx.strokeStyle = colors.accent;
  ctx.lineWidth = 1.2 * scale;
  ctx.globalAlpha = 0.4;
  ctx.stroke();
  ctx.globalAlpha = 1;

  ctx.restore();
}

function drawDiamond(
  ctx: CanvasRenderingContext2D,
  cx: number,
  cy: number,
  size: number,
  scale: number
) {
  const s = size * 0.8;
  ctx.save();
  ctx.translate(cx, cy);

  // Diamond base
  ctx.beginPath();
  ctx.moveTo(0, -s);
  ctx.lineTo(s * 0.7, -s * 0.2);
  ctx.lineTo(s * 0.5, s * 0.4);
  ctx.lineTo(0, s * 0.7);
  ctx.lineTo(-s * 0.5, s * 0.4);
  ctx.lineTo(-s * 0.7, -s * 0.2);
  ctx.closePath();

  const dGrad = ctx.createLinearGradient(0, -s, 0, s * 0.7);
  dGrad.addColorStop(0, '#E0FFFF');
  dGrad.addColorStop(0.3, '#B9F2FF');
  dGrad.addColorStop(0.6, '#87CEEB');
  dGrad.addColorStop(1, '#ADD8E6');
  ctx.fillStyle = dGrad;
  ctx.fill();

  // Sparkle lines
  ctx.strokeStyle = 'rgba(255,255,255,0.6)';
  ctx.lineWidth = 1;
  ctx.beginPath();
  ctx.moveTo(-s * 0.3, -s * 0.3);
  ctx.lineTo(s * 0.3, s * 0.1);
  ctx.moveTo(s * 0.1, -s * 0.5);
  ctx.lineTo(-s * 0.1, 0);
  ctx.stroke();

  // White sparkle dot
  ctx.beginPath();
  ctx.arc(-s * 0.15, -s * 0.4, 2 * scale, 0, Math.PI * 2);
  ctx.fillStyle = 'rgba(255,255,255,0.9)';
  ctx.fill();

  ctx.restore();
}
