import { useState, useCallback } from 'react';
import { useMap, useMapsLibrary } from '@vis.gl/react-google-maps';
import { googleMapsService } from '../services/googleMapsService';
import { MapCoordinates, MapRoute, MapError, PlaceSearchResult } from '../types';

export function useGoogleMaps() {
  const map = useMap();
  const routesLib = useMapsLibrary('routes');
  const placesLib = useMapsLibrary('places');

  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<MapError | null>(null);
  const [currentRoute, setCurrentRoute] = useState<MapRoute | null>(null);

  /**
   * Helper to handle map errors cleanly
   */
  const handleError = useCallback((err: any) => {
    console.error('Map error occurred:', err);
    let mappedError: MapError = {
      type: 'UNKNOWN',
      message: 'Ocorreu um erro inesperado no mapa.'
    };

    if (err.type) {
      mappedError = err;
    } else if (err.message && err.message.includes('permission')) {
      mappedError = {
        type: 'PERMISSION_DENIED',
        message: 'Permissão de GPS/Localização negada.'
      };
    } else if (err.message && err.message.includes('network')) {
      mappedError = {
        type: 'NO_INTERNET',
        message: 'Sem conexão com a internet.'
      };
    } else if (err.message) {
      mappedError = {
        type: 'GOOGLE_UNAVAILABLE',
        message: err.message
      };
    }

    setError(mappedError);
    setLoading(false);
    return mappedError;
  }, []);

  /**
   * Center map on coordinates with custom zoom
   */
  const centerMap = useCallback((coords: MapCoordinates, zoom = 15) => {
    if (!map) return;
    map.panTo(new google.maps.LatLng(coords.lat, coords.lng));
    map.setZoom(zoom);
  }, [map]);

  /**
   * Get user current position and center map
   */
  const findCurrentLocation = useCallback(async (): Promise<MapCoordinates | null> => {
    setLoading(true);
    setError(null);
    try {
      const coords = await googleMapsService.getCurrentLocation();
      centerMap(coords);
      setLoading(false);
      return coords;
    } catch (err) {
      handleError(err);
      return null;
    }
  }, [centerMap, handleError]);

  /**
   * Calculate route and fit camera bounds
   */
  const calculateRoute = useCallback(async (
    origin: MapCoordinates,
    destination: MapCoordinates,
    stops?: MapCoordinates[]
  ): Promise<MapRoute | null> => {
    setLoading(true);
    setError(null);
    try {
      const route = await googleMapsService.calculateRoute(origin, destination, stops);
      setCurrentRoute(route);

      // Fit bounds automatically
      if (map && route.bounds) {
        map.fitBounds(route.bounds);
      } else if (map) {
        const coords = [origin, destination, ...(stops || [])];
        googleMapsService.fitBounds(map, coords);
      }

      setLoading(false);
      return route;
    } catch (err) {
      handleError(err);
      
      // Fallback local calculation to not break business flow
      const fallback = googleMapsService.simulateLocalOfflineRoute(origin, destination);
      setCurrentRoute(fallback);
      setLoading(false);
      return fallback;
    }
  }, [map, handleError]);

  /**
   * Query places autocomplete with bounds bias
   */
  const searchPlaces = useCallback(async (query: string): Promise<PlaceSearchResult[]> => {
    setError(null);
    try {
      const bias = map ? { lat: map.getCenter()?.lat() || -22.1225, lng: map.getCenter()?.lng() || -51.3856 } : undefined;
      return await googleMapsService.searchPlaces(query, bias);
    } catch (err) {
      handleError(err);
      return [];
    }
  }, [map, handleError]);

  /**
   * Reverse geocode coords to address name
   */
  const reverseGeocode = useCallback(async (coords: MapCoordinates): Promise<PlaceSearchResult | null> => {
    setError(null);
    try {
      return await googleMapsService.reverseGeocode(coords);
    } catch (err) {
      handleError(err);
      return null;
    }
  }, [handleError]);

  /**
   * Clear current route
   */
  const clearRoute = useCallback(() => {
    setCurrentRoute(null);
    setError(null);
  }, []);

  return {
    map,
    routesReady: !!routesLib,
    placesReady: !!placesLib,
    loading,
    error,
    currentRoute,
    findCurrentLocation,
    calculateRoute,
    searchPlaces,
    reverseGeocode,
    centerMap,
    clearRoute,
    setError: (err: MapError | null) => setError(err)
  };
}
