import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'

export interface App {
  id: string
  name: string
  packageName: string
  version: string
  versionCode: number
  description?: string
  fileName: string
  filePath: string
  fileSize: number
  iconPath?: string
  isActive: boolean
  createdAt: Date
}

export const useAppsStore = defineStore('apps', () => {
  const apps = ref<App[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const uploadProgress = ref(0)

  const fetchApps = async () => {
    loading.value = true
    error.value = null
    
    try {
      const response = await api.get('/apps')
      apps.value = response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error al cargar aplicaciones'
    } finally {
      loading.value = false
    }
  }

  const uploadApp = async (
    file: File, 
    metadata: { name?: string; packageName?: string; version?: string },
    onProgress?: (progress: number) => void
  ) => {
    const formData = new FormData()
    formData.append('apk', file)
    
    // Solo agregar metadatos si se proporcionan (opcionales ahora)
    if (metadata.name) formData.append('name', metadata.name)
    if (metadata.packageName) formData.append('packageName', metadata.packageName)
    if (metadata.version) formData.append('version', metadata.version)

    loading.value = true
    error.value = null
    uploadProgress.value = 0

    try {
      const response = await api.post('/apps/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
        onUploadProgress: (progressEvent) => {
          if (progressEvent.total) {
            uploadProgress.value = Math.round((progressEvent.loaded * 100) / progressEvent.total)
            if (onProgress) {
              onProgress(uploadProgress.value)
            }
          }
        },
      })
      await fetchApps()
      return response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error al subir aplicación'
      throw err
    } finally {
      loading.value = false
      uploadProgress.value = 0
    }
  }

  const deleteApp = async (id: string) => {
    try {
      await api.delete(`/apps/${id}`)
      await fetchApps()
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error al eliminar aplicación'
      return false
    }
  }

  const installApp = async (appId: string, deviceId: string) => {
    try {
      await api.post(`/apps/${appId}/install/${deviceId}`)
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error al instalar aplicación'
      return false
    }
  }

  const installAppMultiple = async (appId: string, deviceIds: string[]) => {
    try {
      await api.post(`/apps/${appId}/install-multiple`, { deviceIds })
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error al instalar aplicación en dispositivos'
      return false
    }
  }

  const uninstallApp = async (appId: string, deviceId: string) => {
    try {
      await api.delete(`/apps/${appId}/uninstall/${deviceId}`)
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error al desinstalar aplicación'
      return false
    }
  }

  const updateMdmApp = async (file: File, deviceIds: string[], onProgress?: (progress: number) => void) => {
    const formData = new FormData()
    formData.append('apk', file)
    formData.append('deviceIds', JSON.stringify(deviceIds))

    loading.value = true
    error.value = null
    uploadProgress.value = 0

    try {
      const response = await api.post('/apps/mdm/update', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
        onUploadProgress: (progressEvent) => {
          if (progressEvent.total) {
            const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
            uploadProgress.value = progress
            if (onProgress) {
              onProgress(progress)
            }
          }
        },
      })
      return response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error al actualizar MDM'
      throw err
    } finally {
      loading.value = false
      uploadProgress.value = 0
    }
  }

  return {
    apps,
    loading,
    error,
    uploadProgress,
    fetchApps,
    uploadApp,
    deleteApp,
    installApp,
    installAppMultiple,
    uninstallApp,
    updateMdmApp
  }
})
