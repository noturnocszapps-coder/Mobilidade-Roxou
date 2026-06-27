import React from 'react';
import { AdvancedMarker, Pin } from '@vis.gl/react-google-maps';
import { MapCoordinates } from '../types';

interface MapMarkerProps {
  position: MapCoordinates;
  title?: string;
  onClick?: () => void;
  type?: 'origin' | 'destination' | 'driver' | 'stop';
  label?: string;
  iconUrl?: string;
}

export const MapMarker: React.FC<MapMarkerProps> = ({
  position,
  title,
  onClick,
  type = 'origin',
  label,
  iconUrl,
}) => {
  // Determine pin coloring & custom rendering based on marker types
  const getPinSettings = () => {
    switch (type) {
      case 'origin':
        return {
          bg: '#a855f7', // Purple
          glyph: 'A',
          border: '#6b21a8',
        };
      case 'destination':
        return {
          bg: '#f43f5e', // Rose
          glyph: 'B',
          border: '#9f1239',
        };
      case 'stop':
        return {
          bg: '#eab308', // Amber
          glyph: '•',
          border: '#854d0e',
        };
      case 'driver':
      default:
        return {
          bg: '#10b981', // Emerald
          glyph: '🚗',
          border: '#065f46',
        };
    }
  };

  const { bg, glyph, border } = getPinSettings();

  // For standard drivers we can render a custom animated emoji marker or standard Google Pin
  if (type === 'driver') {
    return (
      <AdvancedMarker
        position={position}
        title={title || 'Motorista'}
        onClick={onClick}
      >
        <div 
          className="flex flex-col items-center justify-center filter drop-shadow-md select-none"
          style={{ width: '40px', height: '40px' }}
        >
          <div className="bg-slate-900 border border-emerald-400 p-1.5 rounded-full shadow-lg relative animate-bounce">
            <span className="text-sm">🚗</span>
            <span className="absolute -top-1 -right-1 flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
            </span>
          </div>
        </div>
      </AdvancedMarker>
    );
  }

  return (
    <AdvancedMarker
      position={position}
      title={title}
      onClick={onClick}
    >
      <Pin
        background={bg}
        borderColor={border}
        glyphColor="#ffffff"
        glyph={label || glyph}
      />
    </AdvancedMarker>
  );
};
