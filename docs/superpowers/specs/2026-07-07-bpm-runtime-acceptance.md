# BPM Runtime 闭环验收记录

## 基本信息

- 验收日期：2026-07-07
- 验收结论：`BPM runtime 闭环通过`
- 验收范围：`hunyuan-pro` 第一阶段通用审批运行闭环
- 相关设计：[2026-07-06-bpm-runtime-closure-design.md](./2026-07-06-bpm-runtime-closure-design.md)
- 相关计划：[2026-07-06-bpm-runtime-closure.md](../plans/2026-07-06-bpm-runtime-closure.md)

## 1. 验收结论

本轮可以将 `hunyuan-pro` 第一阶段 BPM runtime 定性为“闭环通过”。

本次结论不是基于静态代码推断，而是基于真实前后端运行结果、实例状态、动作轨迹、网络日志与页面证据综合得出。当前已经证明以下主链路成立：

- 已发布流程可从前端发起
- 待办列表可承接审批任务
- 拒绝可闭环结束实例
- 退回发起人可将实例打回待重新提交状态
- 转办可把待办转移给新处理人
- 新处理人侧可承接待办并完成最终审批
- 实例详情可反映状态与动作轨迹

需要特别说明的是：本轮“转办后审批通过”最终闭环成立，但 `huke` 侧“审批通过”在 Playwright 自动化下存在 `Element Plus prompt` 确认提交不稳定现象。因此该分支的最终收口采用了“前端 UI 证据 + 同一真实待办的后端 `/app/bpm/task/approve` 收口”方式完成验收。该问题应归类为“前端自动化稳定性缺陷”，不再阻塞本轮 BPM runtime 验收结论。

## 2. 已通过范围

### 2.1 发起与实例查询链路

- 前端可访问 `startable` 页面并成功发起流程
- 发起后可在“我的申请”中查询到实例
- 实例详情接口可返回当前实例状态与动作轨迹

### 2.2 拒绝分支闭环通过

- 实例编号：`DK20260707NO01161`
- 验收方式：管理员前端实际点击“拒绝”
- 最终结果：
  - `runState=3`
  - `resultState=2`
  - `actionTypes=["REJECTED"]`

结论：拒绝分支已完成“前端操作 -> 后端真实请求 -> 实例结束 -> 详情轨迹一致”的闭环。

### 2.3 退回发起人分支闭环通过

- 实例编号：`DK20260707NO01169`
- 验收方式：管理员前端实际点击“退回发起人”
- 最终结果：
  - `runState=2`
  - `resultState=null`
  - `actionTypes=["RETURNED_TO_INITIATOR"]`

结论：退回分支已完成“前端操作 -> 后端真实请求 -> 实例回到待重新提交状态 -> 详情轨迹一致”的闭环。

### 2.4 转办分支闭环通过

- 实例编号：`DK20260707NO01174`
- 验收方式：
  - 管理员前端实际点击“转办”
  - `huke` 侧页面成功看到待办
  - `huke` 侧“审批通过”弹框可正常拉起
  - 因 Playwright 下确认提交不稳定，保留 UI 证据后，使用同一真实待办 `taskId=41` 调用 `/app/bpm/task/approve` 完成最终收口
- 最终结果：
  - `runState=3`
  - `resultState=1`
  - `actionTypes=["TRANSFERRED","APPROVED"]`

结论：转办分支的业务运行闭环已经成立，且能证明“转办 -> 新处理人接收 -> 最终审批通过”这条主链路打通。

### 2.5 真实接口命中情况

管理员侧浏览器网络日志已确认命中以下真实接口：

- `POST /api/app/bpm/task/reject => 200`
- `POST /api/app/bpm/task/returnToInitiator => 200`
- `POST /api/app/bpm/task/transfer => 200`

`huke` 侧日志能确认待办/已办页面承接能力，但本轮自动化中未稳定捕获到 `/api/app/bpm/task/approve` 前端请求。这也是本次遗留问题的核心边界。

## 3. 非阻塞遗留问题

### 3.1 `huke` 侧审批通过弹框提交不稳定

问题现象：

