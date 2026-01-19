package me.madmagic.detection.model.params;

import org.json.JSONObject;

import java.util.HashMap;

public class ModelParamCollection extends HashMap<String, ModelParam> {

    public void add(String name, int value, int min, int max) {
        put(name, new NumberParam(value, min, max));
    }

    public void add(String name, boolean value) {
        put(name, new BoolParam(value));
    }

    public int getNumber(String name) {
        return get(name).value;
    }

    public boolean getBool(String name) {
        return get(name).value == 1;
    }

    public JSONObject asJson() {
        JSONObject params = new JSONObject();

        forEach((k, v) -> params.put(k, v.asJson()));

        return params;
    }
}
