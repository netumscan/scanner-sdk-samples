import Foundation
import ScannerSDK

struct DemoSupportedModel: Identifiable, Equatable, Sendable {
    let modelKey: String
    let modelName: String
    let seriesKey: String
    let seriesName: String

    var id: String { modelKey }

    var primaryLabel: String {
        let name = modelName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            ? modelKey
            : modelName
        return name.caseInsensitiveCompare(modelKey) == .orderedSame
            ? name
            : "\(modelKey) · \(name)"
    }

    var secondaryLabel: String {
        seriesName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            ? seriesKey
            : seriesName
    }

    init(
        modelKey: String,
        modelName: String,
        seriesKey: String,
        seriesName: String
    ) {
        self.modelKey = modelKey
        self.modelName = modelName
        self.seriesKey = seriesKey
        self.seriesName = seriesName
    }

    init(sdkModel: SupportedDeviceModel) {
        self.init(
            modelKey: sdkModel.modelKey,
            modelName: sdkModel.modelName,
            seriesKey: sdkModel.seriesKey,
            seriesName: sdkModel.seriesName
        )
    }
}

func normalizeSupportedModels(_ models: [DemoSupportedModel]) -> [DemoSupportedModel] {
    var seen = Set<String>()
    return models.filter { model in
        let key = model.modelKey.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !key.isEmpty else { return false }
        return seen.insert(key.uppercased()).inserted
    }
}

func selectSupportedModel(_ models: [DemoSupportedModel], currentModelKey: String) -> String {
    models.first {
        $0.modelKey.caseInsensitiveCompare(currentModelKey) == .orderedSame
    }?.modelKey ?? models.first?.modelKey ?? ""
}
