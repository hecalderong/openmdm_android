import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  ManyToOne,
  JoinColumn,
} from 'typeorm';
import { Device } from './device.entity';

@Entity('device_logs')
export class DeviceLog {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @ManyToOne(() => Device, device => device.logs)
  @JoinColumn({ name: 'deviceId' })
  device: Device;

  @Column()
  deviceId: string;

  @Column({ type: 'enum', enum: ['info', 'warning', 'error', 'action'] })
  level: string;

  @Column()
  message: string;

  @Column({ type: 'jsonb', nullable: true })
  metadata: any;

  @CreateDateColumn()
  createdAt: Date;
}
