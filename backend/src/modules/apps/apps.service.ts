import { Injectable, NotFoundException, BadRequestException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, In } from 'typeorm';
import { App } from './app.entity';
import { DeviceApp } from './device-app.entity';
import { DevicesService } from '../devices/devices.service';
import { MonitoringGateway } from '../monitoring/monitoring.gateway';
import { extractApkMetadata } from '../../utils/apk-extractor';
import * as fs from 'fs';
import * as path from 'path';

@Injectable()
export class AppsService {
  constructor(
    @InjectRepository(App)
    private appRepository: Repository<App>,
    @InjectRepository(DeviceApp)
    private deviceAppRepository: Repository<DeviceApp>,
    private devicesService: DevicesService,
    private monitoringGateway: MonitoringGateway,
  ) {
    // Set bidirectional reference
    this.devicesService.setAppsService(this);
  }

  async findAll(): Promise<App[]> {
    return this.appRepository.find({
      order: { createdAt: 'DESC' },
    });
  }

  async findOne(id: string): Promise<App> {
    const app = await this.appRepository.findOne({ where: { id } });
    
    if (!app) {
      throw new NotFoundException(`App with ID ${id} not found`);
    }
    
    return app;
  }

  async create(appData: Partial<App>, file: Express.Multer.File): Promise<App> {
    if (!file) {
      throw new BadRequestException('APK file is required');
    }

    // Cambiar permisos del archivo a 644 (rw-r--r--)
    try {
      fs.chmodSync(file.path, 0o644);
      console.log('✅ File permissions set to 644:', file.path);
    } catch (error) {
      console.error('⚠️ Failed to set file permissions:', error.message);
    }

    // Extraer metadatos del APK automáticamente
    console.log('📦 Extracting APK metadata from:', file.path);
    console.log('📝 Received appData:', appData);
    const apkMetadata = await extractApkMetadata(file.path);
    
    // SIEMPRE usar metadatos extraídos (ignorar datos manuales)
    const app = this.appRepository.create({
      name: apkMetadata.label || file.originalname.replace('.apk', ''),
      packageName: apkMetadata.packageName,
      version: apkMetadata.versionName,
      versionCode: apkMetadata.versionCode,
      description: appData.description,
      fileName: file.originalname,
      filePath: file.path,
      fileSize: file.size,
    });

    console.log('✅ App created with metadata:', {
      name: app.name,
      packageName: app.packageName,
      version: app.version,
      versionCode: app.versionCode
    });

    return this.appRepository.save(app);
  }

  async updateMdmApp(file: Express.Multer.File, deviceIds: string[]): Promise<{ message: string; results: any[] }> {
    if (!file) {
      throw new BadRequestException('MDM APK file is required');
    }

    console.log('🔄 MDM Update requested for', deviceIds.length, 'devices');
    
    // Construir URL absoluta del APK
    const baseUrl = process.env.BACKEND_URL || 'http://192.168.1.20:3000';
    const apkUrl = `${baseUrl}/${file.path.replace(/\\/g, '/')}`;

    const results = [];
    for (const deviceId of deviceIds) {
      try {
        const device = await this.devicesService.findOne(deviceId);
        
        console.log('📲 Sending MDM update to device:', device.name);
        
        const appData = {
          name: 'OpenMDM-Android',
          packageName: 'com.androidmdm.app',
          version: 'latest',
          apkUrl,
          fileSize: file.size,
          isMdmUpdate: true,
        };

        const sent = await this.monitoringGateway.sendInstallCommand(deviceId, appData);
        
        results.push({
          deviceId,
          deviceName: device.name,
          success: sent,
        });

        console.log('✅ MDM update command sent to:', device.name);
      } catch (error) {
        console.error('❌ Failed to send MDM update to device:', deviceId, error.message);
        results.push({
          deviceId,
          success: false,
          error: error.message,
        });
      }
    }

    return {
      message: `MDM update sent to ${results.filter(r => r.success).length}/${deviceIds.length} devices`,
      results,
    };
  }

  async update(id: string, updateData: Partial<App>): Promise<App> {
    const app = await this.findOne(id);
    Object.assign(app, updateData);
    return this.appRepository.save(app);
  }

  async remove(id: string): Promise<void> {
    const app = await this.findOne(id);
    
    // Remove all device_apps associations first
    await this.deviceAppRepository.delete({ appId: id });
    
    // Remove file from disk
    if (fs.existsSync(app.filePath)) {
      fs.unlinkSync(app.filePath);
    }
    
    await this.appRepository.remove(app);
  }

