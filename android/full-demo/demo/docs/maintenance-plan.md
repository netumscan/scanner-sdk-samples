# Android Demo 长期维护计划

## 定位

`wrappers/android-kotlin/demo` 是长期维护的 SDK demo app，用于：

- 展示 Android 侧 BLE GATT 主通道和 SPP Classic 兼容通道的接入闭环。
- 验证 Kotlin wrapper 的 initialize、discovery、connect、session、scan event、command、module command、localization 和日志能力。
- 支撑内部调试、客户演示、现场诊断和问题复现。

它不是业务 App 模板，也不是协议实验场。协议编解码、设备能力、会话状态机和命令语义必须来自 `core`、`api-c`、Android adapter 和 Kotlin wrapper，demo 只做宿主 App 编排和展示。

## 当前结论

当前 Android demo 已经具备长期维护的起点：

- Compose 页面已覆盖 discovery、console 和 app logs。
- `DemoViewModel` / `CommandConsoleViewModel` 承担页面状态聚合。
- discovery 设备缓存、日志、格式化、多语言、能力摘要和命令展示已有独立支持文件。
- P1 已抽出 `DemoDiagnosticsStore` 和 `SessionCommandRunner`，诊断摘要和命令执行 guard 不再完全散落在 ViewModel 中。
- P2/P3 已抽出 `DiscoveryCoordinator`、fake backend/session、`ModuleCommandRunner`、compatibility record 和 scenario presets。
- 单测覆盖 discovery guard、session coordinator、formatter、catalog、多语言资源、权限规则、诊断摘要和 runner busy。
- UI instrumentation test 覆盖日志、控制台和本地化截图关键路径。

但主 ViewModel 和控制台能力已经很大。后续不能继续把 discovery、session、command、module、diagnostics、export 全部塞进同一层对象。

## 维护原则

- UI 不直接调用协议细节，只调用 ViewModel、coordinator、runner、store 或 formatter。
- demo 不重复实现协议编解码，不硬编码第二套设备能力判断。
- 新增命令必须优先使用 Kotlin wrapper 公开 API；工程入口必须明确标记风险。
- 所有命令按钮必须受 selected model、capability summary、operation support 或 module family 控制。
- 当前 UI 状态不要保存已翻译字符串；保存 semantic source、provider 或足够重新格式化的原始值。
- 日志可以保留事件发生时的语言，因为日志是历史记录。
- 新增文案必须同时补中文和英文，并通过本地化检查。
- SPP Classic 只做兼容降级，不得把它升级成 Android 主协议路线。

## 分阶段计划

| 阶段 | 内容 | 验收标准 |
| --- | --- | --- |
| P0 | 补齐长期维护文档 | demo README 有入口；新增功能有规则；演示前有 checklist |
| P0 | 固化 demo 自动化入口 | `:demo:assembleDebug`、`:demo:testDebugUnitTest` 可作为最小 CI；失败原因可定位 |
| P1 | 增加诊断摘要 | 已完成：展示 SDK/demo version、Android version、权限、蓝牙、位置、transport、selected/resolved model、session state、最近错误 |
| P1 | 抽出 `DemoDiagnosticsStore` | 已完成：权限、平台状态、SDK diagnostics、最近 failure 不继续散落在页面逻辑里 |
| P1 | 抽出 `SessionCommandRunner` | 已完成：busy、ready guard、operation/master support、成功/失败反馈和日志记录统一 |
| P2 | 增加 fake discovery / fake session 模式 | 已完成：无硬件也能跑 discovery、connect、console、logs 主流程；debug/test 入口标记 fake mode |
| P2 | 抽出 `DiscoveryCoordinator` | 已完成：SDK 初始化、BLE/SPP discovery、connect、failure/recovery 和 fake backend 入口独立 |
| P2 | 抽出 `ModuleCommandRunner` | 已完成：module ready guard、parameter command、NTC06H timeout、反馈和日志独立 |
| P2 | 增加设备/系统兼容矩阵文档 | 已完成：App Logs 可导出 compatibility record；矩阵模板要求持续沉淀 Android/ROM/BLE/SPP 结果 |
| P3 | 增加演示场景 presets | 已完成：快速扫码、信息读取、电量读取、常用主控命令、模组参数、Data Rule Builder 有稳定演示路径 |
| P3 | 增加 APK 发布说明 | 已完成：本地包脚本、release notes 模板和日志脱敏规则已补；真实签名另走发布流程 |

## 推荐目标结构

```text
demo/src/main/java/com/netumscan/scannersdk/demo/
  discovery/
    DiscoveryCoordinator
    DiscoveryUiState
    DemoDiscoveryDeviceStore
  console/
    CommandConsoleViewModel
    SessionCommandRunner
    ModuleCommandRunner
  diagnostics/
    DemoDiagnosticsStore
    DemoEnvironmentSnapshot
  logs/
    AppLogStore
    LogExport
  localization/
    DemoStrings
    DemoLocaleController
  support/
    formatter
    catalog
    recovery
```

当前不要求立刻迁移目录。短期可以保留现有文件布局，但新增复杂逻辑必须优先拆独立类型，而不是继续扩张 `DemoViewModel` / `CommandConsoleViewModel`。

## 重构触发条件

满足任一条件时，先拆职责再继续堆功能：

- 单个 ViewModel 超过 700 行且继续新增业务逻辑。
- 新功能需要同时修改 discovery、session、command、module 和 logs 三个以上区域。
- 同一种错误反馈或成功反馈需要在三个以上调用点复制。
- 新增状态需要同时驱动 UI、日志、语言刷新和恢复动作。
- 新增功能无法用单测覆盖，因为状态耦合过重。

## 每次改动的最小验收

- 文档改动：运行 `python3 tools/dev/check_docs.py`。
- 本地化改动：运行 `python3 tools/dev/check_localization.py`、`python3 tools/dev/check_demo_sdk_localization_coverage.py` 和 `python3 tools/dev/check_demo_sdk_localization_quality.py`。
- Kotlin 行为改动：运行 `cd wrappers/android-kotlin && ./gradlew :demo:testDebugUnitTest`。
- SDK API 相关改动：追加 `./gradlew :sdk:testReleaseUnitTest`。
- UI 行为改动：优先补 `src/androidTest`；没有设备或 emulator 时，在变更说明里明确未跑原因。
- discovery / connect / command 行为改动：按 [smoke-checklist.md](smoke-checklist.md) 做真机验证。
