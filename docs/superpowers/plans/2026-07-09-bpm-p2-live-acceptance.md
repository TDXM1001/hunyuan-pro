# BPM P2 Live Acceptance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce a real P2 live acceptance record proving the existing Hunyuan BPM sample expense flow can be started, observed, failed once, retried, and reconciled through the existing reliability surfaces.

**Architecture:** This is an acceptance and evidence slice, not a feature slice. Reuse the current `sample_expense` API, BPM runtime approval API, integration callback query/retry API, admin instance trace API, and persistent Playwright MCP browser session. Runtime scripts and browser evidence stay under `G:\code-mcp\playwright-mcp-temp`; only the final Markdown acceptance record is committed to this repository.

**Tech Stack:** Java 17, Spring Boot 3, Maven, Vue 3, TypeScript, Vitest, Playwright MCP persistent controller, PowerShell, UTF-8 Markdown.

## Global Constraints

- Production code, contracts, routes, permissions, menus, tests, docs, and verification artifacts must stay in `E:\my-project\hunyuan-pro`.
- Yudao and RuoYi are reference lines only; borrow mechanisms, not code or API names.
- Public Hunyuan BPM APIs must not expose Flowable native objects, names, or IDs.
- Do not add new dependencies.
- Do not add a sample expense menu or complex frontend page in this slice.
- Do not create a generic event bus, MQ callback platform, HTTP callback node platform, or external scheduler.
- Do not commit Playwright screenshots, network logs, browser profiles, or runtime output unless the user explicitly asks for an evidence bundle.
- Playwright MCP runtime/cache/output files must stay under `G:\code-mcp\playwright-mcp-temp\cache` or `G:\code-mcp\playwright-mcp-temp\runtime`.
- For visible browser business-flow checks, keep one browser session alive and reuse it; prefer the persistent Playwright MCP controller at `http://localhost:8934`.
- Verification output and handoff must be Chinese and UTF-8 safe.

---

## File Structure

- Create `docs/superpowers/specs/2026-07-09-bpm-p2-live-acceptance.md`: final P2 live acceptance record with actual evidence, outcomes, blockers if any, and source gate results.
- Runtime-only helper location: `G:\code-mcp\playwright-mcp-temp\runtime\hunyuan-p2-live-acceptance-<timestamp>.json`. This file records live API/browser evidence and is not committed.
- Runtime-only browser/MCP outputs: `G:\code-mcp\playwright-mcp-temp\cache` and `G:\code-mcp\playwright-mcp-temp\runtime`. These files are not committed.

No production Java, TypeScript, Vue, SQL, route, menu, or permission file should be changed by this plan.

---

### Task 1: Preflight Environment and Repository State

**Files:**
- Read: `E:/my-project/hunyuan-pro/AGENTS.md`
- Read: `E:/my-project/hunyuan-pro/docs/superpowers/specs/2026-07-09-bpm-p2-live-acceptance-design.md`
- Read: `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/vite.config.ts`
- Runtime output only: `G:/code-mcp/playwright-mcp-temp/runtime/hunyuan-p2-live-preflight-<timestamp>.json`

**Interfaces:**
- Consumes:
  - Frontend service: `http://127.0.0.1:5788`
  - Backend service: `http://127.0.0.1:1024`
  - Persistent Playwright MCP controller: `http://localhost:8934`
  - MCP server endpoint behind controller: `http://localhost:8933/mcp`
- Produces:
  - A preflight verdict: `READY`, `BLOCKED_SERVICES`, `BLOCKED_MCP`, or `BLOCKED_REPO`
  - Concrete service status evidence for the acceptance record

- [ ] **Step 1: Confirm the repository is clean before live evidence work**

Run:

```powershell
git status --short
git branch --show-current
git log --oneline -5
```

Expected:

- `git status --short` prints no implementation changes.
- Current branch is the user-approved current branch, normally `main`.
- Recent commits include `60d9b78d docs: 增加 BPM P2 收官活体验收设计` and `fc3853b0 test: 验证 BPM 样板回调执行闭环`.

If the worktree is dirty, stop and classify every dirty file before proceeding. Do not overwrite or revert user changes.

- [ ] **Step 2: Confirm the Hunyuan frontend proxy and service ports**

Run:

```powershell
Get-Content -Raw -Encoding UTF8 E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/vite.config.ts
Get-Content -Raw -Encoding UTF8 E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/.env.development
```

Expected:

