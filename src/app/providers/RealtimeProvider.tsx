import React, { createContext, useContext, useState } from 'react';

interface RealtimeContextType {
  notification: string | null;
  showNotification: (message: string) => void;
  setNotification: React.Dispatch<React.SetStateAction<string | null>>;
}

const RealtimeContext = createContext<RealtimeContextType | undefined>(undefined);

export const RealtimeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [notification, setNotification] = useState<string | null>(null);

  const showNotification = (message: string) => {
    setNotification(message);
    setTimeout(() => {
      setNotification((prev) => (prev === message ? null : prev));
    }, 4000);
  };

  return (
    <RealtimeContext.Provider value={{ notification, showNotification, setNotification }}>
      {children}
    </RealtimeContext.Provider>
  );
};

export const useRealtime = () => {
  const context = useContext(RealtimeContext);
  if (!context) {
    throw new Error('useRealtime must be used within a RealtimeProvider');
  }
  return context;
};
