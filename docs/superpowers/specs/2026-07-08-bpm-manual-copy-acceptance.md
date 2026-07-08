# BPM 手工抄送运行端验收记录

## Scope

- 审批通过、审批拒绝、退回发起人支持可选 `copyEmployeeIds`。
- 后端写入 `t_bpm_instance_copy`，被抄送员工可查询“我的抄送”并标记已读。
- 前端新增“我的抄送”列表页，复用统一实例详情抽屉。
- “我的待办”通过、拒绝、退回动作使用本地处理弹框，可选抄送员工。

## Verification

- `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm "-Dtest=BpmInstanceCopyServiceTest,BpmRuntimeCommandServiceTest" test`
  - Result: PASS
  - Evidence: 15 tests, 0 failures, 0 errors, 0 skipped.
- `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom`
  - Result: PASS
  - Evidence: 2 test files, 32 tests passed.
- `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck`
  - Result: PASS
  - Evidence: `vue-tsc --noEmit --skipLibCheck` completed successfully.

## Browser Acceptance

- Not executed in this implementation pass.
- Reason: this pass did not confirm live frontend `http://127.0.0.1:5788`, backend `http://127.0.0.1:1024`, and a reusable authenticated browser session were available.
- Recommended manual/live flow:
  1. 审批人进入“我的待办”。
  2. 通过、拒绝或退回任务时选择一个抄送员工。
  3. 被抄送员工进入“我的抄送”。
  4. 列表看到该流程记录。
  5. 点击“详情”，统一实例详情抽屉打开。
  6. 返回列表后该记录变为已读。

## Boundaries

- 未实现自动抄送节点。
- 未修改 simpleModel 设计器、BPMN 编译器或发布流程。
- 未扩大被抄送人的审批权限。
- 未新增管理员抄送管理页。
- 未新增依赖。
