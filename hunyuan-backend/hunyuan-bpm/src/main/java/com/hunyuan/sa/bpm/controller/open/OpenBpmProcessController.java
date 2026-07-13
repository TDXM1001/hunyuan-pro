package com.hunyuan.sa.bpm.controller.open;

import com.alibaba.fastjson.JSON; import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.integration.domain.command.*; import com.hunyuan.sa.bpm.module.integration.domain.model.*;
import com.hunyuan.sa.bpm.module.integration.service.*; import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import cn.dev33.satoken.annotation.SaIgnore;
import com.hunyuan.sa.base.common.annoation.NoNeedLogin;

@RestController @RequestMapping("/open/bpm/v1")
public class OpenBpmProcessController {
 private final BpmSourceSystemService sources; private final BpmExternalProcessService processes;
 public OpenBpmProcessController(BpmSourceSystemService s,BpmExternalProcessService p){sources=s;processes=p;}
 @SaIgnore @NoNeedLogin @PostMapping("/processes") public ResponseDTO<String> start(@RequestBody String body,HttpServletRequest r){BpmSourceApplicationPrincipal p=authenticate(body,r);return ResponseDTO.ok(processes.start(p,JSON.parseObject(body,BpmExternalStartCommand.class)));}
 @SaIgnore @NoNeedLogin @GetMapping("/tasks/{taskNo}") public ResponseDTO<BpmExternalTaskView> task(@PathVariable String taskNo,@RequestParam String externalEmployeeId,HttpServletRequest r){BpmSourceApplicationPrincipal p=authenticate("",r);return ResponseDTO.ok(processes.getTask(p,externalEmployeeId,taskNo));}
 @SaIgnore @NoNeedLogin @GetMapping("/processes/{instanceNo}/tasks") public ResponseDTO<java.util.List<BpmExternalTaskView>> tasks(@PathVariable String instanceNo,@RequestParam String externalEmployeeId,HttpServletRequest r){BpmSourceApplicationPrincipal p=authenticate("",r);return ResponseDTO.ok(processes.listTasks(p,externalEmployeeId,instanceNo));}
 @SaIgnore @NoNeedLogin @PostMapping("/tasks/{taskNo}/actions") public ResponseDTO<String> act(@PathVariable String taskNo,@RequestBody String body,HttpServletRequest r){BpmSourceApplicationPrincipal p=authenticate(body,r);processes.act(p,taskNo,JSON.parseObject(body,BpmExternalTaskActionCommand.class));return ResponseDTO.ok();}
 private BpmSourceApplicationPrincipal authenticate(String body,HttpServletRequest r){String target=r.getRequestURI()+(r.getQueryString()==null?"":"?"+r.getQueryString());return sources.authenticate(r.getHeader("X-Bpm-App"),r.getHeader("X-Bpm-Timestamp"),r.getHeader("X-Bpm-Nonce"),r.getHeader("X-Bpm-Signature"),r.getMethod(),target,body);}
 @ExceptionHandler(SecurityException.class) public ResponseDTO<String> security(){return ResponseDTO.error(UserErrorCode.NO_PERMISSION);}
 @ExceptionHandler({IllegalArgumentException.class,IllegalStateException.class}) public ResponseDTO<String> contract(RuntimeException ex){return ResponseDTO.userErrorParam(ex.getMessage());}
}
