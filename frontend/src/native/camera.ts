/**
 * Native Camera plugin wrapper.
 *
 * Uses @capacitor/camera on native platforms for better UX (direct camera
 * launch, image quality control, EXIF handling). Falls back to the
 * standard <input type="file" capture> on web browsers / PWA.
 */

import { isNative } from './platform';

/* ── Types ───────────────────────────────────────────────────── */

export interface CapturedPhoto {
  /** Data URI (base64) or file path depending on platform. */
  dataUrl: string;
  /** MIME type of the captured image. */
  mimeType: string;
  /** The File object (available on web; null on native). */
  file: File | null;
}

export interface CaptureOptions {
  /** Preferred quality 0–100 (default: 80). */
  quality?: number;
  /** Max width in px after resize (default: 1280). */
  maxWidth?: number;
  /** Max height in px after resize (default: 960). */
  maxHeight?: number;
  /** Source: 'camera' | 'gallery' | 'prompt' (default: 'prompt'). */
  source?: 'camera' | 'gallery' | 'prompt';
}

/* ── Implementation ──────────────────────────────────────────── */

let _capacitorCamera: typeof import('@capacitor/camera').Camera | null = null;

async function getPlugin() {
  if (!_capacitorCamera) {
    const mod = await import('@capacitor/camera');
    _capacitorCamera = mod.Camera;
  }
  return _capacitorCamera;
}

/**
 * Capture or pick a photo. Returns a base64 data URL.
 *
 * On native: uses the Capacitor Camera plugin with direct camera launch.
 * On web: falls back to null (the existing <input type="file" capture>
 * workflow in ProofCaptureModal handles web capture).
 */
export async function capturePhoto(
  opts: CaptureOptions = {},
): Promise<CapturedPhoto | null> {
  if (!isNative()) {
    // On web, return null — the caller should fall back to <input> picker.
    return null;
  }

  const camera = await getPlugin();
  const { CameraResultType, CameraSource } = await import('@capacitor/camera');

  const sourceMap = {
    camera: CameraSource.Camera,
    gallery: CameraSource.Photos,
    prompt: CameraSource.Prompt,
  } as const;

  const image = await camera.getPhoto({
    quality: opts.quality ?? 80,
    width: opts.maxWidth ?? 1280,
    height: opts.maxHeight ?? 960,
    resultType: CameraResultType.DataUrl,
    source: sourceMap[opts.source ?? 'prompt'],
    correctOrientation: true,
    allowEditing: false,
  });

  if (!image.dataUrl) return null;

  return {
    dataUrl: image.dataUrl,
    mimeType: `image/${image.format}`,
    file: null, // native doesn't produce a File — convert dataUrl to Blob upstream if needed
  };
}

/**
 * Request camera/gallery permissions on native.
 * Returns 'granted' | 'denied' | 'prompt'.
 */
export async function checkCameraPermission(): Promise<'granted' | 'denied' | 'prompt'> {
  if (!isNative()) return 'granted'; // web always has access via <input>

  const camera = await getPlugin();
  const perm = await camera.checkPermissions();
  if (perm.camera === 'granted' && perm.photos === 'granted') return 'granted';
  if (perm.camera === 'denied') return 'denied';
  return 'prompt';
}

/**
 * Request camera permissions explicitly (native only).
 */
export async function requestCameraPermission(): Promise<'granted' | 'denied' | 'prompt'> {
  if (!isNative()) return 'granted';

  const camera = await getPlugin();
  const perm = await camera.requestPermissions();
  if (perm.camera === 'granted') return 'granted';
  if (perm.camera === 'denied') return 'denied';
  return 'prompt';
}

/**
 * Convert a base64 data URL to a File object for FormData upload.
 * Useful when the Capacitor camera returns a dataUrl but the upload
 * API expects a File/Blob.
 */
export function dataUrlToFile(dataUrl: string, filename: string): File {
  const [header, base64] = dataUrl.split(',');
  const mime = header.match(/:(.*?);/)?.[1] ?? 'image/jpeg';
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return new File([bytes], filename, { type: mime });
}
