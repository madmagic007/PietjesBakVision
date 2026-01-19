package me.madmagic.detection.model;

import me.madmagic.detection.model.params.ModelParamCollection;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.bytedeco.opencv.global.opencv_core.inRange;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2HSV;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;

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

    public void doHSVFiltering(Mat src, int v1, int v2, int v3, int v4, int v5, int v6) {
        cvtColor(src, src, COLOR_BGR2HSV);
        Scalar lower = new Scalar(v1, v2, v3, 0);
        Scalar upper = new Scalar(v4, v5, v6, 0);

        Mat lowerMat = new Mat(lower);
        Mat upperMat = new Mat(upper);
        inRange(src, lowerMat, upperMat, src);

        lower.close();
        upper.close();
        lowerMat.close();
        upperMat.close();
    }

    public abstract List<Integer> getDieScore(Mat img, Mat vis);

    public static final Map<String, VisionModel> models = new HashMap<>() {{
        put("WhiteDieBlackPips", new WhiteDieBlackPips("WhiteDieBlackPips"));
    }};
}
