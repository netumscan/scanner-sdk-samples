import SwiftUI

struct DemoLanguageToolbarMenu: ToolbarContent {
    let selectedLanguage: DemoLanguage
    let onLanguageChange: (DemoLanguage) -> Void

    var body: some ToolbarContent {
        ToolbarItem(placement: .topBarTrailing) {
            Menu {
                ForEach(DemoLanguage.allCases) { language in
                    Button(language.label) {
                        onLanguageChange(language)
                    }
                    .disabled(language == selectedLanguage)
                }
            } label: {
                Text(DemoStrings.tr("language"))
            }
        }
    }
}

struct DemoAppLogsToolbarButton: ToolbarContent {
    let onOpenLogs: () -> Void
    var isDisabled = false

    var body: some ToolbarContent {
        ToolbarItem(placement: .topBarTrailing) {
            Button(DemoStrings.tr("app_logs")) {
                onOpenLogs()
            }
            .disabled(isDisabled)
            .accessibilityIdentifier(DemoAccessibility.appLogsToolbarButton)
        }
    }
}