- `.env.development` contains `VITE_GLOB_API_URL=/api`.
- `vite.config.ts` proxies `/api` to the local backend target and rewrites `/api` away before reaching backend controllers.

- [ ] **Step 3: Check backend and frontend HTTP availability**

Run:

```powershell
$serviceEvidence = [ordered]@{}
try {
  $backend = Invoke-WebRequest -UseBasicParsing -TimeoutSec 5 -Uri 'http://127.0.0.1:1024/'
  $serviceEvidence.backend = @{ ok = $true; status = [int]$backend.StatusCode; url = 'http://127.0.0.1:1024/' }
} catch {
  $serviceEvidence.backend = @{ ok = $false; error = $_.Exception.Message; url = 'http://127.0.0.1:1024/' }
}
try {
  $frontend = Invoke-WebRequest -UseBasicParsing -TimeoutSec 5 -Uri 'http://127.0.0.1:5788/'
  $serviceEvidence.frontend = @{ ok = $true; status = [int]$frontend.StatusCode; url = 'http://127.0.0.1:5788/' }
} catch {
  $serviceEvidence.frontend = @{ ok = $false; error = $_.Exception.Message; url = 'http://127.0.0.1:5788/' }
}
$serviceEvidence | ConvertTo-Json -Depth 5
```

Expected:

- Frontend returns HTTP 200 or another reachable HTTP response that proves the dev server is up.
- Backend returns a reachable HTTP response. HTTP 404 is acceptable for `/` if it proves the server responded; connection refused is a blocker.

If either service is unreachable, record `BLOCKED_SERVICES` in the acceptance record and do not fabricate browser proof.

- [ ] **Step 4: Check persistent Playwright MCP controller availability**

Run:

```powershell
try {
  $controller = Invoke-WebRequest -UseBasicParsing -TimeoutSec 5 -Uri 'http://localhost:8934/health'
  @{ ok = $true; status = [int]$controller.StatusCode; url = 'http://localhost:8934/health' } | ConvertTo-Json
} catch {
  @{ ok = $false; error = $_.Exception.Message; url = 'http://localhost:8934/health' } | ConvertTo-Json
}
```

Expected:

- If controller responds, use it for visible browser work.
- If controller is not running, start it according to `AGENTS.md`:

```powershell
Start-Process -FilePath powershell.exe -WindowStyle Hidden -ArgumentList '-NoProfile','-ExecutionPolicy','Bypass','-File','G:\code-mcp\playwright-mcp-temp\local-scripts\start-http.ps1'
Start-Sleep -Seconds 3
Start-Process -FilePath node.exe -WindowStyle Hidden -ArgumentList 'G:\code-mcp\playwright-mcp-temp\runtime\persistent-mcp-controller.cjs'
Start-Sleep -Seconds 3
Invoke-WebRequest -UseBasicParsing -TimeoutSec 5 -Uri 'http://localhost:8934/health'
```

If the controller still cannot be reached, record `BLOCKED_MCP`. Backend/API-only proof may still be useful, but it must not be reported as browser acceptance.

- [ ] **Step 5: Save preflight runtime evidence outside the repo**

Run:

```powershell
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$runtimeDir = 'G:/code-mcp/playwright-mcp-temp/runtime'
New-Item -ItemType Directory -Force -Path $runtimeDir | Out-Null
$preflightPath = Join-Path $runtimeDir "hunyuan-p2-live-preflight-$timestamp.json"
$preflight = [ordered]@{
  timestamp = (Get-Date).ToString('o')
  repo = 'E:/my-project/hunyuan-pro'
  frontend = 'http://127.0.0.1:5788'
  backend = 'http://127.0.0.1:1024'
  mcpController = 'http://localhost:8934'
}
$preflight | ConvertTo-Json -Depth 5 | Set-Content -Encoding UTF8 -Path $preflightPath
Write-Output $preflightPath
```

Expected:

- A JSON file is written under `G:\code-mcp\playwright-mcp-temp\runtime`.
- No runtime evidence is written under `E:\my-project\hunyuan-pro`.

---

### Task 2: Source Gates Before Live Acceptance

**Files:**
- Read only: Maven and frontend source/test files touched by P2.1-P2.4
- Runtime output only: `G:/code-mcp/playwright-mcp-temp/runtime/hunyuan-p2-live-gates-<timestamp>.json`

**Interfaces:**
- Consumes:
  - Existing P2.4 source gates
