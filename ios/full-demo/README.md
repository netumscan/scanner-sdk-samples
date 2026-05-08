# ScannerSDK iOS Demo

长期维护的 iOS demo app，用于展示、验证和演示 Scanner SDK 在 iPhone 宿主工程里的 BLE GATT 接入闭环。

这套代码的目标不是继续证明命令行 demo 能跑，而是给 iPhone 宿主工程一个可持续维护的 demo 起点：

- 初始化 `ScannerSDK`
- BLE discovery
- 设备列表
- 连接 / 断开
- session state
- 扫码事件日志
- 设备信息 / 电量 / `%CHARSET#`

## 长期维护文档

- [docs/maintenance-plan.md](docs/maintenance-plan.md)：长期维护计划、拆分路线和验收标准。
- [docs/development-rules.md](docs/development-rules.md)：新增功能、命令、多语言、日志和测试规则。
- [docs/smoke-checklist.md](docs/smoke-checklist.md)：演示前和真机回归的手工 smoke checklist。
- [../../../docs/guides/demo/ios-demo.md](../../../docs/guides/demo/ios-demo.md)：仓库级 iOS demo 启动说明和维护边界。
- [../../../docs/guides/demo/mobile-demo-status-matrix.md](../../../docs/guides/demo/mobile-demo-status-matrix.md)：Android/iOS demo 能力对齐状态。
- [../../../docs/guides/demo/mobile-demo-compatibility-matrix.md](../../../docs/guides/demo/mobile-demo-compatibility-matrix.md)：移动端真机兼容矩阵模板。

长期维护口径：

- demo 只做宿主 App 编排和展示，不重复实现协议编解码。
- 协议、会话、设备能力和命令语义必须来自 `ScannerSDK` / `core`。
- 当前 UI 状态使用 semantic source 或 provider 支持语言刷新；历史日志保留事件发生时语言。
- P1 已抽出 `DemoDiagnosticsStore` 和 `SessionCommandRunner`。
- P2/P3 已抽出 `DiscoveryCoordinator`、`ModuleCommandRunner`、fake backend/session、compatibility record 和 scenario presets。
- 后续复杂功能优先继续拆 `DemoFeedbackState`、`DemoLogStore` 或独立 formatter，不要继续无限扩张 `AppViewModel`。

## 当前边界

这套 demo 目前是 **长期 demo app 的维护起点**，不是已经完成真机签名交付的最终工程，也不是业务 App 架构模板。

公开 samples 仓库通过 SwiftPM 消费 `https://github.com/netumscan/scanner-sdk-ios.git`，不依赖私有开发仓库里的本地 XCFramework 构建产物。

## 生成 Xcode 工程

这里不手写 `.xcodeproj`，而是用 `XcodeGen`：

```bash
cd ios/full-demo
xcodegen generate
```

生成后打开：

```bash
open ScannerSDKiOSDemo.xcodeproj
```

## iOS 工具链自检

如果 `xcodebuild -showdestinations` 只显示：

- `Any iOS Device`
- 并提示 `iOS XX.X is not installed`

那基本不是仓库代码问题，而是本机 Xcode iOS 平台组件 / runtime 状态有问题。

直接跑：

```bash
./wrappers/apple-swift/scripts/check-ios-toolchain.sh
```

如果脚本输出还是只剩 `Any iOS Device`，就去：

- `Xcode > Settings > Components`
- 或执行 `xcodebuild -downloadPlatform iOS`

把对应 iOS platform/runtime 收齐。

## 当前页面能力

- `Scanner Discovery`
- `Scanner Control Console`
- `App Logs`
- 跟随系统 / 中文 / English 三态语言选择
- `Init SDK`
- `Selected Model`
- `Start Discovery`
- 蓝牙 / 扫描不可用时的可操作系统设置提示
- 设备列表
- `Connect + Open Console`
- `Disconnect`
- `Refresh Info`
- `Get Battery`
- `Send %CHARSET#`
- Android 对齐的能力摘要 / SDK 诊断型号 / 模组能力摘要
- 本地 `ScanTextCharset` 切换
- 命令分组
- 基础 UI 自动化标识与 `ScannerSDKiOSDemoUITests`
- `Module` 参数目录 / 自定义参数读写
- NTC06H 设置码目录 / 模板设置码编辑
- `11.5 Data Rule Builder`
- discovery / console 诊断摘要
- scenario presets：快速扫码、信息读取、电量读取、常用主控命令、模组参数、Data Rule Builder
- `SessionCommandRunner` 统一命令 busy、ready、support guard、反馈和日志
- `ModuleCommandRunner` 统一模组命令 ready、timeout、反馈和日志
- debug/test fake discovery 与 fake session
- 日志筛选 / 导出 / compatibility record / 清空

## 当前页面结构

当前 iOS demo 已对齐 Android demo 的页面拆分思路：

- `Scanner Discovery`
  - Android 对齐的 discovery 概览卡片（状态 / 当前设备 / 选择型号 / 发现设备数）
  - 选择型号
  - 协议模式由选择型号自动决定
  - 显式 `Init SDK` 入口
  - Android 对齐的设备列表卡片（型号 / 传输 / RSSI）
  - 控制台入口
- `Scanner Control Console`
  - 设备状态摘要
  - SDK 解析型号
  - 能力摘要 / 模组能力摘要
  - 模组命令可执行状态
  - Android 风格 `Operations` 主区与 scope chips
  - Android 对齐的可折叠主控快捷操作 / 主控命令区块
  - Android 对齐的 `Module` 操作域
  - NTC06H / NT212X / NT280H / SE4750 常用模组动作
  - Android 对齐的推荐先测项
  - Android 对齐的 NT212X / NT280H / SE4750 参数 catalog、read/write、默认值、枚举值、快捷值和 custom hex 编辑
  - Android 对齐的 NTC06H 设置 catalog、发送设置码、发送并保存、模板设置码和自定义设置码编辑
  - Android 对齐的命令分组卡片、高风险按钮样式与危险确认文案
  - 主控域内的 `Parsing & Advanced` 子区块
  - 主控域内的 `Data Rule Builder` 输入卡片与校验反馈
- `App Logs`
  - 全局日志聚合
  - SDK 调试事件与能力降级提示
  - 来源筛选
  - 等级筛选
  - 搜索
  - `Export Filtered` / `Export All`
  - `Export Compatibility Record`
  - `SDK Diagnostics` 摘要

公开 samples 仓库不导出 IPA，不处理真实签名、TestFlight 或客户分发账号。
  - 自动跟随最新日志
  - `Top` / `Latest` 快捷跳转

## 后续还要收的

- 正式 iOS 构建与签名链
- iPhone 真机权限与宿主集成验证
- 按长期维护计划逐步拆出 feedback、log、discovery、command、module command 子对象
- 设备能力矩阵、错误诊断页、演示场景 presets、发布态完整版本和环境信息面板
