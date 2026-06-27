import React, { useState } from 'react';
import { AppSystemSettings, User, RideRequest } from '@/src/shared/types';
import { PRESIDENTE_PRUDENTE_LOCATIONS } from '@/src/shared/constants';
import { 
  Settings, 
  Shield, 
  Users, 
  DollarSign, 
  Check, 
  X, 
  Award, 
  FileText, 
  ChevronRight, 
  TrendingUp, 
  Key, 
  PieChart, 
  BarChart, 
  Map, 
  Clock,
  Activity,
  UserCheck,
  MapPin
} from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

interface AdminProps {
  settings: AppSystemSettings;
  onUpdateSettings: (newSettings: AppSystemSettings) => void;
  drivers: User[];
  onApproveDriver: (driverId: string) => void;
  onRejectDriver: (driverId: string) => void;
  activeRides: RideRequest[];
}

export const AdminModule: React.FC<AdminProps> = ({
  settings,
  onUpdateSettings,
  drivers,
  onApproveDriver,
  onRejectDriver,
  activeRides,
}) => {
  const [activeSubSection, setActiveSubSection] = useState<'metrics' | 'approvals' | 'config' | 'heat'>('metrics');

  const [minPriceInput, setMinPriceInput] = useState(settings.minRideValue.toString());
  const [pricePerKmInput, setPricePerKmInput] = useState(settings.pricePerKm.toString());
  const [commissionInput, setCommissionInput] = useState(settings.commissionPercent.toString());
  const [successConfigMsg, setSuccessConfigMsg] = useState('');

  const pendingDrivers = drivers.filter((d) => d.status === 'pending_approval');
  const approvedDrivers = drivers.filter((d) => d.status === 'approved' || d.status === 'online' || d.status === 'busy');
  const onlineDrivers = drivers.filter((d) => d.status === 'online' || d.status === 'busy');

  const totalRidesCount = activeRides.length;
  const completedRides = activeRides.filter((r) => r.status === 'completed');
  const inProgressRides = activeRides.filter((r) => r.status === 'in_progress');
  const grossNegotiatedValue = completedRides.reduce((sum, r) => sum + (r.finalPrice || r.suggestedPrice), 0);
  const platformEarnings = (grossNegotiatedValue * settings.commissionPercent) / 100;

  const neighborhoodDemands = [
    { name: 'Centro', count: 48, revenue: 640.00 },
    { name: 'Parque do Povo', count: 35, revenue: 490.00 },
    { name: 'Prudenshopping', count: 31, revenue: 380.00 },
    { name: 'Unoeste', count: 28, revenue: 510.00 },
    { name: 'Ana Jacinta', count: 19, revenue: 290.00 },
    { name: 'Vila Marcondes', count: 14, revenue: 180.00 },
  ];

  const peakHours = [
    { period: 'Manhã (07h-09h)', intensity: 'Alta Demanda 📈' },
    { period: 'Almoço (11h30-13h30)', intensity: 'Média 🚗' },
    { period: 'Tarde/Noite (17h-19h30)', intensity: 'Pico Crítico 🔥 (+20%)' },
    { period: 'Madrugada (00h-04h)', intensity: 'Sazonal/Eventos 🌙 (+30%)' },
  ];

  const handleSaveSettings = (e: React.FormEvent) => {
    e.preventDefault();
    const minVal = parseFloat(minPriceInput);
    const perKm = parseFloat(pricePerKmInput);
    const comm = parseFloat(commissionInput);

    if (isNaN(minVal) || isNaN(perKm) || isNaN(comm)) {
      alert('Por favor, insira valores válidos.');
      return;
    }

    if (perKm < 2.0) {
      alert('O valor mínimo exigido de tarifa por quilômetro é R$ 2,00 por Km.');
      return;
    }

    onUpdateSettings({
      minRideValue: minVal,
      pricePerKm: perKm,
      commissionPercent: comm,
    });

    setSuccessConfigMsg('Configurações de tarifa aplicadas com sucesso!');
    setTimeout(() => setSuccessConfigMsg(''), 3000);
  };

  return (
    <div className="w-full max-w-md mx-auto space-y-4" id="admin-module">
      {/* Title */}
      <div className="bg-gradient-to-br from-slate-950 via-slate-900 to-indigo-950 text-white p-5 rounded-3xl shadow-xl flex items-center gap-3 border border-slate-800 relative overflow-hidden">
        <div className="absolute top-0 right-0 w-24 h-24 bg-violet-600/10 rounded-full blur-2xl pointer-events-none"></div>
        <div className="p-3 bg-violet-600/20 text-violet-400 rounded-2xl border border-violet-500/20">
          <Key className="w-5 h-5" />
        </div>
        <div>
          <span className="text-[9px] text-violet-300 font-black uppercase tracking-widest block">Painel Geral Administrativo</span>
          <h2 className="text-base font-black tracking-tight mt-0.5">Roxou Central Admin</h2>
        </div>
      </div>

      {/* Subsection Tab Toggles */}
      <div className="flex bg-slate-100 p-1 rounded-2xl border border-slate-200/65 shadow-xs text-xs font-bold">
        <button
          onClick={() => setActiveSubSection('metrics')}
          className={`flex-1 py-2 text-center rounded-xl transition-all ${
            activeSubSection === 'metrics' ? 'bg-white text-slate-900 shadow-xs' : 'text-slate-500 hover:text-slate-800'
          }`}
        >
          Métricas
        </button>
        <button
          onClick={() => setActiveSubSection('heat')}
          className={`flex-1 py-2 text-center rounded-xl transition-all ${
            activeSubSection === 'heat' ? 'bg-white text-slate-900 shadow-xs' : 'text-slate-500 hover:text-slate-800'
          }`}
        >
          Mapa Calor / Bairros
        </button>
        <button
          onClick={() => setActiveSubSection('approvals')}
          className={`flex-1 py-2 text-center rounded-xl transition-all flex items-center justify-center gap-1.5 ${
            activeSubSection === 'approvals' ? 'bg-white text-slate-900 shadow-xs' : 'text-slate-500 hover:text-slate-800'
          }`}
        >
          <span>Aprovações</span>
          {pendingDrivers.length > 0 && (
            <span className="w-4 h-4 bg-red-500 text-[9px] text-white font-black rounded-full flex items-center justify-center animate-pulse">
              {pendingDrivers.length}
            </span>
          )}
        </button>
        <button
          onClick={() => setActiveSubSection('config')}
          className={`flex-1 py-2 text-center rounded-xl transition-all ${
            activeSubSection === 'config' ? 'bg-white text-slate-900 shadow-xs' : 'text-slate-500 hover:text-slate-800'
          }`}
        >
          Config
        </button>
      </div>

      {/* SUBSECTION: METRICS & SYSTEM DATA */}
      {activeSubSection === 'metrics' && (
        <div className="space-y-3.5">
          {/* Bento grid of stats */}
          <div className="grid grid-cols-2 gap-3">
            <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs space-y-1">
              <p className="text-[9px] font-bold text-slate-400 uppercase tracking-wider">Viagens Totais</p>
              <p className="text-xl font-black text-slate-800">{totalRidesCount}</p>
              <div className="text-[9px] text-slate-450 flex items-center gap-1 pt-0.5">
                <Activity className="w-3 h-3 text-violet-500" />
                <span>{inProgressRides.length} em andamento</span>
              </div>
            </div>

            <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs space-y-1">
              <p className="text-[9px] font-bold text-slate-400 uppercase tracking-wider">Motoristas Ativos</p>
              <p className="text-xl font-black text-slate-800">{approvedDrivers.length}</p>
              <div className="text-[9px] text-emerald-600 font-bold flex items-center gap-1 pt-0.5">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 inline-block"></span>
                <span>{onlineDrivers.length} online agora</span>
              </div>
            </div>

            <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs col-span-2 space-y-2">
              <div className="flex justify-between items-center border-b border-slate-100 pb-1.5">
                <p className="text-[9px] font-black text-slate-450 uppercase tracking-wider">Desempenho Geral de Receita</p>
                <DollarSign className="w-4 h-4 text-emerald-500" />
              </div>
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <p className="text-[10px] text-slate-400 font-semibold">Volume Negociado:</p>
                  <strong className="text-base font-black text-slate-850">R$ {grossNegotiatedValue.toFixed(2)}</strong>
                </div>
                <div>
                  <p className="text-[10px] text-slate-400 font-semibold">Tarifa Admin ({settings.commissionPercent}%):</p>
                  <strong className="text-base font-black text-violet-700">R$ {platformEarnings.toFixed(2)}</strong>
                </div>
              </div>
            </div>
          </div>

          {/* Quick List of Approved Drivers */}
          <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs space-y-2">
            <h4 className="text-[10px] font-black text-slate-500 uppercase tracking-wider">Lista de Motoristas Cadastrados</h4>
            <div className="space-y-2 max-h-[160px] overflow-y-auto pr-1">
              {drivers.map((drv) => (
                <div key={drv.id} className="flex justify-between items-center text-xs p-2.5 bg-slate-50 rounded-xl border border-slate-100">
                  <div className="flex items-center gap-2">
                    <img src={drv.avatar} className="w-7 h-7 rounded-lg object-cover border border-slate-200 bg-white" alt="" referrerPolicy="no-referrer" />
                    <div>
                      <p className="font-bold text-slate-800">{drv.name}</p>
                      <p className="text-[9px] text-slate-400">{drv.vehicleInfo?.model || 'Sem veículo'}</p>
                    </div>
                  </div>
                  <span className={`text-[9px] font-bold px-2 py-0.5 rounded-full ${
                    drv.status === 'pending_approval' ? 'bg-amber-100 text-amber-800' : 'bg-emerald-100 text-emerald-800'
                  }`}>
                    {drv.status === 'pending_approval' ? 'Aguardando' : 'Aprovado'}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* SUBSECTION: HEAT MAP AND NEIGHBORHOODS */}
      {activeSubSection === 'heat' && (
        <div className="space-y-3.5">
          {/* Heat map info */}
          <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs space-y-2">
            <div className="flex items-center gap-1.5 border-b border-slate-100 pb-2">
              <Map className="w-4 h-4 text-violet-600" />
              <h4 className="text-xs font-black text-slate-800 uppercase tracking-wider">Demanda e Bairros de Presidente Prudente</h4>
            </div>
            <p className="text-[10px] text-slate-500 leading-normal">
              Acompanhamento de fluxo e demanda por regiões específicas de Presidente Prudente. Útil para motoristas se posicionarem nos bairros de pico.
            </p>

            <div className="space-y-2 pt-1">
              {neighborhoodDemands.map((nd, index) => (
                <div key={index} className="space-y-1">
                  <div className="flex justify-between text-[11px] font-bold text-slate-700">
                    <span>{nd.name}</span>
                    <span className="text-violet-700">{nd.count} chamados (R$ {nd.revenue.toFixed(2)})</span>
                  </div>
                  <div className="w-full bg-slate-100 h-2 rounded-full overflow-hidden">
                    <div 
                      className="bg-violet-600 h-full rounded-full" 
                      style={{ width: `${(nd.count / 48) * 100}%` }}
                    ></div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Peak Hours config */}
          <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs space-y-2">
            <div className="flex items-center gap-1.5 border-b border-slate-100 pb-2">
              <Clock className="w-4 h-4 text-amber-500" />
              <h4 className="text-xs font-black text-slate-800 uppercase tracking-wider">Horários de Pico Localizados</h4>
            </div>
            
            <div className="space-y-1.5">
              {peakHours.map((ph, idx) => (
                <div key={idx} className="flex justify-between text-xs bg-slate-50 p-2 rounded-xl border border-slate-100">
                  <span className="text-slate-600 font-medium">{ph.period}:</span>
                  <strong className="text-slate-800">{ph.intensity}</strong>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* SUBSECTION: APPROVALS */}
      {activeSubSection === 'approvals' && (
        <div className="space-y-3">
          <h4 className="text-xs font-black text-slate-600 uppercase px-1">Fila de Aprovação de Motoristas ({pendingDrivers.length})</h4>

          <AnimatePresence mode="popLayout">
            {pendingDrivers.length === 0 ? (
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="bg-white p-8 rounded-3xl border border-dashed border-slate-200 text-center space-y-2 text-slate-400"
              >
                <div className="w-10 h-10 bg-emerald-50 rounded-full flex items-center justify-center mx-auto text-emerald-600">
                  <Check className="w-5 h-5" />
                </div>
                <p className="text-xs font-bold text-slate-700">Tudo em dia!</p>
                <p className="text-[10px] text-slate-400">Não há motoristas pendentes de análise cadastral ou CNH no momento.</p>
              </motion.div>
            ) : (
              pendingDrivers.map((drv) => (
                <motion.div
                  key={drv.id}
                  layout
                  initial={{ opacity: 0, scale: 0.95 }}
                  animate={{ opacity: 1, scale: 1 }}
                  exit={{ opacity: 0, scale: 0.95 }}
                  className="bg-white p-4 rounded-3xl border border-slate-200 shadow-xs space-y-3"
                >
                  <div className="flex gap-3">
                    <img src={drv.avatar} className="w-10 h-10 rounded-xl object-cover border border-slate-200 bg-slate-50" alt="" referrerPolicy="no-referrer" />
                    <div className="flex-1 min-w-0">
                      <h5 className="text-xs font-bold text-slate-900">{drv.name}</h5>
                      <p className="text-[10px] text-slate-400">{drv.email} • {drv.phone}</p>
                      <p className="text-[10px] font-bold text-violet-700 mt-1">Carro: {drv.vehicleInfo?.model} {drv.vehicleInfo?.color} ({drv.vehicleInfo?.plate})</p>
                    </div>
                  </div>

                  <div className="p-3 bg-slate-50 rounded-2xl border border-slate-100 flex items-center gap-2 text-xs">
                    <FileText className="w-4 h-4 text-violet-600" />
                    <div>
                      <p className="font-bold text-slate-800">CNH_VERIFICADA_CADASTRADA.pdf</p>
                      <p className="text-[9px] text-slate-400">Documento do Carro e Habilitação enviados</p>
                    </div>
                  </div>

                  <div className="flex gap-2 pt-1">
                    <button
                      onClick={() => onRejectDriver(drv.id)}
                      className="flex-1 py-2 bg-slate-50 hover:bg-red-50 text-slate-500 hover:text-red-700 border border-slate-200 hover:border-red-100 rounded-xl text-xs font-bold flex items-center justify-center gap-1 transition-colors"
                    >
                      <X className="w-3.5 h-3.5" />
                      <span>Rejeitar</span>
                    </button>
                    <button
                      onClick={() => onApproveDriver(drv.id)}
                      className="flex-[2] py-2 bg-violet-600 hover:bg-violet-700 text-white rounded-xl text-xs font-black flex items-center justify-center gap-1 transition-all shadow-md shadow-violet-600/10 cursor-pointer"
                    >
                      <Check className="w-3.5 h-3.5" />
                      <span>Aprovar Cadastro</span>
                    </button>
                  </div>
                </motion.div>
              ))
            )}
          </AnimatePresence>
        </div>
      )}

      {/* SUBSECTION: CONFIG RATE SETTINGS */}
      {activeSubSection === 'config' && (
        <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-xs">
          <form onSubmit={handleSaveSettings} className="space-y-4">
            <h4 className="text-xs font-black text-slate-800 uppercase tracking-wider border-b border-slate-100 pb-2">Configurações de Tarifas</h4>

            {successConfigMsg && (
              <div className="bg-emerald-50 text-emerald-700 text-xs px-3.5 py-2 rounded-xl border border-emerald-100 flex items-center gap-1.5">
                <Check className="w-3.5 h-3.5 text-emerald-600" />
                <span>{successConfigMsg}</span>
              </div>
            )}

            <div>
              <label className="block text-[10px] font-bold text-slate-500 mb-1 uppercase tracking-wider">Valor Mínimo por KM (R$)</label>
              <input
                type="number"
                step="0.10"
                min="2.00"
                value={pricePerKmInput}
                onChange={(e) => setPricePerKmInput(e.target.value)}
                className="w-full bg-slate-50 border border-slate-200 rounded-xl px-3.5 py-2 text-xs focus:outline-none focus:ring-1 focus:ring-violet-500 text-slate-900 font-medium"
                required
              />
              <p className="text-[9px] text-slate-400 mt-0.5">Mínimo de R$ 2,00 por Km exigido por regras da plataforma.</p>
            </div>

            <div>
              <label className="block text-[10px] font-bold text-slate-500 mb-1 uppercase tracking-wider">Tarifa Mínima do Aplicativo (R$)</label>
              <input
                type="number"
                step="0.50"
                value={minPriceInput}
                onChange={(e) => setMinPriceInput(e.target.value)}
                className="w-full bg-slate-50 border border-slate-200 rounded-xl px-3.5 py-2 text-xs focus:outline-none focus:ring-1 focus:ring-violet-500 text-slate-900 font-medium"
                required
              />
              <p className="text-[9px] text-slate-400 mt-0.5">Corrida mínima sugerida para garantir atratividade aos motoristas.</p>
            </div>

            <div>
              <label className="block text-[10px] font-bold text-slate-500 mb-1 uppercase tracking-wider">Taxa de Comissão (%)</label>
              <input
                type="number"
                min="0"
                max="30"
                value={commissionInput}
                onChange={(e) => setCommissionInput(e.target.value)}
                className="w-full bg-slate-50 border border-slate-200 rounded-xl px-3.5 py-2 text-xs focus:outline-none focus:ring-1 focus:ring-violet-500 text-slate-900 font-medium"
                required
              />
              <p className="text-[9px] text-slate-400 mt-0.5">Porcentagem retida de intermediação.</p>
            </div>

            <button
              type="submit"
              className="w-full py-3 bg-violet-600 hover:bg-violet-700 text-white font-black text-xs uppercase tracking-widest rounded-xl shadow-lg shadow-violet-600/10 transition-all cursor-pointer"
            >
              Aplicar Nova Tarifação
            </button>
          </form>
        </div>
      )}
    </div>
  );
};
