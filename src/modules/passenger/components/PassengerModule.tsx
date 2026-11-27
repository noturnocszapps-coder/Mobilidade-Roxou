import React, { useState, useEffect } from 'react';
import { User, LocationPoint, AppSystemSettings, RideRequest, Proposal } from '@/src/shared/types';
import { calculateDistance, calculateSuggestedPrice } from '@/src/shared/utils';
import { AddressSearch } from '@/src/modules/maps/components/AddressSearch';
import { 
  MapPin, 
  ArrowRightLeft, 
  Users, 
  CreditCard, 
  Check, 
  X, 
  Sparkles, 
  Navigation, 
  Clock, 
  Ban, 
  Heart, 
  TrendingUp, 
  Calendar, 
  DollarSign, 
  Award, 
  Car, 
  Smartphone,
  Bookmark
} from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

interface PassengerProps {
  user: User;
  settings: AppSystemSettings;
  locations: LocationPoint[];
  activeRides: RideRequest[];
  proposals: Proposal[];
  onRequestRide: (data: {
    origin: LocationPoint;
    destination: LocationPoint;
    distanceKm: number;
    suggestedPrice: number;
    paymentMethod: string;
    passengerCount: number;
    observation?: string;
  }) => void;
  onAcceptProposal: (proposal: Proposal) => void;
  onDeclineProposal: (proposalId: string) => void;
  onCancelRide: (requestId: string) => void;
  mapOrigin: LocationPoint | null;
  mapDestination: LocationPoint | null;
  setMapOrigin: (loc: LocationPoint | null) => void;
  setMapDestination: (loc: LocationPoint | null) => void;
}

interface EventItem {
  id: string;
  title: string;
  locationId: string;
  status: string;
  description: string;
  tag: string;
  time: string;
}

