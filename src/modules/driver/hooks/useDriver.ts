import { useState, useEffect, useCallback } from 'react';
import { driverService } from '../services/driverService';

export function useDriver(driverId?: string) {
  const [stats, setStats] = useState({
    totalRides: 0,
    earningsToday: 0.0,
    earningsWeek: 0.0,
    earningsMonth: 0.0,
    averageRating: 5.0,
    kmDriven: 0.0,
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadStats = useCallback(async () => {
    if (!driverId) return;
    setLoading(true);
    setError(null);
    try {
      const driverStats = await driverService.getDriverDashboardStats(driverId);
      setStats(driverStats);
    } catch (err: any) {
      console.error('Error loading driver stats:', err);
      setError(err.message || 'Erro ao carregar estatísticas do motorista.');
    } finally {
      setLoading(false);
    }
  }, [driverId]);

  useEffect(() => {
    loadStats();
  }, [loadStats]);

  const toggleOnline = async (isOnline: boolean) => {
    if (!driverId) return;
    try {
      await driverService.updateDriverOnlineStatus(driverId, isOnline);
    } catch (err: any) {
      console.error('Error toggling online status:', err);
      throw err;
    }
  };

  const updateLocation = async (lat: number, lng: number) => {
    if (!driverId) return;
    try {
      await driverService.updateDriverLocation(driverId, lat, lng);
    } catch (err: any) {
      console.error('Error updating driver location:', err);
    }
  };

  return {
    stats,
    loading,
    error,
    refreshStats: loadStats,
    toggleOnline,
    updateLocation,
  };
}
