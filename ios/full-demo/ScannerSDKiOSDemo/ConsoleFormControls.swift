import SwiftUI

struct ConsoleActionRows: View {
    let labels: [String]
    let isDangerous: (Int) -> Bool
    let action: (Int) -> Void
    var disabled = false
    var accessibilityIdentifier: (Int) -> String? = { _ in nil }

    var body: some View {
        VStack(spacing: 8) {
            ForEach(Array(labels.chunked(into: 2).enumerated()), id: \.offset) { rowIndex, row in
                HStack(spacing: 8) {
                    ForEach(Array(row.enumerated()), id: \.offset) { columnIndex, label in
                        let index = rowIndex * 2 + columnIndex
                        ConsoleActionButton(
                                label: label,
                                dangerous: isDangerous(index),
                                disabled: disabled,
                                accessibilityIdentifier: accessibilityIdentifier(index),
                                action: { action(index) }
                            )
                        }
                    if row.count == 1 {
                        Spacer()
                            .frame(maxWidth: .infinity)
                    }
                }
            }
        }
    }
}

struct DataRuleModeSelector: View {
    let selectedMode: DataRuleFormMode
    let onSelect: (DataRuleFormMode) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(DataRuleFormMode.allCases) { mode in
                    Button(mode.title) {
                        onSelect(mode)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(selectedMode == mode ? .accentColor : .gray.opacity(0.45))
                }
            }
        }
    }
}

struct ConsoleBuilderField: View {
    let title: String
    @Binding var text: String
    var keyboard: UIKeyboardType = .default

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
            TextField(title, text: $text)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .keyboardType(keyboard)
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
        }
    }
}

struct ConsoleExpandableBlock<Content: View>: View {
    let title: String
    var accessibilityIdentifier: String?
    @Binding var isExpanded: Bool
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Button {
                isExpanded.toggle()
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: isExpanded ? "chevron.down" : "chevron.right")
                        .font(.caption.weight(.semibold))
                        .frame(width: 14)
                    Text(title)
                        .font(.body.weight(.semibold))
                        .optionalAccessibilityIdentifier(accessibilityIdentifier)
                    Spacer()
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .optionalAccessibilityIdentifier(accessibilityIdentifier)

            if isExpanded {
                content()
                    .padding(.leading, 6)
            }
        }
    }
}

struct ConsoleActionButton: View {
    let label: String
    let dangerous: Bool
    let disabled: Bool
    var accessibilityIdentifier: String?
    let action: () -> Void

    @ViewBuilder
    var body: some View {
        if let accessibilityIdentifier {
            button.accessibilityIdentifier(accessibilityIdentifier)
        } else {
            button
        }
    }

    private var button: some View {
        Button(action: action) {
            Text(dangerous ? "! \(label)" : label)
                .frame(maxWidth: .infinity)
                .multilineTextAlignment(.center)
        }
        .buttonStyle(.borderedProminent)
        .tint(dangerous ? .red.opacity(0.18) : .clear)
        .foregroundStyle(dangerous ? .red : .primary)
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(dangerous ? Color.red.opacity(0.6) : Color.secondary.opacity(0.35), lineWidth: 1)
        )
        .disabled(disabled)
    }
}

extension Array {
    func chunked(into size: Int) -> [[Element]] {
        guard size > 0 else { return [] }
        return stride(from: 0, to: count, by: size).map { index in
            Array(self[index..<Swift.min(index + size, count)])
        }
    }
}

private extension View {
    @ViewBuilder
    func optionalAccessibilityIdentifier(_ identifier: String?) -> some View {
        if let identifier {
            accessibilityIdentifier(identifier)
        } else {
            self
        }
    }
}

extension Comparable {
    func clamped(to limits: ClosedRange<Self>) -> Self {
        Swift.min(Swift.max(self, limits.lowerBound), limits.upperBound)
    }
}
