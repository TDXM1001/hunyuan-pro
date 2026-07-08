## Task 1: Lock the Menu Management Contract with Source Tests

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/organization-modules.test.ts`

**Interfaces:**
- Consumes: existing source-test pattern using `existsSync`, `readFileSync`, `resolve`, `describe`, `expect`, `it`.
- Produces: tests that later tasks must satisfy:
  - `apps/hunyuan-system/src/api/system/menu.ts` exists.
  - `apps/hunyuan-system/src/views/system/menu/index.vue` exists.
  - menu page uses shared table/search primitives.
  - menu page exposes `path`, `component`, `webPerms`, and `apiPerms`.
  - menu page stays quiet without hero/title/description blocks.

- [ ] **Step 1: Add failing test cases**

Append the following constants near the existing path constants:

```ts
const menuPagePath = 'apps/hunyuan-system/src/views/system/menu/index.vue';
const menuApiPath = 'apps/hunyuan-system/src/api/system/menu.ts';
```

Append these tests inside `describe('organization backend menu docking pages', () => { ... })`:

```ts
  it('provides a real menu management page', () => {
    const pagePath = resolve(process.cwd(), menuPagePath);

    expect(existsSync(pagePath)).toBe(true);

    const source = readFileSync(pagePath, 'utf8');
    expect(source).toContain('ArtSearchPanel');
    expect(source).toContain('ArtTablePanel');
    expect(source).toContain('ArtTableHeader');
    expect(source).toContain('ArtTable');
    expect(source).toContain('row-key="menuId"');
    expect(source).toContain(':tree-props="{ children: \'children\' }"');
  });

  it('keeps menu management dense without extra page title or explainer copy', () => {
    const source = readFileSync(resolve(process.cwd(), menuPagePath), 'utf8');

    expect(source).not.toContain('menu-page__title');
    expect(source).not.toContain('menu-page__hero');
    expect(source).not.toContain('menu-page__desc');
    expect(source).toContain(':collapsible="false"');
  });

  it('wires menu management to backend menu endpoints', () => {
    const apiPath = resolve(process.cwd(), menuApiPath);

    expect(existsSync(apiPath)).toBe(true);

    const source = readFileSync(apiPath, 'utf8');
    expect(source).toContain("'/menu/query'");
    expect(source).toContain("'/menu/tree'");
    expect(source).toContain("'/menu/add'");
    expect(source).toContain("'/menu/update'");
    expect(source).toContain("'/menu/batchDelete'");
    expect(source).toContain("'/menu/auth/url'");
  });

  it('surfaces route, component, and permission fields on the menu page', () => {
    const source = readFileSync(resolve(process.cwd(), menuPagePath), 'utf8');

    expect(source).toContain('path');
    expect(source).toContain('component');
    expect(source).toContain('webPerms');
    expect(source).toContain('apiPerms');
    expect(source).toContain('frameFlag');
    expect(source).toContain('visibleFlag');
    expect(source).toContain('disabledFlag');
  });

  it('keeps menu row actions compact and measurable', () => {
    const source = readFileSync(resolve(process.cwd(), menuPagePath), 'utf8');

    expect(source).toContain('class="menu-page__actions"');
    expect(source).toContain('.menu-page__actions {');
    expect(source).toContain('display: inline-flex;');
    expect(source).toContain('gap: 8px;');
    expect(source).toContain('.menu-page__actions :deep(.el-button)');
    expect(source).toContain('font-size: 14px;');
    expect(source).toContain('line-height: 22px;');
    expect(source).toContain('padding: 0;');
    expect(source).toContain('.menu-page__actions :deep(.el-button + .el-button)');
    expect(source).toContain('margin-left: 0;');
  });
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/organization-modules.test.ts --dom
```

Expected: FAIL because `apps/hunyuan-system/src/api/system/menu.ts` and `apps/hunyuan-system/src/views/system/menu/index.vue` do not exist yet.

- [ ] **Step 3: Commit the failing contract only after confirming failure is expected**

Do not commit this task alone unless the implementation will be paused. If committing is needed, use:

```bash
git add hunyuan-design/apps/hunyuan-system/src/views/system/organization-modules.test.ts
git commit -m "test: add menu management frontend contract"
```

