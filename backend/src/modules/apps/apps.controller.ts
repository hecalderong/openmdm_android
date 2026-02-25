import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
  UseGuards,
  UseInterceptors,
  UploadedFile,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { AppsService } from './apps.service';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';

@Controller('apps')
@UseGuards(JwtAuthGuard)
export class AppsController {
  constructor(private readonly appsService: AppsService) {}

  @Get()
  async findAll() {
    return this.appsService.findAll();
  }

  @Get(':id')
  async findOne(@Param('id') id: string) {
    return this.appsService.findOne(id);
  }

  @Post('upload')
  @UseInterceptors(FileInterceptor('apk'))
  async upload(
    @UploadedFile() file: Express.Multer.File,
    @Body('name') name: string,
    @Body('packageName') packageName: string,
    @Body('version') version: string,
    @Body('versionCode') versionCode: string,
    @Body('description') description?: string,
  ) {
    return this.appsService.create(
      {
        name: name || file.originalname.replace('.apk', ''),
        packageName: packageName || 'com.unknown.app',
        version: version || '1.0',
        versionCode: versionCode ? parseInt(versionCode) : 1,
        description,
      },
      file,
    );
  }

  @Put(':id')
  async update(@Param('id') id: string, @Body() updateData: any) {
    return this.appsService.update(id, updateData);
  }

  @Delete(':id')
  async remove(@Param('id') id: string) {
    await this.appsService.remove(id);
    return { message: 'App removed successfully' };
  }

  @Post(':id/install/:deviceId')
  async installOnDevice(
    @Param('id') appId: string,
    @Param('deviceId') deviceId: string,
  ) {
    return this.appsService.installOnDevice(appId, deviceId);
  }

  @Post(':id/install-multiple')
  async installOnMultipleDevices(
    @Param('id') appId: string,
    @Body('deviceIds') deviceIds: string[],
  ) {
    return this.appsService.installOnMultipleDevices(appId, deviceIds);
  }

  @Delete(':id/uninstall/:deviceId')
  async uninstallFromDevice(
    @Param('id') appId: string,
    @Param('deviceId') deviceId: string,
  ) {
    await this.appsService.uninstallFromDevice(appId, deviceId);
    return { message: 'Uninstallation queued' };
  }

  @Get('device/:deviceId')
  async getDeviceApps(@Param('deviceId') deviceId: string) {
    return this.appsService.getDeviceApps(deviceId);
  }

  @Get('device/:deviceId/pending')
  async getPendingInstallations(@Param('deviceId') deviceId: string) {
    return this.appsService.getPendingInstallations(deviceId);
  }

  @Put(':id/status/:deviceId')
  async updateInstallationStatus(
    @Param('id') appId: string,
    @Param('deviceId') deviceId: string,
    @Body('status') status: string,
    @Body('errorMessage') errorMessage?: string,
  ) {
    return this.appsService.updateInstallationStatus(appId, deviceId, status, errorMessage);
  }

  @Post('mdm/update')
  @UseInterceptors(FileInterceptor('apk'))
  async updateMdmApp(
    @UploadedFile() file: Express.Multer.File,
    @Body('deviceIds') deviceIds: string,
  ) {
    const deviceIdArray = JSON.parse(deviceIds);
    return this.appsService.updateMdmApp(file, deviceIdArray);
  }
}
