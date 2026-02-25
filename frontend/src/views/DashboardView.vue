<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useDevicesStore } from '@/stores/devices'
import { useAppsStore } from '@/stores/apps'

const devicesStore = useDevicesStore()
const appsStore = useAppsStore()

const isLoading = ref(true)

onMounted(async () => {
  await Promise.all([
    devicesStore.fetchDevices(),
    devicesStore.fetchStats(),
    appsStore.fetchApps()
  ])
  isLoading.value = false
})

const recentDevices = computed(() => {
  return devicesStore.devices.slice(0, 5)
})

const deviceStatusDistribution = computed(() => {
  return [
    { label: 'En Línea', value: devicesStore.stats.online, color: '#10b981' },
    { label: 'Fuera de Línea', value: devicesStore.stats.offline, color: '#ef4444' },
    { label: 'Bloqueados', value: devicesStore.stats.locked, color: '#f59e0b' }
  ]
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

const formatDate = (date: string) => {
  return new Date(date).toLocaleString('es-ES', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<template>
  <div class="dashboard">
    <div v-if="isLoading" class="loading">
      <div class="spinner"></div>
      <p>Cargando información...</p>
    </div>

    <div v-else>
      <!-- Stats Cards -->
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon stat-icon-primary">📱</div>
          <div class="stat-content">
            <h3>{{ devicesStore.stats.total }}</h3>
            <p>Dispositivos Totales</p>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon stat-icon-success">✓</div>
          <div class="stat-content">
            <h3>{{ devicesStore.stats.online }}</h3>
            <p>En Línea</p>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon stat-icon-danger">✗</div>
          <div class="stat-content">
            <h3>{{ devicesStore.stats.offline }}</h3>
            <p>Fuera de Línea</p>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon stat-icon-warning">🔒</div>
          <div class="stat-content">
            <h3>{{ devicesStore.stats.locked }}</h3>
            <p>Bloqueados</p>
          </div>
        </div>
      </div>

      <!-- Charts and Recent Activity -->
      <div class="dashboard-grid">
        <!-- Device Status Chart -->
        <div class="card">
          <div class="card-header">
            <h2>Estado de Dispositivos</h2>
          </div>
          <div class="card-body">
            <div class="chart-container">
              <div
                v-for="item in deviceStatusDistribution"
                :key="item.label"
                class="chart-bar"
              >
                <div class="chart-label">{{ item.label }}</div>
                <div class="chart-progress">
                  <div
                    class="chart-progress-bar"
                    :style="{
                      width: `${(item.value / devicesStore.stats.total) * 100}%`,
                      backgroundColor: item.color
                    }"
                  ></div>
                </div>
                <div class="chart-value">{{ item.value }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Recent Devices -->
        <div class="card">
          <div class="card-header">
            <h2>Dispositivos Recientes</h2>
            <RouterLink :to="{ name: 'devices' }" class="link-primary">
              Ver todos →
            </RouterLink>
          </div>
          <div class="card-body">
            <div v-if="recentDevices.length === 0" class="empty-state">
              <p>No hay dispositivos registrados</p>
            </div>
            <div v-else class="devices-list">
              <div
                v-for="device in recentDevices"
                :key="device.id"
                class="device-item"
              >
                <div class="device-info">
                  <div class="device-name">
                    <span class="device-icon">📱</span>
                    <strong>{{ device.name }}</strong>
                  </div>
                  <div class="device-meta">
                    <span class="device-id">{{ device.deviceId }}</span>
                    <span :class="['status-badge', getStatusClass(device.status)]">
                      {{ getStatusLabel(device.status) }}
                    </span>
                  </div>
                </div>
                <div class="device-stats">
                  <div class="device-stat">
                    <span class="stat-label">Batería</span>
                    <span class="stat-value">{{ device.batteryLevel || 0 }}%</span>
                  </div>
                  <div class="device-stat">
                    <span class="stat-label">Última conexión</span>
                    <span class="stat-value">{{ device.lastSeen ? formatDate(device.lastSeen.toString()) : 'Nunca' }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Applications Summary -->
      <div class="card">
        <div class="card-header">
          <h2>Aplicaciones Instaladas</h2>
          <RouterLink :to="{ name: 'apps' }" class="link-primary">
            Gestionar →
          </RouterLink>
        </div>
        <div class="card-body">
          <div class="apps-grid">
            <div v-for="app in appsStore.apps.slice(0, 6)" :key="app.id" class="app-card">
              <div class="app-icon">📦</div>
              <div class="app-info">
                <h4>{{ app.name }}</h4>
                <p>{{ app.packageName }}</p>
                <span class="app-version">v{{ app.version }}</span>
              </div>
            </div>
            <div v-if="appsStore.apps.length === 0" class="empty-state">
              <p>No hay aplicaciones registradas</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
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

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
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

/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.stat-card {
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 1rem;
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.8rem;
}

.stat-icon-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon-success {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}

.stat-icon-danger {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
}

.stat-icon-warning {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}

.stat-content h3 {
  margin: 0 0 0.25rem 0;
  font-size: 2rem;
  color: #333;
}

.stat-content p {
  margin: 0;
  color: #666;
  font-size: 0.9rem;
}

/* Dashboard Grid */
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

/* Card */
.card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.card-header {
  padding: 1.5rem;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  font-size: 1.25rem;
  color: #333;
}

.link-primary {
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.3s;
}

.link-primary:hover {
  color: #764ba2;
}

.card-body {
  padding: 1.5rem;
}

/* Chart */
.chart-container {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.chart-bar {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.chart-label {
  min-width: 120px;
  font-weight: 500;
  color: #333;
}

.chart-progress {
  flex: 1;
  height: 30px;
  background-color: #f3f4f6;
  border-radius: 6px;
  overflow: hidden;
}

.chart-progress-bar {
  height: 100%;
  transition: width 0.5s ease;
}

.chart-value {
  min-width: 40px;
  text-align: right;
  font-weight: 600;
  color: #333;
}

/* Devices List */
.devices-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.device-item {
  padding: 1rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.device-item:hover {
  background-color: #f9fafb;
}

.device-info {
  margin-bottom: 0.75rem;
}

.device-name {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
  font-size: 1.05rem;
}

.device-icon {
  font-size: 1.2rem;
}

.device-meta {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.device-id {
  color: #666;
  font-size: 0.85rem;
}

.status-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
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

.device-stats {
  display: flex;
  gap: 2rem;
}

.device-stat {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.stat-label {
  font-size: 0.75rem;
  color: #666;
}

.stat-value {
  font-weight: 600;
  color: #333;
  font-size: 0.9rem;
}

/* Apps Grid */
.apps-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
}

.app-card {
  padding: 1rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 1rem;
  transition: background-color 0.2s;
}

.app-card:hover {
  background-color: #f9fafb;
}

.app-icon {
  font-size: 2rem;
}

.app-info h4 {
  margin: 0 0 0.25rem 0;
  font-size: 1rem;
  color: #333;
}

.app-info p {
  margin: 0 0 0.25rem 0;
  font-size: 0.75rem;
  color: #666;
}

.app-version {
  font-size: 0.7rem;
  color: #999;
  font-weight: 500;
}

.empty-state {
  text-align: center;
  padding: 2rem;
  color: #999;
}

/* Responsive */
@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .apps-grid {
    grid-template-columns: 1fr;
  }
}
</style>
