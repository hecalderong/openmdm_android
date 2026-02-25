import { Injectable, NotFoundException, OnModuleInit } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Device } from './device.entity';
import { DeviceLog } from './device-log.entity';

@Injectable()
export class DevicesService implements OnModuleInit {
  private monitoringGateway: any; // Inyección manual para evitar dependencia circular
  private appsService: any; // Inyección manual para evitar dependencia circular
  private checkInterval: NodeJS.Timeout;
  
  constructor(
    @InjectRepository(Device)
    private deviceRepository: Repository<Device>,
    @InjectRepository(DeviceLog)
    private deviceLogRepository: Repository<DeviceLog>,
  ) {}

  onModuleInit() {
    console.log('✅ DevicesService initialized - Starting stale device checker');
    // Ejecutar inmediatamente
    setTimeout(() => {
      console.log('⏰ First check triggered');
      this.checkStaleDevices();
    }, 1000); // 1 segundo después de init
    
    // Ejecutar cada minuto
    this.checkInterval = setInterval(() => {
      this.checkStaleDevices();
    }, 60000); // 60 segundos
  }

  onModuleDestroy() {
    if (this.checkInterval) {
      clearInterval(this.checkInterval);
    }
  }

  setMonitoringGateway(gateway: any) {
    this.monitoringGateway = gateway;
  }

  setAppsService(service: any) {
    this.appsService = service;
  }

  async checkStaleDevices() {
    try {
      console.log('🔍 Checking for stale devices...');
      const devices = await this.deviceRepository.find({
        where: { status: 'online' },
      });

      console.log(`   Found ${devices.length} online devices`);
      const now = new Date();
      const staleThreshold = 2 * 60 * 1000; // 2 minutes

      for (const device of devices) {
        if (device.lastSeen) {
          const timeSinceLastSeen = now.getTime() - new Date(device.lastSeen).getTime();
          console.log(`   Device ${device.name}: ${Math.round(timeSinceLastSeen / 1000)}s since last seen`);
          if (timeSinceLastSeen > staleThreshold) {
            console.log(`⏰ Device ${device.name} is stale (${Math.round(timeSinceLastSeen / 1000)}s), marking as offline`);
            device.status = 'offline';
            await this.deviceRepository.save(device);
            
            // Broadcast device offline
            if (this.monitoringGateway && this.monitoringGateway.server) {
              this.monitoringGateway.server.emit('device:offline', { deviceId: device.id });
            }
          }
        }
      }
    } catch (error) {
      console.error('❌ Error checking stale devices:', error.message);
    }
  }

  async findAll(): Promise<Device[]> {
    return this.deviceRepository.find({
      relations: ['assignedUser'],
      order: { createdAt: 'DESC' },
    });
  }

  async findOne(id: string): Promise<Device> {
    const device = await this.deviceRepository.findOne({
      where: { id },
      relations: ['assignedUser', 'installedApps', 'logs'],
    });

    if (!device) {
      throw new NotFoundException(`Device with ID ${id} not found`);
    }

    return device;
  }

  async findByDeviceId(deviceId: string): Promise<Device> {
    return this.deviceRepository.findOne({
      where: { deviceId },
      relations: ['assignedUser'],
    });
  }

  async register(deviceData: Partial<Device>): Promise<Device> {
    const existingDevice = await this.findByDeviceId(deviceData.deviceId);
    
    if (existingDevice) {
      return this.update(existingDevice.id, deviceData);
    }

    const device = this.deviceRepository.create(deviceData);
    const saved = await this.deviceRepository.save(device);
    
    await this.createLog(saved.id, 'info', 'Device registered');
    
    return saved;
  }

  async update(id: string, updateData: Partial<Device>): Promise<Device> {
    console.log('🔄 devices.service.update called:', { id, updateData });
    const device = await this.findOne(id);
    console.log('🔍 Current device status BEFORE update:', device.status);
    Object.assign(device, updateData);
    console.log('🔍 Device status AFTER Object.assign:', device.status);
    const saved = await this.deviceRepository.save(device);
    console.log('✅ Device saved, status:', saved.status);
    return saved;
  }

  async updateStatus(id: string, status: string): Promise<Device> {
    const device = await this.findOne(id);
    device.status = status;
    device.lastSeen = new Date();
    
    await this.createLog(id, 'info', `Device status changed to ${status}`);
    
    return this.deviceRepository.save(device);
  }

  async lockDevice(id: string): Promise<Device> {
    const device = await this.findOne(id);
    device.isLocked = true;
    
    await this.createLog(id, 'action', 'Device locked remotely');
    
    return this.deviceRepository.save(device);
  }

  async unlockDevice(id: string): Promise<Device> {
    const device = await this.findOne(id);
    device.isLocked = false;
    
    await this.createLog(id, 'action', 'Device unlocked');
    
    return this.deviceRepository.save(device);
  }

  async launchApp(id: string, packageName: string): Promise<{ success: boolean; message: string }> {
    const device = await this.findOne(id);
    
    if (!this.monitoringGateway) {
      return { success: false, message: 'Monitoring gateway not available' };
    }
    
    const sent = await this.monitoringGateway.sendLaunchAppCommand(device.id, packageName);
    
    if (sent) {
      return { success: true, message: 'Launch app command sent successfully' };
    } else {
      return { success: false, message: 'Device not connected' };
    }
  }

