import React from 'react';
import { Navigation, Loader2 } from 'lucide-react';

interface LocationButtonProps {
  onClick: () => void;
  loading?: boolean;
  className?: string;
}

export const LocationButton: React.FC<LocationButtonProps> = ({
  onClick,
  loading = false,
  className = '',
}) => {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={loading}
      className={`absolute bottom-4 right-4 bg-slate-950/80 backdrop-blur-md text-white p-3 rounded-full shadow-lg border border-purple-500/30 hover:bg-slate-900 transition-all flex items-center justify-center hover:scale-105 active:scale-95 disabled:opacity-50 z-10 cursor-pointer ${className}`}
      title="Minha Localização"
      id="btn-my-location"
    >
      {loading ? (
        <Loader2 className="w-5 h-5 animate-spin text-purple-400" />
      ) : (
        <Navigation className="w-5 h-5 text-purple-400 fill-current" />
      )}
    </button>
  );
};
