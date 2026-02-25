<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDevicesStore } from '@/stores/devices'
import DeviceAppsCard from '@/components/DeviceAppsCard.vue'

const route = useRoute()
const router = useRouter()
const devicesStore = useDevicesStore()

const deviceId = computed(() => String(route.params.id))
const device = computed(() => 
  devicesStore.devices.find((d) => d.id === deviceId.value)
)

const isLoading = ref(true)

onMounted(async () => {
  await devicesStore.fetchDevices()
  isLoading.value = false

  if (!device.value) {
    router.push({ name: 'devices' })
  }
})

const handleLockDevice = async () => {
  if (confirm('¿Estás seguro de que deseas bloquear este dispositivo?')) {
    await devicesStore.lockDevice(deviceId.value)
  }
}

const handleUnlockDevice = async () => {
  if (confirm('¿Estás seguro de que deseas desbloquear este dispositivo?')) {
    await devicesStore.unlockDevice(deviceId.value)
  }
}

const showLaunchAppModal = ref(false)
const launchAppPackageName = ref('')
const isLaunchingApp = ref(false)

const handleLaunchApp = async () => {
  if (!launchAppPackageName.value.trim()) {
    alert('Por favor ingresa un nombre de paquete válido')
    return
  }

  isLaunchingApp.value = true
  try {
    await devicesStore.launchApp(deviceId.value, launchAppPackageName.value)
    alert(`Comando enviado para lanzar: ${launchAppPackageName.value}`)
    showLaunchAppModal.value = false
    launchAppPackageName.value = ''
  } catch (error) {
    console.error('Error launching app:', error)
    alert('Error al enviar el comando')
  } finally {
    isLaunchingApp.value = false
  }
}

const commonApps = [
  { name: 'Chrome', package: 'com.android.chrome' },
  { name: 'Gmail', package: 'com.google.android.gm' },
  { name: 'Maps', package: 'com.google.android.apps.maps' },
  { name: 'YouTube', package: 'com.google.android.youtube' },
  { name: 'WhatsApp', package: 'com.whatsapp' },
  { name: 'Instagram', package: 'com.instagram.android' },
  { name: 'Facebook', package: 'com.facebook.katana' },
  { name: 'Twitter', package: 'com.twitter.android' },
]

const selectCommonApp = (packageName: string) => {
  launchAppPackageName.value = packageName
}

const showKioskModal = ref(false)
const kioskPackageName = ref('')
const isTogglingKiosk = ref(false)

// Power Schedule state
const showScheduleModal = ref(false)
const isSavingSchedule = ref(false)
const scheduleForm = ref({
  enabled: true,
  powerOnTime: '08:00',
  powerOffTime: '22:00',
  daysOfWeek: [1, 2, 3, 4, 5] // Lunes a Viernes por defecto
})

const daysOfWeekOptions = [
  { value: 0, label: 'Dom' },
  { value: 1, label: 'Lun' },
  { value: 2, label: 'Mar' },
  { value: 3, label: 'Mié' },
  { value: 4, label: 'Jue' },
  { value: 5, label: 'Vie' },
  { value: 6, label: 'Sáb' }
]

const toggleDayOfWeek = (day: number) => {
  const index = scheduleForm.value.daysOfWeek.indexOf(day)
  if (index > -1) {
    scheduleForm.value.daysOfWeek.splice(index, 1)
  } else {
    scheduleForm.value.daysOfWeek.push(day)
    scheduleForm.value.daysOfWeek.sort()
  }
}

