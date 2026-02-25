<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useSettingsStore } from '@/stores/settings'

const settingsStore = useSettingsStore()
const saveSuccess = ref(false)

const handleSave = async () => {
  const success = await settingsStore.saveSettings()
  if (success) {
    saveSuccess.value = true
    setTimeout(() => {
      saveSuccess.value = false
    }, 3000)
  }
}

const handleReset = () => {
  if (confirm('¿Estás seguro de que deseas restaurar la configuración predeterminada?')) {
    settingsStore.resetToDefaults()
  }
}

onMounted(() => {
  settingsStore.loadSettings()
})
</script>

<template>
  <div class="settings-view">
    <div class="page-header">
      <div>
        <h1>Configuración</h1>
        <p>Ajusta las preferencias del sistema MDM</p>
      </div>
      <div class="header-actions">
        <button @click="handleReset" class="btn-secondary">
          ♻️ Restaurar
        </button>
        <button @click="handleSave" class="btn-primary" :disabled="settingsStore.loading">
          💾 Guardar Cambios
        </button>
      </div>
    </div>

    <div v-if="saveSuccess" class="success-message">
      ✅ Configuración guardada correctamente
    </div>

    <div v-if="settingsStore.error" class="error-message">
      ❌ {{ settingsStore.error }}
    </div>

    <div class="settings-grid">
      <!-- General Settings -->
      <div class="card">
        <div class="card-header">
          <h2>⚙️ General</h2>
        </div>
        <div class="card-body">
          <div class="form-group">
            <label>Nombre del Sistema</label>
            <input
              v-model="settingsStore.settings.general.systemName"
              type="text"
              class="form-input"
            />
          </div>
          
          
        </div>
      </div>

      <!-- Notifications Settings -->
      <div class="card">
        <div class="card-header">
          <h2>🔔 Notificaciones</h2>
        </div>
        <div class="card-body">
          <div class="form-group-checkbox">
            <label class="checkbox-label">
              <input v-model="settingsStore.settings.notifications.deviceOffline" type="checkbox" />
              <span>Notificar cuando un dispositivo se desconecte</span>
            </label>
          </div>
          <div class="form-group-checkbox">
            <label class="checkbox-label">
              <input v-model="settingsStore.settings.notifications.lowBattery" type="checkbox" />
              <span>Notificar cuando la batería sea baja (&lt;20%)</span>
            </label>
          </div>
          <div class="form-group-checkbox">
            <label class="checkbox-label">
              <input v-model="settingsStore.settings.notifications.appInstallation" type="checkbox" />
              <span>Notificar instalación/desinstalación de apps</span>
            </label>
          </div>
          <div class="form-group-checkbox">
            <label class="checkbox-label">
              <input v-model="settingsStore.settings.notifications.emailNotifications" type="checkbox" />
              <span>Enviar notificaciones por correo electrónico</span>
            </label>
          </div>
        </div>
      </div>

      <!-- Security Settings -->
      <div class="card">
        <div class="card-header">
          <h2>🔒 Seguridad</h2>
        </div>
        <div class="card-body">
          <div class="form-group">
            <label>Tiempo de sesión (minutos)</label>
            <input
              v-model.number="settingsStore.settings.security.sessionTimeout"
              type="number"
              min="5"
              max="120"
              class="form-input"
            />
            <small class="form-help">Tiempo antes de cerrar sesión automáticamente por inactividad</small>
          </div>
          <div class="form-group">
            <label>Longitud mínima de contraseña</label>
            <input
              v-model.number="settingsStore.settings.security.passwordMinLength"
              type="number"
              min="6"
              max="20"
              class="form-input"
            />
          </div>
          <div class="form-group-checkbox">
            <label class="checkbox-label">
              <input v-model="settingsStore.settings.security.requireStrongPassword" type="checkbox" />
              <span>Requerir contraseñas seguras (mayúsculas, números y símbolos)</span>
            </label>
          </div>
          <div class="form-group-checkbox">
            <label class="checkbox-label">
              <input v-model="settingsStore.settings.security.twoFactorAuth" type="checkbox" />
              <span>Autenticación de dos factores (2FA)</span>
            </label>
          </div>
        </div>
      </div>

      <!-- Device Settings -->
      <div class="card">
        <div class="card-header">
          <h2>📱 Dispositivos</h2>
        </div>
        <div class="card-body">
          <div class="form-group-checkbox">
            <label class="checkbox-label">
              <input v-model="settingsStore.settings.devices.autoLockInactive" type="checkbox" />
              <span>Bloquear automáticamente dispositivos inactivos</span>
            </label>
          </div>
          <div class="form-group">
            <label>Umbral de inactividad (horas)</label>
            <input
              v-model.number="settingsStore.settings.devices.inactiveThreshold"
              type="number"
              min="1"
              max="168"
              class="form-input"
              :disabled="!settingsStore.settings.devices.autoLockInactive"
            />
            <small class="form-help">Tiempo sin actividad antes de bloquear automáticamente</small>
          </div>
          <div class="form-group">
            <label>Intervalo de heartbeat (segundos)</label>
            <input
              v-model.number="settingsStore.settings.devices.heartbeatInterval"
              type="number"
              min="30"
              max="300"
              class="form-input"
            />
            <small class="form-help">Frecuencia con la que los dispositivos envían señales de vida</small>
          </div>
          <div class="form-group-checkbox">
            <label class="checkbox-label">
              <input v-model="settingsStore.settings.devices.autoUpdateApps" type="checkbox" />
              <span>Actualizar aplicaciones automáticamente</span>
            </label>
          </div>
        </div>
      </div>
    </div>

    <!-- System Info Card -->
    <div class="card system-info">
      <div class="card-header">
        <h2>ℹ️ Información del Sistema</h2>
      </div>
      <div class="card-body">
        <div class="info-row">
          <span class="info-label">Versión:</span>
          <span class="info-value">1.0.0</span>
        </div>
        <div class="info-row">
          <span class="info-label">Última actualización:</span>
          <span class="info-value">27 de noviembre de 2025</span>
        </div>
        <div class="info-row">
          <span class="info-label">Estado del sistema:</span>
          <span class="info-value status-online">✓ Operativo</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.settings-view {
  animation: fadeIn 0.5s;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
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

.header-actions {
  display: flex;
  gap: 1rem;
}

.btn-primary, .btn-secondary {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.btn-primary {
  background: linear-gradient(135deg, #df4b4b 0%, #ff3636 100%);
  color: white;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 54, 54, 0.4);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  background: #f3f4f6;
  color: #333;
}

.btn-secondary:hover {
  background-color: #e5e7eb;
  transform: translateY(-2px);
}

.success-message, .error-message {
  padding: 1rem;
  border-radius: 8px;
  margin-bottom: 2rem;
  font-weight: 600;
  animation: slideDown 0.3s;
}

@keyframes slideDown {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

.success-message {
  background: #d1fae5;
  color: #065f46;
  border-left: 4px solid #10b981;
}

.error-message {
  background: #fee2e2;
  color: #991b1b;
  border-left: 4px solid #ef4444;
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 2rem;
  margin-bottom: 2rem;
}

.card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.card-header {
  background: linear-gradient(135deg, #df4b4b 0%, #ff3636 100%);
  color: white;
  padding: 1.25rem 1.5rem;
}

.card-header h2 {
  margin: 0;
  font-size: 1.25rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.card-body {
  padding: 1.5rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #333;
}

.form-input, .form-select {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 1rem;
  transition: border-color 0.2s;
}

.form-input:focus, .form-select:focus {
  outline: none;
  border-color: #ff3636;
}

.form-input:disabled {
  background-color: #f3f4f6;
  cursor: not-allowed;
  opacity: 0.6;
}

.form-help {
  display: block;
  margin-top: 0.5rem;
  font-size: 0.85rem;
  color: #666;
}

.form-group-checkbox {
  margin-bottom: 1rem;
}

.form-group-checkbox:last-child {
  margin-bottom: 0;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  cursor: pointer;
  padding: 0.5rem;
  border-radius: 6px;
  transition: background-color 0.2s;
}

.checkbox-label:hover {
  background-color: #f9fafb;
}

.checkbox-label input[type="checkbox"] {
  width: 1.25rem;
  height: 1.25rem;
  cursor: pointer;
  flex-shrink: 0;
}

.checkbox-label span {
  color: #333;
  font-size: 0.95rem;
}

.system-info {
  max-width: 600px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 0.75rem 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  font-weight: 600;
  color: #666;
}

.info-value {
  color: #333;
}

.status-online {
  color: #10b981;
  font-weight: 600;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 1rem;
  }

  .header-actions {
    width: 100%;
    flex-direction: column;
  }

  .header-actions button {
    width: 100%;
  }

  .settings-grid {
    grid-template-columns: 1fr;
  }
}
</style>
