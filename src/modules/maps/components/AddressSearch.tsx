import React, { useState, useEffect, useRef } from 'react';
import { Search, MapPin, Loader2, X, AlertCircle, Sparkles } from 'lucide-react';
import { googleMapsService } from '../services/googleMapsService';
import { LocationPoint } from '@/src/shared/types';
import { PRESIDENTE_PRUDENTE_LOCATIONS } from '@/src/shared/constants';

const GOOGLE_MAPS_API_KEY =
  process.env.GOOGLE_MAPS_PLATFORM_KEY ||
  (import.meta as any).env?.VITE_GOOGLE_MAPS_API_KEY ||
  (import.meta as any).env?.VITE_GOOGLE_MAPS_PLATFORM_KEY ||
  '';

interface AddressSearchProps {
  value: LocationPoint | null;
  onSelect: (location: LocationPoint) => void;
  placeholder: string;
  className?: string;
  id?: string;
}

export const AddressSearch: React.FC<AddressSearchProps> = ({
  value,
  onSelect,
  placeholder,
  className = '',
  id = 'address-search-input',
}) => {
  const [query, setQuery] = useState('');
  const [suggestions, setSuggestions] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [focused, setFocused] = useState(false);
  
  const containerRef = useRef<HTMLDivElement>(null);

  // Check Google Maps API status
  const isGoogleAvailable = typeof google !== 'undefined' && google.maps && google.maps.places;
  const isKeyMissing = !GOOGLE_MAPS_API_KEY;

  // Sync internal query string when value changes externally
  useEffect(() => {
    if (value) {
      setQuery(value.name || value.address || '');
    } else {
      setQuery('');
    }
  }, [value]);

  // Compute suggestions based on query and Google availability
  useEffect(() => {
    // If the query matches the selected value exactly, do not search
    if (value && query === (value.name || value.address)) {
      setSuggestions([]);
      return;
    }

    const trimmed = query.trim();

    // Case 1: Empty or very short query -> Show all default Presidente Prudente locations instantly
    if (trimmed.length < 3) {
      const defaultSuggestions = PRESIDENTE_PRUDENTE_LOCATIONS.map(loc => ({
        placeId: loc.id,
        name: loc.name,
        formattedAddress: loc.address,
        coordinates: { lat: loc.lat, lng: loc.lng },
        bairro: loc.bairro || 'Prudente',
        isDefault: true
      }));

      if (trimmed.length > 0) {
        // Filter local locations by query
        const filtered = defaultSuggestions.filter(s =>
          s.name.toLowerCase().includes(trimmed.toLowerCase()) ||
          s.formattedAddress.toLowerCase().includes(trimmed.toLowerCase()) ||
          s.bairro.toLowerCase().includes(trimmed.toLowerCase())
        );
        setSuggestions(filtered);
      } else {
        setSuggestions(defaultSuggestions);
      }
      setLoading(false);
      return;
    }

    // Case 2: Query has >= 3 chars -> Trigger search
    setLoading(true);
    const delayDebounce = setTimeout(async () => {
      try {
        if (isGoogleAvailable) {
          // Search via live Google Maps Places API
          const results = await googleMapsService.searchPlaces(query);
          if (results && results.length > 0) {
            setSuggestions(results);
          } else {
            // Fallback to local filtering if no results from Google
            const localFiltered = PRESIDENTE_PRUDENTE_LOCATIONS.filter(loc =>
              loc.name.toLowerCase().includes(query.toLowerCase()) ||
              loc.address.toLowerCase().includes(query.toLowerCase())
            ).map(loc => ({
              placeId: loc.id,
              name: loc.name,
              formattedAddress: loc.address,
              coordinates: { lat: loc.lat, lng: loc.lng },
              bairro: loc.bairro || 'Prudente',
              isDefault: true
            }));
            setSuggestions(localFiltered);
          }
        } else {
          // Fallback to local filtering if Google is unavailable
          const localFiltered = PRESIDENTE_PRUDENTE_LOCATIONS.filter(loc =>
            loc.name.toLowerCase().includes(query.toLowerCase()) ||
            loc.address.toLowerCase().includes(query.toLowerCase())
          ).map(loc => ({
            placeId: loc.id,
            name: loc.name,
            formattedAddress: loc.address,
            coordinates: { lat: loc.lat, lng: loc.lng },
            bairro: loc.bairro || 'Prudente',
            isDefault: true
          }));
          setSuggestions(localFiltered);
        }
      } catch (err) {
        console.warn('Error fetching address suggestions:', err);
        setSuggestions([]);
      } finally {
        setLoading(false);
      }
    }, 300); // Super snappy 300ms debounce!

    return () => clearTimeout(delayDebounce);
  }, [query, value, isGoogleAvailable]);

  // Click outside listener to close dropdown
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setFocused(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSelectSuggestion = (s: any) => {
    const loc: LocationPoint = {
      id: s.placeId || crypto.randomUUID(),
      name: s.name,
      address: s.formattedAddress,
      lat: s.coordinates.lat,
      lng: s.coordinates.lng,
      bairro: s.bairro || 'Prudente',
    };
    onSelect(loc);
    setQuery(s.name);
    setSuggestions([]);
    setFocused(false);
  };

  const handleClear = (e: React.MouseEvent) => {
    e.stopPropagation();
    setQuery('');
    setSuggestions([]);
    // Trigger callback with empty location
    onSelect({
      id: '',
      name: '',
      address: '',
      lat: 0,
      lng: 0,
    });
  };

  return (
    <div ref={containerRef} className={`relative w-full ${className}`}>
      <div className="relative flex items-center">
        <MapPin className="absolute left-3 w-4 h-4 text-slate-400 shrink-0" />
        <input
          type="text"
          id={id}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onFocus={() => setFocused(true)}
          placeholder={placeholder}
          className="w-full bg-slate-50 border border-slate-200 rounded-xl py-2 pl-9 pr-8 text-xs text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-1 focus:ring-violet-500 font-medium"
          autoComplete="off"
        />
        {loading && (
          <Loader2 className="absolute right-3 w-4 h-4 text-purple-600 animate-spin" />
        )}
        {!loading && query && (
          <button
            type="button"
            onClick={handleClear}
            className="absolute right-3 p-0.5 hover:bg-slate-200 rounded-full cursor-pointer"
          >
            <X className="w-3.5 h-3.5 text-slate-400 hover:text-slate-600" />
          </button>
        )}
      </div>

      {/* Suggestion Dropdown */}
      {focused && (
        <div className="absolute left-0 right-0 top-full mt-1.5 bg-white border border-slate-200/80 rounded-2xl shadow-xl max-h-60 overflow-y-auto z-50 divide-y divide-slate-100 animate-in fade-in slide-in-from-top-1 duration-150">
          
          {/* Diagnostic Google Maps API Info Block */}
          <div className="px-3.5 py-1.5 bg-slate-50 text-[10px] flex items-center justify-between text-slate-500 font-semibold gap-1.5 border-b border-slate-100">
            {isKeyMissing ? (
              <div className="flex items-center gap-1 text-amber-600">
                <AlertCircle className="w-3 h-3 text-amber-500" />
                <span>Simulação Ativa (Sem chave do Google Maps)</span>
              </div>
            ) : !isGoogleAvailable ? (
              <div className="flex items-center gap-1 text-rose-600">
                <AlertCircle className="w-3 h-3 text-rose-500 animate-pulse" />
                <span>Google Maps pendente/Places desativado</span>
              </div>
            ) : (
              <div className="flex items-center gap-1 text-emerald-600">
                <Sparkles className="w-3 h-3 text-emerald-500" />
                <span>Google Places ativo e conectado</span>
              </div>
            )}
            <span className="text-[9px] text-slate-400 uppercase font-bold">Roxou</span>
          </div>

          {suggestions.length === 0 ? (
            <div className="px-4 py-3 text-center text-[11px] text-slate-400">
              Nenhum ponto encontrado para "{query}"
            </div>
          ) : (
            suggestions.map((s, idx) => (
              <button
                key={s.placeId || idx}
                type="button"
                onClick={() => handleSelectSuggestion(s)}
                className="w-full px-4 py-2.5 text-left hover:bg-slate-50 flex items-start gap-3 transition-colors duration-150 cursor-pointer"
              >
                <MapPin className={`w-4 h-4 mt-0.5 shrink-0 ${s.isDefault ? 'text-violet-500' : 'text-purple-600'}`} />
                <div className="min-w-0 flex-1">
                  <div className="flex items-center justify-between gap-1.5">
                    <p className="text-xs font-bold text-slate-900 truncate">{s.name}</p>
                    {s.isDefault && (
                      <span className="text-[8px] bg-slate-100 text-slate-500 font-bold px-1.5 py-0.2 rounded shrink-0 uppercase tracking-wider">Prudente</span>
                    )}
                  </div>
                  <p className="text-[10px] text-slate-500 truncate mt-0.5">{s.formattedAddress}</p>
                </div>
              </button>
            ))
          )}
        </div>
      )}
    </div>
  );
};
