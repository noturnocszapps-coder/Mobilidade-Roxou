import React, { useState, useEffect } from 'react';
import { LocationPoint, User, RideRequest } from '@/src/shared/types';
import { PRESIDENTE_PRUDENTE_LOCATIONS } from '@/src/shared/constants';
import { MapPin, Navigation, Compass, Star, Sparkles } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

interface MapMockupProps {
  origin: LocationPoint | null;
  destination: LocationPoint | null;
  drivers: User[];
  onSelectLocation?: (loc: LocationPoint, type: 'origin' | 'destination') => void;
  activeRide?: RideRequest | null;
  simulatedProgress?: number; // 0 to 100
}

export const MapMockup: React.FC<MapMockupProps> = ({
  origin,
  destination,
  drivers,
  onSelectLocation,
  activeRide,
  simulatedProgress = 0,
}) => {
  const [clickMode, setClickMode] = useState<'origin' | 'destination'>('origin');
  const [driverPositions, setDriverPositions] = useState<{ [id: string]: { lat: number; lng: number } }>({});

  // Initialize and jitter driver positions for simulation effect
  useEffect(() => {
    const initialPos: { [id: string]: { lat: number; lng: number } } = {};
    drivers.forEach((driver) => {
      // Find a random location or use preset offsets around Presidente Prudente landmarks
      const randSeed = driver.id === 'd1' ? 0 : driver.id === 'd2' ? 1 : 2;
      const baseLoc = PRESIDENTE_PRUDENTE_LOCATIONS[randSeed % PRESIDENTE_PRUDENTE_LOCATIONS.length];
      initialPos[driver.id] = {
        lat: baseLoc.lat + (Math.sin(randSeed) * 5),
        lng: baseLoc.lng + (Math.cos(randSeed) * 5),
      };
    });
    setDriverPositions(initialPos);

    const interval = setInterval(() => {
      setDriverPositions((prev) => {
        const next = { ...prev };
        Object.keys(next).forEach((id) => {
          // Slow drifting
          next[id] = {
            lat: Math.max(10, Math.min(90, next[id].lat + (Math.random() - 0.5) * 1.5)),
            lng: Math.max(10, Math.min(90, next[id].lng + (Math.random() - 0.5) * 1.5)),
          };
        });
        return next;
      });
    }, 4000);

    return () => clearInterval(interval);
  }, [drivers]);

  // If there's an active ride, calculate car current position along the line
  let carPos = { lat: 0, lng: 0 };
  if (activeRide && activeRide.origin && activeRide.destination) {
    const progressFactor = simulatedProgress / 100;
    carPos = {
      lat: activeRide.origin.lat + (activeRide.destination.lat - activeRide.origin.lat) * progressFactor,
      lng: activeRide.origin.lng + (activeRide.destination.lng - activeRide.origin.lng) * progressFactor,
    };
  }

  const handleLandmarkClick = (loc: LocationPoint) => {
    if (onSelectLocation) {
      onSelectLocation(loc, clickMode);
      // Toggle mode
      setClickMode(clickMode === 'origin' ? 'destination' : 'origin');
    }
  };

  return (
    <div className="relative w-full h-[280px] bg-indigo-950/90 rounded-2xl overflow-hidden shadow-inner border border-indigo-500/20" id="roxou-map-container">
      {/* Grid Pattern and Roads representing a stylized Map of Presidente Prudente */}
      <svg className="absolute inset-0 w-full h-full pointer-events-none opacity-40" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <pattern id="grid" width="40" height="40" patternUnits="userSpaceOnUse">
            <path d="M 40 0 L 0 0 0 40" fill="none" stroke="rgba(168, 85, 247, 0.15)" strokeWidth="1" />
          </pattern>
        </defs>
        <rect width="100%" height="100%" fill="url(#grid)" />

        {/* Main Avenues representing Prudente: Av. Manoel Goulart and Av. Washington Luiz */}
        {/* Av. Manoel Goulart (diagonal from Top Left to Bottom Right) */}
        <path d="M -20 -10 L 120 110" stroke="rgba(168, 85, 247, 0.4)" strokeWidth="6" strokeLinecap="round" />
        <path d="M -20 -10 L 120 110" stroke="#090514" strokeWidth="2" strokeDasharray="5,5" strokeLinecap="round" />

        {/* Av. Washington Luiz (diagonal from Bottom Left to Top Right) */}
        <path d="M -10 110 L 110 -10" stroke="rgba(168, 85, 247, 0.4)" strokeWidth="6" strokeLinecap="round" />
        <path d="M -10 110 L 110 -10" stroke="#090514" strokeWidth="2" strokeDasharray="5,5" strokeLinecap="round" />

        {/* Av. Brasil (Horizontal crossing central area) */}
        <path d="M -20 42 L 120 42" stroke="rgba(168, 85, 247, 0.35)" strokeWidth="5" />
        {/* Av. Coronel Marcondes (Horizontal crossing lower center) */}
        <path d="M -20 55 L 120 55" stroke="rgba(168, 85, 247, 0.35)" strokeWidth="5" />
        {/* Rodovia Assis Chateaubriand (Outer ring at the bottom) */}
        <path d="M -20 85 Q 50 75 120 85" fill="none" stroke="rgba(147, 51, 234, 0.5)" strokeWidth="8" />

        {/* Parque do Povo Green Area */}
        <ellipse cx="50%" cy="50%" rx="40" ry="15" transform="rotate(-15 140 140)" fill="rgba(34, 197, 94, 0.25)" stroke="rgba(34, 197, 94, 0.4)" strokeWidth="1" />
      </svg>

      {/* Grid lines label indicators - Local representation */}
      <div className="absolute top-3 left-3 bg-black/60 backdrop-blur-md px-2.5 py-1 rounded-lg text-[10px] font-mono text-purple-300 pointer-events-none flex items-center gap-1.5 border border-purple-500/20">
        <Compass className="w-3.5 h-3.5 animate-spin-slow text-purple-400" />
        <span>Presidente Prudente, SP</span>
      </div>

      <div className="absolute top-3 right-3 flex items-center gap-1">
        <button
          onClick={() => setClickMode('origin')}
          className={`px-2 py-1 text-[10px] rounded-l font-medium transition-colors ${
            clickMode === 'origin' ? 'bg-purple-600 text-white shadow-sm' : 'bg-black/60 text-purple-300 hover:bg-black/80'
          }`}
          id="btn-select-origin-mode"
        >
          Origem
        </button>
        <button
          onClick={() => setClickMode('destination')}
          className={`px-2 py-1 text-[10px] rounded-r font-medium transition-colors ${
            clickMode === 'destination' ? 'bg-purple-600 text-white shadow-sm' : 'bg-black/60 text-purple-300 hover:bg-black/80'
          }`}
          id="btn-select-destination-mode"
        >
          Destino
        </button>
      </div>

      {/* Instructions Overlay */}
      <div className="absolute bottom-2 left-1/2 transform -translate-x-1/2 bg-black/60 backdrop-blur-md px-3 py-1 rounded-full text-[10px] text-gray-300 pointer-events-none border border-purple-500/10 flex items-center gap-1 whitespace-nowrap">
        <Sparkles className="w-3 h-3 text-yellow-400 animate-pulse" />
        <span>Clique nos locais abaixo para definir {clickMode === 'origin' ? 'origem 🟣' : 'destino 🔴'}</span>
      </div>

      {/* Interactive Landmarks Markers */}
      {PRESIDENTE_PRUDENTE_LOCATIONS.map((loc) => {
        const isOrigin = origin?.id === loc.id;
        const isDestination = destination?.id === loc.id;

        return (
          <button
            key={loc.id}
            onClick={() => handleLandmarkClick(loc)}
            className="absolute -translate-x-1/2 -translate-y-1/2 p-1.5 group focus:outline-none transition-transform active:scale-125 z-10"
            style={{ top: `${loc.lat}%`, left: `${loc.lng}%` }}
            title={loc.name}
            id={`landmark-${loc.id}`}
          >
            {/* Pulsing indicator if selected */}
            <AnimatePresence>
              {(isOrigin || isDestination) && (
                <motion.span
                  initial={{ scale: 0.5, opacity: 0 }}
                  animate={{ scale: 2, opacity: [0.5, 0] }}
                  exit={{ opacity: 0 }}
                  transition={{ repeat: Infinity, duration: 1.5 }}
                  className={`absolute inset-0 rounded-full ${isOrigin ? 'bg-purple-500' : 'bg-red-500'}`}
                />
              )}
            </AnimatePresence>

            {/* Marker Core */}
            <div
              className={`w-3.5 h-3.5 rounded-full flex items-center justify-center border-2 transition-all ${
                isOrigin
                  ? 'bg-purple-500 border-white scale-125 shadow-lg shadow-purple-500/50'
                  : isDestination
                  ? 'bg-red-500 border-white scale-125 shadow-lg shadow-red-500/50'
                  : 'bg-indigo-950 border-purple-400 hover:border-white hover:scale-110'
              }`}
            >
              {isOrigin ? (
                <span className="text-[6px] font-bold text-white">O</span>
              ) : isDestination ? (
                <span className="text-[6px] font-bold text-white">D</span>
              ) : (
                <div className="w-1 h-1 bg-purple-400 rounded-full" />
              )}
            </div>

            {/* Tooltip */}
            <div className="absolute left-1/2 -translate-x-1/2 bottom-full mb-1 bg-black/80 backdrop-blur-md text-[9px] text-white font-medium px-1.5 py-0.5 rounded shadow pointer-events-none opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap z-20 border border-purple-500/20">
              {loc.name}
            </div>
          </button>
        );
      })}

      {/* Dotted Route Line if Origin and Destination are selected */}
      {origin && destination && (
        <svg className="absolute inset-0 w-full h-full pointer-events-none z-0" xmlns="http://www.w3.org/2000/svg">
          {/* Animated Glow Line */}
          <line
            x1={`${origin.lng}%`}
            y1={`${origin.lat}%`}
            x2={`${destination.lng}%`}
            y2={`${destination.lat}%`}
            stroke="url(#purpleGlow)"
            strokeWidth="3.5"
            strokeLinecap="round"
          />
          {/* Main Dotted Path */}
          <line
            x1={`${origin.lng}%`}
            y1={`${origin.lat}%`}
            x2={`${destination.lng}%`}
            y2={`${destination.lat}%`}
            stroke="#a855f7"
            strokeWidth="2.5"
            strokeDasharray="4 4"
            strokeLinecap="round"
            className="animate-dash"
          />
          {/* Glow Definition */}
          <defs>
            <linearGradient id="purpleGlow" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#c084fc" stopOpacity="0.8" />
              <stop offset="100%" stopColor="#ef4444" stopOpacity="0.8" />
            </linearGradient>
          </defs>
        </svg>
      )}

      {/* Online Drivers Representation (Cars drifting on map) */}
      {!activeRide &&
        Object.entries(driverPositions).map(([id, pos]) => {
          const driver = drivers.find((d) => d.id === id);
          if (!driver || driver.status !== 'approved') return null;
          const typedPos = pos as { lat: number; lng: number };

          return (
            <div
              key={id}
              className="absolute -translate-x-1/2 -translate-y-1/2 z-10 transition-all duration-[4000ms] ease-linear pointer-events-none"
              style={{ top: `${typedPos.lat}%`, left: `${typedPos.lng}%` }}
            >
              <div className="relative group/driver flex flex-col items-center">
                <div className="bg-purple-600 border border-purple-300 text-white p-1 rounded-full shadow-lg flex items-center justify-center animate-bounce-slow">
                  <Navigation className="w-3 h-3 fill-current rotate-45 transform" />
                </div>
                <div className="absolute top-full mt-0.5 bg-black/75 text-[8px] text-purple-200 px-1 py-0.2 rounded font-mono whitespace-nowrap">
                  {driver.name.split(' ')[0]} (★{driver.rating})
                </div>
              </div>
            </div>
          );
        })}

      {/* Active Ride Animated Car */}
      {activeRide && (
        <div
          className="absolute -translate-x-1/2 -translate-y-1/2 z-20 transition-all duration-500 ease-out"
          style={{ top: `${carPos.lat}%`, left: `${carPos.lng}%` }}
        >
          <div className="relative bg-purple-500 text-white p-1.5 rounded-full shadow-xl flex items-center justify-center border-2 border-white animate-pulse">
            <Navigation className="w-4 h-4 fill-current rotate-90" />
            <div className="absolute -top-7 bg-purple-600 border border-purple-400 text-[9px] font-bold text-white px-2 py-0.5 rounded shadow-lg whitespace-nowrap">
              Seu Roxou {simulatedProgress === 100 ? 'Chegou!' : 'a caminho...'}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
