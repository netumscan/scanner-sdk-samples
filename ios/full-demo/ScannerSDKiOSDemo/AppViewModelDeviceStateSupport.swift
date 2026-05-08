import Foundation
import ScannerSDK

@MainActor
extension AppViewModel {
    struct DeviceSnapshot {
        let info: ScannerInfo
        let batteryInfo: BatteryInfo?
        let modelConfigApplied: Bool
        let resolvedModel: DeviceModelId
        let capability: DeviceCapabilitySummary
    }

    func applyDeviceState(
        snapshot: DeviceSnapshot,
        effectiveCapability: DeviceCapabilitySummary,
        canExecuteModuleCommands: Bool
    ) {
        let summary = DeviceStateSummaryFormatter.format(info: snapshot.info)
        diagnosticsStore.updateSelectedModel(activeModelId)
        diagnosticsStore.updateResolvedModel(snapshot.resolvedModel)
        applyInfoSummary(summary.infoSummarySource)
        applySdkResolvedModelSummary(
            .resolved(selectedModelId: activeModelId, resolvedModel: snapshot.resolvedModel)
        )
        applyCapabilitySummary(.capability(effectiveCapability))
        applyModuleSummary(.capability(effectiveCapability))
        currentModuleFamily = effectiveCapability.moduleFamily
        supportsModuleCommands = effectiveCapability.supportsModuleCommands
        self.canExecuteModuleCommands = canExecuteModuleCommands
        if let batteryInfo = snapshot.batteryInfo {
            applyBatterySummary(.value(voltageText: batteryInfo.voltageText, percent: batteryInfo.percent))
        }
        applyDeviceCharsetSummary(summary.deviceCharsetSummarySource)
        applyDeviceTerminalSummary(summary.deviceTerminalSummarySource)
    }

    func refreshCachedState(_ session: ScannerSession) throws {
        let snapshot = DeviceSnapshot(
            info: try session.getCachedInfo(),
            batteryInfo: try session.getCachedBatteryInfo(),
            modelConfigApplied: false,
            resolvedModel: try session.getResolvedModelId(),
            capability: try session.getDeviceCapabilitySummary()
        )
        let effectiveCapability = effectiveCapabilityFor(snapshot.capability)
        let canExecuteModuleCommands = try queryModuleCommandAvailability(session)
        diagnosticsStore.updateSelectedModel(activeModelId)
        diagnosticsStore.updateResolvedModel(snapshot.resolvedModel)
        let summary = try CachedDeviceStateSummaryLoader.load(
            infoProvider: { snapshot.info }
        )
        applyInfoSummary(summary.infoSummarySource)
        applySdkResolvedModelSummary(
            .resolved(selectedModelId: activeModelId, resolvedModel: snapshot.resolvedModel)
        )
        applyCapabilitySummary(.capability(effectiveCapability))
        applyModuleSummary(.capability(effectiveCapability))
        currentModuleFamily = effectiveCapability.moduleFamily
        supportsModuleCommands = effectiveCapability.supportsModuleCommands
        self.canExecuteModuleCommands = canExecuteModuleCommands
        if let batteryInfo = snapshot.batteryInfo {
            applyBatterySummary(.value(voltageText: batteryInfo.voltageText, percent: batteryInfo.percent))
        }
        applyDeviceCharsetSummary(summary.deviceCharsetSummarySource)
        applyDeviceTerminalSummary(summary.deviceTerminalSummarySource)
    }

    func refreshLocalizedCachedState(_ session: ScannerSession) throws {
        let snapshot = DeviceSnapshot(
            info: try session.getCachedInfo(),
            batteryInfo: try session.getCachedBatteryInfo(),
            modelConfigApplied: false,
            resolvedModel: try session.getResolvedModelId(),
            capability: try session.getDeviceCapabilitySummary()
        )
        let effectiveCapability = effectiveCapabilityFor(snapshot.capability)
        let summary = try CachedDeviceStateSummaryLoader.load(
            infoProvider: { snapshot.info }
        )
        diagnosticsStore.updateSelectedModel(activeModelId)
        diagnosticsStore.updateResolvedModel(snapshot.resolvedModel)
        let canExecuteModuleCommands = session.latestState == .ready &&
            self.canExecuteModuleCommands
        applyInfoSummary(summary.infoSummarySource)
        applySdkResolvedModelSummary(
            .resolved(selectedModelId: activeModelId, resolvedModel: snapshot.resolvedModel)
        )
        applyCapabilitySummary(.capability(effectiveCapability))
        applyModuleSummary(.capability(effectiveCapability))
        currentModuleFamily = effectiveCapability.moduleFamily
        supportsModuleCommands = effectiveCapability.supportsModuleCommands
        self.canExecuteModuleCommands = canExecuteModuleCommands
        if let batteryInfo = snapshot.batteryInfo {
            applyBatterySummary(.value(voltageText: batteryInfo.voltageText, percent: batteryInfo.percent))
        } else {
            applyBatterySummary(.notLoaded)
        }
        applyDeviceCharsetSummary(summary.deviceCharsetSummarySource)
        applyDeviceTerminalSummary(summary.deviceTerminalSummarySource)
    }

