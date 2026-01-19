package me.madmagic.imagedetection;

import me.madmagic.Util;
import org.bytedeco.opencv.opencv_core.*;

import java.util.*;

import static org.bytedeco.opencv.global.opencv_core.bitwise_and;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class MarbleBlueGoldPips extends ImageDetection {

    private static final Mat edgesKernel = getStructuringElement(MORPH_RECT, new Size(8, 8));
    private static final Mat pipKernel = getStructuringElement(MORPH_ELLIPSE, new Size(7, 7));

    private static final Deque<Mat> edgeBuffer = new ArrayDeque<>();
    private static final int numFrames = 2;

    public Map<String, Integer> params = new HashMap<>() {{
        //put("dieHsvParam1", 14);
        //put("dieHsvParam2", 60);

        put("contourBlur", 15);
        put("contourMinCanny", 100);
        put("contourMaxCanny", 0);
        put("contourMaxCompactness", 50); //50
        put("contourMinArea", 8000); // lower val because 1 might not get contoured properly
        put("contourMaxArea", Integer.MAX_VALUE); //25000

        put("pipBlur", 30); //3
        put("pipHsvParam1", 14);
        put("pipMinArea", 40);
        put("pipMaxArea", 300);
        put("pipMinCircularity", 3);
        put("maxDist", 30);

        put("showContours", 1);
        put("showContourArea", 1);
        put("showContourCompactness", 0);

        put("showPips", 0);
        put("showPipArea", 0);
        put("showPipCircularity", 0);

        put("showDiceVal", 0);
    }};

    private static final Mat m1 = new Mat();
    public MatVector getContours(Mat img, Mat vis) {
        Size blurSize = Util.correctBlur(params.get("contourBlur"));
        cvtColor(img, m1, COLOR_BGR2GRAY);
        GaussianBlur(m1, m1, blurSize, 0);

        //doHSVFiltering(m1, params.get("dieHsvParam1"), 0, 0, params.get("dieHsvParam2"), 255, 255);
        //Canny(m1, m1, 0, 0); // 0, 0 because source is black/white
//        dilate(m1, m1, edgesKernel);
//        morphologyEx(m1, m1, MORPH_CLOSE, edgesKernel);
//        dilate(m1, m1, edgesKernel);
//        morphologyEx(m1, m1, MORPH_CLOSE, edgesKernel);

        Canny(m1, m1, params.get("contourMinCanny"), params.get("contourMaxCanny"));
        Util.dilateIt(m1, edgesKernel, 6);
        Util.morphExIt(m1, edgesKernel, MORPH_CLOSE, 2);

        edgeBuffer.addLast(m1.clone());
        if (edgeBuffer.size() > numFrames) {
            edgeBuffer.removeFirst().close();
        }
        for (Mat e : edgeBuffer) {
            bitwise_and(m1, e, m1);
        }

        Util.contourFill(m1);
        dilate(m1, m1, edgesKernel);

        MatVector dice = new MatVector();
        findContours(m1, dice, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
        MatVector validDice = new MatVector(dice.size());

        for (int i = 0; i < dice.get().length; i++) {
            Mat contour = dice.get(i);

            double area = contourArea(contour);

            double perimeter = arcLength(contour, true);
            double compactness = (perimeter * perimeter) / area;

            if (params.get("showContourArea") == 1) {
                putText(vis, area + "", Util.getXY(contour), FONT_HERSHEY_SIMPLEX, 3, Scalar.RED, 3, LINE_AA, false);
            }

            if (params.get("showContourCompactness") == 1) {
                putText(vis, compactness + "", Util.getXY(contour), FONT_HERSHEY_SIMPLEX, 3, Scalar.RED);
            }

            if (area < params.get("contourMinArea") || area > params.get("contourMaxArea")) continue;
            if (compactness > params.get("contourMaxCompactness")) continue;

            validDice.put(i, contour);
        }

        if (params.get("showContours") == 1) {
            drawContours(vis, dice, -1, Scalar.RED);
            drawContours(vis, validDice, -1, Scalar.BLUE);
        }

        blurSize.close();
        dice.close();
        m1.release();
        return validDice;
    }

    private static final Mat m2 = new Mat();
    private static final Deque<Mat> pipBuffer = new ArrayDeque<>();

    public void detectPips(Mat img, Mat vis, MatVector contours) {
        Size blurSize = Util.correctBlur(params.get("pipBlur"));
        GaussianBlur(img, m2, blurSize, 0);

        //doHSVFiltering(m2, 0, params.get("pipHsvParam1"), 255, 255, 255, 255);
        doHSVFiltering(m2, 0, 15, 235, 255, 60, 255);

        pipBuffer.addLast(m2.clone());
        if (pipBuffer.size() > numFrames) {
            pipBuffer.removeFirst().close();
        }
        for (Mat e : pipBuffer) {
            bitwise_and(m2, e, m2);
        }
        morphologyEx(m2, m2, MORPH_CLOSE, pipKernel);
        Util.contourFill(m2);


        Util.imShow("m2", m2);

        List<Point> detectPipCenters = new ArrayList<>(), validPipCenters = new ArrayList<>();

        for (int i = 0; i < contours.get().length; i++) {
            Mat dice = contours.get(i);

            Rect rect = boundingRect(dice);
            int w = rect.width(),
                    h = rect.height(),
                    x = rect.x(),
                    y = rect.y();

            int pad = (int) Math.floor(0.1 * Math.max(w, h));
            int x0 = Math.max(x - pad, 0),
                    y0 = Math.max(y - pad, 0),
                    x1 = Math.min(x + w + pad, img.cols()),
                    y1 = Math.min(y + h + pad, img.rows());

            Rect roiRect = new Rect(x0, y0, x1 - x0, y1 - y0);
            Mat extractedDice = new Mat(m2, roiRect);

            MatVector dicePipContours = new MatVector();
            findContours(extractedDice, dicePipContours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

            for (Mat contour : dicePipContours.get()) {
                Moments m = moments(contour, false);
                int cx = (int)(m.m10() / m.m00());
                int cy = (int)(m.m01() / m.m00());

                detectPipCenters.add(new Point(cx + x0, cy + y0));

                double area = contourArea(contour);
                double peri = arcLength(contour, true);
                if (peri == 0) continue;
                double circularity = 4 * Math.PI * area / (peri * peri);

                if (circularity < (params.get("pipMinCircularity") / 10f)) continue;
                if (area < params.get("pipMinArea") || area > params.get("pipMaxArea")) continue;

                if (params.get("showPipArea") == 1) {
                    putText(vis, area + "", Util.getXY(contour, x0, y0), FONT_HERSHEY_SIMPLEX, 2, Scalar.RED, 2, LINE_AA, false);
                }

                if (params.get("showPipCircularity") == 1) {
                    putText(vis, circularity + "", Util.getXY(contour, x0, y0), FONT_HERSHEY_SIMPLEX, 2, Scalar.RED, 2, LINE_AA, false);
                }


                validPipCenters.add(new Point(cx + x0, cy + y0));
            }
        }

        if (params.get("showPips") == 1) {
            detectPipCenters.forEach(p -> circle(vis, p, 5, new Scalar(0, 0, 255, 0)));
            validPipCenters.forEach(p -> circle(vis, p, 5, new Scalar(255, 0, 0, 0)));
        }
//
//        List<Double> dists = new ArrayList<>();
//        for (int i = 0; i < validPipCenters.size(); i++) {
//            Point pi = validPipCenters.get(i);
//            for (int j = i + 1; j < validPipCenters.size(); j++) {
//                Point pj = validPipCenters.get(j);
//                double dx = pi.x() - pj.x();
//                double dy = pi.y() - pj.y();
//                double distance = Math.sqrt(dx * dx + dy * dy);
//                dists.add(distance);
//            }
//        }
//
//        dists.sort(Double::compare);
//        double spacing;
//        if (!dists.isEmpty()) {
//            int index = (int) (0.25 * dists.size());
//            spacing = dists.get(index);
//        } else {
//            spacing = 0;
//        }
//
//        double maxDist = spacing * (params.get("maxDist") / 100f) ;
//
//        List<List<Point>> clusters = new ArrayList<>();
//        boolean[] used = new boolean[validPipCenters.size()];
//
//        for (int i = 0; i < validPipCenters.size(); i++) {
//            if (used[i]) continue;
//
//            List<Point> cluster = new ArrayList<>();
//            Stack<Integer> stack = new Stack<>();
//            stack.push(i);
//            used[i] = true;
//            cluster.add(validPipCenters.get(i));
//
//            while (!stack.isEmpty()) {
//                int idx = stack.pop();
//                Point pi = validPipCenters.get(idx);
//
//                for (int j = 0; j < validPipCenters.size(); j++) {
//                    if (used[j]) continue;
//
//                    Point pj = validPipCenters.get(j);
//                    double dx = pi.x() - pj.x();
//                    double dy = pi.y() - pj.y();
//                    double distance = Math.sqrt(dx * dx + dy * dy);
//
//                    if (distance < maxDist) {
//                        used[j] = true;
//                        stack.push(j);
//                        cluster.add(pj);
//                    }
//                }
//            }
//
//            clusters.add(cluster);
//        }
//
//        if (params.get("showDiceVal") == 1) {
//            clusters.forEach(l -> {
//                int sumX = 0;
//                int sumY = 0;
//
//                for (Point p : l) {
//                    sumX += p.x();
//                    sumY += p.y();
//                }
//
//                int cx = sumX / l.size();
//                int cy = sumY / l.size();
//
//                putText(vis, l.size() + "", new Point(cx, cy), FONT_HERSHEY_SIMPLEX, 0.8, new Scalar(0, 0, 255, 0));
//            });
//        }
//

        blurSize.close();
        contours.close();
        m2.release();
    }

    @Override
    public List<Integer> getDieScore(Mat img, Mat vis) {
        return List.of();
    }
}
