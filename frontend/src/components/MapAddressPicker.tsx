/**
 * MapAddressPicker — Interactive Mapbox map for picking a location.
 *
 * The user drags the map (or taps) to move a pin, and the address is
 * reverse-geocoded using Mapbox Geocoding API. The result is passed
 * back via onLocationSelect callback.
 *
 * Usage:
 *   <MapAddressPicker
 *     onLocationSelect={({ lat, lng, line1, city, state, postal, country }) => { ... }}
 *     initialLat={19.076}
 *     initialLng={72.8777}
 *   />
 */

import { useState, useEffect, useRef, useCallback } from 'react';
import mapboxgl from 'mapbox-gl';
import 'mapbox-gl/dist/mapbox-gl.css';

const MAPBOX_TOKEN = import.meta.env.VITE_MAPBOX_TOKEN || '';
mapboxgl.accessToken = MAPBOX_TOKEN;

export interface PickedLocation {
  lat: number;
  lng: number;
  line1: string;
  line2?: string;
  city: string;
  state: string;
  postal: string;
  country: string;
  fullAddress: string;
}

interface Props {
  /** Called whenever the user drops the pin on a new location */
  onLocationSelect: (loc: PickedLocation) => void;
  /** Initial latitude (defaults to Mumbai) */
  initialLat?: number;
  /** Initial longitude */
  initialLng?: number;
  /** Map height */
  height?: string;
  /** Show the search input */
  showSearch?: boolean;
  /** Label above the map */
  label?: string;
}

