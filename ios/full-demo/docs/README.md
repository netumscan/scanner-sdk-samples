# iOS Demo 维护文档

本目录记录 `ScannerSDKiOSDemo` 作为长期 demo app 维护时的开发计划、架构边界和验收清单。

## 文档

- [maintenance-plan.md](maintenance-plan.md)：长期维护计划、拆分路线和验收标准。
- [development-rules.md](development-rules.md)：新增功能、命令、多语言、日志和测试的开发规则。
- [smoke-checklist.md](smoke-checklist.md)：真机或模拟器演示前的手工 smoke checklist。
- [仓库级 iOS demo 指南](../../../../docs/guides/demo/ios-demo.md)：启动方式、维护边界和真机回归入口。
- [跨平台状态矩阵](../../../../docs/guides/demo/mobile-demo-status-matrix.md)：Android/iOS demo 能力对齐状态。
- [兼容矩阵模板](../../../../docs/guides/demo/mobile-demo-compatibility-matrix.md)：真机回归记录模板。

## 不放什么

- 不放 SDK 底层协议设计；协议设计放 `docs/architecture/protocols/`。
- 不放 Swift public API 说明；API 文档放 `docs/api/swift-api.md`。
- 不放发布计划；发布文档放 `docs/architecture/release/`。
- 不放供应商原始资料或设备规格。