- Produces:
  - Fresh gate evidence for `docs/superpowers/specs/2026-07-09-bpm-p2-live-acceptance.md`
  - Runtime gate summary JSON outside the repository

- [ ] **Step 1: Run full BPM backend gate**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test
```

Expected:

- `BUILD SUCCESS`
- `Tests run: 96, Failures: 0, Errors: 0, Skipped: 0` or a higher test count if new tests were added after this plan.

- [ ] **Step 2: Run frontend BPM source contract gate**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected:

- `Test Files  2 passed`
- `Tests  37 passed` or a higher count if new contract tests were added after this plan.

- [ ] **Step 3: Run Hunyuan system frontend typecheck**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected:

- Command exits 0.
- Output includes `vue-tsc --noEmit --skipLibCheck`.

- [ ] **Step 4: Run Flowable boundary compatibility gate**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin -Dtest=BpmFlowableCompatibilityTest test
```

Expected:

- `BUILD SUCCESS`
- `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`

If any source gate fails, stop and use `superpowers:systematic-debugging` before making changes. Do not proceed to live acceptance while source gates are red.

- [ ] **Step 5: Save source gate summary outside the repo**

After all four gates pass, write a UTF-8 JSON summary under `G:\code-mcp\playwright-mcp-temp\runtime`.

Run:

```powershell
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$runtimeDir = 'G:/code-mcp/playwright-mcp-temp/runtime'
New-Item -ItemType Directory -Force -Path $runtimeDir | Out-Null
$gatePath = Join-Path $runtimeDir "hunyuan-p2-live-gates-$timestamp.json"
$gateSummary = [ordered]@{
  timestamp = (Get-Date).ToString('o')
  bpmModule = 'PASS; mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test'
  frontendContracts = 'PASS; pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom'
  frontendTypecheck = 'PASS; pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck'
  flowableBoundary = 'PASS; mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin -Dtest=BpmFlowableCompatibilityTest test'
}
$gateSummary | ConvertTo-Json -Depth 5 | Set-Content -Encoding UTF8 -Path $gatePath
Write-Output $gatePath
```

Expected:

- The JSON file is written under `G:\code-mcp\playwright-mcp-temp\runtime`.
- No gate output file is written under the repository.

---

### Task 3: Live Sample Expense Callback Recovery Proof

**Files:**
- Runtime output only: `G:/code-mcp/playwright-mcp-temp/runtime/hunyuan-p2-live-acceptance-<timestamp>.json`
- Browser evidence only: `G:/code-mcp/playwright-mcp-temp/cache` or `G:/code-mcp/playwright-mcp-temp/runtime`
- No repository file changes

**Interfaces:**
- Consumes:
  - `POST /bpm/sample/expense/create`
  - `POST /bpm/sample/expense/markNextCallbackFailed/{expenseId}`
  - `POST /bpm/sample/expense/start/{expenseId}`
  - `GET /bpm/sample/expense/detail/{expenseId}`
  - `POST /bpm/integration/callback/query`
  - `POST /bpm/integration/callback/retry/{callbackRecordId}`
  - `GET /bpm/instance/trace/{instanceId}`
  - `POST /app/bpm/task/approve`
- Produces:
  - Runtime evidence JSON with `expenseId`, `instanceId`, `callbackRecordId`, callback status transition, and final sample expense status
  - Browser/page proof summary for callback list and instance reliability area

- [ ] **Step 1: Reuse or establish authenticated browser session**

Use the persistent Playwright MCP controller at `http://localhost:8934` to open:

```text
http://127.0.0.1:5788
```

Expected:

- If already logged in, continue.
- If redirected to login, log in with a local administrator account that has BPM integration permissions.
- Keep the browser session alive after the check.

Record in runtime evidence:

```json
{
  "browserSession": "persistent-controller",
  "frontendUrl": "http://127.0.0.1:5788",
  "loggedInAs": "admin-or-current-session-user"
}
```

- [ ] **Step 2: Confirm sample definition can be started**

In the browser or via authenticated API request from the active session, query definitions with `definitionKey = sample_expense_apply`.

Recommended API route:

```text
POST /api/bpm/definition/query
```

Request body:

```json
{
  "definitionKey": "sample_expense_apply",
  "pageNum": 1,
  "pageSize": 10
}
```

Expected:

- A current enabled definition exists for `sample_expense_apply`.
- If no definition exists, stop and write acceptance record status `BLOCKED_DEFINITION_MISSING`.

