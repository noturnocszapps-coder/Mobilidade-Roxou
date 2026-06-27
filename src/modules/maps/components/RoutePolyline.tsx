import React, { useEffect, useRef } from 'react';
import { useMap } from '@vis.gl/react-google-maps';
import { MapCoordinates } from '../types';
import { googleMapsService } from '../services/googleMapsService';

interface RoutePolylineProps {
  encodedPolyline: string | null;
  coordinates?: MapCoordinates[];
  strokeColor?: string;
  strokeWeight?: number;
}

export const RoutePolyline: React.FC<RoutePolylineProps> = ({
  encodedPolyline,
  coordinates,
  strokeColor = '#a855f7', // Roxou purple
  strokeWeight = 5,
}) => {
  const map = useMap();
  const polylineRef = useRef<google.maps.Polyline | null>(null);

  useEffect(() => {
    if (!map) return;

    // Clear previous polyline
    if (polylineRef.current) {
      polylineRef.current.setMap(null);
      polylineRef.current = null;
    }

    // Determine path coordinates
    let pathCoords: google.maps.LatLngLiteral[] = [];
    if (encodedPolyline) {
      pathCoords = googleMapsService.decodePolyline(encodedPolyline);
    } else if (coordinates && coordinates.length > 0) {
      pathCoords = coordinates;
    }

    if (pathCoords.length === 0) return;

    // Create and render new polyline with animated/gradient effect
    const polyline = new google.maps.Polyline({
      path: pathCoords,
      geodesic: true,
      strokeColor,
      strokeOpacity: 0.9,
      strokeWeight,
      map,
    });

    polylineRef.current = polyline;

    return () => {
      if (polylineRef.current) {
        polylineRef.current.setMap(null);
      }
    };
  }, [map, encodedPolyline, coordinates, strokeColor, strokeWeight]);

  return null;
};
