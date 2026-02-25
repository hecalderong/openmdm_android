import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
  ManyToOne,
  JoinColumn,
} from 'typeorm';
import { Device } from '../devices/device.entity';
import { App } from './app.entity';

@Entity('device_apps')
export class DeviceApp {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @ManyToOne(() => Device, device => device.installedApps)
  @JoinColumn({ name: 'deviceId' })
  device: Device;

  @Column()
  deviceId: string;

  @ManyToOne(() => App, app => app.deviceApps)
  @JoinColumn({ name: 'appId' })
  app: App;

  @Column()
  appId: string;

  @Column()
  installedVersion: string;

  @Column({ type: 'enum', enum: ['pending', 'installing', 'installed', 'failed', 'uninstalling'], default: 'pending' })
  status: string;

  @Column({ nullable: true })
  errorMessage: string;

  @CreateDateColumn()
  installedAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}
