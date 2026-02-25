import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'

export interface User {
  id: string
  email: string
  name: string
  role: 'admin' | 'operator' | 'viewer'
  isActive: boolean
  createdAt: string
  updatedAt?: string
}

export interface CreateUserDto {
  email: string
  name: string
  password: string
  role: 'admin' | 'operator' | 'viewer'
}

export interface UpdateUserDto {
  email?: string
  name?: string
  password?: string
  role?: 'admin' | 'operator' | 'viewer'
  isActive?: boolean
}

export const useUsersStore = defineStore('users', () => {
  const users = ref<User[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  const fetchUsers = async () => {
    loading.value = true
    error.value = null
    try {
      const response = await api.get('/users')
      users.value = response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error al cargar usuarios'
      console.error('Error fetching users:', err)
    } finally {
      loading.value = false
    }
  }

  const getUserById = async (id: string): Promise<User | null> => {
    try {
      const response = await api.get(`/users/${id}`)
      return response.data
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error al cargar usuario'
      console.error('Error fetching user:', err)
      return null
    }
  }

  const createUser = async (userData: CreateUserDto): Promise<boolean> => {
    loading.value = true
    error.value = null
    try {
      const response = await api.post('/auth/register', userData)
      users.value.push(response.data)
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error al crear usuario'
      console.error('Error creating user:', err)
      return false
    } finally {
      loading.value = false
    }
  }

  const updateUser = async (id: string, userData: UpdateUserDto): Promise<boolean> => {
    loading.value = true
    error.value = null
    try {
      const response = await api.put(`/users/${id}`, userData)
      const index = users.value.findIndex(u => u.id === id)
      if (index !== -1) {
        users.value[index] = response.data
      }
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error al actualizar usuario'
      console.error('Error updating user:', err)
      return false
    } finally {
      loading.value = false
    }
  }

  const deleteUser = async (id: string): Promise<boolean> => {
    loading.value = true
    error.value = null
    try {
      await api.delete(`/users/${id}`)
      users.value = users.value.filter(u => u.id !== id)
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error al eliminar usuario'
      console.error('Error deleting user:', err)
      return false
    } finally {
      loading.value = false
    }
  }

  const toggleUserStatus = async (id: string): Promise<boolean> => {
    const user = users.value.find(u => u.id === id)
    if (!user) return false
    
    return updateUser(id, { isActive: !user.isActive })
  }

  return {
    users,
    loading,
    error,
    fetchUsers,
    getUserById,
    createUser,
    updateUser,
    deleteUser,
    toggleUserStatus
  }
})
