import React, { useState, useEffect, useCallback } from 'react';
import { APIProvider, Map, useMap } from '@vis.gl/react-google-maps';
import { MapMarker } from './MapMarker';
import { RoutePolyline } from './RoutePolyline';
import { LocationButton } from './LocationButton';
import { RouteSummary } from './RouteSummary';
import { googleMapsService } from '../services/googleMapsService';
import { LocationPoint, RideRequest, User } from '@/src/shared/types';
import { MapCoordinates, MapRoute, MapError } from '../types';
import { AlertCircle, HelpCircle, Loader2 } from 'lucide-react';

const GOOGLE_MAPS_API_KEY =
  process.env.GOOGLE_MAPS_PLATFORM_KEY ||
  (import.meta as any).env?.VITE_GOOGLE_MAPS_API_KEY ||
  (import.meta as any).env?.VITE_GOOGLE_MAPS_PLATFORM_KEY ||
  '';

// Default center: Presidente Prudente center coordinates
const DEFAULT_CENTER: MapCoordinates = {
  lat: -22.1225,
  lng: -51.3856,
};

interface GoogleMapInnerProps {
  origin: LocationPoint | null;
  destination: LocationPoint | null;
  stops?: LocationPoint[];
  drivers?: User[];
  activeRide?: RideRequest | null;
  onSelectLocation?: (location: LocationPoint, type: 'origin' | 'destination') => void;
  clickMode?: 'origin' | 'destination';
}

