/**
 * MapView — displays a static or embedded map for delivery tracking.
 * Uses OpenStreetMap embed (no API key required) with optional
 * vendor and delivery address markers.
 *
 * Phase 3.6 Maps frontend integration.
 */

import { useMemo } from 'react';

interface MapViewProps {
  vendorLat?: number;
  vendorLng?: number;
  deliveryLat?: number;
  deliveryLng?: number;
  vendorName?: string;
  className?: string;
}

export default function MapView({
  vendorLat,
  vendorLng,
  deliveryLat,
  deliveryLng,
  vendorName,
  className = '',
}: MapViewProps) {
  const vLng = vendorLng ?? deliveryLng;

  const mapUrl = useMemo(() => {
    // If we have both vendor and delivery coords, show a bounding box
    if (vendorLat && vLng && deliveryLat && deliveryLng) {
      // OpenStreetMap embed with markers layer
      return `https://www.openstreetmap.org/export/embed.html?bbox=${Math.min(vLng, deliveryLng) - 0.01},${Math.min(vendorLat, deliveryLat) - 0.01},${Math.max(vLng, deliveryLng) + 0.01},${Math.max(vendorLat, deliveryLat) + 0.01}&layer=mapnik&marker=${deliveryLat},${deliveryLng}`;
    }

    // Single point (delivery address only)
    if (deliveryLat && deliveryLng) {
      return `https://www.openstreetmap.org/export/embed.html?bbox=${deliveryLng - 0.01},${deliveryLat - 0.01},${deliveryLng + 0.01},${deliveryLat + 0.01}&layer=mapnik&marker=${deliveryLat},${deliveryLng}`;
    }

    // Default: show a generic location
    return null;
  }, [vendorLat, vLng, deliveryLat, deliveryLng]);

  if (!mapUrl) {
    return (
      <div className={`bg-gray-100 rounded-lg flex items-center justify-center text-gray-400 text-sm ${className}`}
        style={{ minHeight: '200px' }}
      >
        <div className="text-center">
          <svg className="w-8 h-8 mx-auto mb-2 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
              d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
              d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
          Map unavailable — coordinates not set
        </div>
      </div>
    );
  }

  return (
    <div className={`rounded-lg overflow-hidden border border-gray-200 ${className}`}>
      <div className="relative" style={{ paddingBottom: '56.25%', minHeight: '200px' }}>
        <iframe
          src={mapUrl}
          className="absolute inset-0 w-full h-full"
          style={{ border: 0 }}
          allowFullScreen
          loading="lazy"
          referrerPolicy="no-referrer-when-downgrade"
          title="Delivery map"
        />
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
