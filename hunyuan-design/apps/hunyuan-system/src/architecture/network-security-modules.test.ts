import { existsSync, readFileSync } from 'node:fs';

import { describe, expect, it } from 'vitest';

import { resolveWorkspacePath } from '../test-utils/workspace-path';

const level3ProtectPagePath =
  'packages/features/platform-security/src/protection/settings-page.vue';
const loginFailPagePath =
  'packages/features/platform-security/src/protection/login-failure-page.vue';
const loginLogPagePath =
  'packages/features/platform-audit/src/login-log/index.vue';
const operateLogPagePath =
  'packages/features/platform-audit/src/operation-log/index.vue';
const operateLogDrawerPath =
  'packages/features/platform-audit/src/operation-log/components/operate-log-detail-drawer.vue';
const dataMaskingPagePath =
  'packages/features/platform-security/src/data-masking/index.vue';
const apiEncryptPagePath =
  'packages/features/platform-devtools/src/api-encrypt/index.vue';

function readSource(path: string) {
  const resolvedPath = resolveWorkspacePath(path);
  expect(existsSync(resolvedPath)).toBe(true);
  return readFileSync(resolvedPath, 'utf8');
}

describe('network security backend menu docking pages', () => {
  it('provides a real level-3 protect config page', () => {
    const source = readSource(level3ProtectPagePath);

    expect(source).toContain('SystemNetworkSecurityLevel3ProtectConfigIndex');
    expect(source).toContain('Page auto-content-height');
    expect(source).toContain('ArtEditPage');
    expect(source).toContain('ArtEditSection');
    expect(source).not.toContain(
      'absolute inset-0 box-border flex min-h-0 flex-col overflow-hidden p-4',
    );
  });

  it('provides a real login-fail management page', () => {
    const source = readSource(loginFailPagePath);

    expect(source).toContain('SystemNetworkSecurityLoginFailList');
    expect(source).toContain('ArtSearchPanel');
    expect(source).toContain('ArtTable');
    expect(source).toContain('batchDeleteLoginFails');
  });

  it('provides a real login-log page', () => {
    expect(readSource(loginLogPagePath)).toContain(
      'SystemNetworkSecurityLoginLogList',
    );
  });

  it('provides a real operate-log page and operate-log drawer', () => {
    expect(readSource(operateLogPagePath)).toContain(
      'SystemNetworkSecurityOperateLogList',
    );
    expect(readSource(operateLogDrawerPath)).toContain(
      'SystemNetworkSecurityOperateLogDetailDrawer',
    );
  });

  it('provides a capability page for data masking without module-bridge copy', () => {
    const dataMaskingSource = readSource(dataMaskingPagePath);

    expect(dataMaskingSource).toContain('SystemNetworkSecurityDataMaskingList');
    expect(dataMaskingSource).not.toContain('module-bridge');
  });

  it('provides a capability page for api encrypt without module-bridge copy', () => {
    const apiEncryptSource = readSource(apiEncryptPagePath);

    expect(apiEncryptSource).toContain('SystemNetworkSecurityApiEncryptIndex');
    expect(apiEncryptSource).toContain('testResponseEncrypt');
    expect(apiEncryptSource).not.toContain('crypto-js');
  });
});
