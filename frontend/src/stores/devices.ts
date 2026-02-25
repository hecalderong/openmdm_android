import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import websocketService from '@/services/websocket'

export interface Device {
  id: string
  deviceId: string
  name: string
  manufacturer?: string
  model?: string
  androidVersion?: string
  status: 'online' | 'offline' | 'inactive'
  batteryLevel?: number
  storageUsed?: number
  storageTotal?: number
  ramUsed?: number
  ramTotal?: number
  ipAddress?: string
  lastSeen?: Date
  isLocked: boolean
  kioskPackageName?: string
  kioskModeEnabled?: boolean
  powerSchedule?: {
    enabled: boolean
    powerOnTime: string
    powerOffTime: string
    daysOfWeek: number[]
  }
  location?: {
    latitude: number
    longitude: number
    timestamp: Date
  }
}

export interface DeviceStats {
  total: number
  online: number
  offline: number
  locked: number
}

export const useDevicesStore = defineStore('devices', () => {
  const devices = ref<Device[]>([])
  const stats = ref<DeviceStats>({ total: 0, online: 0, offline: 0, locked: 0 })
  const loading = ref(false)
  const error = ref<string | null>(null)

  const fetchDevices = async () => {
    loading.value = true
    error.value = null
    
    try {
      const response = await api.get('/devices')
      devices.value = response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error al cargar dispositivos'
    } finally {
      loading.value = false
    }
  }

  const fetchStats = async () => {
    try {
      const response = await api.get('/devices/stats')
      stats.value = response.data
    } catch (err) {
      console.error('Error fetching stats:', err)
    }
  }

  const getDevice = async (id: string) => {
    try {
      const response = await api.get(`/devices/${id}`)
      return response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error al cargar dispositivo'
      return null
    }
  }

  const lockDevice = async (id: string) => {
    try {
      await api.put(`/devices/${id}/lock`)
      await fetchDevices()
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error al bloquear dispositivo'
      return false
    }
  }

  const unlockDevice = async (id: string) => {
    try {
      await api.put(`/devices/${id}/unlock`)
      await fetchDevices()
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error al desbloquear dispositivo'
      return false
    }
  }

  const launchApp = async (id: string, packageName: string) => {
    try {
      await api.post(`/devices/${id}/launch-app`, { packageName })
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error al lanzar aplicación'
      throw err
    }
  }

  const deleteDevice = async (id: string) => {
    try {
      await api.delete(`/devices/${id}`)
      await fetchDevices()
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error al eliminar dispositivo'
      return false
    }
  }

  // WebSocket listeners
  const setupWebSocket = () => {
    websocketService.on('device:online', (device: Device) => {
      const index = devices.value.findIndex(d => d.id === device.id)
      if (index >= 0) {
        devices.value[index] = device
      } else {
        devices.value.push(device)
      }
      fetchStats()
    })

    websocketService.on('device:offline', ({ deviceId }: { deviceId: string }) => {
      const device = devices.value.find(d => d.id === deviceId)
      if (device) {
        device.status = 'offline'
      }
      fetchStats()
    })

    websocketService.on('device:update', (device: Device) => {
      const index = devices.value.findIndex(d => d.id === device.id)
      if (index >= 0) {
        devices.value[index] = device
      }
    })

    websocketService.on('stats', (newStats: DeviceStats) => {
      stats.value = newStats
    })
  }

  const enableKioskMode = async (id: string, packageName: string) => {
    try {
      await api.post(`/devices/${id}/kiosk-mode`, { packageName })
      await fetchDevices()
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error al activar modo kiosk'
      return false
    }
  }

  const disableKioskMode = async (id: string) => {
    try {
      await api.delete(`/devices/${id}/kiosk-mode`)
      await fetchDevices()
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error al desactivar modo kiosk'
      return false
    }
  }

  const setPowerSchedule = async (id: string, schedule: any) => {
    try {
      await api.post(`/devices/${id}/power-schedule`, schedule)
      await fetchDevices()
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error al configurar horarios'
      throw err
    }
  }

  const clearPowerSchedule = async (id: string) => {
    try {
      await api.delete(`/devices/${id}/power-schedule`)
      await fetchDevices()
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error al eliminar horarios'
      throw err
    }
  }

  return {
    devices,
    stats,
    loading,
    error,
    fetchDevices,
    fetchStats,
    getDevice,
    lockDevice,
    unlockDevice,
    launchApp,
    deleteDevice,
    enableKioskMode,
    disableKioskMode,
    setPowerSchedule,
    clearPowerSchedule,
    setupWebSocket
  }
})
