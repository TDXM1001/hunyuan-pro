# Frontend Foundation Style Standard

## Purpose

This document defines the foundation-level UI baseline for Hunyuan Pro. It is the parent standard for all later page-pattern standards.

Use this document to answer questions such as:

- what spacing scale we should use
- what control heights are allowed
- what radius scale we should use
- what typography sizes are the default
- what the default surface and border rhythm should be

This document should remain stable and low-churn. Page-level standards should inherit from it instead of redefining the basics.

## Source of Truth

This baseline is derived from the current repo implementation, not from abstract design-system theory.

Primary evidence:

- `hunyuan-design/internal/tailwind-config/src/theme.css`
- `hunyuan-design/packages/@core/base/design/src/design-tokens/default.css`
- `hunyuan-design/packages/@vben/art-hooks/src/common/components/art-search-panel/style.css`
- `hunyuan-design/packages/@vben/art-hooks/src/table/components/art-table-header/style.css`
- `hunyuan-design/packages/@vben/art-hooks/src/table/components/art-table/style.css`
- `hunyuan-design/packages/@vben/art-hooks/src/edit/components/art-edit-page/style.css`
- `hunyuan-design/packages/@vben/art-hooks/src/detail/components/art-detail-page/style.css`
- `hunyuan-design/apps/hunyuan-system/src/views/system/employee/components/employee-table-panel.vue`
- `hunyuan-design/apps/web-ele/src/views/demos/table-test.vue`

## Foundation Principles

- Prefer dense, calm, operational admin UI over decorative marketing layouts.
- Prefer one shared baseline over per-page visual improvisation.
- Prefer tokens and shared primitives over page-local magic numbers.
- Prefer a small number of allowed sizes over many similar sizes.

## Global Base Rules

- Root font size: `16px`
- Default body surface: `var(--background)`
- Main app content background: `var(--background-deep)`
- Default card surface: `var(--card)`
- Default border token: `var(--border)`
- Base radius token: `var(--radius)` = `0.5rem` = `8px`
- Root layout should use full height and hidden browser overflow; scrolling should be owned by the app content surfaces.

## Spacing Scale

Use these values as the approved default spacing scale for system pages:

- `4px`: very tight inline adjustments only
- `8px`: tight inline gap, tag gap, small helper spacing
- `10px`: compact toolbar tool gap
- `12px`: compact inner block gap, page content inset for dense business list pages
- `16px`: default major gap between sections, cards, and header groups
- `18px`: special content padding or toolbar-to-body separation inside dense panels
- `24px`: standard card vertical padding and relaxed section spacing
- `26px`: standard business card horizontal padding when a dense but not cramped form or table is needed

Rules:

- Default to `16px` before considering any other gap.
- Use `8px` and `12px` for dense internal structures, not for major page skeleton spacing.
- Use `24px` and `26px` mainly for card body padding, not for every generic container.

## Radius Scale

Approved radius levels:

- `4px`: very small local chips or status markers
- `6px`: compact upload/item micro-surfaces when already used by a shared component
- `8px`: default button, card, input, toolbar, and business panel radius
- `10px+`: only for exceptional shared components that already establish their own stronger shape language

Rules:

- `8px` is the system default.
- Do not introduce `12px`, `14px`, or larger radii into ordinary business pages unless the page family standard explicitly allows it.

## Control Height Scale

Approved default control heights:

- `40px`: primary standard for search actions, header actions, and toolbar tools
- `48px`: section headers and edit/detail section title bars where a stronger structural rhythm is needed
- `52px`: only when a reference page or shared form shell explicitly needs a larger edit-page control surface
- `34px`: pagination control size inside dense tables

Rules:

- If a control is part of list-page search or toolbar action flow, start with `40px`.
- Do not mix `36px` and `40px` controls in the same primary toolbar row.
- Tool buttons should match the height of the primary action anchor rather than visually overpower it.

## Typography Scale

Approved default typography baseline:

