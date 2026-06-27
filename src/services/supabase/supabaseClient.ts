import { createClient } from '@supabase/supabase-js';

const metaEnv = (import.meta as any).env || {};
const supabaseUrl = metaEnv.VITE_SUPABASE_URL || '';
const supabaseAnonKey = metaEnv.VITE_SUPABASE_ANON_KEY || '';

// Detect if Supabase is configured with real credentials (not placeholders)
export const isSupabaseConfigured = !!(
  supabaseUrl && 
  supabaseAnonKey && 
  supabaseUrl !== 'https://your-supabase-project.supabase.co' &&
  !supabaseUrl.includes('your-supabase-project')
);

if (!isSupabaseConfigured) {
  console.warn(
    'Roxou Mobilidade: Supabase environment variables are missing or using placeholders. ' +
    'The app will run in offline simulation mode using state persistence. ' +
    'Configure VITE_SUPABASE_URL and VITE_SUPABASE_ANON_KEY for real live operation.'
  );
}

// Safely initialize or return null
export const supabase = isSupabaseConfigured
  ? createClient(supabaseUrl, supabaseAnonKey)
  : null as any;
