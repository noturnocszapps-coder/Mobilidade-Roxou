import React from 'react';
import { useAuth } from './providers/AuthProvider';
import { useRealtime } from './providers/RealtimeProvider';
import { useSupabase } from './providers/SupabaseProvider';
import { AuthModule } from '@/src/modules/auth/components/AuthModule';
import { PassengerModule } from '@/src/modules/passenger/components/PassengerModule';
import { DriverModule } from '@/src/modules/driver/components/DriverModule';
import { AdminModule } from '@/src/modules/admin/components/AdminModule';
import { ActiveRideModule } from '@/src/modules/rides/components/ActiveRideModule';
import { GoogleMap } from '@/src/modules/maps/components/GoogleMap';
import { PRESIDENTE_PRUDENTE_LOCATIONS } from '@/src/shared/constants';
import { calculateDistance, calculateSuggestedPrice } from '@/src/shared/utils/calculations';
import { LocationPoint, RideRequest, Proposal, ChatMessage, Review, AppSystemSettings, User } from '@/src/shared/types';
import { Shield, Car, Users, LogOut, Key, Compass, Sliders, Bell, Sparkles, CheckCircle2 } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

interface RoutesProps {
  // Shared States and Props passed from App container
  activeRides: RideRequest[];
  proposals: Proposal[];
  settings: AppSystemSettings;
  mapOrigin: LocationPoint | null;
  mapDestination: LocationPoint | null;
  simulatedProgress: number;
  setMapOrigin: React.Dispatch<React.SetStateAction<LocationPoint | null>>;
  setMapDestination: React.Dispatch<React.SetStateAction<LocationPoint | null>>;
  setSimulatedProgress: React.Dispatch<React.SetStateAction<number>>;
  
  // Handlers
  onUpdateSettings: (newSettings: AppSystemSettings) => void;
  onApproveDriver: (driverId: string) => void;
  onRejectDriver: (driverId: string) => void;
  onRequestRide: (data: any) => Promise<void>;
  onAcceptProposal: (proposal: Proposal) => Promise<void>;
  onDeclineProposal: (proposalId: string) => Promise<void>;
  onCancelRide: (requestId: string) => Promise<void>;
  onToggleOnline: () => Promise<void>;
  onAcceptRideAtPrice: (requestId: string, price: number) => Promise<void>;
  onSubmitCounterProposal: (requestId: string, price: number) => Promise<void>;
  onRejectRide: (requestId: string) => void;
  onCompleteRide: (rideId: string) => Promise<void>;
  onAddChatMessage: (rideId: string, message: ChatMessage) => void;
  onAddReview: (review: Review) => void;
  onSimulatePassengerCall: () => void;
}

