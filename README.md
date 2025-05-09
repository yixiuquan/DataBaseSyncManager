
# DatabaseSyncManager 项目说明

## 项目简介

**DatabaseSyncManager** 是一个基于 Flink CDC 的数据库同步管理平台，支持多数据源的实时同步、任务监控、异常告警和统计分析。项目采用前后端分离架构，后端基于 Spring Boot + Flink CDC，前端采用主流的 Vue.js 技术栈，旨在为企业提供高效、稳定、可视化的数据同步解决方案。


## 后端（Backend）

### 技术栈

- **Spring Boot 2.7.x**：主流企业级 Java 微服务框架，负责 REST API、任务调度、业务逻辑等。
- **Flink 1.16.3**：分布式流式计算引擎，负责数据同步任务的实时处理。
- **Flink CDC 2.4.2**：Flink 的 MySQL 变更数据捕获（CDC）插件，实现 MySQL 数据库的实时变更捕获与同步。
- **MySQL**：作为源库和目标库，支持多实例配置。
- **Druid**：数据库连接池，提升数据库访问性能。
- **Lombok**：简化 Java 代码开发。
- **Fastjson**：高性能 JSON 解析库。
- **Swagger2**：API 文档自动生成。
- **Springfox**：Swagger 前端集成。
- **动态数据源**：支持多数据源动态切换。

### 主要功能

- **多数据源管理**：支持多 MySQL 数据库的配置、加密存储和动态切换。
- **同步任务管理**：支持任务的创建、启动、停止、重启，支持多表同步和表映射。
- **实时数据同步**：基于 Flink CDC 实现 MySQL 数据库的全量+增量同步。
- **异常监控与告警**：同步异常自动记录，支持异常统计和告警。
- **同步统计分析**：实时统计同步条数、错误数、各表同步进度等。
- **任务日志与诊断**：详细记录任务运行日志，支持问题定位与诊断。

### 启动方式

1. **配置数据库连接**  
   修改 `application.yml`，配置管理库、源库、目标库等信息。

2. **编译并启动后端服务**  
   ```bash
   mvn clean package
   java -jar target/DatabaseSyncManager-1.0-SNAPSHOT.jar
   ```
   或用 IDE 直接运行 `com.yxq.task.DatabaseSyncManagerApplication`。

3. **访问 API 文档**  
   启动后访问 [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) 查看接口文档。

---

## 前端（Frontend）

前端代码位于 `web/` 目录，采用 Vue.js 及相关生态，提供可视化的同步任务管理与监控界面。

### 技术栈

- **Vue.js 3.x**：主流渐进式前端框架
- **Element Plus / Ant Design Vue**：UI 组件库
- **Axios**：前后端数据交互
- **ECharts**：数据可视化（同步进度、统计图表）
- **Vue Router**：前端路由管理
- **Pinia/Vuex**：状态管理

### 主要功能

- 任务管理界面：可视化创建、编辑、启动、停止同步任务
- 数据源配置：支持多数据源的增删改查
- 同步监控大屏：实时展示同步进度、异常统计、任务运行状态
- 日志与告警：查看任务日志、异常详情、历史告警
- 权限与用户管理（如有）

### 启动方式

直接在浏览器打开index.html即可。
或者
1. 进入前端项目目录
   ```bash
   cd web
   ```
2. 安装依赖
   ```bash
   npm install
   ```
3. 启动开发服务
   ```bash
   npm run serve
   ```
4. 访问 [http://localhost:3000](http://localhost:3000)（或实际端口）

---

## 目录结构

```
DatabaseSyncManager/
├── src/
│   └── main/java/com/yxq/task/       # 后端 Java 代码
├── web/                              # 前端 Vue.js 源码
│   ├── src/                          # 前端主代码
│   ├── public/                       # 静态资源
│   └── package.json                  # 前端依赖配置
├── pom.xml                           # Maven 配置
└── README.md                         # 项目说明
```

---
### 截图
![image](https://github.com/user-attachments/assets/2387ab16-4f49-4623-99e2-4524a50a129f)
![image](https://github.com/user-attachments/assets/9636807f-bdc9-493d-9d80-1c5e75220f80)
![image](https://github.com/user-attachments/assets/a6946d1b-ab16-4cff-9d21-951ff100033c)
![image](https://github.com/user-attachments/assets/535ab976-c739-46c5-8b96-632cedf5d161)

---

## 常见问题

- **initial 模式同步报错**：请参考 issues 或升级 Flink CDC 版本，修正 MySQL 表中的非法日期数据。
- **端口冲突/数据库连接失败**：检查配置文件和数据库状态。
- **前后端跨域问题**：可在 Spring Boot 配置全局 CORS 策略。

---

## 联系与支持

如有问题或建议，请提交 issue 或联系项目维护者。

---
