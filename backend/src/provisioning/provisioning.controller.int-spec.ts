import { Test, TestingModule } from '@nestjs/testing';
import { ProvisioningController } from './provisioning.controller';
import * as fs from 'fs';

describe('ProvisioningController (integration)', () => {
  let controller: ProvisioningController;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [ProvisioningController],
    }).compile();

    controller = module.get<ProvisioningController>(ProvisioningController);
  });

  it('returns 404 json when MDM APK file is missing', async () => {
    const existsSpy = jest.spyOn(fs, 'existsSync').mockReturnValue(false);
    const json = jest.fn();
    const status = jest.fn().mockReturnValue({ json });
    const res = { status } as any;

    await controller.downloadMDMApk(res);

    expect(status).toHaveBeenCalledWith(404);
    expect(json).toHaveBeenCalledWith(
      expect.objectContaining({ message: expect.stringContaining('APK not found') }),
    );

    existsSpy.mockRestore();
  });

  it('returns provisioning instructions in plain text', async () => {
    const send = jest.fn();
    const type = jest.fn().mockReturnValue({ send });
    const res = { type } as any;

    await controller.getProvisioningInstructions(res);

    expect(type).toHaveBeenCalledWith('text/plain');
    expect(send).toHaveBeenCalledWith(expect.stringContaining('PROVISIÓN AUTOMÁTICA'));
  });
});
