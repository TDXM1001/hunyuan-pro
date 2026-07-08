# BPM 实例详情与流转轨迹验收记录

## 结论

通过。BPM 实例详情现在能够统一展示基础信息、当前待办、表单快照和动作轨迹；管理员端实例列表与员工端运行页复用同一套实例详情抽屉。

## 范围

- 后端实例详情返回 `currentTasks` 和 `actionLogs`。
- 员工端我的申请、我的已办继续打开统一实例详情抽屉。
- 管理员端实例列表改为打开统一实例详情抽屉，并通过管理员详情接口加载数据。
- 任务详情仍保留本地任务详情弹层。

## 验证

- `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm "-Dtest=BpmRuntimeDetailServiceTest,BpmTaskDetailServiceTest" test`
  - 结果：`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test`
  - 结果：`Tests run: 30, Failures: 0, Errors: 0, Skipped: 0`
- `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom`
  - 结果：`Test Files 2 passed (2)`，`Tests 29 passed (29)`
- `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck`
  - 结果：`vue-tsc --noEmit --skipLibCheck` 退出码 0
- public API leak check for `org.flowable|Flowable`
  - 结果：无匹配，公共 controller、VO、前端 API 类型未暴露 Flowable 名称
- mojibake source check
  - 结果：无匹配

## 非目标

- 未做 BPMN 图高亮。
- 未新增会签、或签、加签、减签、委派、管理员跳转节点。
- 未复制 Yudao/RuoYi 的 API 路径、页面壳、权限模型或 Flowable 对象结构。
