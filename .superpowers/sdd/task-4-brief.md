## Task 4: Verify the Permission Loop and Record the Result

**Files:**
- Modify only if verification reveals a direct issue:
  - `hunyuan-design/apps/hunyuan-system/src/views/system/menu/index.vue`
  - `hunyuan-design/apps/hunyuan-system/src/api/system/menu.ts`
  - `hunyuan-design/apps/hunyuan-system/src/views/system/organization-modules.test.ts`

**Interfaces:**
- Consumes:
  - existing backend-menu loading in `apps/hunyuan-system/src/router/access.ts`
  - existing role authorization APIs in `apps/hunyuan-system/src/api/system/organization.ts`
  - existing `module-bridge`
- Produces:
  - verified first-stage menu-management increment

- [ ] **Step 1: Run all targeted frontend checks**

Run:

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/organization-modules.test.ts --dom
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

Expected: both PASS.

- [ ] **Step 2: Review the permission loop from source**

Confirm these exact source facts:

```bash
rg -n "getAllMenusApi|fetchMenuListAsync|module-bridge|getRoleSelectedMenu|updateRoleMenu|SystemMenuManagement" hunyuan-design/apps/hunyuan-system/src
```

Expected output includes:

- `apps/hunyuan-system/src/router/access.ts` calling `getAllMenusApi()`.
- `apps/hunyuan-system/src/views/system/module-bridge/index.vue` still present.
- `apps/hunyuan-system/src/views/system/role/index.vue` using `getRoleSelectedMenu` and `updateRoleMenu`.
- `apps/hunyuan-system/src/views/system/menu/index.vue` defining `SystemMenuManagement`.

- [ ] **Step 3: Check final working tree**

Run:

```bash
git status --short
```

Expected: only intended files modified, plus pre-existing unrelated untracked files if they were already present:

- `.playwright-mcp/`
- `hunyuan-system-home-snapshot.md`
- `lefthook.yml`

- [ ] **Step 4: Commit any final correction**

If Task 4 required a correction, commit it:

```bash
git add \
  hunyuan-design/apps/hunyuan-system/src/api/system/menu.ts \
  hunyuan-design/apps/hunyuan-system/src/views/system/menu/index.vue \
  hunyuan-design/apps/hunyuan-system/src/views/system/organization-modules.test.ts
git commit -m "fix: tighten system menu permission loop"
```

If no correction was needed, do not create an empty commit.

