# File 本地运行存储与上传一致性验收记录

## 验收范围

- dev 环境文件上传默认写入项目内 `hunyuan-backend/runtime/upload/`。
- `hunyuan-backend/runtime/upload/` 被 Git 忽略，不提交真实上传文件。
- `FileService.fileUpload()` 在存储上传成功但数据库落库失败时执行删除补偿。
- 补偿删除失败时不误报上传成功，并保留原始落库异常。

## 验收结果

通过。

## 验证命令

| 门禁 | 命令 | 结果 |
| --- | --- | --- |
| 文件聚焦门禁 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-base '-Dtest=FileServiceTest,FileLocalStorageConfigContractTest' test` | PASS；6 tests，0 failures，0 errors |
| hunyuan-base 模块门禁 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-base test` | PASS；12 tests，0 failures，0 errors |
| Git 忽略验证 | `git check-ignore -v hunyuan-backend/runtime/upload/example.tmp` | PASS；命中 `.gitignore:44:/hunyuan-backend/runtime/upload/` |

## 边界说明

- 本轮没有改对象存储 S3 兼容逻辑。
- 本轮没有新增文件引用关系表。
- 本轮没有新增文件管理前端页面。
- 本轮不扫描历史孤儿文件，只处理本次上传后落库失败的即时补偿。
- `FileServiceTest` 中的补偿删除异常日志是测试刻意触发的失败场景，用于证明删除补偿失败时仍保留原始落库异常。

## 非阻塞项

- 本机 Maven 配置 `F:\maven\apache-maven-3.9.11\conf\settings.xml` 仍有 line 235 解析警告，但本轮 Maven 门禁退出 0。
- Lombok `EqualsAndHashCode(callSuper=false)`、Redis 序列化过时提示等编译 warning 为既有提示，本轮未扩大处理。