  async updateLocation(id: string, latitude: number, longitude: number): Promise<Device> {
    const device = await this.findOne(id);
    device.location = {
      latitude,
      longitude,
      timestamp: new Date(),
    };
    
    return this.deviceRepository.save(device);
  }

  async createLog(deviceId: string, level: string, message: string, metadata?: any): Promise<DeviceLog> {
    const log = this.deviceLogRepository.create({
      deviceId,
      level,
      message,
      metadata,
    });
    
    return this.deviceLogRepository.save(log);
  }

  async getDeviceLogs(deviceId: string, limit: number = 100): Promise<DeviceLog[]> {
    return this.deviceLogRepository.find({
      where: { deviceId },
      order: { createdAt: 'DESC' },
      take: limit,
    });
  }

  async remove(id: string): Promise<void> {
    const device = await this.findOne(id);
    await this.deviceRepository.remove(device);
  }

  async getStats() {
    const total = await this.deviceRepository.count();
    const online = await this.deviceRepository.count({ where: { status: 'online' } });
    const offline = await this.deviceRepository.count({ where: { status: 'offline' } });
    const locked = await this.deviceRepository.count({ where: { isLocked: true } });

    return {
      total,
      online,
      offline,
      locked,
    };
  }

  async updateAppInstallStatus(deviceId: string, packageName: string, status: string, errorMessage?: string) {
    if (!this.appsService) {
      console.warn('⚠️ AppsService not set in DevicesService');
      return;
    }

    try {
      // Find the app by packageName
      const apps = await this.appsService.findAll();
      const app = apps.find((a: any) => a.packageName === packageName);

      if (!app) {
        console.warn(`⚠️ App with package ${packageName} not found`);
        return;
      }

      await this.appsService.updateInstallationStatus(app.id, deviceId, status, errorMessage);
      console.log(`✅ Updated install status: ${packageName} -> ${status}`);
    } catch (error) {
      console.error('❌ Error updating app install status:', error.message);
    }
  }

  async removeAppFromDevice(deviceId: string, packageName: string) {
    if (!this.appsService) {
      console.warn('⚠️ AppsService not set in DevicesService');
      return;
    }

    try {
      // Find the app by packageName
      const apps = await this.appsService.findAll();
      const app = apps.find((a: any) => a.packageName === packageName);

      if (!app) {
        console.warn(`⚠️ App with package ${packageName} not found`);
        return;
      }

      await this.appsService.removeAppFromDevice(app.id, deviceId);
      console.log(`✅ Removed app from device: ${packageName}`);
    } catch (error) {
      console.error('❌ Error removing app from device:', error.message);
    }
  }

  async enableKioskMode(deviceId: string, packageName: string) {
    const device = await this.findOne(deviceId);
    
    device.kioskPackageName = packageName;
    device.kioskModeEnabled = true;
    await this.deviceRepository.save(device);

    await this.createLog(deviceId, 'action', `Kiosk mode enabled for: ${packageName}`);

    // Send command via WebSocket to enable kiosk mode on device
    const sent = await this.monitoringGateway.sendKioskModeCommand(deviceId, packageName, true);
    
    if (sent) {
      await this.createLog(deviceId, 'info', `Kiosk mode command sent`);
    } else {
      await this.createLog(deviceId, 'warning', `Device offline - kiosk mode will activate when device reconnects`);
    }

    return device;
  }

  async disableKioskMode(deviceId: string) {
    const device = await this.findOne(deviceId);
    
    const previousPackage = device.kioskPackageName;
    device.kioskPackageName = null;
    device.kioskModeEnabled = false;
    await this.deviceRepository.save(device);

    await this.createLog(deviceId, 'action', `Kiosk mode disabled`);

    // Send command via WebSocket to disable kiosk mode on device
    const sent = await this.monitoringGateway.sendKioskModeCommand(deviceId, null, false);
    
    if (sent) {
      await this.createLog(deviceId, 'info', `Kiosk mode disable command sent`);
    } else {
      await this.createLog(deviceId, 'warning', `Device offline - kiosk mode will deactivate when device reconnects`);
    }

    return device;
  }
  
  async setPowerSchedule(deviceId: string, schedule: any) {
    const device = await this.findOne(deviceId);
    
    device.powerSchedule = schedule;
    await this.deviceRepository.save(device);

    await this.createLog(
      deviceId,
      'action',
      `Power schedule set: ${schedule.powerOnTime} - ${schedule.powerOffTime}`,
    );

    // Send command via WebSocket to configure power schedule
    const sent = await this.monitoringGateway.sendPowerScheduleCommand(
      deviceId,
      schedule,
    );

    if (sent) {
      await this.createLog(deviceId, 'info', `Power schedule command sent`);
    } else {
      await this.createLog(
        deviceId,
        'warning',
        `Device offline - schedule will activate when device reconnects`,
      );
    }

    return device;
  }

  async clearPowerSchedule(deviceId: string) {
    const device = await this.findOne(deviceId);
    
    device.powerSchedule = null;
    await this.deviceRepository.save(device);

    await this.createLog(deviceId, 'action', `Power schedule cleared`);

    // Send command to clear schedule
    await this.monitoringGateway.sendPowerScheduleCommand(deviceId, null);

    return device;
  }
}

