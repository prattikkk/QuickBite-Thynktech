/**
 * Settings page — Notification channel preferences
 */

import { useState, useEffect } from 'react';
import api from '../services/api';

interface NotificationPreferences {
  pushEnabled: boolean;
  emailOrderUpdates: boolean;
  emailPromotions: boolean;
  smsDeliveryAlerts: boolean;
}

const defaultPrefs: NotificationPreferences = {
  pushEnabled: true,
  emailOrderUpdates: true,
  emailPromotions: false,
  smsDeliveryAlerts: true,
};

export default function Settings() {
  const [preferences, setPreferences] = useState<NotificationPreferences>(defaultPrefs);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [darkMode, setDarkMode] = useState(() => {
    if (typeof window !== 'undefined') {
      return localStorage.getItem('theme') === 'dark' || document.documentElement.classList.contains('dark');
    }
    return false;
  });

  useEffect(() => {
    loadPreferences();
  }, []);

  const loadPreferences = async () => {
    try {
      setLoading(true);
      const res = await api.get<any, any>('/notifications/preferences');
      const data = res?.data ?? res;
      setPreferences(data);
    } catch {
      setError('Failed to load preferences');
    } finally {
      setLoading(false);
    }
  };

  const handleToggle = async (key: keyof NotificationPreferences) => {
    const newValue = !preferences[key];
    const updated = { ...preferences, [key]: newValue };
    setPreferences(updated);
    setError(null);
    setSuccess(null);

    try {
      setSaving(true);
      await api.put('/notifications/preferences', { [key]: newValue });
      setSuccess('Preferences saved');
      setTimeout(() => setSuccess(null), 2000);
    } catch {
      // Revert on error
      setPreferences(preferences);
      setError('Failed to save preferences');
    } finally {
      setSaving(false);
    }
  };

  const toggleDarkMode = () => {
    const next = !darkMode;
    setDarkMode(next);
    document.documentElement.classList.toggle('dark', next);
    localStorage.setItem('theme', next ? 'dark' : 'light');
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[50vh]" role="status" aria-label="Loading settings">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
        <span className="sr-only">Loading preferences…</span>
      </div>
    );
  }

  const toggleItems: { key: keyof NotificationPreferences; label: string; description: string }[] = [
    {
      key: 'pushEnabled',
      label: 'Push Notifications',
      description: 'Receive order updates and alerts via push notifications',
    },
    {
      key: 'emailOrderUpdates',
      label: 'Email — Order Updates',
      description: 'Get notified via email when your order status changes',
    },
    {
      key: 'emailPromotions',
      label: 'Email — Promotions',
      description: 'Receive promotional offers, deals, and discount codes',
    },
    {
      key: 'smsDeliveryAlerts',
      label: 'SMS — Delivery Alerts',
      description: 'Get an SMS when your driver is nearby or has arrived',
    },
  ];

  return (
    <div className="max-w-2xl mx-auto p-6">
      <h1 className="text-2xl font-bold text-gray-900 mb-2">Settings</h1>
      <p className="text-gray-500 mb-6">Manage your notification preferences</p>

      {error && (
        <div className="mb-4 p-3 bg-red-50 text-red-700 rounded-lg border border-red-200" role="alert">
          {error}
        </div>
      )}
      {success && (
        <div className="mb-4 p-3 bg-green-50 text-green-700 rounded-lg border border-green-200" role="status">
          {success}
        </div>
      )}

      <div className="bg-white rounded-lg shadow divide-y divide-gray-100">
        <div className="px-6 py-4">
          <h2 className="text-lg font-semibold text-gray-800">Notification Channels</h2>
          <p className="text-sm text-gray-500">Choose how you want to be notified</p>
        </div>

        {toggleItems.map((item) => (
          <div key={item.key} className="flex items-center justify-between px-6 py-4 hover:bg-gray-50 transition-colors">
            <div className="flex-1 min-w-0 mr-4">
              <p className="font-medium text-gray-900">{item.label}</p>
              <p className="text-sm text-gray-500">{item.description}</p>
            </div>
            <button
              type="button"
              role="switch"
              aria-checked={preferences[item.key]}
              aria-label={`${item.label}: ${preferences[item.key] ? 'enabled' : 'disabled'}`}
              disabled={saving}
              onClick={() => handleToggle(item.key)}
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 ${
                preferences[item.key] ? 'bg-primary-600' : 'bg-gray-300'
              } ${saving ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}`}
            >
              <span
                className={`inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform ${
                  preferences[item.key] ? 'translate-x-6' : 'translate-x-1'
                }`}
              />
            </button>
          </div>
        ))}
      </div>

      {/* Dark Mode */}
      <div className="mt-6 bg-white rounded-lg shadow divide-y divide-gray-100">
        <div className="px-6 py-4">
          <h2 className="text-lg font-semibold text-gray-800">Appearance</h2>
          <p className="text-sm text-gray-500">Customize the look and feel</p>
        </div>
        <div className="flex items-center justify-between px-6 py-4 hover:bg-gray-50 transition-colors">
          <div className="flex-1 min-w-0 mr-4">
            <p className="font-medium text-gray-900">Dark Mode</p>
            <p className="text-sm text-gray-500">Use a darker color scheme that's easier on your eyes</p>
          </div>
          <button
            type="button"
            role="switch"
            aria-checked={darkMode}
            aria-label={`Dark mode: ${darkMode ? 'enabled' : 'disabled'}`}
            onClick={toggleDarkMode}
            className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 ${
              darkMode ? 'bg-primary-600' : 'bg-gray-300'
            } cursor-pointer`}
          >
            <span
              className={`inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform ${
                darkMode ? 'translate-x-6' : 'translate-x-1'
              }`}
            />
          </button>
        </div>
      </div>
    </div>
  );
}
