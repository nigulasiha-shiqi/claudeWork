# 短信转发器 (MessageTrans)

一个用于自动拦截短信并转发到邮箱的Android应用程序。

## 项目概述

### 核心功能
1. **短信拦截转发** - 拦截指定SIM卡的短信并自动转发到配置的邮箱
2. **后台长时间运行** - 前台服务确保应用持续在后台工作
3. **完整日志记录** - 记录所有短信转发操作和状态
4. **运行时日志系统** - 实时记录应用运行状态，支持分级筛选便于故障排查
5. **邮件配置缓存** - 邮件配置持久化存储，重装应用后自动恢复
6. **多SIM卡支持** - 用户可选择拦截哪张SIM卡的短信
7. **双卡兼容** - 支持eSIM和物理SIM卡
8. **多邮箱配置** - 支持配置多个邮箱地址，可同时向多个邮箱发送
9. **权限管理** - 智能权限引导，支持电池优化和自启动设置
10. **配置导入导出** - 支持邮件配置的导出和导入，方便备份和迁移
11. **代理服务器支持** - 支持HTTP/SOCKS代理，解决网络限制问题 *(UI暂时隐藏)*
12. **短信编辑功能** - 支持编辑短信内容，修正错误后重新转发
13. **智能重发机制** - 支持失败短信的重新发送，自动重置转发状态

### 技术栈
- **平台**: 原生Android开发
- **语言**: Kotlin
- **最低版本**: Android 11 (API 30+)
- **架构**: MVVM + Repository模式
- **数据库**: Room Database
- **异步处理**: Kotlin Coroutines + WorkManager
- **UI框架**: Material Design 3 + ViewBinding
- **邮件服务**: JavaMail API

### 主要依赖
```kotlin
// Android核心组件
androidx.core:core-ktx:1.12.0
androidx.appcompat:appcompat:1.6.1
com.google.android.material:material:1.11.0

// 架构组件
androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0
androidx.navigation:navigation-fragment-ktx:2.7.6

// 数据库
androidx.room:room-runtime:2.6.1
androidx.room:room-ktx:2.6.1

// 后台任务
androidx.work:work-runtime-ktx:2.9.0

// 邮件发送
com.sun.mail:android-mail:1.6.7
com.sun.mail:android-activation:1.6.7

// JSON处理
com.google.code.gson:gson:2.10.1
```

## 项目架构

### 整体架构设计
```
├── Presentation Layer (UI)
│   ├── MainActivity (主界面)
│   ├── HomeFragment (首页 - 服务状态)
│   ├── SettingsFragment (设置 - 邮箱和SIM卡配置)
│   ├── LogsFragment (日志查看)
│   ├── PermissionGuideActivity (权限引导)
│   └── ViewModels (MVVM架构)
├── Domain Layer (业务逻辑)
│   ├── Repository Interfaces
│   └── Use Cases
├── Data Layer
│   ├── Room Database (本地数据存储)
│   ├── Repository Implementations
│   └── Entities (数据实体)
└── Service Layer
    ├── SmsInterceptorReceiver (短信拦截)
    ├── SmsMonitorService (前台服务)
    ├── EmailService (邮件发送)
    └── BootReceiver (开机自启)
```

### 数据库设计 (当前版本: v3)
**数据库升级历史**: v1.0(版本1) → v1.1(版本2) → v1.2(版本3-代理支持)
```kotlin
// 短信记录表
@Entity(tableName = "sms_logs")
data class SmsLog(
    @PrimaryKey val id: String,
    val phoneNumber: String,     // 发送方号码
    val content: String,         // 短信内容
    val timestamp: Long,         // 时间戳
    val simSlot: Int,           // SIM卡槽 (0=SIM1, 1=SIM2)
    val simType: String,        // SIM卡类型 (physical/esim)
    val isForwarded: Boolean,   // 是否已转发
    val emailsSent: String,     // 已发送的邮箱列表(JSON)
    val errorMessage: String?   // 错误信息
)

// 邮箱配置表 (v1.2增强 - 支持代理配置)
@Entity(tableName = "email_configs")
data class EmailConfig(
    @PrimaryKey val id: String,
    val displayName: String,    // 显示名称
    val emailAddress: String,   // 邮箱地址
    val smtpServer: String,     // SMTP服务器
    val smtpPort: Int,          // SMTP端口
    val username: String,       // 用户名
    val password: String,       // 密码(加密存储)
    val isEnabled: Boolean,     // 是否启用
    val useSSL: Boolean,        // 是否使用SSL
    val useProxy: Boolean = false,      // 是否使用代理
    val proxyType: String = "HTTP",     // 代理类型: HTTP, SOCKS
    val proxyHost: String = "",         // 代理服务器地址
    val proxyPort: Int = 8080,          // 代理端口
    val proxyUsername: String = "",     // 代理用户名
    val proxyPassword: String = ""      // 代理密码
)

// SIM卡配置表
@Entity(tableName = "sim_configs")
data class SimConfig(
    @PrimaryKey val slotIndex: Int,
    val displayName: String,    // 显示名称
    val carrierName: String,    // 运营商名称
    val phoneNumber: String?,   // 手机号码
    val isEnabled: Boolean,     // 是否启用拦截
    val simType: String        // SIM卡类型
)

// 运行日志表
@Entity(tableName = "runtime_logs")
data class RuntimeLog(
    @PrimaryKey val id: String,
    val level: String,          // 日志级别 (DEBUG, INFO, WARN, ERROR)
    val tag: String,            // 日志标签
    val message: String,        // 日志消息
    val details: String?,       // 详细信息
    val timestamp: Long         // 时间戳
)
```

