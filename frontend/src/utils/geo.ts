/**
 * Shared geospatial utility functions.
 * Centralizes Haversine, bearing, Mapbox Directions API route fetching,
 * and geocode caching to avoid duplication across components.
 */

const MAPBOX_TOKEN = import.meta.env.VITE_MAPBOX_TOKEN || '';

// ── Haversine distance (km) ────────────────────────────────────────

export function haversineKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

// ── Bearing (degrees) ──────────────────────────────────────────────

export function bearingTo(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const toRad = (d: number) => (d * Math.PI) / 180;
  const dLng = toRad(lng2 - lng1);
  const y = Math.sin(dLng) * Math.cos(toRad(lat2));
  const x =
    Math.cos(toRad(lat1)) * Math.sin(toRad(lat2)) -
    Math.sin(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.cos(dLng);
  return ((Math.atan2(y, x) * 180) / Math.PI + 360) % 360;
}

// ── Format distance for display ────────────────────────────────────

export function formatDistance(km: number): string {
  if (km < 1) return `${Math.round(km * 1000)} m`;
  return `${km.toFixed(1)} km`;
}

// ── Mapbox Directions API ──────────────────────────────────────────

export interface RouteResult {
  distanceKm: number;
  durationMinutes: number;
  /** GeoJSON coordinates array [[lng, lat], ...] */
  coordinates: [number, number][];
  /** Turn-by-turn step instructions */
  steps: RouteStep[];
}

export interface RouteStep {
  instruction: string;
  distanceMeters: number;
  durationSeconds: number;
  maneuver: { type: string; modifier?: string; location: [number, number] };
}

// Route cache: key → { data, timestamp }
const routeCache = new Map<string, { data: RouteResult; ts: number }>();
const ROUTE_CACHE_TTL = 60_000; // 1 minute

function routeCacheKey(coords: [number, number][]): string {
  return coords.map(([lng, lat]) => `${lng.toFixed(5)},${lat.toFixed(5)}`).join(';');
}

/**
 * Fetch driving route from Mapbox Directions API.
 * Supports 2+ waypoints. Returns null on failure.
 * Results are cached for 1 minute.
 */
export async function fetchRoute(
  waypoints: [number, number][], // [[lng, lat], ...]
  options: { steps?: boolean; overview?: 'full' | 'simplified' | 'false'; signal?: AbortSignal } = {},
): Promise<RouteResult | null> {
  if (!MAPBOX_TOKEN || waypoints.length < 2) return null;

  const { steps = false, overview = 'full', signal } = options;
  const key = routeCacheKey(waypoints);
  const cached = routeCache.get(key);
  if (cached && Date.now() - cached.ts < ROUTE_CACHE_TTL) return cached.data;

  try {
    const coordsStr = waypoints.map(([lng, lat]) => `${lng},${lat}`).join(';');
    const url = `https://api.mapbox.com/directions/v5/mapbox/driving/${coordsStr}?access_token=${MAPBOX_TOKEN}&geometries=geojson&overview=${overview}${steps ? '&steps=true' : ''}`;

    const res = await fetch(url, { signal });
    const data = await res.json();

    if (data.routes && data.routes.length > 0) {
      const route = data.routes[0];
      const result: RouteResult = {
        distanceKm: route.distance / 1000,
        durationMinutes: Math.ceil(route.duration / 60),
        coordinates: route.geometry?.coordinates || [],
        steps: [],
      };

      // Parse steps if available
      if (steps && route.legs) {
        for (const leg of route.legs) {
          if (leg.steps) {
            for (const step of leg.steps) {
              result.steps.push({
                instruction: step.maneuver?.instruction || '',
                distanceMeters: step.distance || 0,
                durationSeconds: step.duration || 0,
                maneuver: {
                  type: step.maneuver?.type || '',
                  modifier: step.maneuver?.modifier,
                  location: step.maneuver?.location || [0, 0],
                },
              });
            }
          }
        }
      }

      routeCache.set(key, { data: result, ts: Date.now() });
      return result;
    }
  } catch (err: any) {
    if (err.name !== 'AbortError') {
      console.warn('[geo] Directions API error:', err.message);
    }
  }
  return null;
}

// ── Geocode cache ──────────────────────────────────────────────────

const geocodeCache = new Map<string, { lat: number; lng: number; address: string; ts: number }>();
const GEO_CACHE_TTL = 300_000; // 5 minutes

export interface GeocodeResult {
  lat: number;
  lng: number;
  address: string;
}

/**
 * Forward geocode an address string using Mapbox.
 * Results are cached for 5 minutes.
 */
export async function geocodeAddress(query: string): Promise<GeocodeResult | null> {
  if (!MAPBOX_TOKEN || !query.trim()) return null;

  const key = query.trim().toLowerCase();
  const cached = geocodeCache.get(key);
  if (cached && Date.now() - cached.ts < GEO_CACHE_TTL) return cached;

  try {
    const res = await fetch(
      `https://api.mapbox.com/geocoding/v5/mapbox.places/${encodeURIComponent(query)}.json?access_token=${MAPBOX_TOKEN}&limit=1`,
    );
    const data = await res.json();
    if (data.features && data.features.length > 0) {
      const [lng, lat] = data.features[0].center;
      const result = { lat, lng, address: data.features[0].place_name || query };
      geocodeCache.set(key, { ...result, ts: Date.now() });
      return result;
    }
  } catch (err) {
    console.warn('[geo] Geocode error:', err);
  }
  return null;
}

/**
 * Reverse geocode lat/lng using Mapbox.
 */
export async function reverseGeocode(
  lat: number,
  lng: number,
  signal?: AbortSignal,
): Promise<GeocodeResult | null> {
  if (!MAPBOX_TOKEN) return null;

  const key = `${lat.toFixed(5)},${lng.toFixed(5)}`;
  const cached = geocodeCache.get(key);
  if (cached && Date.now() - cached.ts < GEO_CACHE_TTL) return cached;

  try {
    const res = await fetch(
      `https://api.mapbox.com/geocoding/v5/mapbox.places/${lng},${lat}.json?access_token=${MAPBOX_TOKEN}&types=address,place,locality,neighborhood,poi&limit=1`,
      { signal },
    );
    const data = await res.json();
    if (data.features && data.features.length > 0) {
      const result = { lat, lng, address: data.features[0].place_name || `${lat}, ${lng}` };
      geocodeCache.set(key, { ...result, ts: Date.now() });
      return result;
    }
  } catch (err: any) {
    if (err.name !== 'AbortError') console.warn('[geo] Reverse geocode error:', err);
  }
  return null;
}

/**
 * Get user's approximate location from IP (fallback when GPS is unavailable).
 * Uses the free ipapi.co service.
 */
export async function getLocationFromIP(): Promise<{ lat: number; lng: number } | null> {
  try {
    const res = await fetch('https://ipapi.co/json/', { signal: AbortSignal.timeout(5000) });
    const data = await res.json();
    if (data.latitude && data.longitude) {
      return { lat: data.latitude, lng: data.longitude };
    }
  } catch {
    // silently ignore
  }
  return null;
}
