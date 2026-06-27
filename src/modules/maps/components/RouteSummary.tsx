import React from 'react';
import { Clock, Navigation, Sparkles } from 'lucide-react';
import { MapRoute } from '../types';

interface RouteSummaryProps {
  route: MapRoute | null;
  className?: string;
}

export const RouteSummary: React.FC<RouteSummaryProps> = ({ route, className = '' }) => {
  if (!route) return null;

  return (
    <div
      className={`bg-slate-950/80 backdrop-blur-md px-4 py-2.5 rounded-2xl text-xs text-white border border-purple-500/20 shadow-lg flex items-center justify-between gap-5 pointer-events-none z-10 ${className}`}
      id="route-summary-panel"
    >
      <div className="flex items-center gap-2">
        <Navigation className="w-4 h-4 text-purple-400" />
        <div>
          <p className="text-[9px] text-gray-400 font-bold uppercase tracking-wider">Distância</p>
          <p className="font-extrabold text-white text-sm">{route.distanceKm.toFixed(1)} km</p>
        </div>
      </div>

      <div className="h-6 w-[1px] bg-purple-500/20" />

      <div className="flex items-center gap-2">
        <Clock className="w-4 h-4 text-purple-400 animate-pulse" />
        <div>
          <p className="text-[9px] text-gray-400 font-bold uppercase tracking-wider">Tempo Estimado</p>
          <p className="font-extrabold text-white text-sm">{route.durationMinutes} min</p>
        </div>
      </div>

      {route.trafficStatus && (
        <>
          <div className="h-6 w-[1px] bg-purple-500/20" />
          <div className="flex items-center gap-1.5 bg-purple-950/50 px-2 py-1 rounded-lg border border-purple-500/10">
            <Sparkles className="w-3.5 h-3.5 text-yellow-400" />
            <span className="text-[10px] font-bold text-purple-300">
              Trânsito: {route.trafficStatus === 'light' ? 'Livre' : route.trafficStatus === 'moderate' ? 'Moderado' : 'Intenso'}
            </span>
          </div>
        </>
      )}
    </div>
  );
};
