/**
 * LiveMapView — Interactive Mapbox GL map with live driver tracking.
 * Uses STOMP WebSocket to receive driver location updates in real time.
 * Falls back to polling GET /api/drivers/orders/{orderId}/location every 10s.
 *
 * Shows:
 *  • Full Mapbox GL interactive map with smooth transitions
 *  • Animated pulsing driver marker with heading
 *  • Vendor origin marker (restaurant icon)
 *  • Delivery destination marker (red pin)
 *  • Route line between vendor → driver → destination
 *  • Distance badge (haversine km/m)
 *  • Live GPS status indicator
 */

import { useState, useEffect, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import api from '../services/api';
import mapboxgl from 'mapbox-gl';
import 'mapbox-gl/dist/mapbox-gl.css';

const MAPBOX_TOKEN = import.meta.env.VITE_MAPBOX_TOKEN || '';

mapboxgl.accessToken = MAPBOX_TOKEN;

interface LiveMapViewProps {
  orderId: string;
  vendorLat?: number;
  vendorLng?: number;
  deliveryLat?: number;
  deliveryLng?: number;
  vendorName?: string;
  /** Pre-supplied driver location from parent (optional) */
  driverLat?: number;
  driverLng?: number;
  className?: string;
  /** When true, uses driverLat/driverLng props directly (driver's own GPS) instead of polling the API */
  isDriverView?: boolean;
}

const USE_WEBSOCKET = import.meta.env.VITE_USE_WEBSOCKET === 'true';
const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';

function haversineKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

export default function LiveMapView({
  orderId,
  vendorLat,
  vendorLng,
  deliveryLat,
  deliveryLng,
  vendorName,
  driverLat: initDriverLat,
  driverLng: initDriverLng,
  className = '',
  isDriverView = false,
}: LiveMapViewProps) {
  const mapContainer = useRef<HTMLDivElement>(null);
  const mapRef = useRef<mapboxgl.Map | null>(null);
  const driverMarkerRef = useRef<mapboxgl.Marker | null>(null);
  const clientRef = useRef<Client | null>(null);
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const [driverPos, setDriverPos] = useState<{ lat: number; lng: number } | null>(
    initDriverLat != null && initDriverLng != null
      ? { lat: initDriverLat, lng: initDriverLng }
      : null,
  );
  const [mapReady, setMapReady] = useState(false);

  // Stable callback for updating driver position
  const updateDriverPos = useCallback((lat: number, lng: number) => {
    setDriverPos({ lat, lng });
  }, []);

  // ── Poll REST fallback ────────────────────────────────────────────
  const pollLocation = useCallback(async () => {
    try {
      const response = await api.get<any, any>(`/drivers/orders/${orderId}/location`);
      const data = response?.data ?? response;
      if (data && data.lat != null && data.lng != null) {
        updateDriverPos(Number(data.lat), Number(data.lng));
      }
    } catch {
      // silently ignore
    }
  }, [orderId, updateDriverPos]);

  // ── Sync driverLat/driverLng props directly in driver view mode ──
  useEffect(() => {
    if (isDriverView && initDriverLat != null && initDriverLng != null) {
      updateDriverPos(initDriverLat, initDriverLng);
    }
  }, [isDriverView, initDriverLat, initDriverLng, updateDriverPos]);

  // ── WebSocket / Polling setup (skip in driver view mode) ──────────
  useEffect(() => {
    if (!orderId || isDriverView) return;

    if (USE_WEBSOCKET) {
      try {
        const token = localStorage.getItem('quickbite_token');
        const client = new Client({
          webSocketFactory: () => new SockJS(WS_URL),
          connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
          reconnectDelay: 5000,
        });
        client.onConnect = () => {
          client.subscribe(`/topic/orders.${orderId}.location`, (msg) => {
            try {
              const payload = JSON.parse(msg.body);
              if (payload.lat != null && payload.lng != null) {
                updateDriverPos(payload.lat, payload.lng);
              }
            } catch { /* ignore */ }
          });
        };
        client.activate();
        clientRef.current = client;
      } catch { /* ignore */ }

      // Also poll as supplement
      pollLocation();
      pollRef.current = setInterval(pollLocation, 15_000);

      return () => {
        clientRef.current?.deactivate();
        clientRef.current = null;
        if (pollRef.current) clearInterval(pollRef.current);
      };
    } else {
      pollLocation();
      pollRef.current = setInterval(pollLocation, 10_000);
      return () => {
        if (pollRef.current) clearInterval(pollRef.current);
      };
    }
  }, [orderId, pollLocation, updateDriverPos]);

  // ── Initialize Mapbox map ─────────────────────────────────────────
  useEffect(() => {
    if (!mapContainer.current) return;
    if (mapRef.current) return; // already initialized

    // Determine center
    const centerLat = deliveryLat ?? vendorLat ?? 0;
    const centerLng = deliveryLng ?? vendorLng ?? 0;

    if (centerLat === 0 && centerLng === 0) return;

    const map = new mapboxgl.Map({
      container: mapContainer.current,
      style: 'mapbox://styles/mapbox/streets-v12',
      center: [centerLng, centerLat],
      zoom: 14,
      attributionControl: false,
    });

    map.addControl(new mapboxgl.AttributionControl({ compact: true }), 'bottom-left');
    map.addControl(new mapboxgl.NavigationControl({ showCompass: false }), 'top-right');

    map.on('load', () => {
      setMapReady(true);

      // ── Add vendor marker ────────────────────────────────────────
      if (vendorLat != null && vendorLng != null) {
        const vendorEl = document.createElement('div');
        vendorEl.innerHTML = `
          <div style="display:flex;align-items:center;justify-content:center;width:36px;height:36px;
            background:#f97316;border-radius:50%;border:3px solid #fff;box-shadow:0 2px 8px rgba(0,0,0,0.3);
            cursor:pointer" title="${vendorName || 'Restaurant'}">
            <span style="font-size:18px">🍽️</span>
          </div>`;
        new mapboxgl.Marker({ element: vendorEl, anchor: 'center' })
          .setLngLat([vendorLng, vendorLat])
          .setPopup(new mapboxgl.Popup({ offset: 20 }).setHTML(
            `<div style="padding:4px 8px;font-weight:600;font-size:13px">${vendorName || 'Restaurant'}</div>`
          ))
          .addTo(map);
      }

      // ── Add delivery destination marker ──────────────────────────
      if (deliveryLat != null && deliveryLng != null) {
        const destEl = document.createElement('div');
        destEl.innerHTML = `
          <div style="display:flex;flex-direction:column;align-items:center;">
            <div style="width:28px;height:28px;background:#ef4444;border-radius:50% 50% 50% 0;
              border:3px solid #fff;box-shadow:0 2px 8px rgba(0,0,0,0.3);transform:rotate(-45deg);
              display:flex;align-items:center;justify-content:center;">
              <span style="transform:rotate(45deg);font-size:12px;color:#fff;font-weight:bold">📍</span>
            </div>
            <div style="width:2px;height:6px;background:#ef4444;margin-top:-2px;"></div>
          </div>`;
        new mapboxgl.Marker({ element: destEl, anchor: 'bottom' })
          .setLngLat([deliveryLng, deliveryLat])
          .setPopup(new mapboxgl.Popup({ offset: 20 }).setHTML(
            `<div style="padding:4px 8px;font-weight:600;font-size:13px">📍 Delivery Location</div>`
          ))
          .addTo(map);
      }

      // ── Add route line source ────────────────────────────────────
      map.addSource('route', {
        type: 'geojson',
        data: {
          type: 'Feature',
          geometry: { type: 'LineString', coordinates: [] },
          properties: {},
        },
      });
      map.addLayer({
        id: 'route-line',
        type: 'line',
        source: 'route',
        layout: { 'line-join': 'round', 'line-cap': 'round' },
        paint: { 'line-color': '#3b82f6', 'line-width': 3, 'line-dasharray': [2, 2] },
      });

      // ── Fit bounds to show all points ────────────────────────────
      const bounds = new mapboxgl.LngLatBounds();
      if (vendorLat != null && vendorLng != null) bounds.extend([vendorLng, vendorLat]);
      if (deliveryLat != null && deliveryLng != null) bounds.extend([deliveryLng, deliveryLat]);
      if (!bounds.isEmpty()) {
        map.fitBounds(bounds, { padding: 60, maxZoom: 15 });
      }
    });

    mapRef.current = map;

    return () => {
      map.remove();
      mapRef.current = null;
      driverMarkerRef.current = null;
      setMapReady(false);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [deliveryLat, deliveryLng, vendorLat, vendorLng, vendorName]);

  // ── Update driver marker when driverPos changes ───────────────────
  useEffect(() => {
    if (!mapRef.current || !mapReady || !driverPos) return;
    const map = mapRef.current;

    // Create or update driver marker
    if (!driverMarkerRef.current) {
      const driverEl = document.createElement('div');
      driverEl.className = 'driver-marker-live';
      driverEl.innerHTML = `
        <div style="position:relative;display:flex;align-items:center;justify-content:center;">
          <div class="driver-pulse-ring" style="position:absolute;width:44px;height:44px;
            border-radius:50%;background:rgba(59,130,246,0.25);animation:driverPulse 2s ease-out infinite;"></div>
          <div style="width:32px;height:32px;background:#2563eb;border-radius:50%;border:3px solid #fff;
            box-shadow:0 2px 10px rgba(37,99,235,0.5);display:flex;align-items:center;justify-content:center;
            z-index:1;position:relative;">
            <span style="font-size:16px">🛵</span>
          </div>
        </div>`;

      // Add CSS animation if not already present
      if (!document.getElementById('driver-pulse-css')) {
        const style = document.createElement('style');
        style.id = 'driver-pulse-css';
        style.textContent = `
          @keyframes driverPulse {
            0% { transform: scale(0.8); opacity: 1; }
            100% { transform: scale(2.2); opacity: 0; }
          }
        `;
        document.head.appendChild(style);
      }

      driverMarkerRef.current = new mapboxgl.Marker({ element: driverEl, anchor: 'center' })
        .setLngLat([driverPos.lng, driverPos.lat])
        .addTo(map);
    } else {
      // Smooth update for marker position
      driverMarkerRef.current.setLngLat([driverPos.lng, driverPos.lat]);
    }

    // Update route line: vendor → driver → delivery
    const routeCoords: [number, number][] = [];
    if (vendorLat != null && vendorLng != null) routeCoords.push([vendorLng, vendorLat]);
    routeCoords.push([driverPos.lng, driverPos.lat]);
    if (deliveryLat != null && deliveryLng != null) routeCoords.push([deliveryLng, deliveryLat]);

    if (routeCoords.length >= 2) {
      const routeSource = map.getSource('route') as mapboxgl.GeoJSONSource | undefined;
      if (routeSource) {
        routeSource.setData({
          type: 'Feature',
          geometry: { type: 'LineString', coordinates: routeCoords },
          properties: {},
        });
      }
    }

    // Fit bounds to include driver
    const bounds = new mapboxgl.LngLatBounds();
    if (vendorLat != null && vendorLng != null) bounds.extend([vendorLng, vendorLat]);
    bounds.extend([driverPos.lng, driverPos.lat]);
    if (deliveryLat != null && deliveryLng != null) bounds.extend([deliveryLng, deliveryLat]);
    if (!bounds.isEmpty()) {
      map.fitBounds(bounds, { padding: 50, maxZoom: 16, duration: 1000 });
    }
  }, [driverPos, mapReady, vendorLat, vendorLng, deliveryLat, deliveryLng]);

  // Distance badge
  const distanceKm =
    driverPos != null && deliveryLat != null && deliveryLng != null
      ? haversineKm(driverPos.lat, driverPos.lng, deliveryLat, deliveryLng)
      : null;

  if (!deliveryLat && !vendorLat) {
    return (
      <div
        className={`bg-gray-100 dark:bg-gray-800 rounded-lg flex items-center justify-center text-gray-400 text-sm ${className}`}
        style={{ minHeight: '220px' }}
      >
        Map unavailable — coordinates not set
      </div>
    );
  }

  return (
    <div className={`rounded-xl overflow-hidden border border-gray-200 dark:border-gray-700 shadow-sm ${className}`}>
      {/* Live status bar */}
      <div className={`flex items-center justify-between px-3 py-2 ${
        driverPos ? 'bg-green-50 dark:bg-green-900/20 border-b border-green-100 dark:border-green-800' :
        'bg-gray-50 dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700'
      }`}>
        <span className={`flex items-center gap-1.5 text-xs font-medium ${
          driverPos ? 'text-green-700 dark:text-green-400' : 'text-gray-500 dark:text-gray-400'
        }`}>
          {driverPos ? (
            <>
              <span className="relative flex h-2.5 w-2.5">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75" />
                <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-green-500" />
              </span>
              {isDriverView ? 'Your location' : 'Driver location live'}
            </>
          ) : (
            <>
              <svg className="w-3.5 h-3.5 animate-spin" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
              </svg>
              {isDriverView ? 'Getting your location…' : 'Waiting for driver position…'}
            </>
          )}
        </span>
        {distanceKm != null && (
          <span className="text-xs font-semibold text-green-700 dark:text-green-400 bg-green-100 dark:bg-green-900/40 px-2 py-0.5 rounded-full">
            {distanceKm < 1
              ? `${Math.round(distanceKm * 1000)} m ${isDriverView ? 'to delivery' : 'away'}`
              : `${distanceKm.toFixed(1)} km ${isDriverView ? 'to delivery' : 'away'}`}
          </span>
        )}
      </div>

      {/* Mapbox GL container */}
      <div ref={mapContainer} style={{ height: '300px', width: '100%' }} />

      {vendorName && (
        <div className="px-3 py-2 bg-white dark:bg-gray-800 border-t border-gray-100 dark:border-gray-700 text-xs text-gray-500 dark:text-gray-400 flex items-center gap-1">
          <span>🍽️</span>
          Delivering from <span className="font-medium text-gray-700 dark:text-gray-300">{vendorName}</span>
        </div>
      )}
    </div>
  );
}
