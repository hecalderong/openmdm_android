import {
  WebSocketGateway,
  WebSocketServer,
  SubscribeMessage,
  OnGatewayConnection,
  OnGatewayDisconnect,
  MessageBody,
  ConnectedSocket,
} from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import { DevicesService } from '../devices/devices.service';

@WebSocketGateway({
  cors: {
    origin: '*',
  },
})
export class MonitoringGateway implements OnGatewayConnection, OnGatewayDisconnect {
  @WebSocketServer()
  server: Server;

  private deviceSockets: Map<string, string> = new Map(); // deviceId -> socketId

  constructor(private devicesService: DevicesService) {
    // Inyectar referencia del gateway en el servicio
    this.devicesService.setMonitoringGateway(this);
  }

  async handleConnection(client: Socket) {
    console.log(`Client connected: ${client.id}`);
    
    // Send initial stats to client
    const stats = await this.devicesService.getStats();
    client.emit('stats', stats);
  }

  async handleDisconnect(client: Socket) {
    console.log(`Client disconnected: ${client.id}`);
    
    // Find and update device status if it was a device connection
    for (const [deviceId, socketId] of this.deviceSockets.entries()) {
      if (socketId === client.id) {
        await this.devicesService.updateStatus(deviceId, 'offline');
        this.deviceSockets.delete(deviceId);
        
        // Broadcast device offline to all clients
        this.server.emit('device:offline', { deviceId });
        break;
      }
    }
  }

  @SubscribeMessage('device:register')
  async handleDeviceRegister(
    @MessageBody() data: any,
    @ConnectedSocket() client: Socket,
  ) {
    try {
      const device = await this.devicesService.register({
        deviceId: data.deviceId,
        name: data.name,
        manufacturer: data.manufacturer,
        model: data.model,
        androidVersion: data.androidVersion,
        serialNumber: data.serialNumber,
        status: 'online',
      });

      this.deviceSockets.set(device.id, client.id);
      
      console.log('✅ Device registered:', {
        deviceId: data.deviceId,
        deviceUUID: device.id,
        socketId: client.id,
      });
      
      // Broadcast to all clients
      this.server.emit('device:online', device);
      
      return { success: true, device };
    } catch (error) {
      return { success: false, error: error.message };
    }
  }

  @SubscribeMessage('device:heartbeat')
  async handleHeartbeat(
    @MessageBody() data: any,
    @ConnectedSocket() client: Socket,
  ) {
    try {
      console.log('💓 Received heartbeat from:', data.deviceId);
      const device = await this.devicesService.findByDeviceId(data.deviceId);
      
      if (device) {
        console.log('📍 Device found, updating status to online:', device.id);
        await this.devicesService.update(device.id, {
          status: 'online',
          lastSeen: new Date(),
          batteryLevel: data.batteryLevel,
          storageUsed: data.storageUsed,
          storageTotal: data.storageTotal,
          ramUsed: data.ramUsed,
          ramTotal: data.ramTotal,
          ipAddress: data.ipAddress,
        });
        console.log('✅ Device status updated to online');

        // Broadcast updated device status
        this.server.emit('device:update', device);
      } else {
        console.log('❌ Device not found:', data.deviceId);
      }

      return { success: true };
    } catch (error) {
      console.error('❌ Heartbeat error:', error.message);
      return { success: false, error: error.message };
    }
  }

  @SubscribeMessage('device:location')
  async handleLocationUpdate(
    @MessageBody() data: any,
    @ConnectedSocket() client: Socket,
  ) {
    try {
      const device = await this.devicesService.findByDeviceId(data.deviceId);
      
      if (device) {
        await this.devicesService.updateLocation(
          device.id,
          data.latitude,
          data.longitude,
        );

        // Broadcast location update
        this.server.emit('device:location:update', {
          deviceId: device.id,
          latitude: data.latitude,
          longitude: data.longitude,
        });
      }

      return { success: true };
    } catch (error) {
      return { success: false, error: error.message };
    }
  }

  @SubscribeMessage('device:install-result')
  async handleInstallResult(
    @MessageBody() data: any,
    @ConnectedSocket() client: Socket,
  ) {
    try {
      console.log('📦 Install result received:', {
        deviceId: data.deviceId,
        packageName: data.packageName,
        success: data.success,
      });

      const device = await this.devicesService.findByDeviceId(data.deviceId);
      
      if (device) {
        // Update device_apps status
        await this.devicesService.updateAppInstallStatus(
          device.id,
          data.packageName,
          data.success ? 'installed' : 'failed',
          data.errorMessage,
        );

        // Broadcast to all admin clients
        this.server.emit('app:install-result', {
          deviceId: device.id,
          packageName: data.packageName,
          success: data.success,
          errorMessage: data.errorMessage,
        });

        console.log('✅ Install result processed and broadcasted');
      }

      return { success: true };
    } catch (error) {
      console.error('❌ Install result error:', error.message);
      return { success: false, error: error.message };
    }
  }

