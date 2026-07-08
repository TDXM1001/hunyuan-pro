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
- Treat the frontend standards as default baselines, not rigid templates. When a page has a stronger business prototype, sibling pattern, or subordinate-detail drawer workflow, follow the stronger scene-specific pattern and explain why it overrides the default baseline.
- Use the existing `@vben/art-hooks` edit/detail page primitives before inventing new page shells.
- Treat `hunyuan-design/apps/web-ele/src/views/demos/edit-test.vue` and `hunyuan-design/apps/web-ele/src/views/demos/detail-test.vue` as the current reference layout.
- Keep edit/detail pages quiet, dense, and operational: page header, status/extra slot, action area, sectioned content, and targeted validation.
- For ordinary menu-backed list/search/table pages, do not add explanatory page copy or standalone page title blocks that repeat the menu or obvious page purpose; if the menu/tab context is already clear, default to no extra title or description.
- If a list/search page only has one natural search row, disable collapse behavior instead of showing an unnecessary `展开 / 收起` toggle.
- When subordinate data needs to keep the parent list context visible, prefer existing drawer/side-surface patterns over forcing a permanent split layout.
- Do not turn the reference pages into broad generators unless the repeated page code proves the abstraction is needed.

## Current Goal
- Harden the backend foundation.
- Focus on `system`, `file`, `sms`, `message`, `mail`, and shared platform rules.
- Keep business modules out of the foundation work.

## BPM Reference Development Rules
- For BPM/process-engine work, use `E:\my-project\huanyuan-pro-jichu\yudao-ui-admin-vue3-master` as the frontend reference line and `E:\my-project\huanyuan-pro-jichu\ruoyi-vue-pro-master` as the backend reference line.
- Treat those repositories as reference material only: borrow component behavior, page structure, interaction patterns, API semantics, validation ideas, and backend processing mechanisms after understanding them.
- All production code, contracts, routes, permissions, menus, tests, docs, and verification artifacts must be completed in the current repository `E:\my-project\hunyuan-pro`.
- Do not wholesale-migrate Yudao/RuoYi code, names, API contracts, page shells, dependency assumptions, or module boundaries into Hunyuan.
- Keep Flowable and third-party BPM concepts behind Hunyuan BPM boundaries; external consumers should see Hunyuan names, Hunyuan IDs, Hunyuan org/employee integration, and Hunyuan page patterns.
- When a reference implementation is useful, first identify the mechanism being borrowed, then implement the smallest Hunyuan-native version that fits the current module and can be verified.

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
- For frontend business-flow checks, use the configured `playwright` MCP server when browser proof is useful. The local MCP checkout is `G:\code-mcp\playwright-mcp-temp`, and its runtime/cache/output files must stay under that directory (`cache/` and `runtime/`) instead of being written into this repo.
- For visible browser business-flow checks, keep one browser session alive and reuse it. Prefer the persistent Playwright MCP controller at `http://localhost:8934` when it is running; it holds a live MCP client connected to `http://localhost:8933/mcp` so browser contexts are not closed between tests. Do not write one-off Playwright MCP scripts that call `client.close()` for visible browser checks unless the user explicitly requests a throwaway run.
- If the persistent controller is not running, start `G:\code-mcp\playwright-mcp-temp\local-scripts\start-http.ps1` for the MCP server and then start the controller under `G:\code-mcp\playwright-mcp-temp\runtime\persistent-mcp-controller.cjs`. Use `start-stdio.ps1` only when the user explicitly requests a one-off stdio run.
- Before running Playwright MCP business-flow checks, make sure the required local services are available, especially frontend `http://127.0.0.1:5788` and backend `http://127.0.0.1:1024` when the flow needs real APIs.
- Treat Playwright MCP screenshots, network logs, saved sessions, browser profiles, and temporary output as runtime evidence. Do not commit those artifacts unless the user explicitly asks for an evidence bundle.
