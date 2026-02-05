package me.madmagic.detection.model;

import me.madmagic.Util;
import me.madmagic.webinterface.socket.SessionRegistry;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.opencv.opencv_core.*;

import java.util.*;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class WhiteDieBlackPips extends VisionModel {

    public WhiteDieBlackPips(String name) {
        super(name);

        params.add("cropLeft", 800, 0, 2000);
        params.add("cropRight", 800, 0, 2000);
        params.add("blur", 21, 1, 30);

        params.add("contourMinCanny", 0, 0, 255);
        params.add("contourMaxCanny", 60, 0, 255);
        params.add("contourKernelSize", 9, 1, 20);
        params.add("contourMinArea", 15000, 0, 100000);
        params.add("contourFrames", 3, 0, 20);

        params.add("blackHatKernelSize", 30, 1, 50);
        params.add("blackHatThresh", 65, 0, 255);

        params.add("pipMinArea", 200, 0, 10000);
        params.add("pipMaxArea", 500, 0, 10000);
        params.add("pipMinCircularity", 75, 0, 100);
        params.add("pipMaxDist", 68, 0, 200);
    }

    private final UMat visMat = new UMat();
    private final UMat blurMat = new UMat();
    private final UMat m1 = new UMat();
    private final UMat dieContoursMat = new UMat();
    private final UMat blackHatMat = new UMat();
    private final Deque<UMat> contourDequeue = new ArrayDeque<>();
    private final UMatVector dieContours = new UMatVector();
    private final UMatVector pipContours = new UMatVector();
    private final UMatVector valiDieContours = new UMatVector();
    private final UMatVector validPipContours = new UMatVector();


    private void getDieContours() {
        Canny(blurMat, m1, params.getNumber("contourMinCanny"), params.getNumber("contourMaxCanny"));

        int s = params.getNumber("contourKernelSize");
        Size contourKSize = new Size(s, s);
        Mat cKernel = getStructuringElement(MORPH_RECT, contourKSize);
        UMat contourKernel = new UMat();
        cKernel.copyTo(contourKernel);

        Util.dilateIt(m1, contourKernel, 4);
        Util.morphExIt(m1, contourKernel, MORPH_CLOSE, 2);
        Util.morphExIt(m1, contourKernel, MORPH_OPEN, 2);

        Util.contourFill(m1);

        contourDequeue.addLast(m1.clone());
        if (contourDequeue.size() > params.getNumber("contourFrames")) {
            contourDequeue.removeFirst().close();
        }
        for (UMat e : contourDequeue) {
            if (e.rows() != m1.rows() || e.cols() != m1.cols()) continue;

            bitwise_or(m1, e, m1);
        }


        Util.clearMatVector(dieContours);
        findContours(m1, dieContours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
        m1.copyTo(dieContoursMat);

        contourKSize.close();
        contourKernel.close();
        cKernel.close();
    }

    private void getPipContours() {
        int s = params.getNumber("blackHatKernelSize");
        Mat bhKernelC = getStructuringElement(MORPH_ELLIPSE, new Size(s, s));
        UMat bhKernel = new UMat();
        bhKernelC.copyTo(bhKernel);

        morphologyEx(blurMat, blackHatMat, MORPH_BLACKHAT, bhKernel);
        normalize(blackHatMat, blackHatMat, 0, 255, NORM_MINMAX, -1, null);
        threshold(blackHatMat, blackHatMat, params.getNumber("blackHatThresh"), 255, THRESH_BINARY);

        Util.clearMatVector(pipContours);
        findContours(blackHatMat, pipContours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

        bhKernel.close();
        bhKernelC.close();
    }

    private List<Point> filterContoursAndGetPipCenters() {
        List<UMat> validPips = new ArrayList<>();
        List<UMat> validDice = new ArrayList<>();
        List<UMat> finalPips = new ArrayList<>();
        List<Point> pipCenters = new ArrayList<>();

        for (int i = 0; i < pipContours.get().length; i++) {
            UMat pipMat = pipContours.get(i);

            double pipArea = contourArea(pipMat);
            double peri = arcLength(pipMat, true);
            if (peri == 0) continue;
            double circularity = 4 * Math.PI * pipArea / (peri * peri);

            if (pipArea < params.getNumber("pipMinArea") || pipArea > params.getNumber("pipMaxArea")) continue;
            if (circularity < (params.getNumber("pipMinCircularity") / 100f)) continue;

            if (params.getBool("showPipArea")) {
                Util.putText(visMat, pipArea, pipMat, 2, Scalar.RED);
            }

            if (params.getBool("showPipCircularity")) {
                Util.putText(visMat, circularity, pipMat,2, Scalar.RED);
            }

            validPips.add(pipMat);
        }

        for (int i = 0; i < dieContours.size(); i++) {
            UMat die = dieContours.get(i);

            double area = contourArea(die);
            if (area < params.getNumber("contourMinArea")) continue;

            boolean hasValidPip = false;
            for (UMat pip : validPips) {
                if (Util.isContourInsideOther(die, pip)) {
                    hasValidPip = true;
                    break;
                }
            }

            if (!hasValidPip) continue;

            validDice.add(die);

            if (params.getBool("showContourArea")) {
                Util.putText(visMat, area, die, 3, Scalar.RED);
            }
        }

        for (UMat pip : validPips) {
            for (UMat die : validDice) {
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

    private List<Integer> clusterAndCountPips(List<Point> pipCenters) {
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

                Util.putText(visMat, l.size(), new Point(cx, cy), 3, Scalar.RED);
            }
        });

        return values;
    }


    @Override
    public List<Integer> getDieScore(UMat img) {
        long t0 = System.nanoTime();

        int cropLeft = params.getNumber("cropLeft");
        int cropRight = params.getNumber("cropRight");

        int newWidth = img.cols() - cropLeft - cropRight;
        if (newWidth <= 0) newWidth = 1;

        Rect roi = new Rect(cropLeft, 0, newWidth, img.rows());
        UMat centerMat = new UMat(img, roi);

        centerMat.copyTo(visMat);
        cvtColor(centerMat, blurMat, COLOR_BGR2GRAY);

        Size blurSize = Util.correctBlur(params.getNumber("blur"));
        GaussianBlur(blurMat, blurMat, blurSize, 0);

        long t1 = System.nanoTime();

        getDieContours();

        long t2 = System.nanoTime();

        getPipContours();

        long t3 = System.nanoTime();

        List<Point> pipCenters = filterContoursAndGetPipCenters();

        long t4 = System.nanoTime();

        List<Integer> dieVals = clusterAndCountPips(pipCenters);

        long t5 = System.nanoTime();

        if (params.getBool("showContours") ) {
            drawContours(visMat, valiDieContours, -1, Scalar.RED);
        }

        if (params.getBool("showPips")) {
            drawContours(visMat, validPipContours, -1, Scalar.RED);
        }

        SessionRegistry.broadcastMats(visMat, dieContoursMat, blackHatMat);

        long t6 = System.nanoTime();

        Util.clearMatVector(pipContours);
        Util.clearMatVector(validPipContours);
        Util.clearMatVector(dieContours);
        Util.clearMatVector(valiDieContours);

        blurSize.close();
        pipCenters.forEach(Pointer::close);

        System.out.printf(
                "cvtBlur: %.1f ms, dieContours: %.1f ms, pips: %.1f ms, pipCenters: %.1f ms, cluster and count: %.1f ms, network: %.1f ms,     \n",
                (t1-t0)/1e6, (t2-t1)/1e6, (t3-t2)/1e6, (t4-t3)/1e6, (t5-t4)/1e6, (t6-t5)/1e6
        );

        return dieVals;
    }
}
