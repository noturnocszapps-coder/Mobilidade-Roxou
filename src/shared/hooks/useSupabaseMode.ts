import { useState, useEffect } from 'react';
import { isSupabaseConfigured } from '@/src/services/supabase/supabaseClient';

export function useSupabaseMode() {
  const [isRealDb, setIsRealDb] = useState(isSupabaseConfigured);

  useEffect(() => {
    setIsRealDb(isSupabaseConfigured);
  }, []);

  return {
    isSupabaseConfigured: isRealDb,
    modeText: isRealDb ? 'Supabase Conectado' : 'Modo Demonstração (Local)',
  };
}
