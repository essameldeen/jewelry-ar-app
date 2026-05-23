import React, { useEffect, useRef, useState } from 'react';
import { Product, RingStyle } from '../data/products';
import { useHandTracking } from '../hooks/useHandTracking';
import { usePoseTracking } from '../hooks/usePoseTracking';
import { drawRing, drawOverlayImage } from './RingRenderer';
import { useLang } from '../context/LanguageContext';

interface Props {
  product: Product;
  onBack: () => void;
}

export const ARTryOn: React.FC<Props> = ({ product, onBack }) => {
  const { lang } = useLang();
  const ar = lang === 'ar';
  const isNecklace = product.category === 'necklace' || product.category === 'luxury';

  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const overlayImgRef = useRef<HTMLImageElement | null>(null);
  const [overlayLoaded, setOverlayLoaded] = useState(false);
  const ringStyle: RingStyle = product.ringStyle;

  // Always call both hooks (React rules) — only init the relevant one
  const handTracking = useHandTracking();
  const poseTracking = usePoseTracking();

  const isLoading = isNecklace ? poseTracking.isLoading : handTracking.isLoading;
  const isDetected = isNecklace
    ? poseTracking.poseLandmarks.isDetected
    : handTracking.landmarks.isDetected;

  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    const init = async () => {
      if (isNecklace) {
        await poseTracking.initPoseLandmarker();
        await poseTracking.startCamera(video);
        poseTracking.startDetection();
      } else {
        await handTracking.initHandLandmarker();
        await handTracking.startCamera(video);
        handTracking.startDetection();
      }
    };
    init();
    return () => {
      handTracking.stop();
      poseTracking.stop();
    };
  }, []);

  // Load product overlay image
  useEffect(() => {
    if (!product.overlay) return;
    setOverlayLoaded(false);
    const img = new Image();
    img.onload = () => {
      overlayImgRef.current = img;
      setOverlayLoaded(true);
    };
    img.src = product.overlay;
  }, [product.overlay]);

  // Draw overlay
  useEffect(() => {
    const canvas = canvasRef.current;
    const video = videoRef.current;
    if (!canvas || !video) return;

    canvas.width = video.videoWidth || 640;
    canvas.height = video.videoHeight || 480;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.clearRect(0, 0, canvas.width, canvas.height);

    ctx.save();
    ctx.translate(canvas.width, 0);
    ctx.scale(-1, 1);

    if (overlayImgRef.current && overlayLoaded) {
      drawOverlayImage(
        ctx,
        overlayImgRef.current,
        isNecklace ? null : handTracking.landmarks,
        isNecklace ? poseTracking.poseLandmarks : null,
        product.category,
        canvas.width,
        canvas.height
      );
    } else if (!isNecklace) {
      drawRing(ctx, handTracking.landmarks, ringStyle, canvas.width, canvas.height);
    }

    ctx.restore();
  }, [handTracking.landmarks, poseTracking.poseLandmarks, ringStyle, overlayLoaded]);

  return (
    <div style={styles.container}>
      {/* Camera + Canvas */}
      <div style={styles.cameraContainer}>
        <video ref={videoRef} style={styles.video} playsInline muted />
        <canvas ref={canvasRef} style={styles.canvas} />

        {/* Loading */}
        {isLoading && (
          <div style={styles.loadingOverlay}>
            <div style={styles.spinner} />
            <p style={styles.loadingText}>{ar ? 'جاري تحميل كاشف اليد...' : 'Loading hand detection...'}</p>
            <p style={styles.loadingHint}>{ar ? 'قد يستغرق بضع ثوانٍ' : 'This may take a few seconds'}</p>
          </div>
        )}

        {/* Status */}
        {!isLoading && (
          <div
            style={{
              ...styles.statusBadge,
              background: isDetected
                ? 'linear-gradient(135deg, #10B981, #059669)'
                : 'linear-gradient(135deg, #F472B6, #EC4899)',
            }}
          >
            <span style={styles.statusDot(isDetected)} />
            {isNecklace
              ? isDetected
                ? (ar ? 'تم الكشف عن الجسم' : 'Body Detected')
                : (ar ? 'ادخل في الإطار' : 'Step into frame')
              : isDetected
              ? (ar ? 'تم الكشف عن اليد' : 'Hand Detected')
              : (ar ? 'أظهر يدك' : 'Show your hand')}
          </div>
        )}
      </div>

      {/* Top Controls */}
      <div style={styles.topBar}>
        <button style={styles.backBtn} onClick={onBack}>
          <span>{ar ? '→' : '←'}</span> {ar ? 'رجوع' : 'Back'}
        </button>

      </div>

      {/* Bottom Product Info */}
      <div style={styles.bottomBar}>
        <div style={styles.productInfo}>
          <h2 style={styles.productName}>{ar ? product.nameAr : product.name}</h2>
          <p style={styles.productPrice}>${product.price.toLocaleString()}</p>
        </div>
        <p style={styles.hint}>
          {isNecklace
            ? (ar ? 'اعرض كتفيك وصدرك للكاميرا' : 'Show your shoulders & chest to the camera')
            : product.category === 'bracelet'
            ? (ar ? 'ضع معصمك في المركز' : 'Position your wrist in the center')
            : (ar ? 'ضع إصبع الخاتم في المركز' : 'Position your ring finger in the center')}
        </p>
      </div>
    </div>
  );
};

