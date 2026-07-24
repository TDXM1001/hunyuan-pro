import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const currentDirectory = dirname(fileURLToPath(import.meta.url));

/**
 * 测试文件必须以源码位置为锚点，避免启动 Vitest 的目录改变文件定位结果。
 */
export const workspaceRoot = resolve(currentDirectory, '../../../..');
export const systemAppRoot = resolve(workspaceRoot, 'apps/hunyuan-system');

export function resolveWorkspacePath(...segments: string[]) {
  return resolve(workspaceRoot, ...segments);
}

export function resolveSystemAppPath(...segments: string[]) {
  return resolve(systemAppRoot, ...segments);
}
