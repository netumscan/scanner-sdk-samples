import Foundation

@MainActor
extension AppViewModel {
    func setLocalizedLastActionResult(_ provider: @escaping () -> String) {
        recoveryAction = nil
        lastActionResultProvider = provider
        errorTextProvider = nil
        applyLocalizedFeedback {
            lastActionResult = provider()
            errorText = nil
        }
    }

    func setLocalizedErrorText(_ provider: @escaping () -> String) {
        recoveryAction = nil
        errorTextProvider = provider
        applyLocalizedFeedback {
            errorText = provider()
        }
    }

    func setLocalizedBlockingErrorText(_ provider: @escaping () -> String) {
        recoveryAction = nil
        errorTextProvider = provider
        applyLocalizedFeedback {
            let message = provider()
            errorText = message
            alertErrorText = message
        }
    }

    func refreshLocalizedFeedback() {
        guard lastActionResultProvider != nil || errorTextProvider != nil else { return }
        applyLocalizedFeedback {
            if let provider = lastActionResultProvider {
                lastActionResult = provider()
            }
            if let provider = errorTextProvider {
                errorText = provider()
            }
        }
    }

    func applyLocalizedFeedback(_ update: () -> Void) {
        isApplyingLocalizedFeedback = true
        update()
        isApplyingLocalizedFeedback = false
    }
}
