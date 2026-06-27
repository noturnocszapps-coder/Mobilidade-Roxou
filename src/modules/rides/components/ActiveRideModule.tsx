import React, { useState, useEffect, useRef } from 'react';
import { User, RideRequest, ChatMessage, Review } from '@/src/shared/types';
import { Shield, Phone, Send, MessageSquare, AlertTriangle, CheckCircle2, Star, Sparkles, MapPin, Share2, Clipboard, Lock, Navigation, Play, Trash } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

interface ActiveRideProps {
  user: User;
  ride: RideRequest;
  onCompleteRide: (rideId: string) => void;
  onCancelRide: (rideId: string) => void;
  onAddChatMessage: (rideId: string, message: ChatMessage) => void;
  onAddReview: (review: Review) => void;
  simulatedProgress: number;
  setSimulatedProgress: React.Dispatch<React.SetStateAction<number>>;
}

export const ActiveRideModule: React.FC<ActiveRideProps> = ({
  user,
  ride,
  onCompleteRide,
  onCancelRide,
  onAddChatMessage,
  onAddReview,
  simulatedProgress,
  setSimulatedProgress,
}) => {
  const [activeSubTab, setActiveSubTab] = useState<'info' | 'chat' | 'safety'>('info');
  const [chatInput, setChatInput] = useState('');
  const [sosTriggered, setSosTriggered] = useState(false);
  const [copiedLink, setCopiedLink] = useState(false);

  // Review states
  const [rating, setRating] = useState<number>(5);
  const [reviewComment, setReviewComment] = useState('');
  const [reviewSubmitted, setReviewSubmitted] = useState(false);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const isPassenger = user.role === 'passenger';

  // Scroll chat to bottom
  useEffect(() => {
    if (activeSubTab === 'chat' && messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [ride.chatMessages, activeSubTab]);

  // Handle auto driver reply in chat simulation
  useEffect(() => {
    if (!ride.chatMessages) return;
    const lastMsg = ride.chatMessages[ride.chatMessages.length - 1];
    
    // If the last message was from the passenger, trigger a simulated reply after 2s
    if (lastMsg && lastMsg.senderRole === 'passenger' && isPassenger) {
      const timer = setTimeout(() => {
        const autoReplies = [
          'Ok! Estou chegando perto do ponto combinado.',
          'Entendido. Estou a caminho, chego em cerca de 3 minutos.',
          'Estou passando pelo Parque do Povo agora, já chego!',
          'Certo, estou no veículo cadastrado. Até já!',
          'Opa, fechado! Já estou encostando.'
        ];
        const randomReply = autoReplies[Math.floor(Math.random() * autoReplies.length)];
        
        onAddChatMessage(ride.id, {
          id: 'reply_' + Date.now(),
          senderRole: 'driver',
          text: randomReply,
          timestamp: new Date().toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
        });
      }, 1800);
      return () => clearTimeout(timer);
    }

    // If last message was from driver and user is driver, simulate passenger reply
    if (lastMsg && lastMsg.senderRole === 'driver' && !isPassenger) {
      const timer = setTimeout(() => {
        const passengerReplies = [
          'Tudo bem, estou aguardando na calçada!',
          'Certo! Estou vestindo camiseta preta.',
          'Ok, sem problemas. Te espero.',
          'Perfeito, muito obrigado!'
        ];
        const randomReply = passengerReplies[Math.floor(Math.random() * passengerReplies.length)];

        onAddChatMessage(ride.id, {
          id: 'reply_' + Date.now(),
          senderRole: 'passenger',
          text: randomReply,
          timestamp: new Date().toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
        });
      }, 1800);
      return () => clearTimeout(timer);
    }
  }, [ride.chatMessages, isPassenger]);

  const handleSendMessage = (e: React.FormEvent) => {
    e.preventDefault();
    if (!chatInput.trim()) return;

    onAddChatMessage(ride.id, {
      id: 'msg_' + Date.now(),
      senderRole: isPassenger ? 'passenger' : 'driver',
      text: chatInput,
      timestamp: new Date().toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
    });

    setChatInput('');
  };

  const handleTriggerSOS = () => {
    setSosTriggered(true);
    // Auto-reset SOS after 10s for simulation
    setTimeout(() => {
      setSosTriggered(false);
    }, 10000);
  };

  const handleShareRoute = () => {
    setCopiedLink(true);
    navigator.clipboard.writeText(`https://roxou.com/track/${ride.id}`);
    setTimeout(() => setCopiedLink(false), 2000);
  };

  const handleSubmitReview = (e: React.FormEvent) => {
    e.preventDefault();
    
    onAddReview({
      id: 'rev_' + Date.now(),
      rideId: ride.id,
      reviewerId: user.id,
      rating,
      comment: reviewComment,
      reviewerRole: isPassenger ? 'passenger' : 'driver',
      createdAt: new Date().toISOString()
    });

    setReviewSubmitted(true);
  };

  return (
    <div className="w-full max-w-md mx-auto space-y-4" id="active-ride-module">
      {/* Simulation Banner */}
      <div className="bg-purple-100 border border-purple-200 px-4 py-2.5 rounded-2xl flex items-center justify-between text-xs text-purple-900">
        <div className="flex items-center gap-1.5 font-bold">
          <Sparkles className="w-4 h-4 text-purple-600 animate-spin-slow" />
          <span>Simulação de Progresso:</span>
        </div>
        <div className="flex items-center gap-2">
          <span className="font-mono font-black">{simulatedProgress}%</span>
          {simulatedProgress < 100 ? (
            <button
              onClick={() => {
                setSimulatedProgress((p) => Math.min(100, p + 20));
              }}
              className="px-2 py-1 bg-purple-600 text-white font-bold rounded hover:bg-purple-700 text-[10px] cursor-pointer"
            >
              Avançar Viagem ➔
            </button>
          ) : (
            <span className="text-green-600 font-extrabold flex items-center gap-1">
              <CheckCircle2 className="w-3.5 h-3.5" /> Chegou!
            </span>
          )}
        </div>
      </div>

      {/* Accepted Ride Card Core */}
      <div className="bg-white rounded-3xl border border-gray-100 shadow-xl overflow-hidden flex flex-col">
        
        {/* Header / Ride Status */}
        <div className="bg-gradient-to-br from-purple-900 to-indigo-950 text-white px-5 py-4">
          <div className="flex justify-between items-center">
            <div>
              <span className="text-[9px] bg-purple-500/30 text-purple-200 font-bold px-2 py-0.5 rounded-full uppercase tracking-wider">
                {simulatedProgress === 100 ? 'Viagem Concluída' : simulatedProgress === 0 ? 'Aguardando Embarque' : 'Em Andamento'}
              </span>
              <h3 className="text-sm font-black mt-1">Corrida no Roxou Mobilidade</h3>
            </div>
            <p className="text-xl font-black text-purple-300">R$ {ride.finalPrice?.toFixed(2) || ride.suggestedPrice.toFixed(2)}</p>
          </div>

          {/* Quick Mini Map route info */}
          <div className="mt-3 bg-white/5 backdrop-blur-sm p-2 rounded-xl text-[11px] text-purple-100 flex items-center justify-between gap-1">
            <span className="font-bold truncate max-w-[130px]">{ride.origin.name}</span>
            <Navigation className="w-3.5 h-3.5 rotate-90 text-purple-400 shrink-0" />
            <span className="font-bold truncate max-w-[130px]">{ride.destination.name}</span>
          </div>
        </div>

        {/* Sub Navigation Inside Active Card */}
        <div className="flex border-b text-xs font-bold bg-gray-50/50">
          <button
            onClick={() => setActiveSubTab('info')}
            className={`flex-1 py-3 text-center border-b-2 transition-all ${
              activeSubTab === 'info' ? 'border-purple-600 text-purple-950' : 'border-transparent text-gray-500'
            }`}
          >
            Status
          </button>
          <button
            onClick={() => setActiveSubTab('chat')}
            className={`flex-1 py-3 text-center border-b-2 transition-all flex items-center justify-center gap-1 ${
              activeSubTab === 'chat' ? 'border-purple-600 text-purple-950' : 'border-transparent text-gray-500'
            }`}
          >
            <span>Mensagens</span>
            {ride.chatMessages && ride.chatMessages.length > 0 && (
              <span className="w-4 h-4 bg-purple-600 text-[9px] text-white font-black rounded-full flex items-center justify-center">
                {ride.chatMessages.length}
              </span>
            )}
          </button>
          <button
            onClick={() => setActiveSubTab('safety')}
            className={`flex-1 py-3 text-center border-b-2 transition-all flex items-center justify-center gap-1 ${
              activeSubTab === 'safety' ? 'border-purple-600 text-purple-950' : 'border-transparent text-gray-500'
            }`}
          >
            <Shield className="w-3.5 h-3.5 text-red-500" />
            <span>Segurança</span>
          </button>
        </div>

        {/* Tab Content Area */}
        <div className="p-5 flex-1 min-h-[300px] flex flex-col justify-between">
          
          {/* TAB: STATUS / INFO */}
          {activeSubTab === 'info' && (
            <div className="space-y-4">
              {simulatedProgress < 100 ? (
                <>
                  {/* Participant Partner Info */}
                  <div className="flex items-center gap-3.5 p-3 bg-purple-50/50 rounded-2xl border border-purple-100/50">
                    <img
                      src={isPassenger ? (ride.driverAvatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150') : ride.passengerAvatar}
                      className="w-12 h-12 rounded-full object-cover border border-purple-200 bg-white"
                      alt=""
                    />
                    <div className="flex-1 min-w-0">
                      <p className="text-[10px] text-purple-500 uppercase font-black">{isPassenger ? 'Seu Motorista Roxou' : 'Seu Passageiro'}</p>
                      <h4 className="text-sm font-black text-gray-900 truncate">
                        {isPassenger ? (ride.driverName || 'Motorista Independente') : ride.passengerName}
                      </h4>
                      <p className="text-[11px] text-gray-500 flex items-center gap-1">
                        <span>★ {isPassenger ? (ride.driverRating?.toFixed(2) || '4.90') : ride.passengerRating.toFixed(2)}</span>
                        <span>•</span>
                        <span className="truncate">{isPassenger ? (ride.vehicleInfo || 'Onix Prata') : `${ride.passengerCount} pessoa(s)`}</span>
                      </p>
                    </div>

                    <a
                      href={`tel:${isPassenger ? (ride.driverPhone || '(18) 99123-4567') : '(18) 99765-4321'}`}
                      className="p-2.5 bg-white border hover:bg-gray-50 rounded-full text-purple-600 transition-colors shadow-sm"
                      title="Ligar"
                    >
                      <Phone className="w-4 h-4" />
                    </a>
                  </div>

                  {/* Physical Trip progress tracker */}
                  <div className="space-y-1.5">
                    <div className="flex justify-between text-xs font-bold text-gray-700">
                      <span>Progresso do Trajeto:</span>
                      <span className="text-purple-600">{simulatedProgress}% concluído</span>
                    </div>
                    <div className="w-full bg-gray-100 rounded-full h-3 overflow-hidden p-0.5 border">
                      <motion.div
                        className="bg-purple-600 h-full rounded-full"
                        animate={{ width: `${simulatedProgress}%` }}
                        transition={{ duration: 0.5 }}
                      ></motion.div>
                    </div>
                  </div>

                  {/* Verification Boarding PIN */}
                  <div className="bg-yellow-50 border border-yellow-200/60 p-4 rounded-2xl flex items-center justify-between">
                    <div>
                      <p className="text-[10px] font-black text-amber-900 uppercase">PIN de Segurança de Embarque</p>
                      <p className="text-[11px] text-amber-700 mt-0.5">Mostre ao motorista antes de iniciar a viagem.</p>
                    </div>
                    <div className="flex items-center gap-1.5 bg-white px-3 py-1.5 rounded-xl border border-yellow-200 shadow-sm font-mono text-base font-black text-amber-900">
                      <Lock className="w-4 h-4 text-amber-600" />
                      <span>{ride.safetyCode}</span>
                    </div>
                  </div>

                  {/* Actions buttons */}
                  {simulatedProgress === 0 && (
                    <button
                      onClick={() => onCancelRide(ride.id)}
                      className="w-full py-2.5 bg-red-50 hover:bg-red-100 border border-red-200 text-red-700 rounded-xl text-xs font-black transition-all"
                    >
                      Cancelar Viagem
                    </button>
                  )}

                  {!isPassenger && simulatedProgress >= 100 && (
                    <button
                      onClick={() => onCompleteRide(ride.id)}
                      className="w-full py-3 bg-purple-600 hover:bg-purple-700 text-white font-black rounded-xl text-xs shadow-md transition-all cursor-pointer"
                    >
                      Finalizar Corrida e Receber
                    </button>
                  )}
                </>
              ) : (
                /* COMPLETED & REVIEWS SYSTEM */
                <div className="space-y-4">
                  <div className="text-center space-y-2 py-4">
                    <div className="w-14 h-14 bg-green-50 rounded-full flex items-center justify-center mx-auto text-green-600 border border-green-200 shadow-md">
                      <CheckCircle2 className="w-8 h-8" />
                    </div>
                    <h4 className="text-base font-extrabold text-gray-900">Você chegou ao seu destino!</h4>
                    <p className="text-xs text-gray-500">Obrigado por usar o Roxou Mobilidade Presidente Prudente. Como foi sua experiência de viagem?</p>
                  </div>

                  <AnimatePresence mode="wait">
                    {!reviewSubmitted ? (
                      /* REVIEW FORM */
                      <motion.form
                        onSubmit={handleSubmitReview}
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        className="bg-gray-50 p-4 rounded-2xl border border-gray-100 space-y-3"
                      >
                        <p className="text-xs font-bold text-gray-700 text-center uppercase">Dar nota para o {isPassenger ? 'motorista' : 'passageiro'}</p>
                        
                        {/* Interactive stars */}
                        <div className="flex justify-center gap-2 py-1">
                          {[1, 2, 3, 4, 5].map((star) => (
                            <button
                              key={star}
                              type="button"
                              onClick={() => setRating(star)}
                              className="focus:outline-none transition-transform active:scale-125"
                            >
                              <Star
                                className={`w-8 h-8 ${
                                  star <= rating ? 'text-amber-400 fill-amber-400' : 'text-gray-300'
                                }`}
                              />
                            </button>
                          ))}
                        </div>

                        <div>
                          <label className="block text-[10px] font-bold text-gray-600 mb-1 uppercase">Escreva um comentário (opcional)</label>
                          <textarea
                            value={reviewComment}
                            onChange={(e) => setReviewComment(e.target.value)}
                            placeholder="Deixe um elogio ou observação sobre a corrida..."
                            rows={2}
                            className="w-full bg-white border rounded-xl p-2 text-xs text-gray-900 focus:outline-none focus:ring-1 focus:ring-purple-500"
                          />
                        </div>

                        <button
                          type="submit"
                          className="w-full py-2.5 bg-purple-600 hover:bg-purple-700 text-white text-xs font-black rounded-xl shadow-md transition-all cursor-pointer"
                        >
                          Enviar Avaliação
                        </button>
                      </motion.form>
                    ) : (
                      /* REVIEW COMPLETED SUCCESS */
                      <motion.div
                        initial={{ opacity: 0, scale: 0.95 }}
                        animate={{ opacity: 1, scale: 1 }}
                        className="bg-green-50 border border-green-200 p-4 rounded-2xl text-center space-y-1"
                      >
                        <Sparkles className="w-5 h-5 text-green-600 mx-auto animate-bounce-slow" />
                        <p className="text-xs font-bold text-green-950">Avaliação registrada!</p>
                        <p className="text-[10px] text-green-700">Seu feedback ajuda a manter a comunidade de Presidente Prudente segura e justa.</p>
                        <button
                          type="button"
                          onClick={() => onCompleteRide(ride.id)}
                          className="mt-2.5 px-4 py-1.5 bg-white border border-green-200 text-green-800 text-[10px] font-bold rounded-lg shadow-sm hover:bg-green-100 transition-colors cursor-pointer"
                        >
                          Voltar ao Início
                        </button>
                      </motion.div>
                    )}
                  </AnimatePresence>
                </div>
              )}
            </div>
          )}

          {/* TAB: CHAT MESSAGES */}
          {activeSubTab === 'chat' && (
            <div className="flex flex-col h-[320px] justify-between">
              {/* Messages feed */}
              <div className="flex-1 overflow-y-auto space-y-3 mb-3 pr-1 max-h-[260px]">
                {ride.chatMessages && ride.chatMessages.length > 0 ? (
                  ride.chatMessages.map((msg) => {
                    const amISender = (isPassenger && msg.senderRole === 'passenger') || (!isPassenger && msg.senderRole === 'driver');
                    return (
                      <div
                        key={msg.id}
                        className={`flex flex-col ${amISender ? 'items-end' : 'items-start'}`}
                      >
                        <div
                          className={`px-3 py-2 rounded-2xl max-w-[80%] text-xs shadow-sm ${
                            amISender
                              ? 'bg-purple-600 text-white rounded-tr-none'
                              : 'bg-gray-100 text-gray-900 rounded-tl-none'
                          }`}
                        >
                          {msg.text}
                        </div>
                        <span className="text-[8px] text-gray-400 mt-0.5 px-1">{msg.timestamp}</span>
                      </div>
                    );
                  })
                ) : (
                  <div className="text-center text-gray-400 py-10 space-y-2">
                    <MessageSquare className="w-8 h-8 text-gray-300 mx-auto" />
                    <p className="text-[11px] font-bold">Inicie o chat com o {isPassenger ? 'motorista' : 'passageiro'}</p>
                    <p className="text-[9px] text-gray-400">Combine detalhes do ponto de encontro no Presidente Prudente.</p>
                  </div>
                )}
                <div ref={messagesEndRef} />
              </div>

              {/* Chat Input form */}
              <form onSubmit={handleSendMessage} className="flex gap-1.5 border-t pt-2.5">
                <input
                  type="text"
                  value={chatInput}
                  onChange={(e) => setChatInput(e.target.value)}
                  placeholder="Envie uma mensagem..."
                  className="flex-1 bg-gray-50 border rounded-xl px-3 py-2 text-xs focus:outline-none"
                />
                <button
                  type="submit"
                  disabled={!chatInput.trim()}
                  className={`p-2.5 rounded-xl text-white transition-all ${
                    chatInput.trim() ? 'bg-purple-600 hover:bg-purple-700' : 'bg-gray-200 cursor-not-allowed'
                  }`}
                >
                  <Send className="w-4 h-4" />
                </button>
              </form>
            </div>
          )}

          {/* TAB: SAFETY TRIGGERS */}
          {activeSubTab === 'safety' && (
            <div className="space-y-4 text-center">
              <div className="bg-red-50 p-4 rounded-2xl border border-red-100 space-y-2">
                <AlertTriangle className="w-10 h-10 text-red-600 mx-auto animate-pulse" />
                <h4 className="text-xs font-black text-red-950 uppercase">Central de Segurança Roxou</h4>
                <p className="text-[10px] text-red-700">Esta corrida está sendo monitorada em Presidente Prudente/SP. Se houver qualquer emergência ou risco à integridade física, use o botão de pânico abaixo.</p>
                
                {sosTriggered ? (
                  <div className="bg-red-600 text-white p-2.5 rounded-xl font-mono text-xs font-bold animate-pulse">
                    🚨 SOS ATIVADO! Acionando Polícia (190) e compartilhando localização em tempo real...
                  </div>
                ) : (
                  <button
                    onClick={handleTriggerSOS}
                    className="w-full py-2.5 bg-red-600 hover:bg-red-700 text-white text-xs font-black rounded-xl shadow-lg shadow-red-600/30 transition-all cursor-pointer"
                  >
                    Ativar Botão de Pânico (Chamar 190)
                  </button>
                )}
              </div>

              {/* Share ride details */}
              <div className="bg-gray-50 p-4 rounded-2xl border border-gray-100 text-left space-y-2">
                <p className="text-[10px] font-black text-gray-700 uppercase">Compartilhamento de Rota</p>
                <p className="text-[10px] text-gray-500">Envie um link de rastreamento em tempo real para seus amigos ou familiares acompanharem sua viagem pelo WhatsApp.</p>
                
                <button
                  onClick={handleShareRoute}
                  className="w-full py-2 bg-white border hover:bg-purple-50 text-purple-700 text-xs font-bold rounded-xl transition-all flex items-center justify-center gap-1.5 shadow-sm"
                >
                  {copiedLink ? (
                    <>
                      <CheckCircle2 className="w-4 h-4 text-green-600" />
                      <span>Link Copiado!</span>
                    </>
                  ) : (
                    <>
                      <Share2 className="w-4 h-4" />
                      <span>Compartilhar Rota de Viagem</span>
                    </>
                  )}
                </button>
              </div>
            </div>
          )}

        </div>
      </div>
    </div>
  );
};
