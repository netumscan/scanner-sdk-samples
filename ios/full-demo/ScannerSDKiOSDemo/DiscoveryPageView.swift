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
        List {
            Section(DemoStrings.tr("discovery_controls")) {
                DiscoveryOverviewGrid(
                    items: [
                        (DemoStrings.tr("status"), viewModel.statusSummary),
                        (DemoStrings.tr("selected_device"), viewModel.deviceSummary),
                        (DemoStrings.tr("transport_mode"), DemoStrings.tr("ios_ble_only")),
                        (DemoStrings.tr("selected_model"), viewModel.selectedModelSummary),
                        (DemoStrings.tr("discovered_devices"), String(viewModel.displayDevices.count)),
                        (DemoStrings.tr("diagnostics"), viewModel.diagnosticsSummary),
                    ]
                )

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

                Picker(
                    DemoStrings.tr("selected_model"),
                    selection: Binding(
                        get: { viewModel.selectedModelId },
                        set: { viewModel.applySelectedModel($0) }
                    )
                ) {
                    ForEach(demoSupportedModels, id: \.self) { modelId in
                        Text(displayModelLabel(modelId)).tag(modelId)
                    }
                }
                .pickerStyle(.menu)
                .disabled(!viewModel.canChangeDiscoveryTarget)
                .accessibilityIdentifier(DemoAccessibility.selectedModelPicker)

                LabeledContent(
                    DemoStrings.tr("protocol_mode"),
                    value: viewModel.protocolChannelKindSummary
                )
                Text(DemoStrings.tr("ios_ble_only_hint"))
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                if !viewModel.canChangeDiscoveryTarget {
                    Text(DemoStrings.tr("disconnect_before_new_discovery"))
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }

                HStack(spacing: 10) {
                    Button(viewModel.isInitialized ? DemoStrings.tr("sdk_ready") : DemoStrings.tr("init_sdk")) {
                        viewModel.initializeSdk()
                    }
                    .buttonStyle(.borderedProminent)
                    .frame(maxWidth: .infinity)
                    .disabled(viewModel.isInitialized)
                    .accessibilityIdentifier(DemoAccessibility.initSdkButton)
                }

                HStack(spacing: 10) {
                    Button(DemoStrings.tr("start_discovery")) {
                        viewModel.startDiscovery()
                    }
                    .buttonStyle(.borderedProminent)
                    .frame(maxWidth: .infinity)
                    .disabled(!viewModel.isInitialized || viewModel.isDiscovering || !viewModel.canChangeDiscoveryTarget)
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

                DemoScenarioPresetView()
            }

            Section(DemoStrings.tr("devices")) {
                if viewModel.displayDevices.isEmpty {
                    Text(DemoStrings.tr("no_devices"))
                        .foregroundStyle(.secondary)
                        .accessibilityIdentifier(DemoAccessibility.noDevicesText)
                } else {
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
                            .disabled(viewModel.isExecuting || !viewModel.canInitiateConnection)

                            if viewModel.hasActiveSession && viewModel.selectedDeviceID != device.deviceId {
                                Text(DemoStrings.tr("disconnect_before_connecting_another_device"))
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            }
                        }
                        .padding(.vertical, 4)
                    }
                }
            }
        }
        .accessibilityIdentifier(DemoAccessibility.discoveryList)
        .navigationTitle(DemoStrings.tr("scanner_discovery"))
        .toolbar {
            DemoLanguageToolbarMenu(
                selectedLanguage: selectedLanguage,
                onLanguageChange: onLanguageChange
            )
            DemoAppLogsToolbarButton(onOpenLogs: onOpenLogs)
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

private struct DiscoveryOverviewGrid: View {
    let items: [(String, String)]

    private let columns = [
        GridItem(.flexible(), spacing: 10),
        GridItem(.flexible(), spacing: 10),
    ]

    var body: some View {
        LazyVGrid(columns: columns, spacing: 10) {
            ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                DiscoveryOverviewCard(title: item.0, value: item.1)
            }
        }
    }
}

private struct DiscoveryOverviewCard: View {
    let title: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
            Text(value)
                .font(.subheadline.weight(.medium))
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.secondary.opacity(0.08), in: RoundedRectangle(cornerRadius: 12))
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
