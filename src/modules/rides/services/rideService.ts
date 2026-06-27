import { supabase, isSupabaseConfigured } from '@/src/services/supabase/supabaseClient';
import { DbRideRequest, DbRideOffer, DbRideStatus, DbOfferStatus } from '@/src/shared/types';

/**
 * RideService manages ride requests, negotiations, offers, status transitions, and chat.
 * Supports both real Supabase integration and mock persistence fallback.
 */

// Local fallback state in case Supabase is not connected yet
const getLocalRides = (): DbRideRequest[] => {
  const data = localStorage.getItem('roxou_db_rides');
  return data ? JSON.parse(data) : [];
};

const saveLocalRides = (rides: DbRideRequest[]) => {
  localStorage.setItem('roxou_db_rides', JSON.stringify(rides));
};

const getLocalOffers = (): DbRideOffer[] => {
  const data = localStorage.getItem('roxou_db_offers');
  return data ? JSON.parse(data) : [];
};

const saveLocalOffers = (offers: DbRideOffer[]) => {
  localStorage.setItem('roxou_db_offers', JSON.stringify(offers));
};

export const rideService = {
  /**
   * 1. Create a ride request
   */
  async createRideRequest(request: Omit<DbRideRequest, 'id' | 'created_at' | 'status' | 'sos_triggered'>): Promise<DbRideRequest> {
    if (isSupabaseConfigured) {
      const { data, error } = await supabase
        .from('ride_requests')
        .insert([{
          ...request,
          status: 'pending' as DbRideStatus,
          sos_triggered: false,
        }])
        .select()
        .single();

      if (error) {
        console.error('Error creating ride request in Supabase:', error);
        throw error;
      }
      return data as DbRideRequest;
    } else {
      const newRequest: DbRideRequest = {
        ...request,
        id: crypto.randomUUID(),
        status: 'pending',
        sos_triggered: false,
        created_at: new Date().toISOString(),
      };
      const rides = getLocalRides();
      rides.push(newRequest);
      saveLocalRides(rides);
      return newRequest;
    }
  },

  /**
   * 2. Get ride requests for a specific passenger or driver
   */
  async getMyRideRequests(userId: string, role: 'passenger' | 'driver'): Promise<DbRideRequest[]> {
    if (isSupabaseConfigured) {
      const column = role === 'passenger' ? 'passenger_id' : 'driver_id';
      const { data, error } = await supabase
        .from('ride_requests')
        .select('*')
        .eq(column, userId)
        .order('created_at', { ascending: false });

      if (error) {
        console.error('Error fetching my ride requests from Supabase:', error);
        throw error;
      }
      return data as DbRideRequest[];
    } else {
      const rides = getLocalRides();
      return rides.filter(r => role === 'passenger' ? r.passenger_id === userId : r.driver_id === userId);
    }
  },

  /**
   * 3. Get all active requests available for drivers in the area
   */
  async getAvailableRideRequestsForDriver(): Promise<DbRideRequest[]> {
    if (isSupabaseConfigured) {
      const { data, error } = await supabase
        .from('ride_requests')
        .select('*')
        .in('status', ['pending', 'negotiating'])
        .is('driver_id', null)
        .order('created_at', { ascending: false });

      if (error) {
        console.error('Error fetching available requests from Supabase:', error);
        throw error;
      }
      return data as DbRideRequest[];
    } else {
      const rides = getLocalRides();
      return rides.filter(r => (r.status === 'pending' || r.status === 'negotiating') && !r.driver_id);
    }
  },

  /**
   * 4. Create counter-proposal offer for a ride request
   */
  async createRideOffer(offer: Omit<DbRideOffer, 'id' | 'created_at' | 'status'>): Promise<DbRideOffer> {
    if (isSupabaseConfigured) {
      await supabase
        .from('ride_requests')
        .update({ status: 'negotiating' as DbRideStatus })
        .eq('id', offer.request_id);

      const { data, error } = await supabase
        .from('ride_offers')
        .insert([{
          ...offer,
          status: 'pending' as DbOfferStatus
        }])
        .select()
        .single();

      if (error) {
        console.error('Error creating ride offer in Supabase:', error);
        throw error;
      }
      return data as DbRideOffer;
    } else {
      const rides = getLocalRides();
      const rideIndex = rides.findIndex(r => r.id === offer.request_id);
      if (rideIndex !== -1) {
        rides[rideIndex].status = 'negotiating';
        saveLocalRides(rides);
      }

      const newOffer: DbRideOffer = {
        ...offer,
        id: crypto.randomUUID(),
        status: 'pending',
        created_at: new Date().toISOString(),
      };
      const offers = getLocalOffers();
      offers.push(newOffer);
      saveLocalOffers(offers);
      return newOffer;
    }
  },

  /**
   * 5. Accept an offer (by passenger)
   */
  async acceptRideOffer(offerId: string): Promise<DbRideRequest> {
    if (isSupabaseConfigured) {
      const { data: offerData, error: offerError } = await supabase
        .from('ride_offers')
        .update({ status: 'accepted' as DbOfferStatus })
        .eq('id', offerId)
        .select()
        .single();

      if (offerError || !offerData) {
        console.error('Error accepting ride offer in Supabase:', offerError);
        throw offerError || new Error('Offer not found');
      }

      await supabase
        .from('ride_offers')
        .update({ status: 'rejected' as DbOfferStatus })
        .eq('request_id', offerData.request_id)
        .neq('id', offerId);

      const { data: requestData, error: requestError } = await supabase
        .from('ride_requests')
        .update({
          status: 'accepted' as DbRideStatus,
          driver_id: offerData.driver_id,
          final_price: offerData.price
        })
        .eq('id', offerData.request_id)
        .select()
        .single();

      if (requestError) {
        console.error('Error updating ride request with accepted driver:', requestError);
        throw requestError;
      }
      return requestData as DbRideRequest;
    } else {
      const offers = getLocalOffers();
      const offerIndex = offers.findIndex(o => o.id === offerId);
      if (offerIndex === -1) throw new Error('Offer not found');

      offers[offerIndex].status = 'accepted';
      const acceptedOffer = offers[offerIndex];

      offers.forEach(o => {
        if (o.request_id === acceptedOffer.request_id && o.id !== offerId) {
          o.status = 'rejected';
        }
      });
      saveLocalOffers(offers);

      const rides = getLocalRides();
      const rideIndex = rides.findIndex(r => r.id === acceptedOffer.request_id);
      if (rideIndex === -1) throw new Error('Ride request not found');

      rides[rideIndex].status = 'accepted';
      rides[rideIndex].driver_id = acceptedOffer.driver_id;
      rides[rideIndex].final_price = acceptedOffer.price;
      saveLocalRides(rides);

      return rides[rideIndex];
    }
  },

  /**
   * 5b. Accept a ride request directly (by driver at suggested price)
   */
  async acceptRideRequestDirectly(requestId: string, driverId: string, price: number): Promise<DbRideRequest> {
    if (isSupabaseConfigured) {
      const { data, error } = await supabase
        .from('ride_requests')
        .update({
          status: 'accepted' as DbRideStatus,
          driver_id: driverId,
          final_price: price
        })
        .eq('id', requestId)
        .select()
        .single();

      if (error) {
        console.error('Error accepting ride request directly in Supabase:', error);
        throw error;
      }
      return data as DbRideRequest;
    } else {
      const rides = getLocalRides();
      const index = rides.findIndex(r => r.id === requestId);
      if (index === -1) throw new Error('Ride request not found');

      rides[index].status = 'accepted';
      rides[index].driver_id = driverId;
      rides[index].final_price = price;
      saveLocalRides(rides);
      return rides[index];
    }
  },

  /**
   * 5c. Decline/Reject a ride offer
   */
  async declineRideOffer(offerId: string): Promise<void> {
    if (isSupabaseConfigured) {
      const { error } = await supabase
        .from('ride_offers')
        .update({ status: 'rejected' as DbOfferStatus })
        .eq('id', offerId);

      if (error) {
        console.error('Error declining offer in Supabase:', error);
        throw error;
      }
    } else {
      const offers = getLocalOffers();
      const index = offers.findIndex(o => o.id === offerId);
      if (index !== -1) {
        offers[index].status = 'rejected';
        saveLocalOffers(offers);
      }
    }
  },

  /**
   * 6. Cancel a ride request
   */
  async cancelRideRequest(requestId: string, cancelledBy: 'passenger' | 'driver'): Promise<DbRideRequest> {
    const status: DbRideStatus = cancelledBy === 'passenger' ? 'cancelled_by_passenger' : 'cancelled_by_driver';

    if (isSupabaseConfigured) {
      const { data, error } = await supabase
        .from('ride_requests')
        .update({ status })
        .eq('id', requestId)
        .select()
        .single();

      if (error) {
        console.error('Error cancelling ride request in Supabase:', error);
        throw error;
      }
      return data as DbRideRequest;
    } else {
      const rides = getLocalRides();
      const index = rides.findIndex(r => r.id === requestId);
      if (index === -1) throw new Error('Ride request not found');

      rides[index].status = status;
      saveLocalRides(rides);
      return rides[index];
    }
  },

  /**
   * 7. Complete the ride successfully
   */
  async completeRide(requestId: string): Promise<DbRideRequest> {
    if (isSupabaseConfigured) {
      const { data, error } = await supabase
        .from('ride_requests')
        .update({ status: 'completed' as DbRideStatus })
        .eq('id', requestId)
        .select()
        .single();

      if (error) {
        console.error('Error completing ride in Supabase:', error);
        throw error;
      }
      return data as DbRideRequest;
    } else {
      const rides = getLocalRides();
      const index = rides.findIndex(r => r.id === requestId);
      if (index === -1) throw new Error('Ride request not found');

      rides[index].status = 'completed';
      saveLocalRides(rides);
      return rides[index];
    }
  }
};
