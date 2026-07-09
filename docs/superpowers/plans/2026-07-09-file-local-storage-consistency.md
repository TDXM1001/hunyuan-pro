# File Local Storage Consistency Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 dev 环境文件上传默认落到项目内本地运行目录，并在上传成功但数据库落库失败时清理刚写入的文件。

**Architecture:** 本轮只改 `hunyuan-base` 文件模块和根 `.gitignore`。先用单元测试锁住 `FileService.fileUpload()` 的补偿语义，再做最小实现；配置层通过文本合同测试锁住 dev 本地目录和 Git 忽略规则。对象存储、文件引用表、大文件上传和历史孤儿文件清理不进入本轮。

**Tech Stack:** Java 17, Spring Boot 3, Maven, JUnit 5, Mockito, AssertJ, PowerShell, UTF-8 Markdown/YAML.

## Global Constraints

- 当前仓库：`E:/my-project/hunyuan-pro`
- 所有新增或编辑文本文件使用 UTF-8。
- 不新增依赖。
- 不改对象存储 S3 兼容逻辑。
- 不新增文件管理前端页面。
- 不提交 `hunyuan-backend/runtime/upload/` 下的真实上传文件。
- 代码注释如需新增，使用中文注释，并只解释补偿这类非显然逻辑。
- 保持现有文件上传 API、返回体和前端调用方式不变。

---

## File Structure

- Modify: `.gitignore`
  - 追加 `/hunyuan-backend/runtime/upload/`，防止本地上传文件进入 Git。
- Modify: `hunyuan-backend/hunyuan-base/src/main/resources/dev/hunyuan-base.yaml`
  - 将 dev 默认 `file.storage.local.upload-path` 改为 `${localPath:./runtime}/upload/`。
- Modify: `hunyuan-backend/hunyuan-base/src/main/java/com/hunyuan/sa/base/module/support/file/service/FileService.java`
  - 在 `fileDao.insert(fileEntity)` 失败时调用 `fileStorageService.delete(uploadVO.getFileKey())`。
  - 删除补偿失败时记录中文错误日志，不覆盖原始落库异常。
- Create: `hunyuan-backend/hunyuan-base/src/test/java/com/hunyuan/sa/base/module/support/file/service/FileServiceTest.java`
  - 覆盖上传成功、上传失败、落库失败补偿、补偿失败保留原始异常。
- Create: `hunyuan-backend/hunyuan-base/src/test/java/com/hunyuan/sa/base/module/support/file/FileLocalStorageConfigContractTest.java`
  - 文本合同测试，锁住 dev 本地上传目录和 `.gitignore` 规则。

---

### Task 1: Lock File Upload Compensation Behavior

**Files:**
- Create: `hunyuan-backend/hunyuan-base/src/test/java/com/hunyuan/sa/base/module/support/file/service/FileServiceTest.java`
- Modify: `hunyuan-backend/hunyuan-base/src/main/java/com/hunyuan/sa/base/module/support/file/service/FileService.java`

**Interfaces:**
- Consumes:
  - `FileService.fileUpload(MultipartFile file, Integer folderType, RequestUser requestUser)`
  - `IFileStorageService.upload(MultipartFile file, String path)`
  - `IFileStorageService.delete(String fileKey)`
  - `FileDao.insert(FileEntity entity)`
- Produces:
  - 上传成功但落库失败时，刚上传的 `fileKey` 被删除。
  - 补偿删除失败时，仍抛出原始落库异常。

- [ ] **Step 1: Write the failing FileService compensation tests**

Create `hunyuan-backend/hunyuan-base/src/test/java/com/hunyuan/sa/base/module/support/file/service/FileServiceTest.java` with this content:

