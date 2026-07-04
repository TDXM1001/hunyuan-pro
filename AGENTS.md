# Agent Instructions for Hunyuan Pro

## Working Rules
- Make one incremental change at a time.
- Explain why a change is needed before editing files.
- Prefer existing project patterns over new abstractions.
- Do not add new dependencies without explicit approval.
- Keep changes tightly scoped to the task.
- Verify every meaningful change with a concrete check.

## Frontend Page Rules
- When creating or changing frontend edit/detail pages, read `docs/frontend-edit-detail-page-standard.md` first.
- When creating or changing frontend list/search/table pages, read `docs/frontend-list-table-page-standard.md` first.
- Use the existing `@vben/art-hooks` edit/detail page primitives before inventing new page shells.
- Treat `hunyuan-design/apps/web-ele/src/views/demos/edit-test.vue` and `hunyuan-design/apps/web-ele/src/views/demos/detail-test.vue` as the current reference layout.
- Keep edit/detail pages quiet, dense, and operational: page header, status/extra slot, action area, sectioned content, and targeted validation.
- For ordinary menu-backed list/search/table pages, do not add explanatory page copy or standalone page title blocks that repeat the menu or obvious page purpose; if the menu/tab context is already clear, default to no extra title or description.
- If a list/search page only has one natural search row, disable collapse behavior instead of showing an unnecessary `展开 / 收起` toggle.
- Do not turn the reference pages into broad generators unless the repeated page code proves the abstraction is needed.

## Current Goal
- Harden the backend foundation.
- Focus on `system`, `file`, `sms`, `message`, `mail`, and shared platform rules.
- Keep business modules out of the foundation work.

## Success Criteria
- Foundation boundaries are clear.
- File handling is reliable and documented.
- SMS has a real minimal implementation path.
- Platform rules are consistent and testable.

## Verification
- Use targeted checks first.
- Prefer existing test commands when possible.
- Do not ship changes you cannot explain.
- For frontend edit/detail page changes, prefer `pnpm --dir hunyuan-design -F @vben/web-ele run typecheck` as the first contract check.
