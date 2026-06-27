export interface User {
  id: string;
  email: string;
  name: string;
  phone: string;
  phoneVerified?: boolean;
  cpf?: string;
  cpfVerified?: boolean;
  role: 'passenger' | 'driver' | 'admin';
  status: 'pending_approval' | 'approved' | 'rejected' | 'offline' | 'online' | 'busy';
  rating: number;
  totalRides: number;
  avatar: string;
  cnh?: string;
  cnhVerified?: boolean;
  vehicleDocVerified?: boolean;
  vehicleInfo?: {
    model: string;
    plate: string;
    color: string;
    year: string;
  };
}

export interface LocationPoint {
  id: string;
  name: string;
  address: string;
  lat: number; // 0 to 100 for SVG canvas
  lng: number; // 0 to 100 for SVG canvas
  bairro?: string; // Presidente Prudente specific neighbourhood
}

export interface RideRequest {
  id: string;
  passengerId: string;
  passengerName: string;
  passengerRating: number;
  passengerAvatar: string;
  origin: LocationPoint;
  destination: LocationPoint;
  stops?: LocationPoint[]; // Intermediate stops (Paradas)
  distanceKm: number;
  suggestedPrice: number;
  finalPrice?: number;
  paymentMethod: string;
  passengerCount: number;
  observation?: string;
  scheduledTime?: string; // Agendamento
  status: 'requested' | 'negotiating' | 'accepted' | 'in_progress' | 'completed' | 'cancelled';
  createdAt: string;
  driverId?: string;
  driverName?: string;
  driverRating?: number;
  driverAvatar?: string;
  driverPhone?: string;
  vehicleInfo?: string;
  safetyCode: string; // 4-digit PIN de embarque
  chatMessages?: ChatMessage[];
  sosTriggered?: boolean;
}

export interface Proposal {
  id: string;
  requestId: string;
  driverId: string;
  driverName: string;
  driverRating: number;
  driverAvatar: string;
  vehicleInfo: string;
  price: number;
  status: 'pending' | 'accepted' | 'declined';
  createdAt: string;
}

export interface ChatMessage {
  id: string;
  senderRole: 'passenger' | 'driver';
  text: string;
  timestamp: string;
}

export interface AppSystemSettings {
  minRideValue: number;
  pricePerKm: number;
  commissionPercent: number;
}

export interface Review {
  id: string;
  rideId: string;
  reviewerId: string;
  rating: number;
  comment: string;
  reviewerRole: 'passenger' | 'driver';
  createdAt: string;
}

// -------------------------------------------------------------------
// SUPABASE REAL DATABASE TYPINGS
// -------------------------------------------------------------------

export type DbUserRole = 'passenger' | 'driver' | 'admin';
export type DbDriverStatus = 'pending' | 'approved' | 'rejected' | 'blocked';
export type DbRideStatus = 
  | 'pending'
  | 'negotiating'
  | 'accepted'
  | 'driver_arriving'
  | 'in_progress'
  | 'completed'
  | 'cancelled_by_passenger'
  | 'cancelled_by_driver'
  | 'problem_reported';
export type DbOfferStatus = 'pending' | 'accepted' | 'rejected' | 'expired';
export type DbPaymentMethod = 'pix' | 'cash' | 'card_external';
export type DbReportStatus = 'open' | 'reviewing' | 'resolved' | 'rejected';

export interface DbProfile {
  id: string; // UUID
  email: string;
  name: string;
  phone?: string;
  phone_verified: boolean;
  cpf?: string;
  cpf_verified: boolean;
  role: DbUserRole;
  rating: number;
  total_rides: number;
  avatar?: string;
  created_at: string;
}

export interface DbDriver {
  id: string; // UUID references DbProfile.id
  status: DbDriverStatus;
  cnh?: string;
  cnh_verified: boolean;
  vehicle_doc_verified: boolean;
  created_at: string;
}

export interface DbVehicle {
  id: string; // UUID
  driver_id: string; // UUID
  model: string;
  plate: string;
  color: string;
  year: number;
  created_at: string;
}

export interface DbRideRequest {
  id: string; // UUID
  passenger_id: string; // UUID
  origin_name: string;
  origin_address: string;
  origin_lat: number;
  origin_lng: number;
  origin_bairro?: string;
  destination_name: string;
  destination_address: string;
  destination_lat: number;
  destination_lng: number;
  destination_bairro?: string;
  stops: any; // JSONB Array
  distance_km: number;
  suggested_price: number;
  final_price?: number;
  payment_method: DbPaymentMethod;
  passenger_count: number;
  observation?: string;
  scheduled_time?: string;
  status: DbRideStatus;
  driver_id?: string; // UUID
  safety_code: string;
  sos_triggered: boolean;
  created_at: string;
}

export interface DbRideOffer {
  id: string; // UUID
  request_id: string; // UUID
  driver_id: string; // UUID
  price: number;
  estimated_time_minutes?: number;
  status: DbOfferStatus;
  created_at: string;
}

export interface DbRideMessage {
  id: string; // UUID
  ride_id: string; // UUID
  sender_id: string; // UUID
  text: string;
  created_at: string;
}

export interface DbDriverLocation {
  id: string; // UUID
  driver_id: string; // UUID
  lat: number;
  lng: number;
  updated_at: string;
}

export interface DbRideReview {
  id: string; // UUID
  ride_id: string; // UUID
  reviewer_id: string; // UUID
  reviewee_id: string; // UUID
  rating: number;
  comment?: string;
  created_at: string;
}

export interface DbRideReport {
  id: string; // UUID
  ride_id?: string; // UUID
  reporter_id: string; // UUID
  description: string;
  status: DbReportStatus;
  created_at: string;
}

export interface DbFavoriteDriver {
  id: string; // UUID
  passenger_id: string; // UUID
  driver_id: string; // UUID
  created_at: string;
}

export interface DbAppSetting {
  id: string;
  min_ride_value: number;
  price_per_km: number;
  commission_percent: number;
  updated_at: string;
}

export interface DbNotification {
  id: string; // UUID
  user_id: string; // UUID
  title: string;
  body: string;
  read: boolean;
  created_at: string;
}
