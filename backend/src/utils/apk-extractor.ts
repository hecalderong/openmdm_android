import * as fs from 'fs';
const ApkReader = require('adbkit-apkreader');

export interface ApkMetadata {
  packageName: string;
  versionName: string;
  versionCode: number;
  label: string;
}

export async function extractApkMetadata(apkPath: string): Promise<ApkMetadata> {
  console.log('📦 Extracting metadata from:', apkPath);
  
  const fallback: ApkMetadata = {
    packageName: 'com.unknown.app',
    versionName: '1.0',
    versionCode: 1,
    label: apkPath.split('/').pop()?.replace('.apk', '') || 'Unknown App'
  };

  if (!fs.existsSync(apkPath)) {
    console.log('❌ APK file does not exist:', apkPath);
    return fallback;
  }

  try {
    const reader = await ApkReader.open(apkPath);
    const manifest = await reader.readManifest();
    
    const packageName = manifest.package || fallback.packageName;
    const versionName = manifest.versionName?.toString() || fallback.versionName;
    const versionCode = manifest.versionCode || fallback.versionCode;
    
    let label = fallback.label;
    if (manifest.application?.label) {
      const appLabel = manifest.application.label;
      if (typeof appLabel === 'string' && !appLabel.startsWith('resourceId:')) {
        label = appLabel;
      }
    }
    
    console.log('✅ Package name extracted:', packageName);
    return { packageName, versionName, versionCode, label };
  } catch (error) {
    console.log('⚠️ Failed to extract package name:', error.message);
  }

  console.log('⚠️ Using fallback package name');
  return fallback;
}
