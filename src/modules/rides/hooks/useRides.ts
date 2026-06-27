import { useState, useEffect, useCallback } from 'react';
import { supabase, isSupabaseConfigured } from '@/src/services/supabase/supabaseClient';
import { mapRideRequestFromDb, mapProposalFromDb } from '@/src/shared/utils';
import { RideRequest, Proposal } from '@/src/shared/types';

export function useRides(currentUserId?: string, userRole?: 'passenger' | 'driver' | 'admin') {
  const [rides, setRides] = useState<RideRequest[]>([]);
  const [offers, setOffers] = useState<Proposal[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Load initial data
  const loadData = useCallback(async () => {
    if (!currentUserId) {
      setRides([]);
      setOffers([]);
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);
    try {
      if (isSupabaseConfigured && supabase) {
        let fetchedRides: any[] = [];

        // Progressive Loading Strategy for Ride Requests
        // Strategy 1: Attempt join with drivers (through driver_id referencing drivers) and profiles (through passenger_id referencing profiles)
        try {
          let query = supabase.from('ride_requests');
          if (userRole === 'admin') {
            query = query.select('*, passenger:profiles!passenger_id(*), driver:drivers!driver_id(*, profiles:profiles(*), vehicles:vehicles(*))');
          } else if (userRole === 'driver') {
            query = query.select('*, passenger:profiles!passenger_id(*), driver:drivers!driver_id(*, profiles:profiles(*), vehicles:vehicles(*))')
                         .or(`driver_id.eq.${currentUserId},status.in.(pending,negotiating)`);
          } else {
            query = query.select('*, passenger:profiles!passenger_id(*), driver:drivers!driver_id(*, profiles:profiles(*), vehicles:vehicles(*))')
                         .eq('passenger_id', currentUserId);
          }
          
          const { data, error: err } = await query.order('created_at', { ascending: false });
          if (!err && data) {
            fetchedRides = data;
          } else {
            throw err || new Error('Empty data');
          }
        } catch (e: any) {
          console.warn('Strategy 1 (nested driver join) failed, trying Strategy 2 (flat profile join):', e);
          
          // Strategy 2: Join without nested vehicles/drivers if relation Cache is weird
          try {
            let query = supabase.from('ride_requests');
            if (userRole === 'admin') {
              query = query.select('*, passenger:profiles!passenger_id(*), driver:profiles!driver_id(*)');
            } else if (userRole === 'driver') {
              query = query.select('*, passenger:profiles!passenger_id(*), driver:profiles!driver_id(*)')
                           .or(`driver_id.eq.${currentUserId},status.in.(pending,negotiating)`);
            } else {
              query = query.select('*, passenger:profiles!passenger_id(*), driver:profiles!driver_id(*)')
                           .eq('passenger_id', currentUserId);
            }
            
            const { data, error: err2 } = await query.order('created_at', { ascending: false });
            if (!err2 && data) {
              fetchedRides = data;
            } else {
              throw err2 || new Error('Empty data');
            }
          } catch (e2: any) {
            console.warn('Strategy 2 (flat profile join) failed, falling back to Strategy 3 (flat select with manual batch enrichment):', e2);
            
            // Strategy 3: Flat select with manual enrich
            try {
              let query = supabase.from('ride_requests').select('*');
              if (userRole === 'driver') {
                query = query.or(`driver_id.eq.${currentUserId},status.in.(pending,negotiating)`);
              } else if (userRole !== 'admin') {
                query = query.eq('passenger_id', currentUserId);
              }
              
              const { data, error: err3 } = await query.order('created_at', { ascending: false });
              if (err3) throw err3;
              
              const ridesList = data || [];
              if (ridesList.length > 0) {
                try {
                  const passengerIds = Array.from(new Set(ridesList.map((r: any) => r.passenger_id).filter(Boolean)));
                  const driverIds = Array.from(new Set(ridesList.map((r: any) => r.driver_id).filter(Boolean)));

                  const [passengersRes, driversRes, vehiclesRes] = await Promise.all([
                    passengerIds.length > 0 ? supabase.from('profiles').select('*').in('id', passengerIds) : Promise.resolve({ data: [] }),
                    driverIds.length > 0 ? supabase.from('profiles').select('*').in('id', driverIds) : Promise.resolve({ data: [] }),
                    driverIds.length > 0 ? supabase.from('vehicles').select('*').in('driver_id', driverIds) : Promise.resolve({ data: [] })
                  ]);

                  const passengersMap = new Map(passengersRes.data?.map((p: any) => [p.id, p]));
                  const driversMap = new Map(driversRes.data?.map((d: any) => [d.id, d]));
                  const vehiclesMap = new Map(vehiclesRes.data?.map((v: any) => [v.driver_id, v]));

                  fetchedRides = ridesList.map((r: any) => ({
                    ...r,
                    passenger: passengersMap.get(r.passenger_id),
                    driver: driversMap.get(r.driver_id) ? {
                      profiles: driversMap.get(r.driver_id),
                      vehicles: vehiclesMap.get(r.driver_id)
                    } : undefined
                  }));
                } catch (enrichErr) {
                  console.error('Error manually enriching rides:', enrichErr);
                  fetchedRides = ridesList;
                }
              } else {
                fetchedRides = [];
              }
            } catch (err3: any) {
              console.warn('Strategy 3 flat select failed:', err3);
              throw err3;
            }
          }
        }

        const mappedRides = fetchedRides.map(mapRideRequestFromDb);
        setRides(mappedRides);

        // Progressive Loading Strategy for Ride Offers
        let fetchedOffers: any[] = [];
        
        // Strategy 1: Attempt nested join
        try {
          const { data, error: offersErr1 } = await supabase
            .from('ride_offers')
            .select('*, driver:drivers!driver_id(*, profiles:profiles(*), vehicles:vehicles(*))');
          if (!offersErr1 && data) {
            fetchedOffers = data;
          } else {
            throw offersErr1 || new Error('Empty data');
          }
        } catch (oe1) {
          console.warn('Strategy 1 (nested offer join) failed, trying Strategy 2:', oe1);
          
          // Strategy 2: Attempt simple join
          try {
            const { data, error: offersErr2 } = await supabase
              .from('ride_offers')
              .select('*, driver:profiles!driver_id(*)');
            if (!offersErr2 && data) {
              fetchedOffers = data;
            } else {
              throw offersErr2 || new Error('Empty data');
            }
          } catch (oe2) {
            console.warn('Strategy 2 (simple offer join) failed, trying Strategy 3 (manual enrich):', oe2);
            
            // Strategy 3: Manual enrich
            try {
              const { data: rawOffersList, error: offersErr3 } = await supabase
                .from('ride_offers')
                .select('*');
              if (offersErr3) throw offersErr3;
              
              const list = rawOffersList || [];
              if (list.length > 0) {
                const driverIds = Array.from(new Set(list.map((o: any) => o.driver_id).filter(Boolean)));
                const [driversRes, vehiclesRes] = await Promise.all([
                  driverIds.length > 0 ? supabase.from('profiles').select('*').in('id', driverIds) : Promise.resolve({ data: [] }),
                  driverIds.length > 0 ? supabase.from('vehicles').select('*').in('driver_id', driverIds) : Promise.resolve({ data: [] })
                ]);

                const driversMap = new Map(driversRes.data?.map((d: any) => [d.id, d]));
                const vehiclesMap = new Map(vehiclesRes.data?.map((v: any) => [v.driver_id, v]));

                fetchedOffers = list.map((o: any) => ({
                  ...o,
                  driver: driversMap.get(o.driver_id) ? {
                    profiles: driversMap.get(o.driver_id),
                    vehicles: vehiclesMap.get(o.driver_id)
                  } : undefined
                }));
              } else {
                fetchedOffers = [];
              }
            } catch (oe3) {
              console.warn('All offer fetching strategies failed:', oe3);
              throw oe3;
            }
          }
        }

        const mappedOffers = fetchedOffers.map(mapProposalFromDb);
        setOffers(mappedOffers);
      } else {
        // Fallback local storage
        const savedRides = localStorage.getItem('roxou_db_rides');
        const savedOffers = localStorage.getItem('roxou_db_offers');
        setRides(savedRides ? JSON.parse(savedRides) : []);
        setOffers(savedOffers ? JSON.parse(savedOffers) : []);
      }
    } catch (err: any) {
      console.warn('Supabase query failed, falling back to offline mode storage:', err);
      // Fallback local storage
      const savedRides = localStorage.getItem('roxou_db_rides');
      const savedOffers = localStorage.getItem('roxou_db_offers');
      setRides(savedRides ? JSON.parse(savedRides) : []);
      setOffers(savedOffers ? JSON.parse(savedOffers) : []);
      setError(null);
    } finally {
      setLoading(false);
    }
  }, [currentUserId, userRole]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Realtime Supabase Subscription
  useEffect(() => {
    if (!isSupabaseConfigured || !supabase) {
      // Local fallback sync: poll to keep tab synced
      const interval = setInterval(() => {
        const savedRides = localStorage.getItem('roxou_db_rides');
        const savedOffers = localStorage.getItem('roxou_db_offers');
        setRides(savedRides ? JSON.parse(savedRides) : []);
        setOffers(savedOffers ? JSON.parse(savedOffers) : []);
      }, 2000);
      return () => clearInterval(interval);
    }

    const channel = supabase
      .channel('realtime_rides_changes_channel')
      .on(
        'postgres_changes',
        { event: '*', schema: 'public', table: 'ride_requests' },
        (payload) => {
          console.log('Realtime Ride Request change, refreshing:', payload);
          loadData();
        }
      )
      .on(
        'postgres_changes',
        { event: '*', schema: 'public', table: 'ride_offers' },
        (payload) => {
          console.log('Realtime Ride Offer change, refreshing:', payload);
          loadData();
        }
      )
      .subscribe();

    return () => {
      supabase.removeChannel(channel);
    };
  }, [loadData]);

  return {
    rides,
    offers,
    loading,
    error,
    refresh: loadData,
  };
}
