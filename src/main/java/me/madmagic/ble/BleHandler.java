package me.madmagic.ble;

import com.welie.blessed.*;
import me.madmagic.game.GameInstance;
import me.madmagic.game.ThrowVal;
import me.madmagic.params.ParamCollection;
import me.madmagic.webinterface.socket.SessionRegistry;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BleHandler {

    private static ParamCollection params = new ParamCollection() {{
        add("brightness", 20, 1, 100);
        add("battery", 100, 0, 100); // yes lmao this is how im gonna do it
    }};

    private static BluetoothPeripheral connectedPeripheral = null;
    public static boolean connected = false;
    public static boolean running = false;

    private static final String SERVICE_UUID = "12345678-1234-1234-1234-123456789012";

    private static final String RX_DIE_VALUE = "11111111-1111-1111-1111-11111111";
    private static final String RX_PARAMS = "22222222-2222-2222-2222-22222222";
    private static final String TX_PARAMS = "33333333-3333-3333-3333-33333333"; // I HATE UUIDs

    private static final BluetoothPeripheralCallback peripheralCallback = new BluetoothPeripheralCallback() {
        @Override
        public void onCharacteristicUpdate(BluetoothPeripheral peripheral, byte[] value, BluetoothGattCharacteristic characteristic, BluetoothCommandStatus status) {
            String uuid = characteristic.getUuid().toString();

            switch (uuid) {
                case RX_DIE_VALUE -> {
                    byte b = value[0];
                    int die1 = (b >> 5) & 0x7;
                    int die2 = (b >> 2) & 0x7;
                    int die3 = b & 0x3;

                    List<Integer> scores = Arrays.asList(die1, die2, die3);
                    ThrowVal throwVal = ThrowVal.fromScores(scores);
                    GameInstance.setThrow(throwVal);
                }
                case RX_PARAMS -> {
                    String[] split = new String(value).split("\\|");
                    int numericValue = Integer.parseInt(split[1]);

                    setParam(split[0], numericValue);
                }
            }
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
        if (service == null) return; // soo sad
        
        BluetoothGattCharacteristic chrDieVal = service.getCharacteristic(UUID.fromString(RX_DIE_VALUE));
        BluetoothGattCharacteristic chrParams = service.getCharacteristic(UUID.fromString(RX_PARAMS));

        if (chrDieVal != null) peripheral.setNotify(chrDieVal, true);
        if (chrParams != null) peripheral.setNotify(chrParams, true);
    }

    public static void startScanning() {
        running = true;
        central.scanForPeripherals();

        Runtime.getRuntime().addShutdownHook(new Thread(BleHandler::stop));
    }

    public static void stop() {
        if (connectedPeripheral != null) {
            connectedPeripheral.cancelConnection();
        }
        central.shutdown();
        running = false;
        connected = false;
    }

    private static void sendData(byte[] data) {
        if (connectedPeripheral != null && connected) {
            connectedPeripheral.writeCharacteristic(
                    UUID.fromString(SERVICE_UUID),
                    UUID.fromString(TX_PARAMS),
                    data,
                    BluetoothGattCharacteristic.WriteType.WITHOUT_RESPONSE
            );
        }
    }

    public static JSONObject getParamsForBroadcast() {
        JSONObject o = params.asJson();
        o.remove("battery");

        return new JSONObject().put("params", o);
    }

    public static void setParam(String name, int value) {
        if (name.equals("battery")) return;

        params.update(name, value);
        String str = String.format("%s|%d", name, value);
        sendData(str.getBytes());

        SessionRegistry.broadcastBleSettings(getParamsForBroadcast());
    }
}
