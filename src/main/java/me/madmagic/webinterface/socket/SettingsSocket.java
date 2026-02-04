package me.madmagic.webinterface.socket;

import me.madmagic.detection.VisionRunner;
import me.madmagic.detection.model.VisionModel;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.json.JSONArray;
import org.json.JSONObject;

public class SettingsSocket implements WebSocketListener {

    private volatile Session s;

    private JSONObject getGlobalMessage() {
        JSONArray modelsArr = new JSONArray();
        VisionModel.models.forEach((s, _) -> modelsArr.put(s));

        JSONObject models = new JSONObject()
                .put("active", VisionRunner.getActiveModelName())
                .put("list", modelsArr);

        return new JSONObject()
                .put("models", models)
                .put("params", VisionRunner.getParamList());
    }

    @Override
    public void onWebSocketText(String message) {
        if (s == null || !s.isOpen()) return;

        if (message.equals("request")) {
            SessionRegistry.send(s, getGlobalMessage());
            return;
        }

        if (message.equals("ping")) {
            SessionRegistry.send(s, "pong");
            return;
        }

        try {
            JSONObject o = new JSONObject(message);
            String msgType = o.getString("type");

            if (msgType.equals("update")) {
                VisionRunner.updateSetting(o.getString("model"), o.getString("key"), o.getInt("value"));
            } else if (msgType.equals("reset")) {
                //stub
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onWebSocketConnect(Session session) {
        s = session;
        SessionRegistry.addSettingSession(s);
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        SessionRegistry.removeSession(s);
        s = null;
    }
}
