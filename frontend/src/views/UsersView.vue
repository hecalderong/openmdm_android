<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUsersStore, type CreateUserDto, type UpdateUserDto } from '@/stores/users'

const usersStore = useUsersStore()

const showModal = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const selectedUserId = ref<string | null>(null)

const formData = ref({
  email: '',
  name: '',
  password: '',
  role: 'operator' as 'admin' | 'operator' | 'viewer',
  isActive: true
})

const getRoleLabel = (role: string) => {
  const labels: Record<string, string> = {
    admin: 'Administrador',
    operator: 'Operador',
    viewer: 'Visualizador'
  }
  return labels[role] || role
}

const getRoleClass = (role: string) => {
  const classes: Record<string, string> = {
    admin: 'role-admin',
    operator: 'role-supervisor',
    viewer: 'role-user'
  }
  return classes[role] || ''
}

const openCreateModal = () => {
  modalMode.value = 'create'
  formData.value = {
    email: '',
    name: '',
    password: '',
    role: 'operator',
    isActive: true
  }
  showModal.value = true
}

const openEditModal = (user: any) => {
  modalMode.value = 'edit'
  selectedUserId.value = user.id
  formData.value = {
    email: user.email,
    name: user.name,
    password: '',
    role: user.role,
    isActive: user.isActive
  }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  selectedUserId.value = null
}

const handleSubmit = async () => {
  if (modalMode.value === 'create') {
    const success = await usersStore.createUser(formData.value as CreateUserDto)
    if (success) {
      closeModal()
      await usersStore.fetchUsers()
    }
  } else if (selectedUserId.value) {
    const updateData: UpdateUserDto = {
      email: formData.value.email,
      name: formData.value.name,
      role: formData.value.role,
      isActive: formData.value.isActive
    }
    if (formData.value.password) {
      updateData.password = formData.value.password
    }
    const success = await usersStore.updateUser(selectedUserId.value, updateData)
    if (success) {
      closeModal()
      await usersStore.fetchUsers()
    }
  }
}

const handleDelete = async (userId: string) => {
  if (confirm('¿Estás seguro de que deseas eliminar este usuario?')) {
    await usersStore.deleteUser(userId)
  }
}

