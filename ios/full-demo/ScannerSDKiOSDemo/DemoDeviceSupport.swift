import Foundation
import ScannerSDK

let demoSupportedModels: [DeviceModelId] = [
    .cs7501,
    .cs8501,
    .c740,
    .c750,
    .cs9501,
    .nt91,
]

func displayModelLabel(_ modelId: DeviceModelId) -> String {
    (try? ScannerSDK.shared.getDeviceModelName(modelId)) ?? String(describing: modelId)
}

func resolveConnectionModelId(
    selectedModelId: DeviceModelId,
    discoveredModelId: DeviceModelId
) -> DeviceModelId {
    selectedModelId != .unknown ? selectedModelId : discoveredModelId
}

func formatSelectedModelSummary(_ selectedModelId: DeviceModelId) -> String {
    if selectedModelId == .unknown {
        return DemoStrings.tr("selected_model_not_provided")
    }
    return "\(DemoStrings.tr("customer_selected_model")): \(displayModelLabel(selectedModelId))"
}

func formatSdkResolvedModelSummary(
    selectedModelId: DeviceModelId,
    resolvedModel: DeviceModelId
) -> String {
    let resolvedValue = if resolvedModel == .unknown {
        DemoStrings.tr("not_resolved")
    } else {
        displayModelLabel(resolvedModel)
    }
    let detail: String
    if selectedModelId == .unknown || resolvedModel == .unknown || selectedModelId == resolvedModel {
        detail = resolvedValue
    } else {
        detail = DemoStrings.format("sdk_model_mismatch_detail", resolvedValue, displayModelLabel(selectedModelId))
    }
    return "\(DemoStrings.tr("sdk_resolved_model")): \(detail)"
}

func mergePreferredCapabilityWithRuntime(
    preferredCapability: DeviceCapabilitySummary?,
    runtimeCapability: DeviceCapabilitySummary
) -> DeviceCapabilitySummary {
    guard let preferredCapability else {
        return runtimeCapability
    }
    return DeviceCapabilitySummary(
        modelId: preferredCapability.modelId,
        modelName: preferredCapability.modelName,
        defaultCommandSet: preferredCapability.defaultCommandSet,
        formFactor: preferredCapability.formFactor,
        moduleFamily: preferredCapability.moduleFamily,
        supportsBasicDeviceCommands: runtimeCapability.supportsBasicDeviceCommands,
        supportsMasterCommands: runtimeCapability.supportsMasterCommands,
        supportsNativeModuleCommands: runtimeCapability.supportsNativeModuleCommands,
        supportsModuleCommandBridge: runtimeCapability.supportsModuleCommandBridge,
        supportsModuleCommands: runtimeCapability.supportsModuleCommands,
        supportsScannerMaster: runtimeCapability.supportsScannerMaster,
        supportsModulePassthrough: runtimeCapability.supportsModulePassthrough,
        supportStatus: preferredCapability.supportStatus
    )
}

enum DemoSessionOperation {
    case initializeSession
    case getBatteryInfo
    case basicDeviceCommands
    case textCommands
    case dataRuleCommands
    case beep
    case disableAckBeep
    case vibrateOn
    case vibrateOff
}

func supportsSessionOperation(
    _ operation: DemoSessionOperation,
    support: SessionOperationSupportSummary
) -> Bool {
    switch operation {
    case .initializeSession:
        return support.supportsInitializeSession
    case .getBatteryInfo:
        return support.supportsGetBatteryInfo
    case .basicDeviceCommands:
        return support.supportsExecuteBasicDeviceCommands
    case .textCommands:
        return support.supportsExecuteTextCommands
    case .dataRuleCommands:
        return support.supportsExecuteDataRuleCommands
    case .beep:
        return support.supportsBeep
    case .disableAckBeep:
        return support.supportsDisableAckBeep
    case .vibrateOn:
        return support.supportsVibrateOn
    case .vibrateOff:
        return support.supportsVibrateOff
    }
}

func formatCapabilitySummary(_ capability: DeviceCapabilitySummary) -> String {
    let commandSetLabel = capability.defaultCommandSet.localizedLabel
    let formFactorLabel = capability.formFactor.localizedLabel
    let statusLabel = capability.supportStatus.localizedLabel
    return DemoStrings.format(
        "capability_summary_format",
        fallback: "%@ / %@ / master=%@ / scannerMaster=%@ / status=%@",
        DemoStrings.sdk(commandSetLabel.localizationKey, fallback: commandSetLabel.fallbackDisplayName),
        DemoStrings.sdk(formFactorLabel.localizationKey, fallback: formFactorLabel.fallbackDisplayName),
        supportFlag(capability.supportsMasterCommands),
        supportFlag(capability.supportsScannerMaster),
        DemoStrings.sdk(statusLabel.localizationKey, fallback: statusLabel.fallbackDisplayName)
    )
}

func formatModuleSummary(_ capability: DeviceCapabilitySummary) -> String {
    let familyLabel = capability.moduleFamily.localizedLabel
    return DemoStrings.format("module_summary_format", DemoStrings.sdk(familyLabel.localizationKey, fallback: familyLabel.fallbackDisplayName), supportFlag(capability.supportsNativeModuleCommands), supportFlag(capability.supportsModuleCommandBridge), supportFlag(capability.supportsModuleCommands), supportFlag(capability.supportsModulePassthrough))
}

func supportFlag(_ value: Bool) -> String {
    DemoStrings.tr(value ? "support_yes" : "support_no")
}

func formatModuleCommandAvailabilitySummary(canExecute: Bool, supportsModuleCommands: Bool) -> String {
    if canExecute {
        return DemoStrings.tr("ready")
    }
    if supportsModuleCommands {
        return DemoStrings.tr("not_ready")
    }
    return DemoStrings.tr("not_detected")
}

func resolveProtocolChannelKind(for capability: DeviceCapabilitySummary?) -> ProtocolChannelKind {
    guard let capability else {
        return .scannerMaster
    }
    if capability.defaultCommandSet == .moduleOnly {
        return .modulePassthrough
    }
    if capability.supportsModulePassthrough && !capability.supportsScannerMaster {
        return .modulePassthrough
    }
    return .scannerMaster
}

func displayModuleFamilyLabel(_ family: ModuleFamily) -> String {
    let label = family.localizedLabel
    return DemoStrings.sdk(label.localizationKey, fallback: label.fallbackDisplayName)
}

func displayTransportLabel(_ transportType: TransportType) -> String {
    let label = transportType.localizedLabel
    return DemoStrings.sdk(label.localizationKey, fallback: label.fallbackDisplayName)
}

func formatDiscoverySignalSummary(_ device: DiscoveredDevice) -> String {
    guard let rssi = device.rssi else {
        return "RSSI -"
    }
    return "RSSI \(rssi)"
}

func formatDiscoveryDeviceSummary(_ device: DiscoveredDevice) -> String {
    let modelSummary = DemoStrings.withLocalizedValue(
        "discovery_model",
        fallback: "Model",
        value: displayModelLabel(device.modelId)
    )
    let transportSummary = DemoStrings.withLocalizedValue(
        "discovery_transport",
        fallback: "Transport",
        value: displayTransportLabel(device.transportType)
    )
    let signalSummary = DemoStrings.withLocalizedValue(
        "discovery_signal",
        fallback: "Signal",
        value: formatDiscoverySignalSummary(device)
    )
    return [modelSummary, transportSummary, signalSummary].joined(separator: " / ")
}

func statusText(_ state: SessionState) -> String {
    let label = state.localizedLabel
    return DemoStrings.sdk(label.localizationKey, fallback: label.fallbackDisplayName)
}
