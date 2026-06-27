import { LocationPoint, AppSystemSettings } from '../types';

// Helper to calculate mock distance in KM
export function calculateDistance(p1: LocationPoint, p2: LocationPoint): number {
  if (!p1 || !p2) return 0;
  
  // Detect if coordinates are SVG grid percentages (0 to 100) or real coordinates
  const isP1Mock = Math.abs(p1.lat) > 0 && Math.abs(p1.lat) <= 100 && Math.abs(p1.lng) > 0 && Math.abs(p1.lng) <= 100;
  const isP2Mock = Math.abs(p2.lat) > 0 && Math.abs(p2.lat) <= 100 && Math.abs(p2.lng) > 0 && Math.abs(p2.lng) <= 100;
  
  // If both are mock coordinates, keep the old mock math
  if (isP1Mock && isP2Mock && (p1.lat > 5 || p2.lat > 5)) {
    const dx = p1.lng - p2.lng;
    const dy = p1.lat - p2.lat;
    const distanceUnits = Math.sqrt(dx * dx + dy * dy);
    const km = parseFloat((distanceUnits * 0.15).toFixed(1));
    return km < 1 ? 1.0 : km;
  }
  
  // High-precision Haversine formula for real Earth coordinates
  const lat1 = p1.lat;
  const lon1 = p1.lng;
  const lat2 = p2.lat;
  const lon2 = p2.lng;

  const R = 6371; // Earth radius in km
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2);
    
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  const km = parseFloat((R * c).toFixed(1));
  return km < 0.5 ? 0.5 : km;
}

// Helper to calculate suggested price
export function calculateSuggestedPrice(distanceKm: number, settings: AppSystemSettings): number {
  const rawPrice = distanceKm * settings.pricePerKm;
  return Math.max(rawPrice, settings.minRideValue);
}
