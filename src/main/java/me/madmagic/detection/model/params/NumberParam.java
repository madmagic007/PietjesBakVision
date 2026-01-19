package me.madmagic.detection.model.params;

import org.json.JSONObject;

public class NumberParam extends ModelParam {

    private final int min, max;

    public NumberParam(int value, int min, int max) {
        super(Type.NUMBER);

        this.value = value;
        this.min = min;
        this.max = max;
    }



    public JSONObject asJson() {
        return new JSONObject()
                .put("value", value)
                .put("min", min)
                .put("max", max);
    }
}