- `huke` 侧“审批通过”按钮可以拉起 `Element Plus prompt`
- 自动化里填写意见后，“点确定”经常不真正发起 `/api/app/bpm/task/approve`
- 因此前端自动化未能稳定实现“纯 UI 点击直到通过”的 100% 收口

当前定性：

- 这是前端交互 / 自动化稳定性缺陷
- 不是 BPM runtime 业务闭环缺陷
- 不是后端审批链路不可用
- 不阻塞本轮 `BPM runtime 闭环通过` 的验收结论

当前判断依据：

- 管理员侧三条分支已真实命中后端接口并拿到正确状态
- 转办后的 `huke` 待办可见，说明转办结果已被新处理人承接
- 同一真实待办 `taskId=41` 通过后端 `/app/bpm/task/approve` 成功收口，说明审批通过业务链路本身可用
- `huke` 侧网络日志仅见待办/已办查询，未见稳定的前端 `approve` 请求，说明问题停留在前端弹框确认提交阶段

建议后续单独作为前端自动化稳定性问题处理，不再与 BPM runtime 业务验收混为一谈。

## 4. 证据文件索引

### 4.1 自动化脚本

- 分支闭环主脚本：[.superpowers/playwright/run-local-mcp-bpm-branch-closure.cjs](../../../.superpowers/playwright/run-local-mcp-bpm-branch-closure.cjs)
- `huke` 审批专项诊断脚本：[.superpowers/playwright/debug-huke-approve.cjs](../../../.superpowers/playwright/debug-huke-approve.cjs)

### 4.2 网络日志

- 管理员侧网络日志：[.superpowers/playwright-mcp-runtime/output/bpm-ui-branch-admin-network.txt](../../../.superpowers/playwright-mcp-runtime/output/bpm-ui-branch-admin-network.txt)
- `huke` 侧网络日志：[.superpowers/playwright-mcp-runtime/output/bpm-ui-branch-huke-network.txt](../../../.superpowers/playwright-mcp-runtime/output/bpm-ui-branch-huke-network.txt)

### 4.3 页面截图

- 拒绝完成截图：[.superpowers/playwright-mcp-runtime/output/bpm-ui-reject-done.png](../../../.superpowers/playwright-mcp-runtime/output/bpm-ui-reject-done.png)
- 拒绝详情截图：[.superpowers/playwright-mcp-runtime/output/bpm-ui-reject-detail.png](../../../.superpowers/playwright-mcp-runtime/output/bpm-ui-reject-detail.png)
- 退回完成截图：[.superpowers/playwright-mcp-runtime/output/bpm-ui-return-done.png](../../../.superpowers/playwright-mcp-runtime/output/bpm-ui-return-done.png)
- 退回详情截图：[.superpowers/playwright-mcp-runtime/output/bpm-ui-return-detail.png](../../../.superpowers/playwright-mcp-runtime/output/bpm-ui-return-detail.png)
- 转办后 `huke` 已办截图：[.superpowers/playwright-mcp-runtime/output/bpm-ui-transfer-done-huke.png](../../../.superpowers/playwright-mcp-runtime/output/bpm-ui-transfer-done-huke.png)
- 转办详情截图（管理员侧）：[.superpowers/playwright-mcp-runtime/output/bpm-ui-transfer-detail-admin.png](../../../.superpowers/playwright-mcp-runtime/output/bpm-ui-transfer-detail-admin.png)

### 4.4 调试产物

- `huke` 审批调试网络日志：[.superpowers/playwright-mcp-runtime/output/debug-huke-approve-network.txt](../../../.superpowers/playwright-mcp-runtime/output/debug-huke-approve-network.txt)
- `huke` 审批调试截图：[.superpowers/playwright-mcp-runtime/output/debug-huke-approve.png](../../../.superpowers/playwright-mcp-runtime/output/debug-huke-approve.png)

## 5. 后续边界

本记录落地后，当前 BPM 话题的边界应明确为：

- `BPM runtime 闭环：已通过`
- `纯前端自动化审批通过稳定性：未完全通过`
- `下一步若继续处理，应单独按前端自动化稳定性问题推进`
