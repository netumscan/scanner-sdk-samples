import SwiftUI

enum ConsoleOperationScope: String, CaseIterable, Identifiable {
    case master
    case module

    var id: String { rawValue }

    var title: String {
        switch self {
        case .master:
            return DemoStrings.tr("master")
        case .module:
            return DemoStrings.tr("module")
        }
    }
}

struct ConsoleSummaryCard: View {
    let title: String
    let isExecuting: Bool
    let onDisconnect: () -> Void
    let rows: [(String, String)]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.headline)
                }
                Spacer()
                Button(DemoStrings.tr("disconnect")) {
                    onDisconnect()
                }
                .buttonStyle(.bordered)
                .tint(.red)
                .disabled(isExecuting)
            }

            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                ConsoleSummaryChip(label: row.0, value: row.1)
            }
        }
        .padding(16)
        .background(Color.secondary.opacity(0.08), in: RoundedRectangle(cornerRadius: 16))
    }
}

struct ConsoleOperationScopeRow: View {
    let selectedScope: ConsoleOperationScope
    let onSelect: (ConsoleOperationScope) -> Void

    var body: some View {
        HStack(spacing: 8) {
            ForEach(ConsoleOperationScope.allCases) { scope in
                Button(scope.title) {
                    onSelect(scope)
                }
                .buttonStyle(.borderedProminent)
                .tint(selectedScope == scope ? .accentColor : .gray.opacity(0.45))
                .frame(maxWidth: .infinity)
                .accessibilityIdentifier(DemoAccessibility.consoleOperationScopeButton(scope.rawValue))
            }
        }
    }
}

struct ConsoleSummaryChip: View {
    let label: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
            Text(value)
                .font(.subheadline)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
    }
}

struct ConsoleFeedbackBanner: View {
    let text: String
    let background: Color
    let foreground: Color

    var body: some View {
        Text(text)
            .font(.subheadline)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(background, in: RoundedRectangle(cornerRadius: 12))
            .foregroundStyle(foreground)
    }
}

struct ConsoleHintBanner: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.footnote)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(Color.secondary.opacity(0.08), in: RoundedRectangle(cornerRadius: 12))
            .foregroundStyle(.secondary)
    }
}

struct ConsoleGroupCard<Content: View>: View {
    let title: String
    let summary: String
    let isDangerous: Bool
    let content: Content

    init(
        title: String,
        summary: String,
        isDangerous: Bool,
        @ViewBuilder content: () -> Content
    ) {
        self.title = title
        self.summary = summary
        self.isDangerous = isDangerous
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .center, spacing: 8) {
                Text(title)
                    .font(.subheadline.weight(.semibold))
                if isDangerous {
                    Text(DemoStrings.tr("high_risk"))
                        .font(.caption2.weight(.bold))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .overlay(
                            Capsule()
                                .stroke(Color.red, lineWidth: 1)
                        )
                        .foregroundStyle(.red)
                }
                Spacer()
                Text(summary)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            content
        }
        .padding(14)
        .background(Color.secondary.opacity(0.08), in: RoundedRectangle(cornerRadius: 14))
    }
}

struct ConsoleSubcard<Content: View>: View {
    let title: String
    let subtitle: String?
    let content: Content

    init(
        title: String,
        subtitle: String? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.title = title
        self.subtitle = subtitle
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.subheadline.weight(.semibold))
            if let subtitle {
                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            content
        }
        .padding(14)
        .background(Color.secondary.opacity(0.08), in: RoundedRectangle(cornerRadius: 14))
    }
}
