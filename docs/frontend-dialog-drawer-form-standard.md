# Frontend Dialog and Drawer Form Standard

## Purpose

This standard defines the default rules for business forms that open inside dialogs or drawers instead of full pages.

These surfaces are common in Hunyuan Pro system management flows and must follow one predictable operational pattern.

## Parent Standards

This document inherits from:

- `docs/frontend-foundation-style-standard.md`
- `docs/frontend-list-table-page-standard.md`

## Reference Implementations

- `hunyuan-design/apps/hunyuan-system/src/views/system/employee/components/employee-form.vue`

## When To Use This Pattern

Use a dialog or drawer form when:

- the form is short to medium length
- the user is staying inside a list workflow
- the action is operational and focused
- the user should return quickly to the source list after completion

Do not use this pattern when:

- the form is long and section-heavy
- the user must compare a lot of surrounding business context
- the workflow already needs a page-level edit or detail structure

## Surface Choice Rules

### Prefer Dialog

Use dialog when:

- the form is compact
- the task is create, quick edit, confirm, or small-scope maintenance
- the primary mental model is “complete this and go back”

### Prefer Drawer

Use drawer when:

- the form is longer
- the user benefits from keeping more of the source page visible
- the content may grow with auxiliary descriptions, tabs, or related embedded sections

## Baseline Dialog Rules

Observed stable baseline from the employee form:

- dialog width: `600px`
- footer actions right-aligned by default
- form controls use full available row width

Rules:

- Start with `600px` for ordinary admin create or edit dialogs.
- Use wider widths only when field count or inline structure proves the need.
- Avoid using oversized dialog shells as a substitute for a proper edit page.

## Form Layout Rules

- Default to a single-column dialog form.
- Use full-width `ElInput` and `ElSelect` controls unless there is a strong reason for inline pairing.
- Keep labels predictable and aligned.
- Validation should remain explicit and field-level.

Rules:

- Dialog forms should optimize for fast scanning and completion.
- Do not create dense multi-column grids inside a compact dialog unless the business case is very strong.

## Action Rules

- Footer actions should stay simple: cancel plus primary confirm in the normal state.
- Primary confirm should remain visually strongest.
- Success paths that create follow-up output may replace the form with a result surface, but the action model should still stay simple.

Observed employee-form pattern:

- normal state: `取消` + `确定`
- result state: `复制密码` + `关闭`

Rules:

- Keep footer actions at low count.
- Avoid more than two primary-level decisions in the footer.

## Result-State Rules

If the dialog transitions into a post-submit result state:

- the success state should stay inside the same dialog when it is brief and actionable
- the result surface should remain visually calmer than a full success page
- the next actions should be explicit and limited

Observed stable values in the employee dialog result:

- top success block padding: `24px 0`
- success icon size: `80px`
- result card radius: `8px`
- result-card max width: `400px`

Rules:

- Use result-state replacement only when the result contains important next-step information.
- If the user only needs a toast and return, close the dialog instead of inventing a result panel.

## Content Density Rules

- Dialog forms should feel denser than page-level edit screens.
- Keep descriptive copy short.
- Use helper text only when the field meaning would otherwise be ambiguous.
- Avoid decorative banners, hero blocks, or large empty top padding.

## Visual Baseline

- Follow the foundation standard for radius, control heights, and typography.
- Use `8px` as the default dialog internal card or result-surface radius.
- Use operational text sizes, not dashboard heading scales.
- Keep semantic colors tied to meaning: success for completion, primary for emphasis, destructive for danger.

## Relationship to Page Patterns

- Dialog and drawer forms complement list pages.
- They do not replace edit pages.
- If the business form keeps growing, the correct move is usually to promote it into an edit-page standard rather than stretching the dialog beyond its natural boundary.

## First Exemplar

The first operational exemplar is:

- `hunyuan-design/apps/hunyuan-system/src/views/system/employee/components/employee-form.vue`

Why:

- it already covers create/edit behavior, validation, submit state, and a success-result variant inside one surface

## Verification

For meaningful dialog or drawer form changes:

1. Read this document and the foundation standard first.
2. Confirm whether the workflow still belongs in a dialog or should become a page.
3. Keep footer actions simple and verify that the dialog remains operational rather than decorative.
4. Run `pnpm --dir hunyuan-design -F @vben/web-ele run typecheck` after shared frontend changes.