export const Routes: React.FC<RoutesProps> = ({
  activeRides,
  proposals,
  settings,
  mapOrigin,
  mapDestination,
  simulatedProgress,
  setMapOrigin,
  setMapDestination,
  setSimulatedProgress,
  onUpdateSettings,
  onApproveDriver,
  onRejectDriver,
  onRequestRide,
  onAcceptProposal,
  onDeclineProposal,
  onCancelRide,
  onToggleOnline,
  onAcceptRideAtPrice,
  onSubmitCounterProposal,
  onRejectRide,
  onCompleteRide,
  onAddChatMessage,
  onAddReview,
  onSimulatePassengerCall,
}) => {
  const { currentUser, drivers, passengers, handleLogin, handleRegisterDriver, handleRegisterPassenger, handleQuickSwitchRole } = useAuth();
  const { showNotification } = useRealtime();
  const { isSupabaseConfigured } = useSupabase();

  const activeRideInMatch = activeRides.find(
    (r) => r.status === 'in_progress' && (r.passengerId === currentUser?.id || r.driverId === currentUser?.id)
  );

  if (!currentUser) {
    return (
      <div className="max-w-md mx-auto w-full py-12">
        <AnimatePresence mode="wait">
          <motion.div
            key="auth"
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -15 }}
          >
            <AuthModule
              onLogin={handleLogin}
              drivers={drivers}
              passengers={passengers}
              onRegisterDriver={handleRegisterDriver}
              onRegisterPassenger={handleRegisterPassenger}
            />
          </motion.div>
        </AnimatePresence>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
      {/* Left Workspace Column: Controls and active state */}
      <div className="col-span-12 lg:col-span-5 flex flex-col gap-6">
        {currentUser.role === 'driver' && currentUser.status === 'online' && (
          <button
            onClick={onSimulatePassengerCall}
            className="w-full py-3 bg-violet-50 border border-violet-100 hover:bg-violet-100 text-violet-800 rounded-2xl text-xs font-bold transition-all flex items-center justify-center gap-2 cursor-pointer shadow-sm"
          >
            <Bell className="w-4 h-4 text-violet-600 animate-bounce" />
            <span>Simular Novo Chamado de Passageiro 🔔</span>
          </button>
        )}

        <div className="flex-1">
          <AnimatePresence mode="wait">
            {activeRideInMatch ? (
              <motion.div
                key="active-ride"
                initial={{ opacity: 0, scale: 0.98 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0 }}
              >
                <ActiveRideModule
                  user={currentUser}
                  ride={activeRideInMatch}
                  onCompleteRide={onCompleteRide}
                  onCancelRide={onCancelRide}
                  onAddChatMessage={onAddChatMessage}
                  onAddReview={onAddReview}
                  simulatedProgress={simulatedProgress}
                  setSimulatedProgress={setSimulatedProgress}
                />
              </motion.div>
            ) : currentUser.role === 'passenger' ? (
              <motion.div
                key="passenger"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
              >
                <PassengerModule
                  user={currentUser}
                  settings={settings}
                  locations={PRESIDENTE_PRUDENTE_LOCATIONS}
                  activeRides={activeRides}
                  proposals={proposals}
                  onRequestRide={onRequestRide}
                  onAcceptProposal={onAcceptProposal}
                  onDeclineProposal={onDeclineProposal}
                  onCancelRide={onCancelRide}
                  mapOrigin={mapOrigin}
                  mapDestination={mapDestination}
                  setMapOrigin={setMapOrigin}
                  setMapDestination={setMapDestination}
                />
              </motion.div>
            ) : currentUser.role === 'driver' ? (
              <motion.div
                key="driver"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
              >
                <DriverModule
                  user={currentUser}
                  settings={settings}
                  activeRides={activeRides}
                  proposals={proposals}
                  onToggleOnline={onToggleOnline}
                  onAcceptRideAtPrice={onAcceptRideAtPrice}
                  onSubmitCounterProposal={onSubmitCounterProposal}
                  onRejectRide={onRejectRide}
                />
              </motion.div>
            ) : (
              <motion.div
                key="admin"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
              >
                <AdminModule
                  settings={settings}
                  onUpdateSettings={onUpdateSettings}
                  drivers={drivers}
                  onApproveDriver={onApproveDriver}
                  onRejectDriver={onRejectDriver}
                  activeRides={activeRides}
                />
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>

      {/* Right Map & Dashboard Feed Column */}
      <div className="col-span-12 lg:col-span-7 flex flex-col gap-6">
        <div className="grid grid-cols-3 gap-4">
          <div className="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-xs">
            <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Viagens Totais</p>
            <h3 className="text-xl font-black text-slate-800 mt-1">
              {activeRides.length + 128}
            </h3>
            <div className="text-[10px] text-emerald-600 font-bold mt-1">↑ 12% vs ontem</div>
          </div>
          <div className="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-xs">
            <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Tarifa Base</p>
            <h3 className="text-xl font-black text-slate-800 mt-1">
              R$ {settings.minRideValue.toFixed(2)}
            </h3>
            <div className="text-[10px] text-slate-500 font-medium mt-1">Presidente Prudente</div>
          </div>
          <div className="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-xs">
            <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Aguardando Aceite</p>
            <h3 className="text-xl font-black text-violet-600 mt-1">
              {activeRides.filter(r => r.status === 'requested' || r.status === 'negotiating').length}
            </h3>
            <div className="text-[10px] text-slate-500 font-medium mt-1">Em tempo real</div>
          </div>
        </div>

        <GoogleMap
          origin={mapOrigin}
          destination={mapDestination}
          drivers={drivers}
          activeRide={activeRideInMatch}
          onSelectLocation={(loc, type) => {
            if (type === 'origin') {
              setMapOrigin(loc);
              showNotification(`Ponto de partida definido: ${loc.name}`);
            } else {
              setMapDestination(loc);
              showNotification(`Destino definido: ${loc.name}`);
            }
          }}
        />

        {/* Realtime Negotiations Log */}
        <div className="bg-white rounded-2xl border border-slate-200/80 shadow-xs flex flex-col overflow-hidden">
          <div className="p-4 border-b border-slate-100 bg-slate-50/50 flex justify-between items-center">
            <h4 className="font-bold text-xs text-slate-700 tracking-tight uppercase">Atividades Recentes no Sistema</h4>
            <span className="text-[10px] bg-emerald-50 text-emerald-700 font-bold px-2 py-0.5 rounded-full uppercase border border-emerald-100">
              Sincronizado
            </span>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="text-[9px] text-slate-400 uppercase font-black tracking-widest border-b border-slate-100">
                  <th className="p-3">Usuário/ID</th>
                  <th className="p-3">Trajeto</th>
                  <th className="p-3">Tarifa Proposta</th>
                  <th className="p-3">Status</th>
                </tr>
              </thead>
              <tbody className="text-xs">
                {activeRides.length === 0 ? (
                  <>
                    <tr className="border-b border-slate-50 hover:bg-slate-50/30">
                      <td className="p-3">
                        <div className="flex items-center gap-2">
                          <div className="w-6 h-6 bg-amber-100 rounded-full flex items-center justify-center text-[9px] font-bold text-amber-700">MA</div>
                          <span className="font-medium text-slate-700">Marcos A.</span>
                        </div>
                      </td>
                      <td className="p-3 text-slate-500">Centro ➔ Parque do Povo</td>
                      <td className="p-3 font-semibold text-slate-900">R$ 10,00</td>
                      <td className="p-3">
                        <span className="px-2 py-0.5 bg-orange-100 text-orange-700 rounded text-[9px] font-bold uppercase border border-orange-200/30">Negociando</span>
                      </td>
                    </tr>
                    <tr className="border-b border-slate-50 hover:bg-slate-50/30">
                      <td className="p-3">
                        <div className="flex items-center gap-2">
                          <div className="w-6 h-6 bg-blue-100 rounded-full flex items-center justify-center text-[9px] font-bold text-blue-700">CL</div>
                          <span className="font-medium text-slate-700">Carla L.</span>
                        </div>
                      </td>
                      <td className="p-3 text-slate-500">Rodoviária ➔ Prudenshopping</td>
                      <td className="p-3 font-semibold text-slate-900">R$ 16,00</td>
                      <td className="p-3">
                        <span className="px-2 py-0.5 bg-violet-100 text-violet-700 rounded text-[9px] font-bold uppercase border border-violet-200/30">Aguardando</span>
                      </td>
                    </tr>
                  </>
                ) : (
                  activeRides.map((ride) => (
                    <tr key={ride.id} className="border-b border-slate-50 hover:bg-slate-50/30">
                      <td className="p-3">
                        <div className="flex items-center gap-2">
                          <div className="w-6 h-6 bg-violet-50 text-violet-700 rounded-full flex items-center justify-center text-[9px] font-bold">
                            {ride.passengerName?.substring(0, 2).toUpperCase() || 'PS'}
                          </div>
                          <span className="font-medium text-slate-700">{ride.passengerName}</span>
                        </div>
                      </td>
                      <td className="p-3 text-slate-500 truncate max-w-[150px]" title={`${ride.origin.name} ➔ ${ride.destination.name}`}>
                        {ride.origin.name.split(',')[0]} ➔ {ride.destination.name.split(',')[0]}
                      </td>
                      <td className="p-3 font-semibold text-slate-900">R$ {ride.suggestedPrice.toFixed(2)}</td>
                      <td className="p-3">
                        <span className={`px-2 py-0.5 rounded text-[9px] font-bold uppercase border ${
                          ride.status === 'requested' ? 'bg-amber-100 text-amber-700 border-amber-200/30' :
                          ride.status === 'negotiating' ? 'bg-orange-100 text-orange-700 border-orange-200/30' :
                          ride.status === 'in_progress' ? 'bg-violet-100 text-violet-700 border-violet-200/30 animate-pulse' :
                          ride.status === 'completed' ? 'bg-emerald-100 text-emerald-700 border-emerald-200/30' :
                          'bg-slate-100 text-slate-600 border-slate-200/30'
                        }`}>
                          {ride.status === 'requested' ? 'Aguardando' :
                           ride.status === 'negotiating' ? 'Negociando' :
                           ride.status === 'in_progress' ? 'Em Viagem' :
                           ride.status === 'completed' ? 'Finalizada' : 'Cancelada'}
                        </span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};
