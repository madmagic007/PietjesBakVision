package me.madmagic.detection.model;

import me.madmagic.detection.model.params.ModelParamCollection;
import org.bytedeco.opencv.opencv_core.UMat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class VisionModel {

    public final String name;

    public VisionModel(String name) {
        this.name = name;
    }

    public ModelParamCollection params = new ModelParamCollection() {{
        add("showContours", false);
        add("showContourArea", false);
        add("showContourCompactness", false);

        add("showPips", false);
        add("showPipArea", false);
        add("showPipCircularity", false);

        add("showDiceVal", false);
    }};

    public abstract List<Integer> getDieScore(UMat img);

    public static final Map<String, VisionModel> models = new HashMap<>() {{
        put("WhiteDieBlackPips", new WhiteDieBlackPips("WhiteDieBlackPips"));
    }};
}
