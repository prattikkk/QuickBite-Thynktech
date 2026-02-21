/**
 * User Profile page — view & edit profile, export data, delete account
 * Phase 5 — API Completeness
 */

import { useState, useEffect, FormEvent } from 'react';
import { useAuthStore, useToastStore } from '../store';
import { userService, UserProfile, ProfileUpdateRequest } from '../services/user.service';

export default function Profile() {
  const { logout } = useAuthStore();
  const { success, error: showError } = useToastStore();

  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  const [form, setForm] = useState({ name: '', phone: '' });

  useEffect(() => {
    loadProfile();
  }, []);

  const loadProfile = async () => {
    try {
      const data = await userService.getProfile();
      setProfile(data);
      setForm({ name: data.name || '', phone: data.phone || '' });
    } catch (err: any) {
      showError(err.message || 'Failed to load profile');
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async (e: FormEvent) => {
    e.preventDefault();
    if (!form.name.trim()) {
      showError('Name is required');
      return;
    }
    setSaving(true);
    try {
      const updated: ProfileUpdateRequest = {};
      if (form.name !== profile?.name) updated.name = form.name.trim();
      if (form.phone !== (profile?.phone || '')) updated.phone = form.phone.trim();

      const data = await userService.updateProfile(updated);
      setProfile(data);
      success('Profile updated');
    } catch (err: any) {
      showError(err.message || 'Failed to update profile');
    } finally {
      setSaving(false);
    }
  };

  const handleExport = async () => {
    setExporting(true);
    try {
      const data = await userService.exportData();
      // Download as JSON file
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `quickbite-data-export-${new Date().toISOString().slice(0, 10)}.json`;
      a.click();
      URL.revokeObjectURL(url);
      success('Data exported — check your downloads');
    } catch (err: any) {
      showError(err.message || 'Failed to export data');
    } finally {
      setExporting(false);
    }
  };

  const handleDelete = async () => {
    setDeleting(true);
    try {
      await userService.deleteAccount();
      success('Account deleted');
      logout();
    } catch (err: any) {
      showError(err.message || 'Failed to delete account');
    } finally {
      setDeleting(false);
      setShowDeleteConfirm(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">My Profile</h1>

      {/* Profile Form */}
      <form onSubmit={handleSave} className="bg-white rounded-lg shadow p-6 mb-6 space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
          <input
            type="email"
            value={profile?.email || ''}
            disabled
            className="w-full px-3 py-2 border border-gray-300 rounded-md bg-gray-50 text-gray-500"
          />
          <p className="text-xs text-gray-400 mt-1">Email cannot be changed</p>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Name</label>
          <input
            type="text"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-primary-500 focus:border-primary-500"
            required
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Phone</label>
          <input
            type="tel"
            value={form.phone}
            onChange={(e) => setForm({ ...form, phone: e.target.value })}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-primary-500 focus:border-primary-500"
            placeholder="(optional)"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Role</label>
          <span className="inline-block px-2 py-1 text-xs font-medium bg-primary-100 text-primary-700 rounded">
            {profile?.role}
          </span>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Member since</label>
          <span className="text-sm text-gray-600">
            {profile?.createdAt ? new Date(profile.createdAt).toLocaleDateString() : '—'}
          </span>
        </div>

        <button
          type="submit"
          disabled={saving}
          className="w-full bg-primary-600 text-white py-2 px-4 rounded-md hover:bg-primary-700 disabled:opacity-50"
        >
          {saving ? 'Saving…' : 'Save Changes'}
        </button>
      </form>

      {/* Data & Privacy */}
      <div className="bg-white rounded-lg shadow p-6 space-y-4">
        <h2 className="text-lg font-semibold text-gray-900">Data & Privacy</h2>

        <button
          onClick={handleExport}
          disabled={exporting}
          className="w-full text-left px-4 py-3 border border-gray-200 rounded-md hover:bg-gray-50 flex justify-between items-center"
        >
          <div>
            <p className="font-medium text-gray-900">Export my data</p>
            <p className="text-sm text-gray-500">Download all your personal data as JSON</p>
          </div>
          <span className="text-primary-600 text-sm">{exporting ? 'Exporting…' : 'Download'}</span>
        </button>

        <button
          onClick={() => setShowDeleteConfirm(true)}
          className="w-full text-left px-4 py-3 border border-red-200 rounded-md hover:bg-red-50 flex justify-between items-center"
        >
          <div>
            <p className="font-medium text-red-700">Delete my account</p>
            <p className="text-sm text-red-500">Permanently anonymize all personal data</p>
          </div>
          <span className="text-red-600 text-sm">Delete</span>
        </button>

        {showDeleteConfirm && (
          <div className="bg-red-50 border border-red-200 rounded-md p-4">
            <p className="text-sm text-red-700 mb-3">
              This action is irreversible. Your account will be deactivated and all personal data anonymized.
            </p>
            <div className="flex gap-2">
              <button
                onClick={handleDelete}
                disabled={deleting}
                className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 disabled:opacity-50 text-sm"
              >
                {deleting ? 'Deleting…' : 'Yes, delete my account'}
              </button>
              <button
                onClick={() => setShowDeleteConfirm(false)}
                className="px-4 py-2 bg-gray-200 text-gray-700 rounded-md hover:bg-gray-300 text-sm"
              >
                Cancel
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
