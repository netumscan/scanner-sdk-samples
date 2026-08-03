import Foundation
import ScannerSDK

let demoSupportedModels: [String] = [
    "CS7501",
    "CS8501",
    "C740",
    "C750",
    "CS9501",
    "NT-91",
]

func displayModelLabel(_ modelKey: String) -> String {
    if modelKey == "" {
        return DemoStrings.tr("none")
    }
    return modelKey
}

func resolveConnectionModelKey(
    selectedModelKey: String,
    discoveredModelKey: String
) -> String {
    selectedModelKey != "" ? selectedModelKey : discoveredModelKey
}

func formatSelectedModelSummary(_ selectedModelKey: String) -> String {
    if selectedModelKey == "" {
        return DemoStrings.tr("selected_model_not_provided")
    }
    return "\(DemoStrings.tr("customer_selected_model")): \(displayModelLabel(selectedModelKey))"
}

func formatSdkResolvedModelSummary(
    selectedModelKey: String,
    resolvedModel: String
) -> String {
    let resolvedValue = resolvedModel == ""
        ? DemoStrings.tr("not_resolved")
        : displayModelLabel(resolvedModel)
    if selectedModelKey == "" || resolvedModel == "" || selectedModelKey == resolvedModel {
        return "\(DemoStrings.tr("sdk_resolved_model")): \(resolvedValue)"
    }
    return "\(DemoStrings.tr("sdk_resolved_model")): \(resolvedValue) / selected=\(displayModelLabel(selectedModelKey))"
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
        value: displayModelLabel(device.modelKey)
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

func supportFlag(_ value: Bool) -> String {
    DemoStrings.tr(value ? "support_yes" : "support_no")
}
