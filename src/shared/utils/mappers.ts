import { LocationPoint, RideRequest, Proposal } from '../types';

export const mapPaymentMethodToDb = (method: string): 'pix' | 'cash' | 'card_external' => {
  if (method === 'Pix') return 'pix';
  if (method === 'Dinheiro') return 'cash';
  return 'card_external';
};

export const mapPaymentMethodFromDb = (method: string): string => {
  if (method === 'pix') return 'Pix';
  if (method === 'cash') return 'Dinheiro';
  return 'Cartão';
};

export const mapStatusToClient = (status: string): 'requested' | 'negotiating' | 'accepted' | 'in_progress' | 'completed' | 'cancelled' => {
  if (status === 'pending') return 'requested';
  if (status === 'negotiating') return 'negotiating';
  if (status === 'accepted' || status === 'driver_arriving') return 'accepted';
  if (status === 'in_progress') return 'in_progress';
  if (status === 'completed') return 'completed';
  return 'cancelled';
};

export const mapStatusToDb = (status: string): any => {
  if (status === 'requested') return 'pending';
  if (status === 'negotiating') return 'negotiating';
  if (status === 'accepted') return 'accepted';
  if (status === 'in_progress') return 'in_progress';
  if (status === 'completed') return 'completed';
  return 'cancelled_by_passenger';
};

export const mapRideRequestFromDb = (db: any): RideRequest => {
  const passengerProfile = db.passenger;
  const driverData = db.driver;
  
  // Driver data could be a profiles object directly if query joins differently, or nested drivers->profiles
  let driverProfile = driverData?.profiles || driverData?.profile || (driverData?.name ? driverData : null);
  if (Array.isArray(driverProfile)) {
    driverProfile = driverProfile[0];
  }
  let vehicleData = driverData?.vehicles || driverData?.vehicle;
  if (Array.isArray(vehicleData)) {
    vehicleData = vehicleData[0];
  }

  return {
    id: db.id,
    passengerId: db.passenger_id,
    passengerName: passengerProfile?.name || db.passenger_name || 'Passageiro',
    passengerRating: passengerProfile?.rating ? parseFloat(passengerProfile.rating) : (db.passenger_rating || 4.9),
    passengerAvatar: passengerProfile?.avatar || db.passenger_avatar || `https://api.dicebear.com/7.x/adventurer/svg?seed=${db.passenger_id || 'passenger'}`,
    origin: {
      id: 'orig_' + db.id,
      name: db.origin_name,
      address: db.origin_address || '',
      lat: Number(db.origin_lat),
      lng: Number(db.origin_lng),
      bairro: db.origin_bairro
    },
    destination: {
      id: 'dest_' + db.id,
      name: db.destination_name,
      address: db.destination_address || '',
      lat: Number(db.destination_lat),
      lng: Number(db.destination_lng),
      bairro: db.destination_bairro
    },
    distanceKm: Number(db.distance_km),
    suggestedPrice: Number(db.suggested_price),
    finalPrice: db.final_price ? Number(db.final_price) : undefined,
    paymentMethod: mapPaymentMethodFromDb(db.payment_method),
    passengerCount: db.passenger_count,
    observation: db.observation || undefined,
    status: mapStatusToClient(db.status),
    createdAt: db.created_at,
    driverId: db.driver_id || undefined,
    driverName: driverProfile?.name || db.driver_name || undefined,
    driverRating: driverProfile?.rating ? parseFloat(driverProfile.rating) : (db.driver_rating || undefined),
    driverAvatar: driverProfile?.avatar || db.driver_avatar || undefined,
    driverPhone: driverProfile?.phone || db.driver_phone || undefined,
    vehicleInfo: db.vehicle_info || (vehicleData ? `${vehicleData.model} ${vehicleData.color} (${vehicleData.plate})` : undefined),
    safetyCode: db.safety_code || '1234',
    sosTriggered: db.sos_triggered || false,
    chatMessages: db.chatMessages || []
  };
};

export const mapProposalFromDb = (db: any): Proposal => {
  let driverProfile = db.driver?.profiles || db.driver?.profile || (db.driver?.name ? db.driver : null);
  if (Array.isArray(driverProfile)) {
    driverProfile = driverProfile[0];
  }
  let vehicleData = db.driver?.vehicles || db.driver?.vehicle;
  if (Array.isArray(vehicleData)) {
    vehicleData = vehicleData[0];
  }

  return {
    id: db.id,
    requestId: db.request_id,
    driverId: db.driver_id,
    driverName: driverProfile?.name || db.driver_name || 'Motorista',
    driverRating: driverProfile?.rating ? parseFloat(driverProfile.rating) : (db.driver_rating || 4.9),
    driverAvatar: driverProfile?.avatar || db.driver_avatar || `https://api.dicebear.com/7.x/adventurer/svg?seed=${db.driver_id || 'driver'}`,
    vehicleInfo: db.vehicle_info || (vehicleData ? `${vehicleData.model} ${vehicleData.color} (${vehicleData.plate})` : 'Veículo Roxou'),
    price: Number(db.price),
    status: db.status === 'accepted' ? 'accepted' : db.status === 'rejected' ? 'declined' : 'pending',
    createdAt: db.created_at
  };
};
