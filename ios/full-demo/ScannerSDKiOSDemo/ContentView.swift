import ScannerSDK
import SwiftUI

private enum DemoRoute: Hashable {
    case console
    case logs
}

struct ContentView: View {
    @StateObject private var viewModel = AppViewModel()
    @StateObject private var localization = DemoLocalization.shared
    @State private var navigationPath: [DemoRoute] = []

    var body: some View {
        NavigationStack(path: $navigationPath) {
            DiscoveryPageView(
                viewModel: viewModel,
                selectedLanguage: localization.language,
                onLanguageChange: { localization.language = $0 },
                onConnect: viewModel.connect,
                onOpenConsole: { push(.console) },
                onOpenLogs: openLogs
            )
            .navigationDestination(for: DemoRoute.self) { route in
                switch route {
                case .console:
                    ConsolePageView(
                        viewModel: viewModel,
                        selectedLanguage: localization.language,
                        onLanguageChange: { localization.language = $0 },
                        onOpenLogs: openLogs
                    )
                case .logs:
                    AppLogsPageView(
                        viewModel: viewModel,
                        selectedLanguage: localization.language,
                        onLanguageChange: { localization.language = $0 }
                    )
                }
            }
        }
        .alert(DemoStrings.tr("error"), isPresented: Binding(
            get: { viewModel.alertErrorText != nil },
            set: { if !$0 { viewModel.alertErrorText = nil } }
        )) {
            Button(DemoStrings.tr("ok"), role: .cancel) {
                viewModel.alertErrorText = nil
            }
        } message: {
            Text(viewModel.alertErrorText ?? "")
        }
        .onChange(of: viewModel.sessionState) { newValue in
            syncNavigation(for: newValue)
        }
        .onChange(of: localization.language) { _ in
            viewModel.refreshLocalizedUi()
        }
        .onAppear {
            viewModel.onAppear()
            configureLaunchRouteIfNeeded()
        }
    }

    private func push(_ route: DemoRoute) {
        guard navigationPath.last != route else { return }
        navigationPath.append(route)
    }

    private func openLogs() {
        guard viewModel.canOpenLogs else { return }
        push(.logs)
    }

    private func syncNavigation(for sessionState: SessionState) {
        switch sessionState {
        case .ready:
            if navigationPath.last != .console {
                navigationPath = [.console]
            }
        case .disconnected, .error, .idle:
            navigationPath.removeAll { $0 == .console }
        default:
            break
        }
    }

    private func configureLaunchRouteIfNeeded() {
#if DEBUG
        let arguments = ProcessInfo.processInfo.arguments
        if arguments.contains("--scanner-sdk-demo-fake") {
            DispatchQueue.main.async {
                viewModel.enableFakeMode()
            }
        }
        if arguments.contains("--scanner-sdk-demo-ui-test-console") {
            viewModel.configureConsoleUiTestState()
            push(.console)
        }
        if arguments.contains("--scanner-sdk-demo-ui-test-discovery-error") {
            viewModel.configureDiscoveryErrorUiTestState()
        }
#endif
    }
}
