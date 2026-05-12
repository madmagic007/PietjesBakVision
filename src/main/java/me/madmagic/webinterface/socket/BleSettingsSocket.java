package me.madmagic.webinterface.socket;

import me.madmagic.ble.BleHandler;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.json.JSONObject;

public class BleSettingsSocket implements WebSocketListener {

    private volatile Session s;

    @Override
    public void onWebSocketText(String message) {
        if (s == null || !s.isOpen()) return;

        if (message.equals("request")) {
            SessionRegistry.send(s, BleHandler.getParamsForBroadcast());
            return;
        }

        if (message.equals("ping")) {
            SessionRegistry.send(s, "pong");
            return;
        }

        if (message.equals("start")) {
            BleHandler.startScanning();
        }

        if (message.equals("stop")) {
            BleHandler.stop();
        }

        try {
            JSONObject o = new JSONObject(message);
            String msgType = o.getString("type");

            if (msgType.equals("update")) {
                BleHandler.setParam(o.getString("key"), o.getInt("value"));
            } else if (msgType.equals("reset")) {
                //stub
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onWebSocketConnect(Session session) {
        s = session;
        SessionRegistry.addBleSettingSession(s);
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        SessionRegistry.removeSession(s);
        s = null;
    }
}
