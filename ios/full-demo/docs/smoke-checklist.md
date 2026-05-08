# iOS Demo Smoke Checklist

本清单用于真机演示、客户问题复现、以及 discovery / session / command 相关改动后的手工验证。

## 环境记录

每次对外演示或回归记录至少写清楚：

- 日期
- App commit
- SDK version 或 commit
- iPhone 型号
- iOS 版本
- 扫描枪型号
- 扫描枪固件版本
- 传输：BLE GATT
- demo language：System / 中文 / English
- discovery / console 诊断摘要截图或导出记录
- App Logs compatibility record

## 启动和语言

- App 可启动，无启动崩溃。
- 语言选择支持 System / 中文 / English。
- 切换语言后 Discovery、Console、Logs 的当前 UI 文案刷新。
- 切换语言后历史日志不强制改写，这属于预期行为。

## Discovery

- `Init SDK` 成功。
- 选择目标型号后，协议模式自动匹配。
- `Start Discovery` 成功。
- 能发现目标设备。
- 设备列表显示 name / device id / model / transport / RSSI。
- discovery 诊断摘要包含 App build、iOS version、蓝牙权限、transport、selected/resolved model、session state 和最近 failure。
- `Stop Discovery` 成功。
- 蓝牙关闭或权限异常时，显示可读错误和恢复动作。

## Connect / Session

- 从设备列表点击连接成功。
- session state 到达 `ready`。
- Console 页面显示当前设备摘要。
- SDK 解析型号、能力摘要、模组能力摘要可读。
- console 诊断摘要包含当前 transport、selected/resolved model、session state 和最近 discovery/session failure。
- `Disconnect` 成功。
- 断开后命令按钮不会继续执行 ready-only 操作。
- 断开后重新连接同一设备成功。

## Scan Event

- 扫描一条普通条码。
- App Logs 能看到 scan event。
- 本地 `ScanTextCharset` 切换后，新扫码文本按当前配置显示。
- terminator 切换后，本地解析行为符合预期。

## Basic Commands

- `Refresh Info` 成功。
- `Get Battery` 成功或显示明确不支持原因。
- `%CHARSET#` / master command 成功或显示明确不支持原因。
- 不支持的 operation 不应表现为无响应。
- command busy 时给出明确等待提示。
- session 未 ready 或 operation/master command 不支持时，页面显示明确 guard 文案，日志有对应记录。

## Module Commands

- 只有支持 module command 的型号显示可执行状态。
- NTC06H / NT212X / NT280H / SE4750 catalog 文案可读。
- 读取参数成功或显示明确不支持原因。
- 写入参数前危险操作确认弹窗显示正确语言。
- NTC06H 设置码发送成功或 timeout 分支给出可读说明。
- 自定义 payload 校验错误能显示明确原因。

## Data Rule Builder

- prefix / suffix / replace 模式切换正常。
- 输入校验错误可读。
- 生成命令可见。
- 发送命令成功或显示明确失败原因。

## App Logs

- source 筛选正常。
- level 筛选正常。
- search 正常。
- `Export Filtered` 可打开系统分享面板。
- `Export All` 可打开系统分享面板。
- `Export Compatibility Record` 可打开系统分享面板，并包含平台、transport、model、session 和最近 failure。
- `SDK Diagnostics` 摘要包含 warning / error 统计和最近事件。
- `Clear` 后日志列表清空。

## 最小自动化回归

```bash
python3 tools/dev/check_localization.py
cd wrappers/apple-swift/demo-ios
xcodebuild test -project ScannerSDKiOSDemo.xcodeproj -scheme ScannerSDKiOSDemo -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.4.1' -only-testing:ScannerSDKiOSDemoTests
```
