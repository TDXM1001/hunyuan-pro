# File 本地运行存储与上传一致性设计

## 结论

本轮采用 **项目内本地运行目录 + Git 忽略 + 上传落库失败补偿**。

文件先存储在当前项目的本地运行目录中，推荐路径为 `hunyuan-backend/runtime/upload/`。该目录只保存本机运行产生的上传文件，不属于源码、配置或验收文档，必须加入 `.gitignore`，避免真实文件进入 Git。

同时补齐当前 `FileService.fileUpload()` 的一致性缺口：存储上传成功后，如果数据库文件记录落库失败，必须调用 `IFileStorageService.delete(fileKey)` 删除刚上传的文件，避免出现不可追踪的本地残留文件。

## 当前证据

- `FileService.fileUpload()` 当前流程是：校验文件 -> `fileStorageService.upload()` -> `fileDao.insert()` -> 返回 `fileId`。
- `IFileStorageService` 已有 `delete(String fileKey)`，本轮无需新增存储接口。
- `FileStorageLocalServiceImpl` 已支持本地上传、URL 生成、下载和删除。
- dev 配置当前使用 `${localPath:/home}/hunyuan/upload/`，在 Windows 本地开发中容易落到项目外部路径，不利于开发者定位与清理。
- `.gitignore` 当前没有忽略 `hunyuan-backend/runtime/upload/`。

## 目标

1. dev 环境本地上传文件默认落到当前仓库内的运行时目录。
2. 上传文件目录不进入 Git。
3. 上传成功但 DB 落库失败时自动删除刚上传的文件。
4. 保持现有上传 API、返回体和前端调用方式不变。
5. 不引入新依赖，不改对象存储能力。

## 非目标

- 不建设大文件分片上传。
- 不改造云存储 S3 兼容逻辑。
- 不新增文件引用关系表。
- 不新增文件管理前端页面。
- 不处理历史已经残留在磁盘上的孤儿文件。
- 不改变 prod、pre、test 环境的上传目录。

## 方案取舍

### 方案 A：项目内 runtime 目录（采用）

dev 环境将 `file.storage.local.upload-path` 调整为 `./runtime/upload/` 或等价项目内路径，并在 `.gitignore` 忽略 `/hunyuan-backend/runtime/upload/`。

优点：

- 上传文件和后端运行上下文靠近，开发者容易定位。
- 不污染源码目录。
- Git 边界清楚，不会误提交真实文件。
- 不依赖操作系统固定目录，Windows 与 Linux 开发机都更容易启动。

代价：

- 如果从不同工作目录启动后端，相对路径可能变化。因此实施时应确认当前启动方式下路径解析稳定，并在设计记录中明确推荐从 `hunyuan-backend` 或打包 jar 所在上下文启动。

### 方案 B：继续使用项目外 `/home` 或本机绝对目录

优点是行为接近原始配置。缺点是 Windows 本地开发不直观，多个项目容易混用同一目录，也不符合“文件先存储在本地项目中”的要求。

### 方案 C：把上传文件放入 `src/main/resources`

不采用。该目录属于源码资源，会参与构建和打包，上传文件不应该进入 classpath。

### 方案 D：把上传文件放入 `target`

不采用。`target` 是构建产物目录，Maven 清理会删除它，不适合作为开发态持久上传目录。

## 设计

### 本地目录边界

新增运行时目录约定：

```text
hunyuan-backend/runtime/upload/
```

Git 忽略规则：

```gitignore
/hunyuan-backend/runtime/upload/
```

如果后续需要保留目录说明，只提交 `hunyuan-backend/runtime/README.md`，不提交 `upload/` 下的任何真实文件。

### dev 配置

仅调整 dev 配置：

```yaml
file:
  storage:
    local:
      upload-path: ${localPath:./runtime}/upload/
```

含义：

- 未传 `localPath` 时，dev 默认使用项目内 `runtime/upload/`。
- 仍保留 `localPath` 覆盖能力，方便临时切到其他磁盘。
- `prod`、`pre`、`test` 暂不改，避免影响非本地环境。

### 上传一致性补偿

当前缺口发生在：

1. `fileStorageService.upload()` 已经成功。
2. `fileDao.insert(fileEntity)` 抛出运行时异常或返回异常结果。
3. 方法直接失败，存储层文件留在磁盘或对象存储中，但数据库没有记录。

本轮补偿逻辑：

```text
上传成功 -> 尝试落库
  -> 落库成功：返回 fileId
  -> 落库失败：调用 fileStorageService.delete(fileKey) 清理刚上传文件，再抛出原始异常
```

如果补偿删除也失败：

- 记录中文错误日志，包含 `fileKey` 和落库异常摘要。
- 不吞掉原始落库失败。
- 不返回上传成功。

### 错误处理

- 参数校验、文件安全校验、存储上传失败仍沿用现有 `ResponseDTO`。
- 落库失败属于服务端一致性失败，保留运行时异常传播，由全局异常处理返回统一错误。
- 补偿失败只记录日志，不覆盖原始落库异常，避免排查时丢失真正失败点。

### 测试策略

新增 `FileServiceTest`，用 mock 存储和 mock DAO 做最小 TDD：

1. 上传成功且落库成功时：
   - 返回成功。
   - `fileId` 写回 `FileUploadVO`。
   - 不调用 `delete(fileKey)`。

2. 上传成功但落库抛异常时：
   - 调用 `delete(fileKey)`。
   - 继续抛出原始落库异常。

3. 上传成功、落库失败、补偿删除也失败时：
   - 仍抛出原始落库异常。
   - 不把补偿失败伪装成上传成功。

4. 存储上传失败时：
   - 直接返回存储失败响应。
   - 不调用 DAO。
   - 不调用删除补偿。

配置层验证：

- 通过源文件测试或文本合同测试确认 dev 配置默认路径包含 `./runtime` 和 `upload`。
- 通过 `.gitignore` 文本合同测试确认 `/hunyuan-backend/runtime/upload/` 被忽略。

## 验收标准

- dev 环境默认上传目录在 `hunyuan-backend/runtime/upload/`。
- `hunyuan-backend/runtime/upload/` 不会被 Git 跟踪。
- 上传成功但落库失败时会调用 `IFileStorageService.delete(fileKey)`。
- 补偿删除失败不会让接口误报上传成功。
- 现有文件上传、URL 获取、下载 API 形态不变。
- 后端聚焦测试通过。

## 验证命令

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-base '-Dtest=FileServiceTest' test
```

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-base test
```

```powershell
git check-ignore -v hunyuan-backend/runtime/upload/example.tmp
```

## 理解校验

- 这不是文件平台重构，而是先补本地开发和上传一致性的最小可靠性缺口。
- 本轮只处理“刚上传但未落库”的即时补偿，不扫描历史孤儿文件。
- 本地目录进入项目树，但运行文件不进入 Git。
- 对象存储仍保留原有模式，后续如需对象存储一致性验收，可复用同一补偿语义单独做。
