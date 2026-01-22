package me.madmagic.detection;

import me.madmagic.detection.model.VisionModel;
import org.bytedeco.opencv.global.opencv_videoio;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.bytedeco.opencv.opencv_videoio.VideoWriter;
import org.json.JSONObject;

public class VisionRunner {

    private static VisionModel model = VisionModel.models.get("WhiteDieBlackPips"); // default
    private static VideoCapture capture;
    private static boolean stopCalled = false;
    private static String newModel = "";

    public static void start() {
        capture = new VideoCapture(0);
        capture.set(opencv_videoio.CAP_PROP_FOURCC, VideoWriter.fourcc((byte) 'M', (byte) 'J', (byte) 'P', (byte) 'G'));
        capture.set(opencv_videoio.CAP_PROP_FRAME_WIDTH, 3840);
        capture.set(opencv_videoio.CAP_PROP_FRAME_HEIGHT, 2160);

        if (!capture.isOpened()) {
            System.out.println("Can not open the cam");
            return;
        }

        Mat cameraFrame = new Mat();

        new Thread(() -> {
            while (capture.read(cameraFrame)) {
                if (stopCalled) break;

                run(cameraFrame);
            }
        }).start();
    }

    public static void stop() {
        stopCalled = true;
    }

    private static void run(Mat cameraFrame) {
        if (!newModel.isEmpty()) {
            VisionModel m = VisionModel.models.get(newModel);
            if (m != null) model = m;
            newModel = "";
        }

        //List<Integer> scores = model.getDieScore(cameraFrame, vis);
    }

    public static void updateModel(String modelName) {
        newModel = modelName;
    }

    public static String getActiveModelName() {
        return !newModel.isEmpty() ? newModel : model.name;
    }

    public static JSONObject getParamList() {
        return VisionModel.models.get(getActiveModelName()).params.asJson();
    }

    public static void updateSetting(String modelName, String key, int value) {
        if (!modelName.equals(getActiveModelName())) return;

        VisionModel.models.get(getActiveModelName()).params.update(key, value);
    }
}
