package me.madmagic.detection.model;

import me.madmagic.Util;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.opencv.opencv_core.*;

import java.util.*;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class WhiteDieBlackPips extends VisionModel {

    public WhiteDieBlackPips(String name) {
        super(name);

        params.add("blur", 19, 1, 30);

        params.add("contourMinCanny", 0, 0, 255);
        params.add("contourMaxCanny", 60, 0, 255);
        params.add("contourKernelSize", 7, 1, 20);
        params.add("contourMinArea", 15000, 0, 100000);
        params.add("contourFrames", 10, 0, 20);

        params.add("blackHatKernelSize", 30, 1, 50);
        params.add("blackHatThresh", 100, 0, 255);

        params.add("pipMinArea", 200, 0, 10000);
        params.add("pipMaxArea", 500, 0, 10000);
        params.add("pipMinCircularity", 70, 0, 100);
        params.add("pipMaxDist", 70, 0, 200);
    }

    private final Mat blur = new Mat();
    private final Mat m1 = new Mat();
    private final Mat blackHat = new Mat();
    private final Deque<Mat> contourDequeue = new ArrayDeque<>();
    private final MatVector dieContours = new MatVector();
    private final MatVector pipContours = new MatVector();
    private final MatVector valiDieContours = new MatVector();
    private final MatVector validPipContours = new MatVector();

    private void getDieContours() {
        Canny(blur, m1, params.getNumber("contourMinCanny"), params.getNumber("contourMaxCanny"));

        int s = params.getNumber("contourKernelSize");
        Size contourKSize = new Size(s, s);
        Mat contourKernel = getStructuringElement(MORPH_RECT, contourKSize);

        Util.dilateIt(m1, contourKernel, 4);
        Util.morphExIt(m1, contourKernel, MORPH_CLOSE, 2);
        Util.morphExIt(m1, contourKernel, MORPH_OPEN, 2);

        Util.contourFill(m1);

        contourDequeue.addLast(m1.clone());
        if (contourDequeue.size() > params.getNumber("contourFrames")) {
            contourDequeue.removeFirst().close();
        }
        for (Mat e : contourDequeue) {
            bitwise_or(m1, e, m1);
        }

        Util.clearMatVector(dieContours);
        findContours(m1, dieContours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

        contourKSize.close();
        contourKernel.close();
    }

    private void getPipContours() {
        int s = params.getNumber("blackHatKernelSize");
        Mat bhKernel = getStructuringElement(MORPH_ELLIPSE, new Size(s, s));

        morphologyEx(blur, blackHat, MORPH_BLACKHAT, bhKernel);
        normalize(blackHat, blackHat, 0, 255, NORM_MINMAX, -1, null);
        threshold(blackHat, blackHat, params.getNumber("blackHatThresh"), 255, THRESH_BINARY);

        Util.clearMatVector(pipContours);
        findContours(blackHat, pipContours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

        bhKernel.close();
    }

    private List<Point> filterContoursAndGetPipCenters(Mat vis) {
        List<Mat> validPips = new ArrayList<>();
        List<Mat> validDice = new ArrayList<>();
        List<Mat> finalPips = new ArrayList<>();
        List<Point> pipCenters = new ArrayList<>();

        for (int i = 0; i < pipContours.get().length; i++) {
            Mat pipMat = pipContours.get(i);

            double pipArea = contourArea(pipMat);
            double peri = arcLength(pipMat, true);
            if (peri == 0) continue;
            double circularity = 4 * Math.PI * pipArea / (peri * peri);

            if (pipArea < params.getNumber("pipMinArea") || pipArea > params.getNumber("pipMaxArea")) continue;
            if (circularity < (params.getNumber("pipMinCircularity") / 100f)) continue;

            if (params.getBool("showPipArea")) {
                Util.putText(vis, pipArea, pipMat,2, Scalar.RED);
            }

            if (params.getBool("showPipCircularity")) {
                Util.putText(vis, circularity, pipMat,2, Scalar.RED);
            }

            validPips.add(pipMat);
        }

        for (int i = 0; i < dieContours.size(); i++) {
            Mat die = dieContours.get(i);

            double area = contourArea(die);
            if (area < params.getNumber("contourMinArea")) continue;

            boolean hasValidPip = false;
            for (Mat pip : validPips) {
                if (Util.isContourInsideOther(die, pip)) {
                    hasValidPip = true;
                    break;
                }
            }

            if (!hasValidPip) continue;

            validDice.add(die);

            if (params.getBool("showContourArea")) {
                Util.putText(vis, area, die, 3, Scalar.RED);
            }
        }

        for (Mat pip : validPips) {
            for (Mat die : validDice) {
                if (Util.isContourInsideOther(die, pip)) {
                    finalPips.add(pip);
                    pipCenters.add(Util.getXY(pip));
                    break;
                }
            }
        }

        valiDieContours.resize(validDice.size());
        validPipContours.resize(finalPips.size());

        for (int i = 0; i < validDice.size(); i++) {
            valiDieContours.put(i, validDice.get(i));
        }

        for (int i = 0; i < finalPips.size(); i++) {
            validPipContours.put(i, finalPips.get(i));
        }

        return pipCenters;
    }

    private List<Integer> clusterAndCountPips(Mat vis, List<Point> pipCenters) {
        List<Integer> values = new ArrayList<>();

        double maxDist = params.getNumber("pipMaxDist");

        List<List<Point>> clusters = new ArrayList<>();
        boolean[] used = new boolean[pipCenters.size()];

        for (int i = 0; i < pipCenters.size(); i++) {
            if (used[i]) continue;

            List<Point> cluster = new ArrayList<>();
            Stack<Integer> stack = new Stack<>();
            stack.push(i);
            used[i] = true;
            cluster.add(pipCenters.get(i));

            while (!stack.isEmpty()) {
                int idx = stack.pop();
                Point pi = pipCenters.get(idx);

                for (int j = 0; j < pipCenters.size(); j++) {
                    if (used[j]) continue;

                    Point pj = pipCenters.get(j);
                    double dx = pi.x() - pj.x();
                    double dy = pi.y() - pj.y();
                    double distance = Math.sqrt(dx * dx + dy * dy);

                    if (distance < maxDist) {
                        used[j] = true;
                        stack.push(j);
                        cluster.add(pj);
                    }
                }
            }

            clusters.add(cluster);
        }

        clusters.forEach(l -> {
            values.add(l.size());

            if (params.getNumber("showDiceVal") == 1) {
                int sumX = 0;
                int sumY = 0;

                for (Point p : l) {
                    sumX += p.x();
                    sumY += p.y();
                }

                int cx = sumX / l.size();
                int cy = sumY / l.size();

                Util.putText(vis, l.size(), new Point(cx, cy), 3, Scalar.RED);
            }
        });

        return values;
    }


    @Override
    public List<Integer> getDieScore(Mat img, Mat vis) {
        cvtColor(img, blur, COLOR_BGR2GRAY);

        Size blurSize = Util.correctBlur(params.getNumber("blur"));
        GaussianBlur(blur, blur, blurSize, 0);

        getDieContours();
        getPipContours();
        List<Point> pipCenters = filterContoursAndGetPipCenters(vis);

        if (params.getBool("showContours") ) {
            drawContours(vis, valiDieContours, -1, Scalar.RED);
        }

        if (params.getBool("showPips")) {
            drawContours(vis, validPipContours, -1, Scalar.RED);
        }

        List<Integer> dieVals = clusterAndCountPips(vis, pipCenters);

        Util.clearMatVector(pipContours);
        Util.clearMatVector(validPipContours);
        Util.clearMatVector(dieContours);
        Util.clearMatVector(valiDieContours);

        blurSize.close();
        pipCenters.forEach(Pointer::close);

        return dieVals;
    }
}
