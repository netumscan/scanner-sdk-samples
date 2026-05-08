import SwiftUI
import ScannerSDK

struct ContentView: View {
    @ObservedObject var model: QuickStartViewModel

    var body: some View {
        NavigationStack {
            List {
                Section("Status") {
                    Text(model.status)
                    if let name = model.connectedDeviceName {
                        Text("Connected: \(name)")
                    }
                    Button("Start BLE Discovery") {
                        model.startDiscovery()
                    }
                    Button("Disconnect") {
                        model.disconnect()
                    }
                    .disabled(model.connectedDeviceName == nil)
                }

                Section("Devices") {
                    ForEach(model.devices, id: \.deviceId) { device in
                        Button {
                            model.connect(device)
                        } label: {
                            VStack(alignment: .leading) {
                                Text(device.name.isEmpty ? "(unnamed)" : device.name)
                                Text("\(device.deviceId)  RSSI \(device.rssi.map(String.init) ?? "-")")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }

                Section("Scans") {
                    ForEach(model.scans.prefix(20), id: \.self) { text in
                        Text(text)
                    }
                }
            }
            .navigationTitle("Scanner SDK")
        }
    }
}

