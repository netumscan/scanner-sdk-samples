# ScannerSDK Android Demo

长期维护的 Android demo app，用于展示、验证和演示 Scanner SDK 在 Android 宿主工程里的 BLE GATT / SPP Classic 接入闭环。

这套代码不是协议实验场，也不是业务 App 模板。它的职责是把 Android 宿主侧会遇到的权限、发现、连接、会话、命令、日志和诊断流程跑清楚；协议编解码、设备能力、会话状态机和命令语义必须来自 `:sdk`、`api-c` 和 `core`。

## 长期维护文档

- [docs/maintenance-plan.md](docs/maintenance-plan.md)：长期维护定位、分阶段计划、目标结构和验收标准。
- [docs/development-rules.md](docs/development-rules.md)：新增页面、命令、多语言、日志、诊断和测试规则。
- [docs/smoke-checklist.md](docs/smoke-checklist.md)：演示前、真机回归和客户问题复现的手工 checklist。
- [../../../docs/guides/demo/android-demo.md](../../../docs/guides/demo/android-demo.md)：仓库级 Android demo 启动说明和完整真机回归清单。
- [../../../docs/guides/demo/mobile-demo-status-matrix.md](../../../docs/guides/demo/mobile-demo-status-matrix.md)：Android/iOS demo 能力对齐状态。
- [../../../docs/guides/demo/mobile-demo-compatibility-matrix.md](../../../docs/guides/demo/mobile-demo-compatibility-matrix.md)：移动端真机兼容矩阵模板。

## 当前能力

- 初始化 `ScannerSDK`
- `BLE GATT` / `SPP Classic` 传输模式选择
- BLE discovery
- SPP Classic discovery / 手动地址连接
- 设备列表
- 连接 / 断开
- session state
- `Scanner Discovery` / `Scanner Control Console` / `App Logs`
- 跟随系统 / 中文 / English 三态语言选择
- 设备信息 / 电量读取
- 主控快捷操作和分组命令
- NT212X / NT280H / SE4750 模组参数 catalog
- NTC06H 设置码 catalog 和模板编辑
- `11.5 Data Rule Builder`
- discovery / console 诊断摘要
- scenario presets：快速扫码、信息读取、电量读取、常用主控命令、模组参数、Data Rule Builder
- 日志筛选、诊断摘要、导出和 compatibility record
- debug/test fake discovery 与 fake session
- `SessionCommandRunner` 统一命令 busy、ready、support guard、反馈和日志
- `ModuleCommandRunner` 统一模组命令 ready、timeout、反馈和日志

## 当前边界

- demo 可以做 UI 编排、Android 权限处理、平台状态提示、日志展示、导出和客户演示路径。
- demo 不允许复制协议编解码，不允许硬编码第二套设备能力判断，不允许绕过 wrapper 直接堆平台协议逻辑。
- `SPP Classic` 只作为 Android 兼容降级通道维护，不作为主协议路线。
- raw text / raw frame 只能作为工程或诊断入口，必须有明确风险提示。
- fake mode 只用于自动化、截图和无硬件演示；客户包默认禁用，启用时必须在诊断摘要和日志里可见。
- 本地包脚本只产出 debug APK / local artifact，不处理真实签名、keystore 或客户分发账号。

## 本地运行

从 `android/full-demo/` 打开 Android Studio / CodeX 项目，或直接运行：

```bash
cd android/full-demo
./gradlew :demo:assembleDebug
./gradlew :demo:testDebugUnitTest
```

真机验证 BLE / SPP 行为时，按 [docs/smoke-checklist.md](docs/smoke-checklist.md) 执行。

公开 samples 仓库只消费 `com.netumscan:scanner-sdk-android:0.1.1`，不包含私有开发仓库里的 `:sdk` 模块、`core`、`api-c` 或本地打包脚本。

## 新增功能前先确认

- 新功能是否需要先补 `core/model`、`core/protocol`、`core/session` 或 `api-c`。
- UI 是否受 selected model、capability summary、operation support 或 module family 控制。
- 是否需要新增中文和英文文案。
- 是否需要更新日志导出、诊断摘要或 smoke checklist。
- 新增命令是否已经走 `SessionCommandRunner`，并具备 ready/support guard、成功/失败反馈和日志。
- 是否能用 fake / pure Kotlin 单测覆盖；不能覆盖的真机验证要写清楚。
