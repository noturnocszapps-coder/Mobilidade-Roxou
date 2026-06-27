import { MapCoordinates, MapRoute, PlaceSearchResult, MapError } from '../types';

// Simple in-memory route cache with 5 minutes TTL
const routeCache = new Map<string, { route: MapRoute; timestamp: number }>();
const CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

export const googleMapsService = {
  /**
   * Helper to generate a cache key for origin and destination
   */
  getCacheKey(origin: MapCoordinates, destination: MapCoordinates): string {
    const origKey = `${origin.lat.toFixed(5)},${origin.lng.toFixed(5)}`;
    const destKey = `${destination.lat.toFixed(5)},${destination.lng.toFixed(5)}`;
    return `${origKey}_to_${destKey}`;
  },

  /**
   * Initialize Google Maps API or verify availability
   */
  async initializeGoogleMaps(): Promise<boolean> {
    if (typeof window !== 'undefined' && (window as any).google && (window as any).google.maps) {
      return true;
    }
    // Return true if Google library loads, we load it via @vis.gl/react-google-maps APIProvider
    return false;
  },

  /**
   * Get Current Location using browser Geolocation API
   */
  getCurrentLocation(): Promise<MapCoordinates> {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        reject({
          type: 'NO_GPS',
          message: 'Seu navegador não suporta geolocalização.'
        } as MapError);
        return;
      }

      navigator.geolocation.getCurrentPosition(
        (position) => {
          resolve({
            lat: position.coords.latitude,
            lng: position.coords.longitude,
          });
        },
        (error) => {
          let type: 'PERMISSION_DENIED' | 'NO_GPS' | 'UNKNOWN' = 'UNKNOWN';
          let message = 'Erro ao obter localização atual.';

          if (error.code === error.PERMISSION_DENIED) {
            type = 'PERMISSION_DENIED';
            message = 'Permissão de localização negada pelo usuário.';
          } else if (error.code === error.POSITION_UNAVAILABLE) {
            type = 'NO_GPS';
            message = 'Sinal de GPS indisponível ou inacessível.';
          } else if (error.code === error.TIMEOUT) {
            type = 'NO_GPS';
            message = 'Tempo limite esgotado ao obter localização.';
          }

          reject({ type, message } as MapError);
        },
        { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
      );
    });
  },

  /**
   * Autocomplete Places search using google.maps.places.Place
   */
  async searchPlaces(query: string, locationBias?: MapCoordinates): Promise<PlaceSearchResult[]> {
    if (!query || query.trim().length < 3) return [];

    try {
      if (typeof google === 'undefined' || !google.maps || !google.maps.places) {
        throw new Error('Google Maps Places Library is not available yet');
      }

      const bias = locationBias
        ? new google.maps.LatLng(locationBias.lat, locationBias.lng)
        : undefined;

      // Use the modern Place.searchByText API
      const { places } = await google.maps.places.Place.searchByText({
        textQuery: query,
        fields: ['id', 'displayName', 'formattedAddress', 'location'],
        locationBias: bias,
        maxResultCount: 6,
      });

      if (!places || places.length === 0) return [];

      return places.map((place) => {
        const coordinates: MapCoordinates = {
          lat: place.location?.lat() || -22.1225,
          lng: place.location?.lng() || -51.3856,
        };

        // Extract neighborhood from formattedAddress or mock neighborhood for Presidente Prudente
        let bairro = '';
        const addr = place.formattedAddress || '';
        const parts = addr.split(',');
        if (parts.length > 1) {
          bairro = parts[1].trim();
        }

        return {
          placeId: place.id || '',
          name: place.displayName || '',
          formattedAddress: addr,
          coordinates,
          bairro: bairro || 'Prudente'
        };
      });
    } catch (error: any) {
      console.warn('searchPlaces failed:', error);
      // Fallback with simple filter on local locations for offline compatibility
      return [];
    }
  },

  /**
   * Reverse Geocode coordinates to address
   */
  async reverseGeocode(coordinates: MapCoordinates): Promise<PlaceSearchResult> {
    try {
      if (typeof google === 'undefined' || !google.maps || !google.maps.Geocoder) {
        throw new Error('Google Maps Geocoder is not loaded');
      }

      const geocoder = new google.maps.Geocoder();
      const response = await geocoder.geocode({ location: coordinates });

      if (response.results && response.results[0]) {
        const result = response.results[0];
        let bairro = '';
        // Find neighborhood in address components
        for (const component of result.address_components) {
          if (component.types.includes('sublocality') || component.types.includes('sublocality_level_1')) {
            bairro = component.long_name;
            break;
          }
        }

        return {
          placeId: result.place_id,
          name: result.formatted_address.split(',')[0],
          formattedAddress: result.formatted_address,
          coordinates,
          bairro: bairro || 'Prudente'
        };
      }
      throw new Error('Endereço não encontrado para estas coordenadas');
    } catch (error: any) {
      console.warn('reverseGeocode failed:', error);
      return {
        placeId: 'manual',
        name: `Localização Customizada`,
        formattedAddress: `${coordinates.lat.toFixed(5)}, ${coordinates.lng.toFixed(5)}`,
        coordinates,
        bairro: 'Prudente'
      };
    }
  },

  /**
   * Calculate real route using google.maps.routes.Route.computeRoutes
   */
  async calculateRoute(
    origin: MapCoordinates,
    destination: MapCoordinates,
    stops?: MapCoordinates[]
  ): Promise<MapRoute> {
    const cacheKey = this.getCacheKey(origin, destination);
    const cached = routeCache.get(cacheKey);

    if (cached && Date.now() - cached.timestamp < CACHE_TTL_MS) {
      console.log('Returning cached route:', cacheKey);
      return cached.route;
    }

    try {
      if (typeof google === 'undefined' || !google.maps) {
        throw new Error('GOOGLE_UNAVAILABLE');
      }

      // Check if Routes API is loaded
      if (!(google.maps as any).routes && !(google.maps as any).Route) {
        // Fallback to standard DirectionsService if Routes library isn't imported via Routes API
        return this.fallbackDirectionsService(origin, destination, stops);
      }

      const routesLib = (google.maps as any).routes || (google.maps as any).Route;

      // Build intermediate waypoints
      const intermediates = stops?.map(stop => ({
        location: {
          latLng: {
            latitude: stop.lat,
            longitude: stop.lng
          }
        }
      })) || [];

      const request: any = {
        origin: {
          location: {
            latLng: {
              latitude: origin.lat,
              longitude: origin.lng
            }
          }
        },
        destination: {
          location: {
            latLng: {
              latitude: destination.lat,
              longitude: destination.lng
            }
          }
        },
        travelMode: 'DRIVING',
        routingPreference: 'TRAFFIC_AWARE',
        fields: ['path', 'distanceMeters', 'durationMillis', 'viewport', 'legs']
      };

      if (intermediates.length > 0) {
        request.intermediates = intermediates;
      }

      // Compute routes using google.maps.routes API or Route class
      const computeResult = await routesLib.Route.computeRoutes(request);
      const route = computeResult.routes?.[0];

      if (!route) {
        throw new Error('Nenhuma rota encontrada.');
      }

      // Extract details
      const distanceKm = parseFloat((route.distanceMeters / 1000).toFixed(1));
      const durationMinutes = Math.ceil(parseInt(route.durationMillis || '0') / 60000);
      
      // Decode polyline or create polylines
      let polyline = '';
      if (route.polyline?.encodedPolyline) {
        polyline = route.polyline.encodedPolyline;
      } else if (route.createPolylines) {
        const polylines = route.createPolylines();
        if (polylines.length > 0) {
          polyline = polylines[0].getPath().getArray().map((latlng: any) => `${latlng.lat()},${latlng.lng()}`).join(';');
        }
      }

      // Determine traffic status
      let trafficStatus: 'light' | 'moderate' | 'heavy' = 'light';
      if (route.durationMinutes && route.durationMinutesWithoutTraffic) {
        const diff = route.durationMinutes - route.durationMinutesWithoutTraffic;
        if (diff > 5) trafficStatus = 'heavy';
        else if (diff > 2) trafficStatus = 'moderate';
      }

      const mapRoute: MapRoute = {
        distanceKm: distanceKm || 1.0,
        durationMinutes: durationMinutes || 3,
        polyline,
        bounds: route.viewport,
        trafficStatus
      };

      // Save to cache
      routeCache.set(cacheKey, { route: mapRoute, timestamp: Date.now() });

      return mapRoute;
    } catch (error: any) {
      console.warn('Routes API computeRoutes failed, trying fallback DirectionsService:', error);
      return this.fallbackDirectionsService(origin, destination, stops);
    }
  },

  /**
   * Fallback using standard DirectionsService
   */
  async fallbackDirectionsService(
    origin: MapCoordinates,
    destination: MapCoordinates,
    stops?: MapCoordinates[]
  ): Promise<MapRoute> {
    const cacheKey = this.getCacheKey(origin, destination);
    const cached = routeCache.get(cacheKey);

    if (cached && Date.now() - cached.timestamp < CACHE_TTL_MS) {
      return cached.route;
    }

    return new Promise((resolve, reject) => {
      try {
        if (typeof google === 'undefined' || !google.maps) {
          reject(new Error('GOOGLE_UNAVAILABLE'));
          return;
        }

        const directionsService = new google.maps.DirectionsService();
        const waypoints = stops?.map(stop => ({
          location: new google.maps.LatLng(stop.lat, stop.lng),
          stopover: true
        })) || [];

        directionsService.route(
          {
            origin: new google.maps.LatLng(origin.lat, origin.lng),
            destination: new google.maps.LatLng(destination.lat, destination.lng),
            waypoints,
            travelMode: google.maps.TravelMode.DRIVING,
          },
          (result, status) => {
            if (status === google.maps.DirectionsStatus.OK && result && result.routes[0]) {
              const route = result.routes[0];
              const leg = route.legs[0];

              // Sum up total distance and duration across all legs
              let totalDistanceMeters = 0;
              let totalDurationSeconds = 0;
              route.legs.forEach(l => {
                totalDistanceMeters += l.distance?.value || 0;
                totalDurationSeconds += l.duration?.value || 0;
              });

              const distanceKm = parseFloat((totalDistanceMeters / 1000).toFixed(1));
              const durationMinutes = Math.ceil(totalDurationSeconds / 60);

              const mapRoute: MapRoute = {
                distanceKm: distanceKm || 1.0,
                durationMinutes: durationMinutes || 3,
                polyline: route.overview_polyline || '',
                bounds: {
                  east: route.bounds.getNorthEast().lng(),
                  north: route.bounds.getNorthEast().lat(),
                  south: route.bounds.getSouthWest().lat(),
                  west: route.bounds.getSouthWest().lng(),
                }
              };

              routeCache.set(cacheKey, { route: mapRoute, timestamp: Date.now() });
              resolve(mapRoute);
            } else {
              // Local simulation fallback if google route fails or no quota
              console.warn('DirectionsService failed with status:', status);
              resolve(this.simulateLocalOfflineRoute(origin, destination));
            }
          }
        );
      } catch (err) {
        console.warn('DirectionsService threw exception, falling back to local simulation:', err);
        resolve(this.simulateLocalOfflineRoute(origin, destination));
      }
    });
  },

  /**
   * Local route simulation for complete robust offline operation or key missing
   */
  simulateLocalOfflineRoute(origin: MapCoordinates, destination: MapCoordinates): MapRoute {
    const lat1 = origin.lat;
    const lon1 = origin.lng;
    const lat2 = destination.lat;
    const lon2 = destination.lng;

    // Haversine formula for distance in km
    const R = 6371; // Earth radius in km
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
      Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    const distanceKm = parseFloat((R * c).toFixed(1)) || 1.0;

    // Assume 40 km/h average speed in city
    const durationMinutes = Math.max(2, Math.ceil((distanceKm / 40) * 60));

    // Simple encoded polyline or mock polyline points representing a straight line
    const polyline = '';

    return {
      distanceKm,
      durationMinutes,
      polyline,
      bounds: {
        east: Math.max(lon1, lon2) + 0.01,
        north: Math.max(lat1, lat2) + 0.01,
        south: Math.min(lat1, lat2) - 0.01,
        west: Math.min(lon1, lon2) - 0.01,
      }
    };
  },

  /**
   * Helper to calculate simple straight distance in km
   */
  calculateDistance(p1: MapCoordinates, p2: MapCoordinates): number {
    return this.simulateLocalOfflineRoute(p1, p2).distanceKm;
  },

  /**
   * Helper to calculate simple duration in minutes
   */
  calculateDuration(p1: MapCoordinates, p2: MapCoordinates): number {
    return this.simulateLocalOfflineRoute(p1, p2).durationMinutes;
  },

  /**
   * Decodes polyline into coordinate array for plotting on Map
   */
  decodePolyline(encoded: string): MapCoordinates[] {
    if (!encoded) return [];
    
    // Standard polyline decoding algorithm
    const poly: MapCoordinates[] = [];
    let index = 0, len = encoded.length;
    let lat = 0, lng = 0;

    while (index < len) {
      let b, shift = 0, result = 0;
      do {
        b = encoded.charCodeAt(index++) - 63;
        result |= (b & 0x1f) << shift;
        shift += 5;
      } while (b >= 0x20);
      let dlat = ((result & 1) ? ~(result >> 1) : (result >> 1));
      lat += dlat;

      shift = 0;
      result = 0;
      do {
        b = encoded.charCodeAt(index++) - 63;
        result |= (b & 0x1f) << shift;
        shift += 5;
      } while (b >= 0x20);
      let dlng = ((result & 1) ? ~(result >> 1) : (result >> 1));
      lng += dlng;

      poly.push({ lat: lat / 1E5, lng: lng / 1E5 });
    }

    return poly;
  },

  /**
   * Fits bounds for a map given coordinates
   */
  fitBounds(map: google.maps.Map, coordinates: MapCoordinates[]): void {
    if (!map || coordinates.length === 0) return;
    const bounds = new google.maps.LatLngBounds();
    coordinates.forEach((coord) => {
      bounds.extend(new google.maps.LatLng(coord.lat, coord.lng));
    });
    map.fitBounds(bounds);
  }
};
