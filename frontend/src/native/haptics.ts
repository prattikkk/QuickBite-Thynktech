/**
 * Native Haptics plugin wrapper.
 *
 * Provides tactile feedback on order events (new assignment, delivery confirmed).
 * Uses @capacitor/haptics on native; silently no-ops on web.
 */

import { isNative } from './platform';

let _plugin: typeof import('@capacitor/haptics').Haptics | null = null;

async function getPlugin() {
  if (!_plugin) {
    const mod = await import('@capacitor/haptics');
    _plugin = mod.Haptics;
  }
  return _plugin;
}

/** Light tap — e.g., button press. */
export async function impactLight(): Promise<void> {
  if (!isNative()) return;
  const haptics = await getPlugin();
  const { ImpactStyle } = await import('@capacitor/haptics');
  await haptics.impact({ style: ImpactStyle.Light });
}

/** Medium tap — e.g., order state transition. */
export async function impactMedium(): Promise<void> {
  if (!isNative()) return;
  const haptics = await getPlugin();
  const { ImpactStyle } = await import('@capacitor/haptics');
  await haptics.impact({ style: ImpactStyle.Medium });
}

/** Heavy tap — e.g., new order assignment. */
export async function impactHeavy(): Promise<void> {
  if (!isNative()) return;
  const haptics = await getPlugin();
  const { ImpactStyle } = await import('@capacitor/haptics');
  await haptics.impact({ style: ImpactStyle.Heavy });
}

/** Success notification — e.g., delivery confirmed. */
export async function notificationSuccess(): Promise<void> {
  if (!isNative()) return;
  const haptics = await getPlugin();
  const { NotificationType } = await import('@capacitor/haptics');
  await haptics.notification({ type: NotificationType.Success });
}

/** Warning notification — e.g., order about to expire. */
export async function notificationWarning(): Promise<void> {
  if (!isNative()) return;
  const haptics = await getPlugin();
  const { NotificationType } = await import('@capacitor/haptics');
  await haptics.notification({ type: NotificationType.Warning });
}

/** Error notification — e.g., delivery cancelled. */
export async function notificationError(): Promise<void> {
  if (!isNative()) return;
  const haptics = await getPlugin();
  const { NotificationType } = await import('@capacitor/haptics');
  await haptics.notification({ type: NotificationType.Error });
}