- [ ] **Step 3: Create a sample expense draft**

Use the authenticated browser session or an API helper that reuses the same token/cookies.

Request:

```http
POST /api/bpm/sample/expense/create
Content-Type: application/json

{
  "title": "P2收官活体验收费用申请",
  "amount": 128.50,
  "applicantEmployeeId": 1
}
```

Expected:

- Response success code is `0`.
- Response data is a numeric `expenseId`.

Record `expenseId`.

- [ ] **Step 4: Mark the next callback as failed**

Request:

```http
POST /api/bpm/sample/expense/markNextCallbackFailed/{expenseId}
```

Expected:

- Response success code is `0`.
- Response data is a success message.

Record that failure injection was enabled before starting the flow.

- [ ] **Step 5: Start the BPM instance from the sample expense**

Request:

```http
POST /api/bpm/sample/expense/start/{expenseId}
```

Expected:

- Response success code is `0`.
- Response data is a numeric `instanceId`.

Record `instanceId`.

If the response says the definition is missing or not startable, stop and write `BLOCKED_DEFINITION_MISSING` or `BLOCKED_DEFINITION_DISABLED`.

- [ ] **Step 6: Find and approve the created task**

Preferred browser route:

```text
http://127.0.0.1:5788/system/bpm/runtime/my-todo-list
```

Find the task for the newly created `instanceId` or title `P2收官活体验收费用申请`, then approve it through the UI.

If the UI approval prompt is unstable but the task row is visible, use the backend approval API for the same real task:

```http
POST /api/app/bpm/task/approve
Content-Type: application/json

{
  "taskId": 123,
  "commentText": "P2收官活体验收通过"
}
```

Expected:

- Instance reaches a terminal result state.
- If UI was used, record the page route and visible row/title.
- If API fallback was used, record the `taskId`, endpoint, and response summary.

Do not approve an unrelated task. The `taskId` must belong to the `instanceId` from Step 5.

- [ ] **Step 7: Query callback records for the sample instance**

Request:

```http
POST /api/bpm/integration/callback/query
Content-Type: application/json

{
  "businessType": "sample_expense",
  "eventId": "",
  "callbackStatus": 2,
  "pageNum": 1,
  "pageSize": 10
}
```

Expected after the first callback execution:

- A row exists with `businessType = "sample_expense"`, `businessId = expenseId`, and `instanceId = instanceId`.
- `callbackStatus = 2` means failed.
- `failureReason` contains `样板费用申请模拟回调失败`.
- `retryCount` is at least `1`.
- Capture `callbackRecordId`.

If the record is still pending because automatic execution has not run yet, trigger manual retry once and record that the first visible state was pending. If the first retry consumes the failure injection and returns failed, continue to Step 8. If no callback record exists, stop and investigate the business result event publishing chain.

- [ ] **Step 8: Verify failed callback is visible in browser reliability UI**

Use the browser to open the callback record list:

```text
http://127.0.0.1:5788/system/bpm/integration/callback-record-list
```

Search:

- 业务类型: `sample_expense`
- 回调状态: `失败`

Expected:

- A row for the current `callbackRecordId` or `eventId` is visible.
- The row shows `失败`, retry count, failure reason, and `重试` action.

Also open the admin instance detail drawer from the instance list if available:

```text
http://127.0.0.1:5788/system/bpm/runtime/instance-list
```

Expected:

- The current `instanceId` detail shows the reliability area.
- Callback records are visible for the instance.
- Notification records are visible when notification data exists for the instance. If no notification listener data exists in the local database, record this as an environment data limitation rather than a UI failure.

- [ ] **Step 9: Retry the failed callback through the existing path**

Preferred browser route:

- Click `重试` on the failed callback row in `/system/bpm/integration/callback-record-list`.

API fallback:

```http
POST /api/bpm/integration/callback/retry/{callbackRecordId}
```

Expected:

- Response success code is `0`.
- Callback record becomes `callbackStatus = 1` success.
- `responsePayloadJson` contains `{"approvalStatus":2}` for approval or `{"approvalStatus":3}` for rejection.

Record whether retry was triggered through the browser action or API fallback.

- [ ] **Step 10: Confirm sample expense final state**

Request:

```http
GET /api/bpm/sample/expense/detail/{expenseId}
```

Expected for an approved flow:

