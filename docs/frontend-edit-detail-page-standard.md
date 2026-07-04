# Frontend Edit and Detail Page Standard

## Purpose

This standard captures the current Hunyuan Pro edit/detail page layout that future Codex work should follow. It is intentionally narrow: preserve the project style, avoid new dependencies, and make page work verifiable.

## Reference Pages

- Edit page reference: `hunyuan-design/apps/web-ele/src/views/demos/edit-test.vue`
- Detail page reference: `hunyuan-design/apps/web-ele/src/views/demos/detail-test.vue`
- Drawer capability demo: `hunyuan-design/apps/web-ele/src/views/demos/form/basic.vue` (API capability only, not a business-style reference by itself)
- Business drawer detail reference: `hunyuan-design/apps/hunyuan-system/src/views/support/dict/components/dict-data-drawer.vue`
- Shared edit components: `hunyuan-design/packages/@vben/art-hooks/src/edit/components/`
- Shared detail components: `hunyuan-design/packages/@vben/art-hooks/src/detail/components/`

## Layout Contract

Use the existing page primitives first:

- Edit pages should use `ArtEditPage` plus `ArtEditSection`.
- Detail pages should use `ArtDetailPage` plus `ArtDetail`.
- When a detail or subordinate CRUD surface needs to preserve the parent list context, an established drawer/side-surface pattern is acceptable. In that case, keep the same density and component vocabulary instead of forcing a full routed detail page.
- Use the outer full-content wrapper pattern from the reference pages: full available height, `min-h-0`, hidden outer overflow, and a scrollable page body.
- Keep the page header stable: back slot on the left, title, optional status/extra slot, and right-aligned actions.
- Use sectioned content instead of a long flat form or flat detail list.
- Prefer three columns on desktop, two columns on tablet, and one column on mobile through the shared component styles.
- Use full-width rows for long text, embedded tables, upload areas, and other dense content.

## Edit Page Rules

- Use `ElForm` with `label-position="top"` inside `ArtEditPage`.
- Group form fields by business meaning using `ArtEditSection`.
- Keep common actions in the page header actions slot: reset, save draft, save, submit, or other workflow actions.
- Use `ArtTable` inside a full-width section item when an edit form needs inline rule/detail rows.
- Use `ArtAttachmentUpload` for attachment-like fields unless a real business upload component already exists.
- Keep field-level validation explicit with Element Plus `FormRules`.
- Do not create a generic form generator only for one or two pages. Extract only after repeated pages prove the same structure.

## Detail Page Rules

- Prefer a `sections` configuration with `DetailSection<T>[]`.
- Use `prop` for ordinary fields, `formatter` for simple display transforms, and slots for tags, status, tables, file lists, or other rich display.
- Set `span` deliberately for long values or embedded content.
- Keep status and tag rendering consistent with Element Plus `ElTag`.
- Use `emptyText` or the component default instead of ad hoc placeholder text.
- Keep detail pages read-oriented. Editing actions should route to an edit page or open an established edit surface.

## Style Boundaries

- Use project CSS variables such as `var(--radius)`, `var(--el-bg-color)`, `var(--el-fill-color-lighter)`, and `var(--el-border-color-lighter)`.
- Keep visual density suitable for an admin system. Avoid marketing-style hero layouts, decorative gradients, oversized cards, or one-off visual systems.
- Keep page copy minimal. Use titles and section names to carry meaning. Add descriptions only when the page would otherwise be ambiguous.
- Do not add new UI libraries or icon packages. Use existing Element Plus and project UI primitives.

## When Not To Use This Pattern

- Do not use this pattern for list/search pages. Follow `docs/frontend-list-table-page-standard.md` and the existing table primitives instead.
- Do not use it for dashboards, reports, authentication, or visual landing pages.
- Do not use the drawer API demo alone as a style reference for business pages. It only shows the mechanics of `useVbenDrawer`; business pages still need scene-specific structure and copy discipline.
- Do not force it onto a workflow that already has a stronger established sibling page pattern.

## Verification

For any meaningful edit/detail page change:

1. Read the reference page and shared component implementation before editing.
2. Make the smallest page or component change that satisfies the task.
3. Run `pnpm --dir hunyuan-design -F @vben/web-ele run typecheck`.
4. If the change affects shared components, inspect every direct consumer with `rg` and run the narrowest useful additional check.
5. Explain what was changed, why it matches this standard, and what verification passed.
