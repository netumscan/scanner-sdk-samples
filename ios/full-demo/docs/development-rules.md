# iOS Demo 开发规则

## 新增页面或功能

新增功能前先判断它属于哪一类：

| 类型 | 应放位置 | 规则 |
| --- | --- | --- |
| 纯展示 View | `*View.swift` / `Console*Views.swift` | 不直接调用 SDK，不写协议判断 |
| 页面状态聚合 | `AppViewModel` 或后续拆出的 state object | 只编排，不复制协议逻辑 |
| SDK discovery / connect | `DiscoveryCoordinator` 方向 | 处理平台生命周期、failure、recovery |
| 命令执行 | `SessionCommandRunner` 方向 | 统一 busy、session ready、operation support、错误反馈 |
| 模组命令 | `ModuleCommandRunner` 方向 | 按 model / capability gating，危险操作二次确认 |
| 日志和导出 | `DemoLogStore` 方向 | 不在 View 里拼日志，不泄露敏感扫码内容 |
| 文案和格式化 | `DemoStrings` / formatter / catalog | 不在 View 中散落硬编码业务文案 |

如果一个改动跨越三类以上，先拆小，不要一次性塞进 `AppViewModel`。

## 命令和设备能力

- 新增命令必须优先使用 Swift wrapper 公开 API。
- 新增命令必须通过 `SessionCommandRunner` 或同等 runner 入口执行，不能在 `AppViewModel` 里重新散落 busy、ready、support、try/catch 分支。
- runner 必须统一处理：
  - 已有命令正在执行时的 busy 提示；
  - session 不存在或未 ready 时的可读错误；
  - operation support / master command support 不支持时的可读原因；
  - 成功、失败反馈和 App Logs 事件。
- 只有工程或诊断场景允许 raw frame / raw text command，并且 UI 必须明确标记风险。
- 命令按钮必须受 `selectedModelId`、`capabilitySummary`、`operation support` 或 module family 控制。
- 不支持的能力不要靠执行失败来告诉用户，应该在 UI 层禁用、隐藏或给出明确原因。
- 危险操作必须走确认弹窗，确认文案要本地化。

## 多语言

- demo 普通文案使用 `DemoStrings.tr(...)`。
- SDK runtime label 使用 `DemoStrings.sdk(...)`，key 必须是 `nsdk.*` 语义 key。
- 不要直接在业务代码里调用 `NSLocalizedString`；只能在本地化封装层调用。
- 当前 UI 状态不要保存已翻译字符串，例如 status、summary、last action、error text。
- 当前 UI 状态应保存：
  - semantic source，例如 `DemoStatusSummarySource`；
  - provider，例如 `() -> String`；
  - 或足够重新格式化的原始值。
- 日志是历史记录，可以保存事件发生时的文案。
- 新增语言相关逻辑必须考虑 `refreshLocalizedUi()` 后是否刷新。

## 错误反馈和恢复动作

- discovery failure、session failure、command failure 要保留原始错误信息。
- 展示层错误文案应包含可读 label，必要时附加 raw code。
- 可恢复错误必须设置 `recoveryAction`。
- 不能吞掉 SDK error code、BLE issue、platform raw code。
- 错误详情如果包含本地化 label，必须在 provider 内重新计算，不能提前翻译后捕获。
- discovery 和 console 的诊断摘要必须覆盖 App build、iOS version、transport、selected/resolved model、session state 和最近 failure。

## 日志

- 日志必须有 source 和 level。
- SDK debug / warning / error 事件要进入 App Logs。
- 日志导出默认导出当前筛选结果，另提供全部导出。
- compatibility record 导出必须包含平台、transport、model、session 和最近 failure，便于沉淀兼容矩阵。
- 扫码数据、设备 ID、平台错误等进入客户现场版本前，必须按仓库级日志脱敏说明处理。

## Fake / 测试模式

- fake discovery / fake session 只能模拟 wrapper 行为，不得成为另一套协议实现。
- fake 数据必须覆盖 BLE discovery、ready session、busy、unsupported、timeout、disconnect 和 failure 场景。
- UI test 和截图测试优先走 fake 数据，真机 checklist 覆盖真实 BLE。
- fake 模式不得默认进入客户演示包；启用时必须在诊断摘要和日志中明确标记。

## 测试

新增行为优先补单测：

- 本地化：补语言切换测试。
- command：补 session not ready、unsupported、busy、success / failure 测试。
- module：补 title provider、parameter ID、payload、NTC06H timeout 分支测试。
- discovery / session failure：补 recovery action 和 error detail 测试。
- formatter / catalog：补纯函数测试。

手工验证按 [smoke-checklist.md](smoke-checklist.md) 执行。

## 提交前检查

常用最小命令：

```bash
python3 tools/dev/check_docs.py
python3 tools/dev/check_localization.py
cd wrappers/apple-swift/demo-ios
xcodebuild test -project ScannerSDKiOSDemo.xcodeproj -scheme ScannerSDKiOSDemo -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.4.1' -only-testing:ScannerSDKiOSDemoTests
```

如果本机没有对应 simulator runtime，先运行：

```bash
./wrappers/apple-swift/scripts/check-ios-toolchain.sh
```
