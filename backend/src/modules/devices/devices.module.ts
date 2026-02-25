import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ScheduleModule } from '@nestjs/schedule';
import { DevicesService } from './devices.service';
import { DevicesController } from './devices.controller';
import { Device } from './device.entity';
import { DeviceLog } from './device-log.entity';

@Module({
  imports: [
    TypeOrmModule.forFeature([Device, DeviceLog]),
    ScheduleModule.forRoot(),
  ],
  controllers: [DevicesController],
  providers: [DevicesService],
  exports: [DevicesService],
})
export class DevicesModule {}