- `approvalStatus = 2`
- `instanceId` equals the recorded `instanceId`
- `callbackEventId` is not empty
- `approvedAt` is not empty
- `rejectedAt` is empty

If the flow was intentionally rejected instead:

- `approvalStatus = 3`
- `rejectedAt` is not empty
- `approvedAt` is empty

- [ ] **Step 11: Save live runtime evidence outside the repo**

Write a JSON evidence file under `G:\code-mcp\playwright-mcp-temp\runtime`.

Use this shape:

```json
{
  "status": "PASSED",
  "timestamp": "2026-07-09T00:00:00+08:00",
  "frontendUrl": "http://127.0.0.1:5788",
  "backendUrl": "http://127.0.0.1:1024",
  "mcpController": "http://localhost:8934",
  "expenseId": 1001,
  "instanceId": 88,
  "businessType": "sample_expense",
  "callbackRecordId": 1,
  "failureReason": "样板费用申请模拟回调失败",
  "retryPath": "browser",
  "finalApprovalStatus": 2,
  "callbackFinalStatus": 1,
  "uiEvidence": [
    "回调记录列表显示失败状态和重试按钮",
    "回调记录重试后显示成功状态",
    "实例详情可靠性区域显示回调记录"
  ],
  "limitations": []
}
```

Replace the numeric and status values with the actual values gathered in Steps 3-10. The saved runtime JSON file is evidence only and must not be committed.

---

### Task 4: Write and Commit the Live Acceptance Record

**Files:**
- Create: `docs/superpowers/specs/2026-07-09-bpm-p2-live-acceptance.md`

**Interfaces:**
- Consumes:
  - Source gate outputs from Task 2
  - Runtime evidence JSON from Task 3
  - Browser/page observations from Task 3
- Produces:
  - Final P2 live acceptance record

- [ ] **Step 1: Create the acceptance record from actual evidence**

Create `docs/superpowers/specs/2026-07-09-bpm-p2-live-acceptance.md` from the runtime evidence files. First load the newest matching evidence JSON files created by Task 2 and Task 3:

```powershell
$runtimeDir = 'G:/code-mcp/playwright-mcp-temp/runtime'
$liveEvidencePath = (Get-ChildItem -LiteralPath $runtimeDir -Filter 'hunyuan-p2-live-acceptance-*.json' |
  Sort-Object LastWriteTime -Descending |
  Select-Object -First 1).FullName
$gateEvidencePath = (Get-ChildItem -LiteralPath $runtimeDir -Filter 'hunyuan-p2-live-gates-*.json' |
  Sort-Object LastWriteTime -Descending |
  Select-Object -First 1).FullName
if (-not $liveEvidencePath) {
  throw '缺少 Task 3 生成的 hunyuan-p2-live-acceptance runtime 证据文件'
}
if (-not $gateEvidencePath) {
  throw '缺少 Task 2 生成的 hunyuan-p2-live-gates runtime 证据文件'
}
$live = Get-Content -Raw -Encoding UTF8 $liveEvidencePath | ConvertFrom-Json
$gates = Get-Content -Raw -Encoding UTF8 $gateEvidencePath | ConvertFrom-Json
$conclusion = if ($live.status -eq 'PASSED') { 'P2 收官活体验收通过。' } else { 'P2 收官活体验收阻塞。' }
$blockers = if ($live.blockers -and $live.blockers.Count -gt 0) {
  ($live.blockers | ForEach-Object { "- $_" }) -join "`n"
} else {
  '无'
}
$limitations = if ($live.limitations -and $live.limitations.Count -gt 0) {
  ($live.limitations | ForEach-Object { "- $_" }) -join "`n"
} else {
  '无'
}
$uiEvidence = if ($live.uiEvidence -and $live.uiEvidence.Count -gt 0) {
  ($live.uiEvidence | ForEach-Object { "- $_" }) -join "`n"
} else {
  '- 未记录 UI 证据；如果状态为 PASSED，必须补充浏览器页面证据后再提交。'
}
```

Then create the Markdown:

```powershell
$markdown = @"
# BPM P2 收官活体验收记录

## 结论

$conclusion

## 环境

- 前端：``$($live.frontendUrl)``
- 后端：``$($live.backendUrl)``
- Playwright MCP controller：``$($live.mcpController)``
- 验收时间：``$($live.timestamp)``
- Runtime 证据文件：``$liveEvidencePath``
- 源级门禁证据文件：``$gateEvidencePath``

## 样板业务证据