const handleSavePowerSchedule = async () => {
  if (scheduleForm.value.daysOfWeek.length === 0) {
    alert('Selecciona al menos un día de la semana')
    return
  }

  if (!confirm(`¿Configurar horarios?\n\nON: ${scheduleForm.value.powerOnTime}\nOFF: ${scheduleForm.value.powerOffTime}\n\nEl dispositivo se bloqueará automáticamente en el horario configurado.`)) {
    return
  }

  isSavingSchedule.value = true
  try {
    await devicesStore.setPowerSchedule(deviceId.value, scheduleForm.value)
    alert('Horarios configurados correctamente')
    showScheduleModal.value = false
    await devicesStore.fetchDevices()
  } catch (error) {
    console.error('Error saving schedule:', error)
    alert('Error al guardar horarios')
  } finally {
    isSavingSchedule.value = false
  }
}

const handleClearPowerSchedule = async () => {
  if (!confirm('¿Eliminar horarios programados?\n\nEl dispositivo dejará de bloquearse automáticamente.')) {
    return
  }

  try {
    await devicesStore.clearPowerSchedule(deviceId.value)
    alert('Horarios eliminados correctamente')
    await devicesStore.fetchDevices()
  } catch (error) {
    console.error('Error clearing schedule:', error)
    alert('Error al eliminar horarios')
  }
}

const formatDaysOfWeek = (days: number[]) => {
  if (!days || days.length === 0) return '-'
  const dayNames = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb']
  return days.map(d => dayNames[d]).join(', ')
}

const handleEnableKiosk = async () => {
  if (!kioskPackageName.value.trim()) {
    alert('Por favor ingresa un nombre de paquete válido')
    return
  }

  if (!confirm(`¿Activar modo kiosk con la app:\n${kioskPackageName.value}?\n\nEl usuario NO podrá salir de esta app hasta que desactives el modo kiosk.`)) {
    return
  }

  isTogglingKiosk.value = true
  try {
    const success = await devicesStore.enableKioskMode(deviceId.value, kioskPackageName.value)
    if (success) {
      alert(`Modo kiosk activado con: ${kioskPackageName.value}`)
      showKioskModal.value = false
      kioskPackageName.value = ''
    } else {
      alert('Error al activar modo kiosk')
    }
  } catch (error) {
    console.error('Error enabling kiosk:', error)
    alert('Error al activar modo kiosk')
  } finally {
    isTogglingKiosk.value = false
  }
}

const handleDisableKiosk = async () => {
  if (!confirm('¿Desactivar modo kiosk?\n\nEl dispositivo volverá a funcionar normalmente.')) {
    return
  }

  isTogglingKiosk.value = true
  try {
    const success = await devicesStore.disableKioskMode(deviceId.value)
    if (success) {
      alert('Modo kiosk desactivado')
    } else {
      alert('Error al desactivar modo kiosk')
    }
  } catch (error) {
    console.error('Error disabling kiosk:', error)
    alert('Error al desactivar modo kiosk')
  } finally {
    isTogglingKiosk.value = false
  }
}

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

