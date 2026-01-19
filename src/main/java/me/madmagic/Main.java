package me.madmagic;


import me.madmagic.imagedetection.ImageDetection;
import me.madmagic.imagedetection.WhiteDieBlackPips;
import me.madmagic.webinterface.ServerInstance;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Scalar;

import java.util.List;


public class Main {

    public static void main(String[] args) throws Exception {
        ServerInstance.init();

//       Util.openCamera(mat -> {
//           runOnMat(mat);
//           waitKey(1);
//       }, 1);
    }

    private static final Mat vis = new Mat();

    public static void runOnMat(Mat cameraImg) {
        //long t1 = System.nanoTime();

        ImageDetection id = new WhiteDieBlackPips();

        cameraImg.copyTo(vis);
        List<Integer> dieVals = id.getDieScore(cameraImg, vis);

        int total = 0;
        for (Integer i : dieVals) {
            if (i == 6) i = 60;
            if (i == 1) i = 100;

            total += i;
        }

        Util.putText(vis, total, new Point(10, 300), 10, Scalar.RED);

        Util.imShow("vis", vis);

        //System.out.println((System.nanoTime() - t1) / 1000);
        System.out.println(Pointer.physicalBytes());
    }
}

