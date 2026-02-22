/**
 * Native plugin abstraction layer — barrel exports.
 *
 * Import everything from `@/native` to access platform-aware wrappers
 * that use Capacitor on native and fall back to Web APIs on browsers.
 *
 * @example
 *   import { isNative, capturePhoto, watchPosition } from '@/native';
 */

// Platform detection
export {
  isNative,
  isPWA,
  isBrowser,
  getPlatform,
  resolveApiBaseUrl,
  resolveWsUrl,
} from './platform';

// Geolocation
export {
  watchPosition,
  getCurrentPosition,
  checkPermission as checkGeoPermission,
  type GeoPosition,
  type GeoWatchOptions,
} from './geolocation';

// Camera
export {
  capturePhoto,
  checkCameraPermission,
  requestCameraPermission,
  dataUrlToFile,
  type CapturedPhoto,
  type CaptureOptions,
} from './camera';

// Push Notifications
export {
  initPushNotifications,
  requestPushPermission,
  checkPushPermission,
  onTokenReceived,
  onNotificationReceived,
  onNotificationAction,
  type PushToken,
  type PushNotificationData,
} from './push';

// Network
export {
  getNetworkStatus,
  onNetworkStatusChange,
  type NetworkStatus,
} from './network';

// Haptics
export {
  impactLight,
  impactMedium,
  impactHeavy,
  notificationSuccess,
  notificationWarning,
  notificationError,
} from './haptics';

// App Lifecycle
export {
  initNativeApp,
  onAppStateChange,
  onBackButton,
  onDeepLink,
} from './app-lifecycle';
