import SwiftUI

struct ContentView: View {
    @ObservedObject var viewModel: QuickStartViewModel

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 16) {
                    statusSection
                    discoverySection
                    devicesSection
                    quickActionsSection
                    summarySection(title: "Device Info", value: viewModel.deviceInfo)
                    summarySection(title: "Battery", value: viewModel.battery)
                    if !viewModel.lastAction.isEmpty {
                        Text(viewModel.lastAction)
                            .font(.callout)
                            .foregroundStyle(.secondary)
                    }
                    scansSection
                }
                .padding()
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle("Scanner SDK Quick Start")
            .navigationBarTitleDisplayMode(.inline)
        }
        .onDisappear {
            viewModel.shutdown()
        }
    }

    private var statusSection: some View {
        GroupBox {
            VStack(alignment: .leading, spacing: 8) {
                LabeledContent("SDK", value: viewModel.sdkVersion.isEmpty ? "…" : viewModel.sdkVersion)
                LabeledContent("Status", value: viewModel.status)
                if let name = viewModel.connectedDeviceName {
                    LabeledContent("Connected", value: name)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private var discoverySection: some View {
        GroupBox("Discovery") {
            VStack(alignment: .leading, spacing: 12) {
                Picker("Target model", selection: $viewModel.selectedModelKey) {
                    ForEach(viewModel.models, id: \.modelKey) { model in
                        Text("\(model.modelName) (\(model.modelKey))")
                            .tag(model.modelKey)
                    }
                }
                .disabled(viewModel.isDiscovering || viewModel.isConnecting || viewModel.isReady)

                HStack {
                    Button(viewModel.canRetry ? "Retry Discovery" : "Start Discovery") {
                        viewModel.startDiscovery()
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(
                        viewModel.selectedModelKey.isEmpty ||
                        viewModel.isDiscovering ||
                        viewModel.isConnecting ||
                        viewModel.isReady
                    )

                    Button("Stop Discovery") {
                        viewModel.stopDiscovery()
                    }
                    .buttonStyle(.bordered)
                    .disabled(!viewModel.isDiscovering)

                    Button("Disconnect", role: .destructive) {
                        viewModel.disconnect()
                    }
                    .buttonStyle(.bordered)
                    .disabled(!viewModel.isReady && !viewModel.isConnecting)
                }
            }
        }
    }

    private var devicesSection: some View {
        GroupBox("Devices") {
            VStack(spacing: 0) {
                if viewModel.devices.isEmpty {
                    Text("No matching BLE scanners found yet.")
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(viewModel.devices) { device in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(device.displayName)
                                    .font(.headline)
                                Text("\(device.modelKey.isEmpty ? "Model unresolved" : device.modelKey) · RSSI \(device.rssi.map(String.init) ?? "—")")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            Button("Connect") {
                                viewModel.connect(device)
                            }
                            .disabled(viewModel.isConnecting || viewModel.isReady)
                        }
                        .padding(.vertical, 10)
                        if device.id != viewModel.devices.last?.id {
                            Divider()
                        }
                    }
                }
            }
        }
    }

    private var quickActionsSection: some View {
        GroupBox("Quick Actions") {
            HStack {
                Button("Trigger Scan") {
                    viewModel.triggerScan()
                }
                .buttonStyle(.borderedProminent)
                .disabled(
                    !viewModel.isReady ||
                    !viewModel.operationSupport.triggerScan ||
                    viewModel.isRunningCommand
                )

                Button("Refresh Device Info") {
                    viewModel.refreshInfo()
                }
                .buttonStyle(.bordered)
                .disabled(
                    !viewModel.isReady ||
                    !viewModel.operationSupport.refreshInfo ||
                    viewModel.isRunningCommand
                )

                Button("Read Battery") {
                    viewModel.readBattery()
                }
                .buttonStyle(.bordered)
                .disabled(
                    !viewModel.isReady ||
                    !viewModel.operationSupport.battery ||
                    viewModel.isRunningCommand
                )
            }
        }
    }

    private func summarySection(title: String, value: String) -> some View {
        GroupBox(title) {
            Text(value)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private var scansSection: some View {
        GroupBox("Recent Scans") {
            VStack(alignment: .leading, spacing: 0) {
                if viewModel.scans.isEmpty {
                    Text("No scans received.")
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(viewModel.scans) { scan in
                        VStack(alignment: .leading, spacing: 4) {
                            Text(scan.text.isEmpty ? "(empty scan)" : scan.text)
                            Text("Type \(scan.barcodeType) · \(scan.text.count) characters")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        .padding(.vertical, 8)
                        if scan.id != viewModel.scans.last?.id {
                            Divider()
                        }
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}