const formatDate = (date: string) => {
  return new Date(date).toLocaleString('es-ES', {
    year: 'numeric',
    month: 'long',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}
</script>

<template>
  <div class="device-detail">
    <!-- Loading State -->
    <div v-if="isLoading" class="loading">
      <div class="spinner"></div>
      <p>Cargando información del dispositivo...</p>
    </div>

    <!-- Device Not Found -->
    <div v-else-if="!device" class="not-found">
      <h2>Dispositivo no encontrado</h2>
      <RouterLink :to="{ name: 'devices' }" class="btn-primary">
        ← Volver a dispositivos
      </RouterLink>
    </div>

    <!-- Device Info -->
    <div v-else class="device-content">
      <!-- Header -->
      <div class="detail-header">
        <button @click="router.back()" class="btn-back">← Volver</button>
        <div class="header-title">
          <h1>{{ device.name }}</h1>
          <span :class="['status-badge', getStatusClass(device.status)]">
            {{ getStatusLabel(device.status) }}
          </span>
        </div>
        <div class="header-actions">
          <button
            @click="showLaunchAppModal = true"
            class="btn btn-info"
            :disabled="device.status === 'offline'"
          >
            🚀 Lanzar App
          </button>
          <button
            v-if="device.status !== 'inactive'"
            @click="handleLockDevice"
            class="btn btn-warning"
          >
            🔒 Bloquear
          </button>
          <button
            v-else
            @click="handleUnlockDevice"
            class="btn btn-success"
          >
            🔓 Desbloquear
          </button>
        </div>
      </div>

      <!-- Kiosk Mode Card -->
      <div class="card kiosk-card">
        <div class="card-header">
          <h2>🔒 Modo Kiosk</h2>
          <span v-if="device.kioskModeEnabled" class="kiosk-active-badge">ACTIVO</span>
        </div>
        <div class="card-body">
          <div v-if="device.kioskModeEnabled" class="kiosk-active">
            <div class="kiosk-info">
              <div class="kiosk-status">
                <span class="kiosk-icon">🔐</span>
                <div class="kiosk-details">
                  <strong>Modo Kiosk Activo</strong>
                  <p>App bloqueada: <code>{{ device.kioskPackageName }}</code></p>
                  <p class="kiosk-hint">El usuario no puede salir de esta aplicación</p>
                </div>
              </div>
              <button
                @click="handleDisableKiosk"
                class="btn btn-danger"
                :disabled="device.status === 'offline' || isTogglingKiosk"
              >
                <span v-if="isTogglingKiosk">Desactivando...</span>
                <span v-else>🔓 Desactivar Kiosk</span>
              </button>
            </div>
          </div>
          <div v-else class="kiosk-inactive">
            <p class="kiosk-description">
              El modo kiosk bloquea el dispositivo para ejecutar una sola aplicación.
              Ideal para puntos de venta, kioscos de información o tablets de propósito único.
            </p>
            <button
              @click="showKioskModal = true"
              class="btn btn-primary"
              :disabled="device.status === 'offline'"
            >
              🔐 Activar Modo Kiosk
            </button>
          </div>
        </div>
      </div>

      <!-- Power Schedule Card -->
      <div class="card schedule-card">
        <div class="card-header">
          <h2>⏰ Horarios Programados</h2>
          <span v-if="device.powerSchedule?.enabled" class="schedule-active-badge">ACTIVO</span>
        </div>
        <div class="card-body">
          <div v-if="device.powerSchedule?.enabled" class="schedule-active">
            <div class="schedule-info">
              <div class="schedule-times">
                <div class="time-item">
                  <span class="time-icon">🌅</span>
                  <div>
                    <strong>Encendido</strong>
                    <p class="time-value">{{ device.powerSchedule.powerOnTime }}</p>
                  </div>
                </div>
                <div class="time-item">
                  <span class="time-icon">🌙</span>
                  <div>
                    <strong>Apagado</strong>
                    <p class="time-value">{{ device.powerSchedule.powerOffTime }}</p>
                  </div>
                </div>
              </div>
              <div class="schedule-days">
                <strong>Días activos:</strong>
                <p>{{ formatDaysOfWeek(device.powerSchedule.daysOfWeek) }}</p>
              </div>
              <button
                @click="handleClearPowerSchedule"
                class="btn btn-danger btn-sm"
                :disabled="device.status === 'offline'"
              >
                🗑️ Eliminar Horarios
              </button>
            </div>
          </div>
          <div v-else class="schedule-inactive">
            <p class="schedule-description">
              Configura horarios automáticos para bloquear/desbloquear el dispositivo.
              Ideal para controlar el acceso durante horarios laborales.
            </p>
            <button
              @click="showScheduleModal = true"
              class="btn btn-primary"
              :disabled="device.status === 'offline'"
            >
              ⏰ Configurar Horarios
            </button>
          </div>
        </div>
      </div>

      <!-- Main Info Grid -->
      <div class="info-grid">
        <!-- Device Info Card -->
        <div class="card">
          <div class="card-header">
            <h2>Información del Dispositivo</h2>
          </div>
          <div class="card-body">
            <div class="info-row">
              <span class="info-label">ID del Dispositivo</span>
              <span class="info-value">{{ device.deviceId }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">Nombre</span>
              <span class="info-value">{{ device.name }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">Estado</span>
              <span :class="['status-badge', getStatusClass(device.status)]">
                {{ getStatusLabel(device.status) }}
              </span>
            </div>
            <div class="info-row">
              <span class="info-label">Última Conexión</span>
              <span class="info-value">{{ device.lastSeen ? formatDate(device.lastSeen.toString()) : 'Nunca' }}</span>
            </div>
          </div>
        </div>

        <!-- Battery Card -->
        <div class="card">
          <div class="card-header">
            <h2>🔋 Batería</h2>
          </div>
          <div class="card-body">
            <div class="battery-circle">
              <svg viewBox="0 0 100 100" class="battery-svg">
                <circle
                  cx="50"
                  cy="50"
                  r="40"
                  stroke="#e5e7eb"
                  stroke-width="8"
                  fill="none"
                />
                <circle
                  cx="50"
                  cy="50"
                  r="40"
                  :stroke="(device.batteryLevel || 0) > 50 ? '#10b981' : (device.batteryLevel || 0) > 20 ? '#f59e0b' : '#ef4444'"
                  stroke-width="8"
                  fill="none"
                  :stroke-dasharray="`${(device.batteryLevel || 0) * 2.51} 251`"
                  stroke-linecap="round"
                  transform="rotate(-90 50 50)"
                />
              </svg>
              <div class="battery-percent">{{ device.batteryLevel || 0 }}%</div>
            </div>
          </div>
        </div>

        <!-- Storage Card -->
        <div class="card">
          <div class="card-header">
            <h2>💾 Almacenamiento</h2>
          </div>
          <div class="card-body">
            <div class="storage-info">
              <div class="storage-bar">
                <div
                  class="storage-fill"
                  :style="{
                    width: `${((device.storageUsed || 0) / (device.storageTotal || 1)) * 100}%`
                  }"
                ></div>
              </div>
              <div class="storage-text">
                <span class="storage-used">{{ device.storageUsed || 0 }} GB</span>
                <span class="storage-total">de {{ device.storageTotal || 0 }} GB</span>
              </div>
              <div class="storage-percent">
                {{ Math.round(((device.storageUsed || 0) / (device.storageTotal || 1)) * 100) }}% utilizado
              </div>
            </div>
          </div>
        </div>

        <!-- RAM Card -->
        <div class="card">
          <div class="card-header">
            <h2>🧠 Memoria RAM</h2>
          </div>
          <div class="card-body">
            <div class="ram-info">
              <div class="ram-bar">
                <div
                  class="ram-fill"
                  :style="{
                    width: `${((device.ramUsed || 0) / (device.ramTotal || 1)) * 100}%`
                  }"
                ></div>
              </div>
              <div class="ram-text">
                <span class="ram-used">{{ device.ramUsed || 0 }} GB</span>
                <span class="ram-total">de {{ device.ramTotal || 0 }} GB</span>
              </div>
              <div class="ram-percent">
                {{ Math.round(((device.ramUsed || 0) / (device.ramTotal || 1)) * 100) }}% utilizado
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Location Card -->
      <div v-if="device.location" class="card">
        <div class="card-header">
          <h2>📍 Ubicación</h2>
        </div>
        <div class="card-body">
          <div class="location-info">
            <div class="location-coords">
              <div class="coord-item">
                <span class="coord-label">Latitud</span>
                <span class="coord-value">{{ device.location.latitude }}</span>
              </div>
              <div class="coord-item">
                <span class="coord-label">Longitud</span>
                <span class="coord-value">{{ device.location.longitude }}</span>
              </div>
            </div>
            <div class="map-placeholder">
              🗺️ Mapa (Integración con Google Maps o similar)
            </div>
          </div>
        </div>
      </div>

      <!-- Activity Log Card -->
      <div class="card">
        <div class="card-header">
          <h2>📝 Registro de Actividad</h2>
        </div>
        <div class="card-body">
          <div class="activity-log">
            <div class="log-item">
              <div class="log-icon">✓</div>
              <div class="log-content">
                <strong>Dispositivo conectado</strong>
                <span class="log-time">{{ device.lastSeen ? formatDate(device.lastSeen.toString()) : 'Nunca' }}</span>
              </div>
            </div>
            <div class="log-item">
              <div class="log-icon">📊</div>
              <div class="log-content">
                <strong>Actualización de estado</strong>
                <span class="log-time">Batería: {{ device.batteryLevel || 0 }}%</span>
              </div>
            </div>
            <div class="log-item">
              <div class="log-icon">📍</div>
              <div class="log-content">
                <strong>Ubicación actualizada</strong>
                <span class="log-time" v-if="device.location">
                  {{ device.location.latitude }}, {{ device.location.longitude }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Apps Installed Card -->
      <DeviceAppsCard :device-id="deviceId" />
    </div>

    <!-- Launch App Modal -->
    <div v-if="showLaunchAppModal" class="modal-overlay" @click.self="showLaunchAppModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h2>🚀 Lanzar Aplicación</h2>
          <button @click="showLaunchAppModal = false" class="modal-close">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label for="packageName">Nombre del Paquete</label>
            <input
              id="packageName"
              v-model="launchAppPackageName"
              type="text"
              class="form-control"
              placeholder="com.ejemplo.miapp"
              @keyup.enter="handleLaunchApp"
            />
          </div>
          
          <div class="common-apps">
            <h3>Aplicaciones Comunes</h3>
            <div class="apps-grid">
              <button
                v-for="app in commonApps"
                :key="app.package"
                @click="selectCommonApp(app.package)"
                class="app-btn"
                :class="{ active: launchAppPackageName === app.package }"
              >
                {{ app.name }}
              </button>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="showLaunchAppModal = false" class="btn-cancel">
            Cancelar
          </button>
          <button
            @click="handleLaunchApp"
            :disabled="!launchAppPackageName.trim() || isLaunchingApp"
            class="btn-primary"
          >
            {{ isLaunchingApp ? 'Enviando...' : 'Lanzar App' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Kiosk Mode Modal -->
    <div v-if="showKioskModal" class="modal-overlay" @click.self="showKioskModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h2>🔐 Activar Modo Kiosk</h2>
          <button @click="showKioskModal = false" class="modal-close">×</button>
        </div>
        <div class="modal-body">
          <div class="kiosk-warning">
            <span class="warning-icon">⚠️</span>
            <div class="warning-text">
              <strong>Modo Kiosk Permanente</strong>
              <p>El dispositivo quedará bloqueado a una sola aplicación. El usuario NO podrá salir ni acceder a otras apps hasta que desactives el modo kiosk desde este dashboard.</p>
            </div>
          </div>
          
          <div class="form-group">
            <label for="kioskPackageName">Nombre del Paquete</label>
            <input
              id="kioskPackageName"
              v-model="kioskPackageName"
              type="text"
              class="form-control"
              placeholder="com.empresa.kiosk"
              @keyup.enter="handleEnableKiosk"
            />
            <span class="form-hint">Ejemplo: com.android.chrome, com.empresa.pos</span>
          </div>

          <div class="common-apps-section">
            <h4>Apps Comunes:</h4>
            <div class="common-apps-grid">
              <button
                v-for="app in commonApps"
                :key="app.package"
                @click="kioskPackageName = app.package"
                class="common-app-btn"
                :class="{ selected: kioskPackageName === app.package }"
              >
                {{ app.name }}
              </button>
            </div>
          </div>

          <div class="kiosk-features">
            <h4>Características del Modo Kiosk:</h4>
            <ul>
              <li>✅ App bloqueada permanentemente</li>
              <li>✅ Auto-inicio al encender dispositivo</li>
              <li>✅ Botones HOME, BACK y RECENT deshabilitados</li>
              <li>✅ Notificaciones ocultas</li>
              <li>✅ Acceso a ajustes bloqueado</li>
              <li>⚠️ Requiere permisos Device Owner</li>
            </ul>
          </div>
        </div>
        <div class="modal-actions">
          <button
            @click="showKioskModal = false"
            class="btn-secondary"
          >
            Cancelar
          </button>
          <button
            @click="handleEnableKiosk"
            :disabled="!kioskPackageName.trim() || isTogglingKiosk"
            class="btn-primary"
          >
            {{ isTogglingKiosk ? 'Activando...' : '🔐 Activar Modo Kiosk' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Power Schedule Modal -->
    <div v-if="showScheduleModal" class="modal-overlay" @click.self="showScheduleModal = false">
      <div class="modal-content schedule-modal">
        <div class="modal-header">
          <h2>⏰ Configurar Horarios Programados</h2>
          <button @click="showScheduleModal = false" class="modal-close">×</button>
        </div>
        <div class="modal-body">
          <div class="schedule-form">
            <div class="form-row">
              <div class="form-group">
                <label for="powerOnTime">
                  <span class="time-icon">🌅</span> Hora de Encendido
                </label>
                <input
                  id="powerOnTime"
                  v-model="scheduleForm.powerOnTime"
                  type="time"
                  class="form-control"
                />
              </div>
              <div class="form-group">
                <label for="powerOffTime">
                  <span class="time-icon">🌙</span> Hora de Apagado
                </label>
                <input
                  id="powerOffTime"
                  v-model="scheduleForm.powerOffTime"
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
                  :class="{ active: scheduleForm.daysOfWeek.includes(day.value) }"
                  type="button"
                >
                  {{ day.label }}
                </button>
              </div>
            </div>

            <div class="schedule-info-box">
              <h4>ℹ️ Información Importante</h4>
              <ul>
                <li>El dispositivo se <strong>bloqueará automáticamente</strong> a la hora de apagado</li>
                <li>El dispositivo se <strong>desbloqueará</strong> a la hora de encendido</li>
                <li>Funciona solo en los días seleccionados</li>
                <li>Requiere que el dispositivo esté online durante la configuración</li>
              </ul>
            </div>
          </div>
        </div>
        <div class="modal-actions">
          <button
            @click="showScheduleModal = false"
            class="btn-secondary"
          >
            Cancelar
          </button>
          <button
            @click="handleSavePowerSchedule"
            :disabled="isSavingSchedule || scheduleForm.daysOfWeek.length === 0"
            class="btn-primary"
          >
            {{ isSavingSchedule ? 'Guardando...' : '💾 Guardar Horarios' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.device-detail {
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

/* Not Found */
.not-found {
  text-align: center;
  padding: 4rem;
  background: white;
  border-radius: 12px;
}

.btn-primary {
  display: inline-block;
  padding: 0.75rem 1.5rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  text-decoration: none;
  border-radius: 8px;
  font-weight: 600;
  margin-top: 1rem;
}

/* Header */
.detail-header {
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  margin-bottom: 2rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.btn-back {
  background: none;
  border: none;
  color: #667eea;
  font-weight: 600;
  cursor: pointer;
  margin-bottom: 1rem;
  font-size: 1rem;
}

.btn-back:hover {
  text-decoration: underline;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1rem;
}

.header-title h1 {
  margin: 0;
  font-size: 2rem;
  color: #333;
}

.header-actions {
  display: flex;
  gap: 0.75rem;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s;
}

.btn:hover {
  transform: translateY(-2px);
}

.btn-warning {
  background-color: #f59e0b;
  color: white;
}

.btn-success {
  background-color: #10b981;
  color: white;
}

/* Status Badge */
.status-badge {
  padding: 0.35rem 0.85rem;
  border-radius: 12px;
  font-size: 0.85rem;
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

/* Info Grid */
.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1.5rem;
  margin-bottom: 1.5rem;
}

/* Card */
.card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.card-header {
  padding: 1rem 1.5rem;
  border-bottom: 1px solid #f0f0f0;
}

.card-header h2 {
  margin: 0;
  font-size: 1.1rem;
  color: #333;
}

.card-body {
  padding: 1.5rem;
}

/* Device Info */
.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  font-weight: 600;
  color: #666;
  font-size: 0.9rem;
}

.info-value {
  color: #333;
  font-size: 0.95rem;
}

/* Battery Circle */
.battery-circle {
  position: relative;
  width: 150px;
  height: 150px;
  margin: 0 auto;
}

.battery-svg {
  width: 100%;
  height: 100%;
}

.battery-percent {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 2rem;
  font-weight: 700;
  color: #333;
}

/* Storage & RAM */
.storage-info,
.ram-info {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.storage-bar,
.ram-bar {
  width: 100%;
  height: 30px;
  background-color: #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}

.storage-fill {
  height: 100%;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  transition: width 0.3s;
}

.ram-fill {
  height: 100%;
  background: linear-gradient(90deg, #10b981 0%, #059669 100%);
  transition: width 0.3s;
}

.storage-text,
.ram-text {
  display: flex;
  justify-content: space-between;
  font-size: 1.1rem;
}

.storage-used,
.ram-used {
  font-weight: 700;
  color: #333;
}

.storage-total,
.ram-total {
  color: #666;
}

.storage-percent,
.ram-percent {
  text-align: center;
  color: #999;
  font-size: 0.9rem;
}

/* Location */
.location-info {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.location-coords {
  display: flex;
  gap: 2rem;
}

.coord-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.coord-label {
  font-weight: 600;
  color: #666;
  font-size: 0.9rem;
}

.coord-value {
  font-size: 1.1rem;
  color: #333;
  font-family: monospace;
}

.map-placeholder {
  height: 300px;
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  color: #999;
}

/* Activity Log */
.activity-log {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.log-item {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 1rem;
  background: #f9fafb;
  border-radius: 8px;
}

.log-icon {
  width: 32px;
  height: 32px;
  background: white;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
}

.log-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.log-time {
  font-size: 0.85rem;
  color: #999;
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
  animation: fadeIn 0.2s;
}

.modal-content {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  animation: slideUp 0.3s;
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
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
  color: #999;
  cursor: pointer;
  line-height: 1;
  padding: 0;
  width: 32px;
  height: 32px;
}

.modal-close:hover {
  color: #333;
}

.modal-body {
  padding: 1.5rem;
}

.form-group {
  margin-bottom: 1.5rem;
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
  transition: border-color 0.2s;
}

.form-control:focus {
  outline: none;
  border-color: #ff3636;
}

.common-apps {
  margin-top: 2rem;
}

.common-apps h3 {
  margin: 0 0 1rem 0;
  font-size: 1.1rem;
  color: #666;
}

.apps-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 0.75rem;
}

.app-btn {
  padding: 0.75rem;
  background: #f9fafb;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 500;
  transition: all 0.2s;
  color: #333;
}

.app-btn:hover {
  background: #f3f4f6;
  border-color: #d1d5db;
}

.app-btn.active {
  background: linear-gradient(135deg, #ff3636 0%, #ff5252 100%);
  border-color: #ff3636;
  color: white;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  padding: 1.5rem;
  border-top: 1px solid #e5e7eb;
}

.btn-cancel {
  padding: 0.75rem 1.5rem;
  background: #f3f4f6;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  color: #666;
  transition: background 0.2s;
}

.btn-cancel:hover {
  background: #e5e7eb;
}

.btn-primary {
  padding: 0.75rem 1.5rem;
  background: linear-gradient(135deg, #ff3636 0%, #ff5252 100%);
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  color: white;
  transition: transform 0.2s;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-info {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
}

.btn-info:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.log-item {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 1rem;
  background-color: #f9fafb;
  border-radius: 8px;
}

.log-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  flex-shrink: 0;
}

.log-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.log-content strong {
  color: #333;
}

.log-time {
  font-size: 0.85rem;
  color: #666;
}

/* Responsive */
@media (max-width: 768px) {
  .info-grid {
    grid-template-columns: 1fr;
  }

  .location-coords {
    flex-direction: column;
    gap: 1rem;
  }
}

/* Kiosk Mode Card */
.kiosk-card {
  margin-bottom: 2rem;
}

.kiosk-active-badge {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.kiosk-active {
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
  border: 2px solid #10b981;
  border-radius: 12px;
  padding: 1.5rem;
}

.kiosk-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 2rem;
}

.kiosk-status {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  flex: 1;
}

.kiosk-icon {
  font-size: 2.5rem;
}

.kiosk-details strong {
  display: block;
  font-size: 1.1rem;
  color: #047857;
  margin-bottom: 0.5rem;
}

.kiosk-details p {
  margin: 0.25rem 0;
  color: #065f46;
}

.kiosk-details code {
  background: #fff;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  color: #667eea;
  border: 1px solid #10b981;
}

.kiosk-hint {
  font-size: 0.9rem;
  font-style: italic;
  color: #059669 !important;
}

.kiosk-inactive {
  text-align: center;
  padding: 2rem;
}

/* Power Schedule Card Styles */
.schedule-card {
  margin-bottom: 2rem;
}

.schedule-active-badge {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  padding: 0.5rem 1rem;
  border-radius: 20px;
  font-size: 0.85rem;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
}

.schedule-active {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border: 2px solid #f59e0b;
  border-radius: 12px;
  padding: 1.5rem;
}

.schedule-info {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.schedule-times {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
}

.time-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  background: white;
  padding: 1rem;
  border-radius: 8px;
  border: 1px solid #f59e0b;
}

.time-icon {
  font-size: 2rem;
}

.time-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: #d97706;
  margin: 0;
}

.schedule-days {
  background: white;
  padding: 1rem;
  border-radius: 8px;
  border: 1px solid #f59e0b;
}

.schedule-days strong {
  display: block;
  color: #92400e;
  margin-bottom: 0.5rem;
}

.schedule-days p {
  margin: 0;
  color: #b45309;
  font-weight: 500;
}

.schedule-inactive {
  text-align: center;
  padding: 2rem;
}

.schedule-description {
  color: #6b7280;
  font-size: 1rem;
  margin-bottom: 1.5rem;
  line-height: 1.6;
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

.kiosk-description {
  color: #666;
  margin-bottom: 1.5rem;
  line-height: 1.6;
}

.kiosk-warning {
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
  flex-shrink: 0;
}

.warning-text strong {
  display: block;
  color: #92400e;
  margin-bottom: 0.25rem;
}

.warning-text p {
  margin: 0;
  color: #78350f;
  font-size: 0.9rem;
  line-height: 1.5;
}

.kiosk-features {
  margin-top: 1.5rem;
  padding: 1rem;
  background: #f9fafb;
  border-radius: 8px;
}

.kiosk-features h4 {
  margin: 0 0 1rem 0;
  color: #333;
  font-size: 0.95rem;
}

.kiosk-features ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 0.5rem;
}

.kiosk-features li {
  font-size: 0.9rem;
  color: #555;
  padding-left: 0.5rem;
}

/* Common Apps Grid for Kiosk */
.common-apps-section {
  margin-top: 1rem;
}

.common-apps-section h4 {
  margin: 0 0 0.75rem 0;
  font-size: 0.9rem;
  color: #555;
}

.common-app-btn.selected {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-color: #667eea;
}
</style>