- `businessType`：`sample_expense`
- `expenseId`：``$($live.expenseId)``
- `instanceId`：``$($live.instanceId)``
- `callbackRecordId`：``$($live.callbackRecordId)``
- 首次失败原因：``$($live.failureReason)``
- 重试路径：``$($live.retryPath)``
- 最终样板状态：``approvalStatus = $($live.finalApprovalStatus)``
- 最终回调状态：``callbackStatus = $($live.callbackFinalStatus)``

## 页面证据

$uiEvidence

## 源级门禁

| 门禁 | 命令 | 结果 |
| --- | --- | --- |
| BPM 模块门禁 | ``mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test`` | $($gates.bpmModule) |
| 前端 BPM 合同测试 | ``pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom`` | $($gates.frontendContracts) |
| 前端类型检查 | ``pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck`` | $($gates.frontendTypecheck) |
| Flowable 边界门禁 | ``mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin -Dtest=BpmFlowableCompatibilityTest test`` | $($gates.flowableBoundary) |

## 边界说明

- 本轮没有新增样板费用页面、菜单或路由。
- 本轮没有新增 MQ、事件总线、HTTP 回调平台或外部调度。
- 本轮没有提交 Playwright runtime 输出、截图、网络日志或浏览器 profile。
- Hunyuan BPM 对外合同仍未暴露 Flowable 原生对象。

## 非阻塞项

$limitations

## 阻塞项

$blockers
"@
$target = 'docs/superpowers/specs/2026-07-09-bpm-p2-live-acceptance.md'
$markdown | Set-Content -Encoding UTF8 -Path $target
Get-Content -Raw -Encoding UTF8 $target
```

Expected:

- The acceptance record contains concrete values from the runtime evidence files.
- If `$live.status` is not `PASSED`, the conclusion says the acceptance is blocked.
- If UI evidence is missing, the generated document contains a visible warning and must be corrected before commit.

- [ ] **Step 2: Scan the acceptance record for unfilled instructions**

Run:

```powershell
rg -n "实际|写入真实|根据真实|\\[|\\]|TBD|TODO|待执行|未执行|未记录 UI 证据" docs/superpowers/specs/2026-07-09-bpm-p2-live-acceptance.md
```

Expected:

- No output.

If there is output, replace the instruction text with actual evidence or an explicit blocker.

- [ ] **Step 3: Check Markdown UTF-8 and whitespace**

Run:

```powershell
Get-Content -Raw -Encoding UTF8 docs/superpowers/specs/2026-07-09-bpm-p2-live-acceptance.md
git diff --check -- docs/superpowers/specs/2026-07-09-bpm-p2-live-acceptance.md
```

Expected:

- Chinese text renders correctly.
- `git diff --check` exits 0.

- [ ] **Step 4: Commit only the acceptance record**

Run:

```powershell
git status --short
git add docs/superpowers/specs/2026-07-09-bpm-p2-live-acceptance.md
git diff --cached --check
git diff --cached --name-only
git commit -m "docs: 增加 BPM P2 收官活体验收记录"
```

Expected:

- Staged files contain only `docs/superpowers/specs/2026-07-09-bpm-p2-live-acceptance.md`.
- Commit succeeds.
- Runtime evidence under `G:\code-mcp\playwright-mcp-temp` is not staged.

---

## Final Verification

After Task 4, run:

```powershell
git status --short
git log --oneline -6
```

Expected:

- `git status --short` is clean or contains only explicitly recognized unrelated user changes.
- Recent commits include:
  - `docs: 增加 BPM P2 收官活体验收记录`
  - `docs: 增加 BPM P2 收官活体验收设计`
  - `test: 验证 BPM 样板回调执行闭环`

## Self-Review

- Spec coverage: Task 1 covers environment and repository preflight. Task 2 covers source gates. Task 3 covers the live sample expense callback failure/retry proof. Task 4 covers the acceptance record and commit boundary.
- Placeholder scan: The plan includes template instructions only inside Task 4 Step 1; Task 4 Step 2 explicitly fails the final acceptance record if those instructions remain.
- Type and endpoint consistency: The plan uses existing Hunyuan endpoints `/bpm/sample/expense/**`, `/bpm/integration/callback/**`, `/bpm/instance/trace/{instanceId}`, and `/app/bpm/task/approve`; frontend calls go through `/api` because `hunyuan-system` uses `VITE_GLOB_API_URL=/api`.
