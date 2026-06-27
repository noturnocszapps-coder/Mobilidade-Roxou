import React, { useState, useEffect, useRef } from 'react';
import { Search, MapPin, Loader2, X } from 'lucide-react';
import { googleMapsService } from '../services/googleMapsService';
import { LocationPoint } from '@/src/shared/types';

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

  // Sync internal query string when value changes externally
  useEffect(() => {
    if (value) {
      setQuery(value.name || value.address || '');
    } else {
      setQuery('');
    }
  }, [value]);

  // Debounce and search suggestions
  useEffect(() => {
    if (query.trim().length < 3 || (value && query === (value.name || value.address))) {
      setSuggestions([]);
      return;
    }

    const delayDebounce = setTimeout(async () => {
      setLoading(true);
      try {
        const results = await googleMapsService.searchPlaces(query);
        setSuggestions(results);
      } catch (err) {
        console.warn('Error fetching address suggestions:', err);
        setSuggestions([]);
      } finally {
        setLoading(false);
      }
    }, 4000); // 400ms debounce

    return () => clearTimeout(delayDebounce);
  }, [query, value]);

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

  const handleClear = () => {
    setQuery('');
    setSuggestions([]);
    // Trigger callback with null or trigger an empty location
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
            className="absolute right-3 p-0.5 hover:bg-slate-200 rounded-full"
          >
            <X className="w-3.5 h-3.5 text-slate-400 hover:text-slate-600" />
          </button>
        )}
      </div>

      {/* Suggestion Dropdown */}
      {focused && suggestions.length > 0 && (
        <div className="absolute left-0 right-0 top-full mt-1.5 bg-white border border-slate-200/80 rounded-2xl shadow-xl max-h-60 overflow-y-auto z-50 divide-y divide-slate-100">
          {suggestions.map((s) => (
            <button
              key={s.placeId}
              type="button"
              onClick={() => handleSelectSuggestion(s)}
              className="w-full px-4 py-2.5 text-left hover:bg-slate-50 flex items-start gap-3 transition-colors duration-150"
            >
              <MapPin className="w-4 h-4 text-purple-600 mt-0.5 shrink-0" />
              <div className="min-w-0">
                <p className="text-xs font-bold text-slate-900 truncate">{s.name}</p>
                <p className="text-[10px] text-slate-500 truncate mt-0.5">{s.formattedAddress}</p>
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
};
