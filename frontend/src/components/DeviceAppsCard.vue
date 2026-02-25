<template>
  <div class="device-apps-card">
    <div class="card-header">
      <h3>📱 Aplicaciones Instaladas</h3>
      <button @click="refreshApps" class="btn-refresh" :disabled="loading">
        <span v-if="loading">🔄 Cargando...</span>
        <span v-else>🔄 Actualizar</span>
      </button>
    </div>

    <div v-if="loading && apps.length === 0" class="loading-state">
      <div class="spinner"></div>
      <p>Cargando aplicaciones...</p>
    </div>

    <div v-else-if="error" class="error-state">
      <p>❌ {{ error }}</p>
      <button @click="refreshApps" class="btn-retry">Reintentar</button>
    </div>

    <div v-else-if="apps.length === 0" class="empty-state">
      <p>📦 No hay aplicaciones instaladas en este dispositivo</p>
    </div>

    <div v-else class="apps-table-container">
      <table class="apps-table">
        <thead>
          <tr>
            <th>Aplicación</th>
            <th>Paquete</th>
            <th>Versión</th>
            <th>Estado</th>
            <th>Instalado</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="app in apps" :key="app.id">
            <td class="app-name">
              <div class="app-info">
                <span class="app-icon">📱</span>
                <span>{{ app.app.name }}</span>
              </div>
            </td>
            <td class="package-name">{{ app.app.packageName }}</td>
            <td>{{ app.installedVersion || app.app.version }}</td>
            <td>
              <span :class="['status-badge', `status-${app.status}`]">
                {{ getStatusLabel(app.status) }}
              </span>
            </td>
            <td class="date-cell">{{ formatDate(app.installedAt) }}</td>
            <td class="actions-cell">
              <button
                v-if="app.status === 'installed'"
                @click="handleUninstall(app)"
                class="btn-uninstall"
                :disabled="uninstalling === app.id"
              >
                <span v-if="uninstalling === app.id">🔄</span>
                <span v-else>🗑️</span>
                {{ uninstalling === app.id ? 'Desinstalando...' : 'Desinstalar' }}
              </button>
              <span v-else-if="app.status === 'pending'" class="status-text">
                ⏳ Pendiente
              </span>
              <span v-else-if="app.status === 'installing'" class="status-text">
                ⬇️ Instalando...
              </span>
              <span v-else-if="app.status === 'failed'" class="status-text">
                ❌ Error
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="apps.length > 0" class="card-footer">
      <p>Total: {{ apps.length }} aplicación{{ apps.length !== 1 ? 'es' : '' }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import api from '@/services/api'
import websocket from '@/services/websocket'

interface DeviceApp {
  id: string
  deviceId: string
  appId: string
  installedVersion: string
  status: 'pending' | 'installing' | 'installed' | 'failed'
  errorMessage?: string
  installedAt: string
  updatedAt: string
  app: {
    id: string
    name: string
    packageName: string
    version: string
    versionCode: number
    description?: string
    fileName: string
    fileSize: number
    iconPath?: string
  }
}

const props = defineProps<{
  deviceId: string
}>()

const apps = ref<DeviceApp[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const uninstalling = ref<string | null>(null)

const fetchApps = async () => {
  loading.value = true
  error.value = null

  try {
    const response = await api.get(`/apps/device/${props.deviceId}`)
    apps.value = response.data
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Error al cargar aplicaciones'
    console.error('Error fetching device apps:', err)
  } finally {
    loading.value = false
  }
}

const refreshApps = () => {
  fetchApps()
}

const handleUninstall = async (app: DeviceApp) => {
  if (!confirm(`¿Estás seguro de que deseas desinstalar "${app.app.name}"?`)) {
    return
  }

  uninstalling.value = app.id

  try {
    await api.delete(`/apps/${app.appId}/uninstall/${props.deviceId}`)
    
    // Actualizar el estado localmente
    const appIndex = apps.value.findIndex(a => a.id === app.id)
    if (appIndex !== -1) {
      apps.value[appIndex].status = 'pending'
    }

    alert(`✅ Comando de desinstalación enviado para "${app.app.name}"`)
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Error al desinstalar aplicación'
    alert('❌ Error al enviar comando de desinstalación')
    console.error('Error uninstalling app:', err)
  } finally {
    uninstalling.value = null
  }
}

// WebSocket listener para resultado de desinstalación
const handleUninstallResult = (data: any) => {
  console.log('🗑️ Uninstall result received:', data)
  
  // Si el resultado es para este dispositivo
  if (data.deviceId === props.deviceId) {
    if (data.success) {
      // Remover la app de la lista
      apps.value = apps.value.filter(app => app.app.packageName !== data.packageName)
      alert(`✅ "${data.packageName}" desinstalada exitosamente`)
    } else {
      // Mostrar error
      alert(`❌ Error al desinstalar "${data.packageName}": ${data.errorMessage || 'Error desconocido'}`)
      // Refrescar para obtener el estado actualizado
      fetchApps()
    }
  }
}

// WebSocket listener para resultado de instalación
const handleInstallResult = (data: any) => {
  console.log('📦 Install result received:', data)
  
  // Si el resultado es para este dispositivo, refrescar
  if (data.deviceId === props.deviceId) {
    fetchApps()
  }
}

const getStatusLabel = (status: string): string => {
  const labels: Record<string, string> = {
    pending: 'Pendiente',
    installing: 'Instalando',
    installed: 'Instalada',
    failed: 'Error'
  }
  return labels[status] || status
}

const formatDate = (date: string): string => {
  return new Date(date).toLocaleString('es-ES', {
    year: 'numeric',
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  fetchApps()
  
  // Conectar WebSocket si no está conectado
  if (!websocket.connected) {
    websocket.connect()
  }
  
  // Escuchar eventos de desinstalación e instalación
  websocket.on('app:uninstall-result', handleUninstallResult)
  websocket.on('app:install-result', handleInstallResult)
})

onUnmounted(() => {
  // Limpiar listeners
  websocket.off('app:uninstall-result', handleUninstallResult)
  websocket.off('app:install-result', handleInstallResult)
})

// Refrescar cuando cambia el deviceId
watch(() => props.deviceId, () => {
  fetchApps()
})
</script>

<style scoped>
.device-apps-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  margin-top: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e5e7eb;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.card-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: white;
}

.btn-refresh {
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-refresh:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.3);
}

.btn-refresh:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading-state,
.error-state,
.empty-state {
  padding: 60px 24px;
  text-align: center;
}

.spinner {
  width: 40px;
  height: 40px;
  margin: 0 auto 16px;
  border: 4px solid #f3f4f6;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-state p {
  color: #ef4444;
  margin-bottom: 16px;
}

.btn-retry {
  padding: 8px 16px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
}

.btn-retry:hover {
  background: #5568d3;
}

.empty-state p {
  color: #6b7280;
  font-size: 16px;
}

.apps-table-container {
  overflow-x: auto;
}

.apps-table {
  width: 100%;
  border-collapse: collapse;
}

.apps-table thead {
  background: #f9fafb;
}

.apps-table th {
  padding: 12px 16px;
  text-align: left;
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 2px solid #e5e7eb;
}

.apps-table td {
  padding: 16px;
  border-bottom: 1px solid #e5e7eb;
  font-size: 14px;
  color: #374151;
}

.apps-table tbody tr:hover {
  background: #f9fafb;
}

.app-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.app-icon {
  font-size: 20px;
}

.app-name {
  font-weight: 500;
}

.package-name {
  font-family: 'Monaco', 'Courier New', monospace;
  font-size: 12px;
  color: #6b7280;
}

.status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.status-pending {
  background: #fef3c7;
  color: #92400e;
}

.status-installing {
  background: #dbeafe;
  color: #1e40af;
}

.status-installed {
  background: #d1fae5;
  color: #065f46;
}

.status-failed {
  background: #fee2e2;
  color: #991b1b;
}

.date-cell {
  color: #6b7280;
  font-size: 13px;
}

.actions-cell {
  text-align: center;
}

.btn-uninstall {
  padding: 6px 14px;
  background: #ef4444;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.btn-uninstall:hover:not(:disabled) {
  background: #dc2626;
  transform: translateY(-1px);
  box-shadow: 0 4px 6px rgba(239, 68, 68, 0.2);
}

.btn-uninstall:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.status-text {
  font-size: 13px;
  color: #6b7280;
  font-weight: 500;
}

.card-footer {
  padding: 16px 24px;
  background: #f9fafb;
  border-top: 1px solid #e5e7eb;
  text-align: right;
}

.card-footer p {
  margin: 0;
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
}

@media (max-width: 768px) {
  .apps-table th,
  .apps-table td {
    padding: 12px 8px;
    font-size: 12px;
  }

  .card-header h3 {
    font-size: 18px;
  }

  .btn-refresh,
  .btn-uninstall {
    font-size: 12px;
    padding: 6px 12px;
  }
}
</style>
