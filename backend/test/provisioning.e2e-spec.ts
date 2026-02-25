import { INestApplication } from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import * as request from 'supertest';
import { ProvisioningModule } from '../src/provisioning/provisioning.module';

describe('Provisioning (e2e)', () => {
  let app: INestApplication;

  beforeAll(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [ProvisioningModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    await app.init();
  });

  afterAll(async () => {
    await app.close();
  });

  it('/provisioning/instructions (GET)', async () => {
    const response = await request(app.getHttpServer())
      .get('/provisioning/instructions')
      .expect(200);

    expect(response.text).toContain('PROVISIÓN AUTOMÁTICA');
  });
});
