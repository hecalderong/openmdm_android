import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface SystemSettings {
  general: {
    systemName: string
    language: string
    timezone: string
  }
  notifications: {
    deviceOffline: boolean
    lowBattery: boolean
    appInstallation: boolean
    emailNotifications: boolean
  }
  security: {
    sessionTimeout: number
    requireStrongPassword: boolean
    twoFactorAuth: boolean
    passwordMinLength: number
  }
  devices: {
    autoLockInactive: boolean
    inactiveThreshold: number
    heartbeatInterval: number
    autoUpdateApps: boolean
  }
}

export const useSettingsStore = defineStore('settings', () => {
  const settings = ref<SystemSettings>({
    general: {
      systemName: 'OpenMDM-Android',
      language: 'es',
      timezone: 'America/Mexico_City'
    },
    notifications: {
      deviceOffline: true,
      lowBattery: true,
      appInstallation: true,
      emailNotifications: false
    },
    security: {
      sessionTimeout: 30,
      requireStrongPassword: true,
      twoFactorAuth: false,
      passwordMinLength: 8
    },
    devices: {
      autoLockInactive: false,
      inactiveThreshold: 24,
      heartbeatInterval: 60,
      autoUpdateApps: false
    }
  })

  const loading = ref(false)
  const error = ref<string | null>(null)

  // Load settings from localStorage
  const loadSettings = () => {
    try {
      const saved = localStorage.getItem('mdm_settings')
      if (saved) {
        settings.value = { ...settings.value, ...JSON.parse(saved) }
      }
    } catch (err) {
      console.error('Error loading settings:', err)
    }
  }

  // Save settings to localStorage
  const saveSettings = async (): Promise<boolean> => {
    loading.value = true
    error.value = null
    try {
      localStorage.setItem('mdm_settings', JSON.stringify(settings.value))
      // Aquí se podría agregar una llamada al backend para persistir la configuración
      // await api.post('/settings', settings.value)
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error al guardar configuración'
      console.error('Error saving settings:', err)
      return false
    } finally {
      loading.value = false
    }
  }

  const updateGeneralSettings = (data: Partial<SystemSettings['general']>) => {
    settings.value.general = { ...settings.value.general, ...data }
  }

  const updateNotificationSettings = (data: Partial<SystemSettings['notifications']>) => {
    settings.value.notifications = { ...settings.value.notifications, ...data }
  }

  const updateSecuritySettings = (data: Partial<SystemSettings['security']>) => {
    settings.value.security = { ...settings.value.security, ...data }
  }

  const updateDeviceSettings = (data: Partial<SystemSettings['devices']>) => {
    settings.value.devices = { ...settings.value.devices, ...data }
  }

  const resetToDefaults = () => {
    settings.value = {
      general: {
        systemName: 'OpenMDM-Android',
        language: 'es',
        timezone: 'America/Mexico_City'
      },
      notifications: {
        deviceOffline: true,
        lowBattery: true,
        appInstallation: true,
        emailNotifications: false
      },
      security: {
        sessionTimeout: 30,
        requireStrongPassword: true,
        twoFactorAuth: false,
        passwordMinLength: 8
      },
      devices: {
        autoLockInactive: false,
        inactiveThreshold: 24,
        heartbeatInterval: 60,
        autoUpdateApps: false
      }
    }
  }

  // Load settings on store initialization
  loadSettings()

  return {
    settings,
    loading,
    error,
    loadSettings,
    saveSettings,
    updateGeneralSettings,
    updateNotificationSettings,
    updateSecuritySettings,
    updateDeviceSettings,
    resetToDefaults
  }
})
