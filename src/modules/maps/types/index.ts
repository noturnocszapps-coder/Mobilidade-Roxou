export interface MapCoordinates {
  lat: number;
  lng: number;
}

export interface MapRoute {
  distanceKm: number;
  durationMinutes: number;
  polyline: string;
  bounds?: google.maps.LatLngBoundsLiteral;
  trafficStatus?: 'light' | 'moderate' | 'heavy';
}

export interface PlaceSearchResult {
  placeId: string;
  name: string;
  formattedAddress: string;
  coordinates: MapCoordinates;
  bairro?: string;
}

export interface MapCacheItem {
  route: MapRoute;
  timestamp: number;
}

export type MapErrorType =
  | 'NO_INTERNET'
  | 'NO_GPS'
  | 'GOOGLE_UNAVAILABLE'
  | 'PERMISSION_DENIED'
  | 'INVALID_ORIGIN'
  | 'INVALID_DESTINATION'
  | 'QUOTA_EXCEEDED'
  | 'UNKNOWN';

export interface MapError {
  type: MapErrorType;
  message: string;
}
