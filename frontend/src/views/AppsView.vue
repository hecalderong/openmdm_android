<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAppsStore } from '@/stores/apps'
import { useDevicesStore } from '@/stores/devices'

const router = useRouter()
const appsStore = useAppsStore()
const devicesStore = useDevicesStore()

const isLoading = ref(true)
const showUploadModal = ref(false)
const showInstallModal = ref(false)
const showMdmUpdateModal = ref(false)
const selectedApp = ref<string | null>(null)
const selectedDevices = ref<string[]>([])

const uploadFile = ref<File | null>(null)
const uploadProgress = ref(0)
const isUploading = ref(false)

const mdmUpdateFile = ref<File | null>(null)
const mdmUpdateProgress = ref(0)
const isUpdatingMdm = ref(false)
const mdmSelectedDevices = ref<string[]>([])

onMounted(async () => {
  await Promise.all([
    appsStore.fetchApps(),
    devicesStore.fetchDevices()
  ])
  isLoading.value = false
})

const onlineDevices = computed(() => {
  return devicesStore.devices.filter((d) => d.status === 'online')
})

const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files && target.files[0]) {
    uploadFile.value = target.files[0]
  }
}

const handleUpload = async () => {
  if (!uploadFile.value) {
    alert('❌ Por favor selecciona un archivo APK')
    return
  }

  isUploading.value = true
  uploadProgress.value = 0

  try {
    await appsStore.uploadApp(
      uploadFile.value,
      {}, // Metadatos se extraerán automáticamente del APK
      (progress: number) => {
        uploadProgress.value = progress
      }
    )

    alert('✅ Aplicación subida exitosamente (metadatos extraídos automáticamente)')
    showUploadModal.value = false
    uploadFile.value = null
    uploadProgress.value = 0
  } catch (error: any) {
    alert('❌ Error al subir la aplicación: ' + (error.response?.data?.message || error.message))
  } finally {
    isUploading.value = false
  }
}

const openInstallModal = (appId: string) => {
  selectedApp.value = appId
  selectedDevices.value = []
  showInstallModal.value = true
}

const toggleDeviceSelection = (deviceId: string) => {
  const index = selectedDevices.value.indexOf(deviceId)
  if (index > -1) {
    selectedDevices.value.splice(index, 1)
  } else {
    selectedDevices.value.push(deviceId)
  }
}

const selectAllDevices = () => {
  if (selectedDevices.value.length === onlineDevices.value.length) {
    selectedDevices.value = []
  } else {
    selectedDevices.value = onlineDevices.value.map((d) => d.id)
  }
}

const handleInstall = async () => {
  if (!selectedApp.value || selectedDevices.value.length === 0) return

  await appsStore.installAppMultiple(selectedApp.value, selectedDevices.value)
  showInstallModal.value = false
  selectedApp.value = null
  selectedDevices.value = []
}

const handleUninstall = async (appId: string) => {
  if (confirm('¿Estás seguro de que deseas desinstalar esta aplicación del sistema?')) {
    await appsStore.deleteApp(appId)
  }
}

const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

const handleMdmFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files && target.files[0]) {
    mdmUpdateFile.value = target.files[0]
  }
}

const toggleMdmDeviceSelection = (deviceId: string) => {
  const index = mdmSelectedDevices.value.indexOf(deviceId)
  if (index > -1) {
    mdmSelectedDevices.value.splice(index, 1)
  } else {
    mdmSelectedDevices.value.push(deviceId)
  }
}

const selectAllMdmDevices = () => {
  if (mdmSelectedDevices.value.length === onlineDevices.value.length) {
    mdmSelectedDevices.value = []
  } else {
    mdmSelectedDevices.value = onlineDevices.value.map((d) => d.id)
  }
}