```java
package com.hunyuan.sa.base.module.support.file.service;

import com.hunyuan.sa.base.common.domain.RequestUser;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.enumeration.UserTypeEnum;
import com.hunyuan.sa.base.module.support.file.constant.FileFolderTypeEnum;
import com.hunyuan.sa.base.module.support.file.dao.FileDao;
import com.hunyuan.sa.base.module.support.file.domain.entity.FileEntity;
import com.hunyuan.sa.base.module.support.file.domain.vo.FileUploadVO;
import com.hunyuan.sa.base.module.support.securityprotect.service.SecurityFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileServiceTest {

    private FileService fileService;

    private IFileStorageService fileStorageService;

    private FileDao fileDao;

    private SecurityFileService securityFileService;

    @BeforeEach
    void setUp() {
        fileService = new FileService();
        fileStorageService = mock(IFileStorageService.class);
        fileDao = mock(FileDao.class);
        securityFileService = mock(SecurityFileService.class);

        ReflectionTestUtils.setField(fileService, "fileStorageService", fileStorageService);
        ReflectionTestUtils.setField(fileService, "fileDao", fileDao);
        ReflectionTestUtils.setField(fileService, "securityFileService", securityFileService);
    }

    @Test
    void fileUploadShouldPersistRecordAndKeepStorageWhenDbInsertSucceeds() {
        MultipartFile file = buildFile();
        when(securityFileService.checkFile(file)).thenReturn(ResponseDTO.ok());
        when(fileStorageService.upload(file, FileFolderTypeEnum.COMMON.getFolder()))
                .thenReturn(ResponseDTO.ok(buildUploadVO("private/common/demo.txt")));
        when(fileDao.insert(any(FileEntity.class))).thenAnswer(invocation -> {
            FileEntity entity = invocation.getArgument(0);
            entity.setFileId(99L);
            return 1;
        });

        ResponseDTO<FileUploadVO> response = fileService.fileUpload(file, FileFolderTypeEnum.COMMON.getValue(), buildRequestUser());

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getFileId()).isEqualTo(99L);
        ArgumentCaptor<FileEntity> entityCaptor = ArgumentCaptor.forClass(FileEntity.class);
        verify(fileDao).insert(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getFileKey()).isEqualTo("private/common/demo.txt");
        assertThat(entityCaptor.getValue().getFileName()).isEqualTo("demo.txt");
        verify(fileStorageService, never()).delete("private/common/demo.txt");
    }

    @Test
    void fileUploadShouldDeleteUploadedFileWhenDbInsertFails() {
        MultipartFile file = buildFile();
        RuntimeException dbException = new IllegalStateException("db down");
        when(securityFileService.checkFile(file)).thenReturn(ResponseDTO.ok());
        when(fileStorageService.upload(file, FileFolderTypeEnum.COMMON.getFolder()))
                .thenReturn(ResponseDTO.ok(buildUploadVO("private/common/demo.txt")));
        when(fileDao.insert(any(FileEntity.class))).thenThrow(dbException);
        when(fileStorageService.delete("private/common/demo.txt")).thenReturn(ResponseDTO.ok());

        assertThatThrownBy(() -> fileService.fileUpload(file, FileFolderTypeEnum.COMMON.getValue(), buildRequestUser()))
                .isSameAs(dbException);

        verify(fileStorageService).delete("private/common/demo.txt");
    }

    @Test
    void fileUploadShouldKeepOriginalDbExceptionWhenCompensationDeleteFails() {
        MultipartFile file = buildFile();
        RuntimeException dbException = new IllegalStateException("db down");
        when(securityFileService.checkFile(file)).thenReturn(ResponseDTO.ok());
        when(fileStorageService.upload(file, FileFolderTypeEnum.COMMON.getFolder()))
                .thenReturn(ResponseDTO.ok(buildUploadVO("private/common/demo.txt")));
        when(fileDao.insert(any(FileEntity.class))).thenThrow(dbException);
        when(fileStorageService.delete("private/common/demo.txt"))
                .thenThrow(new IllegalStateException("delete failed"));

        assertThatThrownBy(() -> fileService.fileUpload(file, FileFolderTypeEnum.COMMON.getValue(), buildRequestUser()))
                .isSameAs(dbException);

        verify(fileStorageService).delete("private/common/demo.txt");
    }

    @Test
    void fileUploadShouldReturnStorageFailureWithoutDbInsertOrCompensation() {
        MultipartFile file = buildFile();
        when(securityFileService.checkFile(file)).thenReturn(ResponseDTO.ok());
        when(fileStorageService.upload(file, FileFolderTypeEnum.COMMON.getFolder()))
                .thenReturn(ResponseDTO.userErrorParam("上传失败"));

        ResponseDTO<FileUploadVO> response = fileService.fileUpload(file, FileFolderTypeEnum.COMMON.getValue(), buildRequestUser());

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("上传失败");
        verify(fileDao, never()).insert(any(FileEntity.class));
        verify(fileStorageService, never()).delete(any());
    }

    private MockMultipartFile buildFile() {
        return new MockMultipartFile(
                "file",
                "demo.txt",
                "text/plain",
                "hello".getBytes(StandardCharsets.UTF_8)
        );
    }

    private FileUploadVO buildUploadVO(String fileKey) {
        FileUploadVO uploadVO = new FileUploadVO();
        uploadVO.setFileKey(fileKey);
        uploadVO.setFileName("demo.txt");
        uploadVO.setFileType("txt");
        uploadVO.setFileSize(5L);
        uploadVO.setFileUrl("http://localhost/upload/" + fileKey);
        return uploadVO;
    }

    private RequestUser buildRequestUser() {
        return new RequestUser() {
            @Override
            public Long getUserId() {
                return 1L;
            }

            @Override
            public String getUserName() {
                return "管理员";
            }

            @Override
            public UserTypeEnum getUserType() {
                return UserTypeEnum.ADMIN_EMPLOYEE;
            }

            @Override
            public String getIp() {
                return "127.0.0.1";
            }

            @Override
            public String getUserAgent() {
                return "JUnit";
            }
        };
    }
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-base '-Dtest=FileServiceTest' test
```

