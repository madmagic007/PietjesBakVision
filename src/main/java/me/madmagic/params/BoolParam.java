package me.madmagic.params;

import org.json.JSONObject;

public class BoolParam extends Param {

    public BoolParam(boolean defaultValue) {
        super(Type.BOOL);

        value = defaultValue ? 1 : 0;
    }

    public JSONObject asJson() {
        return new JSONObject()
                .put("value", value);
    }
}
