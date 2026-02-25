import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
  ManyToOne,
  OneToMany,
  JoinColumn,
} from 'typeorm';
import { User } from '../users/user.entity';
import { DeviceLog } from './device-log.entity';
import { DeviceApp } from '../apps/device-app.entity';

@Entity('devices')
export class Device {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ unique: true })
  deviceId: string;

  @Column()
  name: string;

  @Column({ nullable: true })
  manufacturer: string;

  @Column({ nullable: true })
  model: string;

  @Column({ nullable: true })
  androidVersion: string;

  @Column({ nullable: true })
  serialNumber: string;

  @Column({ type: 'enum', enum: ['online', 'offline', 'inactive'], default: 'offline' })
  status: string;

  @Column({ type: 'float', nullable: true })
  batteryLevel: number;

  @Column({ type: 'float', nullable: true })
  storageUsed: number;

  @Column({ type: 'float', nullable: true })
  storageTotal: number;

  @Column({ type: 'float', nullable: true })
  ramUsed: number;

  @Column({ type: 'float', nullable: true })
  ramTotal: number;

  @Column({ nullable: true })
  ipAddress: string;

  @Column({ type: 'jsonb', nullable: true })
  location: {
    latitude: number;
    longitude: number;
    timestamp: Date;
  };

  @Column({ type: 'timestamp', nullable: true })
  lastSeen: Date;

  @Column({ default: false })
  isLocked: boolean;

  @Column({ default: false })
  isAdminEnabled: boolean;

  @Column({ nullable: true })
  kioskPackageName: string;

  @Column({ default: false })
  kioskModeEnabled: boolean;

  @Column({ type: 'jsonb', nullable: true })
  powerSchedule: {
    enabled: boolean;
    powerOnTime: string;  // HH:mm format
    powerOffTime: string; // HH:mm format
    daysOfWeek: number[]; // 0-6 (Sunday-Saturday)
  };

  @ManyToOne(() => User, user => user.devices, { nullable: true })
  @JoinColumn({ name: 'assignedUserId' })
  assignedUser: User;

  @Column({ nullable: true })
  assignedUserId: string;

  @OneToMany(() => DeviceLog, log => log.device)
  logs: DeviceLog[];

  @OneToMany(() => DeviceApp, app => app.device)
  installedApps: DeviceApp[];

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}
