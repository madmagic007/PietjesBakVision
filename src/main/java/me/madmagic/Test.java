package me.madmagic;


import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class Test {

    private static final List<Canvas> canvases = new ArrayList<>();
    private static final List<JSlider> sliders = new ArrayList<>();

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new java.awt.GridLayout(3, 3));
        frame.setSize(1920, 1080);
        frame.setVisible(true);

        JFrame settings = new JFrame();
        settings.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        settings.setVisible(true);
        settings.setLayout(new FlowLayout());

        for (int i = 0; i < 3 * 3; i++) {
            Canvas c = new Canvas();

            canvases.add(c);
            frame.add(c);
        }

        for (int i = 0; i < 2; i++) {
            JSlider slider = new JSlider();
            JLabel valueLabel = new JLabel(slider.getValue() + "");

            slider.addChangeListener(e -> valueLabel.setText(slider.getValue() + ""));
            sliders.add(slider);
        }

        Util.openCamera(camImg -> {
            splitCols(camImg, 0);

            Mat hsv = new Mat();
            cvtColor(camImg, hsv, COLOR_BGR2HSV);
            splitCols(hsv, 3);

            Mat lab = new Mat();
            cvtColor(camImg, lab, COLOR_BGR2Lab);
            splitCols(hsv, 6);

            hsv.close();
            lab.close();

        },1);
    }

    private static void splitCols(Mat col, int baseIndex) {
        MatVector cols = new MatVector();
        split(col, cols);

        for (int i = 0; i < cols.get().length; i++) {
            Mat m = cols.get()[i];
            showImg(m, baseIndex + i);
        }

        cols.close();
    }

    private static final OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
    private static final Java2DFrameConverter bufConverter = new Java2DFrameConverter();

    private static void showImg(Mat mat, int index) {
        Mat m1 = new Mat();
        //normalize(mat, m1, null, sliders.get(0).getValue(), 255, NORM_MINMAX);

        org.bytedeco.javacv.Frame f = matConverter.convert(mat);
        Canvas c = canvases.get(index);
        c.getGraphics().drawImage(bufConverter.getBufferedImage(f), 0, 0, c.getWidth(), c.getHeight(), null);
    }
}
