package com.hunyuan.sa.bpm.schema;
import org.junit.jupiter.api.Test; import java.nio.charset.StandardCharsets; import java.nio.file.*; import static org.assertj.core.api.Assertions.assertThat;
class BpmM6SchemaSourceTest {
 @Test void migrationMustContainM6ControlPlaneAndPermissionClosure() throws Exception {String sql=Files.readString(Path.of("..","..","数据库SQL脚本","mysql","sql-update-log","v3.58.0.sql"),StandardCharsets.UTF_8);assertThat(sql).contains("t_bpm_source_system_version","t_bpm_source_application","t_bpm_external_employee_mapping","t_bpm_external_public_reference","t_bpm_process_binding_version","t_bpm_event_subscription_version","uk_bpm_callback_event_subscription","information_schema.columns","information_schema.statistics","/system/bpm/integration/configuration-workbench.vue","bpm:integration-config:query","bpm:integration-config:update","bpm:integration-config:publish");}
}