export const PassengerModule: React.FC<PassengerProps> = ({
  user,
  settings,
  locations,
  activeRides,
  proposals,
  onRequestRide,
  onAcceptProposal,
  onDeclineProposal,
  onCancelRide,
  mapOrigin,
  mapDestination,
  setMapOrigin,
  setMapDestination,
}) => {
  const [activeSubTab, setActiveSubTab] = useState<'request' | 'events' | 'dashboard'>('request');
  const [origin, setOrigin] = useState<LocationPoint | null>(null);
  const [destination, setDestination] = useState<LocationPoint | null>(null);
  const [passengerCount, setPassengerCount] = useState<number>(1);
  const [paymentMethod, setPaymentMethod] = useState<string>('Pix');
  const [observation, setObservation] = useState<string>('');
  const [eventAlert, setEventAlert] = useState<string | null>(null);

  // Sync map selections with forms
  useEffect(() => {
    setOrigin(mapOrigin);
  }, [mapOrigin]);

  useEffect(() => {
    setDestination(mapDestination);
  }, [mapDestination]);

  const handleOriginChange = (locId: string) => {
    const loc = locations.find((l) => l.id === locId) || null;
    setOrigin(loc);
    setMapOrigin(loc);
  };

  const handleDestinationChange = (locId: string) => {
    const loc = locations.find((l) => l.id === locId) || null;
    setDestination(loc);
    setMapDestination(loc);
  };

  const handleSwapLocations = () => {
    const temp = origin;
    setOrigin(destination);
    setDestination(temp);
    setMapOrigin(destination);
    setMapDestination(temp);
  };

  const distance = origin && destination ? calculateDistance(origin, destination) : 0;
  const suggestedPrice = origin && destination ? calculateSuggestedPrice(distance, settings) : 0;

  const handleSubmitRequest = (e: React.FormEvent) => {
    e.preventDefault();
    if (!origin || !destination) return;

    onRequestRide({
      origin,
      destination,
      distanceKm: distance,
      suggestedPrice,
      paymentMethod,
      passengerCount,
      observation,
    });

    setObservation('');
  };

  const pendingRide = activeRides.find(
    (r) => r.passengerId === user.id && (r.status === 'requested' || r.status === 'negotiating')
  );

  const currentProposals = pendingRide
    ? proposals.filter((p) => p.requestId === pendingRide.id && p.status === 'pending')
    : [];

  const EVENTS: EventItem[] = [
    {
      id: 'evt1',
      title: 'Expo Prudente 2026',
      locationId: '12', // Terminal Rodoviário representing Recinto
      status: 'Acontecendo Agora 🔥',
      description: 'Feira agropecuária, shows nacionais de sertanejo e praça de alimentação completa.',
      tag: 'Expo Prudente',
      time: '18:00 - 02:00',
    },
    {
      id: 'evt2',
      title: 'FEJUPI 2026',
      locationId: '1', // Parque do Povo
      status: 'Confirmado 📅',
      description: 'Festa Junina de Presidente Prudente no Parque do Povo com comidas e fogueira gigante.',
      tag: 'FEJUPI',
      time: '19:00 - 23:30',
    },
    {
      id: 'evt3',
      title: 'Festival Matarazzo',
      locationId: '9', // Centro Cultural Matarazzo
      status: 'Hoje às 20h 🎭',
      description: 'Apresentações culturais de teatro e música instrumental de graça para a população.',
      tag: 'Matarazzo',
      time: '20:00 - 22:00',
    },
    {
      id: 'evt4',
      title: 'Cervejada Unoeste',
      locationId: '11', // Unoeste Campus II
      status: 'Sexta-Feira 🎓',
      description: 'Tradicional confraternização universitária no Campus II da Unoeste Prudente.',
      tag: 'Unoeste',
      time: '14:00 - 22:00',
    },
  ];

  const handleEventAction = (event: EventItem, direction: 'to' | 'from') => {
    const eventLoc = locations.find((l) => l.id === event.locationId);
    if (!eventLoc) return;

    const centerLoc = locations.find((l) => l.id === '3') || locations[0];

    if (direction === 'to') {
      setOrigin(centerLoc);
      setMapOrigin(centerLoc);
      setDestination(eventLoc);
      setMapDestination(eventLoc);
      setEventAlert(`Rota configurada: Indo para o evento "${event.title}"!`);
    } else {
      setOrigin(eventLoc);
      setMapOrigin(eventLoc);
      setDestination(centerLoc);
      setMapDestination(centerLoc);
      setEventAlert(`Rota configurada: Voltando do evento "${event.title}"!`);
    }

    setActiveSubTab('request');
    setTimeout(() => setEventAlert(null), 4000);
  };

  const favoriteDrivers = [
    { name: 'José Santos', vehicle: 'Chevrolet Onix • ROX-1010', rating: 4.85, rides: 14 },
    { name: 'Carlos Lima', vehicle: 'Toyota Corolla • PPR-2022', rating: 4.95, rides: 8 },
  ];

  const favoriteAddresses = [
    { label: '🏠 Casa', name: 'Parque do Povo', id: '1' },
    { label: '💼 Trabalho', name: 'Prudenshopping', id: '2' },
    { label: '🎓 Faculdade', name: 'Unoeste - Campus II', id: '11' },
  ];

  const handleQuickAddress = (locId: string, type: 'origin' | 'destination') => {
    const loc = locations.find((l) => l.id === locId);
    if (!loc) return;

    if (type === 'destination') {
      setDestination(loc);
      setMapDestination(loc);
      setEventAlert(`Destino rápido definido: ${loc.name}`);
    } else {
      setOrigin(loc);
      setMapOrigin(loc);
      setEventAlert(`Partida rápida definida: ${loc.name}`);
    }
    setActiveSubTab('request');
    setTimeout(() => setEventAlert(null), 3000);
  };

  return (
    <div className="w-full max-w-md mx-auto space-y-4" id="passenger-module">
      {/* Title greeting */}
      <div className="bg-gradient-to-br from-slate-900 via-violet-950 to-black text-white p-5 rounded-3xl shadow-xl relative overflow-hidden flex justify-between items-center border border-slate-800">
        <div className="absolute top-0 right-0 w-24 h-24 bg-violet-600/10 rounded-full blur-2xl pointer-events-none"></div>
        <div className="z-10">
          <p className="text-[10px] text-violet-300 uppercase tracking-widest font-extrabold flex items-center gap-1">
            <Smartphone className="w-3 h-3 text-violet-400" />
            <span>Painel do Passageiro</span>
          </p>
          <h2 className="text-lg font-black tracking-tight mt-1">{user.name}</h2>
          <div className="flex items-center gap-2 mt-1.5 text-[10px] text-slate-300 bg-white/5 border border-white/10 px-2 py-0.5 rounded-full w-fit">
            <span className="font-extrabold text-amber-400">★ {user.rating.toFixed(1)}</span>
            <span className="text-slate-500">•</span>
            <span className="font-semibold text-slate-300">{user.totalRides} viagens completas</span>
          </div>
        </div>
        <div className="relative">
          <img src={user.avatar} className="w-14 h-14 rounded-2xl border border-violet-500/30 object-cover shadow-lg" alt="" referrerPolicy="no-referrer" />
          <div className="absolute -bottom-1 -right-1 bg-emerald-500 w-3 h-3 rounded-full border-2 border-slate-950"></div>
        </div>
      </div>

      {/* Tabs */}
      {!pendingRide && (
        <div className="flex bg-slate-100 p-1 rounded-2xl border border-slate-200/60 shadow-xs text-xs font-bold">
          <button
            onClick={() => setActiveSubTab('request')}
            className={`flex-1 py-2.5 text-center rounded-xl transition-all flex items-center justify-center gap-1.5 ${
              activeSubTab === 'request' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-800'
            }`}
          >
            <Navigation className="w-3.5 h-3.5 text-violet-600" />
            <span>Solicitar</span>
          </button>
          <button
            onClick={() => setActiveSubTab('events')}
            className={`flex-1 py-2.5 text-center rounded-xl transition-all flex items-center justify-center gap-1.5 ${
              activeSubTab === 'events' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-800'
            }`}
          >
            <Sparkles className="w-3.5 h-3.5 text-amber-500" />
            <span>Roxou Eventos</span>
          </button>
          <button
            onClick={() => setActiveSubTab('dashboard')}
            className={`flex-1 py-2.5 text-center rounded-xl transition-all flex items-center justify-center gap-1.5 ${
              activeSubTab === 'dashboard' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-800'
            }`}
          >
            <Bookmark className="w-3.5 h-3.5 text-blue-500" />
            <span>Favoritos / Painel</span>
          </button>
        </div>
      )}

      {/* Main Mode Toggle Context */}
      <AnimatePresence mode="wait">
        {pendingRide ? (
          /* NEGOTIATION / WAITING FOR DRIVERS SCREEN */
          <motion.div
            key="negotiating-screen"
            initial={{ opacity: 0, scale: 0.98 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0 }}
            className="space-y-4"
          >
            {/* Status overview card */}
            <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm space-y-4">
              <div className="flex justify-between items-center border-b border-slate-100 pb-3">
                <div className="flex items-center gap-2">
                  <span className="w-2.5 h-2.5 bg-amber-500 rounded-full animate-ping"></span>
                  <span className="text-xs font-bold text-slate-800">Aguardando motoristas...</span>
                </div>
                <button
                  onClick={() => onCancelRide(pendingRide.id)}
                  className="text-xs text-red-600 hover:text-red-700 font-bold flex items-center gap-1 bg-red-50 hover:bg-red-100 px-3 py-1.5 rounded-xl border border-red-100 transition-all cursor-pointer shadow-xs"
                  id="btn-cancel-ride"
                >
                  <Ban className="w-3.5 h-3.5" />
                  <span>Cancelar</span>
                </button>
              </div>

              {/* Route details */}
              <div className="bg-slate-50 p-3.5 rounded-2xl text-xs space-y-2 border border-slate-100">
                <div>
                  <span className="text-slate-400 font-medium">De:</span> <strong className="text-slate-700">{pendingRide.origin.name}</strong>
                  <span className="text-[10px] bg-slate-200 text-slate-600 font-bold px-1.5 py-0.2 rounded ml-1 uppercase">{pendingRide.origin.bairro}</span>
                </div>
                <div>
                  <span className="text-slate-400 font-medium">Para:</span> <strong className="text-slate-700">{pendingRide.destination.name}</strong>
                  <span className="text-[10px] bg-slate-200 text-slate-600 font-bold px-1.5 py-0.2 rounded ml-1 uppercase">{pendingRide.destination.bairro}</span>
                </div>
                <div className="flex justify-between items-center pt-2 border-t border-slate-100 text-[11px] text-slate-500">
                  <span>{pendingRide.distanceKm} km • {pendingRide.passengerCount} passageiro(s)</span>
                  <span>Pagamento: <strong className="text-slate-700">{pendingRide.paymentMethod}</strong></span>
                </div>
                {pendingRide.observation && (
                  <div className="text-[10px] text-violet-700 bg-violet-50 p-2 rounded-lg mt-2 border border-violet-100">
                    Obs: "{pendingRide.observation}"
                  </div>
                )}
              </div>

              <div className="flex justify-between items-center p-3 bg-violet-50 border border-violet-100 rounded-2xl">
                <span className="text-xs text-violet-900 font-bold">Sua oferta inicial sugerida:</span>
                <span className="text-base font-extrabold text-violet-700">R$ {pendingRide.suggestedPrice.toFixed(2)}</span>
              </div>
            </div>

            {/* Bids list from drivers */}
            <div className="space-y-3">
              <h4 className="text-xs font-black text-slate-600 uppercase tracking-wider px-1">Propostas Recebidas em Tempo Real ({currentProposals.length})</h4>

              <AnimatePresence mode="popLayout">
                {currentProposals.length === 0 ? (
                  <motion.div
                    key="no-proposals"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    className="bg-white p-8 rounded-3xl border border-dashed border-slate-200 text-center space-y-3 shadow-xs"
                  >
                    <div className="w-12 h-12 bg-violet-50 rounded-full flex items-center justify-center mx-auto text-violet-600">
                      <Clock className="w-6 h-6 animate-spin" />
                    </div>
                    <div>
                      <p className="text-xs font-bold text-slate-700">Analisando sua rota...</p>
                      <p className="text-[10px] text-slate-400 mt-1">Motoristas de Presidente Prudente estão analisando sua corrida e enviando propostas em instantes.</p>
                    </div>
                  </motion.div>
                ) : (
                  currentProposals.map((prop) => {
                    const priceDiff = prop.price - pendingRide.suggestedPrice;
                    return (
                      <motion.div
                        key={prop.id}
                        initial={{ opacity: 0, y: 10 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, scale: 0.95 }}
                        className="bg-white p-4 rounded-3xl border border-slate-200 shadow-sm hover:border-violet-300 transition-all flex flex-col justify-between gap-4"
                      >
                        <div className="flex items-center gap-3">
                          <img src={prop.driverAvatar} className="w-11 h-11 rounded-xl border border-slate-100 object-cover" alt="" referrerPolicy="no-referrer" />
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-1.5">
                              <span className="text-xs font-bold text-slate-900 truncate">{prop.driverName}</span>
                              <span className="bg-amber-50 text-amber-700 text-[9px] font-bold px-1.5 py-0.2 rounded-full border border-amber-200">★ {prop.driverRating}</span>
                            </div>
                            <p className="text-[10px] text-slate-500 truncate mt-0.5">{prop.vehicleInfo}</p>
                          </div>

                          <div className="text-right">
                            <p className="text-base font-black text-slate-950">R$ {prop.price.toFixed(2)}</p>
                            <p className={`text-[9px] font-bold ${priceDiff <= 0 ? 'text-emerald-600' : 'text-amber-600'} mt-0.5`}>
                              {priceDiff === 0 ? 'Valor sugerido' : priceDiff < 0 ? `R$ ${Math.abs(priceDiff).toFixed(2)} de desconto` : `+ R$ ${priceDiff.toFixed(2)}`}
                            </p>
                          </div>
                        </div>

                        <div className="flex gap-2">
                          <button
                            onClick={() => onDeclineProposal(prop.id)}
                            className="flex-1 py-2 bg-slate-50 hover:bg-red-50 text-slate-500 hover:text-red-700 border border-slate-200 hover:border-red-100 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-1"
                          >
                            <X className="w-4 h-4" />
                            <span>Recusar</span>
                          </button>
                          <button
                            onClick={() => onAcceptProposal(prop)}
                            className="flex-[2] py-2 bg-violet-600 hover:bg-violet-700 text-white rounded-xl text-xs font-black transition-all flex items-center justify-center gap-1 shadow-md shadow-violet-600/10 cursor-pointer"
                          >
                            <Check className="w-4 h-4" />
                            <span>Aceitar Motorista</span>
                          </button>
                        </div>
                      </motion.div>
                    );
                  })
                )}
              </AnimatePresence>
            </div>
          </motion.div>
        ) : (
          /* ALL OTHER PASSENGER SCREEN SUBTABS */
          <motion.div
            key={activeSubTab}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ duration: 0.15 }}
          >
            {eventAlert && (
              <div className="bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs px-3.5 py-3 rounded-2xl mb-3 font-semibold flex items-center gap-2 shadow-xs">
                <Check className="w-4 h-4 text-emerald-600 shrink-0" />
                <span>{eventAlert}</span>
              </div>
            )}

            {/* TAB: REQUEST RIDE */}
            {activeSubTab === 'request' && (
              <div className="bg-white p-5 rounded-3xl border border-slate-200/80 shadow-xs space-y-4">
                <div className="flex items-center justify-between border-b border-slate-100 pb-2.5">
                  <h3 className="text-xs font-black text-slate-800 uppercase tracking-wider flex items-center gap-1.5">
                    <Navigation className="w-4 h-4 text-violet-600" />
                    <span>Configurar Trajeto</span>
                  </h3>
                  <span className="text-[10px] bg-violet-100 text-violet-800 font-extrabold px-2.5 py-0.5 rounded-full">Prudente/SP</span>
                </div>

                <form onSubmit={handleSubmitRequest} className="space-y-4">
                  {/* Point of Origin & Destination selection */}
                  <div className="relative space-y-2">
                    <div className="absolute left-4 top-[20px] bottom-[20px] w-0.5 bg-slate-200 border-dashed border-slate-300"></div>

                    {/* Origin */}
                    <div className="flex gap-2 items-center">
                      <div className="w-3.5 h-3.5 rounded-full bg-violet-600 ring-4 ring-violet-100 shrink-0 z-10 ml-2"></div>
                      <div className="flex-1">
                        <label className="block text-[8px] font-black text-violet-900 uppercase tracking-widest mb-1">Ponto de Partida</label>
                        <AddressSearch
                          value={origin}
                          onSelect={(loc) => {
                            setOrigin(loc);
                            setMapOrigin(loc);
                          }}
                          placeholder="Onde o motorista deve te buscar?"
                          id="passenger-origin-search"
                        />
                      </div>
                    </div>

                    {/* Switch Button in the middle */}
                    <div className="absolute right-2 top-1/2 -translate-y-1/2 z-10">
                      <button
                        type="button"
                        onClick={handleSwapLocations}
                        className="p-1.5 bg-white border border-slate-200 rounded-full hover:bg-violet-50 hover:border-violet-300 transition-colors shadow-sm"
                      >
                        <ArrowRightLeft className="w-3.5 h-3.5 text-violet-600 rotate-90" />
                      </button>
                    </div>

                    {/* Destination */}
                    <div className="flex gap-2 items-center pt-1.5">
                      <div className="w-3.5 h-3.5 rounded-full bg-rose-600 ring-4 ring-rose-100 shrink-0 z-10 ml-2"></div>
                      <div className="flex-1">
                        <label className="block text-[8px] font-black text-rose-900 uppercase tracking-widest mb-1">Ponto de Destino</label>
                        <AddressSearch
                          value={destination}
                          onSelect={(loc) => {
                            setDestination(loc);
                            setMapDestination(loc);
                          }}
                          placeholder="Para onde você quer ir?"
                          id="passenger-destination-search"
                        />
                      </div>
                    </div>
                  </div>

                  {/* Extra parameters */}
                  <div className="grid grid-cols-2 gap-3 pt-1">
                    <div>
                      <label className="block text-[8px] font-black text-slate-500 uppercase tracking-wider mb-1">Passageiros</label>
                      <div className="flex bg-slate-50 border border-slate-200 rounded-xl p-1 items-center justify-around">
                        {[1, 2, 3, 4].map((count) => (
                          <button
                            key={count}
                            type="button"
                            onClick={() => setPassengerCount(count)}
                            className={`w-6 h-6 text-xs font-bold rounded-lg flex items-center justify-center transition-all ${
                              passengerCount === count ? 'bg-violet-600 text-white shadow-sm font-extrabold' : 'text-slate-600 hover:bg-slate-200'
                            }`}
                          >
                            {count}
                          </button>
                        ))}
                      </div>
                    </div>

                    <div>
                      <label className="block text-[8px] font-black text-slate-500 uppercase tracking-wider mb-1">Forma de Pagamento</label>
                      <select
                        value={paymentMethod}
                        onChange={(e) => setPaymentMethod(e.target.value)}
                        className="w-full bg-slate-50 border border-slate-200 rounded-xl py-2 px-2.5 text-xs font-bold text-slate-800 focus:outline-none focus:ring-1 focus:ring-violet-500"
                      >
                        <option value="Pix">Pix (Direto ao Motorista)</option>
                        <option value="Dinheiro">Dinheiro</option>
                        <option value="Cartão">Cartão (Maquininha)</option>
                      </select>
                    </div>
                  </div>

                  <div>
                    <label className="block text-[8px] font-black text-slate-500 uppercase tracking-wider mb-1">Observações para o Motorista</label>
                    <input
                      type="text"
                      value={observation}
                      onChange={(e) => setObservation(e.target.value)}
                      placeholder="Ex: Levo mala, ligo ar, com bebê, etc."
                      className="w-full bg-slate-50 border border-slate-200 rounded-xl py-2 px-3 text-xs text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-1 focus:ring-violet-500"
                    />
                  </div>

                  {/* Price suggestions */}
                  {origin && destination && (
                    <div className="bg-violet-50/50 p-3.5 rounded-2xl border border-violet-100/60 space-y-1.5">
                      <div className="flex justify-between items-center text-[11px]">
                        <span className="text-slate-500">Distância calculada:</span>
                        <span className="font-bold text-slate-950">{distance} km</span>
                      </div>
                      <div className="flex justify-between items-center text-[11px]">
                        <span className="text-slate-500">Tarifa básica recomendada:</span>
                        <span className="font-bold text-slate-950">R$ {settings.minRideValue.toFixed(2)}</span>
                      </div>
                      <div className="flex justify-between items-center text-xs pt-1.5 border-t border-violet-100 font-semibold">
                        <span className="text-violet-900">Preço Sugerido (Sua Oferta Inicial):</span>
                        <span className="text-base font-black text-violet-700">R$ {suggestedPrice.toFixed(2)}</span>
                      </div>
                      <p className="text-[9px] text-slate-400 italic text-center pt-1 font-medium">Os valores sugeridos podem ser aceitos ou contrapropostos pelas partes.</p>
                    </div>
                  )}

                  <button
                    type="submit"
                    disabled={!origin || !destination}
                    className={`w-full py-3.5 rounded-xl font-black text-xs uppercase tracking-widest transition-all flex items-center justify-center gap-2 cursor-pointer ${
                      origin && destination
                        ? 'bg-violet-600 hover:bg-violet-700 text-white shadow-lg shadow-violet-600/20'
                        : 'bg-slate-100 text-slate-400 cursor-not-allowed border border-slate-200'
                    }`}
                  >
                    <span>Solicitar Viagem Negociada</span>
                    <Clock className="w-4 h-4" />
                  </button>
                </form>
              </div>
            )}

            {/* TAB: ROXOU PORTAL INTEGRATION (EVENTS) */}
            {activeSubTab === 'events' && (
              <div className="bg-white p-5 rounded-3xl border border-slate-200/80 shadow-xs space-y-4">
                <div className="flex items-center justify-between border-b border-slate-100 pb-2.5">
                  <h3 className="text-xs font-black text-slate-800 uppercase tracking-wider flex items-center gap-1.5">
                    <Sparkles className="w-4 h-4 text-amber-500" />
                    <span>Conexão Roxou Eventos</span>
                  </h3>
                  <span className="text-[9px] bg-amber-50 text-amber-700 border border-amber-200 font-extrabold px-2 py-0.5 rounded-full uppercase">Integração Portal</span>
                </div>
                <p className="text-[10px] text-slate-500 leading-normal">
                  Peça uma corrida negociada diretamente das maiores festas e eventos de Presidente Prudente com apenas um clique! Rotas rápidas de ida ou volta configuradas automaticamente.
                </p>

                <div className="space-y-3 max-h-[300px] overflow-y-auto pr-1">
                  {EVENTS.map((event) => (
                    <div key={event.id} className="bg-slate-50 p-3.5 rounded-2xl border border-slate-200/60 hover:border-violet-300 transition-all space-y-2">
                      <div className="flex justify-between items-start">
                        <div>
                          <span className="text-[8px] bg-violet-100 text-violet-800 font-black px-1.5 py-0.2 rounded-md uppercase tracking-wider">{event.tag}</span>
                          <h4 className="text-xs font-black text-slate-850 mt-1">{event.title}</h4>
                        </div>
                        <span className="text-[9px] text-amber-600 bg-amber-50 font-bold px-1.5 py-0.2 rounded border border-amber-200/50">{event.status}</span>
                      </div>
                      <p className="text-[10px] text-slate-500 leading-tight">{event.description}</p>
                      <div className="text-[9px] text-slate-400 font-medium flex items-center gap-1">
                        <Calendar className="w-3 h-3 text-slate-500" />
                        <span>Horário: {event.time}</span>
                      </div>

                      <div className="flex gap-2 pt-1 border-t border-slate-200/40">
                        <button
                          onClick={() => handleEventAction(event, 'to')}
                          className="flex-1 py-1.5 bg-white hover:bg-violet-50 text-violet-700 border border-violet-100 hover:border-violet-200 rounded-lg text-[10px] font-bold transition-all"
                        >
                          🚕 Ir ao Evento
                        </button>
                        <button
                          onClick={() => handleEventAction(event, 'from')}
                          className="flex-1 py-1.5 bg-violet-600 hover:bg-violet-700 text-white rounded-lg text-[10px] font-black transition-all"
                        >
                          👋 Voltar p/ Casa
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* TAB: PASSENGER DASHBOARD (FAVORITES / SUMMARY) */}
            {activeSubTab === 'dashboard' && (
              <div className="bg-white p-5 rounded-3xl border border-slate-200/80 shadow-xs space-y-4">
                <div className="flex items-center justify-between border-b border-slate-100 pb-2.5">
                  <h3 className="text-xs font-black text-slate-800 uppercase tracking-wider flex items-center gap-1.5">
                    <Bookmark className="w-4 h-4 text-blue-500" />
                    <span>Favoritos &amp; Estatísticas</span>
                  </h3>
                  <span className="text-[10px] bg-blue-50 text-blue-700 border border-blue-200 font-extrabold px-2.5 py-0.5 rounded-full">Seu Histórico</span>
                </div>

                {/* Gastos Totais Mockup */}
                <div className="grid grid-cols-2 gap-3 bg-slate-50 p-3 rounded-2xl border border-slate-100">
                  <div className="text-center">
                    <p className="text-[9px] font-bold text-slate-400 uppercase tracking-wider">Gastos Estimados</p>
                    <p className="text-lg font-black text-slate-800 mt-0.5">R$ 148,50</p>
                    <span className="text-[8px] text-slate-500">Este mês</span>
                  </div>
                  <div className="text-center border-l border-slate-200">
                    <p className="text-[9px] font-bold text-slate-400 uppercase tracking-wider">Média p/ Viagem</p>
                    <p className="text-lg font-black text-slate-800 mt-0.5">R$ 14,20</p>
                    <span className="text-[8px] text-emerald-600 font-extrabold">Preço Justo</span>
                  </div>
                </div>

                {/* Endereços Favoritos */}
                <div className="space-y-2">
                  <p className="text-[10px] font-black text-slate-500 uppercase tracking-wider">Endereços Salvos</p>
                  <div className="grid grid-cols-1 gap-1.5">
                    {favoriteAddresses.map((addr) => (
                      <div key={addr.id} className="flex justify-between items-center bg-slate-50/50 p-2.5 rounded-xl border border-slate-100 text-xs">
                        <div className="flex items-center gap-2">
                          <span className="font-black text-slate-800">{addr.label}:</span>
                          <span className="text-slate-500 truncate max-w-[150px]">{addr.name}</span>
                        </div>
                        <div className="flex gap-1.5">
                          <button
                            onClick={() => handleQuickAddress(addr.id, 'origin')}
                            className="bg-white hover:bg-slate-100 border border-slate-200 px-2 py-1 rounded text-[9px] font-bold text-slate-700"
                          >
                            Partida
                          </button>
                          <button
                            onClick={() => handleQuickAddress(addr.id, 'destination')}
                            className="bg-violet-600 hover:bg-violet-700 text-white px-2 py-1 rounded text-[9px] font-black"
                          >
                            Destino
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Motoristas Favoritos */}
                <div className="space-y-2">
                  <p className="text-[10px] font-black text-slate-500 uppercase tracking-wider">Motoristas Preferidos</p>
                  <div className="space-y-1.5">
                    {favoriteDrivers.map((driver, index) => (
                      <div key={index} className="flex items-center justify-between p-2.5 bg-slate-50/50 rounded-xl border border-slate-100 text-xs">
                        <div>
                          <p className="font-bold text-slate-850 flex items-center gap-1">
                            <span>{driver.name}</span>
                            <span className="text-[9px] text-amber-500">★ {driver.rating}</span>
                          </p>
                          <p className="text-[10px] text-slate-450">{driver.vehicle}</p>
                        </div>
                        <span className="text-[9px] bg-violet-100 text-violet-800 font-bold px-2 py-0.5 rounded-md">
                          {driver.rides} corridas
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};
