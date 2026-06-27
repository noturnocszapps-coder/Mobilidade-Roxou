-- Roxou Mobilidade — Banco de dados Supabase (PostgreSQL)
-- Fundação real para produção em Presidente Prudente/SP

-- -------------------------------------------------------------------
-- 1. ENUMS & CUSTOM TYPES
-- -------------------------------------------------------------------
CREATE TYPE user_role AS ENUM ('passenger', 'driver', 'admin');
CREATE TYPE driver_status AS ENUM ('pending', 'approved', 'rejected', 'blocked');
CREATE TYPE ride_status AS ENUM (
  'pending', 
  'negotiating', 
  'accepted', 
  'driver_arriving', 
  'in_progress', 
  'completed', 
  'cancelled_by_passenger', 
  'cancelled_by_driver', 
  'problem_reported'
);
CREATE TYPE offer_status AS ENUM ('pending', 'accepted', 'rejected', 'expired');
CREATE TYPE payment_method AS ENUM ('pix', 'cash', 'card_external');
CREATE TYPE report_status AS ENUM ('open', 'reviewing', 'resolved', 'rejected');

-- Enable UUID Extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- -------------------------------------------------------------------
-- 2. TABLES DEFINITIONS
-- -------------------------------------------------------------------

-- PROFILES: Shared user profiles linked with Supabase Auth
CREATE TABLE profiles (
  id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  email TEXT UNIQUE NOT NULL,
  name TEXT NOT NULL,
  phone TEXT,
  phone_verified BOOLEAN DEFAULT FALSE,
  cpf TEXT,
  cpf_verified BOOLEAN DEFAULT FALSE,
  role user_role DEFAULT 'passenger' NOT NULL,
  rating NUMERIC(3, 2) DEFAULT 5.0,
  total_rides INTEGER DEFAULT 0,
  avatar TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- DRIVERS: Driver-specific credentials
CREATE TABLE drivers (
  id UUID PRIMARY KEY REFERENCES profiles(id) ON DELETE CASCADE,
  status driver_status DEFAULT 'pending' NOT NULL,
  cnh TEXT,
  cnh_verified BOOLEAN DEFAULT FALSE,
  vehicle_doc_verified BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- VEHICLES: Vehicles used by verified drivers
CREATE TABLE vehicles (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  driver_id UUID UNIQUE REFERENCES drivers(id) ON DELETE CASCADE NOT NULL,
  model TEXT NOT NULL,
  plate TEXT NOT NULL,
  color TEXT NOT NULL,
  year INTEGER NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- RIDE_REQUESTS: Trip requests created by passengers
CREATE TABLE ride_requests (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  passenger_id UUID REFERENCES profiles(id) ON DELETE SET NULL NOT NULL,
  origin_name TEXT NOT NULL,
  origin_address TEXT NOT NULL,
  origin_lat NUMERIC(8, 5) NOT NULL,
  origin_lng NUMERIC(8, 5) NOT NULL,
  origin_bairro TEXT, -- Bairro de Presidente Prudente (ex: Centro, Ana Jacinta, Parque do Povo)
  destination_name TEXT NOT NULL,
  destination_address TEXT NOT NULL,
  destination_lat NUMERIC(8, 5) NOT NULL,
  destination_lng NUMERIC(8, 5) NOT NULL,
  destination_bairro TEXT,
  stops JSONB DEFAULT '[]'::jsonb, -- Array de paradas adicionais
  distance_km NUMERIC(5, 2) NOT NULL,
  suggested_price NUMERIC(7, 2) NOT NULL,
  final_price NUMERIC(7, 2),
  payment_method payment_method DEFAULT 'pix' NOT NULL,
  passenger_count INTEGER DEFAULT 1 NOT NULL,
  observation TEXT,
  scheduled_time TIMESTAMP WITH TIME ZONE, -- Agendamento opcional
  status ride_status DEFAULT 'pending' NOT NULL,
  driver_id UUID REFERENCES drivers(id) ON DELETE SET NULL,
  safety_code TEXT NOT NULL, -- PIN de embarque com 4 dígitos
  sos_triggered BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- RIDE_OFFERS: Negotiation offers / counterproposals by drivers
CREATE TABLE ride_offers (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  request_id UUID REFERENCES ride_requests(id) ON DELETE CASCADE NOT NULL,
  driver_id UUID REFERENCES drivers(id) ON DELETE CASCADE NOT NULL,
  price NUMERIC(7, 2) NOT NULL,
  estimated_time_minutes INTEGER,
  status offer_status DEFAULT 'pending' NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- RIDE_MESSAGES: Internal chat messages between passenger and driver in an active ride
CREATE TABLE ride_messages (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  ride_id UUID REFERENCES ride_requests(id) ON DELETE CASCADE NOT NULL,
  sender_id UUID REFERENCES profiles(id) ON DELETE CASCADE NOT NULL,
  text TEXT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- DRIVER_LOCATIONS: Real-time driver coordinates (updated every 30s / 100m)
CREATE TABLE driver_locations (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  driver_id UUID UNIQUE REFERENCES drivers(id) ON DELETE CASCADE NOT NULL,
  lat NUMERIC(8, 5) NOT NULL,
  lng NUMERIC(8, 5) NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- RIDE_REVIEWS: Post-trip reviews (rating + comment)
CREATE TABLE ride_reviews (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  ride_id UUID REFERENCES ride_requests(id) ON DELETE SET NULL NOT NULL,
  reviewer_id UUID REFERENCES profiles(id) ON DELETE CASCADE NOT NULL,
  reviewee_id UUID REFERENCES profiles(id) ON DELETE CASCADE NOT NULL,
  rating NUMERIC(2, 1) NOT NULL,
  comment TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- RIDE_REPORTS: Safety or incident reporting (linked to SOS or complaints)
CREATE TABLE ride_reports (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  ride_id UUID REFERENCES ride_requests(id) ON DELETE SET NULL,
  reporter_id UUID REFERENCES profiles(id) ON DELETE CASCADE NOT NULL,
  description TEXT NOT NULL,
  status report_status DEFAULT 'open' NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- FAVORITE_DRIVERS: Passenger's preferred driver list
CREATE TABLE favorite_drivers (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  passenger_id UUID REFERENCES profiles(id) ON DELETE CASCADE NOT NULL,
  driver_id UUID REFERENCES drivers(id) ON DELETE CASCADE NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
  UNIQUE (passenger_id, driver_id)
);

-- APP_SETTINGS: Admin parameters for commission, minimum prices, and pricing per km
CREATE TABLE app_settings (
  id TEXT PRIMARY KEY,
  min_ride_value NUMERIC(6, 2) DEFAULT 12.00 NOT NULL,
  price_per_km NUMERIC(5, 2) DEFAULT 2.00 NOT NULL,
  commission_percent NUMERIC(4, 2) DEFAULT 15.00 NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- NOTIFICATIONS: Web push notifications history for users
CREATE TABLE notifications (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES profiles(id) ON DELETE CASCADE NOT NULL,
  title TEXT NOT NULL,
  body TEXT NOT NULL,
  read BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);


-- -------------------------------------------------------------------
-- 3. ENABLE ROW LEVEL SECURITY (RLS)
-- -------------------------------------------------------------------
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE drivers ENABLE ROW LEVEL SECURITY;
ALTER TABLE vehicles ENABLE ROW LEVEL SECURITY;
ALTER TABLE ride_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE ride_offers ENABLE ROW LEVEL SECURITY;
ALTER TABLE ride_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE driver_locations ENABLE ROW LEVEL SECURITY;
ALTER TABLE ride_reviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE ride_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE favorite_drivers ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;


-- -------------------------------------------------------------------
-- 4. RLS POLICIES
-- -------------------------------------------------------------------

-- PROFILES policies
CREATE POLICY "Public profiles are viewable by everyone." ON profiles
  FOR SELECT USING (true);

CREATE POLICY "Users can update their own profile." ON profiles
  FOR UPDATE USING (auth.uid() = id);

-- DRIVERS policies
CREATE POLICY "Drivers are viewable by authenticated users." ON drivers
  FOR SELECT USING (auth.role() = 'authenticated');

CREATE POLICY "Drivers can insert their own application." ON drivers
  FOR INSERT WITH CHECK (auth.uid() = id);

CREATE POLICY "Drivers can update their own application." ON drivers
  FOR UPDATE USING (auth.uid() = id);

-- VEHICLES policies
CREATE POLICY "Vehicles are viewable by authenticated users." ON vehicles
  FOR SELECT USING (auth.role() = 'authenticated');

CREATE POLICY "Drivers can insert/update their own vehicles." ON vehicles
  FOR ALL USING (
    driver_id = auth.uid() OR 
    EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin')
  );

-- RIDE_REQUESTS policies
CREATE POLICY "Passengers can manage their own requests." ON ride_requests
  FOR ALL USING (
    passenger_id = auth.uid() OR
    EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin')
  );

CREATE POLICY "Drivers can view active requests." ON ride_requests
  FOR SELECT USING (
    EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role IN ('driver', 'admin'))
  );

CREATE POLICY "Drivers can update rides they are involved in." ON ride_requests
  FOR UPDATE USING (
    driver_id = auth.uid() OR
    EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin')
  );

-- RIDE_OFFERS policies
CREATE POLICY "Passengers can view offers for their requests." ON ride_offers
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM ride_requests r 
      WHERE r.id = request_id AND (r.passenger_id = auth.uid() OR r.driver_id = auth.uid())
    ) OR
    driver_id = auth.uid() OR
    EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin')
  );

CREATE POLICY "Drivers can manage their own offers." ON ride_offers
  FOR ALL USING (
    driver_id = auth.uid() OR
    EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin')
  );

-- RIDE_MESSAGES policies
CREATE POLICY "Users involved can manage messages." ON ride_messages
  FOR ALL USING (
    sender_id = auth.uid() OR
    EXISTS (
      SELECT 1 FROM ride_requests r 
      WHERE r.id = ride_id AND (r.passenger_id = auth.uid() OR r.driver_id = auth.uid())
    )
  );

-- DRIVER_LOCATIONS policies
CREATE POLICY "All authenticated users can see driver locations." ON driver_locations
  FOR SELECT USING (auth.role() = 'authenticated');

CREATE POLICY "Drivers can manage their own locations." ON driver_locations
  FOR ALL USING (
    driver_id = auth.uid()
  );

-- RIDE_REVIEWS policies
CREATE POLICY "Reviews are public for authenticated users." ON ride_reviews
  FOR SELECT USING (auth.role() = 'authenticated');

CREATE POLICY "Reviews can be created by the reviewer." ON ride_reviews
  FOR INSERT WITH CHECK (reviewer_id = auth.uid());

-- RIDE_REPORTS policies
CREATE POLICY "Only admins and reporters can view reports." ON ride_reports
  FOR SELECT USING (
    reporter_id = auth.uid() OR
    EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin')
  );

CREATE POLICY "Users can create reports." ON ride_reports
  FOR INSERT WITH CHECK (reporter_id = auth.uid());

-- FAVORITE_DRIVERS policies
CREATE POLICY "Passengers can manage their favorites." ON favorite_drivers
  FOR ALL USING (passenger_id = auth.uid());

-- APP_SETTINGS policies
CREATE POLICY "App settings are viewable by authenticated users." ON app_settings
  FOR SELECT USING (auth.role() = 'authenticated');

CREATE POLICY "Only admins can modify app settings." ON app_settings
  FOR ALL USING (
    EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin')
  );

-- NOTIFICATIONS policies
CREATE POLICY "Users can view and manage their own notifications." ON notifications
  FOR ALL USING (user_id = auth.uid());
