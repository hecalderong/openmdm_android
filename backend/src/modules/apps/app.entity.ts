import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
  OneToMany,
} from 'typeorm';
import { DeviceApp } from './device-app.entity';

@Entity('apps')
export class App {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column()
  name: string;

  @Column()
  packageName: string;

  @Column()
  version: string;

  @Column()
  versionCode: number;

  @Column({ nullable: true })
  description: string;

  @Column()
  fileName: string;

  @Column()
  filePath: string;

  @Column({ type: 'bigint' })
  fileSize: number;

  @Column({ nullable: true })
  iconPath: string;

  @Column({ default: true })
  isActive: boolean;

  @Column({ type: 'jsonb', nullable: true })
  permissions: string[];

  @Column({ default: false })
  isKioskApp: boolean;

  @Column({ default: false })
  autoStartOnBoot: boolean;

  @OneToMany(() => DeviceApp, deviceApp => deviceApp.app)
  deviceApps: DeviceApp[];

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}