- Base text: `14px`
- Secondary helper text: `13px`
- Section title: `16px`
- Large page heading: `24px` only for landing or dashboard-like pages

Typical line-height pairing:

- `13px / 22px` for muted descriptions
- `14px / normal admin rhythm` for form and table content
- `16px / 24px` for section titles

Rules:

- Treat `14px` as the operational default for tables and forms.
- Use `13px` for secondary descriptions, help text, and muted annotations.
- Use `16px` for section headings inside business surfaces.
- Do not escalate to larger titles inside ordinary list or form cards unless the page family standard allows it.

## Surface and Border Rules

- Business cards should default to a quiet white surface with a light border.
- Default business-card radius should be `8px`.
- Search cards and table cards in dense business pages should prefer a light-border look over heavy shadows.
- Neutral toolbar tools should use a quiet background and `1px` border until hover or active state.

Rules:

- Prefer border-defined separation before adding shadow-defined separation.
- Use visual emphasis on meaning, not on chrome.

## Button and Toolbar Baseline

- Primary, secondary, and search action buttons in list pages should default to `40px` height.
- Default horizontal padding for primary toolbar buttons: `18px`
- Shared tool buttons: `40px x 40px`
- Shared tool icon size: `16px`
- Default tool gap: `10px`
- Default cluster separation between left business actions and right shared tools: `16px`

Rules:

- The primary left action is the anchor of the toolbar.
- Shared utility tools should visually align to the primary action but remain lower in emphasis.
- Table row actions are intentionally lighter than page-level buttons and should stay compact.

## Card and Panel Padding Baseline

Observed stable business baseline:

- Search card body padding: `24px 26px`
- Table card body padding: `24px 26px 18px`
- Dense table panel inner padding: `16px`
- Edit/detail page header padding: `16px 18px`
- Edit/detail page footer padding: `12px 18px`

Rules:

- Use the business list-page paddings for operational search and table pages.
- Use the edit/detail paddings only in edit/detail family pages.
- Do not borrow dashboard or information-card padding into dense admin list pages without a reason.

## Color and Semantic Usage

Use semantic tokens first:

- primary: action emphasis
- success: positive state or completion
- warning: caution or reversible danger
- destructive: destructive actions and error emphasis
- muted/secondary: passive text and low-priority UI

Rules:

- Do not hardcode brand colors in business pages when semantic tokens already exist.
- Status colors should reflect meaning consistently across tags, buttons, alerts, and table operations.

## Scroll and Container Rules

- Browser-level scrolling should be avoided in the main application shell.
- Major app containers should use full height with `min-h-0` where vertical flex layouts are involved.
- Inner page bodies, table surfaces, and detail/edit content areas should own scrolling.

Rules:

- If a page shows a browser scrollbar unexpectedly, inspect the height chain before adding one-off overflow hacks.

## What This Foundation Standard Covers

This document is normative for:

- spacing scale
- radius scale
- control heights
- typography baseline
- neutral surface rhythm
- toolbar emphasis order

This document does not fully define:

- authentication layouts
- dashboard composition
- tree + list page composition
- edit/detail composition
- drawer/dialog form behavior

Those belong to page-family standards.

## Follow-On Standards

This document should feed the next standards in order:

1. `docs/frontend-list-table-page-standard.md`
2. `docs/frontend-tree-list-page-standard.md`
3. `docs/frontend-dialog-drawer-form-standard.md`
4. `docs/frontend-auth-page-standard.md`
5. `docs/frontend-profile-page-standard.md`
6. `docs/frontend-status-fallback-page-standard.md`

## Verification

Before changing foundation-level styles:

1. Verify the proposed value already appears in a stable shared component or canonical reference page.
2. Prefer promoting an existing stable value over inventing a new one.
3. Check which page families inherit the shared primitive before changing it.
4. Run `pnpm --dir hunyuan-design -F @vben/web-ele run typecheck` after shared frontend changes.