## 文件结构

### 主要目录结构
```
message-trans/
├── app/
│   ├── src/main/
│   │   ├── java/com/messagetrans/
│   │   │   ├── data/                          # 数据层
│   │   │   │   ├── database/
│   │   │   │   │   ├── entities/              # 数据实体
│   │   │   │   │   │   ├── SmsLog.kt
│   │   │   │   │   │   ├── EmailConfig.kt
│   │   │   │   │   │   ├── SimConfig.kt
│   │   │   │   │   │   └── RuntimeLog.kt
│   │   │   │   │   ├── dao/                   # 数据访问对象
│   │   │   │   │   │   ├── SmsLogDao.kt
│   │   │   │   │   │   ├── EmailConfigDao.kt
│   │   │   │   │   │   ├── SimConfigDao.kt
│   │   │   │   │   │   └── RuntimeLogDao.kt
│   │   │   │   │   └── AppDatabase.kt         # 数据库配置
│   │   │   │   ├── repository/                # 仓储实现
│   │   │   │   │   ├── SmsRepositoryImpl.kt
│   │   │   │   │   ├── EmailRepositoryImpl.kt
│   │   │   │   │   ├── SimRepositoryImpl.kt
│   │   │   │   │   └── RuntimeLogRepositoryImpl.kt
│   │   │   │   └── preferences/               # 偏好设置
│   │   │   ├── domain/                        # 领域层
│   │   │   │   └── repository/                # 仓储接口
│   │   │   │       ├── SmsRepository.kt
│   │   │   │       ├── EmailRepository.kt
│   │   │   │       ├── SimRepository.kt
│   │   │   │       └── RuntimeLogRepository.kt
│   │   │   ├── presentation/                  # 表示层
│   │   │   │   ├── ui/
│   │   │   │   │   ├── main/
│   │   │   │   │   │   ├── MainActivity.kt
│   │   │   │   │   │   └── HomeFragment.kt
│   │   │   │   │   ├── settings/
│   │   │   │   │   │   ├── SettingsFragment.kt
│   │   │   │   │   │   └── adapter/
│   │   │   │   │   │       ├── EmailConfigAdapter.kt
│   │   │   │   │   │       └── SimConfigAdapter.kt
│   │   │   │   │   ├── logs/
│   │   │   │   │   │   ├── LogsFragment.kt
│   │   │   │   │   │   ├── LogsPagerAdapter.kt
│   │   │   │   │   │   ├── SmsLogsFragment.kt
│   │   │   │   │   │   ├── RuntimeLogsFragment.kt
│   │   │   │   │   │   └── adapter/
│   │   │   │   │   │       ├── SmsLogAdapter.kt
│   │   │   │   │   │       └── RuntimeLogAdapter.kt
│   │   │   │   │   └── permission/
│   │   │   │   │       └── PermissionGuideActivity.kt
│   │   │   │   └── viewmodel/                 # 视图模型
│   │   │   │       ├── HomeViewModel.kt
│   │   │   │       ├── SettingsViewModel.kt
│   │   │   │       ├── LogsViewModel.kt
│   │   │   │       └── RuntimeLogsViewModel.kt
│   │   │   ├── service/                       # 服务层
│   │   │   │   ├── sms/
│   │   │   │   │   ├── SmsInterceptorReceiver.kt  # 短信拦截器
│   │   │   │   │   ├── SmsMonitorService.kt       # 监控服务
│   │   │   │   │   └── EmailSendingWorker.kt      # 邮件发送工作器
│   │   │   │   ├── email/
│   │   │   │   │   └── EmailService.kt            # 邮件服务
│   │   │   │   └── BootReceiver.kt                # 开机启动接收器
│   │   │   ├── utils/                         # 工具类
│   │   │   │   ├── PermissionManager.kt       # 权限管理
│   │   │   │   ├── SmsUtils.kt               # 短信工具
│   │   │   │   ├── RuntimeLogger.kt          # 运行时日志工具
│   │   │   │   ├── EmailConfigCache.kt       # 邮件配置缓存
│   │   │   │   └── SimCardManager.kt         # SIM卡管理
│   │   │   └── MessageTransApplication.kt     # 应用程序入口
│   │   ├── res/                              # 资源文件
│   │   │   ├── layout/                       # 布局文件
│   │   │   │   ├── activity_main.xml
│   │   │   │   ├── activity_permission_guide.xml
│   │   │   │   ├── fragment_home.xml
│   │   │   │   ├── fragment_settings.xml
│   │   │   │   ├── fragment_logs.xml
│   │   │   │   ├── fragment_sms_logs.xml
│   │   │   │   ├── fragment_runtime_logs.xml
│   │   │   │   ├── dialog_email_config.xml    # 邮箱配置对话框(含代理设置)
│   │   │   │   ├── dialog_sms_edit.xml        # 短信编辑对话框(v1.2新增)
│   │   │   │   ├── item_email_config.xml
│   │   │   │   ├── item_sim_config.xml
│   │   │   │   ├── item_sms_log.xml
│   │   │   │   └── item_runtime_log.xml
│   │   │   ├── values/                       # 值资源
│   │   │   │   ├── strings.xml              # 字符串资源
│   │   │   │   ├── colors.xml               # 颜色资源
│   │   │   │   └── themes.xml               # 主题资源
│   │   │   ├── drawable/                     # 图标资源
│   │   │   ├── menu/                        # 菜单资源
│   │   │   ├── navigation/                  # 导航资源
│   │   │   └── xml/                         # XML配置
│   │   └── AndroidManifest.xml              # 应用清单
│   ├── build.gradle.kts                     # 应用构建配置
│   └── proguard-rules.pro                   # 混淆规则
├── gradle/                                  # Gradle配置
├── build.gradle.kts                         # 项目构建配置
├── settings.gradle.kts                      # 项目设置
├── gradle.properties                        # Gradle属性
└── README.md                               # 项目说明
```