const styles: Record<string, React.CSSProperties> & { statusDot: (active: boolean) => React.CSSProperties } = {
  container: {
    position: 'relative',
    width: '100vw',
    height: '100vh',
    background: '#000',
    overflow: 'hidden',
  },
  cameraContainer: {
    position: 'relative',
    width: '100%',
    height: '100%',
  },
  video: {
    position: 'absolute',
    top: 0,
    left: 0,
    width: '100%',
    height: '100%',
    objectFit: 'cover',
    transform: 'scaleX(-1)',
  },
  canvas: {
    position: 'absolute',
    top: 0,
    left: 0,
    width: '100%',
    height: '100%',
    objectFit: 'cover',
    pointerEvents: 'none',
  },

  // Loading
  loadingOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    background: 'rgba(0,0,0,0.75)',
    zIndex: 10,
  },
  spinner: {
    width: 52,
    height: 52,
    border: '4px solid rgba(255,255,255,0.15)',
    borderTopColor: '#D946EF',
    borderRadius: '50%',
    animation: 'spin 0.8s linear infinite',
  },
  loadingText: {
    color: '#fff',
    marginTop: 16,
    fontSize: '1rem',
    fontWeight: 500,
  },
  loadingHint: {
    color: 'rgba(255,255,255,0.5)',
    marginTop: 6,
    fontSize: '0.8rem',
  },

  // Status
  statusBadge: {
    position: 'absolute',
    bottom: 110,
    left: '50%',
    transform: 'translateX(-50%)',
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    padding: '8px 20px',
    borderRadius: 25,
    color: '#fff',
    fontSize: '0.8rem',
    fontWeight: 600,
    zIndex: 5,
    boxShadow: '0 4px 15px rgba(0,0,0,0.3)',
  },
  statusDot: (active: boolean): React.CSSProperties => ({
    width: 8,
    height: 8,
    borderRadius: '50%',
    background: active ? '#6EE7B7' : '#FCA5A5',
    boxShadow: active ? '0 0 8px #6EE7B7' : '0 0 8px #FCA5A5',
  }),

  // Top bar
  topBar: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    padding: 16,
    zIndex: 5,
    background: 'linear-gradient(180deg, rgba(0,0,0,0.5) 0%, transparent 100%)',
  },
  backBtn: {
    display: 'flex',
    alignItems: 'center',
    gap: 6,
    padding: '10px 20px',
    borderRadius: 12,
    border: 'none',
    background: 'rgba(255,255,255,0.2)',
    backdropFilter: 'blur(10px)',
    color: '#fff',
    fontSize: '0.9rem',
    fontWeight: 600,
  },
  // Bottom bar
  bottomBar: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    padding: '30px 24px 32px',
    background: 'linear-gradient(transparent, rgba(0,0,0,0.8))',
    textAlign: 'center',
    zIndex: 5,
  },
  productInfo: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'baseline',
    gap: 16,
    marginBottom: 6,
  },
  productName: {
    fontFamily: "'Playfair Display', serif",
    color: '#fff',
    fontSize: '1.4rem',
    fontWeight: 700,
  },
  productPrice: {
    color: '#D946EF',
    fontSize: '1.1rem',
    fontWeight: 600,
  },
  hint: {
    color: 'rgba(255,255,255,0.6)',
    fontSize: '0.8rem',
  },
};
