import SwiftUI

@main
struct ScannerSDKQuickStartApp: App {
    @StateObject private var model = QuickStartViewModel()

    var body: some Scene {
        WindowGroup {
            ContentView(model: model)
        }
    }
}

