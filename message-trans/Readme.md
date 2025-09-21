# 短信转发器 (MessageTrans)

一个用于自动拦截短信并转发到邮箱的Android应用程序。

## 项目概述

### 核心功能
1. **短信拦截转发** - 拦截指定SIM卡的短信并自动转发到配置的邮箱
2. **后台长时间运行** - 前台服务确保应用持续在后台工作
3. **完整日志记录** - 记录所有短信转发操作和状态
4. **多SIM卡支持** - 用户可选择拦截哪张SIM卡的短信
5. **双卡兼容** - 支持eSIM和物理SIM卡
6. **多邮箱配置** - 支持配置多个邮箱地址，可同时向多个邮箱发送
7. **权限管理** - 智能权限引导，支持电池优化和自启动设置

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

### 数据库设计
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

// 邮箱配置表
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
    val useSSL: Boolean        // 是否使用SSL
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
│   │   │   │   │   │   └── SimConfig.kt
│   │   │   │   │   ├── dao/                   # 数据访问对象
│   │   │   │   │   │   ├── SmsLogDao.kt
│   │   │   │   │   │   ├── EmailConfigDao.kt
│   │   │   │   │   │   └── SimConfigDao.kt
│   │   │   │   │   └── AppDatabase.kt         # 数据库配置
│   │   │   │   ├── repository/                # 仓储实现
│   │   │   │   │   ├── SmsRepositoryImpl.kt
│   │   │   │   │   ├── EmailRepositoryImpl.kt
│   │   │   │   │   └── SimRepositoryImpl.kt
│   │   │   │   └── preferences/               # 偏好设置
│   │   │   ├── domain/                        # 领域层
│   │   │   │   └── repository/                # 仓储接口
│   │   │   │       ├── SmsRepository.kt
│   │   │   │       ├── EmailRepository.kt
│   │   │   │       └── SimRepository.kt
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
│   │   │   │   │   │   └── adapter/
│   │   │   │   │   │       └── SmsLogAdapter.kt
│   │   │   │   │   └── permission/
│   │   │   │   │       └── PermissionGuideActivity.kt
│   │   │   │   └── viewmodel/                 # 视图模型
│   │   │   │       ├── HomeViewModel.kt
│   │   │   │       ├── SettingsViewModel.kt
│   │   │   │       └── LogsViewModel.kt
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
│   │   │   │   └── SmsUtils.kt               # 短信工具
│   │   │   └── MessageTransApplication.kt     # 应用程序入口
│   │   ├── res/                              # 资源文件
│   │   │   ├── layout/                       # 布局文件
│   │   │   │   ├── activity_main.xml
│   │   │   │   ├── activity_permission_guide.xml
│   │   │   │   ├── fragment_home.xml
│   │   │   │   ├── fragment_settings.xml
│   │   │   │   ├── fragment_logs.xml
│   │   │   │   ├── item_email_config.xml
│   │   │   │   ├── item_sim_config.xml
│   │   │   │   └── item_sms_log.xml
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
SMS接收 → SmsInterceptorReceiver → 检查SIM卡配置 → 记录日志 → 触发邮件发送
```

### 2. 邮件发送流程
```
短信拦截 → WorkManager入队 → EmailSendingWorker → 获取邮箱配置 → 发送邮件 → 更新状态
```

### 3. 权限管理流程
```
应用启动 → 检查权限状态 → 权限引导界面 → 分步骤授权 → 完成设置
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
- 敏感数据不参与备份
- ProGuard代码混淆保护

### 隐私保护
- 短信内容仅用于转发，不上传第三方
- 本地数据库存储，用户完全控制
- 支持清理历史日志

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

### 后续优化方向
1. 增加邮件模板自定义功能
2. 支持短信关键词过滤
3. 增加统计图表展示
4. 支持云端备份配置
5. 增加多语言支持

---

**开发完成时间**: 2025年1月
**开发者**: Claude Code Assistant
**版本**: v1.0
**许可证**: MIT License