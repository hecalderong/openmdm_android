import { extractApkMetadata } from './apk-extractor';

describe('extractApkMetadata', () => {
  it('returns fallback metadata when APK file does not exist', async () => {
    const metadata = await extractApkMetadata('/tmp/file-that-does-not-exist.apk');

    expect(metadata.packageName).toBe('com.unknown.app');
    expect(metadata.versionName).toBe('1.0');
    expect(metadata.versionCode).toBe(1);
    expect(metadata.label).toBe('file-that-does-not-exist');
  });
});