const handleToggleStatus = async (userId: string) => {
  await usersStore.toggleUserStatus(userId)
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('es-MX', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

onMounted(() => {
  usersStore.fetchUsers()
})
</script>

<template>
  <div class="users-view">
    <div class="page-header">
      <div>
        <h1>Usuarios</h1>
        <p>Gestiona los usuarios y permisos del sistema MDM</p>
      </div>
      <button class="btn-primary" @click="openCreateModal">
        ➕ Agregar Usuario
      </button>
    </div>

    <div v-if="usersStore.loading" class="loading">
      <p>Cargando usuarios...</p>
    </div>

    <div v-else-if="usersStore.error" class="error-message">
      <p>❌ {{ usersStore.error }}</p>
    </div>

    <div v-else class="users-table-container">
      <table class="users-table">
        <thead>
          <tr>
            <th>Usuario</th>
            <th>Correo Electrónico</th>
            <th>Rol</th>
            <th>Estado</th>
            <th>Fecha Creación</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in usersStore.users" :key="user.id">
            <td>
              <div class="user-cell">
                <span class="user-avatar">👤</span>
                <strong>{{ user.name }}</strong>
              </div>
            </td>
            <td>{{ user.email }}</td>
            <td>
              <span :class="['role-badge', getRoleClass(user.role)]">
                {{ getRoleLabel(user.role) }}
              </span>
            </td>
            <td>
              <button 
                :class="['status-badge', user.isActive ? 'status-active' : 'status-inactive']"
                @click="handleToggleStatus(user.id)"
                :title="user.isActive ? 'Desactivar' : 'Activar'"
              >
                {{ user.isActive ? 'Activo' : 'Inactivo' }}
              </button>
            </td>
            <td>{{ formatDate(user.createdAt) }}</td>
            <td>
              <div class="actions">
                <button class="btn btn-primary" @click="openEditModal(user)" title="Editar">✏️</button>
                <button class="btn btn-danger" @click="handleDelete(user.id)" title="Eliminar">🗑️</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="usersStore.users.length === 0" class="empty-state">
        <p>No hay usuarios registrados</p>
      </div>
    </div>

    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal">
        <div class="modal-header">
          <h2>{{ modalMode === 'create' ? 'Crear Usuario' : 'Editar Usuario' }}</h2>
          <button class="btn-close" @click="closeModal">✕</button>
        </div>
        <form @submit.prevent="handleSubmit" class="modal-body">
          <div class="form-group">
            <label for="name">Nombre Completo</label>
            <input id="name" v-model="formData.name" type="text" class="form-input" required />
          </div>

          <div class="form-group">
            <label for="email">Correo Electrónico</label>
            <input id="email" v-model="formData.email" type="email" class="form-input" required />
          </div>

          <div class="form-group">
            <label for="password">{{ modalMode === 'create' ? 'Contraseña' : 'Nueva Contraseña (opcional)' }}</label>
            <input
              id="password"
              v-model="formData.password"
              type="password"
              class="form-input"
              :required="modalMode === 'create'"
              placeholder="Mínimo 8 caracteres"
            />
          </div>

          <div class="form-group">
            <label for="role">Rol</label>
            <select id="role" v-model="formData.role" class="form-select" required>
              <option value="admin">Administrador</option>
              <option value="operator">Operador</option>
              <option value="viewer">Visualizador</option>
            </select>
          </div>

          <div v-if="modalMode === 'edit'" class="form-group-checkbox">
            <label class="checkbox-label">
              <input v-model="formData.isActive" type="checkbox" />
              <span>Usuario activo</span>
            </label>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn-secondary" @click="closeModal">Cancelar</button>
            <button type="submit" class="btn-primary" :disabled="usersStore.loading">
              {{ usersStore.loading ? 'Guardando...' : 'Guardar' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.users-view {
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

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 54, 54, 0.4);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading, .error-message {
  background: white;
  padding: 2rem;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.error-message {
  background: #fee2e2;
  color: #991b1b;
}

.users-table-container {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.users-table {
  width: 100%;
  border-collapse: collapse;
}

.users-table thead {
  background-color: #f9fafb;
}

.users-table th {
  padding: 1rem;
  text-align: left;
  font-weight: 600;
  color: #333;
  border-bottom: 2px solid #e5e7eb;
}

.users-table tbody tr {
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.2s;
}

.users-table tbody tr:hover {
  background-color: #f9fafb;
}

.users-table td {
  padding: 1rem;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.user-avatar {
  font-size: 1.5rem;
}

.role-badge {
  padding: 0.35rem 0.85rem;
  border-radius: 12px;
  font-size: 0.8rem;
  font-weight: 600;
  display: inline-block;
}

.role-admin {
  background-color: #dbeafe;
  color: #1e40af;
}

.role-supervisor {
  background-color: #fef3c7;
  color: #92400e;
}

.role-user {
  background-color: #f3e8ff;
  color: #6b21a8;
}

.status-badge {
  padding: 0.35rem 0.85rem;
  border-radius: 12px;
  font-size: 0.8rem;
  font-weight: 600;
  display: inline-block;
  border: none;
  cursor: pointer;
  transition: opacity 0.2s;
}

.status-badge:hover {
  opacity: 0.8;
}

.status-active {
  background-color: #d1fae5;
  color: #065f46;
}

.status-inactive {
  background-color: #fee2e2;
  color: #991b1b;
}

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
  transition: transform 0.2s;
}

.btn:hover {
  transform: translateY(-2px);
}

.btn-danger {
  background-color: #ef4444;
  color: white;
}

.empty-state {
  padding: 3rem;
  text-align: center;
  color: #666;
}

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
}

.modal {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
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
}

.btn-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #666;
  padding: 0.25rem;
  width: 2rem;
  height: 2rem;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.btn-close:hover {
  background-color: #f3f4f6;
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

.form-group-checkbox {
  margin-bottom: 1.5rem;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
}

.checkbox-label input[type="checkbox"] {
  width: 1.25rem;
  height: 1.25rem;
  cursor: pointer;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #e5e7eb;
}

.btn-secondary {
  padding: 0.75rem 1.5rem;
  background: #f3f4f6;
  color: #333;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn-secondary:hover {
  background-color: #e5e7eb;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 1rem;
  }

  .page-header .btn-primary {
    width: 100%;
  }

  .users-table-container {
    overflow-x: auto;
  }

  .users-table {
    min-width: 800px;
  }
}
</style>
