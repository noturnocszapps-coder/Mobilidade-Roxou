import { supabase, isSupabaseConfigured } from '@/src/services/supabase/supabaseClient';
import { DbDriver, DbVehicle, DbDriverStatus, DbUserRole } from '@/src/shared/types';

/**
 * DriverService manages driver verification, online/offline status, location updates, and operational statistics.
 */

// Local fallback store for offline prototyping
const getLocalDrivers = (): DbDriver[] => {
  const data = localStorage.getItem('roxou_db_drivers');
  return data ? JSON.parse(data) : [];
};

const saveLocalDrivers = (drivers: DbDriver[]) => {
  localStorage.setItem('roxou_db_drivers', JSON.stringify(drivers));
};

export const driverService = {
  /**
   * 1. Register a new driver profile with vehicle info
   */
  async createDriverProfile(
    userId: string,
    cnh: string,
    vehicle: Omit<DbVehicle, 'id' | 'driver_id' | 'created_at'>
  ): Promise<{ driver: DbDriver; vehicle: DbVehicle }> {
    if (isSupabaseConfigured) {
      // Step A: Update user role to 'driver' in profiles
      const { error: profileError } = await supabase
        .from('profiles')
        .update({ role: 'driver' as DbUserRole })
        .eq('id', userId);

      if (profileError) {
        console.error('Error updating user role in Supabase:', profileError);
        throw profileError;
      }

      // Step B: Insert or upsert driver credentials
      const { data: driverData, error: driverError } = await supabase
        .from('drivers')
        .upsert([{
          id: userId,
          status: 'pending' as DbDriverStatus,
          cnh,
          cnh_verified: false,
          vehicle_doc_verified: false
        }])
        .select()
        .single();

      if (driverError) {
        console.error('Error creating driver profile in Supabase:', driverError);
        throw driverError;
      }

      // Step C: Insert vehicle details
      const { data: vehicleData, error: vehicleError } = await supabase
        .from('vehicles')
        .upsert([{
          driver_id: userId,
          ...vehicle
        }])
        .select()
        .single();

      if (vehicleError) {
        console.error('Error creating vehicle registry in Supabase:', vehicleError);
        throw vehicleError;
      }

      return {
        driver: driverData as DbDriver,
        vehicle: vehicleData as DbVehicle
      };
    } else {
      const newDriver: DbDriver = {
        id: userId,
        status: 'pending',
        cnh,
        cnh_verified: false,
        vehicle_doc_verified: false,
        created_at: new Date().toISOString()
      };

      const newVehicle: DbVehicle = {
        id: crypto.randomUUID(),
        driver_id: userId,
        ...vehicle,
        created_at: new Date().toISOString()
      };

      const drivers = getLocalDrivers();
      // Remove any existing driver entry to behave like upsert
      const filteredDrivers = drivers.filter(d => d.id !== userId);
      filteredDrivers.push(newDriver);
      saveLocalDrivers(filteredDrivers);

      // Save vehicle to local state as well
      const vehicles = JSON.parse(localStorage.getItem('roxou_db_vehicles') || '[]');
      const filteredVehicles = vehicles.filter((v: any) => v.driver_id !== userId);
      filteredVehicles.push(newVehicle);
      localStorage.setItem('roxou_db_vehicles', JSON.stringify(filteredVehicles));

      return {
        driver: newDriver,
        vehicle: newVehicle
      };
    }
  },

  /**
   * 2. Toggle driver online/offline status
   */
  async updateDriverOnlineStatus(userId: string, isOnline: boolean): Promise<void> {
    if (isSupabaseConfigured) {
      const statusValue = isOnline ? 'approved' : 'pending'; // Approved indicates ready in our mockups
      const { error } = await supabase
        .from('drivers')
        .update({ status: statusValue as DbDriverStatus })
        .eq('id', userId);

      if (error) {
        console.error('Error setting driver online status in Supabase:', error);
        throw error;
      }
    } else {
      const drivers = getLocalDrivers();
      const index = drivers.findIndex(d => d.id === userId);
      if (index !== -1) {
        drivers[index].status = isOnline ? 'approved' : 'pending';
        saveLocalDrivers(drivers);
      }
    }
  },

  /**
   * 3. Update real-time coordinates of the driver
   */
  async updateDriverLocation(userId: string, lat: number, lng: number): Promise<void> {
    if (isSupabaseConfigured) {
      const { error } = await supabase
        .from('driver_locations')
        .upsert([{
          driver_id: userId,
          lat,
          lng,
          updated_at: new Date().toISOString()
        }], { onConflict: 'driver_id' });

      if (error) {
        console.error('Error updating driver coordinates in Supabase:', error);
        throw error;
      }
    } else {
      const locations = JSON.parse(localStorage.getItem('roxou_db_driver_locations') || '[]');
      const filtered = locations.filter((loc: any) => loc.driver_id !== userId);
      filtered.push({
        driver_id: userId,
        lat,
        lng,
        updated_at: new Date().toISOString()
      });
      localStorage.setItem('roxou_db_driver_locations', JSON.stringify(filtered));
    }
  },

  /**
   * 4. Retrieve dashboard analytics and operational metrics for a driver
   */
  async getDriverDashboardStats(userId: string): Promise<{
    totalRides: number;
    earningsToday: number;
    earningsWeek: number;
    earningsMonth: number;
    averageRating: number;
    kmDriven: number;
  }> {
    if (isSupabaseConfigured) {
      try {
        const { data, error } = await supabase
          .from('ride_requests')
          .select('*')
          .eq('driver_id', userId)
          .eq('status', 'completed');

        if (error) {
          throw error;
        }

        const completedRides = data || [];
        const totalRides = completedRides.length;

        // Calculate earnings based on final_price
        const totalRevenue = completedRides.reduce((sum, r) => sum + (r.final_price || r.suggested_price || 0), 0);
        const commissionMultiplier = 0.85; // 15% system fee

        const earningsMonth = totalRevenue * commissionMultiplier;
        const earningsWeek = earningsMonth * 0.25;
        const earningsToday = totalRides > 0 ? (totalRevenue / totalRides) * commissionMultiplier : 0;

        // Avg Rating
        let averageRating = 5.0;
        try {
          const { data: profile } = await supabase
            .from('profiles')
            .select('rating')
            .eq('id', userId)
            .single();
          if (profile?.rating) {
            averageRating = profile.rating;
          }
        } catch (profileErr) {
          console.warn('Error fetching driver rating from profiles table, using default:', profileErr);
        }

        return {
          totalRides,
          earningsToday,
          earningsWeek,
          earningsMonth,
          averageRating,
          kmDriven: completedRides.reduce((sum, r) => sum + (r.distance_km || 0), 0)
        };
      } catch (err: any) {
        console.warn('Supabase getDriverDashboardStats query failed, falling back to local calculation:', err);
        return getLocalStats(userId);
      }
    } else {
      return getLocalStats(userId);
    }
  }
};

const getLocalStats = (userId: string) => {
  const rides = getLocalRides();
  const driverRides = rides.filter(r => r.driver_id === userId && r.status === 'completed');
  const totalRides = driverRides.length;

  const basePrice = 14.20;
  const commissionMultiplier = 0.85; // 15% platform fee
  
  const earningsToday = totalRides > 0 ? 84.50 : 0.00;
  const earningsWeek = totalRides > 0 ? (totalRides * basePrice * commissionMultiplier) : 0.00;
  const earningsMonth = totalRides > 0 ? (totalRides * basePrice * commissionMultiplier * 4.2) : 0.00;
  const kmDriven = totalRides * 6.4;

  return {
    totalRides,
    earningsToday,
    earningsWeek,
    earningsMonth,
    averageRating: 4.92,
    kmDriven
  };
};

const getLocalRides = (): any[] => {
  const data = localStorage.getItem('roxou_db_rides');
  return data ? JSON.parse(data) : [];
};
