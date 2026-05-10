package me.madmagic.ble;

import com.welie.blessed.*;

import java.util.UUID;

public class BleHandler {

    private static BluetoothPeripheral connectedPeripheral = null;
    private static boolean connected = false;

    private static final String SERVICE_UUID = "12345678-1234-1234-1234-123456789012";
    private static final String CHARACTERISTIC_UUID = "87654321-4321-4321-4321-210987654321";

    private static final BluetoothPeripheralCallback peripheralCallback = new BluetoothPeripheralCallback() {
        @Override
        public void onCharacteristicUpdate(BluetoothPeripheral peripheral, byte[] value, BluetoothGattCharacteristic characteristic, BluetoothCommandStatus status) {
            byte b = value[0];
            int die1 = (b >> 5) & 0x7;
            int die2 = (b >> 2) & 0x7;
            int die3 = b & 0x3;
            System.out.println("Dice: " + die1 + ", " + die2 + ", " + die3);
        }
    };

    private static final BluetoothCentralManagerCallback bluetoothCentralManagerCallback = new BluetoothCentralManagerCallback() {
        @Override
        public void onDiscoveredPeripheral(BluetoothPeripheral peripheral, ScanResult scanResult) {
            if ("ESP32-C6".equals(peripheral.getName())) {
                central.stopScan();
                central.connectPeripheral(peripheral, peripheralCallback);
            }
        }

        @Override
        public void onConnectedPeripheral(BluetoothPeripheral peripheral) {
            connected = true;
            connectedPeripheral = peripheral;
            setupDataStream(peripheral);
        }

        @Override
        public void onDisconnectedPeripheral(BluetoothPeripheral peripheral, BluetoothCommandStatus status) {
            connected = false;
            connectedPeripheral = null;
            central.scanForPeripherals();
        }
    };

    private static final BluetoothCentralManager central = new BluetoothCentralManager(bluetoothCentralManagerCallback);

    private static void setupDataStream(BluetoothPeripheral peripheral) {
        BluetoothGattService service = peripheral.getService(UUID.fromString(SERVICE_UUID));

        if (service != null) {
            BluetoothGattCharacteristic chr = service.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID));

            if (chr != null) {
                peripheral.setNotify(chr, true);
            }
        }
    }

    public static void init() {
        central.scanForPeripherals();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("shutdown");
            if (connectedPeripheral != null) {
                System.out.println("cancelling");
                connectedPeripheral.cancelConnection();
            }
            central.shutdown();
        }));
    }
}
