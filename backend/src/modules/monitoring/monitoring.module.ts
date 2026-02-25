import { Module } from '@nestjs/common';
import { MonitoringGateway } from './monitoring.gateway';
import { DevicesModule } from '../devices/devices.module';

@Module({
  imports: [DevicesModule],
  providers: [MonitoringGateway],
  exports: [MonitoringGateway],
})
export class MonitoringModule {}
