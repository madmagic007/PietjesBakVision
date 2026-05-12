package me.madmagic.params;

import org.json.JSONObject;

public abstract class Param {

    protected int value = 0;
    public final Type type;

    public Param(Type type) {
        this.type = type;
    }

    public enum Type {
        BOOL, NUMBER
    }

    abstract JSONObject asJson();
}
