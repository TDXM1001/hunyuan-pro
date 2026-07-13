package com.hunyuan.sa.bpm.controller.admin;
import cn.dev33.satoken.annotation.SaCheckPermission; import com.hunyuan.sa.base.common.domain.ResponseDTO; import com.hunyuan.sa.bpm.module.integration.domain.entity.*; import com.hunyuan.sa.bpm.module.integration.service.BpmM6ConfigurationService; import org.springframework.web.bind.annotation.*; import java.util.List;
@RestController @RequestMapping("/bpm/integration/config")
public class AdminBpmM6ConfigurationController {
 private final BpmM6ConfigurationService service; public AdminBpmM6ConfigurationController(BpmM6ConfigurationService s){service=s;}
 @GetMapping("/systems") @SaCheckPermission("bpm:integration-config:query") public ResponseDTO<List<BpmSourceSystemVersionEntity>> systems(){return ResponseDTO.ok(service.listSystems());}
 @PostMapping("/systems") @SaCheckPermission("bpm:integration-config:update") public ResponseDTO<Long> saveSystem(@RequestBody BpmSourceSystemVersionEntity e){return ResponseDTO.ok(service.saveSystem(e));}
 @PostMapping("/systems/{id}/publish") @SaCheckPermission("bpm:integration-config:publish") public ResponseDTO<Long> publishSystem(@PathVariable Long id){return ResponseDTO.ok(service.publishSystem(id));}
 @GetMapping("/applications") @SaCheckPermission("bpm:integration-config:query") public ResponseDTO<List<BpmSourceApplicationEntity>> applications(){return ResponseDTO.ok(service.listApplications());}
 @PostMapping("/applications") @SaCheckPermission("bpm:integration-config:update") public ResponseDTO<Long> saveApplication(@RequestBody BpmSourceApplicationEntity e){return ResponseDTO.ok(service.saveApplication(e));}
 @GetMapping("/mappings") @SaCheckPermission("bpm:integration-config:query") public ResponseDTO<List<BpmExternalEmployeeMappingEntity>> mappings(){return ResponseDTO.ok(service.listMappings());}
 @PostMapping("/mappings") @SaCheckPermission("bpm:integration-config:update") public ResponseDTO<Long> saveMapping(@RequestBody BpmExternalEmployeeMappingEntity e){return ResponseDTO.ok(service.saveMapping(e));}
 @GetMapping("/bindings") @SaCheckPermission("bpm:integration-config:query") public ResponseDTO<List<BpmProcessBindingVersionEntity>> bindings(){return ResponseDTO.ok(service.listBindings());}
 @PostMapping("/bindings") @SaCheckPermission("bpm:integration-config:update") public ResponseDTO<Long> saveBinding(@RequestBody BpmProcessBindingVersionEntity e){return ResponseDTO.ok(service.saveBinding(e));}
 @PostMapping("/bindings/{id}/publish") @SaCheckPermission("bpm:integration-config:publish") public ResponseDTO<Long> publishBinding(@PathVariable Long id){return ResponseDTO.ok(service.publishBinding(id));}
 @GetMapping("/subscriptions") @SaCheckPermission("bpm:integration-config:query") public ResponseDTO<List<BpmEventSubscriptionVersionEntity>> subscriptions(){return ResponseDTO.ok(service.listSubscriptions());}
 @PostMapping("/subscriptions") @SaCheckPermission("bpm:integration-config:update") public ResponseDTO<Long> saveSubscription(@RequestBody BpmEventSubscriptionVersionEntity e){return ResponseDTO.ok(service.saveSubscription(e));}
 @PostMapping("/subscriptions/{id}/publish") @SaCheckPermission("bpm:integration-config:publish") public ResponseDTO<Long> publishSubscription(@PathVariable Long id){return ResponseDTO.ok(service.publishSubscription(id));}
}