### 核心类说明

#### 服务层核心类
- **SmsInterceptorReceiver**: 广播接收器，监听SMS_RECEIVED广播
- **SmsMonitorService**: 前台服务，注册短信接收器并维持后台运行
- **EmailService**: 邮件发送服务，支持SMTP协议和SSL/TLS加密
- **EmailSendingWorker**: WorkManager工作器，异步处理邮件发送任务

#### 权限管理核心类
- **PermissionManager**: 统一管理应用所需的各种权限
- **PermissionGuideActivity**: 分步骤引导用户授予必要权限

#### 数据管理核心类
- **AppDatabase**: Room数据库配置和实例管理
- **Repository类**: 数据仓储模式，封装数据访问逻辑

## 核心功能实现

### 1. 短信拦截流程
```
SMS接收 → SmsInterceptorReceiver → 检查SIM卡配置 → 记录日志 → 触发邮件发送 → 记录运行日志
```

### 2. 邮件发送流程
```
短信拦截 → WorkManager入队 → EmailSendingWorker → 获取邮箱配置 → 发送邮件 → 更新状态 → 同步缓存
```

### 3. 权限管理流程
```
应用启动 → 检查权限状态 → 权限引导界面 → 分步骤授权 → 完成设置
```

### 4. 日志系统流程
```
操作事件 → RuntimeLogger → 加密存储 → 分级显示 → 自动清理
```

### 5. 配置缓存流程
```
配置变更 → EmailConfigCache → AES加密 → SharedPreferences存储 → 应用重启时恢复
```

## 权限说明

### 必需权限
- `READ_SMS`: 读取短信内容
- `RECEIVE_SMS`: 接收短信广播
- `INTERNET`: 网络访问(发送邮件)
- `ACCESS_NETWORK_STATE`: 网络状态检查
- `FOREGROUND_SERVICE`: 前台服务权限
- `RECEIVE_BOOT_COMPLETED`: 开机自启权限
- `POST_NOTIFICATIONS`: 通知权限(Android 13+)
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: 电池优化白名单

### 特殊权限处理
- **电池优化**: 引导用户将应用加入白名单
- **自启动权限**: 适配主流厂商(华为、小米、OPPO、VIVO等)
- **通知权限**: Android 13+版本适配

## 安全特性

### 数据安全
- 邮箱密码加密存储
- 运行日志AES加密保护
- 配置缓存AES加密存储
- 敏感数据不参与备份
- ProGuard代码混淆保护

