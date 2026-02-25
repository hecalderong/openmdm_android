<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useDevicesStore } from '@/stores/devices'

const router = useRouter()
const devicesStore = useDevicesStore()

const isLoading = ref(true)
const searchQuery = ref('')
const filterStatus = ref('all')
const selectedDevices = ref<string[]>([])
const showBulkKioskModal = ref(false)
const bulkKioskPackage = ref('')
const isBulkProcessing = ref(false)
const showBulkScheduleModal = ref(false)
const bulkScheduleForm = ref({
  enabled: true,
  powerOnTime: '08:00',
  powerOffTime: '22:00',
  daysOfWeek: [1, 2, 3, 4, 5]
})
const isBulkScheduleProcessing = ref(false)

const daysOfWeekOptions = [
  { value: 0, label: 'Dom' },
  { value: 1, label: 'Lun' },
  { value: 2, label: 'Mar' },
  { value: 3, label: 'Mié' },
  { value: 4, label: 'Jue' },
  { value: 5, label: 'Vie' },
  { value: 6, label: 'Sáb' }
]

onMounted(async () => {
  await devicesStore.fetchDevices()
  isLoading.value = false
})

const allSelected = computed(() => {
  return filteredDevices.value.length > 0 && 
    selectedDevices.value.length === filteredDevices.value.length
})

const toggleSelectAll = () => {
  if (allSelected.value) {
    selectedDevices.value = []
  } else {
    selectedDevices.value = filteredDevices.value.map(d => d.id)
  }
}

const toggleDeviceSelection = (deviceId: string) => {
  const index = selectedDevices.value.indexOf(deviceId)
  if (index > -1) {
    selectedDevices.value.splice(index, 1)
  } else {
    selectedDevices.value.push(deviceId)
  }
}

const handleBulkKiosk = async () => {
  if (!bulkKioskPackage.value.trim()) {
    alert('Por favor ingresa un nombre de paquete válido')
    return
  }

  if (!confirm(`¿Activar modo kiosk en ${selectedDevices.value.length} dispositivo(s)?\n\nApp: ${bulkKioskPackage.value}`)) {
    return
  }

  isBulkProcessing.value = true
  let successCount = 0
  let failCount = 0

  for (const deviceId of selectedDevices.value) {
    try {
      const success = await devicesStore.enableKioskMode(deviceId, bulkKioskPackage.value)
      if (success) successCount++
      else failCount++
    } catch (error) {
      failCount++
    }
  }

  isBulkProcessing.value = false
  showBulkKioskModal.value = false
  bulkKioskPackage.value = ''
  selectedDevices.value = []

  alert(`Modo kiosk activado:\n✅ Exitosos: ${successCount}\n❌ Fallidos: ${failCount}`)
}

const toggleDayOfWeek = (day: number) => {
  const index = bulkScheduleForm.value.daysOfWeek.indexOf(day)
  if (index > -1) {
    bulkScheduleForm.value.daysOfWeek.splice(index, 1)
  } else {
    bulkScheduleForm.value.daysOfWeek.push(day)
    bulkScheduleForm.value.daysOfWeek.sort()
  }
}

const handleBulkSchedule = async () => {
  if (bulkScheduleForm.value.daysOfWeek.length === 0) {
    alert('Selecciona al menos un día de la semana')
    return
  }

  if (!confirm(`¿Configurar horarios en ${selectedDevices.value.length} dispositivo(s)?\n\nON: ${bulkScheduleForm.value.powerOnTime}\nOFF: ${bulkScheduleForm.value.powerOffTime}`)) {
    return
  }

  isBulkScheduleProcessing.value = true
  let successCount = 0
  let failCount = 0

  for (const deviceId of selectedDevices.value) {
    try {
      await devicesStore.setPowerSchedule(deviceId, bulkScheduleForm.value)
      successCount++
    } catch (error) {
      failCount++
    }
  }

  isBulkScheduleProcessing.value = false
  showBulkScheduleModal.value = false
  selectedDevices.value = []

  alert(`Horarios configurados:\n✅ Exitosos: ${successCount}\n❌ Fallidos: ${failCount}`)
}

