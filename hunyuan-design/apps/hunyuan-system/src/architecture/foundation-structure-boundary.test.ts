import {
  existsSync,
  readFileSync,
  readdirSync,
  statSync,
} from 'node:fs';
import { basename, join, relative, resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

const workspaceRoot = resolve(process.cwd());
const appRoot = resolve(workspaceRoot, 'apps/hunyuan-system');
const featureRoot = resolve(workspaceRoot, 'packages/features');

const legacySystemApiFiles = new Set([
  'apps/hunyuan-system/src/api/system/api-encrypt.ts',
  'apps/hunyuan-system/src/api/system/cache.ts',
  'apps/hunyuan-system/src/api/system/config.ts',
  'apps/hunyuan-system/src/api/system/data-masking.ts',
  'apps/hunyuan-system/src/api/system/dict.ts',
  'apps/hunyuan-system/src/api/system/file.ts',
  'apps/hunyuan-system/src/api/system/job.ts',
  'apps/hunyuan-system/src/api/system/login-log.ts',
  'apps/hunyuan-system/src/api/system/message.ts',
  'apps/hunyuan-system/src/api/system/network-protect.ts',
  'apps/hunyuan-system/src/api/system/operate-log.ts',
  'apps/hunyuan-system/src/api/system/reload.ts',
  'apps/hunyuan-system/src/api/system/serial-number.ts',
  'apps/hunyuan-system/src/api/system/sms.ts',
]);

const legacySupportViewFiles = new Set([
  'apps/hunyuan-system/src/views/support/api-encrypt/api-encrypt-index.vue',
  'apps/hunyuan-system/src/views/support/cache/cache-list.vue',
  'apps/hunyuan-system/src/views/support/cache/components/cache-key-drawer.vue',
  'apps/hunyuan-system/src/views/support/config/config-list.vue',
  'apps/hunyuan-system/src/views/support/dict/components/dict-data-drawer.vue',
  'apps/hunyuan-system/src/views/support/dict/index.vue',
  'apps/hunyuan-system/src/views/support/file/file-list.vue',
  'apps/hunyuan-system/src/views/support/job/components/job-log-drawer.vue',
  'apps/hunyuan-system/src/views/support/job/job-list.vue',
  'apps/hunyuan-system/src/views/support/level3protect/data-masking-list.vue',
  'apps/hunyuan-system/src/views/support/level3protect/level3-protect-config-index.vue',
  'apps/hunyuan-system/src/views/support/login-fail/login-fail-list.vue',
  'apps/hunyuan-system/src/views/support/login-log/login-log-list.vue',
  'apps/hunyuan-system/src/views/support/message/message-list.vue',
  'apps/hunyuan-system/src/views/support/operate-log/components/operate-log-detail-drawer.vue',
  'apps/hunyuan-system/src/views/support/operate-log/operate-log-list.vue',
  'apps/hunyuan-system/src/views/support/reload/components/reload-result-drawer.vue',
  'apps/hunyuan-system/src/views/support/reload/reload-list.vue',
  'apps/hunyuan-system/src/views/support/serial-number/components/serial-number-record-drawer.vue',
  'apps/hunyuan-system/src/views/support/serial-number/serial-number-list.vue',
  'apps/hunyuan-system/src/views/support/sms/send-log-list.vue',
  'apps/hunyuan-system/src/views/support/sms/template-list.vue',
]);

function normalizePath(path: string) {
  return relative(workspaceRoot, path).replaceAll('\\', '/');
}

function collectFiles(root: string, extensions: ReadonlySet<string>) {
  if (!existsSync(root)) {
    return [];
  }

  const files: string[] = [];
  const visit = (directory: string) => {
    for (const entry of readdirSync(directory, { withFileTypes: true })) {
      const path = join(directory, entry.name);
      if (entry.isDirectory()) {
        visit(path);
        continue;
      }
      if (
        extensions.has(entry.name.slice(entry.name.lastIndexOf('.'))) &&
        !entry.name.includes('.test.') &&
        !entry.name.includes('.spec.')
      ) {
        files.push(normalizePath(path));
      }
    }
  };

  visit(root);
  return files.sort();
}

function unexpectedFiles(currentFiles: string[], frozenFiles: Set<string>) {
  return currentFiles.filter((path) => !frozenFiles.has(path));
}

describe('底座前端结构边界', () => {
  it('应用内历史平台 API 只能减少不能新增', () => {
    const files = collectFiles(
      resolve(appRoot, 'src/api/system'),
      new Set(['.ts']),
    );

    expect(unexpectedFiles(files, legacySystemApiFiles)).toEqual([]);
  });

  it('应用内历史平台页面只能减少不能新增', () => {
    const files = collectFiles(
      resolve(appRoot, 'src/views/support'),
      new Set(['.ts', '.vue']),
    );

    expect(unexpectedFiles(files, legacySupportViewFiles)).toEqual([]);
  });

  it('feature 不得反向依赖应用实现', () => {
    const files = collectFiles(featureRoot, new Set(['.ts', '.vue']));
    const violations = files.flatMap((path) => {
      const source = readFileSync(resolve(workspaceRoot, path), 'utf8');
      const reasons = [
        source.includes("from '#/") ? '使用应用别名 #/' : '',
        source.includes('apps/hunyuan-system') ? '引用应用源码路径' : '',
        source.includes('@hunyuan/system') ? '依赖应用包' : '',
      ].filter(Boolean);
      return reasons.map((reason) => `${path}: ${reason}`);
    });

    expect(violations).toEqual([]);
  });

  it('现有 feature 均通过公开入口声明模块边界', () => {
    const appPackage = JSON.parse(
      readFileSync(resolve(appRoot, 'package.json'), 'utf8'),
    ) as { dependencies?: Record<string, string> };
    const featureDirectories = readdirSync(featureRoot)
      .map((name) => resolve(featureRoot, name))
      .filter((path) => statSync(path).isDirectory())
      .sort();

    expect(featureDirectories.length).toBeGreaterThan(0);

    for (const directory of featureDirectories) {
      const packageJson = JSON.parse(
        readFileSync(resolve(directory, 'package.json'), 'utf8'),
      ) as { exports?: Record<string, unknown>; name: string };
      const indexPath = resolve(directory, 'src/index.ts');
      const source = readFileSync(indexPath, 'utf8');

      expect(packageJson.name).toMatch(/^@hunyuan\/feature-/);
      expect(packageJson.exports?.['.']).toBeTruthy();
      expect(appPackage.dependencies?.[packageJson.name]).toBe('workspace:*');
      expect(source, basename(directory)).toContain('id:');
      expect(source, basename(directory)).toContain('capabilities:');
      expect(source, basename(directory)).toMatch(/routes?:/);
    }
  });
});
