import { LocationPoint, AppSystemSettings, User } from '../types';

export const PRESIDENTE_PRUDENTE_LOCATIONS: LocationPoint[] = [
  { id: '1', name: 'Parque do Povo', address: 'Av. 14 de Setembro, Presidente Prudente - SP', lat: -22.1225, lng: -51.4078, bairro: 'Parque do Povo' },
  { id: '2', name: 'Prudenshopping', address: 'Av. Manoel Goulart, 2400 - Jd. Alvorada', lat: -22.1152, lng: -51.4070, bairro: 'Prudenshopping' },
  { id: '3', name: 'Terminal Urbano Central', address: 'Av. Brasil, Centro, Presidente Prudente - SP', lat: -22.1215, lng: -51.3881, bairro: 'Centro' },
  { id: '4', name: 'Conjunto Brasil Novo', address: 'Av. Roberto Simonsen, Brasil Novo', lat: -22.0833, lng: -51.4012, bairro: 'Brasil Novo' },
  { id: '5', name: 'Aeroporto Estadual de Prudente', address: 'Rodovia Assis Chateaubriand, km 459', lat: -22.1758, lng: -51.4244, bairro: 'Aeroporto' },
  { id: '6', name: 'UBS Ana Jacinta', address: 'Av. Oswald de Andrade, Jd. Ana Jacinta', lat: -22.1611, lng: -51.4419, bairro: 'Ana Jacinta' },
  { id: '7', name: 'Residencial Morumbi', address: 'Rua Siqueira Campos, Morumbi', lat: -22.1120, lng: -51.3780, bairro: 'Morumbi' },
  { id: '8', name: 'Jardim Bongiovani (Toledo)', address: 'Rua Alvino Gomes Teixeira, Bongiovani', lat: -22.1332, lng: -51.4111, bairro: 'Bongiovani' },
  { id: '9', name: 'Centro Cultural Matarazzo', address: 'Rua Quintino Bocaiúva, 749 - Vila Marcondes', lat: -22.1172, lng: -51.3833, bairro: 'Vila Marcondes' },
  { id: '10', name: 'Cohab / Cica', address: 'Av. Washington Luiz, Jd. Cohab', lat: -22.1350, lng: -51.4020, bairro: 'Cohab' },
  { id: '11', name: 'Unoeste - Campus II', address: 'Rodovia Raposo Tavares, km 572', lat: -22.1200, lng: -51.4420, bairro: 'Unoeste' },
  { id: '12', name: 'Terminal Rodoviário', address: 'Av. Manoel Goulart, Centro', lat: -22.1188, lng: -51.3920, bairro: 'Rodoviária' }
];

export const DEFAULT_SETTINGS: AppSystemSettings = {
  minRideValue: 8.00,
  pricePerKm: 2.20,
  commissionPercent: 10
};

export const MOCK_PASSENGERS: User[] = [
  {
    id: 'p1',
    email: 'ana.silva@email.com',
    name: 'Ana Silva',
    phone: '(18) 99765-4321',
    role: 'passenger',
    status: 'online',
    rating: 4.9,
    totalRides: 42,
    avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150'
  },
  {
    id: 'p2',
    email: 'marcos@email.com',
    name: 'Marcos Oliveira',
    phone: '(18) 99654-1234',
    role: 'passenger',
    status: 'online',
    rating: 4.7,
    totalRides: 15,
    avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150'
  }
];

export const MOCK_DRIVERS: User[] = [
  {
    id: 'd1',
    email: 'jose.motorista@email.com',
    name: 'José Santos',
    phone: '(18) 99123-4567',
    role: 'driver',
    status: 'approved',
    rating: 4.85,
    totalRides: 312,
    avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150',
    vehicleInfo: {
      model: 'Chevrolet Onix',
      plate: 'ROX-1010',
      color: 'Prata',
      year: '2021'
    }
  },
  {
    id: 'd2',
    email: 'carlos.uber@email.com',
    name: 'Carlos Lima',
    phone: '(18) 98877-6655',
    role: 'driver',
    status: 'approved',
    rating: 4.95,
    totalRides: 850,
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150',
    vehicleInfo: {
      model: 'Toyota Corolla',
      plate: 'PPR-2022',
      color: 'Preto',
      year: '2022'
    }
  },
  {
    id: 'd3',
    email: 'fernanda.drive@email.com',
    name: 'Fernanda Souza',
    phone: '(18) 99622-3344',
    role: 'driver',
    status: 'pending_approval',
    rating: 5.0,
    totalRides: 0,
    avatar: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150',
    vehicleInfo: {
      model: 'Hyundai HB20',
      plate: 'ROX-7788',
      color: 'Branco',
      year: '2020'
    }
  }
];
