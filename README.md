# Hunyuan Pro

混元全栈管理项目，包含前端与后端两个子项目：

| 目录 | 说明 | 技术栈 |
|------|------|--------|
| [hunyuan-design](./hunyuan-design) | 前端管理界面 | Vue 3 + Element Plus + Vite |
| [hunyuan-backend](./hunyuan-backend) | 后端 API 服务 | Java 17 + Spring Boot 3 |

## 快速开始

### 前端

```bash
cd hunyuan-design
pnpm install
pnpm dev
```

### 后端

```bash
cd hunyuan-backend
mvn clean install -DskipTests
cd hunyuan-admin
mvn spring-boot:run
```