const filteredDevices = computed(() => {
  let filtered = devicesStore.devices

  // Filter by search query
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    filtered = filtered.filter(
      (device) =>
        device.name.toLowerCase().includes(query) ||
        device.deviceId.toLowerCase().includes(query)
    )
  }

  // Filter by status
  if (filterStatus.value !== 'all') {
    filtered = filtered.filter((device) => device.status === filterStatus.value)
  }

  return filtered
})

const getStatusClass = (status: string) => {
  const classes: Record<string, string> = {
    online: 'status-online',
    offline: 'status-offline',
    locked: 'status-locked'
  }
  return classes[status] || 'status-unknown'
}

const getStatusLabel = (status: string) => {
  const labels: Record<string, string> = {
    online: 'En Línea',
    offline: 'Fuera de Línea',
    locked: 'Bloqueado'
  }
  return labels[status] || status
}

const handleLockDevice = async (deviceId: string) => {
  if (confirm('¿Estás seguro de que deseas bloquear este dispositivo?')) {
    await devicesStore.lockDevice(deviceId)
  }
}

const handleUnlockDevice = async (deviceId: string) => {
  if (confirm('¿Estás seguro de que deseas desbloquear este dispositivo?')) {
    await devicesStore.unlockDevice(deviceId)
  }
}

const viewDeviceDetail = (deviceId: string) => {
  router.push({ name: 'device-detail', params: { id: deviceId } })
}