const GoogleMapInner: React.FC<GoogleMapInnerProps> = ({
  origin,
  destination,
  stops = [],
  drivers = [],
  activeRide,
  onSelectLocation,
  clickMode = 'origin',
}) => {
  const map = useMap();
  const [mapError, setMapError] = useState<MapError | null>(null);
  const [gpsLoading, setGpsLoading] = useState(false);
  const [computedRoute, setComputedRoute] = useState<MapRoute | null>(null);
  const [routeLoading, setRouteLoading] = useState(false);

  // Center or fit bounds on mount and when origin/destination change
  useEffect(() => {
    if (!map) return;

    const coords: MapCoordinates[] = [];
    if (origin && origin.lat && origin.lng) {
      coords.push({ lat: origin.lat, lng: origin.lng });
    }
    if (destination && destination.lat && destination.lng) {
      coords.push({ lat: destination.lat, lng: destination.lng });
    }
    stops.forEach((s) => {
      if (s.lat && s.lng) coords.push({ lat: s.lat, lng: s.lng });
    });

    if (coords.length > 1) {
      googleMapsService.fitBounds(map, coords);
    } else if (coords.length === 1) {
      map.panTo(coords[0]);
      map.setZoom(15);
    } else {
      map.panTo(DEFAULT_CENTER);
      map.setZoom(13);
    }
  }, [map, origin, destination, stops]);

  // Recalculate route when endpoints update
  useEffect(() => {
    if (!origin || !destination) {
      setComputedRoute(null);
      return;
    }

    const fetchRoute = async () => {
      setRouteLoading(true);
      try {
        const origCoords = { lat: origin.lat, lng: origin.lng };
        const destCoords = { lat: destination.lat, lng: destination.lng };
        const stopsCoords = stops.map((s) => ({ lat: s.lat, lng: s.lng }));

        const route = await googleMapsService.calculateRoute(origCoords, destCoords, stopsCoords);
        setComputedRoute(route);
      } catch (err) {
        console.warn('Could not calculate real route, using local simulation:', err);
        const fallback = googleMapsService.simulateLocalOfflineRoute(origin, destination);
        setComputedRoute(fallback);
      } finally {
        setRouteLoading(false);
      }
    };

    fetchRoute();
  }, [origin, destination, stops]);

  // Handle My Location Click
  const handleMyLocation = async () => {
    setGpsLoading(true);
    setMapError(null);
    try {
      const coords = await googleMapsService.getCurrentLocation();
      if (map) {
        map.panTo(coords);
        map.setZoom(16);
      }

      // Automatically reverse geocode and set as origin if callback is available
      if (onSelectLocation && clickMode === 'origin') {
        const geocoded = await googleMapsService.reverseGeocode(coords);
        onSelectLocation(geocoded, 'origin');
      }
    } catch (err: any) {
      setMapError(err);
    } finally {
      setGpsLoading(false);
    }
  };

  // Map click handler to place origin or destination
  const handleMapClick = async (event: any) => {
    if (!onSelectLocation || !event.detail.latLng) return;
    const clickedCoords: MapCoordinates = {
      lat: event.detail.latLng.lat,
      lng: event.detail.latLng.lng,
    };

    try {
      setRouteLoading(true);
      const geocoded = await googleMapsService.reverseGeocode(clickedCoords);
      onSelectLocation(geocoded, clickMode);
    } catch (err) {
      console.warn('Geocoding on map click failed:', err);
    } finally {
      setRouteLoading(false);
    }
  };

  // Convert drivers list to printable/map markers
  const activeDrivers = drivers.filter(
    (drv) => drv.role === 'driver' && drv.status === 'online'
  );

  return (
    <div className="relative w-full h-full min-h-[350px] bg-slate-900 rounded-3xl overflow-hidden shadow-2xl border border-purple-500/10">
      <Map
        defaultCenter={DEFAULT_CENTER}
        defaultZoom={13}
        gestureHandling="greedy"
        disableDefaultUI={true}
        mapId="roxou_real_map"
        onClick={handleMapClick}
        className="w-full h-full"
      >
        {/* Origin Marker */}
        {origin && origin.lat && origin.lng && (
          <MapMarker
            position={{ lat: origin.lat, lng: origin.lng }}
            title={origin.name || 'Origem'}
            type="origin"
          />
        )}

        {/* Destination Marker */}
        {destination && destination.lat && destination.lng && (
          <MapMarker
            position={{ lat: destination.lat, lng: destination.lng }}
            title={destination.name || 'Destino'}
            type="destination"
          />
        )}

        {/* Intermediate Stop Markers */}
        {stops.map((stop, idx) => (
          <MapMarker
            key={stop.id || idx}
            position={{ lat: stop.lat, lng: stop.lng }}
            title={stop.name || `Parada ${idx + 1}`}
            type="stop"
            label={`${idx + 1}`}
          />
        ))}

        {/* Online drivers around */}
        {activeDrivers.map((drv) => {
          // Fallback coordinate generation near center if driver has no exact lat/lng
          const lat = (drv as any).lat || DEFAULT_CENTER.lat + (Math.random() - 0.5) * 0.02;
          const lng = (drv as any).lng || DEFAULT_CENTER.lng + (Math.random() - 0.5) * 0.02;
          return (
            <MapMarker
              key={drv.id}
              position={{ lat, lng }}
              title={drv.name}
              type="driver"
            />
          );
        })}

        {/* Route path renderer */}
        {computedRoute && (
          <RoutePolyline
            encodedPolyline={computedRoute.polyline}
            coordinates={
              !computedRoute.polyline
                ? [
                    { lat: origin!.lat, lng: origin!.lng },
                    ...stops.map((s) => ({ lat: s.lat, lng: s.lng })),
                    { lat: destination!.lat, lng: destination!.lng },
                  ]
                : undefined
            }
          />
        )}
      </Map>

      {/* Floating Overlays */}
      <LocationButton onClick={handleMyLocation} loading={gpsLoading} />

      {computedRoute && (
        <div className="absolute top-4 left-4 right-4 flex justify-center">
          <RouteSummary route={computedRoute} />
        </div>
      )}

      {/* Loader indicator */}
      {routeLoading && (
        <div className="absolute top-4 right-4 bg-slate-950/80 backdrop-blur-md px-3 py-1.5 rounded-full flex items-center gap-1.5 border border-purple-500/20 text-white text-[10px] z-10 font-medium">
          <Loader2 className="w-3.5 h-3.5 text-purple-400 animate-spin" />
          <span>Calculando...</span>
        </div>
      )}

      {/* Error Banners */}
      {mapError && (
        <div className="absolute bottom-4 left-4 right-16 bg-rose-950/90 border border-rose-500/30 backdrop-blur-md px-4 py-3 rounded-2xl flex items-start gap-3 text-white shadow-xl z-20 animate-in fade-in slide-in-from-bottom-2">
          <AlertCircle className="w-4.5 h-4.5 text-rose-400 shrink-0 mt-0.5" />
          <div className="min-w-0 flex-1">
            <h4 className="text-xs font-bold text-rose-200">Falha de Localização</h4>
            <p className="text-[10px] text-rose-300 mt-1 leading-relaxed">{mapError.message}</p>
          </div>
          <button
            onClick={() => setMapError(null)}
            className="text-xs text-rose-300 hover:text-white underline font-semibold cursor-pointer shrink-0"
          >
            Fechar
          </button>
        </div>
      )}
    </div>
  );
};

