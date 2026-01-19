package me.madmagic;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;

import static org.bytedeco.opencv.global.opencv_highgui.imshow;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class Util {

    public static Mat loadResourceImage(String resourceName) {
        return loadImage(Util.class.getResource(resourceName).getFile());
    }

    public static Mat loadImage(String path) {
        return imread(path);
    }

    public static Point getXY(Mat mat, int offetX, int offsetY) {
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

    public static Point getXY(Mat mat) {
       return getXY(mat, 0, 0);
    }

    public static Size correctBlur(int val) {
        if (val % 2 == 0)
            val += 1;

        return new Size(val, val);
    }

    public static void imShow(String title, Mat mat) {
        Mat scaled = new Mat();
        Size size = new Size();

        resize(mat, scaled, size, .2, .2, INTER_AREA);
        imshow(title, scaled);

        size.close();
        scaled.close();
    }

    public static void contourFill(Mat src) {
        MatVector contours = new MatVector();
        findContours(src, contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

        Mat filled = Mat.zeros(src.size(), src.type()).asMat();
        drawContours(filled, contours, -1, Scalar.WHITE, FILLED, LINE_8, null, 0, new Point());

        filled.copyTo(src);
        contours.close();
    }

    public static void dilateIt(Mat src, Mat kernel, int iterations) {
        for (int i = 0; i < iterations; i++) {
            dilate(src, src, kernel);
        }
    }

    public static void morphExIt(Mat src, Mat kernel, int operation, int iterations) {
        for (int i = 0; i < iterations; i++) {
            morphologyEx(src, src, operation, kernel);
        }
    }

    public static void putText(Mat img, Object text, Point p, int scale, Scalar col) {
        opencv_imgproc.putText(img, text.toString(), p, FONT_HERSHEY_SIMPLEX, scale, col, 3, LINE_AA, false);
        p.close();
    }

    public static void putText(Mat img, Object text, Mat contour, int scale, Scalar col) {
        putText(img, text.toString(), Util.getXY(contour), scale, col);
    }

    public static void clearMatVector(MatVector vec) {
        for (int i = 0; i < vec.get().length; i++) {
            Mat n = vec.get(i);
            n.close();
        }

        vec.clear();
    }

    public static boolean isContourInsideOther(Mat outer, Mat inner) {
        BytePointer bp = inner.ptr(0, 0);
        int x = bp.getInt(0);
        int y = bp.getInt(4);

        Point2f pt = new Point2f(x, y);
        double result = pointPolygonTest(outer, pt, false);

        bp.close();
        pt.close();

         return result >= 0;
    }
}
