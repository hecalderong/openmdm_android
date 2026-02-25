<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useDevicesStore } from '@/stores/devices'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const devicesStore = useDevicesStore()

const isSidebarOpen = ref(true)

const toggleSidebar = () => {
  isSidebarOpen.value = !isSidebarOpen.value
}

const handleLogout = () => {
  authStore.logout()
  router.push({ name: 'login' })
}

onMounted(() => {
  devicesStore.setupWebSocket()
})

const menuItems = [
  { name: 'dashboard', label: 'Dashboard', icon: '📊' },
  { name: 'devices', label: 'Dispositivos', icon: '📱' },
  { name: 'apps', label: 'Aplicaciones', icon: '📦' },
  { name: 'users', label: 'Usuarios', icon: '👥' },
  { name: 'settings', label: 'Configuración', icon: '⚙️' }
]
</script>

<template>
  <div class="dashboard-layout">
    <!-- Sidebar -->
    <aside class="sidebar" :class="{ 'sidebar-collapsed': !isSidebarOpen }">
      <div class="sidebar-header">
        <img src="@/assets/logo.png" alt="Logo" class="sidebar-logo" />
        <h2 v-if="isSidebarOpen">MDM</h2>
      </div>

      <nav class="sidebar-nav">
        <RouterLink
          v-for="item in menuItems"
          :key="item.name"
          :to="{ name: item.name }"
          class="nav-item"
          :class="{ active: route.name === item.name }"
        >
          <span class="nav-icon">{{ item.icon }}</span>
          <span v-if="isSidebarOpen" class="nav-label">{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="sidebar-footer">
        <button @click="toggleSidebar" class="btn-toggle">
          <span v-if="isSidebarOpen">◀</span>
          <span v-else>▶</span>
        </button>
      </div>
    </aside>

    <!-- Main Content -->
    <div class="main-content">
      <!-- Top Navbar -->
      <header class="navbar">
        <div class="navbar-left">
          <button @click="toggleSidebar" class="btn-menu">☰</button>
          <h1 class="page-title">{{ route.meta.title || 'Dashboard' }}</h1>
        </div>

        <div class="navbar-right">
          <div class="stats-badge">
            <span class="badge badge-success">{{ devicesStore.stats.online }}</span>
            <span class="badge-label">En Línea</span>
          </div>

          <div class="stats-badge">
            <span class="badge badge-danger">{{ devicesStore.stats.offline }}</span>
            <span class="badge-label">Fuera de Línea</span>
          </div>

          <div class="user-menu">
            <div class="user-info">
              <span class="user-avatar">👤</span>
              <span class="user-name">{{ authStore.user?.name || 'Admin' }}</span>
            </div>
            <button @click="handleLogout" class="btn-logout">
              Cerrar Sesión
            </button>
          </div>
        </div>
      </header>

      <!-- Page Content -->
      <main class="page-content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.dashboard-layout {
  display: flex;
  min-height: 100vh;
  background-color: #f5f7fa;
}

/* Sidebar */
.sidebar {
  width: 260px;
  background: linear-gradient(180deg, #d45252 0%, #ff3636 100%);
  color: white;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  position: fixed;
  height: 100vh;
  z-index: 100;
}

.sidebar-collapsed {
  width: 70px;
}

.sidebar-header {
  padding: 1.5rem;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
}

.sidebar-logo {
  width: 120px;
  height: 50px;
  flex-shrink: 0;
}

.sidebar-header h2 {
  margin: 0;
  font-size: 1.5rem;
}

.sidebar-nav {
  flex: 1;
  padding: 1rem 0;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 1rem 1.5rem;
  color: rgba(255, 255, 255, 0.8);
  text-decoration: none;
  transition: all 0.3s;
  gap: 1rem;
}

.nav-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
  color: white;
}

.nav-item.active {
  background-color: rgba(255, 255, 255, 0.2);
  color: white;
  border-left: 4px solid white;
}

.nav-icon {
  font-size: 1.5rem;
  min-width: 1.5rem;
}

.nav-label {
  font-weight: 500;
}

.sidebar-footer {
  padding: 1rem;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.btn-toggle {
  width: 100%;
  padding: 0.75rem;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  color: white;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1.2rem;
  transition: background-color 0.3s;
}

.btn-toggle:hover {
  background: rgba(255, 255, 255, 0.2);
}

/* Main Content */
.main-content {
  flex: 1;
  margin-left: 260px;
  display: flex;
  flex-direction: column;
  transition: margin-left 0.3s ease;
}

.sidebar-collapsed + .main-content {
  margin-left: 70px;
}

/* Navbar */
.navbar {
  background: white;
  padding: 1rem 2rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 50;
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.btn-menu {
  display: none;
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #333;
}

.page-title {
  margin: 0;
  font-size: 1.5rem;
  color: #333;
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 2rem;
}

.stats-badge {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.badge {
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-weight: 600;
  font-size: 0.9rem;
}

.badge-success {
  background-color: #10b981;
  color: white;
}

.badge-danger {
  background-color: #ef4444;
  color: white;
}

.badge-label {
  font-size: 0.85rem;
  color: #666;
}

.user-menu {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.user-avatar {
  font-size: 1.5rem;
}

.user-name {
  font-weight: 500;
  color: #333;
}

.btn-logout {
  padding: 0.5rem 1rem;
  background-color: #ef4444;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: background-color 0.3s;
}

.btn-logout:hover {
  background-color: #dc2626;
}

/* Page Content */
.page-content {
  flex: 1;
  padding: 2rem;
}

/* Responsive */
@media (max-width: 768px) {
  .sidebar {
    transform: translateX(-100%);
  }

  .sidebar-collapsed {
    transform: translateX(0);
  }

  .main-content {
    margin-left: 0;
  }

  .btn-menu {
    display: block;
  }

  .stats-badge {
    display: none;
  }
}
</style>
