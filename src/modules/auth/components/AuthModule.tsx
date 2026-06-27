import React, { useState } from 'react';
import { User } from '@/src/shared/types';
import { MOCK_PASSENGERS, MOCK_DRIVERS } from '@/src/shared/constants';
import { Lock, Mail, Phone, User as UserIcon, Car, CreditCard, Shield, FileText, CheckCircle2, ChevronRight, LogIn } from 'lucide-react';
import { motion } from 'motion/react';

interface AuthProps {
  onLogin: (user: User) => void;
  drivers: User[];
  passengers: User[];
  onRegisterDriver: (driver: User) => void;
  onRegisterPassenger: (passenger: User) => void;
}

export const AuthModule: React.FC<AuthProps> = ({
  onLogin,
  drivers,
  passengers,
  onRegisterDriver,
  onRegisterPassenger,
}) => {
  const [activeTab, setActiveTab] = useState<'login' | 'register_passenger' | 'register_driver'>('login');
  const [roleMode, setRoleMode] = useState<'passenger' | 'driver' | 'admin'>('passenger');

  // Form states
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');

  // Vehicle states
  const [vehicleModel, setVehicleModel] = useState('');
  const [vehiclePlate, setVehiclePlate] = useState('');
  const [vehicleColor, setVehicleColor] = useState('');
  const [vehicleYear, setVehicleYear] = useState('');
  const [licenseUploaded, setLicenseUploaded] = useState(false);
  const [dragActive, setDragActive] = useState(false);

  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Fast-track mock login to simplify testing
  const handleQuickLogin = (user: User) => {
    onLogin(user);
  };

  const handleLoginSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage('');

    if (!email) {
      setErrorMessage('Por favor, insira seu e-mail.');
      return;
    }

    // Admin quick login
    if (email.toLowerCase() === 'admin@roxou.com' || (email.toLowerCase() === 'admin' && password === 'admin')) {
      const adminUser: User = {
        id: 'admin',
        email: 'admin@roxou.com',
        name: 'Administrador Roxou',
        phone: '(18) 3221-1010',
        role: 'admin',
        status: 'online',
        rating: 5.0,
        totalRides: 0,
        avatar: 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150',
      };
      onLogin(adminUser);
      return;
    }

    // Check roles
    if (roleMode === 'passenger') {
      const pass = passengers.find((p) => p.email.toLowerCase() === email.toLowerCase());
      if (pass) {
        onLogin(pass);
      } else {
        // Fallback or create temporary session for easy testing
        const fallbackPass: User = {
          id: 'temp_' + Date.now(),
          email,
          name: name || email.split('@')[0],
          phone: '(18) 99999-9999',
          role: 'passenger',
          status: 'online',
          rating: 5.0,
          totalRides: 1,
          avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150',
        };
        onLogin(fallbackPass);
      }
    } else {
      const drv = drivers.find((d) => d.email.toLowerCase() === email.toLowerCase());
      if (drv) {
        if (drv.status === 'pending_approval') {
          setErrorMessage('Seu cadastro como motorista está aguardando aprovação administrativa.');
        } else if (drv.status === 'rejected') {
          setErrorMessage('Seu cadastro de motorista foi rejeitado. Contate o suporte.');
        } else {
          onLogin(drv);
        }
      } else {
        setErrorMessage('E-mail do motorista não cadastrado. Cadastre-se na aba abaixo!');
      }
    }
  };

  const handlePassengerRegister = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name || !email || !phone) {
      setErrorMessage('Por favor, preencha todos os campos obrigatórios.');
      return;
    }

    const newUser: User = {
      id: 'p_' + Math.random().toString(36).substr(2, 9),
      email,
      name,
      phone,
      role: 'passenger',
      status: 'online',
      rating: 5.0,
      totalRides: 0,
      avatar: `https://api.dicebear.com/7.x/adventurer/svg?seed=${name}`,
    };

    onRegisterPassenger(newUser);
    setSuccessMessage('Passageiro cadastrado com sucesso! Faça o login.');
    setActiveTab('login');
    setRoleMode('passenger');
    setEmail(email);
  };

  const handleDriverRegister = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name || !email || !phone || !vehicleModel || !vehiclePlate || !vehicleColor || !vehicleYear) {
      setErrorMessage('Por favor, preencha todos os campos do motorista e veículo.');
      return;
    }

    if (!licenseUploaded) {
      setErrorMessage('Por favor, envie o documento da CNH para validação de segurança.');
      return;
    }

    const newDriver: User = {
      id: 'd_' + Math.random().toString(36).substr(2, 9),
      email,
      name,
      phone,
      role: 'driver',
      status: 'pending_approval', // needs admin approval
      rating: 5.0,
      totalRides: 0,
      avatar: `https://api.dicebear.com/7.x/bottts/svg?seed=${name}`,
      vehicleInfo: {
        model: vehicleModel,
        plate: vehiclePlate,
        color: vehicleColor,
        year: vehicleYear,
      },
    };

    onRegisterDriver(newDriver);
    setSuccessMessage('Cadastro enviado! Um administrador avaliará sua CNH e veículo em instantes.');
    setActiveTab('login');
    setRoleMode('driver');
    setEmail(email);
  };

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      setLicenseUploaded(true);
    }
  };

  return (
    <div className="w-full max-w-md mx-auto bg-white rounded-3xl shadow-xl overflow-hidden border border-slate-200 flex flex-col" id="auth-module">
      {/* Brand Header */}
      <div className="bg-gradient-to-br from-slate-900 via-violet-950 to-black text-white px-8 py-10 text-center relative overflow-hidden">
        <div className="absolute top-0 right-0 w-32 h-32 bg-violet-500/20 rounded-full blur-2xl pointer-events-none"></div>
        <div className="absolute -bottom-8 -left-8 w-24 h-24 bg-violet-500/10 rounded-full blur-xl pointer-events-none"></div>
        
        <div className="flex justify-center mb-2">
          <div className="w-14 h-14 bg-violet-600 rounded-2xl flex items-center justify-center shadow-lg shadow-violet-600/30">
            <Shield className="w-8 h-8 text-white" />
          </div>
        </div>
        <h1 className="text-3xl font-extrabold tracking-tight">Roxou<span className="text-violet-400 font-medium text-lg ml-1">Mobilidade</span></h1>
        <p className="text-violet-200 text-xs mt-1">Conectando Presidente Prudente com corridas justas e seguras</p>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-gray-100 bg-gray-50/50">
        <button
          onClick={() => { setActiveTab('login'); setErrorMessage(''); setSuccessMessage(''); }}
          className={`flex-1 py-4 text-center text-sm font-semibold transition-all border-b-2 ${
            activeTab === 'login' ? 'border-violet-600 text-violet-700 font-bold' : 'border-transparent text-gray-500 hover:text-gray-900'
          }`}
          id="tab-login"
        >
          Entrar
        </button>
        <button
          onClick={() => { setActiveTab('register_passenger'); setErrorMessage(''); setSuccessMessage(''); }}
          className={`flex-1 py-4 text-center text-sm font-semibold transition-all border-b-2 ${
            activeTab === 'register_passenger' ? 'border-violet-600 text-violet-700 font-bold' : 'border-transparent text-gray-500 hover:text-gray-900'
          }`}
          id="tab-register-passenger"
        >
          Passageiro
        </button>
        <button
          onClick={() => { setActiveTab('register_driver'); setErrorMessage(''); setSuccessMessage(''); }}
          className={`flex-1 py-4 text-center text-sm font-semibold transition-all border-b-2 ${
            activeTab === 'register_driver' ? 'border-violet-600 text-violet-700 font-bold' : 'border-transparent text-gray-500 hover:text-gray-900'
          }`}
          id="tab-register-driver"
        >
          Motorista
        </button>
      </div>

      <div className="p-6 flex-1 flex flex-col justify-between">
        {/* Messages */}
        {errorMessage && (
          <div className="mb-4 bg-red-50 text-red-700 text-xs px-4 py-3 rounded-xl border border-red-100 flex items-center gap-2">
            <span className="font-bold">Aviso:</span> {errorMessage}
          </div>
        )}
        {successMessage && (
          <div className="mb-4 bg-green-50 text-green-700 text-xs px-4 py-3 rounded-xl border border-green-100 flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-green-600 shrink-0" />
            <span>{successMessage}</span>
          </div>
        )}

        {/* Tab Content: LOGIN */}
        {activeTab === 'login' && (
          <form onSubmit={handleLoginSubmit} className="space-y-4">
            <div className="flex bg-gray-100 p-1 rounded-xl">
              <button
                type="button"
                onClick={() => setRoleMode('passenger')}
                className={`flex-1 py-2 text-center text-xs font-bold rounded-lg transition-all ${
                  roleMode === 'passenger' ? 'bg-white text-slate-800 shadow-sm' : 'text-gray-500 hover:text-gray-900'
                }`}
              >
                Passageiro
              </button>
              <button
                type="button"
                onClick={() => setRoleMode('driver')}
                className={`flex-1 py-2 text-center text-xs font-bold rounded-lg transition-all ${
                  roleMode === 'driver' ? 'bg-white text-slate-800 shadow-sm' : 'text-gray-500 hover:text-gray-900'
                }`}
              >
                Motorista
              </button>
              <button
                type="button"
                onClick={() => setRoleMode('admin')}
                className={`flex-1 py-2 text-center text-xs font-bold rounded-lg transition-all ${
                  roleMode === 'admin' ? 'bg-white text-slate-800 shadow-sm' : 'text-gray-500 hover:text-gray-900'
                }`}
              >
                Admin
              </button>
            </div>

            <div>
              <label className="block text-xs font-bold text-gray-700 mb-1.5 uppercase tracking-wider">E-mail de acesso</label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder={roleMode === 'admin' ? 'admin@roxou.com' : roleMode === 'passenger' ? 'ex: ana.silva@email.com' : 'ex: jose.motorista@email.com'}
                  className="w-full pl-10 pr-4 py-3 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-violet-500/20 focus:border-violet-600 transition-all text-gray-900"
                  required
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-gray-700 mb-1.5 uppercase tracking-wider">Senha</label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full pl-10 pr-4 py-3 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-violet-500/20 focus:border-violet-600 transition-all text-gray-900"
                />
              </div>
            </div>

            <button
              type="submit"
              className="w-full py-3.5 bg-violet-600 hover:bg-violet-700 active:bg-violet-800 text-white font-bold rounded-xl shadow-lg shadow-violet-600/20 transition-all flex items-center justify-center gap-2 text-sm mt-6 cursor-pointer"
              id="btn-login-submit"
            >
              <LogIn className="w-4 h-4" />
              <span>Entrar no Roxou</span>
            </button>

            {/* Quick-test helper triggers */}
            <div className="pt-6 border-t border-gray-100 mt-6">
              <p className="text-[10px] text-gray-400 font-bold uppercase tracking-wider mb-2.5 text-center">Acesso Rápido para Testes</p>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => handleQuickLogin(MOCK_PASSENGERS[0])}
                  className="p-2.5 bg-violet-50 hover:bg-violet-100 text-violet-950 rounded-xl text-left border border-violet-100/50 transition-all group"
                >
                  <div className="flex items-center gap-1.5">
                    <img src={MOCK_PASSENGERS[0].avatar} className="w-5 h-5 rounded-full object-cover" alt="" />
                    <span className="text-[11px] font-bold truncate">{MOCK_PASSENGERS[0].name}</span>
                  </div>
                  <span className="text-[9px] text-violet-500 font-medium block mt-1">Perfil Passageiro ➔</span>
                </button>

                <button
                  type="button"
                  onClick={() => handleQuickLogin(MOCK_DRIVERS[0])}
                  className="p-2.5 bg-violet-50 hover:bg-violet-100 text-violet-950 rounded-xl text-left border border-violet-100/50 transition-all group"
                >
                  <div className="flex items-center gap-1.5">
                    <img src={MOCK_DRIVERS[0].avatar} className="w-5 h-5 rounded-full object-cover" alt="" />
                    <span className="text-[11px] font-bold truncate">{MOCK_DRIVERS[0].name}</span>
                  </div>
                  <span className="text-[9px] text-violet-500 font-medium block mt-1">Perfil Motorista ➔</span>
                </button>
              </div>

              <div className="mt-2 text-center">
                <button
                  type="button"
                  onClick={() => handleQuickLogin({
                    id: 'admin',
                    email: 'admin@roxou.com',
                    name: 'Administrador Roxou',
                    phone: '(18) 3221-1010',
                    role: 'admin',
                    status: 'online',
                    rating: 5.0,
                    totalRides: 0,
                    avatar: 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150',
                  })}
                  className="text-[10px] text-gray-500 hover:text-violet-600 font-bold underline"
                >
                  Entrar como Administrador Geral
                </button>
              </div>
            </div>
          </form>
        )}

        {/* Tab Content: PASSENGER REGISTER */}
        {activeTab === 'register_passenger' && (
          <form onSubmit={handlePassengerRegister} className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-gray-700 mb-1.5 uppercase tracking-wider">Nome Completo</label>
              <div className="relative">
                <UserIcon className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="Seu nome completo"
                  className="w-full pl-10 pr-4 py-3 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-violet-500/20 focus:border-violet-600 transition-all text-gray-900"
                  required
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-gray-700 mb-1.5 uppercase tracking-wider">WhatsApp / Telefone</label>
              <div className="relative">
                <Phone className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="(18) 99XXX-XXXX"
                  className="w-full pl-10 pr-4 py-3 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-violet-500/20 focus:border-violet-600 transition-all text-gray-900"
                  required
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-gray-700 mb-1.5 uppercase tracking-wider">E-mail</label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="seu.email@exemplo.com"
                  className="w-full pl-10 pr-4 py-3 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-violet-500/20 focus:border-violet-600 transition-all text-gray-900"
                  required
                />
              </div>
            </div>

            <button
              type="submit"
              className="w-full py-3.5 bg-violet-600 hover:bg-violet-700 text-white font-bold rounded-xl shadow-lg shadow-violet-600/20 transition-all flex items-center justify-center gap-2 text-sm mt-6 cursor-pointer"
              id="btn-register-passenger-submit"
            >
              <span>Concluir Cadastro de Passageiro</span>
              <ChevronRight className="w-4 h-4" />
            </button>
          </form>
        )}

        {/* Tab Content: DRIVER REGISTER */}
        {activeTab === 'register_driver' && (
          <form onSubmit={handleDriverRegister} className="space-y-4 max-h-[420px] overflow-y-auto pr-1">
            <div className="bg-violet-50 p-3 rounded-xl border border-violet-100 flex gap-2 items-start">
              <Shield className="w-5 h-5 text-violet-600 shrink-0 mt-0.5" />
              <div>
                <p className="text-[11px] font-bold text-violet-950">Segurança Roxou Mobilidade</p>
                <p className="text-[9px] text-violet-700">Para atuar como motorista independente em Presidente Prudente, você precisa preencher os dados do carro e enviar a CNH para aprovação.</p>
              </div>
            </div>

            <p className="text-[11px] font-bold text-gray-800 uppercase tracking-wide border-b pb-1">1. Dados Pessoais</p>
            
            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="block text-[10px] font-bold text-gray-700 mb-1 uppercase">Nome Completo</label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="Nome completo"
                  className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-xs text-gray-900"
                  required
                />
              </div>
              <div>
                <label className="block text-[10px] font-bold text-gray-700 mb-1 uppercase">Celular / WhatsApp</label>
                <input
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="(18) 99XXX-XXXX"
                  className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-xs text-gray-900"
                  required
                />
              </div>
            </div>

            <div>
              <label className="block text-[10px] font-bold text-gray-700 mb-1 uppercase">E-mail</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="seu.email@exemplo.com"
                className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-xs text-gray-900"
                required
              />
            </div>

            <p className="text-[11px] font-bold text-gray-800 uppercase tracking-wide border-b pb-1 pt-2">2. Informações do Veículo</p>

            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="block text-[10px] font-bold text-gray-700 mb-1 uppercase">Modelo do Carro</label>
                <input
                  type="text"
                  value={vehicleModel}
                  onChange={(e) => setVehicleModel(e.target.value)}
                  placeholder="Ex: Fiat Uno / Corolla"
                  className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-xs text-gray-900"
                  required
                />
              </div>
              <div>
                <label className="block text-[10px] font-bold text-gray-700 mb-1 uppercase">Placa do Veículo</label>
                <input
                  type="text"
                  value={vehiclePlate}
                  onChange={(e) => setVehiclePlate(e.target.value)}
                  placeholder="Ex: ROX-3A12"
                  className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-xs text-gray-900"
                  required
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="block text-[10px] font-bold text-gray-700 mb-1 uppercase">Cor</label>
                <input
                  type="text"
                  value={vehicleColor}
                  onChange={(e) => setVehicleColor(e.target.value)}
                  placeholder="Ex: Prata / Preto"
                  className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-xs text-gray-900"
                  required
                />
              </div>
              <div>
                <label className="block text-[10px] font-bold text-gray-700 mb-1 uppercase">Ano</label>
                <input
                  type="text"
                  value={vehicleYear}
                  onChange={(e) => setVehicleYear(e.target.value)}
                  placeholder="Ex: 2019"
                  className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-xs text-gray-900"
                  required
                />
              </div>
            </div>

            <p className="text-[11px] font-bold text-gray-800 uppercase tracking-wide border-b pb-1 pt-2">3. Enviar CNH (Carteira de Habilitação)</p>

            {/* Document Upload Simulation */}
            <div
              onDragEnter={handleDrag}
              onDragOver={handleDrag}
              onDragLeave={handleDrag}
              onDrop={handleDrop}
              onClick={() => setLicenseUploaded(true)}
              className={`border-2 border-dashed rounded-xl p-4 text-center cursor-pointer transition-all ${
                licenseUploaded
                  ? 'bg-violet-50/50 border-violet-500'
                  : dragActive
                  ? 'bg-violet-100/50 border-violet-500 scale-[0.99]'
                  : 'bg-gray-50 border-gray-300 hover:border-violet-400'
              }`}
            >
              {licenseUploaded ? (
                <div className="flex flex-col items-center justify-center gap-1">
                  <CheckCircle2 className="w-8 h-8 text-violet-600" />
                  <p className="text-xs font-bold text-violet-950">Documento CNH_Frente_Verso.jpg carregado!</p>
                  <p className="text-[10px] text-gray-500">Clique novamente para substituir</p>
                </div>
              ) : (
                <div className="flex flex-col items-center justify-center gap-1.5">
                  <FileText className="w-8 h-8 text-gray-400" />
                  <p className="text-xs font-bold text-gray-700">Arraste ou clique para enviar foto da CNH</p>
                  <p className="text-[10px] text-gray-400">Formatos aceitos: JPG, PNG ou PDF (Max 5MB)</p>
                </div>
              )}
            </div>

            <button
              type="submit"
              className="w-full py-3.5 bg-violet-600 hover:bg-violet-700 text-white font-bold rounded-xl shadow-lg shadow-violet-600/20 transition-all flex items-center justify-center gap-2 text-sm mt-6 cursor-pointer"
              id="btn-register-driver-submit"
            >
              <span>Enviar Cadastro de Motorista</span>
              <ChevronRight className="w-4 h-4" />
            </button>
          </form>
        )}
      </div>
    </div>
  );
};
