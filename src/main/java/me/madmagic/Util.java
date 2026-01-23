package me.madmagic;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;

import static org.bytedeco.opencv.global.opencv_core.inRange;
import static org.bytedeco.opencv.global.opencv_highgui.imshow;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class Util {

    public static Point getXY(UMat mat, int offetX, int offsetY) {
        Moments m = moments(mat);
        if (m.m00() != 0) {
            int cx = (int) (m.m10() / m.m00());
            int cy = (int) (m.m01() / m.m00());

            m.close();
            return new Point(cx + offetX, cy + offsetY);
        }

        m.close();
        return new Point(0, 0);
    }

    public static Point getXY(UMat mat) {
       return getXY(mat, 0, 0);
    }

    public static Size correctBlur(int val) {
        if (val % 2 == 0)
            val += 1;

        return new Size(val, val);
    }

    public static void imScale(UMat src, UMat dest, double factor) {
        Size size = new Size();

        resize(src, dest, size, factor, factor, INTER_AREA);

        size.close();
    }

    public static void imShow(String title, UMat mat) {
        UMat scaled = new UMat();

        imScale(mat, scaled, .2);
        imshow(title, scaled);

        scaled.close();
    }

    public static void contourFill(UMat src) {
        UMatVector contours = new UMatVector();
        findContours(src, contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

        UMat filled = UMat.zeros(src.size(), src.type());
        drawContours(filled, contours, -1, Scalar.WHITE, FILLED, LINE_8, null, 0, new Point());

        filled.copyTo(src);
        contours.close();
    }

    public static void dilateIt(UMat src, UMat kernel, int iterations) {
        for (int i = 0; i < iterations; i++) {
            dilate(src, src, kernel);
        }
    }

    public static void morphExIt(UMat src, UMat kernel, int operation, int iterations) {
        for (int i = 0; i < iterations; i++) {
            morphologyEx(src, src, operation, kernel);
        }
    }

    public static void putText(UMat img, Object text, Point p, int scale, Scalar col) {
        opencv_imgproc.putText(img, text.toString(), p, FONT_HERSHEY_SIMPLEX, scale, col, 3, LINE_AA, false);
        p.close();
    }

    public static void putText(UMat img, Object text, UMat contour, int scale, Scalar col) {
        putText(img, text.toString(), Util.getXY(contour), scale, col);
    }

    public static void clearMatVector(UMatVector vec) {
        for (int i = 0; i < vec.get().length; i++) {
            UMat n = vec.get(i);
            n.close();
        }

        vec.clear();
    }

    public static boolean isContourInsideOther(UMat outer, UMat inner) {
        Mat cpuMat = new Mat();
        inner.copyTo(cpuMat);

        BytePointer bp = cpuMat.ptr(0, 0);
        int x = bp.getInt(0);
        int y = bp.getInt(4);

        Point2f pt = new Point2f(x, y);
        double result = pointPolygonTest(outer, pt, false);

        bp.close();
        pt.close();

        return result >= 0;
    }

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
}
