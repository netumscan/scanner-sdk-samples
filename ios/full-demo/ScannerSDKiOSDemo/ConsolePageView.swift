import SwiftUI
import ScannerSDK

enum DemoPendingDangerAction {
    case command(DemoCommandEntry)
    case quickAction(DemoQuickAction)
    case moduleAction(DemoModuleAction)

    var title: String {
        switch self {
        case .command(let entry):
            return Self.currentCommandEntry(matching: entry.command)?.title ?? entry.title
        case .quickAction(let action):
            return action.title
        case .moduleAction(let action):
            return Self.currentModuleAction(matching: action.id)?.title ?? action.title
        }
    }

    var riskText: String {
        switch self {
        case .command(let entry):
            return entry.riskText
        case .quickAction(let action):
            return action.riskText
        case .moduleAction(let action):
            return Self.currentModuleAction(matching: action.id)?.riskText ?? action.riskText
        }
    }

    private static func currentCommandEntry(matching command: DemoConsoleCommand) -> DemoCommandEntry? {
        DemoCommandCatalog.groups
            .flatMap(\.commands)
            .first { $0.command == command }
    }

    private static func currentModuleAction(matching id: String) -> DemoModuleAction? {
        DemoCommandCatalog.moduleActionRows
            .flatMap(\.actions)
            .first { $0.id == id }
    }
}

struct ConsolePageView: View {
    @ObservedObject var viewModel: AppViewModel
    let selectedLanguage: DemoLanguage
    let onLanguageChange: (DemoLanguage) -> Void
    let onOpenLogs: () -> Void

    @State private var parseStateExpanded = false
    @State private var quickActionsExpanded = true
    @State private var commandsExpanded = true
    @State private var builderExpanded = true
    @State private var selectedGroupIndex = Self.initialSelectedGroupIndex()
    @State private var selectedOperationScope: ConsoleOperationScope = Self.initialOperationScope()
    @State private var pendingDangerAction: DemoPendingDangerAction?
    @State private var builderMode: DataRuleFormMode = .prefix
    @State private var builderValueA = ""
    @State private var builderValueB = ""
    @State private var moduleDomainIndex = 0
    @State private var inlineModulePresetID: String?
    @State private var moduleParameterIDText = ""
    @State private var modulePayloadHexText = ""
    @State private var moduleNumericInputText = ""
    @State private var modulePersistWrite = true

    var body: some View {
        List {
            ConsoleStatusSection(viewModel: viewModel)
            ConsoleOperationsSection(
                viewModel: viewModel,
                parseStateExpanded: $parseStateExpanded,
                quickActionsExpanded: $quickActionsExpanded,
                commandsExpanded: $commandsExpanded,
                builderExpanded: $builderExpanded,
                selectedGroupIndex: $selectedGroupIndex,
                selectedOperationScope: $selectedOperationScope,
                pendingDangerAction: $pendingDangerAction,
                builderMode: $builderMode,
                builderValueA: $builderValueA,
                builderValueB: $builderValueB,
                moduleDomainIndex: $moduleDomainIndex,
                inlineModulePresetID: $inlineModulePresetID,
                moduleParameterIDText: $moduleParameterIDText,
                modulePayloadHexText: $modulePayloadHexText,
                moduleNumericInputText: $moduleNumericInputText,
                modulePersistWrite: $modulePersistWrite
            )
        }
        .accessibilityIdentifier(DemoAccessibility.consoleList)
        .navigationTitle(DemoStrings.tr("scanner_console"))
        .toolbar {
            DemoLanguageToolbarMenu(
                selectedLanguage: selectedLanguage,
                onLanguageChange: onLanguageChange
            )
            DemoAppLogsToolbarButton(onOpenLogs: onOpenLogs)
        }
        .alert(DemoStrings.tr("dangerous_command"), isPresented: Binding(
            get: { pendingDangerAction != nil },
            set: { if !$0 { pendingDangerAction = nil } }
        )) {
            Button(DemoStrings.tr("cancel"), role: .cancel) {
                pendingDangerAction = nil
            }
            Button(DemoStrings.tr("execute"), role: .destructive) {
                switch pendingDangerAction {
                case .command(let entry):
                    viewModel.executeCommand(entry)
                case .quickAction(let action):
                    viewModel.performQuickAction(action)
                case .moduleAction(let action):
                    viewModel.performModuleAction(action)
                case .none:
                    break
                }
                pendingDangerAction = nil
            }
        } message: {
            Text(
                "\(pendingDangerAction?.title ?? DemoStrings.tr("dangerous_action"))\n\n" +
                (pendingDangerAction?.riskText ?? DemoStrings.tr("dangerous_command_default_risk"))
            )
        }
        .onAppear {
            configureDangerDialogForUiTestIfNeeded()
        }
    }

    private func configureDangerDialogForUiTestIfNeeded() {
#if DEBUG
        guard ProcessInfo.processInfo.arguments.contains("--scanner-sdk-demo-ui-test-danger-dialog"),
              pendingDangerAction == nil else {
            return
        }
        pendingDangerAction = .quickAction(.ackBeepOn)
#endif
    }

    private static func initialOperationScope() -> ConsoleOperationScope {
#if DEBUG
        ProcessInfo.processInfo.arguments.contains("--scanner-sdk-demo-ui-test-module-scope") ? .module : .master
#else
        .master
#endif
    }

    private static func initialSelectedGroupIndex() -> Int {
#if DEBUG
        guard ProcessInfo.processInfo.arguments.contains("--scanner-sdk-demo-ui-test-data-rules") else {
            return 0
        }
        return DemoCommandCatalog.groups.firstIndex { group in
            group.commands.contains { entry in
                if case .text(let commandText) = entry.command {
                    return commandText == "$SCAN#4" || commandText == "$SCAN#3"
                }
                return false
            }
        } ?? 0
#else
        0
#endif
    }
}
