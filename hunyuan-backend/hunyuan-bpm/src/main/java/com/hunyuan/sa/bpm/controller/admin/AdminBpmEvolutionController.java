package com.hunyuan.sa.bpm.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.evolution.domain.form.BpmMigrationPreviewForm;
import com.hunyuan.sa.bpm.module.evolution.domain.form.BpmMigrationDispositionForm;
import com.hunyuan.sa.bpm.module.evolution.domain.vo.BpmMigrationBatchDetailVO;
import com.hunyuan.sa.bpm.module.evolution.domain.vo.BpmMigrationOperationVO;
import com.hunyuan.sa.bpm.module.evolution.domain.vo.GraphEvolutionDiffVO;
import com.hunyuan.sa.bpm.module.evolution.service.BpmDefinitionEvolutionService;
import com.hunyuan.sa.bpm.module.evolution.service.BpmInstanceMigrationService;
import com.hunyuan.sa.bpm.module.evolution.domain.vo.BpmAffectedInstanceVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class AdminBpmEvolutionController {
    @Resource private BpmDefinitionEvolutionService evolutionService;
    @Resource private BpmInstanceMigrationService migrationService;

    @GetMapping("/bpm/evolution/diff")
    @SaCheckPermission("bpm:evolution:query")
    public ResponseDTO<GraphEvolutionDiffVO> diff(@RequestParam Long sourceVersionId, @RequestParam Long targetVersionId) {
        return evolutionService.diff(sourceVersionId, targetVersionId);
    }

    @GetMapping("/bpm/evolution/affected")
    @SaCheckPermission("bpm:evolution:query")
    public ResponseDTO<List<BpmAffectedInstanceVO>> affected(@RequestParam Long sourceVersionId) {
        return evolutionService.affectedInstances(sourceVersionId);
    }

    @PostMapping("/bpm/evolution/migration/preview")
    @SaCheckPermission("bpm:evolution:preview")
    public ResponseDTO<BpmMigrationOperationVO> preview(@RequestBody BpmMigrationPreviewForm form) {
        return narrow(migrationService.preview(form));
    }

    @PostMapping("/bpm/evolution/migration/{batchId}/execute")
    @SaCheckPermission("bpm:evolution:execute")
    public ResponseDTO<BpmMigrationOperationVO> execute(@PathVariable Long batchId) {
        return narrow(migrationService.execute(batchId));
    }

    @GetMapping("/bpm/evolution/migration/{batchId}")
    @SaCheckPermission("bpm:evolution:audit")
    public ResponseDTO<BpmMigrationBatchDetailVO> detail(@PathVariable Long batchId) {
        return migrationService.detail(batchId);
    }

    @PostMapping("/bpm/evolution/migration/item/{itemId}/dispose")
    @SaCheckPermission("bpm:evolution:execute")
    public ResponseDTO<BpmMigrationOperationVO> dispose(@PathVariable Long itemId,
                                                          @RequestBody BpmMigrationDispositionForm form) {
        return narrow(migrationService.dispose(itemId, form));
    }

    private ResponseDTO<BpmMigrationOperationVO> narrow(ResponseDTO<BpmMigrationBatchDetailVO> response) {
        if (!Boolean.TRUE.equals(response.getOk()) || response.getData() == null) {
            return ResponseDTO.error(response);
        }
        return ResponseDTO.ok(BpmMigrationOperationVO.from(response.getData()));
    }
}
