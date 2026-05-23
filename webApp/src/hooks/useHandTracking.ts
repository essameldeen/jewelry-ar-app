import { useRef, useCallback, useState, useEffect } from 'react';
import { HandLandmarker, FilesetResolver, DrawingUtils } from '@mediapipe/tasks-vision';

export interface HandLandmarks {
  indexFingerTip: { x: number; y: number; z: number };
  middleFingerTip: { x: number; y: number; z: number };
  ringFingerBase: { x: number; y: number; z: number };
  ringFingerTip: { x: number; y: number; z: number };
  wrist: { x: number; y: number; z: number };
  isDetected: boolean;
}

const SMOOTHING = 0.6;

function lerp(a: number, b: number, t: number) {
  return a + (b - a) * t;
}

export function useHandTracking() {
  const handLandmarkerRef = useRef<HandLandmarker | null>(null);
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const animFrameRef = useRef<number>(0);
  const [landmarks, setLandmarks] = useState<HandLandmarks>({
    indexFingerTip: { x: 0, y: 0, z: 0 },
    middleFingerTip: { x: 0, y: 0, z: 0 },
    ringFingerBase: { x: 0, y: 0, z: 0 },
    ringFingerTip: { x: 0, y: 0, z: 0 },
    wrist: { x: 0, y: 0, z: 0 },
    isDetected: false,
  });
  const [isLoading, setIsLoading] = useState(true);
  const prevLandmarks = useRef<HandLandmarks | null>(null);

  // Initialize MediaPipe
  const initHandLandmarker = useCallback(async () => {
    try {
      setIsLoading(true);
      const vision = await FilesetResolver.forVisionTasks(
        'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@latest/wasm'
      );
      handLandmarkerRef.current = await HandLandmarker.createFromOptions(vision, {
        baseOptions: {
          modelAssetPath:
            'https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task',
          delegate: 'GPU',
        },
        runningMode: 'VIDEO',
        numHands: 1,
        minHandDetectionConfidence: 0.5,
        minHandPresenceConfidence: 0.5,
        minTrackingConfidence: 0.5,
      });
      setIsLoading(false);
    } catch (err) {
      console.error('Failed to init HandLandmarker:', err);
      setIsLoading(false);
    }
  }, []);

  // Start camera
  const startCamera = useCallback(async (video: HTMLVideoElement) => {
    videoRef.current = video;
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'user', width: 640, height: 480 },
      });
      video.srcObject = stream;
      await video.play();
    } catch (err) {
      console.error('Camera access failed:', err);
    }
  }, []);

  // Detection loop
  const startDetection = useCallback(() => {
    const detect = () => {
      const video = videoRef.current;
      const landmarker = handLandmarkerRef.current;
      if (!video || !landmarker || video.readyState < 2) {
        animFrameRef.current = requestAnimationFrame(detect);
        return;
      }

      const result = landmarker.detectForVideo(video, performance.now());

      if (result.landmarks && result.landmarks.length > 0) {
        const hand = result.landmarks[0];
        // MediaPipe landmarks: 0=wrist, 8=index tip, 12=middle tip, 13=ring base, 16=ring tip
        const raw: HandLandmarks = {
          wrist: hand[0],
          indexFingerTip: hand[8],
          middleFingerTip: hand[12],
          ringFingerBase: hand[13],
          ringFingerTip: hand[16],
          isDetected: true,
        };

        // Smooth with previous frame
        if (prevLandmarks.current && prevLandmarks.current.isDetected) {
          const p = prevLandmarks.current;
          const smoothed: HandLandmarks = {
            indexFingerTip: {
              x: lerp(p.indexFingerTip.x, raw.indexFingerTip.x, SMOOTHING),
              y: lerp(p.indexFingerTip.y, raw.indexFingerTip.y, SMOOTHING),
              z: lerp(p.indexFingerTip.z, raw.indexFingerTip.z, SMOOTHING),
            },
            middleFingerTip: {
              x: lerp(p.middleFingerTip.x, raw.middleFingerTip.x, SMOOTHING),
              y: lerp(p.middleFingerTip.y, raw.middleFingerTip.y, SMOOTHING),
              z: lerp(p.middleFingerTip.z, raw.middleFingerTip.z, SMOOTHING),
            },
            ringFingerBase: {
              x: lerp(p.ringFingerBase.x, raw.ringFingerBase.x, SMOOTHING),
              y: lerp(p.ringFingerBase.y, raw.ringFingerBase.y, SMOOTHING),
              z: lerp(p.ringFingerBase.z, raw.ringFingerBase.z, SMOOTHING),
            },
            ringFingerTip: {
              x: lerp(p.ringFingerTip.x, raw.ringFingerTip.x, SMOOTHING),
              y: lerp(p.ringFingerTip.y, raw.ringFingerTip.y, SMOOTHING),
              z: lerp(p.ringFingerTip.z, raw.ringFingerTip.z, SMOOTHING),
            },
            wrist: {
              x: lerp(p.wrist.x, raw.wrist.x, SMOOTHING),
              y: lerp(p.wrist.y, raw.wrist.y, SMOOTHING),
              z: lerp(p.wrist.z, raw.wrist.z, SMOOTHING),
            },
            isDetected: true,
          };
          prevLandmarks.current = smoothed;
          setLandmarks(smoothed);
        } else {
          prevLandmarks.current = raw;
          setLandmarks(raw);
        }
      } else {
        prevLandmarks.current = null;
        setLandmarks((prev) => (prev.isDetected ? { ...prev, isDetected: false } : prev));
      }

      animFrameRef.current = requestAnimationFrame(detect);
    };

    animFrameRef.current = requestAnimationFrame(detect);
  }, []);

  // Stop
  const stop = useCallback(() => {
    if (animFrameRef.current) cancelAnimationFrame(animFrameRef.current);
    if (videoRef.current?.srcObject) {
      (videoRef.current.srcObject as MediaStream).getTracks().forEach((t) => t.stop());
    }
  }, []);

  return {
    landmarks,
    isLoading,
    initHandLandmarker,
    startCamera,
    startDetection,
    stop,
    videoRef,
    canvasRef,
  };
}