Expected:

- Command fails.
- `fileUploadShouldDeleteUploadedFileWhenDbInsertFails` fails because `fileStorageService.delete("private/common/demo.txt")` was not invoked.
- `fileUploadShouldKeepOriginalDbExceptionWhenCompensationDeleteFails` fails for the same missing compensation behavior.

- [ ] **Step 3: Add minimal compensation implementation**

Modify `hunyuan-backend/hunyuan-base/src/main/java/com/hunyuan/sa/base/module/support/file/service/FileService.java`.

Add Lombok logger import and annotation:

```java
import lombok.extern.slf4j.Slf4j;
```

Change the class annotation from:

```java
@Service
public class FileService {
```

to:

```java
@Slf4j
@Service
public class FileService {
```

Replace:

```java
        fileDao.insert(fileEntity);
```

with:

```java
        try {
            fileDao.insert(fileEntity);
        } catch (RuntimeException ex) {
            compensateUploadedFile(uploadVO.getFileKey(), ex);
            throw ex;
        }
```

Add this private method near the bottom of the class before `queryPage` or before the closing brace:

```java
    /**
     * 上传已成功但文件记录落库失败时，清理刚写入的存储文件，避免出现不可追踪的孤儿文件。
     */
    private void compensateUploadedFile(String fileKey, RuntimeException dbException) {
        if (StringUtils.isBlank(fileKey)) {
            log.error("文件上传落库失败，且上传结果缺少 fileKey，无法执行补偿清理", dbException);
            return;
        }
        try {
            ResponseDTO<String> deleteResponse = fileStorageService.delete(fileKey);
            if (!Boolean.TRUE.equals(deleteResponse.getOk())) {
                log.error("文件上传落库失败后的补偿删除未成功，fileKey={}，deleteMsg={}", fileKey, deleteResponse.getMsg(), dbException);
            }
        } catch (RuntimeException deleteException) {
            log.error("文件上传落库失败后的补偿删除异常，fileKey={}", fileKey, deleteException);
        }
    }
```

- [ ] **Step 4: Run focused test and verify GREEN**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-base '-Dtest=FileServiceTest' test
```

Expected:

- `BUILD SUCCESS`
- `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 5: Commit Task 1**

Run:

