# iOS Demo 长期维护计划

## 定位

`ScannerSDKiOSDemo` 是长期维护的 SDK demo app，用于：

- 展示 iOS 侧 BLE GATT 接入闭环。
- 验证 Swift wrapper 的 discovery、connect、session、scan event、command、module command 和日志能力。
- 支撑内部调试、客户演示和问题复现。

它不是业务 App 模板，也不是协议实验场。协议编解码、设备能力、会话状态机和命令语义必须来自 `ScannerSDK` / `core`，demo 只做宿主 App 编排和展示。

## 当前结论

当前架构能满足现阶段长期维护的起点：

- SwiftUI View 负责展示和触发动作。
- `AppViewModel` 作为页面级状态聚合入口。
- 目录、格式化、本地化、日志、缓存读取已拆到支持文件。
- P1 已抽出 `DemoDiagnosticsStore` 和 `SessionCommandRunner`，诊断摘要和命令执行 guard 不再完全散落在 `AppViewModel` 中。
- P2/P3 已抽出 `DiscoveryCoordinator`、fake backend/session、`ModuleCommandRunner`、compatibility record 和 scenario presets。
- 多语言当前态通过 semantic source 或 provider 延迟刷新。
- 单测已覆盖多语言、能力摘要、日志、设备状态、命令目录、诊断摘要和 runner busy 的关键路径。

但 `AppViewModel` 已经偏胖。后续继续加大功能时，不能再把 discovery、session、command、module、feedback、log 全部继续塞进主对象。

## 维护原则

- UI 不直接调用底层协议细节，只调用 `AppViewModel` 或后续拆出的 coordinator / runner / store。
- demo 不重复实现协议编解码，不硬编码设备能力判断。
- 新增命令必须先确认 SDK wrapper 已有稳定 API 或明确是工程模式入口。
- 所有设备能力显示必须受 selected model、capability summary 和 operation support 控制。
- 当前 UI 状态不要保存已翻译字符串；保存 semantic source 或 provider。
- 日志可以保留事件发生时的语言，因为日志是历史记录，不是当前 UI 状态。
- 新增文案必须同时补中文和英文，并通过本地化检查。

## 分阶段计划

| 阶段 | 内容 | 验收标准 |
| --- | --- | --- |
| P0 | 补齐长期维护文档和手工 smoke checklist | README 有入口；新增功能有规则可循；演示前有 checklist |
| P1 | 抽出 `DemoFeedbackState` | `lastActionResult`、`errorText`、provider、语言刷新逻辑离开 `AppViewModel` 主体；现有多语言测试通过 |
| P1 | 抽出 `DemoLogStore` | `events`、筛选、诊断摘要、导出逻辑离开 `AppViewModel` 主体；日志测试通过 |
| P1 | 抽出 `SessionCommandRunner` | 已完成：`requireActiveSession`、`executeAction`、operation support、master command support 独立；命令错误和成功反馈测试通过 |
| P2 | 抽出 `DiscoveryCoordinator` | 已完成：SDK 初始化、discovery、connect、failure/recovery 和 fake backend 入口独立；fake discovery/session 测试通过 |
| P2 | 抽出 `ModuleCommandRunner` | 已完成：module ready guard、parameter command、NTC06H timeout、反馈和日志独立；模块命令测试通过 |
| P2 | 增加设备能力矩阵记录 | 已完成：App Logs 可导出 compatibility record；矩阵模板要求持续沉淀 iOS/BLE 结果 |
| P2 | 增加错误诊断摘要 | 已完成：discovery / console / logs 展示最近 failure、平台状态和 session 事实；独立诊断页暂不拆 |
| P3 | 增加演示场景 presets | 已完成：快速扫码、信息读取、电量读取、常用主控命令、模组参数、Data Rule Builder 有可复用演示路径 |
| P1 | 增加诊断摘要 | 已完成：展示 App build、iOS version、蓝牙权限、transport、selected/resolved model、session state 和最近 failure |
| P3 | 增加本地发布资料 | 已完成：本地包脚本、release notes 模板和日志脱敏规则已补；真实签名/TestFlight 另走发布流程 |

## 推荐目标结构

```text
ScannerSDKiOSDemo/
  Views/
    Discovery
    Console
    Logs
    Diagnostics
  ViewModel/
    AppViewModel
    DemoFeedbackState
    DemoLogStore
    DiscoveryCoordinator
    SessionCommandRunner
    ModuleCommandRunner
  Support/
    Catalog
    Formatter
    Localization
    RecoveryAction
```

当前不要求立刻迁移成这个目录结构。短期可以继续使用现有文件布局，但新增复杂逻辑时必须优先放入独立类型，而不是继续扩张 `AppViewModel`。

## 重构触发条件

满足任一条件时，必须先拆职责再继续堆功能：

- 单个 `AppViewModel*Support.swift` 文件超过 350 行且继续新增逻辑。
- 新功能需要同时修改 discovery、session、command 和日志三个以上区域。
- 同一种错误反馈或成功反馈需要在三个以上调用点复制。
- 新增状态需要同时驱动 UI、日志、语言刷新和恢复动作。
- 新增功能无法用现有单测覆盖，因为状态耦合过重。

## 每次改动的最小验收

- 文档或 UI 文案改动：运行 `python3 tools/dev/check_docs.py` 和 `python3 tools/dev/check_localization.py`。
- ViewModel 行为改动：运行 `xcodebuild ... -only-testing:ScannerSDKiOSDemoTests`。
- 多语言当前态改动：至少补一条语言切换测试。
- discovery / connect / command 行为改动：按 [smoke-checklist.md](smoke-checklist.md) 做手工验证。
- 不能跑的验证要在提交说明或 PR 描述里明确写原因。
