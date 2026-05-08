import Foundation
import ScannerSDK

@MainActor
extension AppViewModel {
    func bindGlobalStreamsIfNeeded() {
        guard !globalStreamsBound else { return }
        globalStreamsBound = true

        discoveryFailureTask = Task { [weak self] in
            guard let self else { return }
            for await failure in discoveryCoordinator.discoveryFailures {
                if Task.isCancelled { return }
                let label = failure.localizedStatusLabel
                self.applyDiscoveryFailure(failure)
                self.appendEvent(
                    .sdk,
                    failure.recoverable ? .warn : .error,
                    DemoStrings.withLocalizedValue(
                        "discovery_failure",
                        fallback: "Discovery failure",
                        value: "\(DemoStrings.sdk(label.localizationKey, fallback: label.fallbackDisplayName)) / \(failure.message)"
                    )
                )
            }
        }

        sessionFailureTask = Task { [weak self] in
            guard let self else { return }
            for await failure in ScannerSDK.shared.sessionFailures {
                if Task.isCancelled { return }
                let label = failure.code.localizedStatusLabel
                self.applySessionFailure(failure)
                self.appendEvent(
                    .sdk,
                    .error,
                    DemoStrings.withLocalizedValue(
                        "global_session_failure",
                        fallback: "Global session failure",
                        value: "\(DemoStrings.sdk(label.localizationKey, fallback: label.fallbackDisplayName)) / \(failure.message)"
                    )
                )
            }
        }

        sdkDebugTask = Task { [weak self] in
            guard let self else { return }
            for await message in discoveryCoordinator.debugEvents {
                if Task.isCancelled { return }
                let event = makeSdkConsoleEvent(message)
                self.appendEvent(event.source, event.level, event.message)
            }
        }
    }

    func applyDiscoveryFailure(_ failure: DiscoveryFailure) {
        let label = failure.localizedStatusLabel
        applyStatusSummary(.sdkLabel(localizationKey: label.localizationKey, fallback: label.fallbackDisplayName))
        diagnosticsStore.updateRecentFailure("\(DemoStrings.tr("discovery_failure")): \(discoveryFailureDetail(failure))")
        if failure.recoverable {
            setLocalizedLastActionResult {
                DemoStrings.tr("filtered_discovery_failed_switched")
            }
            return
        }
        setLocalizedErrorText {
            self.discoveryFailureDetail(failure)
        }
        recoveryAction = demoRecoveryAction(for: failure)
    }

    func discoveryFailureDetail(_ failure: DiscoveryFailure) -> String {
        let label = failure.localizedStatusLabel
        let message = "\(DemoStrings.sdk(label.localizationKey, fallback: label.fallbackDisplayName)): \(failure.message)"
        var metadata: [String] = []
        if let issue = failure.bleScanIssue {
            let issueLabel = issue.localizedLabel
            metadata.append("ble=\(DemoStrings.sdk(issueLabel.localizationKey, fallback: issueLabel.fallbackDisplayName))")
        }
        if let code = failure.platformErrorCode {
            metadata.append("raw=\(code)")
        }
        return metadata.isEmpty ? message : "\(message) [\(metadata.joined(separator: ", "))]"
    }

    func sessionFailureDetail(_ failure: SessionFailure) -> String {
        let label = failure.code.localizedStatusLabel
        let message = "\(DemoStrings.sdk(label.localizationKey, fallback: label.fallbackDisplayName)): \(failure.message)"
        var metadata: [String] = []
        if let issue = failure.bleTransportIssue {
            let issueLabel = issue.localizedLabel
            metadata.append("ble=\(DemoStrings.sdk(issueLabel.localizationKey, fallback: issueLabel.fallbackDisplayName))")
        }
        if let code = failure.platformErrorCode {
            metadata.append("raw=\(code)")
        }
        return metadata.isEmpty ? message : "\(message) [\(metadata.joined(separator: ", "))]"
    }
}