```powershell
git add hunyuan-backend/hunyuan-base/src/main/java/com/hunyuan/sa/base/module/support/file/service/FileService.java hunyuan-backend/hunyuan-base/src/test/java/com/hunyuan/sa/base/module/support/file/service/FileServiceTest.java
git diff --cached --check
git commit -m "feat: 增加文件上传落库失败补偿"
```

Expected:

- Staged files contain only `FileService.java` and `FileServiceTest.java`.
- Commit succeeds.

---

### Task 2: Lock Local Runtime Upload Directory and Git Boundary

**Files:**
- Modify: `.gitignore`
- Modify: `hunyuan-backend/hunyuan-base/src/main/resources/dev/hunyuan-base.yaml`
- Create: `hunyuan-backend/hunyuan-base/src/test/java/com/hunyuan/sa/base/module/support/file/FileLocalStorageConfigContractTest.java`

**Interfaces:**
- Consumes:
  - dev config property `file.storage.local.upload-path`
  - root `.gitignore`
- Produces:
  - dev default upload path `${localPath:./runtime}/upload/`
  - Git ignore rule `/hunyuan-backend/runtime/upload/`

- [ ] **Step 1: Write the failing config and git-boundary contract test**

Create `hunyuan-backend/hunyuan-base/src/test/java/com/hunyuan/sa/base/module/support/file/FileLocalStorageConfigContractTest.java` with this content:

```java
package com.hunyuan.sa.base.module.support.file;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FileLocalStorageConfigContractTest {

    @Test
    void devConfigShouldStoreLocalUploadFilesUnderProjectRuntimeDirectory() throws IOException {
        Path devConfigPath = repoRoot().resolve("hunyuan-backend/hunyuan-base/src/main/resources/dev/hunyuan-base.yaml");
        String devConfig = Files.readString(devConfigPath, StandardCharsets.UTF_8);

        assertThat(devConfig).contains("upload-path: ${localPath:./runtime}/upload/");
    }

    @Test
    void gitIgnoreShouldExcludeRuntimeUploadFiles() throws IOException {
        Path gitIgnorePath = repoRoot().resolve(".gitignore");
        String gitIgnore = Files.readString(gitIgnorePath, StandardCharsets.UTF_8);

        assertThat(gitIgnore).contains("/hunyuan-backend/runtime/upload/");
    }

    private Path repoRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.exists(current.resolve(".gitignore"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("未找到仓库根目录 .gitignore");
    }
}
```

- [ ] **Step 2: Run the contract test and verify RED**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-base '-Dtest=FileLocalStorageConfigContractTest' test
```

Expected:

- Command fails.
- One assertion fails because `dev/hunyuan-base.yaml` still contains `${localPath:/home}/hunyuan/upload/`.
- One assertion fails because `.gitignore` does not contain `/hunyuan-backend/runtime/upload/`.

- [ ] **Step 3: Update dev upload path**

Modify `hunyuan-backend/hunyuan-base/src/main/resources/dev/hunyuan-base.yaml`.

Replace:

```yaml
      upload-path: ${localPath:/home}/hunyuan/upload/   #文件上传目录
```

with:

```yaml
      upload-path: ${localPath:./runtime}/upload/   # 文件上传目录，dev 默认写入 hunyuan-backend/runtime/upload/
```

- [ ] **Step 4: Add Git ignore rule**

Modify root `.gitignore`.

Add this block near the logs/runtime section:

```gitignore
# Local runtime uploads
/hunyuan-backend/runtime/upload/
```

- [ ] **Step 5: Run the contract test and verify GREEN**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-base '-Dtest=FileLocalStorageConfigContractTest' test
```

Expected:

