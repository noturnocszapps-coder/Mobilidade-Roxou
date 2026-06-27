import React, { createContext, useContext, useState, useEffect } from 'react';
import { User } from '@/src/shared/types';
import { MOCK_PASSENGERS, MOCK_DRIVERS } from '@/src/shared/constants';

interface AuthContextType {
  currentUser: User | null;
  passengers: User[];
  drivers: User[];
  setCurrentUser: React.Dispatch<React.SetStateAction<User | null>>;
  setPassengers: React.Dispatch<React.SetStateAction<User[]>>;
  setDrivers: React.Dispatch<React.SetStateAction<User[]>>;
  handleLogin: (user: User) => void;
  handleLogout: () => void;
  handleRegisterPassenger: (newPass: User) => void;
  handleRegisterDriver: (newDriver: User) => void;
  handleQuickSwitchRole: (role: 'passenger' | 'driver' | 'admin') => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [currentUser, setCurrentUser] = useState<User | null>(() => {
    const saved = localStorage.getItem('roxou_current_user');
    return saved ? JSON.parse(saved) : null;
  });

  const [passengers, setPassengers] = useState<User[]>(() => {
    const saved = localStorage.getItem('roxou_passengers');
    return saved ? JSON.parse(saved) : MOCK_PASSENGERS;
  });

  const [drivers, setDrivers] = useState<User[]>(() => {
    const saved = localStorage.getItem('roxou_drivers');
    return saved ? JSON.parse(saved) : MOCK_DRIVERS;
  });

  useEffect(() => {
    localStorage.setItem('roxou_current_user', currentUser ? JSON.stringify(currentUser) : '');
  }, [currentUser]);

  useEffect(() => {
    localStorage.setItem('roxou_passengers', JSON.stringify(passengers));
  }, [passengers]);

  useEffect(() => {
    localStorage.setItem('roxou_drivers', JSON.stringify(drivers));
  }, [drivers]);

  const handleLogin = (user: User) => {
    setCurrentUser(user);
  };

  const handleLogout = () => {
    setCurrentUser(null);
  };

  const handleRegisterPassenger = (newPass: User) => {
    setPassengers((prev) => [...prev, newPass]);
  };

  const handleRegisterDriver = (newDriver: User) => {
    setDrivers((prev) => [...prev, newDriver]);
  };

  const handleQuickSwitchRole = (role: 'passenger' | 'driver' | 'admin') => {
    if (role === 'passenger') {
      setCurrentUser(passengers[0]);
    } else if (role === 'driver') {
      setCurrentUser(drivers[0]);
    } else {
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
      setCurrentUser(adminUser);
    }
  };

  return (
    <AuthContext.Provider
      value={{
        currentUser,
        passengers,
        drivers,
        setCurrentUser,
        setPassengers,
        setDrivers,
        handleLogin,
        handleLogout,
        handleRegisterPassenger,
        handleRegisterDriver,
        handleQuickSwitchRole,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
