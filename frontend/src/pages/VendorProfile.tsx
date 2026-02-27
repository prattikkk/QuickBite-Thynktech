/**
 * Vendor Profile — create / edit restaurant details
 */

import { useState, useEffect, useRef, FormEvent, useCallback } from 'react';
import { vendorService } from '../services';
import { VendorDTO, VendorCreateRequest, VendorUpdateRequest } from '../types';
import { useToastStore } from '../store';
import MapAddressPicker from '../components/MapAddressPicker';
import type { PickedLocation } from '../components/MapAddressPicker';
import mapboxgl from 'mapbox-gl';
import 'mapbox-gl/dist/mapbox-gl.css';

const MAPBOX_TOKEN = import.meta.env.VITE_MAPBOX_TOKEN || '';

mapboxgl.accessToken = MAPBOX_TOKEN;

interface Props {
  vendor: VendorDTO | null;
  onProfileUpdated: (vendor: VendorDTO) => void;
}

export default function VendorProfile({ vendor, onProfileUpdated }: Props) {
  const { success, error: showError } = useToastStore();
  const [saving, setSaving] = useState(false);
  const radiusMapContainer = useRef<HTMLDivElement>(null);
  const radiusMapRef = useRef<mapboxgl.Map | null>(null);

  const [form, setForm] = useState({
    name: '',
    description: '',
    address: '',
    openHours: '',
    deliveryRadiusKm: '',
    lat: 0,
    lng: 0,
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
        lat: vendor.lat || 0,
        lng: vendor.lng || 0,
      });
    }
  }, [vendor]);

  // ── Delivery radius Mapbox map ──────────────────────────────────
  useEffect(() => {
    if (!radiusMapContainer.current || !vendor?.lat || !vendor?.lng) return;

    // Create or update the map
    if (!radiusMapRef.current) {
      const map = new mapboxgl.Map({
        container: radiusMapContainer.current,
        style: 'mapbox://styles/mapbox/streets-v12',
        center: [vendor.lng, vendor.lat],
        zoom: 13,
        attributionControl: false,
        interactive: true,
      });

      map.addControl(new mapboxgl.NavigationControl({ showCompass: false }), 'top-right');

      // Restaurant marker
      const el = document.createElement('div');
      el.innerHTML = `<div style="display:flex;align-items:center;justify-content:center;width:36px;height:36px;
        background:#f97316;border-radius:50%;border:3px solid #fff;box-shadow:0 2px 8px rgba(0,0,0,0.3);">
        <span style="font-size:18px">🍽️</span>
      </div>`;
      new mapboxgl.Marker({ element: el, anchor: 'center' })
        .setLngLat([vendor.lng, vendor.lat])
        .addTo(map);

      map.on('load', () => {
        // Delivery radius circle
        map.addSource('delivery-radius', {
          type: 'geojson',
          data: createCircleGeoJSON(vendor.lng, vendor.lat, radiusKm()),
        });
        map.addLayer({
          id: 'delivery-radius-fill',
          type: 'fill',
          source: 'delivery-radius',
          paint: {
            'fill-color': '#3b82f6',
            'fill-opacity': 0.12,
          },
        });
        map.addLayer({
          id: 'delivery-radius-border',
          type: 'line',
          source: 'delivery-radius',
          paint: {
            'line-color': '#3b82f6',
            'line-width': 2,
            'line-dasharray': [3, 2],
          },
        });
      });

      radiusMapRef.current = map;
    }

    return () => {
      // Cleanup only on unmount
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [vendor?.lat, vendor?.lng]);

  // Update radius circle when deliveryRadiusKm changes
  useEffect(() => {
    if (!radiusMapRef.current || !vendor?.lat || !vendor?.lng) return;
    const map = radiusMapRef.current;
    if (!map.isStyleLoaded()) return;

    const source = map.getSource('delivery-radius') as mapboxgl.GeoJSONSource | undefined;
    if (source) {
      source.setData(createCircleGeoJSON(vendor.lng, vendor.lat, radiusKm()));
      // Adjust zoom to fit radius
      const km = radiusKm();
      if (km > 0) {
        const zoom = km > 20 ? 10 : km > 10 ? 11 : km > 5 ? 12 : km > 2 ? 13 : 14;
        map.flyTo({ center: [vendor.lng, vendor.lat], zoom, duration: 800 });
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [form.deliveryRadiusKm, vendor?.lat, vendor?.lng]);

  // Cleanup map on unmount
  useEffect(() => {
    return () => {
      radiusMapRef.current?.remove();
      radiusMapRef.current = null;
    };
  }, []);

  const radiusKm = () => {
    const v = parseFloat(form.deliveryRadiusKm);
    return isNaN(v) || v <= 0 ? 0 : v;
  };

  const handleLocationSelect = useCallback((loc: PickedLocation) => {
    setForm((prev) => ({
      ...prev,
      address: loc.fullAddress,
      lat: loc.lat,
      lng: loc.lng,
    }));
  }, []);

  /** Generate a GeoJSON circle polygon (64 points) */
  function createCircleGeoJSON(lng: number, lat: number, radiusKm: number): GeoJSON.FeatureCollection {
    if (radiusKm <= 0) {
      return { type: 'FeatureCollection', features: [] };
    }
    const points = 64;
    const coords: [number, number][] = [];
    const km = radiusKm;
    for (let i = 0; i <= points; i++) {
      const angle = (i / points) * 2 * Math.PI;
      const dx = km * Math.cos(angle);
      const dy = km * Math.sin(angle);
      const dlng = dx / (111.32 * Math.cos((lat * Math.PI) / 180));
      const dlat = dy / 110.574;
      coords.push([lng + dlng, lat + dlat]);
    }
    return {
      type: 'FeatureCollection',
      features: [
        {
          type: 'Feature',
          geometry: { type: 'Polygon', coordinates: [coords] },
          properties: {},
        },
      ],
    };
  }

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
          lat: form.lat || undefined,
          lng: form.lng || undefined,
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
          lat: form.lat || undefined,
          lng: form.lng || undefined,
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
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Restaurant Location <span className="text-red-500">*</span>
          </label>
          <MapAddressPicker
            label="Pin your restaurant location on the map"
            height="280px"
            initialLat={form.lat || vendor?.lat || 19.076}
            initialLng={form.lng || vendor?.lng || 72.8777}
            onLocationSelect={handleLocationSelect}
          />
          <input
            type="text"
            value={form.address}
            onChange={(e) => setForm({ ...form, address: e.target.value })}
            className="w-full mt-2 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
            placeholder="Full address (auto-filled from map or enter manually)"
          />
          <p className="text-xs text-gray-400 mt-1">
            Drag the pin on the map or search to set your restaurant's exact location
          </p>
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

        {/* Delivery radius map preview */}
        {vendor?.lat != null && vendor?.lng != null && (
          <div className="rounded-xl overflow-hidden border border-gray-200 shadow-sm">
            <div className="px-3 py-2 bg-gray-50 border-b border-gray-200 flex items-center justify-between">
              <span className="text-xs font-medium text-gray-600 flex items-center gap-1.5">
                📍 Delivery Area Preview
              </span>
              {radiusKm() > 0 && (
                <span className="text-xs font-semibold text-blue-600 bg-blue-50 px-2 py-0.5 rounded-full">
                  {radiusKm()} km radius
                </span>
              )}
            </div>
            <div ref={radiusMapContainer} style={{ height: '220px', width: '100%' }} />
          </div>
        )}

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
