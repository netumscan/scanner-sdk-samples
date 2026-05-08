import SwiftUI

struct DemoScenarioPresetView: View {
    let presets: [DemoScenarioPreset] = DemoScenarioPresets.all

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(DemoStrings.tr("demo_scenario_presets"))
                .font(.headline)
            ForEach(presets) { preset in
                VStack(alignment: .leading, spacing: 3) {
                    Text(preset.title)
                        .font(.subheadline.weight(.semibold))
                    Text(preset.summary)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(.vertical, 4)
    }
}