### 隐私保护
- 短信内容仅用于转发，不上传第三方
- 本地数据库存储，用户完全控制
- 支持清理历史日志
- 配置导出使用Base64编码保护

## 开发和维护

### 构建项目
```bash
# 克隆项目
git clone <repository-url>

# 编译debug版本
./gradlew assembleDebug

# 编译release版本
./gradlew assembleRelease
```

### 代码规范
- 使用Kotlin作为主要开发语言
- 遵循MVVM架构模式
- 使用Repository模式管理数据
- 协程处理异步操作
- Material Design设计规范

### 测试建议
- 测试多种Android版本兼容性
- 测试不同厂商的权限适配
- 测试双SIM卡场景
- 测试邮件发送稳定性
- 测试长时间后台运行

### 已知问题和限制
1. 部分厂商可能限制短信广播接收
2. 邮件发送依赖网络环境
3. 某些情况下系统可能杀死后台服务
4. 不同厂商的电池优化策略可能影响运行

### 最新功能更新

#### v1.2 (2025年9月) - 增强版 
- ✅ **代理服务器支持**: 支持HTTP/SOCKS代理，解决网络限制问题 *(UI暂时隐藏)*
- ✅ **短信编辑功能**: 支持编辑短信内容，修正错误内容后重新转发
- ✅ **运行日志筛选**: 支持按日志级别(INFO/WARN/ERROR)筛选运行日志
- ✅ **代理连接测试**: 提供代理连接测试功能，验证代理配置有效性 *(UI暂时隐藏)*
- ✅ **短信重发功能**: 支持重新发送失败的短信，自动重置转发状态
- ✅ **数据库升级**: 升级到版本3，支持代理配置字段

#### v1.1 (2025年1月) - 基础增强版
- ✅ **运行时日志系统**: 实时记录应用运行状态，分级日志便于故障排查
- ✅ **邮件配置缓存**: AES加密的持久化存储，重装应用后自动恢复配置
- ✅ **分页日志界面**: 将日志页面分为"短信日志"和"运行日志"两个标签
- ✅ **配置导入导出**: 支持邮件配置的备份和恢复，使用Base64编码
- ✅ **增强的错误处理**: 完善的异常捕获和日志记录机制

#### 核心改进类（v1.2更新）
- **EmailConfig.kt**: 增加代理配置字段(useProxy, proxyType, proxyHost等)
- **EmailConfigDialog.kt**: 支持代理配置界面和验证 *(UI暂时隐藏)*
- **SettingsFragment.kt**: 集成代理设置和测试功能 *(UI暂时隐藏)*
- **LogsViewModel.kt**: 增加日志筛选、短信编辑和重发功能
- **SmsRepositoryImpl.kt**: 新增updateSmsLog方法支持编辑
- **dialog_sms_edit.xml**: 新增短信编辑对话框布局

**UI状态说明**: 代理相关的UI界面已暂时设置为隐藏状态(`android:visibility="gone"`)，但所有后端功能代码完整保留，可随时通过修改布局文件重新启用。

### 后续优化方向
1. 增加邮件模板自定义功能
2. 支持短信关键词过滤
3. 增加统计图表展示
4. 支持云端备份配置
5. 增加多语言支持
6. 优化日志清理策略
7. 增加配置同步到云端功能

---

**开发完成时间**: 2025年9月
**开发者**: Claude Code Assistant  
**当前版本**: v1.2 (代理增强版)
**许可证**: MIT License

## 版本历史

### v1.2 (2025年9月) - 代理增强版
- 新增代理服务器支持（HTTP/SOCKS），解决网络环境限制
- 新增短信内容编辑功能，支持修正错误内容后重新转发
- 新增运行日志筛选器，支持按级别筛选日志(INFO/WARN/ERROR)
- 新增代理连接测试功能，验证代理配置有效性
- 新增短信重发功能，支持失败短信的重新发送
- 升级数据库到版本3，支持代理配置字段
- 优化日志管理系统，增强用户交互体验
- **注意**: 代理相关UI功能暂时隐藏，后端支持完整保留

### v1.1 (2025年1月) - 增强版
- 新增运行时日志系统，实时监控应用状态
- 新增邮件配置缓存，支持重装后自动恢复
- 重构日志界面为分页设计（短信日志 + 运行日志）
- 新增配置导入导出功能
- 优化错误处理和日志记录机制
- 增强数据安全性（AES加密存储）

### v1.0 (2025年1月) - 基础版
- 基础短信拦截和邮件转发功能
- 多SIM卡支持和多邮箱配置
- 权限管理和后台服务
- 基础日志记录系统
- MVVM架构实现