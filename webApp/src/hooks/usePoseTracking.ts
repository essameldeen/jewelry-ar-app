import { useRef, useCallback, useState } from 'react';
import { PoseLandmarker, FilesetResolver } from '@mediapipe/tasks-vision';

export interface PoseLandmarks {
  nose: { x: number; y: number; z: number };
  leftShoulder: { x: number; y: number; z: number };
  rightShoulder: { x: number; y: number; z: number };
  isDetected: boolean;
}

const SMOOTHING = 0.35;

function lerp(a: number, b: number, t: number) {
  return a + (b - a) * t;
}

export function usePoseTracking() {
  const poseLandmarkerRef = useRef<PoseLandmarker | null>(null);
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const animFrameRef = useRef<number>(0);
  const prevPose = useRef<PoseLandmarks | null>(null);

  const [poseLandmarks, setPoseLandmarks] = useState<PoseLandmarks>({
    nose: { x: 0.5, y: 0.15, z: 0 },
    leftShoulder: { x: 0.35, y: 0.4, z: 0 },
    rightShoulder: { x: 0.65, y: 0.4, z: 0 },
    isDetected: false,
  });
  const [isLoading, setIsLoading] = useState(true);

  const initPoseLandmarker = useCallback(async () => {
    try {
      setIsLoading(true);
      const vision = await FilesetResolver.forVisionTasks(
        'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@latest/wasm'
      );
      poseLandmarkerRef.current = await PoseLandmarker.createFromOptions(vision, {
        baseOptions: {
          modelAssetPath:
            'https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/1/pose_landmarker_lite.task',
          delegate: 'GPU',
        },
        runningMode: 'VIDEO',
        numPoses: 1,
        minPoseDetectionConfidence: 0.5,
        minPosePresenceConfidence: 0.5,
        minTrackingConfidence: 0.5,
      });
      setIsLoading(false);
    } catch (err) {
      console.error('Failed to init PoseLandmarker:', err);
      setIsLoading(false);
    }
  }, []);

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

  const startDetection = useCallback(() => {
    const detect = () => {
      const video = videoRef.current;
      const landmarker = poseLandmarkerRef.current;
      if (!video || !landmarker || video.readyState < 2) {
        animFrameRef.current = requestAnimationFrame(detect);
        return;
      }

      const result = landmarker.detectForVideo(video, performance.now());

      if (result.landmarks && result.landmarks.length > 0) {
        const pose = result.landmarks[0];
        // 0=nose, 11=left shoulder, 12=right shoulder
        const raw: PoseLandmarks = {
          nose: pose[0],
          leftShoulder: pose[11],
          rightShoulder: pose[12],
          isDetected: true,
        };

        if (prevPose.current?.isDetected) {
          const p = prevPose.current;
          const smoothed: PoseLandmarks = {
            nose: {
              x: lerp(p.nose.x, raw.nose.x, SMOOTHING),
              y: lerp(p.nose.y, raw.nose.y, SMOOTHING),
              z: lerp(p.nose.z, raw.nose.z, SMOOTHING),
            },
            leftShoulder: {
              x: lerp(p.leftShoulder.x, raw.leftShoulder.x, SMOOTHING),
              y: lerp(p.leftShoulder.y, raw.leftShoulder.y, SMOOTHING),
              z: lerp(p.leftShoulder.z, raw.leftShoulder.z, SMOOTHING),
            },
            rightShoulder: {
              x: lerp(p.rightShoulder.x, raw.rightShoulder.x, SMOOTHING),
              y: lerp(p.rightShoulder.y, raw.rightShoulder.y, SMOOTHING),
              z: lerp(p.rightShoulder.z, raw.rightShoulder.z, SMOOTHING),
            },
            isDetected: true,
          };
          prevPose.current = smoothed;
          setPoseLandmarks(smoothed);
        } else {
          prevPose.current = raw;
          setPoseLandmarks(raw);
        }
      } else {
        prevPose.current = null;
        setPoseLandmarks((prev) => (prev.isDetected ? { ...prev, isDetected: false } : prev));
      }

      animFrameRef.current = requestAnimationFrame(detect);
    };
    animFrameRef.current = requestAnimationFrame(detect);
  }, []);

  const stop = useCallback(() => {
    if (animFrameRef.current) cancelAnimationFrame(animFrameRef.current);
    if (videoRef.current?.srcObject) {
      (videoRef.current.srcObject as MediaStream).getTracks().forEach((t) => t.stop());
    }
  }, []);

  return { poseLandmarks, isLoading, initPoseLandmarker, startCamera, startDetection, stop };
}
