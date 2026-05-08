# Android Demo Smoke Checklist

本清单用于真机演示、客户问题复现、以及 discovery / session / command 相关改动后的手工验证。

## 环境记录

每次对外演示或回归记录至少写清楚：

- 日期
- App commit
- SDK version 或 commit
- Android 手机型号
- Android 版本 / ROM
- 扫描枪型号
- 扫描枪固件版本
- 传输：BLE GATT / SPP Classic
- demo language：System / 中文 / English
- 是否清空过 App 数据和权限
- discovery / console 诊断摘要截图或导出记录
- App Logs compatibility record

## 启动和权限

- App 可启动，无启动崩溃或 ANR。
- `Init SDK` 成功。
- Android 12 及以上 BLE / SPP discovery 前至少确认 `BLUETOOTH_SCAN`、`BLUETOOTH_CONNECT` 和必要的 `ACCESS_FINE_LOCATION`。
- Android 11 及以下 BLE / SPP discovery 前确认 `ACCESS_FINE_LOCATION`。
- 蓝牙关闭时点击 `Start Discovery`，页面显示可读错误，App 不崩。
- 位置服务关闭且 ROM 门控扫描时，页面显示可读错误或恢复建议。

## 启动和语言

- 首次安装默认跟随系统语言。
- 语言选择支持 `跟随系统` / `中文` / `English`。
- 切换语言后 Discovery、Console、Logs 当前 UI 文案刷新。
- 切换语言后历史日志不强制改写，这属于预期行为。

## Discovery

- 初始状态未选 transport，不能开始扫描或加载设备。
- 选择 `BLE GATT` 后，`Start Discovery` 成功。
- BLE 设备列表逐步出现目标设备。
- `Stop Discovery` 成功，停止后日志不持续刷新发现事件。
- 选择 `SPP Classic` 后，BLE 设备不会混入 SPP 列表。
- SPP discovery 能发现本轮扫描到的 Classic / Dual 设备。
- 已配对但本轮未扫描到的 SPP 设备不应凭空显示。
- 手动输入 SPP 地址后能进入同一套连接流程。
- 切换 selected model 后，discovery summary 和连接后的客户选择型号一致。
- discovery 诊断摘要包含 demo version、Android version、权限、蓝牙/位置、transport、selected/resolved model、session state 和最近 failure。

## Connect / Session

- 从设备列表点击连接前，日志记录已停止 discovery。
- 连接成功后 session state 到达 `READY`。
- READY 后自动进入 `Scanner Control Console`。
- 控制台显示当前设备摘要、客户选择型号、SDK 诊断型号和能力摘要。
- 进入控制台后等待 2 秒，只触发一次自动刷新信息。
- console 诊断摘要包含当前 transport、selected/resolved model、session state 和最近 discovery/session failure。
- `Disconnect` 成功，页面回到 discovery。
- 设备主动断开后，控制台自动收口，不继续允许 ready-only 命令。
- 断开后重新连接同一设备成功。

## Scan Event

- 扫描一条普通条码。
- App Logs 能看到 scan event。
- 日志记录 source / level 可读。
- 如果后续客户现场版本导出扫码内容，先确认脱敏策略。

## Basic Commands

- `Refresh Info` 成功。
- `Get Battery` 成功或显示明确不支持原因。
- 日志里同时能看到原始响应和 `core` 解析后的结构化结果。
- `Ack Beep On/Off` 成功或显示明确不支持原因。
- `Vibrate On/Off` 成功或显示明确不支持原因。
- 自动刷新尚未完成时连续点击多个命令，命令按顺序完成，不并发抢写。
- command busy 时按钮禁用或显示明确等待状态。
- session 未 ready 或 operation/master command 不支持时，页面显示明确 guard 文案，日志有对应记录。

## Module Commands

- 只有支持 module command 的型号显示可执行状态。
- NT212X / NT280H / SE4750 catalog 文案可读。
- NTC06H setting catalog 文案可读。
- 读取参数成功或显示明确不支持原因。
- 写入参数前危险操作确认弹窗显示正确语言。
- NTC06H 设置码发送成功或 timeout 分支给出可读说明。
- 自定义 payload 校验错误能显示明确原因。

## Data Rule Builder

- prefix / suffix / hide / replace 等模式切换正常。
- 输入校验错误可读。
- 生成命令可见。
- 发送命令成功或显示明确失败原因。
- 危险或持久化相关操作有二次确认。

## App Logs

- source 筛选正常。
- level 筛选正常。
- search 正常。
- `Export Filtered` 导出当前筛选结果。
- `Export All` 导出 discovery + console 两侧事件。
- `Export Compatibility Record` 可导出平台、transport、model、session 和最近 failure。
- 导出内容包含导出时间、筛选 summary、事件数量和多行 command records。
- `SDK Diagnostics` 摘要包含 warning / error 统计和最近事件。
- `Clear` 后日志列表清空。

## 最小自动化回归

```bash
python3 tools/dev/check_localization.py
python3 tools/dev/check_demo_sdk_localization_coverage.py
python3 tools/dev/check_demo_sdk_localization_quality.py
cd wrappers/android-kotlin
./gradlew :demo:testDebugUnitTest
./gradlew :demo:assembleDebug
```

如果改动涉及 SDK API 或 native bridge，追加：

```bash
cd wrappers/android-kotlin
./gradlew :sdk:testReleaseUnitTest
./gradlew :sdk:assembleRelease
```
