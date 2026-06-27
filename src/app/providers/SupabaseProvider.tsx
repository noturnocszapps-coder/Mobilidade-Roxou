import React, { createContext, useContext, useState, useEffect } from 'react';
import { isSupabaseConfigured as checkSupabase } from '@/src/services/supabase/supabaseClient';

interface SupabaseContextType {
  isSupabaseConfigured: boolean;
  modeText: string;
}

const SupabaseContext = createContext<SupabaseContextType | undefined>(undefined);

export const SupabaseProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [isConfigured, setIsConfigured] = useState(checkSupabase);

  useEffect(() => {
    setIsConfigured(checkSupabase);
  }, []);

  const modeText = isConfigured ? 'Supabase Conectado' : 'Modo Demonstração (Local)';

  return (
    <SupabaseContext.Provider value={{ isSupabaseConfigured: isConfigured, modeText }}>
      {children}
    </SupabaseContext.Provider>
  );
};

export const useSupabase = () => {
  const context = useContext(SupabaseContext);
  if (!context) {
    throw new Error('useSupabase must be used within a SupabaseProvider');
  }
  return context;
};
