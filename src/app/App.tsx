import React, { useState, useEffect } from 'react';
import { ThemeProvider } from './providers/ThemeProvider';
import { SupabaseProvider, useSupabase } from './providers/SupabaseProvider';
import { AuthProvider, useAuth } from './providers/AuthProvider';
import { RealtimeProvider, useRealtime } from './providers/RealtimeProvider';
import { Routes } from './routes';
import { LocationPoint, RideRequest, Proposal, ChatMessage, AppSystemSettings, Review, User } from '@/src/shared/types';
import { PRESIDENTE_PRUDENTE_LOCATIONS, DEFAULT_SETTINGS } from '@/src/shared/constants';
import { calculateDistance, calculateSuggestedPrice } from '@/src/shared/utils/calculations';
import { Shield, LogOut, Bell, Sparkles, CheckCircle2 } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

// Hooks & Services
import { useRides } from '@/src/modules/rides/hooks/useRides';
import { useDriver } from '@/src/modules/driver/hooks/useDriver';
import { rideService } from '@/src/modules/rides/services/rideService';
import { driverService } from '@/src/modules/driver/services/driverService';
import { mapPaymentMethodToDb } from '@/src/shared/utils/mappers';

const AppContent: React.FC = () => {
  const { currentUser, drivers, passengers, setCurrentUser, setPassengers, setDrivers, handleLogout, handleQuickSwitchRole } = useAuth();
  const { isSupabaseConfigured, modeText } = useSupabase();
  const { notification, showNotification, setNotification } = useRealtime();

  // Local/Fallback States for prototype feel
  const [localActiveRides, setLocalActiveRides] = useState<RideRequest[]>(() => {
    const saved = localStorage.getItem('roxou_active_rides');
    return saved ? JSON.parse(saved) : [];
  });

  const [localProposals, setLocalProposals] = useState<Proposal[]>(() => {
    const saved = localStorage.getItem('roxou_proposals');
    return saved ? JSON.parse(saved) : [];
  });

  // Database mode Hooks
  const { rides: dbRides, offers: dbOffers, loading: ridesLoading, error: ridesError, refresh: refreshRides } = useRides(
    currentUser?.id,
    currentUser?.role
  );

  const { stats: driverStats, refreshStats: refreshDriverStats } = useDriver(
    currentUser?.id
  );

  const activeRides = isSupabaseConfigured ? dbRides : localActiveRides;
  const proposals = isSupabaseConfigured ? dbOffers : localProposals;

  const setActiveRides = (updater: any) => {
    if (typeof updater === 'function') {
      setLocalActiveRides(updater);
    } else {
      setLocalActiveRides(updater);
    }
  };

  const setProposals = (updater: any) => {
    if (typeof updater === 'function') {
      setLocalProposals(updater);
    } else {
      setLocalProposals(updater);
    }
  };

  const [globalLoading, setGlobalLoading] = useState<boolean>(false);
  const [globalError, setGlobalError] = useState<string | null>(null);

  const [settings, setSettings] = useState<AppSystemSettings>(() => {
    const saved = localStorage.getItem('roxou_settings');
    return saved ? JSON.parse(saved) : DEFAULT_SETTINGS;
  });

  // Map selections (shared to link clicking map landmarks with form)
  const [mapOrigin, setMapOrigin] = useState<LocationPoint | null>(null);
  const [mapDestination, setMapDestination] = useState<LocationPoint | null>(null);

  // Active Ride Progress State
  const [simulatedProgress, setSimulatedProgress] = useState<number>(0);

  // Sync state with localstorage
  useEffect(() => {
    localStorage.setItem('roxou_active_rides', JSON.stringify(localActiveRides));
  }, [localActiveRides]);

  useEffect(() => {
    localStorage.setItem('roxou_proposals', JSON.stringify(localProposals));
  }, [localProposals]);

  useEffect(() => {
    localStorage.setItem('roxou_settings', JSON.stringify(settings));
  }, [settings]);

  // Simulated ride progress interval for active ride
  useEffect(() => {
    const activeRunningRide = activeRides.find(
      (r) => r.status === 'in_progress' && (r.passengerId === currentUser?.id || r.driverId === currentUser?.id)
    );

    if (!activeRunningRide) {
      setSimulatedProgress(0);
      return;
    }

    const interval = setInterval(() => {
      setSimulatedProgress((prev) => {
        if (prev >= 100) {
          clearInterval(interval);
          return 100;
        }
        return Math.min(100, prev + 10);
      });
    }, 4000);

    return () => clearInterval(interval);
  }, [activeRides, currentUser]);

  // Passenger proposal simulator
  useEffect(() => {
    const requestedRide = activeRides.find(
      (r) => r.passengerId === currentUser?.id && (r.status === 'requested' || r.status === 'negotiating')
    );

    if (!requestedRide) return;

    // Simulate 1st proposal
    const timer1 = setTimeout(() => {
      const driver = drivers.find((d) => d.id === 'd1' && d.status === 'approved') || drivers[0];
      const exists = proposals.some((p) => p.requestId === requestedRide.id && p.driverId === driver.id);
      if (exists) return;

      const newProposal: Proposal = {
        id: 'prop_' + Date.now() + '_1',
        requestId: requestedRide.id,
        driverId: driver.id,
        driverName: driver.name,
        driverRating: driver.rating,
        driverAvatar: driver.avatar,
        vehicleInfo: `${driver.vehicleInfo?.model} ${driver.vehicleInfo?.color} (${driver.vehicleInfo?.plate})`,
        price: requestedRide.suggestedPrice,
        status: 'pending',
        createdAt: new Date().toISOString()
      };

      setProposals((prev) => [...prev, newProposal]);
      showNotification(`🚕 ${driver.name} aceitou fazer a corrida pelo seu preço sugerido de R$ ${requestedRide.suggestedPrice.toFixed(2)}!`);
    }, 4000);

    // Simulate 2nd counter-proposal
    const timer2 = setTimeout(() => {
      const driver = drivers.find((d) => d.id === 'd2' && d.status === 'approved') || drivers[1];
      const exists = proposals.some((p) => p.requestId === requestedRide.id && p.driverId === driver.id);
      if (exists) return;

      const counterPrice = Math.round(requestedRide.suggestedPrice + 4.50);
      const newProposal: Proposal = {
        id: 'prop_' + Date.now() + '_2',
        requestId: requestedRide.id,
        driverId: driver.id,
        driverName: driver.name,
        driverRating: driver.rating,
        driverAvatar: driver.avatar,
        vehicleInfo: `${driver.vehicleInfo?.model} ${driver.vehicleInfo?.color} (${driver.vehicleInfo?.plate})`,
        price: counterPrice,
        status: 'pending',
        createdAt: new Date().toISOString()
      };

      setProposals((prev) => [...prev, newProposal]);
      showNotification(`💬 ${driver.name} fez uma contraproposta de R$ ${counterPrice.toFixed(2)} para sua viagem!`);
    }, 8500);

    return () => {
      clearTimeout(timer1);
      clearTimeout(timer2);
    };
  }, [activeRides, currentUser, drivers]);

  // Handlers
  const handleRequestRide = async (data: {
    origin: LocationPoint;
    destination: LocationPoint;
    distanceKm: number;
    suggestedPrice: number;
    paymentMethod: string;
    passengerCount: number;
    observation?: string;
  }) => {
    if (!currentUser) return;
    const safetyCode = Math.floor(1000 + Math.random() * 9000).toString();

    if (isSupabaseConfigured) {
      setGlobalLoading(true);
      setGlobalError(null);
      try {
        await rideService.createRideRequest({
          passenger_id: currentUser.id,
          origin_name: data.origin.name,
          origin_address: data.origin.address || '',
          origin_lat: data.origin.lat,
          origin_lng: data.origin.lng,
          origin_bairro: data.origin.bairro,
          destination_name: data.destination.name,
          destination_address: data.destination.address || '',
          destination_lat: data.destination.lat,
          destination_lng: data.destination.lng,
          destination_bairro: data.destination.bairro,
          stops: [],
          distance_km: data.distanceKm,
          suggested_price: data.suggestedPrice,
          payment_method: mapPaymentMethodToDb(data.paymentMethod),
          passenger_count: data.passengerCount,
          observation: data.observation,
          safety_code: safetyCode,
        });
        showNotification('Sua solicitação de corrida foi publicada no Supabase!');
        refreshRides();
      } catch (err: any) {
        console.error('Error in handleRequestRide:', err);
        setGlobalError(err.message || 'Erro ao publicar solicitação de corrida.');
      } finally {
        setGlobalLoading(false);
      }
    } else {
      const newRequest: RideRequest = {
        id: 'req_' + Date.now(),
        passengerId: currentUser.id,
        passengerName: currentUser.name,
        passengerRating: currentUser.rating,
        passengerAvatar: currentUser.avatar,
        origin: data.origin,
        destination: data.destination,
        distanceKm: data.distanceKm,
        suggestedPrice: data.suggestedPrice,
        paymentMethod: data.paymentMethod,
        passengerCount: data.passengerCount,
        observation: data.observation,
        status: 'requested',
        createdAt: new Date().toISOString(),
        safetyCode,
        chatMessages: []
      };

      setActiveRides((prev) => [newRequest, ...prev]);
      showNotification('Sua solicitação de corrida foi publicada! Aguardando propostas...');
    }
  };

  const handleAcceptProposal = async (proposal: Proposal) => {
    if (isSupabaseConfigured) {
      setGlobalLoading(true);
      setGlobalError(null);
      try {
        await rideService.acceptRideOffer(proposal.id);
        showNotification(`Sua corrida foi aceita por R$ ${proposal.price.toFixed(2)}!`);
        refreshRides();
      } catch (err: any) {
        console.error('Error in handleAcceptProposal:', err);
        setGlobalError(err.message || 'Erro ao aceitar proposta.');
      } finally {
        setGlobalLoading(false);
      }
    } else {
      setActiveRides((prev) =>
        prev.map((r) => {
          if (r.id === proposal.requestId) {
            const driverModel = drivers.find((d) => d.id === proposal.driverId);
            return {
              ...r,
              status: 'in_progress',
              driverId: proposal.driverId,
              driverName: proposal.driverName,
              driverRating: proposal.driverRating,
              driverAvatar: proposal.driverAvatar,
              driverPhone: driverModel?.phone || '(18) 99123-4567',
              vehicleInfo: proposal.vehicleInfo,
              finalPrice: proposal.price,
              chatMessages: [
                {
                  id: 'init_msg',
                  senderRole: 'driver',
                  text: `Olá ${r.passengerName}! Corrida confirmada. Já estou a caminho do ponto de embarque.`,
                  timestamp: new Date().toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
                }
              ]
            };
          }
          return r;
        })
      );

      setDrivers((prev) =>
        prev.map((d) => (d.id === proposal.driverId ? { ...d, status: 'busy' } : d))
      );

      setProposals((prev) =>
        prev.map((p) => (p.requestId === proposal.requestId ? { ...p, status: 'declined' as const } : p))
      );

      showNotification(`Sua corrida foi aceita por R$ ${proposal.price.toFixed(2)}!`);
    }
  };

  const handleDeclineProposal = async (proposalId: string) => {
    if (isSupabaseConfigured) {
      try {
        await rideService.declineRideOffer(proposalId);
        showNotification('Proposta recusada com sucesso.');
        refreshRides();
      } catch (err: any) {
        console.error(err);
      }
    } else {
      setProposals((prev) =>
        prev.map((p) => (p.id === proposalId ? { ...p, status: 'declined' as const } : p))
      );
    }
  };

  const handleCancelRide = async (requestId: string) => {
    if (isSupabaseConfigured) {
      setGlobalLoading(true);
      try {
        await rideService.cancelRideRequest(requestId, 'passenger');
        showNotification('Sua solicitação de viagem foi cancelada no Supabase.');
        refreshRides();
      } catch (err: any) {
        console.error(err);
        setGlobalError(err.message || 'Erro ao cancelar viagem.');
      } finally {
        setGlobalLoading(false);
      }
    } else {
      setActiveRides((prev) =>
        prev.map((r) => (r.id === requestId ? { ...r, status: 'cancelled' as const } : r))
      );
      showNotification('Sua solicitação de viagem foi cancelada.');
    }
  };

  const handleToggleOnline = async () => {
    if (!currentUser) return;
    const isNowOnline = currentUser.status !== 'online' && currentUser.status !== 'busy';
    const nextStatus = isNowOnline ? 'online' : 'offline';

    if (isSupabaseConfigured) {
      setGlobalLoading(true);
      try {
        await driverService.updateDriverOnlineStatus(currentUser.id, isNowOnline);
        const updatedUser = { ...currentUser, status: nextStatus as any };
        setCurrentUser(updatedUser);
        showNotification(isNowOnline ? 'Você está ONLINE no Supabase. Aguardando passageiros...' : 'Você está OFFLINE no Supabase.');
      } catch (err: any) {
        console.error(err);
        setGlobalError(err.message || 'Erro ao atualizar status online.');
      } finally {
        setGlobalLoading(false);
      }
    } else {
      const updatedUser = { ...currentUser, status: nextStatus as any };
      setCurrentUser(updatedUser);

      setDrivers((prev) =>
        prev.map((d) => (d.id === currentUser.id ? { ...d, status: nextStatus as any } : d))
      );

      showNotification(isNowOnline ? 'Você está ONLINE. Aguardando passageiros...' : 'Você está OFFLINE.');
    }
  };

  const handleAcceptRideAtPrice = async (requestId: string, price: number) => {
    if (!currentUser) return;

    if (isSupabaseConfigured) {
      setGlobalLoading(true);
      try {
        await rideService.acceptRideRequestDirectly(requestId, currentUser.id, price);
        showNotification('Corrida aceita no Supabase! Vá até o ponto de embarque.');
        refreshRides();
      } catch (err: any) {
        console.error(err);
        setGlobalError(err.message || 'Erro ao aceitar corrida.');
      } finally {
        setGlobalLoading(false);
      }
    } else {
      setActiveRides((prev) =>
        prev.map((r) => {
          if (r.id === requestId) {
            return {
              ...r,
              status: 'in_progress',
              driverId: currentUser.id,
              driverName: currentUser.name,
              driverRating: currentUser.rating,
              driverAvatar: currentUser.avatar,
              driverPhone: currentUser.phone,
              vehicleInfo: `${currentUser.vehicleInfo?.model} ${currentUser.vehicleInfo?.color} (${currentUser.vehicleInfo?.plate})`,
              finalPrice: price,
              chatMessages: [
                {
                  id: 'init_msg',
                  senderRole: 'driver',
                  text: 'Olá! Aceitei sua corrida no valor sugerido. Chego em minutos.',
                  timestamp: new Date().toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
                }
              ]
            };
          }
          return r;
        })
      );

      setDrivers((prev) =>
        prev.map((d) => (d.id === currentUser.id ? { ...d, status: 'busy' } : d))
      );

      showNotification('Corrida aceita! Vá até o ponto de embarque do passageiro.');
    }
  };

  const handleSubmitCounterProposal = async (requestId: string, price: number) => {
    if (!currentUser) return;

    if (isSupabaseConfigured) {
      setGlobalLoading(true);
      try {
        await rideService.createRideOffer({
          request_id: requestId,
          driver_id: currentUser.id,
          price,
          estimated_time_minutes: 5,
        });
        showNotification(`Sua proposta de R$ ${price.toFixed(2)} foi enviada no Supabase.`);
        refreshRides();
      } catch (err: any) {
        console.error(err);
        setGlobalError(err.message || 'Erro ao enviar proposta.');
      } finally {
        setGlobalLoading(false);
      }
    } else {
      const newProp: Proposal = {
        id: 'prop_' + Date.now() + '_' + currentUser.id,
        requestId,
        driverId: currentUser.id,
        driverName: currentUser.name,
        driverRating: currentUser.rating,
        driverAvatar: currentUser.avatar,
        vehicleInfo: `${currentUser.vehicleInfo?.model} ${currentUser.vehicleInfo?.color} (${currentUser.vehicleInfo?.plate})`,
        price,
        status: 'pending',
        createdAt: new Date().toISOString()
      };

      setProposals((prev) => [...prev, newProp]);

      setActiveRides((prev) =>
        prev.map((r) => (r.id === requestId ? { ...r, status: 'negotiating' as const } : r))
      );

      showNotification(`Sua proposta de R$ ${price.toFixed(2)} foi enviada ao passageiro.`);
    }
  };

  const handleRejectRide = (requestId: string) => {
    showNotification('Solicitação ignorada com sucesso.');
  };

  const handleCompleteRide = async (rideId: string) => {
    if (isSupabaseConfigured) {
      setGlobalLoading(true);
      try {
        await rideService.completeRide(rideId);
        showNotification('Sua viagem foi concluída com sucesso no Supabase!');
        refreshRides();
      } catch (err: any) {
        console.error(err);
        setGlobalError(err.message || 'Erro ao concluir viagem.');
      } finally {
        setGlobalLoading(false);
      }
    } else {
      setActiveRides((prev) =>
        prev.map((r) => (r.id === rideId ? { ...r, status: 'completed' as const } : r))
      );

      const ride = activeRides.find((r) => r.id === rideId);
      if (ride && ride.driverId) {
        setDrivers((prev) =>
          prev.map((d) => (d.id === ride.driverId ? { ...d, status: 'online', totalRides: d.totalRides + 1 } : d))
        );
        
        if (currentUser?.id === ride.driverId) {
          setCurrentUser((prev) => prev ? { ...prev, status: 'online', totalRides: prev.totalRides + 1 } : null);
        }
      }

      if (ride && currentUser?.id === ride.passengerId) {
        setPassengers((prev) =>
          prev.map((p) => (p.id === ride.passengerId ? { ...p, totalRides: p.totalRides + 1 } : p))
        );
        setCurrentUser((prev) => prev ? { ...prev, totalRides: prev.totalRides + 1 } : null);
      }

      showNotification('Sua viagem foi concluída com sucesso!');
    }
  };

  const handleAddChatMessage = (rideId: string, message: ChatMessage) => {
    setActiveRides((prev) =>
      prev.map((r) => {
        if (r.id === rideId) {
          return {
            ...r,
            chatMessages: [...(r.chatMessages || []), message]
          };
        }
        return r;
      })
    );
  };

  const handleAddReview = (review: Review) => {
    const ride = activeRides.find((r) => r.id === review.rideId);
    if (!ride) return;

    if (review.reviewerRole === 'passenger' && ride.driverId) {
      setDrivers((prev) =>
        prev.map((d) => {
          if (d.id === ride.driverId) {
            const nextRating = parseFloat(((d.rating * 9 + review.rating) / 10).toFixed(2));
            return { ...d, rating: nextRating };
          }
          return d;
        })
      );
    } else if (review.reviewerRole === 'driver') {
      setPassengers((prev) =>
        prev.map((p) => {
          if (p.id === ride.passengerId) {
            const nextRating = parseFloat(((p.rating * 9 + review.rating) / 10).toFixed(2));
            return { ...p, rating: nextRating };
          }
          return p;
        })
      );
    }

    showNotification('Sua avaliação foi registrada na plataforma!');
  };

  const handleUpdateSettings = (newSettings: AppSystemSettings) => {
    setSettings(newSettings);
  };

  const handleApproveDriver = (driverId: string) => {
    setDrivers((prev) =>
      prev.map((d) => (d.id === driverId ? { ...d, status: 'approved' } : d))
    );
    showNotification('Cadastro de motorista APROVADO com sucesso.');
  };

  const handleRejectDriver = (driverId: string) => {
    setDrivers((prev) =>
      prev.map((d) => (d.id === driverId ? { ...d, status: 'rejected' } : d))
    );
    showNotification('Cadastro de motorista REJEITADO.');
  };

  const handleSimulatePassengerCall = () => {
    if (!currentUser || currentUser.role !== 'driver') return;

    const indexO = Math.floor(Math.random() * PRESIDENTE_PRUDENTE_LOCATIONS.length);
    let indexD = Math.floor(Math.random() * PRESIDENTE_PRUDENTE_LOCATIONS.length);
    if (indexD === indexO) {
      indexD = (indexO + 1) % PRESIDENTE_PRUDENTE_LOCATIONS.length;
    }

    const o = PRESIDENTE_PRUDENTE_LOCATIONS[indexO];
    const d = PRESIDENTE_PRUDENTE_LOCATIONS[indexD];
    const distanceKm = calculateDistance(o, d);
    const suggestedPrice = calculateSuggestedPrice(distanceKm, settings);

    const randPassNames = ['Carlos Nobre', 'Gabriela Rocha', 'Thiago Lima', 'Patrícia Sales'];
    const randName = randPassNames[Math.floor(Math.random() * randPassNames.length)];
    const safetyCode = Math.floor(1000 + Math.random() * 9000).toString();

    const simulatedRequest: RideRequest = {
      id: 'req_simulated_' + Date.now(),
      passengerId: 'temp_pass_' + Date.now(),
      passengerName: randName,
      passengerRating: parseFloat((4.5 + Math.random() * 0.5).toFixed(1)),
      passengerAvatar: `https://api.dicebear.com/7.x/adventurer/svg?seed=${randName}`,
      origin: o,
      destination: d,
      distanceKm,
      suggestedPrice,
      paymentMethod: 'Pix',
      passengerCount: Math.floor(1 + Math.random() * 3),
      observation: Math.random() > 0.5 ? 'Estou na calçada em frente ao local.' : undefined,
      status: 'requested',
      createdAt: new Date().toISOString(),
      safetyCode,
      chatMessages: []
    };

    setActiveRides((prev) => [simulatedRequest, ...prev]);
    showNotification(`🔔 Nova viagem solicitada por ${randName} de R$ ${suggestedPrice.toFixed(2)} disponível!`);
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 flex flex-col antialiased font-sans pb-10" id="roxou-app-root">
      
      {/* Toast Notification Banner */}
      <AnimatePresence>
        {notification && (
          <motion.div
            initial={{ opacity: 0, y: -50, scale: 0.9 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -20, scale: 0.9 }}
            className="fixed top-4 left-1/2 -translate-x-1/2 z-50 w-full max-w-sm px-4"
          >
            <div className="bg-slate-900 text-white p-3.5 rounded-2xl shadow-2xl border border-violet-500/30 flex items-center gap-2.5">
              <Bell className="w-5 h-5 text-violet-400 shrink-0 animate-bounce-slow" />
              <p className="text-xs font-semibold leading-snug">{notification}</p>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Top Main Navigation Header */}
      <header className="bg-white border-b border-slate-200 sticky top-0 z-40 shadow-xs">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-violet-600 rounded-xl flex items-center justify-center text-white font-black text-xl shadow-md shadow-violet-600/20">R</div>
            <div className="flex flex-col">
              <span className="text-base sm:text-lg font-bold text-slate-800 leading-tight">Roxou Mobilidade</span>
              <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">Presidente Prudente - SP</span>
            </div>
          </div>

          <div className="flex items-center gap-6">
            <div className="hidden md:flex items-center gap-2 bg-emerald-50 px-3 py-1.5 rounded-full border border-emerald-100">
              <div className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse"></div>
              <span className="text-xs font-semibold text-emerald-700">
                {drivers.filter(d => d.role === 'driver' && d.status === 'approved').length + 342} Motoristas Online
              </span>
            </div>

            {currentUser ? (
              <div className="flex items-center gap-4 sm:border-l sm:pl-6 sm:border-slate-200">
                <div className="text-right hidden sm:block">
                  <p className="text-[9px] uppercase font-extrabold tracking-wider text-slate-400">
                    {currentUser.role === 'passenger' ? 'Passageiro' : currentUser.role === 'driver' ? 'Motorista' : 'Administrador'}
                  </p>
                  <p className="text-sm font-bold text-slate-700">{currentUser.name}</p>
                </div>
                <div className="relative">
                  <img
                    src={currentUser.avatar}
                    className="w-10 h-10 bg-slate-100 rounded-xl border-2 border-slate-200 shadow-xs object-cover"
                    alt={currentUser.name}
                    referrerPolicy="no-referrer"
                  />
                  <span className={`absolute -bottom-0.5 -right-0.5 block h-3 w-3 rounded-full ${currentUser.status === 'online' || currentUser.status === 'busy' ? 'bg-emerald-500' : 'bg-slate-400'} ring-2 ring-white`}></span>
                </div>
                <button
                  onClick={handleLogout}
                  className="p-2 bg-slate-50 hover:bg-red-50 text-slate-400 hover:text-red-600 rounded-xl border border-slate-200 hover:border-red-100 transition-all shadow-xs"
                  title="Sair da Conta"
                  id="btn-logout"
                >
                  <LogOut className="w-4 h-4" />
                </button>
              </div>
            ) : (
              <div className="text-xs font-bold text-slate-400 uppercase tracking-widest flex items-center gap-2">
                <Shield className="w-4 h-4 text-violet-500" />
                <span className="hidden sm:inline">Roxou Seguro</span>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* Supabase Integration Status Alert */}
      <div className="w-full">
        <div className={`text-white text-[10px] py-2 text-center flex items-center justify-center gap-1.5 font-bold uppercase tracking-wider ${isSupabaseConfigured ? 'bg-emerald-500' : 'bg-amber-500'}`}>
          {isSupabaseConfigured ? <CheckCircle2 className="w-3.5 h-3.5" /> : <Shield className="w-3.5 h-3.5 animate-pulse" />}
          <span>{modeText} — {isSupabaseConfigured ? 'Dados Reais Ativos' : 'Modo demonstração ativo'} em Presidente Prudente/SP!</span>
        </div>
      </div>

      {/* Global Error Alert */}
      {globalError && (
        <div className="bg-red-600 text-white text-xs py-2.5 px-4 text-center font-bold flex items-center justify-center gap-2">
          <span>⚠️ {globalError}</span>
          <button onClick={() => setGlobalError(null)} className="underline ml-2 hover:text-slate-100">[Fechar]</button>
        </div>
      )}

      {/* Sandbox instructions helper for single window testing */}
      {currentUser && (
        <div className="bg-slate-900 text-white text-[10px] py-2.5 text-center flex flex-wrap items-center justify-center gap-2 font-medium border-b border-slate-800">
          <Sparkles className="w-3.5 h-3.5 text-amber-400 animate-pulse" />
          <span>PROTÓTIPO MULTI-FUNÇÕES: Alterne entre perfis de teste rápidos:</span>
          <div className="inline-flex bg-slate-800/80 p-0.5 rounded-lg border border-slate-700">
            <button
              onClick={() => handleQuickSwitchRole('passenger')}
              className={`px-2.5 py-0.5 rounded text-[9px] font-bold transition-all ${currentUser.role === 'passenger' ? 'bg-violet-600 text-white shadow-xs' : 'text-slate-400 hover:text-white'}`}
            >
              Passageiro
            </button>
            <button
              onClick={() => handleQuickSwitchRole('driver')}
              className={`px-2.5 py-0.5 rounded text-[9px] font-bold transition-all ${currentUser.role === 'driver' ? 'bg-violet-600 text-white shadow-xs' : 'text-slate-400 hover:text-white'}`}
            >
              Motorista
            </button>
            <button
              onClick={() => handleQuickSwitchRole('admin')}
              className={`px-2.5 py-0.5 rounded text-[9px] font-bold transition-all ${currentUser.role === 'admin' ? 'bg-violet-600 text-white shadow-xs' : 'text-slate-400 hover:text-white'}`}
            >
              Admin
            </button>
          </div>
        </div>
      )}

      {/* Main Content Layout container */}
      <main className="flex-1 max-w-6xl w-full mx-auto px-4 sm:px-6 py-6" id="roxou-main-content">
        <Routes
          activeRides={activeRides}
          proposals={proposals}
          settings={settings}
          mapOrigin={mapOrigin}
          mapDestination={mapDestination}
          simulatedProgress={simulatedProgress}
          setMapOrigin={setMapOrigin}
          setMapDestination={setMapDestination}
          setSimulatedProgress={setSimulatedProgress}
          onUpdateSettings={handleUpdateSettings}
          onApproveDriver={handleApproveDriver}
          onRejectDriver={handleRejectDriver}
          onRequestRide={handleRequestRide}
          onAcceptProposal={handleAcceptProposal}
          onDeclineProposal={handleDeclineProposal}
          onCancelRide={handleCancelRide}
          onToggleOnline={handleToggleOnline}
          onAcceptRideAtPrice={handleAcceptRideAtPrice}
          onSubmitCounterProposal={handleSubmitCounterProposal}
          onRejectRide={handleRejectRide}
          onCompleteRide={handleCompleteRide}
          onAddChatMessage={handleAddChatMessage}
          onAddReview={handleAddReview}
          onSimulatePassengerCall={handleSimulatePassengerCall}
        />
      </main>

      {/* Bottom Bar Info */}
      <footer className="max-w-6xl mx-auto w-full px-4 sm:px-6 mt-8 pt-6 border-t border-slate-200 flex flex-col sm:flex-row justify-between items-center gap-4 text-center sm:text-left">
        <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest italic">Roxou Mobilidade © 2026 • Desenvolvido para Presidente Prudente</p>
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2">
            <div className="w-2 h-2 bg-emerald-400 rounded-full animate-pulse"></div>
            <span className="text-[10px] font-bold text-slate-600">SISTEMA OPERACIONAL</span>
          </div>
          <span className="text-[10px] font-mono text-slate-400">v2.4.0-prod</span>
        </div>
      </footer>
    </div>
  );
};

export const App: React.FC = () => {
  return (
    <ThemeProvider>
      <SupabaseProvider>
        <AuthProvider>
          <RealtimeProvider>
            <AppContent />
          </RealtimeProvider>
        </AuthProvider>
      </SupabaseProvider>
    </ThemeProvider>
  );
};

export default App;
