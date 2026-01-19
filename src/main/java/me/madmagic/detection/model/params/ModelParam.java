package me.madmagic.detection.model.params;

import org.json.JSONObject;

public abstract class ModelParam {

    protected int value = 0;
    public final Type type;

    public ModelParam(Type type) {
        this.type = type;
    }

    public enum Type {
        BOOL, NUMBER
    }

    abstract JSONObject asJson();
}
