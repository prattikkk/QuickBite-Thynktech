/**
 * Vendor Profile — create / edit restaurant details
 */

import { useState, useEffect, FormEvent } from 'react';
import { vendorService } from '../services';
import { VendorDTO, VendorCreateRequest, VendorUpdateRequest } from '../types';
import { useToastStore } from '../store';

interface Props {
  vendor: VendorDTO | null;
  onProfileUpdated: (vendor: VendorDTO) => void;
}

export default function VendorProfile({ vendor, onProfileUpdated }: Props) {
  const { success, error: showError } = useToastStore();
  const [saving, setSaving] = useState(false);

  const [form, setForm] = useState({
    name: '',
    description: '',
    address: '',
    openHours: '',
    deliveryRadiusKm: '',
  });

  useEffect(() => {
    if (vendor) {
      setForm({
        name: vendor.name || '',
        description: vendor.description || '',
        address: vendor.address || '',
        openHours: vendor.openHours
          ? Object.entries(vendor.openHours)
              .map(([day, hours]) => `${day}: ${hours}`)
              .join('\n')
          : '',
        deliveryRadiusKm: vendor.deliveryRadiusKm != null ? String(vendor.deliveryRadiusKm) : '',
      });
    }
  }, [vendor]);

  const parseOpenHours = (text: string): Record<string, string> => {
    const result: Record<string, string> = {};
    text
      .split('\n')
      .filter((l) => l.trim())
      .forEach((line) => {
        const [day, ...rest] = line.split(':');
        if (day && rest.length) {
          result[day.trim()] = rest.join(':').trim();
        }
      });
    return result;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!form.name.trim()) {
      showError('Restaurant name is required');
      return;
    }

    setSaving(true);
    try {
      const openHours = form.openHours.trim() ? parseOpenHours(form.openHours) : undefined;

      if (vendor) {
        // Update existing
        const data: VendorUpdateRequest = {
          name: form.name,
          description: form.description || undefined,
          address: form.address || undefined,
          openHours,
          deliveryRadiusKm: form.deliveryRadiusKm ? Number(form.deliveryRadiusKm) : undefined,
        };
        const updated = await vendorService.updateVendorProfile(data);
        success('Restaurant updated!');
        onProfileUpdated(updated);
      } else {
        // Create new
        const data: VendorCreateRequest = {
          name: form.name,
          description: form.description || undefined,
          address: form.address || undefined,
          openHours,
          deliveryRadiusKm: form.deliveryRadiusKm ? Number(form.deliveryRadiusKm) : undefined,
        };
        const created = await vendorService.createVendorProfile(data);
        success('Restaurant created!');
        onProfileUpdated(created);
      }
    } catch (err: any) {
      showError(err.message || 'Failed to save profile');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-2xl">
      <h2 className="text-xl font-bold text-gray-900 mb-6">
        {vendor ? 'Edit Restaurant Profile' : 'Create Your Restaurant'}
      </h2>

      {!vendor && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
          <p className="text-blue-800 text-sm">
            Welcome! Set up your restaurant profile so customers can find and order from you.
          </p>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Restaurant Name <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
            placeholder="e.g. Tasty Burger Joint"
            required
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
          <textarea
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
            rows={3}
            placeholder="Tell customers about your restaurant, specialties, etc."
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Address</label>
          <input
            type="text"
            value={form.address}
            onChange={(e) => setForm({ ...form, address: e.target.value })}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
            placeholder="123 Main St, City"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Opening Hours</label>
          <textarea
            value={form.openHours}
            onChange={(e) => setForm({ ...form, openHours: e.target.value })}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 font-mono text-sm"
            rows={4}
            placeholder={`Mon: 9:00 AM - 10:00 PM\nTue: 9:00 AM - 10:00 PM\nWed: 9:00 AM - 10:00 PM`}
          />
          <p className="text-xs text-gray-400 mt-1">One line per day, format: Day: Hours</p>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Delivery Radius (km)</label>
          <input
            type="number"
            min="0"
            max="100"
            step="0.1"
            value={form.deliveryRadiusKm}
            onChange={(e) => setForm({ ...form, deliveryRadiusKm: e.target.value })}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
            placeholder="e.g. 5 (leave blank for unlimited)"
          />
          <p className="text-xs text-gray-400 mt-1">Maximum distance you deliver to from your restaurant</p>
        </div>

        {vendor && (
          <div className="bg-gray-50 rounded-lg p-4 text-sm text-gray-600">
            <p>
              <strong>Rating:</strong> {vendor.rating ?? 'N/A'} &nbsp;|&nbsp;{' '}
              <strong>Menu Items:</strong> {vendor.menuItemCount} &nbsp;|&nbsp;{' '}
              <strong>Status:</strong>{' '}
              <span className={vendor.active ? 'text-green-600' : 'text-red-600'}>
                {vendor.active ? 'Active' : 'Inactive'}
              </span>
              {vendor.deliveryRadiusKm != null && (
                <> &nbsp;|&nbsp; <strong>Delivery Radius:</strong> {vendor.deliveryRadiusKm} km</>
              )}
            </p>
          </div>
        )}

        <button
          type="submit"
          disabled={saving}
          className="w-full px-4 py-3 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 font-semibold text-lg"
        >
          {saving ? 'Saving...' : vendor ? 'Update Restaurant' : 'Create Restaurant'}
        </button>
      </form>
    </div>
  );
}
