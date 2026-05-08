# Android Demo 开发规则

## 新增页面或功能

新增功能前先判断它属于哪一类：

| 类型 | 应放位置 | 规则 |
| --- | --- | --- |
| 纯展示 Composable | `*Screen.kt` / `*Ui*.kt` | 不直接调用 SDK，不写协议判断 |
| 页面状态聚合 | `DemoViewModel`、`CommandConsoleViewModel` 或后续拆出的 state object | 只编排，不复制协议逻辑 |
| SDK discovery / connect | `DiscoveryCoordinator` 方向 | 处理权限、平台状态、discovery failure、recovery |
| 命令执行 | `SessionCommandRunner` 方向 | 统一 busy、session ready、operation support、错误反馈 |
| 模组命令 | `ModuleCommandRunner` 方向 | 按 model / capability gating，危险操作二次确认 |
| 诊断信息 | `DemoDiagnosticsStore` 方向 | 保留 SDK code、platform raw code、权限和环境信息 |
| 日志和导出 | `AppLogStore` / `LogExport` | 不在 View 里拼日志，不泄露敏感扫码内容 |
| 文案和格式化 | `DemoStrings` / formatter / catalog | 不在 View 中散落硬编码业务文案 |

如果一个改动跨越三类以上，先拆小。不要用“只是 demo”当理由把逻辑塞进 UI。

## 命令和设备能力

- 新增命令必须优先使用 Kotlin wrapper 公开 API。
- 新增命令必须通过 `SessionCommandRunner` 或同等 runner 入口执行，不能在 ViewModel 里重新散落 busy、ready、support、try/catch 分支。
- runner 必须统一处理：
  - 已有命令正在执行时的 busy 提示；
  - session 不存在或未 ready 时的可读错误；
  - operation support / master command support 不支持时的可读原因；
  - 成功、失败反馈和 App Logs 事件。
- 只有工程或诊断场景允许 raw frame / raw text command，并且 UI 必须明确标记风险。
- 命令按钮必须受 `selectedModelId`、`capabilitySummary`、`operation support` 或 module family 控制。
- 不支持的能力不要靠执行失败来告诉用户，应该在 UI 层禁用、隐藏或给出明确原因。
- 危险操作必须走确认弹窗，确认标题、正文和按钮文案要本地化。
- demo 不允许自己解析设备响应来覆盖 `core` 的结构化结果。

## Discovery 和权限

- 初始状态不能默认选中 BLE 或 SPP；用户必须确认传输模式。
- BLE 和 SPP 设备列表分开维护，不允许混在同一个可连接列表里。
- 连接前应停止当前 discovery，并在日志里记录。
- Android 12+ 仍要保留 `ACCESS_FINE_LOCATION` 的兼容说明，因为部分 ROM 会继续门控扫描回调。
- permission denied、Bluetooth disabled、Location disabled、scanner unavailable 必须有可读提示。
- 可恢复错误应提供明确 recovery action 或操作建议。

## 多语言

- demo 普通文案使用 `DemoStrings` / `demoStringResource`。
- SDK runtime label 使用 `nsdk.*` 语义 key 和对应 fallback。
- 不要直接在业务代码里散落 `getString(...)` 后保存为长期 UI 状态。
- 当前 UI 状态应保存 semantic source、provider 或原始值，语言切换后可以重新格式化。
- 日志是历史记录，可以保存事件发生时的文案。
- 新增文案必须同时补 `values/strings.xml` 和 `values-zh-rCN/strings.xml`。
- 新增 SDK label 必须通过 demo SDK localization coverage / quality 检查。

## 日志和诊断

- 日志必须有 source 和 level。
- SDK debug / warning / error 事件要进入 App Logs。
- discovery、session、command failure 必须保留 operation、error code、platform raw code 或原始异常信息。
- discovery 和 console 的诊断摘要必须覆盖 demo version、Android version、权限、蓝牙/定位状态、transport、selected/resolved model、session state 和最近 failure。
- 日志导出默认导出当前筛选结果，另提供全部导出。
- 扫码数据、设备 ID、蓝牙地址、平台错误等进入客户现场版本前，必须按仓库级日志脱敏说明处理。
- 诊断面板只展示事实和恢复建议，不反向修改 selected model 或 capability summary。

## Fake / 测试模式

- fake discovery / fake session 只能模拟 wrapper 行为，不得成为另一套协议实现。
- fake 数据必须覆盖 BLE、SPP、ready、busy、unsupported、timeout、disconnected 和 permission failure。
- UI test 和截图测试优先走 fake 数据，真机 checklist 覆盖真实 BLE/SPP。
- fake 模式不得默认进入 release 客户演示包，除非 UI 明确标记。

## 测试

新增行为优先补单测：

- 本地化：补语言切换、资源覆盖、fallback 测试。
- discovery：补未选 transport、permission denied、stop before connect、BLE/SPP 列表隔离。
- command：补 session not ready、unsupported、busy、success / failure。
- module：补 title provider、parameter ID、payload、timeout、requires save 分支。
- logs：补 source / level / search / export summary。
- formatter / catalog：补纯函数测试。

UI 行为改动优先补 `src/androidTest`，尤其是危险确认、多语言刷新、日志筛选和主流程页面状态。

## 提交前检查

常用最小命令：

```bash
python3 tools/dev/check_docs.py
python3 tools/dev/check_localization.py
python3 tools/dev/check_demo_sdk_localization_coverage.py
python3 tools/dev/check_demo_sdk_localization_quality.py
cd wrappers/android-kotlin
./gradlew :demo:testDebugUnitTest
./gradlew :demo:assembleDebug
```

涉及 SDK API 或 native bridge 时追加：

```bash
cd wrappers/android-kotlin
./gradlew :sdk:testReleaseUnitTest
./gradlew :sdk:assembleRelease
```

涉及真机 discovery、connect、SPP、command 或 module command 时，按 [smoke-checklist.md](smoke-checklist.md) 执行手工验证。
