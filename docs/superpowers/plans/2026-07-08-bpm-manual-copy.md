# BPM 手工抄送运行端 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 审批人通过、拒绝、退回待办时可手工抄送员工，系统写入 `t_bpm_instance_copy`，被抄送员工可在“我的抄送”查看并打开统一实例详情。

**Architecture:** 后端新增 Hunyuan 原生抄送服务，围绕现有 `BpmInstanceCopyEntity` 做写入、我的抄送分页、标记已读；`BpmTaskService` 只在 `approve`、`reject`、`returnToInitiator` 成功链路中调用该服务。前端复用现有 BPM runtime 列表页结构和 `BpmInstanceDetailDrawer`，待办页把 `ElMessageBox.prompt` 升级为本地审批动作弹框，附带可选员工多选。

**Tech Stack:** Java 17, Spring Boot 3, MyBatis-Plus, Flowable behind Hunyuan BPM boundary, Vue 3, TypeScript, Element Plus, `@vben/art-hooks`, Vitest, Maven.

## Global Constraints

- 只实现审批时手工抄送 P0：`approve`、`reject`、`returnToInitiator`。
- 不实现自动抄送节点，不修改 simpleModel 设计器、BPMN 编译器或发布流程。
- 不实现会签、或签、加签、减签、跳转节点、管理员抄送管理页。
- `transfer` 不携带抄送，因为转办是任务责任流转，不是审批结论。
- 不新增依赖，不迁移 Yudao/RuoYi API、页面壳、权限模型或 Flowable 对象结构。
- 生产代码、契约、菜单、测试和验收记录只落在 `E:/my-project/hunyuan-pro` 当前仓库。
- 前端列表页遵循 `docs/frontend-list-table-page-standard.md`，不添加解释性页头文案。
- 后端接口只暴露 Hunyuan 投影字段，不暴露 Flowable 历史任务或评论对象。
- 抄送写入失败时审批动作回滚，避免“用户以为已知会但记录不存在”。
- 浏览器验收如执行，复用长连接 Playwright MCP controller，运行输出不写入当前仓库。

---

## File Structure

- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmCopyTypeEnum.java`
  - 手工抄送类型枚举，避免服务和页面散落魔法字符串。
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmInstanceCopyQueryForm.java`
  - “我的抄送”分页查询表单，内部注入当前员工。
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmInstanceCopyVO.java`
  - “我的抄送”列表返回对象。
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/dao/BpmInstanceCopyDao.java`
  - 增加分页查询方法。
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/resources/mapper/bpm/runtime/BpmInstanceCopyMapper.xml`
  - 增加抄送列表 SQL，关联 `t_bpm_instance` 取实例编号、标题、状态。
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceCopyService.java`
  - 负责写入手工抄送、查询我的抄送、标记已读。
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmTaskApproveForm.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmTaskRejectForm.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmTaskReturnForm.java`
  - 增加 `List<Long> copyEmployeeIds`。
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskService.java`
  - 审批动作成功后调用 `BpmInstanceCopyService#createManualCopies`。
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/app/AppBpmInstanceController.java`
  - 增加 `/app/bpm/my-copy` 和 `/app/bpm/copy/read/{copyId}`。
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmInstanceCopyServiceTest.java`
  - 覆盖抄送服务写入、查询、标记已读。
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeCommandServiceTest.java`
  - 覆盖 approve/reject/return 携带抄送。
- Create: `数据库SQL脚本/mysql/sql-update-log/v3.37.0.sql`
  - 增加“我的抄送”菜单和 `bpm_runtime_user` 授权。
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts`
  - 增加抄送 API、类型、审批动作 `copyEmployeeIds`。
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/my-copy-list.vue`
  - 新增“我的抄送”列表页，复用实例详情抽屉。
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/my-todo-list.vue`
  - 通过、拒绝、退回使用本地动作弹框，支持可选抄送员工。
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`
  - 覆盖新增 API 路径和 `copyEmployeeIds` 转发。
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`
  - 覆盖新页面、菜单 SQL、详情抽屉复用、待办页抄送字段。
- Create: `docs/superpowers/specs/2026-07-08-bpm-manual-copy-acceptance.md`
  - 记录验证命令、通过范围和未做边界。

---

### Task 1: Backend Copy Domain And Service

**Files:**
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmCopyTypeEnum.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmInstanceCopyQueryForm.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmInstanceCopyVO.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/dao/BpmInstanceCopyDao.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/resources/mapper/bpm/runtime/BpmInstanceCopyMapper.xml`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceCopyService.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmInstanceCopyServiceTest.java`

**Interfaces:**
- Consumes: `BpmInstanceCopyEntity`, `BpmInstanceCopyDao`, `BpmInstanceDao`, `BpmCurrentActorProvider`, `BpmOrgIdentityGateway`, `SmartPageUtil`, `BpmCopyReadStateEnum`.
- Produces:
  - `BpmCopyTypeEnum.MANUAL_APPROVE_COPY`, `MANUAL_REJECT_COPY`, `MANUAL_RETURN_COPY`
  - `ResponseDTO<String> createManualCopies(BpmTaskEntity taskEntity, Collection<Long> targetEmployeeIds, String reasonSnapshot, BpmCopyTypeEnum copyTypeEnum)`
  - `ResponseDTO<PageResult<BpmInstanceCopyVO>> queryMyCopyPage(BpmInstanceCopyQueryForm queryForm)`
  - `ResponseDTO<String> markRead(Long copyId)`
  - `List<BpmInstanceCopyVO> BpmInstanceCopyDao.queryMyCopyPage(Page page, @Param("queryForm") BpmInstanceCopyQueryForm queryForm)`

- [ ] **Step 1: Write failing service tests**

Add focused Mockito tests to `BpmInstanceCopyServiceTest.java`:

