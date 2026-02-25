import { io, Socket } from 'socket.io-client'

class WebSocketService {
  private socket: Socket | null = null
  private url: string

  constructor() {
    this.url = import.meta.env.VITE_WS_URL || 'http://localhost:3000'
  }

  connect(token?: string): void {
    if (this.socket?.connected) {
      return
    }

    this.socket = io(this.url, {
      auth: {
        token: token || localStorage.getItem('token')
      },
      reconnection: true,
      reconnectionDelay: 1000,
      reconnectionDelayMax: 5000,
      reconnectionAttempts: Infinity
    })

    this.socket.on('connect', () => {
      console.log('✅ WebSocket connected')
    })

    this.socket.on('disconnect', () => {
      console.log('❌ WebSocket disconnected')
    })

    this.socket.on('connect_error', (error) => {
      console.error('WebSocket connection error:', error)
    })
  }

  disconnect(): void {
    if (this.socket) {
      this.socket.disconnect()
      this.socket = null
    }
  }

  on(event: string, callback: (...args: any[]) => void): void {
    this.socket?.on(event, callback)
  }

  off(event: string, callback?: (...args: any[]) => void): void {
    this.socket?.off(event, callback)
  }

  emit(event: string, data: any): void {
    this.socket?.emit(event, data)
  }

  get connected(): boolean {
    return this.socket?.connected || false
  }
}

export default new WebSocketService()