  async installOnDevice(appId: string, deviceId: string): Promise<DeviceApp> {
    console.log('📱 installOnDevice called:', { appId, deviceId });
    
    const app = await this.findOne(appId);
    const device = await this.devicesService.findOne(deviceId);
    
    console.log('✅ Found:', { app: app?.name, device: device?.name });

    // Check if already installed
    const existing = await this.deviceAppRepository.findOne({
      where: { appId, deviceId },
    });

    let saved: DeviceApp;
    
    if (existing) {
      existing.status = 'pending';
      saved = await this.deviceAppRepository.save(existing);
    } else {
      const deviceApp = this.deviceAppRepository.create({
        appId,
        deviceId,
        installedVersion: app.version,
        status: 'pending',
      });
      saved = await this.deviceAppRepository.save(deviceApp);
    }
    
    await this.devicesService.createLog(
      deviceId,
      'action',
      `Installation queued for app: ${app.name}`,
    );

    // Send install command via WebSocket
    const appData = {
      appId: app.id,
      name: app.name,
      packageName: app.packageName,
      version: app.version,
      downloadUrl: `${process.env.BACKEND_URL || 'http://192.168.1.20:3000'}/uploads/apks/${path.basename(app.filePath)}`,
    };
    
    console.log('📤 Sending install command:', { deviceId, appData });
    const sent = await this.monitoringGateway.sendInstallCommand(deviceId, appData);
    
    console.log('📨 Send result:', sent ? 'SUCCESS' : 'FAILED - Device offline');
    
    if (sent) {
      await this.devicesService.createLog(
        deviceId,
        'info',
        `Install command sent for app: ${app.name}`,
      );
    } else {
      await this.devicesService.createLog(
        deviceId,
        'warning',
        `Device offline - install will be queued for app: ${app.name}`,
      );
    }

    return saved;
  }

  async installOnMultipleDevices(appId: string, deviceIds: string[]): Promise<DeviceApp[]> {
    console.log('🔢 installOnMultipleDevices called:', { appId, deviceIds, count: deviceIds?.length });
    
    const results = [];
    
    for (const deviceId of deviceIds) {
      try {
        console.log(`  ⏳ Processing device ${deviceId}...`);
        const result = await this.installOnDevice(appId, deviceId);
        results.push(result);
        console.log(`  ✅ Success for device ${deviceId}`);
      } catch (error) {
        console.error(`  ❌ Failed to install on device ${deviceId}:`, error.message);
      }
    }
    
    console.log(`🏁 Finished installing on ${results.length}/${deviceIds.length} devices`);
    return results;
  }

  async uninstallFromDevice(appId: string, deviceId: string): Promise<void> {
    const deviceApp = await this.deviceAppRepository.findOne({
      where: { appId, deviceId },
    });

    if (!deviceApp) {
      throw new NotFoundException('App not installed on this device');
    }

    const app = await this.findOne(appId);
    
    deviceApp.status = 'uninstalling';
    await this.deviceAppRepository.save(deviceApp);
    
    await this.devicesService.createLog(
      deviceId,
      'action',
      `Uninstallation queued for app: ${app.name}`,
    );
    
    // Send uninstall command via WebSocket
    const sent = await this.monitoringGateway.sendUninstallCommand(deviceId, app.packageName);
    
    if (sent) {
      await this.devicesService.createLog(
        deviceId,
        'info',
        `Uninstall command sent for app: ${app.name}`,
      );
    } else {
      await this.devicesService.createLog(
        deviceId,
        'warning',
        `Device offline - uninstall will be queued for app: ${app.name}`,
      );
    }
  }

  async updateInstallationStatus(
    appId: string,
    deviceId: string,
    status: string,
    errorMessage?: string,
  ): Promise<DeviceApp> {
    const deviceApp = await this.deviceAppRepository.findOne({
      where: { appId, deviceId },
    });

    if (!deviceApp) {
      throw new NotFoundException('Installation record not found');
    }

    deviceApp.status = status;
    if (errorMessage) {
      deviceApp.errorMessage = errorMessage;
    }

    return this.deviceAppRepository.save(deviceApp);
  }

  async removeAppFromDevice(appId: string, deviceId: string): Promise<void> {
    const deviceApp = await this.deviceAppRepository.findOne({
      where: { appId, deviceId },
    });

    if (deviceApp) {
      await this.deviceAppRepository.remove(deviceApp);
      console.log(`✅ Removed device_app record: appId=${appId}, deviceId=${deviceId}`);
    } else {
      console.warn(`⚠️ No device_app record found to remove: appId=${appId}, deviceId=${deviceId}`);
    }
  }

  async getDeviceApps(deviceId: string): Promise<DeviceApp[]> {
    return this.deviceAppRepository.find({
      where: { deviceId },
      relations: ['app'],
      order: { installedAt: 'DESC' },
    });
  }

  async getPendingInstallations(deviceId: string): Promise<DeviceApp[]> {
    return this.deviceAppRepository.find({
      where: { 
        deviceId,
        status: In(['pending', 'installing']),
      },
      relations: ['app'],
    });
  }
}