```java
package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.common.enumeration.BpmCopyTypeEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmCopyReadStateEnum;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceCopyDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceCopyEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceCopyQueryForm;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceCopyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmInstanceCopyServiceTest {

    private BpmInstanceCopyService service;
    private BpmInstanceCopyDao copyDao;
    private BpmCurrentActorProvider actorProvider;
    private BpmOrgIdentityGateway orgIdentityGateway;

    @BeforeEach
    void setUp() {
        service = new BpmInstanceCopyService();
        copyDao = Mockito.mock(BpmInstanceCopyDao.class);
        actorProvider = Mockito.mock(BpmCurrentActorProvider.class);
        orgIdentityGateway = Mockito.mock(BpmOrgIdentityGateway.class);
        setField(service, "bpmInstanceCopyDao", copyDao);
        setField(service, "bpmCurrentActorProvider", actorProvider);
        setField(service, "bpmOrgIdentityGateway", orgIdentityGateway);
    }

    @Test
    void createManualCopiesShouldDeduplicateTargetsAndSkipCurrentActor() {
        BpmTaskEntity task = buildTask();
        when(actorProvider.requireCurrentEmployeeId()).thenReturn(10L);
        when(orgIdentityGateway.requireEmployee(22L))
                .thenReturn(new BpmEmployeeSnapshot(22L, "李四", 9L, "财务部", null, null));
        when(orgIdentityGateway.requireEmployee(23L))
                .thenReturn(new BpmEmployeeSnapshot(23L, "王五", 8L, "行政部", null, null));

        ResponseDTO<String> response = service.createManualCopies(
                task,
                List.of(22L, 22L, 10L, 23L),
                "同意，知会财务和行政",
                BpmCopyTypeEnum.MANUAL_APPROVE_COPY
        );

        assertThat(response.getOk()).isTrue();
        ArgumentCaptor<BpmInstanceCopyEntity> captor = ArgumentCaptor.forClass(BpmInstanceCopyEntity.class);
        verify(copyDao, Mockito.times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(BpmInstanceCopyEntity::getTargetEmployeeId)
                .containsExactly(22L, 23L);
        assertThat(captor.getAllValues()).allSatisfy(entity -> {
            assertThat(entity.getInstanceId()).isEqualTo(8L);
            assertThat(entity.getDefinitionId()).isEqualTo(2L);
            assertThat(entity.getDefinitionNodeId()).isEqualTo(5L);
            assertThat(entity.getEngineProcessInstanceId()).isEqualTo("process-8");
            assertThat(entity.getSourceNodeKey()).isEqualTo("manager_approve");
            assertThat(entity.getSourceNodeName()).isEqualTo("经理审批");
            assertThat(entity.getCopyType()).isEqualTo("MANUAL_APPROVE_COPY");
            assertThat(entity.getReadState()).isEqualTo(BpmCopyReadStateEnum.UNREAD.getValue());
            assertThat(entity.getReasonSnapshot()).isEqualTo("同意，知会财务和行政");
            assertThat(entity.getSentAt()).isNotNull();
        });
    }

    @Test
    void createManualCopiesShouldReturnOkWithoutInsertWhenTargetsAreEmpty() {
        ResponseDTO<String> response = service.createManualCopies(
                buildTask(),
                List.of(),
                "同意",
                BpmCopyTypeEnum.MANUAL_APPROVE_COPY
        );

        assertThat(response.getOk()).isTrue();
        verify(copyDao, never()).insert(any());
    }

    @Test
    void queryMyCopyPageShouldScopeToCurrentEmployee() {
        when(actorProvider.requireCurrentEmployeeId()).thenReturn(22L);
        BpmInstanceCopyQueryForm form = new BpmInstanceCopyQueryForm();
        form.setPageNum(1);
        form.setPageSize(10);
        form.setInstanceNo("SN-2026");
        form.setTitle("请假");
        form.setReadState(BpmCopyReadStateEnum.UNREAD.getValue());

        ResponseDTO<?> response = service.queryMyCopyPage(form);

        assertThat(response.getOk()).isTrue();
        assertThat(form.getTargetEmployeeId()).isEqualTo(22L);
        verify(copyDao).queryMyCopyPage(any(), Mockito.same(form));
    }

    @Test
    void markReadShouldOnlyUpdateCurrentEmployeeCopy() {
        when(actorProvider.requireCurrentEmployeeId()).thenReturn(22L);

        ResponseDTO<String> response = service.markRead(100L);

        assertThat(response.getOk()).isTrue();
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.Wrapper<BpmInstanceCopyEntity>> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.Wrapper.class);
        verify(copyDao).update(any(BpmInstanceCopyEntity.class), captor.capture());
    }

    private BpmTaskEntity buildTask() {
        BpmTaskEntity task = new BpmTaskEntity();
        task.setTaskId(1L);
        task.setInstanceId(8L);
        task.setDefinitionId(2L);
        task.setDefinitionNodeId(5L);
        task.setEngineProcessInstanceId("process-8");
        task.setTaskKey("manager_approve");
        task.setTaskName("经理审批");
        return task;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("设置测试字段失败: " + fieldName, ex);
        }
    }
}
```

- [ ] **Step 2: Run failing test**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmInstanceCopyServiceTest test
```

Expected: FAIL because `BpmCopyTypeEnum`, `BpmInstanceCopyQueryForm`, `BpmInstanceCopyService`, and `queryMyCopyPage` do not exist yet.

- [ ] **Step 3: Add enum, query form, VO, DAO, mapper**

Create `BpmCopyTypeEnum.java`:

```java
package com.hunyuan.sa.bpm.common.enumeration;

/**
 * 流程抄送类型。
 */
public enum BpmCopyTypeEnum {

    MANUAL_APPROVE_COPY("审批通过手工抄送"),
    MANUAL_REJECT_COPY("审批拒绝手工抄送"),
    MANUAL_RETURN_COPY("退回发起人手工抄送");

    private final String desc;

    BpmCopyTypeEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
```

Create `BpmInstanceCopyQueryForm.java`:

```java
package com.hunyuan.sa.bpm.module.runtime.domain.form;

import com.hunyuan.sa.base.common.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BpmInstanceCopyQueryForm extends PageParam {

    @Schema(description = "实例编号")
    private String instanceNo;

    @Schema(description = "流程标题")
    private String title;

    @Schema(description = "已读状态")
    private Integer readState;

    @Schema(hidden = true)
    private Long targetEmployeeId;
}
```

Create `BpmInstanceCopyVO.java`:

```java
package com.hunyuan.sa.bpm.module.runtime.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BpmInstanceCopyVO {

    @Schema(description = "抄送ID")
    private Long copyId;

    @Schema(description = "实例ID")
    private Long instanceId;

    @Schema(description = "实例编号")
    private String instanceNo;

    @Schema(description = "流程标题")
    private String title;

    @Schema(description = "抄送类型")
    private String copyType;

    @Schema(description = "已读状态")
    private Integer readState;

    @Schema(description = "来源节点")
    private String sourceNodeName;

    @Schema(description = "被抄送人")
    private String targetNameSnapshot;

    @Schema(description = "抄送原因")
    private String reasonSnapshot;

    @Schema(description = "发送时间")
    private LocalDateTime sentAt;

    @Schema(description = "阅读时间")
    private LocalDateTime readAt;

    @Schema(description = "发起人姓名")
    private String startEmployeeNameSnapshot;

    @Schema(description = "运行状态")
    private Integer runState;

    @Schema(description = "结果状态")
    private Integer resultState;
}
```

Modify `BpmInstanceCopyDao.java`:

```java
package com.hunyuan.sa.bpm.module.runtime.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceCopyEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceCopyQueryForm;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceCopyVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BpmInstanceCopyDao extends BaseMapper<BpmInstanceCopyEntity> {

    List<BpmInstanceCopyVO> queryMyCopyPage(Page page, @Param("queryForm") BpmInstanceCopyQueryForm queryForm);
}
```

Replace `BpmInstanceCopyMapper.xml` content:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceCopyDao">

    <select id="queryMyCopyPage" resultType="com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceCopyVO">
        select
            c.copy_id as copyId,
            c.instance_id as instanceId,
            i.instance_no as instanceNo,
            i.title as title,
            c.copy_type as copyType,
            c.read_state as readState,
            c.source_node_name as sourceNodeName,
            c.target_name_snapshot as targetNameSnapshot,
            c.reason_snapshot as reasonSnapshot,
            c.sent_at as sentAt,
            c.read_at as readAt,
            i.start_employee_name_snapshot as startEmployeeNameSnapshot,
            i.run_state as runState,
            i.result_state as resultState
        from t_bpm_instance_copy c
        inner join t_bpm_instance i on i.instance_id = c.instance_id
        where c.target_employee_id = #{queryForm.targetEmployeeId}
        <if test="queryForm.instanceNo != null and queryForm.instanceNo != ''">
            and i.instance_no like concat('%', #{queryForm.instanceNo}, '%')
        </if>
        <if test="queryForm.title != null and queryForm.title != ''">
            and i.title like concat('%', #{queryForm.title}, '%')
        </if>
        <if test="queryForm.readState != null">
            and c.read_state = #{queryForm.readState}
        </if>
        order by c.sent_at desc, c.copy_id desc
    </select>
</mapper>
```

- [ ] **Step 4: Add `BpmInstanceCopyService`**

Create `BpmInstanceCopyService.java`:

```java
package com.hunyuan.sa.bpm.module.runtime.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartPageUtil;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.common.enumeration.BpmCopyReadStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmCopyTypeEnum;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceCopyDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceCopyEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceCopyQueryForm;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceCopyVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class BpmInstanceCopyService {

    @Resource
    private BpmInstanceCopyDao bpmInstanceCopyDao;

    @Resource
    private BpmCurrentActorProvider bpmCurrentActorProvider;

    @Resource
    private BpmOrgIdentityGateway bpmOrgIdentityGateway;

    public ResponseDTO<String> createManualCopies(
            BpmTaskEntity taskEntity,
            Collection<Long> targetEmployeeIds,
            String reasonSnapshot,
            BpmCopyTypeEnum copyTypeEnum
    ) {
        if (targetEmployeeIds == null || targetEmployeeIds.isEmpty()) {
            return ResponseDTO.ok();
        }

        Long currentEmployeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        LinkedHashSet<Long> targetIds = new LinkedHashSet<>();
        for (Long targetEmployeeId : targetEmployeeIds) {
            if (targetEmployeeId != null && !targetEmployeeId.equals(currentEmployeeId)) {
                targetIds.add(targetEmployeeId);
            }
        }
        if (targetIds.isEmpty()) {
            return ResponseDTO.ok();
        }

        LocalDateTime now = LocalDateTime.now();
        for (Long targetId : targetIds) {
            BpmEmployeeSnapshot snapshot = bpmOrgIdentityGateway.requireEmployee(targetId);
            BpmInstanceCopyEntity entity = new BpmInstanceCopyEntity();
            entity.setInstanceId(taskEntity.getInstanceId());
            entity.setDefinitionId(taskEntity.getDefinitionId());
            entity.setDefinitionNodeId(taskEntity.getDefinitionNodeId());
            entity.setEngineProcessInstanceId(taskEntity.getEngineProcessInstanceId());
            entity.setSourceNodeKey(taskEntity.getTaskKey());
            entity.setSourceNodeName(taskEntity.getTaskName());
            entity.setTargetEmployeeId(snapshot.employeeId());
            entity.setTargetNameSnapshot(snapshot.actualName());
            entity.setCopyType(copyTypeEnum.name());
            entity.setReadState(BpmCopyReadStateEnum.UNREAD.getValue());
            entity.setReasonSnapshot(reasonSnapshot);
            entity.setSentAt(now);
            bpmInstanceCopyDao.insert(entity);
        }
        return ResponseDTO.ok();
    }

    public ResponseDTO<PageResult<BpmInstanceCopyVO>> queryMyCopyPage(BpmInstanceCopyQueryForm queryForm) {
        queryForm.setTargetEmployeeId(bpmCurrentActorProvider.requireCurrentEmployeeId());
        Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
        List<BpmInstanceCopyVO> list = bpmInstanceCopyDao.queryMyCopyPage(page, queryForm);
        return ResponseDTO.ok(SmartPageUtil.convert2PageResult(page, list));
    }

    public ResponseDTO<String> markRead(Long copyId) {
        Long currentEmployeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        BpmInstanceCopyEntity updateEntity = new BpmInstanceCopyEntity();
        updateEntity.setReadState(BpmCopyReadStateEnum.READ.getValue());
        updateEntity.setReadAt(LocalDateTime.now());
        bpmInstanceCopyDao.update(updateEntity, Wrappers.<BpmInstanceCopyEntity>lambdaUpdate()
                .eq(BpmInstanceCopyEntity::getCopyId, copyId)
                .eq(BpmInstanceCopyEntity::getTargetEmployeeId, currentEmployeeId)
                .eq(BpmInstanceCopyEntity::getReadState, BpmCopyReadStateEnum.UNREAD.getValue()));
        return ResponseDTO.ok();
    }
}
```

- [ ] **Step 5: Run service tests**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmInstanceCopyServiceTest test
```

Expected: PASS.

- [ ] **Step 6: Commit task**

```powershell
git add hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmCopyTypeEnum.java `
  hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmInstanceCopyQueryForm.java `
  hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmInstanceCopyVO.java `
  hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/dao/BpmInstanceCopyDao.java `
  hunyuan-backend/hunyuan-bpm/src/main/resources/mapper/bpm/runtime/BpmInstanceCopyMapper.xml `
  hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceCopyService.java `
  hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmInstanceCopyServiceTest.java
git commit -m "feat: 增加BPM手工抄送服务"
```

---

### Task 2: Wire Copy Into Approval Actions

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmTaskApproveForm.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmTaskRejectForm.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmTaskReturnForm.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskService.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeCommandServiceTest.java`

**Interfaces:**
- Consumes: `BpmInstanceCopyService#createManualCopies`, `BpmCopyTypeEnum`.
- Produces: approval forms with `List<Long> copyEmployeeIds`; task service approval methods call copy service only after existing state/log updates are successful.

- [ ] **Step 1: Write failing command service tests**

Extend `BpmRuntimeCommandServiceTest` setup:

```java
import com.hunyuan.sa.bpm.common.enumeration.BpmCopyTypeEnum;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceCopyService;
```

Add field:

```java
private BpmInstanceCopyService bpmInstanceCopyService;
```

In `setUp()`:

```java
bpmInstanceCopyService = Mockito.mock(BpmInstanceCopyService.class);
setField(bpmTaskService, "bpmInstanceCopyService", bpmInstanceCopyService);
when(bpmInstanceCopyService.createManualCopies(any(), any(), any(), any())).thenReturn(ResponseDTO.ok());
```

Add three tests:

```java
@Test
void approveShouldCreateManualCopiesWhenCopyEmployeesProvided() {
    BpmTaskEntity taskEntity = buildPendingTask();
    when(bpmTaskDao.selectById(1L)).thenReturn(taskEntity);
    when(taskCurrentActorProvider().requireCurrentEmployeeId()).thenReturn(10L);
    when(taskIdentityGateway().requireEmployee(10L)).thenReturn(new BpmEmployeeSnapshot(10L, "王主管", 7L, "人事部", null, null));
    when(taskServiceProjectionService().syncActiveTasksForInstance(8L)).thenReturn(1);

    BpmTaskApproveForm form = new BpmTaskApproveForm();
    form.setTaskId(1L);
    form.setCommentText("同意");
    form.setCopyEmployeeIds(java.util.List.of(22L, 23L));

    ResponseDTO<String> response = bpmTaskService.approve(form);

    assertThat(response.getOk()).isTrue();
    verify(bpmInstanceCopyService).createManualCopies(taskEntity, java.util.List.of(22L, 23L), "同意", BpmCopyTypeEnum.MANUAL_APPROVE_COPY);
}

@Test
void rejectShouldCreateManualCopiesWhenCopyEmployeesProvided() {
    BpmTaskEntity taskEntity = buildPendingTask();
    when(bpmTaskDao.selectById(1L)).thenReturn(taskEntity);
    when(taskCurrentActorProvider().requireCurrentEmployeeId()).thenReturn(10L);
    when(taskIdentityGateway().requireEmployee(10L)).thenReturn(new BpmEmployeeSnapshot(10L, "王主管", 7L, "人事部", null, null));
    when(taskServiceProjectionService().syncActiveTasksForInstance(8L)).thenReturn(0);

    BpmTaskRejectForm form = new BpmTaskRejectForm();
    form.setTaskId(1L);
    form.setCommentText("不同意");
    form.setCopyEmployeeIds(java.util.List.of(22L));

    ResponseDTO<String> response = bpmTaskService.reject(form);

    assertThat(response.getOk()).isTrue();
    verify(bpmInstanceCopyService).createManualCopies(taskEntity, java.util.List.of(22L), "不同意", BpmCopyTypeEnum.MANUAL_REJECT_COPY);
}

@Test
void returnShouldCreateManualCopiesWhenCopyEmployeesProvided() {
    BpmTaskEntity taskEntity = buildPendingTask();
    BpmInstanceEntity instanceEntity = new BpmInstanceEntity();
    instanceEntity.setInstanceId(8L);
    instanceEntity.setRunState(BpmInstanceRunStateEnum.RUNNING.getValue());
    when(bpmTaskDao.selectById(1L)).thenReturn(taskEntity);
    when(bpmInstanceDao.selectById(8L)).thenReturn(instanceEntity);
    when(taskCurrentActorProvider().requireCurrentEmployeeId()).thenReturn(10L);
    when(taskIdentityGateway().requireEmployee(10L)).thenReturn(new BpmEmployeeSnapshot(10L, "王主管", 7L, "人事部", null, null));

    BpmTaskReturnForm form = new BpmTaskReturnForm();
    form.setTaskId(1L);
    form.setCommentText("请补材料");
    form.setCopyEmployeeIds(java.util.List.of(23L));

    ResponseDTO<String> response = bpmTaskService.returnToInitiator(form);

    assertThat(response.getOk()).isTrue();
    verify(bpmInstanceCopyService).createManualCopies(taskEntity, java.util.List.of(23L), "请补材料", BpmCopyTypeEnum.MANUAL_RETURN_COPY);
}
```

Add helper:

```java
private BpmTaskEntity buildPendingTask() {
    BpmTaskEntity taskEntity = new BpmTaskEntity();
    taskEntity.setTaskId(1L);
    taskEntity.setInstanceId(8L);
    taskEntity.setDefinitionId(2L);
    taskEntity.setDefinitionNodeId(5L);
    taskEntity.setEngineTaskId("task-1");
    taskEntity.setEngineProcessInstanceId("process-8");
    taskEntity.setTaskKey("manager_approve");
    taskEntity.setTaskName("经理审批");
    taskEntity.setTaskState(BpmTaskStateEnum.PENDING.getValue());
    taskEntity.setAssigneeEmployeeId(10L);
    return taskEntity;
}
```

- [ ] **Step 2: Run failing command test**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmRuntimeCommandServiceTest test
```

Expected: FAIL because task forms do not expose `copyEmployeeIds` and `BpmTaskService` has no `bpmInstanceCopyService` field.

- [ ] **Step 3: Add `copyEmployeeIds` to forms**

In each of `BpmTaskApproveForm`, `BpmTaskRejectForm`, `BpmTaskReturnForm`, add import and field:

```java
import java.util.List;
```

```java
@Schema(description = "手工抄送员工ID列表")
private List<Long> copyEmployeeIds;
```

- [ ] **Step 4: Wire task service**

Modify imports in `BpmTaskService.java`:

```java
import com.hunyuan.sa.bpm.common.enumeration.BpmCopyTypeEnum;
import java.util.Collection;
```

Add resource:

```java
@Resource
private BpmInstanceCopyService bpmInstanceCopyService;
```

Change action entry points:

```java
public ResponseDTO<String> approve(BpmTaskApproveForm approveForm) {
    return completeTask(
            approveForm.getTaskId(),
            approveForm.getCommentText(),
            BpmTaskResultEnum.APPROVED,
            "APPROVED",
            approveForm.getCopyEmployeeIds(),
            BpmCopyTypeEnum.MANUAL_APPROVE_COPY
    );
}

public ResponseDTO<String> reject(BpmTaskRejectForm rejectForm) {
    return completeTask(
            rejectForm.getTaskId(),
            rejectForm.getCommentText(),
            BpmTaskResultEnum.REJECTED,
            "REJECTED",
            rejectForm.getCopyEmployeeIds(),
            BpmCopyTypeEnum.MANUAL_REJECT_COPY
    );
}
```

Inside `returnToInitiator`, after `bpmTaskActionLogDao.insert` and before `return ResponseDTO.ok();` add:

```java
ResponseDTO<String> copyResponse = bpmInstanceCopyService.createManualCopies(
        taskEntity,
        returnForm.getCopyEmployeeIds(),
        returnForm.getCommentText(),
        BpmCopyTypeEnum.MANUAL_RETURN_COPY
);
if (!copyResponse.getOk()) {
    return copyResponse;
}
```

Change `completeTask` signature:

```java
private ResponseDTO<String> completeTask(
        Long taskId,
        String commentText,
        BpmTaskResultEnum resultEnum,
        String actionType,
        Collection<Long> copyEmployeeIds,
        BpmCopyTypeEnum copyTypeEnum
)
```

Before `return ResponseDTO.ok();` in `completeTask`, add:

```java
ResponseDTO<String> copyResponse = bpmInstanceCopyService.createManualCopies(
        taskEntity,
        copyEmployeeIds,
        commentText,
        copyTypeEnum
);
if (!copyResponse.getOk()) {
    return copyResponse;
}
```

- [ ] **Step 5: Run command tests**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmRuntimeCommandServiceTest,BpmInstanceCopyServiceTest test
```

Expected: PASS.

- [ ] **Step 6: Commit task**

```powershell
git add hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmTaskApproveForm.java `
  hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmTaskRejectForm.java `
  hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmTaskReturnForm.java `
  hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskService.java `
  hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeCommandServiceTest.java
git commit -m "feat: 支持审批动作手工抄送"
```

---

### Task 3: App Copy API And Menu SQL

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/app/AppBpmInstanceController.java`
- Create: `数据库SQL脚本/mysql/sql-update-log/v3.37.0.sql`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`

**Interfaces:**
- Produces:
  - `POST /app/bpm/my-copy`
  - `POST /app/bpm/copy/read/{copyId}`
  - menu path `/system/bpm/runtime/my-copy-list`
  - component `/system/bpm/runtime/my-copy-list.vue`

- [ ] **Step 1: Write failing API and menu contract tests**

In `bpm-api.test.ts`, add expectations that `runtime.ts` contains:

```ts
expect(source).toContain('/app/bpm/my-copy');
expect(source).toContain('/app/bpm/copy/read/');
expect(source).toContain('copyEmployeeIds');
```

In `bpm-modules.test.ts`, extend SQL/menu tests with:

```ts
expect(bpmMenuSqlSource).toContain("'我的抄送'");
expect(bpmMenuSqlSource).toContain("'/system/bpm/runtime/my-copy-list'");
expect(bpmMenuSqlSource).toContain("'/system/bpm/runtime/my-copy-list.vue'");
expect(bpmMenuSqlSource).toContain("'bpm_runtime_user'");
```

- [ ] **Step 2: Run failing frontend contract tests**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: FAIL because API paths and SQL increment are not present.

- [ ] **Step 3: Add app copy endpoints**

Modify `AppBpmInstanceController.java` imports:

```java
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceCopyQueryForm;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceCopyVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceCopyService;
```

Add resource:

```java
@Resource
private BpmInstanceCopyService bpmInstanceCopyService;
```

Add methods:

```java
@Operation(summary = "查询我的抄送")
@PostMapping("/app/bpm/my-copy")
public ResponseDTO<PageResult<BpmInstanceCopyVO>> queryMyCopy(@RequestBody @Valid BpmInstanceCopyQueryForm queryForm) {
    return bpmInstanceCopyService.queryMyCopyPage(queryForm);
}

@Operation(summary = "标记我的抄送已读")
@PostMapping("/app/bpm/copy/read/{copyId}")
public ResponseDTO<String> markCopyRead(@PathVariable Long copyId) {
    return bpmInstanceCopyService.markRead(copyId);
}
```

- [ ] **Step 4: Add menu SQL increment**

Create `数据库SQL脚本/mysql/sql-update-log/v3.37.0.sql`:

```sql
-- BPM 运行端：我的抄送
INSERT INTO t_system_menu
(menu_id, menu_name, parent_id, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
VALUES
(320, '我的抄送', 308, 2, '/system/bpm/runtime/my-copy-list', '/system/bpm/runtime/my-copy-list.vue', '', '', 50, 1, 1, now(), now());

INSERT INTO t_system_role_menu (role_id, menu_id, create_time, update_time)
SELECT role_id, 320, now(), now()
FROM t_system_role
WHERE role_code = 'bpm_runtime_user'
  AND NOT EXISTS (
      SELECT 1
      FROM t_system_role_menu rm
      WHERE rm.role_id = t_system_role.role_id
        AND rm.menu_id = 320
  );
```

- [ ] **Step 5: Run backend compile and frontend contract tests**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -DskipTests compile
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: Maven compile PASS; frontend tests still may fail until Task 4 adds frontend API/page, but SQL assertions should now pass.

- [ ] **Step 6: Commit task**

```powershell
git add hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/app/AppBpmInstanceController.java `
  数据库SQL脚本/mysql/sql-update-log/v3.37.0.sql `
  hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts `
  hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts
git commit -m "feat: 增加BPM我的抄送接口和菜单"
```

---

### Task 4: Frontend Copy API And My Copy List

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/my-copy-list.vue`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`

**Interfaces:**
- Produces:
  - `BpmInstanceCopyRecord`
  - `BpmInstanceCopyPageQueryParams`
  - `queryMyBpmCopyPage(params)`
  - `markBpmCopyRead(copyId)`
  - `SystemBpmRuntimeMyCopyList`

- [ ] **Step 1: Strengthen failing frontend tests**

In `bpm-api.test.ts`, assert request bodies and optional copy IDs:

```ts
expect(source).toContain('export interface BpmInstanceCopyRecord');
expect(source).toContain('export interface BpmInstanceCopyPageQueryParams');
expect(source).toContain('queryMyBpmCopyPage');
expect(source).toContain('markBpmCopyRead');
expect(source).toContain('copyEmployeeIds?: number[]');
expect(source).toContain('copyEmployeeIds: params.copyEmployeeIds ?? []');
```

In `bpm-modules.test.ts`, add:

```ts
const myCopyPath =
  'apps/hunyuan-system/src/views/system/bpm/runtime/my-copy-list.vue';
const myCopySource = readWorkspaceFile(myCopyPath);

expect(myCopySource).toContain("defineOptions({ name: 'SystemBpmRuntimeMyCopyList' })");
expect(myCopySource).toContain('BpmInstanceDetailDrawer');
expect(myCopySource).toContain('queryMyBpmCopyPage');
expect(myCopySource).toContain('markBpmCopyRead');
expect(myCopySource).toContain(':collapsible=\"false\"');
```

- [ ] **Step 2: Run failing frontend tests**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: FAIL because frontend API and page do not exist.

- [ ] **Step 3: Extend runtime API**

In `runtime.ts`, add types:

```ts
export interface BpmInstanceCopyRecord {
  copyId: number;
  copyType: string;
  instanceId: number;
  instanceNo: string;
  readAt?: null | string;
  readState?: null | number;
  reasonSnapshot?: null | string;
  resultState?: null | number;
  runState?: null | number;
  sentAt?: null | string;
  sourceNodeName?: null | string;
  startEmployeeNameSnapshot?: null | string;
  targetNameSnapshot?: null | string;
  title: string;
}

export interface BpmInstanceCopyPageQueryParams {
  instanceNo?: string;
  pageNum: number;
  pageSize: number;
  readState?: null | number;
  title?: string;
}
```

Add `copyEmployeeIds?: number[]` to `BpmTaskApproveForm`, `BpmTaskRejectForm`, and `BpmTaskReturnForm`.

Add functions:

```ts
export async function queryMyBpmCopyPage(
  params: BpmInstanceCopyPageQueryParams,
) {
  return requestClient.post<PageResult<BpmInstanceCopyRecord>>(
    '/app/bpm/my-copy',
    {
      instanceNo: params.instanceNo?.trim() || undefined,
      pageNum: params.pageNum,
      pageSize: params.pageSize,
      readState: params.readState ?? undefined,
      title: params.title?.trim() || undefined,
    },
  );
}

export async function markBpmCopyRead(copyId: number) {
  return requestClient.post<string>(`/app/bpm/copy/read/${copyId}`);
}
```

In `approveBpmTask`, `rejectBpmTask`, and `returnBpmTaskToInitiator`, include:

```ts
copyEmployeeIds: params.copyEmployeeIds ?? [],
```

- [ ] **Step 4: Add my-copy list page**

Create `my-copy-list.vue` by mirroring `my-instance-list.vue` structure, with these required imports and behavior:

```vue
<script setup lang="ts">
import type { BpmInstanceCopyRecord } from '#/api/system/bpm';
import type { ColumnOption } from '@vben/art-hooks/table';

import { computed, onMounted, reactive, ref } from 'vue';

import { ArtSearchPanel } from '@vben/art-hooks/common';
import { ArtTable, ArtTableHeader, ArtTablePanel, useTableColumns } from '@vben/art-hooks/table';
import { Page } from '@vben/common-ui';

import { ElButton, ElCard, ElFormItem, ElInput, ElMessage, ElOption, ElSelect, ElTag } from 'element-plus';

import { markBpmCopyRead, queryMyBpmCopyPage } from '#/api/system/bpm';

import BpmInstanceDetailDrawer from './components/bpm-instance-detail-drawer.vue';

defineOptions({ name: 'SystemBpmRuntimeMyCopyList' });

const loading = ref(false);
const showSearchBar = ref(true);
const rows = ref<BpmInstanceCopyRecord[]>([]);
const detailDrawerRef = ref<InstanceType<typeof BpmInstanceDetailDrawer>>();

const searchForm = reactive({
  instanceNo: '',
  readState: undefined as number | undefined,
  title: '',
});

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const columnsFactory = (): ColumnOption<BpmInstanceCopyRecord>[] => [
  { type: 'globalIndex', label: '序号', width: 70, align: 'center' },
  { prop: 'instanceNo', label: '实例编号', minWidth: 150 },
  { prop: 'title', label: '流程标题', minWidth: 220 },
  { prop: 'copyType', label: '抄送类型', minWidth: 150, useSlot: true },
  { prop: 'sourceNodeName', label: '来源节点', minWidth: 150 },
  { prop: 'reasonSnapshot', label: '抄送原因', minWidth: 220, showOverflowTooltip: true },
  { prop: 'readState', label: '已读状态', width: 110, align: 'center', useSlot: true },
  { prop: 'sentAt', label: '发送时间', minWidth: 180 },
  { prop: 'actions', label: '操作', width: 100, align: 'center', fixed: 'right', useSlot: true },
];

const { columns, columnChecks } = useTableColumns(columnsFactory);

const hasPagination = computed(() => pagination.total > pagination.size);
const tableHeight = computed(() => (hasPagination.value ? 'calc(100% - 44px)' : '100%'));

function getCopyTypeLabel(value?: null | string) {
  const labelMap: Record<string, string> = {
    MANUAL_APPROVE_COPY: '审批通过抄送',
    MANUAL_REJECT_COPY: '审批拒绝抄送',
    MANUAL_RETURN_COPY: '退回发起人抄送',
  };
  return value ? (labelMap[value] ?? value) : '-';
}

function getReadStateLabel(value?: null | number) {
  return value === 1 ? '已读' : '未读';
}

function getReadStateType(value?: null | number) {
  return value === 1 ? 'info' : 'warning';
}

async function loadData() {
  loading.value = true;
  try {
    const result = await queryMyBpmCopyPage({
      instanceNo: searchForm.instanceNo,
      pageNum: pagination.current,
      pageSize: pagination.size,
      readState: searchForm.readState,
      title: searchForm.title,
    });
    rows.value = result?.list ?? [];
    pagination.total = result?.total ?? 0;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pagination.current = 1;
  void loadData();
}

function handleReset() {
  Object.assign(searchForm, {
    instanceNo: '',
    readState: undefined,
    title: '',
  });
  pagination.current = 1;
  void loadData();
}

function handleToggleSearchBar() {
  showSearchBar.value = !showSearchBar.value;
}

async function openDetail(row: BpmInstanceCopyRecord) {
  await markBpmCopyRead(row.copyId);
  await loadData();
  detailDrawerRef.value?.open(row.instanceId);
}

function handleCurrentChange(value: number) {
  pagination.current = value;
  void loadData();
}

function handleSizeChange(value: number) {
  pagination.size = value;
  pagination.current = 1;
  void loadData();
}

onMounted(() => {
  void loadData().catch((error) => {
    ElMessage.error(error?.message || '我的抄送加载失败');
  });
});
</script>
```

Use the template and scoped styles from `my-instance-list.vue`, adapted to class prefix `runtime-copy-page`, with search fields `实例编号`、`流程标题`、`已读状态`, table slots `copyType`、`readState`、`actions`, and footer `<BpmInstanceDetailDrawer ref="detailDrawerRef" />`.

- [ ] **Step 5: Run frontend tests and typecheck**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected: PASS.

- [ ] **Step 6: Commit task**

```powershell
git add hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts `
  hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/my-copy-list.vue `
  hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts `
  hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts
git commit -m "feat: 增加BPM我的抄送页面"
```

---

### Task 5: Todo Action Dialog With Optional Copy Employees

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/my-todo-list.vue`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`

**Interfaces:**
- Consumes: `queryEmployeePage(params)` from `hunyuan-design/apps/hunyuan-system/src/api/system/organization.ts`.
- Produces: local dialog state that submits `commentText` and `copyEmployeeIds` for approve/reject/return only; `transfer` remains unchanged.

- [ ] **Step 1: Write failing module test**

In `bpm-modules.test.ts`, assert:

```ts
expect(myTodoSource).toContain('queryEmployeePage');
expect(myTodoSource).toContain('copyEmployeeIds');
expect(myTodoSource).toContain('openActionDialog');
expect(myTodoSource).toContain('submitActionDialog');
expect(myTodoSource).not.toContain("ElMessageBox.prompt('请输入审批意见'");
```

- [ ] **Step 2: Run failing frontend test**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: FAIL because current approval actions use `ElMessageBox.prompt` and do not load employees.

- [ ] **Step 3: Add imports and dialog state**

In `my-todo-list.vue`, remove `ElMessageBox` from imports only after approve/reject/return no longer use it. Keep it for transfer until a separate transfer selector exists.

Add:

```ts
import type { EmployeeRecord } from '#/api/system/organization';

import { queryEmployeePage } from '#/api/system/organization';
```

Add action state:

```ts
type TodoActionType = 'approve' | 'reject' | 'return';

const actionDialogVisible = ref(false);
const actionSubmitting = ref(false);
const employeeLoading = ref(false);
const employeeOptions = ref<EmployeeRecord[]>([]);
const currentActionRow = ref<BpmTaskRecord>();

const actionForm = reactive({
  commentText: '',
  copyEmployeeIds: [] as number[],
  type: 'approve' as TodoActionType,
});
```

Add helpers:

```ts
function getActionDialogTitle() {
  if (actionForm.type === 'approve') {
    return '审批通过';
  }
  if (actionForm.type === 'reject') {
    return '审批拒绝';
  }
  return '退回发起人';
}

function getActionPlaceholder() {
  if (actionForm.type === 'approve') {
    return '同意';
  }
  if (actionForm.type === 'reject') {
    return '不同意';
  }
  return '请补充材料';
}

async function loadEmployeeOptions(keyword = '') {
  employeeLoading.value = true;
  try {
    const result = await queryEmployeePage({
      keyword,
      pageNum: 1,
      pageSize: 20,
    });
    employeeOptions.value = result?.list ?? [];
  } finally {
    employeeLoading.value = false;
  }
}

async function openActionDialog(type: TodoActionType, row: BpmTaskRecord) {
  currentActionRow.value = row;
  Object.assign(actionForm, {
    commentText: '',
    copyEmployeeIds: [],
    type,
  });
  actionDialogVisible.value = true;
  await loadEmployeeOptions();
}
```

- [ ] **Step 4: Replace approve/reject/return handlers**

Replace current `handleApprove`, `handleReject`, and `handleReturn` bodies:

```ts
function handleApprove(row: BpmTaskRecord) {
  void openActionDialog('approve', row);
}

function handleReject(row: BpmTaskRecord) {
  void openActionDialog('reject', row);
}

function handleReturn(row: BpmTaskRecord) {
  void openActionDialog('return', row);
}
```

Add submit function:

```ts
async function submitActionDialog() {
  if (!currentActionRow.value) {
    return;
  }
  actionSubmitting.value = true;
  try {
    const payload = {
      commentText: actionForm.commentText,
      copyEmployeeIds: actionForm.copyEmployeeIds,
      taskId: currentActionRow.value.taskId,
    };
    if (actionForm.type === 'approve') {
      await approveBpmTask(payload);
      ElMessage.success('审批已通过');
    } else if (actionForm.type === 'reject') {
      await rejectBpmTask(payload);
      ElMessage.success('审批已拒绝');
    } else {
      await returnBpmTaskToInitiator(payload);
      ElMessage.success('已退回发起人');
    }
    actionDialogVisible.value = false;
    await loadData();
  } finally {
    actionSubmitting.value = false;
  }
}
```

- [ ] **Step 5: Add dialog template**

Add below the task detail dialog:

```vue
<ElDialog v-model="actionDialogVisible" :title="getActionDialogTitle()" width="560px">
  <div class="runtime-task-page__action-form">
    <ElFormItem label="处理意见">
      <ElInput
        v-model="actionForm.commentText"
        :placeholder="getActionPlaceholder()"
        type="textarea"
        :rows="3"
      />
    </ElFormItem>
    <ElFormItem label="抄送员工">
      <ElSelect
        v-model="actionForm.copyEmployeeIds"
        clearable
        filterable
        multiple
        :loading="employeeLoading"
        placeholder="可选，选择需要知会的员工"
        remote
        :remote-method="loadEmployeeOptions"
        style="width: 100%"
      >
        <ElOption
          v-for="employee in employeeOptions"
          :key="employee.employeeId"
          :label="employee.actualName"
          :value="employee.employeeId"
        />
      </ElSelect>
    </ElFormItem>
  </div>
  <template #footer>
    <ElButton @click="actionDialogVisible = false">取消</ElButton>
    <ElButton :loading="actionSubmitting" type="primary" @click="submitActionDialog">
      确定
    </ElButton>
  </template>
</ElDialog>
```

Add scoped style:

```css
.runtime-task-page__action-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.runtime-task-page__action-form :deep(.el-form-item) {
  margin-bottom: 0;
}
```

- [ ] **Step 6: Run frontend verification**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected: PASS.

- [ ] **Step 7: Commit task**

```powershell
git add hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/my-todo-list.vue `
  hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts
git commit -m "feat: 待办审批支持手工抄送"
```

---

### Task 6: Verification And Acceptance Record

**Files:**
- Create: `docs/superpowers/specs/2026-07-08-bpm-manual-copy-acceptance.md`

**Interfaces:**
- Consumes: all prior tasks.
- Produces: durable acceptance note with exact commands, scope, and boundaries.

- [ ] **Step 1: Run backend tests**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmInstanceCopyServiceTest,BpmRuntimeCommandServiceTest test
```

Expected: PASS.

- [ ] **Step 2: Run frontend contract tests**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: PASS.

- [ ] **Step 3: Run frontend typecheck**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected: PASS.

- [ ] **Step 4: Optional browser acceptance when services are running**

Preconditions:
- Frontend: `http://127.0.0.1:5788`
- Backend: `http://127.0.0.1:1024`
- Persistent Playwright MCP controller: `http://localhost:8934`

Flow:
1. 审批人登录，进入“我的待办”。
2. 对一条待办执行通过、拒绝或退回，选择一个抄送员工。
3. 被抄送员工登录，进入“我的抄送”。
4. 列表看到该流程记录。
5. 点击“详情”，统一实例详情抽屉打开。
6. 返回列表后该记录变为已读。

- [ ] **Step 5: Write acceptance record**

Create `docs/superpowers/specs/2026-07-08-bpm-manual-copy-acceptance.md`:

```markdown
# BPM 手工抄送运行端验收记录

## Scope

- 审批通过、审批拒绝、退回发起人支持可选 `copyEmployeeIds`。
- 后端写入 `t_bpm_instance_copy`，被抄送员工可查询“我的抄送”并标记已读。
- 前端新增“我的抄送”列表页，复用统一实例详情抽屉。

## Verification

- `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmInstanceCopyServiceTest,BpmRuntimeCommandServiceTest test`
  - Result: PASS
- `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom`
  - Result: PASS
- `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck`
  - Result: PASS

## Boundaries

- 未实现自动抄送节点。
- 未修改 simpleModel 设计器、BPMN 编译器或发布流程。
- 未扩大被抄送人的审批权限。
- 未新增管理员抄送管理页。
- 未新增依赖。
```

- [ ] **Step 6: Commit acceptance**

```powershell
git add docs/superpowers/specs/2026-07-08-bpm-manual-copy-acceptance.md
git commit -m "docs: 记录BPM手工抄送验收"
```

---

## Full Verification Gate

Run after all tasks:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmInstanceCopyServiceTest,BpmRuntimeCommandServiceTest test
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected: all commands PASS.

## Self-Review Notes

- Spec coverage: approve/reject/return 手工抄送、写入 `t_bpm_instance_copy`、“我的抄送”列表、详情已读、菜单授权、测试验收均有对应任务。
- Scope boundary: plan excludes automatic copy nodes, designer/compiler changes, transfer copy, admin copy page, extra dependencies.
- Type consistency: backend uses `copyEmployeeIds`, `BpmInstanceCopyQueryForm`, `BpmInstanceCopyVO`, `BpmCopyTypeEnum`; frontend mirrors `BpmInstanceCopyRecord`, `BpmInstanceCopyPageQueryParams`, `queryMyBpmCopyPage`, `markBpmCopyRead`.
- Verification: backend Maven, frontend Vitest, frontend typecheck, optional Playwright business flow are separated so source contract checks can pass without live services.
