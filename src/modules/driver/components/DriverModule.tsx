import React, { useState } from 'react';
import { User, RideRequest, Proposal, AppSystemSettings } from '@/src/shared/types';
import { 
  MapPin, 
  Navigation, 
  Compass, 
  Star, 
  DollarSign, 
  RefreshCw, 
  Check, 
  X, 
  Plus, 
  Minus,
  TrendingUp,
  Award,
  Clock,
  Car,
  Shield,
  FileText
} from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

interface DriverProps {
  user: User;
  settings: AppSystemSettings;
  activeRides: RideRequest[];
  proposals: Proposal[];
  onToggleOnline: () => void;
  onAcceptRideAtPrice: (requestId: string, price: number) => void;
  onSubmitCounterProposal: (requestId: string, price: number) => void;
  onRejectRide: (requestId: string) => void;
}

export const DriverModule: React.FC<DriverProps> = ({
  user,
  settings,
  activeRides,
  proposals,
  onToggleOnline,
  onAcceptRideAtPrice,
  onSubmitCounterProposal,
  onRejectRide,
}) => {
  const [activeSubTab, setActiveSubTab] = useState<'rides' | 'financial' | 'profile'>('rides');
  const [negotiatingRideId, setNegotiatingRideId] = useState<string | null>(null);
  const [counterPrice, setCounterPrice] = useState<number>(0);

  const isOnline = user.status === 'online' || user.status === 'busy';

  // Available rides are requested rides where the driver hasn't already sent a proposal
  const availableRides = activeRides.filter(
    (r) => r.status === 'requested' || r.status === 'negotiating'
  ).filter(
    (r) => r.passengerId !== user.id // Cannot drive yourself
  );

  const handleStartCounterOffer = (ride: RideRequest) => {
    setNegotiatingRideId(ride.id);
    setCounterPrice(Math.round(ride.suggestedPrice));
  };

  const adjustPrice = (amount: number) => {
    setCounterPrice((prev) => Math.max(settings.minRideValue, prev + amount));
  };

  const handleSendCounterOffer = (requestId: string) => {
    onSubmitCounterProposal(requestId, counterPrice);
    setNegotiatingRideId(null);
  };

  const commissionMultiplier = (100 - settings.commissionPercent) / 100;
  
  const totalRidesCompleted = user.totalRides || 0;
  const earningsToday = totalRidesCompleted > 0 ? 84.50 : 0.00;
  const earningsWeek = totalRidesCompleted > 0 ? (totalRidesCompleted * 14.20 * commissionMultiplier) : 0.00;
  const earningsMonth = totalRidesCompleted > 0 ? (totalRidesCompleted * 14.20 * commissionMultiplier * 4.2) : 0.00;
  
  const kmDrivenSimulated = totalRidesCompleted * 6.4;
  const hoursOnlineSimulated = totalRidesCompleted * 0.8 + (isOnline ? 1.5 : 0);
  const averageRideValueSimulated = totalRidesCompleted > 0 ? 14.20 : 0.00;

  return (
    <div className="w-full max-w-md mx-auto space-y-4" id="driver-module">
      {/* Driver profile status header */}
      <div className="bg-gradient-to-br from-slate-950 via-slate-900 to-violet-950 text-white p-5 rounded-3xl shadow-xl relative overflow-hidden space-y-4 border border-slate-800">
        <div className="absolute top-0 right-0 w-24 h-24 bg-violet-600/10 rounded-full blur-2xl pointer-events-none"></div>
        <div className="flex justify-between items-center z-10 relative">
          <div className="flex items-center gap-3">
            <img src={user.avatar} className="w-12 h-12 rounded-2xl border border-violet-500/30 object-cover shadow-md" alt="" referrerPolicy="no-referrer" />
            <div>
              <h2 className="text-sm font-black tracking-tight">{user.name}</h2>
              <div className="flex items-center gap-1.5 text-[11px] text-violet-200 mt-1">
                <span className="bg-violet-600/40 px-1.5 py-0.2 rounded-md font-extrabold text-[10px]">★ {user.rating.toFixed(2)}</span>
                <span className="text-slate-500">•</span>
                <span className="font-semibold">{user.vehicleInfo?.model || 'Motorista Roxou'}</span>
              </div>
            </div>
          </div>

          <button
            onClick={onToggleOnline}
            className={`px-4 py-2 rounded-xl text-xs font-black tracking-widest transition-all cursor-pointer ${
              isOnline
                ? 'bg-emerald-500 hover:bg-emerald-600 text-white shadow-lg shadow-emerald-500/20 animate-pulse'
                : 'bg-slate-700 hover:bg-slate-650 text-white'
            }`}
            id="btn-toggle-online"
          >
            {isOnline ? '● ONLINE' : '○ OFFLINE'}
          </button>
        </div>

        {/* Driver quick metrics summary row */}
        <div className="grid grid-cols-3 gap-2 pt-3 border-t border-slate-800 text-center relative z-10">
          <div className="bg-white/5 p-2 rounded-2xl border border-white/5">
            <p className="text-[8px] text-violet-300 font-black uppercase tracking-wider">Viagens</p>
            <p className="text-sm font-black text-white mt-0.5">{totalRidesCompleted}</p>
          </div>
          <div className="bg-white/5 p-2 rounded-2xl border border-white/5">
            <p className="text-[8px] text-violet-300 font-black uppercase tracking-wider">Hoje Liq.</p>
            <p className="text-sm font-black text-emerald-400 mt-0.5">R$ {earningsToday.toFixed(2)}</p>
          </div>
          <div className="bg-white/5 p-2 rounded-2xl border border-white/5">
            <p className="text-[8px] text-violet-300 font-black uppercase tracking-wider">Mês Est.</p>
            <p className="text-sm font-black text-violet-300 mt-0.5">R$ {earningsMonth.toFixed(2)}</p>
          </div>
        </div>
      </div>

      {/* Driver Tabs */}
      <div className="flex bg-slate-100 p-1 rounded-2xl border border-slate-200/60 shadow-xs text-xs font-bold">
        <button
          onClick={() => setActiveSubTab('rides')}
          className={`flex-1 py-2.5 text-center rounded-xl transition-all flex items-center justify-center gap-1.5 ${
            activeSubTab === 'rides' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-800'
          }`}
        >
          <Navigation className="w-3.5 h-3.5 text-violet-600" />
          <span>Chamados</span>
          {isOnline && availableRides.length > 0 && (
            <span className="w-4 h-4 bg-red-500 text-[9px] text-white font-black rounded-full flex items-center justify-center">
              {availableRides.length}
            </span>
          )}
        </button>
        <button
          onClick={() => setActiveSubTab('financial')}
          className={`flex-1 py-2.5 text-center rounded-xl transition-all flex items-center justify-center gap-1.5 ${
            activeSubTab === 'financial' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-800'
          }`}
        >
          <DollarSign className="w-3.5 h-3.5 text-emerald-500" />
          <span>Financeiro</span>
        </button>
        <button
          onClick={() => setActiveSubTab('profile')}
          className={`flex-1 py-2.5 text-center rounded-xl transition-all flex items-center justify-center gap-1.5 ${
            activeSubTab === 'profile' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-800'
          }`}
        >
          <Car className="w-3.5 h-3.5 text-blue-500" />
          <span>Meu Perfil</span>
        </button>
      </div>

      {/* SUBTAB CONTENT */}
      <AnimatePresence mode="wait">
        {activeSubTab === 'rides' && (
          <motion.div
            key="rides-tab"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            className="space-y-3"
          >
            <div className="flex justify-between items-center px-1">
              <h3 className="text-xs font-black text-slate-650 uppercase tracking-wider">Corrida sob demanda</h3>
              <span className="text-[10px] bg-violet-100 text-violet-800 font-extrabold px-2.5 py-0.5 rounded-full">
                Prudente / SP
              </span>
            </div>

            {!isOnline ? (
              /* OFFLINE STATUS CARD */
              <div className="bg-white p-8 rounded-3xl border border-dashed border-slate-200 text-center space-y-3 shadow-xs">
                <div className="w-12 h-12 bg-slate-100 rounded-full flex items-center justify-center mx-auto text-slate-400">
                  <Compass className="w-6 h-6" />
                </div>
                <div>
                  <p className="text-sm font-bold text-slate-800">Você está offline</p>
                  <p className="text-xs text-slate-400 mt-1">Fique online para começar a receber solicitações de passageiros na região de Presidente Prudente/SP.</p>
                </div>
                <button
                  onClick={onToggleOnline}
                  className="py-2.5 px-6 bg-violet-600 hover:bg-violet-700 text-white font-black rounded-xl text-xs shadow-md transition-all cursor-pointer"
                >
                  Ficar Online Agora
                </button>
              </div>
            ) : availableRides.length === 0 ? (
              /* ONLINE WAITING CARD */
              <div className="bg-white p-8 rounded-3xl border border-dashed border-slate-200 text-center space-y-3 shadow-xs">
                <div className="w-10 h-10 bg-violet-50 rounded-full flex items-center justify-center mx-auto text-violet-600 animate-pulse">
                  <RefreshCw className="w-5 h-5 animate-spin-slow" />
                </div>
                <div>
                  <p className="text-xs font-bold text-slate-700">Aguardando solicitações...</p>
                  <p className="text-[10px] text-slate-400 mt-1">Nenhum passageiro chamando no momento. Os pedidos surgirão nesta tela em tempo real.</p>
                </div>
              </div>
            ) : (
              /* LIST OF INCOMING RIDE REQUESTS */
              <div className="space-y-3">
                {availableRides.map((ride) => {
                  const hasSentProposal = proposals.some(
                    (p) => p.requestId === ride.id && p.driverId === user.id
                  );

                  return (
                    <div
                      key={ride.id}
                      className="bg-white p-4 rounded-3xl border border-slate-200 shadow-sm space-y-3 hover:border-violet-300 transition-all"
                    >
                      {/* Passenger Details Header */}
                      <div className="flex justify-between items-start">
                        <div className="flex items-center gap-2.5">
                          <img
                            src={ride.passengerAvatar}
                            className="w-10 h-10 rounded-xl object-cover border border-slate-100 bg-slate-50"
                            alt=""
                            referrerPolicy="no-referrer"
                          />
                          <div>
                            <div className="flex items-center gap-1">
                              <span className="text-xs font-bold text-slate-900">{ride.passengerName}</span>
                              <span className="bg-amber-50 text-amber-600 text-[9px] font-bold px-1 py-0.2 rounded border border-amber-200">★ {ride.passengerRating}</span>
                            </div>
                            <span className="text-[9px] text-slate-400 font-semibold">{ride.paymentMethod} • {ride.passengerCount} pass</span>
                          </div>
                        </div>

                        <div className="text-right">
                          <span className="text-[9px] text-slate-400 block font-bold">Oferta do Passageiro:</span>
                          <span className="text-base font-black text-slate-950">R$ {ride.suggestedPrice.toFixed(2)}</span>
                        </div>
                      </div>

                      {/* Locations and Distance */}
                      <div className="bg-slate-50 p-3 rounded-2xl space-y-1.5 text-xs border border-slate-100">
                        <div className="flex items-start gap-1.5">
                          <span className="w-2 h-2 rounded-full bg-violet-600 mt-1 shrink-0"></span>
                          <span className="text-slate-600 truncate"><b>De:</b> {ride.origin.name} <span className="text-[9px] text-violet-700 bg-violet-50 font-bold px-1 rounded">{ride.origin.bairro}</span></span>
                        </div>
                        <div className="flex items-start gap-1.5">
                          <span className="w-2 h-2 rounded-full bg-rose-600 mt-1 shrink-0"></span>
                          <span className="text-slate-600 truncate"><b>Para:</b> {ride.destination.name} <span className="text-[9px] text-rose-700 bg-rose-50 font-bold px-1 rounded">{ride.destination.bairro}</span></span>
                        </div>
                        <div className="pt-1.5 border-t border-slate-200/40 text-[10px] text-slate-500 font-semibold flex justify-between">
                          <span>Distância: {ride.distanceKm} KM</span>
                          {ride.observation && <span className="text-violet-600 font-bold truncate max-w-[150px]">Obs: "{ride.observation}"</span>}
                        </div>
                      </div>

                      {/* Proposals Action Options */}
                      {hasSentProposal ? (
                        <div className="bg-violet-50 text-violet-800 text-[11px] font-bold py-2 px-3 rounded-xl border border-violet-100 text-center">
                          ✓ Sua proposta foi enviada! Aguardando aceite do passageiro.
                        </div>
                      ) : negotiatingRideId === ride.id ? (
                        /* COUNTER OFFER EDIT AREA */
                        <div className="bg-slate-50 p-3 rounded-2xl space-y-3 border border-slate-200/80">
                          <div className="flex justify-between items-center">
                            <span className="text-[11px] font-bold text-slate-500">Definir Contraproposta:</span>
                            <span className="text-sm font-black text-violet-700">R$ {counterPrice.toFixed(2)}</span>
                          </div>
                          
                          <div className="flex items-center justify-center gap-4">
                            <button
                              onClick={() => adjustPrice(-2)}
                              className="w-10 h-10 bg-white hover:bg-slate-100 text-slate-700 font-black rounded-full flex items-center justify-center border border-slate-200 transition-colors cursor-pointer"
                            >
                              <Minus className="w-4 h-4" />
                            </button>
                            <span className="text-xs font-black text-slate-700">Ajustar Valor</span>
                            <button
                              onClick={() => adjustPrice(2)}
                              className="w-10 h-10 bg-white hover:bg-slate-100 text-slate-700 font-black rounded-full flex items-center justify-center border border-slate-200 transition-colors cursor-pointer"
                            >
                              <Plus className="w-4 h-4" />
                            </button>
                          </div>

                          <div className="flex gap-2">
                            <button
                              onClick={() => setNegotiatingRideId(null)}
                              className="flex-1 py-2 bg-white hover:bg-slate-100 text-slate-600 font-bold rounded-xl text-xs border border-slate-200"
                            >
                              Voltar
                            </button>
                            <button
                              onClick={() => handleSendCounterOffer(ride.id)}
                              className="flex-1 py-2 bg-violet-600 hover:bg-violet-700 text-white font-black rounded-xl text-xs shadow-md"
                            >
                              Enviar Proposta
                            </button>
                          </div>
                        </div>
                      ) : (
                        /* CHOOSE ACTIONS: Accept suggestion or Counter Offer */
                        <div className="flex gap-2">
                          <button
                            onClick={() => onRejectRide(ride.id)}
                            className="p-2.5 bg-slate-50 hover:bg-red-50 text-slate-400 hover:text-red-600 border border-slate-200 hover:border-red-100 rounded-xl transition-all"
                            title="Recusar"
                          >
                            <X className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => handleStartCounterOffer(ride)}
                            className="flex-1 py-2 bg-slate-50 hover:bg-violet-50 text-violet-700 border border-violet-200 hover:border-violet-300 rounded-xl text-xs font-bold transition-all"
                          >
                            Contraproposta
                          </button>
                          <button
                            onClick={() => onAcceptRideAtPrice(ride.id, ride.suggestedPrice)}
                            className="flex-1 py-2 bg-violet-600 hover:bg-violet-700 text-white rounded-xl text-xs font-black transition-all shadow-md shadow-violet-600/10 cursor-pointer"
                          >
                            Aceitar (R$ {ride.suggestedPrice.toFixed(2)})
                          </button>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </motion.div>
        )}

        {/* TAB: FINANCIAL DETAILED DASHBOARD */}
        {activeSubTab === 'financial' && (
          <motion.div
            key="financial-tab"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            className="space-y-3.5"
          >
            {/* Main gains card */}
            <div className="bg-white p-5 rounded-3xl border border-slate-200/80 shadow-xs space-y-4">
              <div className="flex justify-between items-center border-b border-slate-100 pb-2.5">
                <span className="text-xs font-black text-slate-800 uppercase tracking-wider flex items-center gap-1.5">
                  <TrendingUp className="w-4 h-4 text-emerald-500" />
                  <span>Painel de Rendimentos</span>
                </span>
                <span className="text-[10px] bg-emerald-50 text-emerald-700 font-bold px-2 py-0.5 rounded-full border border-emerald-200">
                  Presidente Prudente
                </span>
              </div>

              {/* Bento box grids for detailed driver gains */}
              <div className="grid grid-cols-2 gap-3">
                <div className="bg-slate-50 p-3 rounded-2xl border border-slate-100 text-center">
                  <p className="text-[9px] text-slate-450 uppercase font-black">Ganho do Dia</p>
                  <p className="text-xl font-black text-slate-850 mt-1">R$ {earningsToday.toFixed(2)}</p>
                  <span className="text-[8px] text-slate-400">Líquido de taxas</span>
                </div>
                <div className="bg-slate-50 p-3 rounded-2xl border border-slate-100 text-center">
                  <p className="text-[9px] text-slate-450 uppercase font-black">Esta Semana</p>
                  <p className="text-xl font-black text-emerald-600 mt-1">R$ {earningsWeek.toFixed(2)}</p>
                  <span className="text-[8px] text-slate-400">Saldo acumulado</span>
                </div>
                <div className="bg-slate-50 p-3 rounded-2xl border border-slate-100 text-center col-span-2">
                  <p className="text-[9px] text-slate-450 uppercase font-black">Faturamento Mensal Estimado</p>
                  <p className="text-2xl font-black text-violet-700 mt-1">R$ {earningsMonth.toFixed(2)}</p>
                  <span className="text-[9px] text-slate-500 font-medium">Considerando histórico e coeficientes sazonais</span>
                </div>
              </div>

              {/* Statistics lists */}
              <div className="space-y-2.5 pt-1.5">
                <p className="text-[10px] font-black text-slate-500 uppercase tracking-wider">Estatísticas Operacionais</p>
                
                <div className="space-y-1.5">
                  <div className="flex justify-between text-xs bg-slate-50 p-2 rounded-xl border border-slate-150">
                    <span className="text-slate-500">Quilômetros Rodados:</span>
                    <strong className="text-slate-800">{kmDrivenSimulated.toFixed(1)} km</strong>
                  </div>
                  <div className="flex justify-between text-xs bg-slate-50 p-2 rounded-xl border border-slate-150">
                    <span className="text-slate-500">Tempo de Conexão Online:</span>
                    <strong className="text-slate-800">{hoursOnlineSimulated.toFixed(1)} horas</strong>
                  </div>
                  <div className="flex justify-between text-xs bg-slate-50 p-2 rounded-xl border border-slate-150">
                    <span className="text-slate-500">Ticket Médio por Viagem:</span>
                    <strong className="text-emerald-600">R$ {averageRideValueSimulated.toFixed(2)}</strong>
                  </div>
                  <div className="flex justify-between text-xs bg-slate-50 p-2 rounded-xl border border-slate-150">
                    <span className="text-slate-500">Comissão de Plataforma (Taxa):</span>
                    <strong className="text-amber-600">{settings.commissionPercent}%</strong>
                  </div>
                </div>
              </div>
            </div>
          </motion.div>
        )}

        {/* TAB: DRIVER PERFIL / SECURITY CREDENTIALS */}
        {activeSubTab === 'profile' && (
          <motion.div
            key="profile-tab"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            className="space-y-3.5"
          >
            <div className="bg-white p-5 rounded-3xl border border-slate-200/80 shadow-xs space-y-4">
              <div className="flex justify-between items-center border-b border-slate-100 pb-2.5">
                <span className="text-xs font-black text-slate-800 uppercase tracking-wider flex items-center gap-1.5">
                  <Award className="w-4 h-4 text-violet-500" />
                  <span>Credenciais de Motorista</span>
                </span>
                <span className="text-[10px] bg-violet-50 text-violet-700 font-bold px-2 py-0.5 rounded-full border border-violet-200">
                  Verificado ✅
                </span>
              </div>

              {/* Security approvals */}
              <div className="space-y-2.5 text-xs">
                <p className="text-[10px] font-black text-slate-500 uppercase tracking-wider">Status de Documentação</p>
                
                <div className="space-y-1.5">
                  <div className="flex justify-between items-center p-2 bg-emerald-50 text-emerald-800 rounded-xl border border-emerald-100 font-medium">
                    <div className="flex items-center gap-1.5">
                      <Shield className="w-3.5 h-3.5 text-emerald-600" />
                      <span>Carteira de Habilitação (CNH)</span>
                    </div>
                    <strong>APROVADO</strong>
                  </div>
                  <div className="flex justify-between items-center p-2 bg-emerald-50 text-emerald-800 rounded-xl border border-emerald-100 font-medium">
                    <div className="flex items-center gap-1.5">
                      <FileText className="w-3.5 h-3.5 text-emerald-600" />
                      <span>Documento do Veículo (CRLV)</span>
                    </div>
                    <strong>APROVADO</strong>
                  </div>
                  <div className="flex justify-between items-center p-2 bg-emerald-50 text-emerald-800 rounded-xl border border-emerald-100 font-medium">
                    <div className="flex items-center gap-1.5">
                      <Award className="w-3.5 h-3.5 text-emerald-600" />
                      <span>Antecedentes Criminais</span>
                    </div>
                    <strong>APROVADO</strong>
                  </div>
                </div>
              </div>

              {/* Vehicle parameters */}
              <div className="space-y-2.5 pt-1">
                <p className="text-[10px] font-black text-slate-500 uppercase tracking-wider">Dados do Veículo</p>
                <div className="bg-slate-50 p-3.5 rounded-2xl border border-slate-100 text-xs space-y-1.5 text-slate-600">
                  <p><b>Modelo:</b> {user.vehicleInfo?.model || 'Não Informado'}</p>
                  <p><b>Placa:</b> {user.vehicleInfo?.plate || 'Não Informado'}</p>
                  <p><b>Cor:</b> {user.vehicleInfo?.color || 'Não Informado'}</p>
                  <p><b>Ano:</b> {user.vehicleInfo?.year || 'Não Informado'}</p>
                </div>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};
