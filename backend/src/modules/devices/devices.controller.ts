import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
  UseGuards,
  Query,
} from '@nestjs/common';
import { DevicesService } from './devices.service';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';

@Controller('devices')
@UseGuards(JwtAuthGuard)
export class DevicesController {
  constructor(private readonly devicesService: DevicesService) {}

  @Get()
  async findAll() {
    return this.devicesService.findAll();
  }

  @Get('stats')
  async getStats() {
    return this.devicesService.getStats();
  }

  @Get(':id')
  async findOne(@Param('id') id: string) {
    return this.devicesService.findOne(id);
  }

  @Get(':id/logs')
  async getDeviceLogs(
    @Param('id') id: string,
    @Query('limit') limit?: string,
  ) {
    return this.devicesService.getDeviceLogs(id, limit ? parseInt(limit) : 100);
  }

  @Post('register')
  async register(@Body() deviceData: any) {
    return this.devicesService.register(deviceData);
  }

  @Put(':id')
  async update(@Param('id') id: string, @Body() updateData: any) {
    return this.devicesService.update(id, updateData);
  }

  @Put(':id/lock')
  async lock(@Param('id') id: string) {
    return this.devicesService.lockDevice(id);
  }

  @Put(':id/unlock')
  async unlock(@Param('id') id: string) {
    return this.devicesService.unlockDevice(id);
  }

  @Post(':id/launch-app')
  async launchApp(
    @Param('id') id: string,
    @Body('packageName') packageName: string,
  ) {
    return this.devicesService.launchApp(id, packageName);
  }

  @Put(':id/location')
  async updateLocation(
    @Param('id') id: string,
    @Body('latitude') latitude: number,
    @Body('longitude') longitude: number,
  ) {
    return this.devicesService.updateLocation(id, latitude, longitude);
  }

  @Delete(':id')
  async remove(@Param('id') id: string) {
    await this.devicesService.remove(id);
    return { message: 'Device removed successfully' };
  }

  @Post(':id/kiosk-mode')
  async enableKioskMode(
    @Param('id') id: string,
    @Body() body: { packageName: string },
  ) {
    return await this.devicesService.enableKioskMode(id, body.packageName);
  }

  @Delete(':id/kiosk-mode')
  async disableKioskMode(@Param('id') id: string) {
    return await this.devicesService.disableKioskMode(id);
  }

  @Post(':id/power-schedule')
  async setPowerSchedule(
    @Param('id') id: string,
    @Body() schedule: {
      enabled: boolean;
      powerOnTime: string;
      powerOffTime: string;
      daysOfWeek: number[];
    },
  ) {
    return await this.devicesService.setPowerSchedule(id, schedule);
  }

  @Delete(':id/power-schedule')
  async clearPowerSchedule(@Param('id') id: string) {
    return await this.devicesService.clearPowerSchedule(id);
  }
}