    func applyCachedSuccessState(
        _ session: ScannerSession,
        actionResult: String,
        actionResultProvider: (() -> String)? = nil
    ) {
        do {
            let snapshot = DeviceSnapshot(
                info: try session.getCachedInfo(),
                batteryInfo: try session.getCachedBatteryInfo(),
                modelConfigApplied: false,
                resolvedModel: try session.getResolvedModelId(),
                capability: try session.getDeviceCapabilitySummary()
            )
            let effectiveCapability = effectiveCapabilityFor(snapshot.capability)
            let canExecuteModuleCommands = try queryModuleCommandAvailability(session)
            diagnosticsStore.updateSelectedModel(activeModelId)
            diagnosticsStore.updateResolvedModel(snapshot.resolvedModel)
            let state = try CachedCommandSuccessStateLoader.load(
                actionResult: actionResult,
                summaryProvider: {
                    try CachedDeviceStateSummaryLoader.load(infoProvider: { snapshot.info })
                }
            )
            applyInfoSummary(state.infoSummarySource)
            applySdkResolvedModelSummary(
                .resolved(selectedModelId: activeModelId, resolvedModel: snapshot.resolvedModel)
            )
            applyCapabilitySummary(.capability(effectiveCapability))
            applyModuleSummary(.capability(effectiveCapability))
            currentModuleFamily = effectiveCapability.moduleFamily
            supportsModuleCommands = effectiveCapability.supportsModuleCommands
            self.canExecuteModuleCommands = canExecuteModuleCommands
            if let batteryInfo = snapshot.batteryInfo {
                applyBatterySummary(.value(voltageText: batteryInfo.voltageText, percent: batteryInfo.percent))
            }
            applyDeviceCharsetSummary(state.deviceCharsetSummarySource)
            applyDeviceTerminalSummary(state.deviceTerminalSummarySource)
            if let actionResultProvider {
                setLocalizedLastActionResult(actionResultProvider)
            } else {
                lastActionResult = state.lastActionResult
                errorText = nil
            }
        } catch {
            if let actionResultProvider {
                setLocalizedLastActionResult(actionResultProvider)
            } else {
                lastActionResult = actionResult
                errorText = nil
            }
            appendEvent(.session, .warn, "\(DemoStrings.tr("refresh_cached_state_failed")): \(DemoErrorFormatter.detail(error))")
        }
    }

    func preferredCapabilityForSelectedModel() -> DeviceCapabilitySummary? {
        guard selectedModelId != .unknown else {
            return nil
        }
        return ScannerSDK.shared.getDeviceModelProfile(selectedModelId)?.capability
    }

    func preferredCapabilityForActiveModel() -> DeviceCapabilitySummary? {
        let modelId = activeModelId
        guard modelId != .unknown else {
            return nil
        }
        return ScannerSDK.shared.getDeviceModelProfile(modelId)?.capability
    }

    func effectiveCapabilityFor(_ runtimeCapability: DeviceCapabilitySummary) -> DeviceCapabilitySummary {
        mergePreferredCapabilityWithRuntime(
            preferredCapability: preferredCapabilityForActiveModel(),
            runtimeCapability: runtimeCapability
        )
    }

    func applyPreferredCapabilitySummary() {
        if let preferredCapability = preferredCapabilityForActiveModel() {
            selectedChannelKind = resolveProtocolChannelKind(for: preferredCapability)
            applyCapabilitySummary(.capability(preferredCapability))
            applyModuleSummary(.capability(preferredCapability))
            currentModuleFamily = preferredCapability.moduleFamily
            supportsModuleCommands = preferredCapability.supportsModuleCommands
            canExecuteModuleCommands = false
        } else {
            selectedChannelKind = .scannerMaster
            applyCapabilitySummary(.notLoaded)
            applyModuleSummary(.notLoaded)
            currentModuleFamily = .unknown
            supportsModuleCommands = false
            canExecuteModuleCommands = false
        }
        applySdkResolvedModelSummary(.notLoaded)
    }

    func resetDisconnectedDeviceState() {
        applyInfoSummary(.notLoaded)
        applyBatterySummary(.notLoaded)
        applyDeviceCharsetSummary(.notReadCharsetQuery)
        applyDeviceTerminalSummary(.notSetSessionCacheEmpty)
        applyPreferredCapabilitySummary()
    }

    func queryModuleCommandAvailability(
        _ session: ScannerSession
    ) throws -> Bool {
        if session.latestState != .ready {
            return false
        }
        return try session.getOperationSupportSummary().supportsDefaultModuleCommandProbe
    }
}