interface GoogleMapProps {
  origin: LocationPoint | null;
  destination: LocationPoint | null;
  stops?: LocationPoint[];
  drivers?: User[];
  activeRide?: RideRequest | null;
  onSelectLocation?: (location: LocationPoint, type: 'origin' | 'destination') => void;
  clickMode?: 'origin' | 'destination';
}

export const GoogleMap: React.FC<GoogleMapProps> = (props) => {
  const [apiAuthFailed, setApiAuthFailed] = useState(false);

  useEffect(() => {
    (window as any).gm_authFailure = () => {
      console.error("Google Maps API Authentication Failed!");
      setApiAuthFailed(true);
    };
    return () => {
      try {
        delete (window as any).gm_authFailure;
      } catch (e) {}
    };
  }, []);

  // If API Key is missing, display beautiful splash instructions
  if (!GOOGLE_MAPS_API_KEY) {
    return (
      <div className="relative w-full h-full min-h-[350px] bg-slate-900 rounded-3xl p-6 flex flex-col items-center justify-center text-center border border-purple-500/10">
        <div className="bg-purple-950/50 p-4 rounded-full border border-purple-500/20 mb-4 animate-bounce">
          <HelpCircle className="w-8 h-8 text-purple-400" />
        </div>
        <h3 className="text-sm font-bold text-white uppercase tracking-wider">Google Maps pendente de chave</h3>
        <p className="text-xs text-slate-400 max-w-sm mt-2 leading-relaxed">
          Para ver mapas reais, por favor configure a chave de API secreta <span className="text-purple-400 font-bold font-mono">GOOGLE_MAPS_PLATFORM_KEY</span> no painel do AI Studio.
        </p>
        <div className="mt-4 bg-slate-950/80 px-4 py-3 rounded-2xl text-left border border-purple-500/10 max-w-xs font-mono text-[9px] text-slate-300">
          <p className="font-bold text-purple-400"># Como configurar:</p>
          <p className="mt-1">1. Acesse Settings &gt; Secrets</p>
          <p>2. Adicione GOOGLE_MAPS_PLATFORM_KEY</p>
          <p>3. Cole a chave de API do console Google Cloud</p>
        </div>
      </div>
    );
  }

  if (apiAuthFailed) {
    return (
      <div className="relative w-full h-full min-h-[350px] bg-slate-900 rounded-3xl p-6 flex flex-col items-center justify-center text-center border border-rose-500/10 animate-in fade-in duration-300">
        <div className="bg-rose-950/50 p-4 rounded-full border border-rose-500/20 mb-4">
          <AlertCircle className="w-8 h-8 text-rose-400 animate-pulse" />
        </div>
        <h3 className="text-sm font-bold text-white uppercase tracking-wider">Falha na Autenticação do Google Maps</h3>
        <p className="text-xs text-slate-400 max-w-sm mt-2 leading-relaxed">
          A chave de API <span className="text-purple-400 font-mono font-bold">GOOGLE_MAPS_PLATFORM_KEY</span> foi fornecida, mas o Google recusou a autenticação.
        </p>
        <div className="mt-4 bg-slate-950/80 p-4 rounded-2xl text-left border border-rose-500/10 max-w-sm font-mono text-[9px] text-slate-300 space-y-2">
          <p className="font-bold text-rose-400"># Verificações recomendadas:</p>
          <p>1. Verifique se a chave está correta (sem espaços ou caracteres cortados).</p>
          <p>2. Certifique-se de que a <strong className="text-white">Maps JavaScript API</strong> e a <strong className="text-white">Places API</strong> estão ativadas no Console Google Cloud.</p>
          <p>3. Confirme se o faturamento (billing) está ativo no projeto associado do Cloud.</p>
        </div>
      </div>
    );
  }

  return (
    <APIProvider apiKey={GOOGLE_MAPS_API_KEY} version="weekly" language="pt-BR" libraries={['places']}>
      <GoogleMapInner {...props} />
    </APIProvider>
  );
};