- `BUILD SUCCESS`
- `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 6: Verify Git ignore behavior with a representative path**

Run:

```powershell
git check-ignore -v hunyuan-backend/runtime/upload/example.tmp
```

Expected:

- Output points to `.gitignore` and the `/hunyuan-backend/runtime/upload/` rule.

- [ ] **Step 7: Commit Task 2**

Run:

```powershell
git add .gitignore hunyuan-backend/hunyuan-base/src/main/resources/dev/hunyuan-base.yaml hunyuan-backend/hunyuan-base/src/test/java/com/hunyuan/sa/base/module/support/file/FileLocalStorageConfigContractTest.java
git diff --cached --check
git commit -m "chore: 使用项目内本地文件上传目录"
```

Expected:

- Staged files contain only `.gitignore`, dev yaml, and `FileLocalStorageConfigContractTest.java`.
- Commit succeeds.

---

### Task 3: Final Backend Foundation File Gate and Acceptance Note

**Files:**
- Create: `docs/superpowers/specs/2026-07-09-file-local-storage-consistency-acceptance.md`

**Interfaces:**
- Consumes:
  - Task 1 compensation behavior.
  - Task 2 local runtime upload directory and Git ignore rule.
- Produces:
  - A UTF-8 acceptance note with verification evidence and known boundaries.

- [ ] **Step 1: Run focused file gates**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-base '-Dtest=FileServiceTest,FileLocalStorageConfigContractTest' test
```

Expected:

- `BUILD SUCCESS`
- `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 2: Run hunyuan-base module gate**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-base test
```

Expected:

- `BUILD SUCCESS`
- Existing and new `hunyuan-base` tests pass.

- [ ] **Step 3: Verify upload directory remains untracked**

Run:

```powershell
git check-ignore -v hunyuan-backend/runtime/upload/example.tmp
git status --short
```

Expected:

- `git check-ignore` prints the `.gitignore` rule for `/hunyuan-backend/runtime/upload/`.
- `git status --short` does not show `hunyuan-backend/runtime/upload/example.tmp`.

- [ ] **Step 4: Write acceptance note**

Create `docs/superpowers/specs/2026-07-09-file-local-storage-consistency-acceptance.md` with this content, replacing test counts only if Maven reports different fresh counts:

```markdown
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
| hunyuan-base 模块门禁 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-base test` | PASS |
| Git 忽略验证 | `git check-ignore -v hunyuan-backend/runtime/upload/example.tmp` | PASS；命中 `/hunyuan-backend/runtime/upload/` |

## 边界说明

- 本轮没有改对象存储 S3 兼容逻辑。
- 本轮没有新增文件引用关系表。
- 本轮没有新增文件管理前端页面。
- 本轮不扫描历史孤儿文件，只处理本次上传后落库失败的即时补偿。
```

- [ ] **Step 5: Scan acceptance note and whitespace**

Run:

```powershell
rg -n "TBD|TODO|待定|未执行|\\[ \\]" docs/superpowers/specs/2026-07-09-file-local-storage-consistency-acceptance.md
git diff --check -- docs/superpowers/specs/2026-07-09-file-local-storage-consistency-acceptance.md
```

Expected:

- `rg` prints no output.
- `git diff --check` exits 0.

- [ ] **Step 6: Commit Task 3**

Run:

```powershell
git add docs/superpowers/specs/2026-07-09-file-local-storage-consistency-acceptance.md
git diff --cached --check
git commit -m "docs: 增加文件本地存储一致性验收记录"
```

Expected:

- Commit succeeds.

---

## Final Verification

After all tasks complete, run:

```powershell
git status --short
git log --oneline -6
```

Expected:

- `git status --short` is clean.
- Recent commits include:
  - `docs: 增加文件本地存储一致性验收记录`
  - `chore: 使用项目内本地文件上传目录`
  - `feat: 增加文件上传落库失败补偿`
  - `docs: 增加文件本地存储一致性设计`

## Self-Review

- Spec coverage: Task 1 covers upload DB-failure compensation. Task 2 covers dev local runtime upload directory and Git ignore boundary. Task 3 covers final verification and acceptance record.
- Placeholder scan: no task uses TODO/TBD or vague “add tests” instructions; each task includes concrete code, commands, and expected results.
- Type consistency: plan uses existing `FileService`, `IFileStorageService`, `FileDao`, `FileEntity`, `FileUploadVO`, `FileFolderTypeEnum`, and `SecurityFileService` names from current source.
