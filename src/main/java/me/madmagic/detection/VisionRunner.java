package me.madmagic.detection;

import me.madmagic.Main;
import me.madmagic.detection.model.VisionModel;
import me.madmagic.game.ThrowVal;
import me.madmagic.webinterface.socket.SessionRegistry;
import org.bytedeco.opencv.global.opencv_videoio;
import org.bytedeco.opencv.opencv_core.UMat;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.bytedeco.opencv.opencv_videoio.VideoWriter;
import org.json.JSONObject;

import java.util.List;

import static org.bytedeco.opencv.global.opencv_videoio.CAP_PROP_BUFFERSIZE;

public class VisionRunner {

    private static VisionModel model = VisionModel.models.get("WhiteDieBlackPips"); // default
    private static VideoCapture capture;
    public static volatile boolean stopCalled = true;
    private static volatile String newModel = "";
    private static ThrowVal throwVal;

    public static void start() {
        capture = new VideoCapture(Main.camIndex);
        capture.set(CAP_PROP_BUFFERSIZE, 1);
        capture.set(opencv_videoio.CAP_PROP_FPS, 5);
        capture.set(opencv_videoio.CAP_PROP_FOURCC, VideoWriter.fourcc((byte) 'M', (byte) 'J', (byte) 'P', (byte) 'G'));
        capture.set(opencv_videoio.CAP_PROP_FRAME_WIDTH, 3840);
        capture.set(opencv_videoio.CAP_PROP_FRAME_HEIGHT, 2160);

        if (!capture.isOpened()) {
            System.out.println("Can not open the cam");
            return;
        }

        stopCalled = false;
        UMat cameraFrame = new UMat();

        new Thread(() -> {
            while (!stopCalled) {
                if (!capture.grab()) continue; // flush stale frames
                if (!capture.retrieve(cameraFrame)) continue;

                run(cameraFrame);
            }

            cameraFrame.close();
            capture.close();
        }).start();
    }

    public static void stop() {
        stopCalled = true;
    }

    private static void run(UMat cameraFrame) {
        if (!newModel.isEmpty()) {
            VisionModel m = VisionModel.models.get(newModel);
            if (m != null) model = m;
            newModel = "";
        }

        List<Integer> scores = model.getDieScore(cameraFrame);

        if (scores.size() != 3) {
            SessionRegistry.broadcastThrowVal("0");
            return;
        }

        throwVal = ThrowVal.fromScores(scores);
        SessionRegistry.broadcastThrowVal(throwVal.scoreAsString());
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

    public static ThrowVal getCurrentThrow() {
        return throwVal;
    }

    public static boolean isRunning() {
        return !stopCalled;
    }
}