  @SubscribeMessage('device:uninstall-result')
  async handleUninstallResult(
    @MessageBody() data: any,
    @ConnectedSocket() client: Socket,
  ) {
    try {
      console.log('🗑️ Uninstall result received:', {
        deviceId: data.deviceId,
        packageName: data.packageName,
        success: data.success,
      });

      const device = await this.devicesService.findByDeviceId(data.deviceId);
      
      if (device) {
        if (data.success) {
          // Remove from device_apps table
          await this.devicesService.removeAppFromDevice(device.id, data.packageName);
          
          console.log('✅ App removed from device_apps table');
        } else {
          // Update status to failed if uninstall failed
          await this.devicesService.updateAppInstallStatus(
            device.id,
            data.packageName,
            'failed',
            data.errorMessage || 'Uninstall failed',
          );
        }

        // Broadcast to all admin clients
        this.server.emit('app:uninstall-result', {
          deviceId: device.id,
          packageName: data.packageName,
          success: data.success,
          errorMessage: data.errorMessage,
        });

        console.log('✅ Uninstall result processed and broadcasted');
      }

      return { success: true };
    } catch (error) {
      console.error('❌ Uninstall result error:', error.message);
      return { success: false, error: error.message };
    }
  }

  @SubscribeMessage('device:command')
  async handleCommand(
    @MessageBody() data: any,
  ) {
    // Send command to specific device
    const socketId = this.deviceSockets.get(data.deviceId);
    
    if (socketId) {
      this.server.to(socketId).emit('command', {
        type: data.commandType,
        payload: data.payload,
      });
      
      return { success: true, message: 'Command sent' };
    }
    
    return { success: false, message: 'Device not connected' };
  }

  // Admin methods to send commands to devices
  async sendLockCommand(deviceId: string) {
    const socketId = this.deviceSockets.get(deviceId);
    
    if (socketId) {
      this.server.to(socketId).emit('command', {
        type: 'lock',
      });
      
      await this.devicesService.lockDevice(deviceId);
      return true;
    }
    
    return false;
  }

  async sendUnlockCommand(deviceId: string) {
    const socketId = this.deviceSockets.get(deviceId);
    
    if (socketId) {
      this.server.to(socketId).emit('command', {
        type: 'unlock',
      });
      
      await this.devicesService.unlockDevice(deviceId);
      return true;
    }
    
    return false;
  }

  async sendInstallCommand(deviceId: string, appData: any) {
    const socketId = this.deviceSockets.get(deviceId);
    
    console.log('🔍 sendInstallCommand:', {
      deviceId,
      socketId,
      hasSocket: !!socketId,
      connectedDevices: Array.from(this.deviceSockets.keys()),
    });
    
    if (socketId) {
      this.server.to(socketId).emit('command', {
        type: 'install_app',
        payload: appData,
      });
      
      console.log('✅ Install command sent to device:', deviceId);
      return true;
    }
    
    console.log('❌ Device not connected:', deviceId);
    return false;
  }

  async sendUninstallCommand(deviceId: string, packageName: string) {
    const socketId = this.deviceSockets.get(deviceId);
    
    if (socketId) {
      this.server.to(socketId).emit('command', {
        type: 'uninstall_app',
        payload: { packageName },
      });
      
      return true;
    }
    
    return false;
  }

  async sendLaunchAppCommand(deviceId: string, packageName: string) {
    const socketId = this.deviceSockets.get(deviceId);
    
    if (socketId) {
      this.server.to(socketId).emit('command', {
        type: 'launch_app',
        payload: { packageName },
      });
      
      await this.devicesService.createLog(
        deviceId,
        'action',
        `Launch app command sent: ${packageName}`,
      );
      
      return true;
    }
    
    return false;
  }

  async sendKioskModeCommand(deviceId: string, packageName: string | null, enable: boolean) {
    const socketId = this.deviceSockets.get(deviceId);
    
    if (socketId) {
      this.server.to(socketId).emit('command', {
        type: enable ? 'enable_kiosk' : 'disable_kiosk',
        payload: { packageName },
      });
      
      console.log(`✅ Kiosk mode ${enable ? 'enable' : 'disable'} command sent to device:`, deviceId);
      return true;
    }
    
    console.log('❌ Device not connected:', deviceId);
    return false;
  }

  async sendPowerScheduleCommand(deviceId: string, schedule: any) {
    const socketId = this.deviceSockets.get(deviceId);
    
    if (socketId) {
      this.server.to(socketId).emit('command', {
        type: schedule ? 'set_power_schedule' : 'clear_power_schedule',
        payload: schedule,
      });
      
      console.log(`✅ Power schedule command sent to device:`, deviceId);
      return true;
    }
    
    console.log('❌ Device not connected:', deviceId);
    return false;
  }
}
