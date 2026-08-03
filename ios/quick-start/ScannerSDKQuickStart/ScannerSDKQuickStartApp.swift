import SwiftUI

@main
struct ScannerSDKQuickStartApp: App {
    @StateObject private var viewModel = QuickStartViewModel(
        backend: ScannerQuickStartBackend()
    )

    var body: some Scene {
        WindowGroup {
            ContentView(viewModel: viewModel)
        }
    }
}
