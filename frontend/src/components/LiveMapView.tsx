/**
 * LiveMapView — Leaflet-free live driver tracking map using Canvas + OSM tile overlay.
 * Uses STOMP WebSocket to receive driver location updates in real time.
 * Falls back to polling GET /api/drivers/orders/{orderId}/location every 10s when WS disabled.
 *
 * Shows:
 *  • Static background tile via OpenStreetMap embed iframe
 *  • Animated pulsing driver marker overlaid on top using CSS
 *  • Distance badge and accuracy ring
 */

import { useState, useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import api from '../services/api';

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

function latLngToPercent(
  lat: number,
  lng: number,
  minLat: number,
  maxLat: number,
  minLng: number,
  maxLng: number,
) {
  const x = ((lng - minLng) / (maxLng - minLng)) * 100;
  const y = ((maxLat - lat) / (maxLat - minLat)) * 100;
  return { x, y };
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
}: LiveMapViewProps) {
  const [driverPos, setDriverPos] = useState<{ lat: number; lng: number } | null>(
    initDriverLat != null && initDriverLng != null
      ? { lat: initDriverLat, lng: initDriverLng }
      : null,
  );
  const clientRef = useRef<Client | null>(null);
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // ── Poll REST fallback ────────────────────────────────────────────
  const pollLocation = async () => {
    try {
      const response = await api.get<any, any>(`/drivers/orders/${orderId}/location`);
      const data = response?.data ?? response;
      if (data && data.lat != null && data.lng != null) {
        setDriverPos({ lat: Number(data.lat), lng: Number(data.lng) });
      }
    } catch {
      // silently ignore
    }
  };

  useEffect(() => {
    if (!orderId) return;

    if (USE_WEBSOCKET) {
      // ── STOMP WebSocket ─────────────────────────────────────────
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
                setDriverPos({ lat: payload.lat, lng: payload.lng });
              }
            } catch { /* ignore */ }
          });
        };
        client.activate();
        clientRef.current = client;
      } catch { /* ignore */ }

      return () => {
        clientRef.current?.deactivate();
        clientRef.current = null;
      };
    } else {
      // ── REST polling ────────────────────────────────────────────
      pollLocation();
      pollRef.current = setInterval(pollLocation, 10_000);
      return () => {
        if (pollRef.current) clearInterval(pollRef.current);
      };
    }
  }, [orderId]);

  // ── Bounding box ──────────────────────────────────────────────────
  const allLats = [vendorLat, deliveryLat, driverPos?.lat].filter((v) => v != null) as number[];
  const allLngs = [vendorLng, deliveryLng, driverPos?.lng].filter((v) => v != null) as number[];

  const hasCoords = allLats.length >= 2;

  // Compute iframe embed bbox
  const minLat = hasCoords ? Math.min(...allLats) - 0.008 : (deliveryLat ?? 0) - 0.01;
  const maxLat = hasCoords ? Math.max(...allLats) + 0.008 : (deliveryLat ?? 0) + 0.01;
  const minLng = hasCoords ? Math.min(...allLngs) - 0.008 : (deliveryLng ?? 0) - 0.01;
  const maxLng = hasCoords ? Math.max(...allLngs) + 0.008 : (deliveryLng ?? 0) + 0.01;

  const mapUrl = `https://www.openstreetmap.org/export/embed.html?bbox=${minLng},${minLat},${maxLng},${maxLat}&layer=mapnik`;

  if (!deliveryLat && !vendorLat) {
    return (
      <div
        className={`bg-gray-100 rounded-lg flex items-center justify-center text-gray-400 text-sm ${className}`}
        style={{ minHeight: '220px' }}
      >
        Map unavailable — coordinates not set
      </div>
    );
  }

  // ── Driver marker CSS position ────────────────────────────────────
  const driverMarker =
    driverPos != null
      ? latLngToPercent(driverPos.lat, driverPos.lng, minLat, maxLat, minLng, maxLng)
      : null;

  const deliveryMarker =
    deliveryLat != null && deliveryLng != null
      ? latLngToPercent(deliveryLat, deliveryLng, minLat, maxLat, minLng, maxLng)
      : null;

  // Distance badge
  const distanceKm =
    driverPos != null && deliveryLat != null && deliveryLng != null
      ? haversineKm(driverPos.lat, driverPos.lng, deliveryLat, deliveryLng)
      : null;

  return (
    <div className={`rounded-lg overflow-hidden border border-gray-200 ${className}`}>
      {/* Live status bar */}
      {driverPos && (
        <div className="flex items-center justify-between px-3 py-1.5 bg-green-50 border-b border-green-100">
          <span className="flex items-center gap-1.5 text-xs font-medium text-green-700">
            <span className="relative flex h-2.5 w-2.5">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75" />
              <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-green-500" />
            </span>
            Driver location live
          </span>
          {distanceKm != null && (
            <span className="text-xs text-green-700 font-semibold">
              {distanceKm < 1
                ? `${Math.round(distanceKm * 1000)} m away`
                : `${distanceKm.toFixed(1)} km away`}
            </span>
          )}
        </div>
      )}

      {/* Map container with overlaid markers */}
      <div className="relative" style={{ paddingBottom: '56.25%', minHeight: '200px' }}>
        <iframe
          src={mapUrl}
          className="absolute inset-0 w-full h-full"
          style={{ border: 0 }}
          allowFullScreen
          loading="lazy"
          referrerPolicy="no-referrer-when-downgrade"
          title="Live delivery map"
        />

        {/* Delivery destination marker */}
        {deliveryMarker && (
          <div
            className="absolute z-10 pointer-events-none"
            style={{
              left: `calc(${deliveryMarker.x.toFixed(2)}% - 10px)`,
              top: `calc(${deliveryMarker.y.toFixed(2)}% - 20px)`,
            }}
          >
            <svg width="20" height="28" viewBox="0 0 20 28" fill="none">
              <path d="M10 0C4.48 0 0 4.48 0 10C0 17.5 10 28 10 28C10 28 20 17.5 20 10C20 4.48 15.52 0 10 0Z" fill="#ef4444" />
              <circle cx="10" cy="10" r="4" fill="white" />
            </svg>
          </div>
        )}

        {/* Live driver marker */}
        {driverMarker && (
          <div
            className="absolute z-20 pointer-events-none"
            style={{
              left: `calc(${driverMarker.x.toFixed(2)}% - 14px)`,
              top: `calc(${driverMarker.y.toFixed(2)}% - 14px)`,
            }}
          >
            {/* Pulsing ring */}
            <span className="absolute inset-0 rounded-full bg-blue-400 opacity-40 animate-ping" />
            {/* Driver dot */}
            <div className="relative w-7 h-7 rounded-full bg-blue-600 border-2 border-white shadow-lg flex items-center justify-center">
              <svg className="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 24 24">
                <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5S10.62 6.5 12 6.5s2.5 1.12 2.5 2.5S13.38 11.5 12 11.5z" />
              </svg>
            </div>
          </div>
        )}

        {/* No driver yet overlay */}
        {!driverPos && (
          <div className="absolute bottom-3 left-3 bg-white bg-opacity-90 rounded-md px-2.5 py-1 text-xs text-gray-500 shadow">
            Waiting for driver location…
          </div>
        )}
      </div>

      {vendorName && (
        <div className="px-3 py-2 bg-white border-t border-gray-100 text-xs text-gray-500 flex items-center gap-1">
          <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M5.05 4.05a7 7 0 119.9 9.9L10 18.9l-4.95-4.95a7 7 0 010-9.9zM10 11a2 2 0 100-4 2 2 0 000 4z" clipRule="evenodd" />
          </svg>
          Delivering from {vendorName}
        </div>
      )}
    </div>
  );
}
