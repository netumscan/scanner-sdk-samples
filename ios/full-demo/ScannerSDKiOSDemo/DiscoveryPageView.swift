import SwiftUI
import ScannerSDK
import UIKit

struct DiscoveryPageView: View {
    @Environment(\.openURL) private var openURL

    @ObservedObject var viewModel: AppViewModel
    let selectedLanguage: DemoLanguage
    let onLanguageChange: (DemoLanguage) -> Void
    let onConnect: (DiscoveredDevice) -> Void
    let onOpenConsole: () -> Void
    let onOpenLogs: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                DiscoverySectionCard(title: DemoStrings.tr("discovery_controls")) {
                    discoveryControlsContent
                }

                DiscoverySectionCard(title: DemoStrings.tr("devices")) {
                    devicesContent
                }
            }
            .padding(16)
        }
        .accessibilityIdentifier(DemoAccessibility.discoveryList)
        .navigationTitle(DemoStrings.tr("scanner_discovery"))
        .toolbar {
            DemoLanguageToolbarMenu(
                selectedLanguage: selectedLanguage,
                onLanguageChange: onLanguageChange
            )
            DemoAppLogsToolbarButton(onOpenLogs: onOpenLogs, isDisabled: !viewModel.canOpenLogs)
        }
    }

    @ViewBuilder
    private var discoveryControlsContent: some View {
        if let lastAction = viewModel.lastActionResult {
            DiscoveryFeedbackBanner(
                text: lastAction,
                background: Color.accentColor.opacity(0.12),
                foreground: .primary
            )
            .accessibilityIdentifier(DemoAccessibility.discoveryLastActionBanner)
        }
        if let errorText = viewModel.errorText {
            DiscoveryFeedbackBanner(
                text: viewModel.recoveryAction.map { "\(errorText)\n\($0.hint)" } ?? errorText,
                background: Color.red.opacity(0.12),
                foreground: .red,
                actionTitle: viewModel.recoveryAction?.title,
                action: {
                    if let action = viewModel.recoveryAction {
                        performRecoveryAction(action)
                    }
                }
            )
            .accessibilityIdentifier(DemoAccessibility.discoveryErrorBanner)
        }
        if viewModel.isConnecting {
            DiscoveryFeedbackBanner(
                text: DemoStrings.tr(
                    "connecting_keep_current_screen",
                    fallback: "Connecting. Keep this screen open until the console appears."
                ),
                background: Color.orange.opacity(0.12),
                foreground: .orange
            )
        }

        Button(viewModel.isInitialized ? DemoStrings.tr("sdk_ready") : DemoStrings.tr("init_sdk")) {
            viewModel.initializeSdk()
        }
        .buttonStyle(.borderedProminent)
        .frame(maxWidth: .infinity)
        .disabled(viewModel.isInitialized)
        .accessibilityIdentifier(DemoAccessibility.initSdkButton)

        Menu {
            ForEach(viewModel.supportedModels) { model in
                Button {
                    viewModel.applySelectedModel(model.modelKey)
                } label: {
                    VStack(alignment: .leading) {
                        Text(model.primaryLabel)
                        if !model.secondaryLabel.isEmpty {
                            Text(model.secondaryLabel)
                        }
                    }
                }
            }
        } label: {
            HStack(spacing: 10) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(DemoStrings.tr("selected_model"))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text(selectedModelMenuLabel)
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(.primary)
                }
                Spacer()
                Image(systemName: "chevron.up.chevron.down")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.secondary)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 10))
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(Color.secondary.opacity(0.18), lineWidth: 1)
            )
        }
        .disabled(
            !viewModel.canChangeDiscoveryTarget ||
                viewModel.isLoadingSupportedModels ||
                viewModel.supportedModels.isEmpty
        )
        .accessibilityIdentifier(DemoAccessibility.selectedModelPicker)

        supportedModelsStatus

        if !viewModel.canChangeDiscoveryTarget {
            Text(viewModel.isDiscovering
                ? DemoStrings.tr(
                    "stop_discovery_before_changing_model",
                    fallback: "Stop discovery before changing model."
                )
                : DemoStrings.tr("disconnect_before_new_discovery"))
                .font(.footnote)
                .foregroundStyle(.secondary)
        }

        HStack(spacing: 10) {
            Button(DemoStrings.tr("start_discovery")) {
                viewModel.startDiscovery()
            }
            .buttonStyle(.borderedProminent)
            .frame(maxWidth: .infinity)
            .disabled(
                !viewModel.isInitialized ||
                    viewModel.isDiscovering ||
                    !viewModel.canChangeDiscoveryTarget ||
                    !viewModel.hasValidSelectedModel ||
                    viewModel.isLoadingSupportedModels ||
                    viewModel.supportedModelsError != nil
            )
            .accessibilityIdentifier(DemoAccessibility.startDiscoveryButton)

            Button(DemoStrings.tr("stop_discovery")) {
                viewModel.stopDiscovery()
            }
            .buttonStyle(.bordered)
            .frame(maxWidth: .infinity)
            .disabled(!viewModel.isDiscovering)
            .accessibilityIdentifier(DemoAccessibility.stopDiscoveryButton)
        }

        HStack(spacing: 10) {
            Button(DemoStrings.tr("disconnect_connection")) {
                viewModel.disconnect()
            }
            .buttonStyle(.bordered)
            .frame(maxWidth: .infinity)
            .disabled(viewModel.isExecuting || viewModel.isConnecting || !viewModel.hasActiveSession)
            .accessibilityIdentifier(DemoAccessibility.discoveryDisconnectButton)

            Button(DemoStrings.tr("open_console")) {
                onOpenConsole()
            }
            .buttonStyle(.bordered)
            .frame(maxWidth: .infinity)
            .disabled(!viewModel.hasReadySession)
            .accessibilityIdentifier(DemoAccessibility.openConsoleButton)
        }

    }

    private var selectedModelMenuLabel: String {
        if viewModel.isLoadingSupportedModels {
            return DemoStrings.tr("loading_supported_models", fallback: "Loading supported models…")
        }
        return viewModel.supportedModels.first {
            $0.modelKey.caseInsensitiveCompare(viewModel.selectedModelKey) == .orderedSame
        }?.primaryLabel ?? DemoStrings.tr(
            "no_supported_models_for_transport",
            fallback: "No supported models"
        )
    }

    @ViewBuilder
    private var supportedModelsStatus: some View {
        if viewModel.isLoadingSupportedModels {
            HStack(spacing: 8) {
                ProgressView()
                Text(DemoStrings.tr("loading_supported_models", fallback: "Loading supported models…"))
            }
            .font(.footnote)
            .foregroundStyle(.secondary)
            .accessibilityIdentifier(DemoAccessibility.supportedModelsStatus)
        } else if let error = viewModel.supportedModelsError {
            HStack(alignment: .center, spacing: 8) {
                Text(error)
                    .font(.footnote)
                    .foregroundStyle(.red)
                Spacer()
                Button(DemoStrings.tr("retry", fallback: "Retry")) {
                    viewModel.reloadSupportedModels()
                }
                .buttonStyle(.bordered)
                .accessibilityIdentifier(DemoAccessibility.supportedModelsRetryButton)
            }
            .accessibilityIdentifier(DemoAccessibility.supportedModelsStatus)
        } else if viewModel.supportedModels.isEmpty {
            Text(DemoStrings.tr(
                "no_supported_models_for_transport",
                fallback: "No supported models are available for BLE GATT."
            ))
            .font(.footnote)
            .foregroundStyle(.secondary)
            .accessibilityIdentifier(DemoAccessibility.supportedModelsStatus)
        } else {
            Text(DemoStrings.format(
                "supported_models_loaded_summary",
                fallback: "%d supported models loaded",
                viewModel.supportedModels.count
            ))
            .font(.footnote)
            .foregroundStyle(.secondary)
            .accessibilityIdentifier(DemoAccessibility.supportedModelsStatus)
        }
    }

    @ViewBuilder
    private var devicesContent: some View {
        if viewModel.displayDevices.isEmpty {
            Text(DemoStrings.tr("no_devices"))
                .foregroundStyle(.secondary)
                .accessibilityIdentifier(DemoAccessibility.noDevicesText)
        } else {
            VStack(alignment: .leading, spacing: 10) {
                ForEach(viewModel.displayDevices, id: \.deviceId) { device in
                    VStack(alignment: .leading, spacing: 6) {
                        HStack(alignment: .top, spacing: 10) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(device.name.isEmpty ? DemoStrings.unnamedDevice : device.name)
                                    .font(.headline)
                                Text(device.deviceId)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }

                            Spacer()

                            Text(formatDiscoverySignalSummary(device))
                                .font(.caption.weight(.semibold))
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .background(Color.accentColor.opacity(0.12), in: Capsule())
                                .foregroundStyle(Color.accentColor)
                        }

                        Text(formatDiscoveryDeviceSummary(device))
                            .font(.caption)
                            .foregroundStyle(.secondary)

                        Button(
                            viewModel.selectedDeviceID == device.deviceId && viewModel.isConnecting
                                ? DemoStrings.tr("connecting")
                                : DemoStrings.tr("connect_open_console")
                        ) {
                            onConnect(device)
                        }
                        .buttonStyle(.bordered)
                        .disabled(viewModel.isExecuting || !viewModel.canInitiateConnection || device.connectable == false)

                        if viewModel.hasActiveSession && viewModel.selectedDeviceID != device.deviceId {
                            Text(DemoStrings.tr("disconnect_before_connecting_another_device"))
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                    .padding(12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
                }
            }
        }
    }

    private func performRecoveryAction(_ action: DemoRecoveryAction) {
        switch action {
        case .enableBluetooth, .allowBluetoothAccess, .recoverBleScanner, .recoverBleConnection:
            if let url = URL(string: UIApplication.openSettingsURLString) {
                openURL(url)
            }
        }
    }
}

private struct DiscoverySectionCard<Content: View>: View {
    let title: String
    let content: Content

    init(title: String, @ViewBuilder content: () -> Content) {
        self.title = title
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.subheadline.weight(.semibold))
            content
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.secondary.opacity(0.08), in: RoundedRectangle(cornerRadius: 14))
    }
}

private struct DiscoveryFeedbackBanner: View {
    let text: String
    let background: Color
    let foreground: Color
    var actionTitle: String?
    var action: (() -> Void)?

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(text)
                .font(.subheadline)
                .frame(maxWidth: .infinity, alignment: .leading)

            if let actionTitle, let action {
                Button(actionTitle) {
                    action()
                }
                .buttonStyle(.borderedProminent)
                .tint(.accentColor)
                .accessibilityIdentifier(DemoAccessibility.discoveryRecoveryActionButton)
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(background, in: RoundedRectangle(cornerRadius: 12))
        .foregroundStyle(foreground)
    }
}