const formatDate = (date: string) => {
  return new Date(date).toLocaleString('es-ES', {
    year: 'numeric',
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<template>
  <div class="devices-view">
    <!-- Header -->
    <div class="page-header">
      <h1>Dispositivos</h1>
      <p>Gestiona todos los dispositivos Android conectados al sistema MDM</p>
    </div>

    <!-- Stats Summary -->
    <div class="stats-summary">
      <div class="stat">
        <span class="stat-value">{{ devicesStore.stats.total }}</span>
        <span class="stat-label">Total</span>
      </div>
      <div class="stat stat-success">
        <span class="stat-value">{{ devicesStore.stats.online }}</span>
        <span class="stat-label">En Línea</span>
      </div>
      <div class="stat stat-danger">
        <span class="stat-value">{{ devicesStore.stats.offline }}</span>
        <span class="stat-label">Fuera de Línea</span>
      </div>
      <div class="stat stat-warning">
        <span class="stat-value">{{ devicesStore.stats.locked }}</span>
        <span class="stat-label">Bloqueados</span>
      </div>
    </div>

    <!-- Filters -->
    <div class="filters-bar">
      <input
        v-model="searchQuery"
        type="text"
        placeholder="Buscar dispositivo por nombre o ID..."
        class="search-input"
      />

      <select v-model="filterStatus" class="filter-select">
        <option value="all">Todos los estados</option>
        <option value="online">En Línea</option>
        <option value="offline">Fuera de Línea</option>
        <option value="locked">Bloqueados</option>
      </select>
      
      <button 
        v-if="selectedDevices.length > 0"
        @click="showBulkKioskModal = true"
        class="btn btn-primary btn-bulk"
      >
        🔐 Modo Kiosk ({{ selectedDevices.length }})
      </button>
      
      <button 
        v-if="selectedDevices.length > 0"
        @click="showBulkScheduleModal = true"
        class="btn btn-success btn-bulk"
        style="margin-left: 0.5rem;"
      >
        ⏰ Horarios ({{ selectedDevices.length }})
      </button>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="loading">
      <div class="spinner"></div>
      <p>Cargando dispositivos...</p>
    </div>

    <!-- Devices Table -->
    <div v-else-if="filteredDevices.length > 0" class="devices-table-container">
      <table class="devices-table">
        <thead>
          <tr>
            <th>
              <input 
                type="checkbox" 
                :checked="allSelected"
                @change="toggleSelectAll"
                class="checkbox-select"
              />
            </th>
            <th>Dispositivo</th>
            <th>Estado</th>
            <th>Modo Kiosk</th>
            <th>Batería</th>
            <th>Almacenamiento</th>
            <th>Última Conexión</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="device in filteredDevices" :key="device.id" :class="{ 'row-selected': selectedDevices.includes(device.id) }">
            <td>
              <input 
                type="checkbox" 
                :checked="selectedDevices.includes(device.id)"
                @change="toggleDeviceSelection(device.id)"
                class="checkbox-select"
              />
            </td>
            <td>
              <div class="device-cell">
                <span class="device-icon">📱</span>
                <div>
                  <strong>{{ device.name }}</strong>
                  <div class="device-id">{{ device.deviceId }}</div>
                </div>
              </div>
            </td>
            <td>
              <span :class="['status-badge', getStatusClass(device.status)]">
                {{ getStatusLabel(device.status) }}
              </span>
            </td>
            <td>
              <div v-if="device.kioskModeEnabled" class="kiosk-badge-active">
                🔒 ACTIVO
                <div class="kiosk-package">{{ device.kioskPackageName }}</div>
              </div>
              <span v-else class="kiosk-badge-inactive">-</span>
            </td>
            <td>
              <div class="battery-indicator">
                <div class="battery-bar">
                  <div
                    class="battery-fill"
                    :style="{ width: `${device.batteryLevel || 0}%` }"
                    :class="{
                      'battery-low': (device.batteryLevel || 0) < 20,
                      'battery-medium': (device.batteryLevel || 0) >= 20 && (device.batteryLevel || 0) < 50,
                      'battery-high': (device.batteryLevel || 0) >= 50
                    }"
                  ></div>
                </div>
                <span class="battery-text">{{ device.batteryLevel || 0 }}%</span>
              </div>
            </td>
            <td>
              <div class="storage-indicator">
                {{ device.storageUsed || 0 }} / {{ device.storageTotal || 0 }} GB
                <div class="progress-mini">
                  <div
                    class="progress-fill"
                    :style="{
                      width: `${((device.storageUsed || 0) / (device.storageTotal || 1)) * 100}%`
                    }"
                  ></div>
                </div>
              </div>
            </td>
            <td>
              <span class="timestamp">{{ device.lastSeen ? formatDate(device.lastSeen.toString()) : 'Nunca' }}</span>
            </td>
            <td>
              <div class="actions">
                <button
                  @click="viewDeviceDetail(device.id)"
                  class="btn btn-primary"
                  title="Ver detalles"
                >
                  👁️
                </button>
                <button
                  v-if="device.status !== 'inactive'"
                  @click="handleLockDevice(device.id)"
                  class="btn btn-warning"
                  title="Bloquear"
                >
                  🔒
                </button>
                <button
                  v-else
                  @click="handleUnlockDevice(device.id)"
                  class="btn btn-success"
                  title="Desbloquear"
                >
                  🔓
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Empty State -->
    <div v-else class="empty-state">
      <div class="empty-icon">📱</div>
      <h3>No se encontraron dispositivos</h3>
      <p v-if="searchQuery || filterStatus !== 'all'">
        Intenta ajustar los filtros de búsqueda
      </p>
      <p v-else>No hay dispositivos registrados en el sistema</p>
    </div>

    <!-- Bulk Kiosk Modal -->
    <div v-if="showBulkKioskModal" class="modal-overlay" @click.self="showBulkKioskModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h2>🔐 Activar Modo Kiosk Masivo</h2>
          <button @click="showBulkKioskModal = false" class="modal-close">×</button>
        </div>
        <div class="modal-body">
          <div class="bulk-warning">
            <span class="warning-icon">⚠️</span>
            <div>
              <strong>Aplicar a {{ selectedDevices.length }} dispositivo(s)</strong>
              <p>Todos los dispositivos seleccionados quedarán bloqueados a la misma aplicación</p>
            </div>
          </div>

          <div class="form-group">
            <label for="bulkKioskPackage">Nombre del Paquete</label>
            <input
              id="bulkKioskPackage"
              v-model="bulkKioskPackage"
              type="text"
              class="form-control"
              placeholder="com.empresa.kiosk"
              @keyup.enter="handleBulkKiosk"
            />
          </div>

          <div class="common-apps">
            <button 
              v-for="app in [
                { name: 'Chrome', package: 'com.android.chrome' },
                { name: 'Gmail', package: 'com.google.android.gm' },
                { name: 'Maps', package: 'com.google.android.apps.maps' }
              ]"
              :key="app.package"
              @click="bulkKioskPackage = app.package"
              class="app-quick-btn"
            >
              {{ app.name }}
            </button>
          </div>
        </div>
        <div class="modal-actions">
          <button @click="showBulkKioskModal = false" class="btn-secondary">
            Cancelar
          </button>
          <button
            @click="handleBulkKiosk"
            :disabled="!bulkKioskPackage.trim() || isBulkProcessing"
            class="btn-primary"
          >
            {{ isBulkProcessing ? 'Procesando...' : `🔐 Activar en ${selectedDevices.length} dispositivo(s)` }}
          </button>
        </div>
      </div>
    </div>

    <!-- Bulk Schedule Modal -->
    <div v-if="showBulkScheduleModal" class="modal-overlay" @click.self="showBulkScheduleModal = false">
      <div class="modal-content schedule-modal">
        <div class="modal-header">
          <h2>⏰ Configurar Horarios Masivos</h2>
          <button @click="showBulkScheduleModal = false" class="modal-close">×</button>
        </div>
        <div class="modal-body">
          <div class="bulk-warning">
            <strong>⚠️ Atención:</strong>
            <p>Esta configuración se aplicará a <strong>{{ selectedDevices.length }} dispositivo(s)</strong> seleccionado(s).</p>
          </div>

          <div class="schedule-form">
            <div class="form-row">
              <div class="form-group">
                <label for="bulkPowerOnTime">
                  <span class="time-icon">🌅</span> Hora de Encendido
                </label>
                <input
                  id="bulkPowerOnTime"
                  v-model="bulkScheduleForm.powerOnTime"
                  type="time"
                  class="form-control"
                />
              </div>
              <div class="form-group">
                <label for="bulkPowerOffTime">
                  <span class="time-icon">🌙</span> Hora de Apagado
                </label>
                <input
                  id="bulkPowerOffTime"
                  v-model="bulkScheduleForm.powerOffTime"
                  type="time"
                  class="form-control"
                />
              </div>
            </div>

            <div class="form-group">
              <label>Días de la Semana</label>
              <div class="days-selector">
                <button
                  v-for="day in daysOfWeekOptions"
                  :key="day.value"
                  @click="toggleDayOfWeek(day.value)"
                  class="day-btn"
                  :class="{ active: bulkScheduleForm.daysOfWeek.includes(day.value) }"
                  type="button"
                >
                  {{ day.label }}
                </button>
              </div>
            </div>

            <div class="schedule-info-box">
              <h4>ℹ️ Información</h4>
              <ul>
                <li>Los dispositivos se bloquearán a la hora de apagado</li>
                <li>Los dispositivos se desbloquearán a la hora de encendido</li>
                <li>Solo funciona en los días seleccionados</li>
              </ul>
            </div>
          </div>
        </div>
        <div class="modal-actions">
          <button
            @click="showBulkScheduleModal = false"
            class="btn-secondary"
          >
            Cancelar
          </button>
          <button
            @click="handleBulkSchedule"
            :disabled="bulkScheduleForm.daysOfWeek.length === 0 || isBulkScheduleProcessing"
            class="btn-primary"
          >
            {{ isBulkScheduleProcessing ? 'Procesando...' : `💾 Configurar ${selectedDevices.length} dispositivo(s)` }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.devices-view {
  animation: fadeIn 0.5s;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.page-header {
  margin-bottom: 2rem;
}

.page-header h1 {
  margin: 0 0 0.5rem 0;
  font-size: 2rem;
  color: #333;
}

.page-header p {
  margin: 0;
  color: #666;
}

/* Stats Summary */
.stats-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 1rem;
  margin-bottom: 2rem;
}

.stat {
  background: white;
  padding: 1rem;
  border-radius: 8px;
  text-align: center;
  border-left: 4px solid #667eea;
}

.stat-success {
  border-left-color: #10b981;
}

.stat-danger {
  border-left-color: #ef4444;
}

.stat-warning {
  border-left-color: #f59e0b;
}

.stat-value {
  display: block;
  font-size: 2rem;
  font-weight: 700;
  color: #333;
  margin-bottom: 0.25rem;
}

.stat-label {
  display: block;
  font-size: 0.85rem;
  color: #666;
}

/* Filters */
.filters-bar {
  display: flex;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.search-input {
  flex: 1;
  padding: 0.75rem 1rem;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 1rem;
}

.search-input:focus {
  outline: none;
  border-color: #667eea;
}

.filter-select {
  padding: 0.75rem 1rem;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 1rem;
  background: white;
  cursor: pointer;
}

.filter-select:focus {
  outline: none;
  border-color: #667eea;
}

/* Loading */
.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem;
  gap: 1rem;
}

.spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f3f4f6;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* Table */
.devices-table-container {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.devices-table {
  width: 100%;
  border-collapse: collapse;
}

.devices-table thead {
  background-color: #f9fafb;
}

.devices-table th {
  padding: 1rem;
  text-align: left;
  font-weight: 600;
  color: #333;
  border-bottom: 2px solid #e5e7eb;
}

.devices-table tbody tr {
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.2s;
}

.devices-table tbody tr:hover {
  background-color: #f9fafb;
}

.devices-table td {
  padding: 1rem;
}

.device-cell {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.device-icon {
  font-size: 1.5rem;
}

.device-id {
  font-size: 0.8rem;
  color: #666;
  margin-top: 0.25rem;
}

/* Status Badge */
.status-badge {
  padding: 0.35rem 0.85rem;
  border-radius: 12px;
  font-size: 0.8rem;
  font-weight: 600;
  display: inline-block;
}

.status-online {
  background-color: #d1fae5;
  color: #065f46;
}

.status-offline {
  background-color: #fee2e2;
  color: #991b1b;
}

.status-locked {
  background-color: #fef3c7;
  color: #92400e;
}

/* Battery Indicator */
.battery-indicator {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.battery-bar {
  width: 60px;
  height: 20px;
  background-color: #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
}

.battery-fill {
  height: 100%;
  transition: width 0.3s;
}

.battery-low {
  background-color: #ef4444;
}

.battery-medium {
  background-color: #f59e0b;
}

.battery-high {
  background-color: #10b981;
}

.battery-text {
  font-size: 0.85rem;
  font-weight: 600;
}

/* Storage/RAM Indicators */
.storage-indicator,
.ram-indicator {
  font-size: 0.9rem;
  color: #333;
}

.progress-mini {
  width: 100%;
  height: 4px;
  background-color: #e5e7eb;
  border-radius: 2px;
  margin-top: 0.25rem;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background-color: #667eea;
  transition: width 0.3s;
}

/* Timestamp */
.timestamp {
  font-size: 0.85rem;
  color: #666;
}

/* Actions */
.actions {
  display: flex;
  gap: 0.5rem;
}

.btn {
  padding: 0.5rem 0.75rem;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1.2rem;
  transition: transform 0.2s, box-shadow 0.2s;
}

.btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.btn-primary {
  background-color: #667eea;
}

.btn-warning {
  background-color: #f59e0b;
}

.btn-success {
  background-color: #10b981;
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 4rem 2rem;
  background: white;
  border-radius: 12px;
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.empty-state h3 {
  margin: 0 0 0.5rem 0;
  color: #333;
}

.empty-state p {
  margin: 0;
  color: #666;
}

/* Responsive */
@media (max-width: 1024px) {
  .devices-table-container {
    overflow-x: auto;
  }

  .devices-table {
    min-width: 900px;
  }
}

@media (max-width: 768px) {
  .filters-bar {
    flex-direction: column;
  }

  .stats-summary {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* Checkbox Selection */
.checkbox-select {
  width: 18px;
  height: 18px;
  cursor: pointer;
  accent-color: #667eea;
}

.row-selected {
  background-color: #f0f4ff !important;
}

/* Kiosk Badge */
.kiosk-badge-active {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.25rem;
  padding: 0.5rem 0.75rem;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  border-radius: 8px;
  font-size: 0.75rem;
  font-weight: 600;
}

.kiosk-package {
  font-size: 0.7rem;
  opacity: 0.9;
  font-family: 'Courier New', monospace;
}

.kiosk-badge-inactive {
  color: #999;
  font-size: 1.2rem;
}

/* Bulk Actions */
.btn-bulk {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 0.75rem 1.5rem;
  font-weight: 600;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(102, 126, 234, 0.7);
  }
  50% {
    box-shadow: 0 0 0 10px rgba(102, 126, 234, 0);
  }
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.modal-content {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: slideUp 0.3s;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid #e5e7eb;
}

.modal-header h2 {
  margin: 0;
  font-size: 1.5rem;
  color: #333;
}

.modal-close {
  background: none;
  border: none;
  font-size: 2rem;
  cursor: pointer;
  color: #999;
  line-height: 1;
}

.modal-close:hover {
  color: #333;
}

.modal-body {
  padding: 1.5rem;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  padding: 1.5rem;
  border-top: 1px solid #e5e7eb;
}

.bulk-warning {
  display: flex;
  gap: 1rem;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border: 2px solid #f59e0b;
  border-radius: 8px;
  padding: 1rem;
  margin-bottom: 1.5rem;
}

.warning-icon {
  font-size: 1.5rem;
}

.bulk-warning strong {
  display: block;
  color: #92400e;
  margin-bottom: 0.25rem;
}

.bulk-warning p {
  margin: 0;
  color: #78350f;
  font-size: 0.9rem;
}

/* Schedule Modal Styles */
.schedule-modal .modal-content {
  max-width: 600px;
}

.schedule-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.form-group label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: #374151;
  margin-bottom: 0.5rem;
}

.time-icon {
  font-size: 1.2rem;
}

.form-control[type="time"] {
  width: 100%;
  padding: 0.75rem;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  font-size: 1rem;
  transition: all 0.2s;
}

.form-control[type="time"]:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.days-selector {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.day-btn {
  padding: 0.75rem 1rem;
  border: 2px solid #e5e7eb;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  color: #6b7280;
  transition: all 0.2s;
  min-width: 60px;
}

.day-btn:hover {
  border-color: #667eea;
  color: #667eea;
}

.day-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: #667eea;
  color: white;
}

.schedule-info-box {
  background: #eff6ff;
  border: 2px solid #3b82f6;
  border-radius: 8px;
  padding: 1rem;
}

.schedule-info-box h4 {
  color: #1e40af;
  margin-top: 0;
  margin-bottom: 0.75rem;
  font-size: 1rem;
}

.schedule-info-box ul {
  margin: 0;
  padding-left: 1.5rem;
}

.schedule-info-box li {
  color: #1e3a8a;
  margin-bottom: 0.5rem;
  line-height: 1.5;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #333;
}

.form-control {
  width: 100%;
  padding: 0.75rem;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  font-size: 1rem;
  transition: border-color 0.3s;
}

.form-control:focus {
  outline: none;
  border-color: #667eea;
}

.common-apps {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-top: 1rem;
}

.app-quick-btn {
  padding: 0.5rem 1rem;
  border: 2px solid #e5e7eb;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.2s;
}

.app-quick-btn:hover {
  border-color: #667eea;
  background: #f0f4ff;
}

.btn-secondary {
  padding: 0.75rem 1.5rem;
  background: #e5e7eb;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  color: #333;
  transition: background 0.2s;
}

.btn-secondary:hover {
  background: #d1d5db;
}

.btn-primary {
  padding: 0.75rem 1.5rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  color: white;
  transition: transform 0.2s;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