export default function MapAddressPicker({
  onLocationSelect,
  initialLat = 19.076,
  initialLng = 72.8777,
  height = '300px',
  showSearch = true,
  label = 'Pin your location on the map',
}: Props) {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<mapboxgl.Map | null>(null);
  const markerRef = useRef<mapboxgl.Marker | null>(null);
  const [geocoding, setGeocoding] = useState(false);
  const [currentAddress, setCurrentAddress] = useState<string>('');
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<any[]>([]);
  const [searching, setSearching] = useState(false);
  const searchTimeout = useRef<ReturnType<typeof setTimeout> | null>(null);
  const geocodeAbort = useRef<AbortController | null>(null);

  // Try to get user's current location
  const [userLocated, setUserLocated] = useState(false);

  // ── Initialize map ──────────────────────────────────────────────
  useEffect(() => {
    if (!mapContainerRef.current || !MAPBOX_TOKEN) return;

    const map = new mapboxgl.Map({
      container: mapContainerRef.current,
      style: 'mapbox://styles/mapbox/streets-v12',
      center: [initialLng, initialLat],
      zoom: 14,
      attributionControl: false,
    });

    map.addControl(new mapboxgl.NavigationControl({ showCompass: false }), 'top-right');

    // Geolocate control — user can tap to center on their GPS
    const geolocate = new mapboxgl.GeolocateControl({
      positionOptions: { enableHighAccuracy: true },
      trackUserLocation: false,
      showUserHeading: false,
    });
    map.addControl(geolocate, 'top-left');

    // Create the draggable pin marker
    const markerEl = document.createElement('div');
    markerEl.innerHTML = `
      <div style="display:flex;flex-direction:column;align-items:center;cursor:grab;">
        <div style="font-size:36px;line-height:1;filter:drop-shadow(0 2px 4px rgba(0,0,0,0.3));
          transition:transform 0.15s;">📍</div>
        <div style="width:2px;height:8px;background:#ef4444;margin-top:-4px;"></div>
      </div>
    `;

    const marker = new mapboxgl.Marker({
      element: markerEl,
      draggable: true,
      anchor: 'bottom',
    })
      .setLngLat([initialLng, initialLat])
      .addTo(map);

    // On drag end, reverse-geocode the new location
    marker.on('dragend', () => {
      const { lng, lat } = marker.getLngLat();
      reverseGeocode(lat, lng);
    });

    // On map click, move marker there
    map.on('click', (e) => {
      marker.setLngLat(e.lngLat);
      reverseGeocode(e.lngLat.lat, e.lngLat.lng);
    });

    // Auto-locate on load
    geolocate.on('geolocate', (e: any) => {
      if (!userLocated) {
        const lat = e.coords.latitude;
        const lng = e.coords.longitude;
        marker.setLngLat([lng, lat]);
        map.flyTo({ center: [lng, lat], zoom: 16, duration: 1000 });
        reverseGeocode(lat, lng);
        setUserLocated(true);
      }
    });

    mapRef.current = map;
    markerRef.current = marker;

    // Initial reverse geocode (after short delay so map loads)
    setTimeout(() => {
      map.once('idle', () => {
        geolocate.trigger(); // try to auto-locate
      });
    }, 500);

    // Reverse geocode initial position as fallback
    reverseGeocode(initialLat, initialLng);

    return () => {
      map.remove();
      mapRef.current = null;
      markerRef.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ── Reverse Geocode ─────────────────────────────────────────────
  const reverseGeocode = useCallback(
    async (lat: number, lng: number) => {
      if (!MAPBOX_TOKEN) return;

      // Cancel any in-flight request
      if (geocodeAbort.current) geocodeAbort.current.abort();
      const controller = new AbortController();
      geocodeAbort.current = controller;

      setGeocoding(true);
      try {
        const res = await fetch(
          `https://api.mapbox.com/geocoding/v5/mapbox.places/${lng},${lat}.json?access_token=${MAPBOX_TOKEN}&types=address,place,locality,neighborhood,poi&limit=1`,
          { signal: controller.signal }
        );
        const data = await res.json();

        if (data.features && data.features.length > 0) {
          const feature = data.features[0];
          const parsed = parseMapboxFeature(feature, lat, lng);
          setCurrentAddress(parsed.fullAddress);
          onLocationSelect(parsed);
        } else {
          // No results — still pass coordinates
          const fallback: PickedLocation = {
            lat,
            lng,
            line1: `${lat.toFixed(6)}, ${lng.toFixed(6)}`,
            city: '',
            state: '',
            postal: '',
            country: '',
            fullAddress: `${lat.toFixed(6)}, ${lng.toFixed(6)}`,
          };
          setCurrentAddress(fallback.fullAddress);
          onLocationSelect(fallback);
        }
      } catch (err: any) {
        if (err.name !== 'AbortError') {
          console.warn('Reverse geocode failed:', err);
        }
      } finally {
        setGeocoding(false);
      }
    },
    [onLocationSelect]
  );

  // ── Forward Search (typeahead) ──────────────────────────────────
  const handleSearchChange = (value: string) => {
    setSearchQuery(value);
    if (searchTimeout.current) clearTimeout(searchTimeout.current);

    if (!value.trim()) {
      setSearchResults([]);
      return;
    }

    searchTimeout.current = setTimeout(async () => {
      if (!MAPBOX_TOKEN) return;
      setSearching(true);
      try {
        // Use current marker position for proximity bias
        const marker = markerRef.current;
        const proximityParam = marker
          ? `&proximity=${marker.getLngLat().lng},${marker.getLngLat().lat}`
          : '';
        const res = await fetch(
          `https://api.mapbox.com/geocoding/v5/mapbox.places/${encodeURIComponent(value)}.json?access_token=${MAPBOX_TOKEN}&limit=5&types=address,place,locality,neighborhood,poi${proximityParam}`
        );
        const data = await res.json();
        setSearchResults(data.features || []);
      } catch {
        setSearchResults([]);
      } finally {
        setSearching(false);
      }
    }, 400);
  };

  const handleSelectResult = (feature: any) => {
    const [lng, lat] = feature.center;
    if (markerRef.current && mapRef.current) {
      markerRef.current.setLngLat([lng, lat]);
      mapRef.current.flyTo({ center: [lng, lat], zoom: 16, duration: 1000 });
    }
    setSearchQuery(feature.place_name);
    setSearchResults([]);
    reverseGeocode(lat, lng);
  };

  // ── Parse Mapbox feature into PickedLocation ────────────────────
  function parseMapboxFeature(feature: any, lat: number, lng: number): PickedLocation {
    const context = feature.context || [];
    const getCtx = (type: string) =>
      context.find((c: any) => c.id.startsWith(type))?.text || '';

    // Street address or POI name
    const line1 = feature.text
      ? feature.address
        ? `${feature.address} ${feature.text}`
        : feature.text
      : feature.place_name?.split(',')[0] || '';

    const city =
      getCtx('place') || getCtx('locality') || getCtx('district') || '';
    const state = getCtx('region') || '';
    const postal = getCtx('postcode') || '';
    const country = getCtx('country') || '';

    return {
      lat,
      lng,
      line1: line1.trim(),
      line2: '',
      city: city.trim(),
      state: state.trim(),
      postal: postal.trim(),
      country: country.trim() || 'IN',
      fullAddress: feature.place_name || line1,
    };
  }

  if (!MAPBOX_TOKEN) {
    return (
      <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 text-sm text-yellow-800">
        Map unavailable — Mapbox token not configured.
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {/* Label */}
      <div className="flex items-center gap-2">
        <span className="text-sm font-medium text-gray-700">📍 {label}</span>
        {geocoding && (
          <span className="text-xs text-gray-400 animate-pulse">Locating…</span>
        )}
      </div>

      {/* Search box */}
      {showSearch && (
        <div className="relative">
          <div className="relative">
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => handleSearchChange(e.target.value)}
              placeholder="Search for a place or address…"
              className="w-full pl-9 pr-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 text-sm"
            />
            <svg
              className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
              />
            </svg>
            {searching && (
              <div className="absolute right-3 top-1/2 -translate-y-1/2">
                <div className="w-4 h-4 border-2 border-primary-500 border-t-transparent rounded-full animate-spin" />
              </div>
            )}
          </div>

          {/* Dropdown results */}
          {searchResults.length > 0 && (
            <div className="absolute z-50 w-full mt-1 bg-white rounded-lg shadow-lg border border-gray-200 max-h-48 overflow-y-auto">
              {searchResults.map((r: any) => (
                <button
                  key={r.id}
                  type="button"
                  onClick={() => handleSelectResult(r)}
                  className="w-full text-left px-3 py-2 hover:bg-primary-50 text-sm border-b last:border-b-0 border-gray-100 transition-colors"
                >
                  <span className="font-medium text-gray-800">
                    {r.text || r.place_name?.split(',')[0]}
                  </span>
                  <br />
                  <span className="text-xs text-gray-500">{r.place_name}</span>
                </button>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Map */}
      <div className="relative rounded-xl overflow-hidden border border-gray-200 shadow-sm">
        <div ref={mapContainerRef} style={{ height, width: '100%' }} />

        {/* Floating instruction */}
        <div className="absolute bottom-3 left-1/2 -translate-x-1/2 bg-white/90 backdrop-blur-sm rounded-full px-4 py-1.5 shadow-md text-xs text-gray-600 pointer-events-none">
          Drag the pin or tap to set location
        </div>
      </div>

      {/* Current address display */}
      {currentAddress && (
        <div className="flex items-start gap-2 p-3 bg-green-50 border border-green-200 rounded-lg">
          <span className="text-green-600 mt-0.5">✓</span>
          <div>
            <p className="text-sm font-medium text-green-800">Selected Location</p>
            <p className="text-sm text-green-700">{currentAddress}</p>
          </div>
        </div>
      )}
    </div>
  );
}