const handleMdmUpdate = async () => {
  if (!mdmUpdateFile.value || mdmSelectedDevices.value.length === 0) return

  isUpdatingMdm.value = true
  mdmUpdateProgress.value = 0

  await appsStore.updateMdmApp(mdmUpdateFile.value, mdmSelectedDevices.value, (progress: number) => {
    mdmUpdateProgress.value = progress
  })

  isUpdatingMdm.value = false
  showMdmUpdateModal.value = false
  mdmUpdateFile.value = null
  mdmSelectedDevices.value = []
  mdmUpdateProgress.value = 0
}
</script>

<template>
  <div class="apps-view">
    <!-- Header -->
    <div class="page-header">
      <div>
        <h1>Aplicaciones</h1>
        <p>Gestiona e instala aplicaciones APK en los dispositivos</p>
      </div>
      <div class="header-actions">
        <button @click="showMdmUpdateModal = true" class="btn-secondary">
          🔄 Actualizar MDM
        </button>
        <button @click="showUploadModal = true" class="btn-primary">
          📤 Subir APK
        </button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="loading">
      <div class="spinner"></div>
      <p>Cargando aplicaciones...</p>
    </div>

    <!-- Apps Grid -->
    <div v-else-if="appsStore.apps.length > 0" class="apps-grid">
      <div v-for="app in appsStore.apps" :key="app.id" class="app-card">
        <div class="app-icon">📦</div>
        <div class="app-info">
          <h3>{{ app.name }}</h3>
          <p class="app-package">{{ app.packageName }}</p>
          <div class="app-meta">
            <span class="app-version">v{{ app.version }}</span>
            <span class="app-size">{{ formatFileSize(app.fileSize) }}</span>
          </div>
        </div>
        <div class="app-actions">
          <button @click="openInstallModal(app.id)" class="btn btn-primary">
            Instalar
          </button>
          <button @click="handleUninstall(app.id)" class="btn btn-danger">
            Eliminar
          </button>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="empty-state">
      <div class="empty-icon">📦</div>
      <h3>No hay aplicaciones registradas</h3>
      <p>Sube tu primera aplicación APK para comenzar</p>
      <button @click="showUploadModal = true" class="btn-primary">
        📤 Subir APK
      </button>
    </div>

    <!-- Upload Modal -->
    <div v-if="showUploadModal" class="modal-overlay" @click="showUploadModal = false">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h2>Subir Aplicación APK</h2>
          <button @click="showUploadModal = false" class="btn-close">✕</button>
        </div>
        <div class="modal-body">
          <div class="file-upload">
            <input
              type="file"
              accept=".apk"
              @change="handleFileSelect"
              id="file-input"
              class="file-input"
            />
            <label for="file-input" class="file-label">
              <div class="file-icon">📁</div>
              <div v-if="uploadFile">
                <strong>{{ uploadFile.name }}</strong>
                <p>{{ formatFileSize(uploadFile.size) }}</p>
              </div>
              <div v-else>
                <strong>Seleccionar archivo APK</strong>
                <p>Arrastra o haz clic para seleccionar</p>
              </div>
            </label>
          </div>

          <div class="info-message">
            ℹ️ Los metadatos (nombre, package, versión) se extraerán automáticamente del APK
          </div>

          <div v-if="isUploading" class="upload-progress">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: `${uploadProgress}%` }"></div>
            </div>
            <p>Subiendo... {{ uploadProgress }}%</p>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="showUploadModal = false" class="btn btn-secondary">
            Cancelar
          </button>
          <button
            @click="handleUpload"
            class="btn btn-primary"
            :disabled="!uploadFile || isUploading"
          >
            Subir
          </button>
        </div>
      </div>
    </div>

    <!-- Install Modal -->
    <div v-if="showInstallModal" class="modal-overlay" @click="showInstallModal = false">
      <div class="modal modal-large" @click.stop>
        <div class="modal-header">
          <h2>Instalar en Dispositivos</h2>
          <button @click="showInstallModal = false" class="btn-close">✕</button>
        </div>
        <div class="modal-body">
          <div class="devices-selection">
            <div class="selection-header">
              <label class="checkbox-label">
                <input
                  type="checkbox"
                  :checked="selectedDevices.length === onlineDevices.length"
                  @change="selectAllDevices"
                />
                <span>Seleccionar todos ({{ onlineDevices.length }} dispositivos)</span>
              </label>
            </div>

            <div v-if="onlineDevices.length === 0" class="empty-state-small">
              <p>No hay dispositivos en línea disponibles</p>
            </div>

            <div v-else class="devices-list">
              <label
                v-for="device in onlineDevices"
                :key="device.id"
                class="device-checkbox"
              >
                <input
                  type="checkbox"
                  :checked="selectedDevices.includes(device.id)"
                  @change="toggleDeviceSelection(device.id)"
                />
                <div class="device-info">
                  <span class="device-icon">📱</span>
                  <div>
                    <strong>{{ device.name }}</strong>
                    <div class="device-id">{{ device.deviceId }}</div>
                  </div>
                </div>
                <span class="battery-mini">🔋 {{ device.batteryLevel }}%</span>
              </label>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="showInstallModal = false" class="btn btn-secondary">
            Cancelar
          </button>
          <button
            @click="handleInstall"
            class="btn btn-primary"
            :disabled="selectedDevices.length === 0"
          >
            Instalar en {{ selectedDevices.length }} dispositivo(s)
          </button>
        </div>
      </div>
    </div>

    <!-- MDM Update Modal -->
    <div v-if="showMdmUpdateModal" class="modal-overlay" @click="showMdmUpdateModal = false">
      <div class="modal modal-large" @click.stop>
        <div class="modal-header">
          <h2>Actualizar MDM en Dispositivos</h2>
          <button @click="showMdmUpdateModal = false" class="btn-close">✕</button>
        </div>
        <div class="modal-body">
          <div class="file-upload">
            <input
              type="file"
              accept=".apk"
              @change="handleMdmFileSelect"
              id="mdm-file-input"
              class="file-input"
            />
            <label for="mdm-file-input" class="file-label">
              <div class="file-icon">📁</div>
              <div v-if="mdmUpdateFile">
                <strong>{{ mdmUpdateFile.name }}</strong>
                <p>{{ formatFileSize(mdmUpdateFile.size) }}</p>
              </div>
              <div v-else>
                <strong>Seleccionar APK del MDM</strong>
                <p>Arrastra o haz clic para seleccionar</p>
              </div>
            </label>
          </div>

          <div v-if="isUpdatingMdm" class="upload-progress">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: `${mdmUpdateProgress}%` }"></div>
            </div>
            <p>Actualizando... {{ mdmUpdateProgress }}%</p>
          </div>

          <div class="devices-selection" style="margin-top: 1.5rem;">
            <div class="selection-header">
              <label class="checkbox-label">
                <input
                  type="checkbox"
                  :checked="mdmSelectedDevices.length === onlineDevices.length"
                  @change="selectAllMdmDevices"
                />
                <span>Seleccionar todos ({{ onlineDevices.length }} dispositivos)</span>
              </label>
            </div>

            <div v-if="onlineDevices.length === 0" class="empty-state-small">
              <p>No hay dispositivos en línea disponibles</p>
            </div>

            <div v-else class="devices-list">
              <label
                v-for="device in onlineDevices"
                :key="device.id"
                class="device-checkbox"
              >
                <input
                  type="checkbox"
                  :checked="mdmSelectedDevices.includes(device.id)"
                  @change="toggleMdmDeviceSelection(device.id)"
                />
                <div class="device-info">
                  <span class="device-icon">📱</span>
                  <div>
                    <strong>{{ device.name }}</strong>
                    <div class="device-id">{{ device.deviceId }}</div>
                  </div>
                </div>
                <span class="battery-mini">🔋 {{ device.batteryLevel }}%</span>
              </label>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="showMdmUpdateModal = false" class="btn btn-secondary">
            Cancelar
          </button>
          <button
            @click="handleMdmUpdate"
            class="btn btn-primary"
            :disabled="!mdmUpdateFile || mdmSelectedDevices.length === 0 || isUpdatingMdm"
          >
            Actualizar en {{ mdmSelectedDevices.length }} dispositivo(s)
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.apps-view {
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

.btn-primary {
  padding: 0.75rem 1.5rem;
  background: linear-gradient(135deg, #df4b4b 0%, #ff3636 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 54, 54, 0.4);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-secondary {
  padding: 0.75rem 1.5rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.btn-secondary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.btn-secondary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
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

/* Apps Grid */
.apps-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
}

.app-card {
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  gap: 1rem;
  transition: transform 0.2s, box-shadow 0.2s;
}

.app-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.app-icon {
  font-size: 3rem;
  text-align: center;
}

.app-info {
  flex: 1;
}

.app-info h3 {
  margin: 0 0 0.5rem 0;
  font-size: 1.25rem;
  color: #333;
}

.app-package {
  margin: 0 0 0.75rem 0;
  font-size: 0.85rem;
  color: #666;
  font-family: monospace;
}

.app-meta {
  display: flex;
  gap: 1rem;
  font-size: 0.85rem;
}

.app-version {
  color: #667eea;
  font-weight: 600;
}

.app-size {
  color: #999;
}

.app-actions {
  display: flex;
  gap: 0.5rem;
}

.btn {
  flex: 1;
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s;
}

.btn:hover {
  transform: translateY(-2px);
}

.btn-danger {
  background-color: #ef4444;
  color: white;
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
  margin: 0 0 1.5rem 0;
  color: #666;
}

/* Modal */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.modal {
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
  width: 100%;
  max-width: 500px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}

.modal-large {
  max-width: 700px;
}

.modal-header {
  padding: 1.5rem;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h2 {
  margin: 0;
  font-size: 1.5rem;
  color: #333;
}

.btn-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #999;
  padding: 0;
  width: 32px;
  height: 32px;
}

.btn-close:hover {
  color: #333;
}

.modal-body {
  padding: 1.5rem;
  overflow-y: auto;
  flex: 1;
}

.modal-footer {
  padding: 1.5rem;
  border-top: 1px solid #e5e7eb;
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
}

.btn-secondary {
  padding: 0.75rem 1.5rem;
  background-color: #e5e7eb;
  color: #333;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
}

/* File Upload */
.file-upload {
  margin-bottom: 1.5rem;
}

.file-input {
  display: none;
}

.file-label {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  border: 2px dashed #ccc;
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.3s, background-color 0.3s;
  text-align: center;
}

.file-label:hover {
  border-color: #667eea;
  background-color: #f9fafb;
}

.file-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.info-message {
  padding: 1rem;
  background-color: #e0f2fe;
  border-left: 4px solid #0284c7;
  border-radius: 4px;
  color: #0c4a6e;
  font-size: 0.9rem;
  margin-bottom: 1rem;
}

.upload-progress {
  margin-top: 1rem;
}

.progress-bar {
  width: 100%;
  height: 30px;
  background-color: #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 0.5rem;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #df4b4b 0%, #ff3636 100%);
  transition: width 0.3s;
}

/* Devices Selection */
.devices-selection {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.selection-header {
  padding-bottom: 1rem;
  border-bottom: 1px solid #e5e7eb;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  font-weight: 600;
}

.checkbox-label input[type="checkbox"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.devices-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-height: 400px;
  overflow-y: auto;
}

.device-checkbox {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.device-checkbox:hover {
  background-color: #f9fafb;
}

.device-checkbox input[type="checkbox"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.device-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.device-icon {
  font-size: 1.2rem;
}

.device-id {
  font-size: 0.75rem;
  color: #666;
}

.battery-mini {
  font-size: 0.85rem;
  color: #666;
}

.empty-state-small {
  text-align: center;
  padding: 2rem;
  color: #999;
}

/* Responsive */
@media (max-width: 768px) {
  .apps-grid {
    grid-template-columns: 1fr;
  }

  .page-header {
    flex-direction: column;
    gap: 1rem;
  }

  .page-header .btn-primary {
    width: 100%;
  }
}
</style>
