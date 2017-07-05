package com.test.fugatakase.opencvtest.com.test.fugatakase.opencvtest.View;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by FugaTakase on 2017/06/12.
 */

public class MyImageEditView extends View{

    private Bitmap srcBitmap;
    private Bitmap dispBitmap;
    private Bitmap maskBitmap;
    private Mat srcMat;
    private Mat dispMat;
    private Mat maskMat;
    private Mat cutMat;
    private Mat labMat;

    private int dispWidth;
    private int dispHeight;

    private double threshold = 0.05;    // 閾値
    private int touchPointX;
    private int touchPointY;
    private int movePointX;
    private int movePointY;
    private double[] touchPointColorLab;

    private boolean withMask;


    private static final double v25_7 = Math.pow(25, 7);
    private static final double d6 = Math.toRadians(6);
    private static final double d25 = Math.toRadians(25);
    private static final double d30 = Math.toRadians(30);
    private static final double d60 = Math.toRadians(60);
    private static final double d63 = Math.toRadians(63);
    private static final double d275 = Math.toRadians(275);
    private static final double kl = 1;
    private static final double kc = 1;
    private static final double kh = 1;

    private static final double MAX_COLOR_DISTANCE;
    static{
        Mat whiteMat = new Mat(new Size(1,1), CvType.CV_8UC3, Scalar.all(255));
        Mat blackMat = new Mat(new Size(1,1), CvType.CV_8UC3, Scalar.all(0));

        Imgproc.cvtColor(whiteMat, whiteMat, Imgproc.COLOR_BGR2Lab);
        Imgproc.cvtColor(blackMat, blackMat, Imgproc.COLOR_BGR2Lab);

        double[] whiteLab = whiteMat.get(0,0);
        double[] blackLab = blackMat.get(0,0);

        MAX_COLOR_DISTANCE = dif(whiteLab[0],whiteLab[1],whiteLab[2],blackLab[0],blackLab[1],blackLab[2]);

    }

    private final double[] MASC_COLOR_BGRA = {255, 0, 0, 0};

    private List<Mat> undoViewStack;
    public Mat popUndoStack(){
        if(undoViewStack != null && undoViewStack.size() <= 0){
            Mat v = undoViewStack.get(undoViewStack.size() - 1);
            undoViewStack.remove(v);
            return v;
        } else {
            return null;
        }
    }
    public void pushUndoStack(Mat v){
        if(undoViewStack != null){
            undoViewStack = new ArrayList<>();
        }

        undoViewStack.add(v);
    }

    private List<Mat> redoViewStack;
    public Mat popRedoStack(){
        if(redoViewStack != null && redoViewStack.size() <= 0){
            Mat v = redoViewStack.get(redoViewStack.size() - 1);
            redoViewStack.remove(v);
            return v;
        } else {
            return null;
        }
    }
    public void pushRedoStack(Mat v){
        if(redoViewStack != null){
            redoViewStack = new ArrayList<>();
        }

        redoViewStack.add(v);
    }

    public MyImageEditView(Context context)
    {
        super(context);
    }

    public MyImageEditView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public MyImageEditView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    public MyImageEditView(Context context, View parentView, int dispWidth, int dispHeght, Bitmap src){
        super(context);
        undoViewStack = new ArrayList<>();
        redoViewStack = new ArrayList<>();
        this.dispWidth = dispWidth;
        this.dispHeight = dispHeght;

        withMask = false;

        setBitmap(src);

        invalidate();

    }

    public void setBitmap(Bitmap bitmap){

        srcBitmap = bitmap;
        srcMat = new Mat();
        Utils.bitmapToMat(srcBitmap, srcMat);
        //Imgproc.cvtColor(srcMat, srcMat, Imgproc.COLOR_BGR2BGRA);


        double widthScale =  (double) dispWidth / (double)srcBitmap.getWidth();
        double heightScale = (double)dispHeight / (double)srcBitmap.getHeight() ;

        double scale = Math.min(widthScale, heightScale);

        dispMat = new Mat();
        Imgproc.resize(srcMat,dispMat, new Size(0,0) , scale ,scale, Imgproc.INTER_AREA);

        //　色差計算用のLab表色系に変換したMatを用意
        labMat = new Mat(dispMat.size(), CvType.CV_8UC3);
        Imgproc.cvtColor(dispMat, labMat, Imgproc.COLOR_BGRA2BGR);
        Imgproc.cvtColor(labMat, labMat, Imgproc.COLOR_BGR2Lab);

        dispBitmap = Bitmap.createBitmap(dispMat.width(), dispMat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dispMat, dispBitmap, true);

        //マスク画像も透明で初期化
        maskMat = new Mat(dispMat.size(), CvType.CV_8UC4, Scalar.all(255));
        cutMat = new Mat(dispMat.size(), CvType.CV_8UC4, Scalar.all(0));

        maskBitmap = Bitmap.createBitmap(maskMat.width(), maskMat.height(), Bitmap.Config.ARGB_8888);


        invalidate();
    }

    public Bitmap getBitmap(){

        double widthScale =  (double)srcBitmap.getWidth() / (double) dispWidth;
        double heightScale = (double)srcBitmap.getHeight() / (double)dispHeight;

        double scale = Math.max(widthScale, heightScale);

        Mat resultMat = new Mat();
        Imgproc.resize(dispMat, resultMat, new Size(0,0) , scale ,scale, Imgproc.INTER_AREA);

        Bitmap resultBitmap = Bitmap.createBitmap(resultMat.width(), resultMat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(resultMat, resultBitmap ,true);

        return resultBitmap;

    }

    public Bitmap getDispBitmap(boolean withMask){

        if(!withMask){
            return dispMat2Bitmap();
        }

        Mat brendMat = new Mat();
        Core.addWeighted(dispMat, 0.8d, maskMat, 0.2d, 1.0d, brendMat);

        Bitmap resultBitmap = Bitmap.createBitmap(brendMat.width(), brendMat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(brendMat, resultBitmap ,true);

        return resultBitmap;

    }

    private Bitmap dispMat2Bitmap(){
        Bitmap resultBitmap = Bitmap.createBitmap(dispMat.width(), dispMat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dispMat, resultBitmap ,true);

        return resultBitmap;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // ペイント生成
        Paint imagePaint = new Paint();
        canvas.drawBitmap(getDispBitmap(withMask),0 ,0 , imagePaint);

    }

    @Override
    public boolean performClick()
    {
        super.performClick();
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                withMask = true;
                onTouchDown(event);
                break;
            case MotionEvent.ACTION_UP:
                withMask = false;
                onTouchUp(event);
                break;
            case MotionEvent.ACTION_MOVE:
                withMask = true;
                onTouchMove(event);
                break;
            default:
                break;
        }

        return true;
    }

    private void onTouchDown(MotionEvent event) {
        redoViewStack.clear();
        //マスク画像を透明で初期化
        maskMat = new Mat(dispMat.size(), CvType.CV_8UC4, Scalar.all(255));
        cutMat = new Mat(dispMat.size(), CvType.CV_8UC4, Scalar.all(0));

        touchPointX = Math.round(event.getX());
        if(touchPointX > dispMat.width()){
            touchPointX = dispMat.width() -1;
            movePointX = touchPointX;
        }
        touchPointY = Math.round(event.getY());
        if(touchPointY > dispMat.height()){
            touchPointY = dispMat.height() -1;
            movePointY = touchPointY;
        }

        touchPointColorLab = new double[3];

        touchPointColorLab = labMat.get(touchPointY, touchPointX);

        createMaskImage(touchPointX, touchPointY);
        invalidate();

    }

    private void onTouchMove(MotionEvent event) {

        redoViewStack.clear();

        movePointX = Math.round(event.getX());
        movePointY = Math.round(event.getY());

        createMaskImage(movePointX, movePointY);
        invalidate();

    }

    private void onTouchUp(MotionEvent event) {
        pushUndoStack(dispMat.clone());

        Core.bitwise_or(dispMat, cutMat, dispMat);
        invalidate();

    }


    private void createMaskImage(int x , int y){


        judgeColor(x ,y);

    }

    private void judgeColor(int x, int y){

        for (int i = x - 30;  i <= x + 30 ; i++ ){
            if(i >= 0 && i < dispMat.width()) {
                for (int j = y - 30; j <= y + 30; j++) {
                    if (j >= 0 && j < dispMat.height()) {

                        if (isInARangeColor(labMat.get(j, i))) {
                            maskMat.put(j, i, MASC_COLOR_BGRA);
                            cutMat.put(j, i, new double[]{255, 255, 255, 255});

                        }

                    }
                }
            }
        }
    }

    private boolean isEqualBGRAColor(double[] bgraColor1, double[] bgraColor2){
        if(bgraColor1[0] == bgraColor2[0] && bgraColor1[1] == bgraColor2[1] && bgraColor1[2] == bgraColor2[2] && bgraColor1[3] == bgraColor2[3]){
            return true;
        }else{
            return false;
        }
    }

    private boolean isInARangeColor(double[] labColor){

        if(labColor == null || labColor.length != 3){
            return false;
        }

        if (dif(touchPointColorLab[0], touchPointColorLab[1], touchPointColorLab[2], labColor[0], labColor[1], labColor[2]) / (MAX_COLOR_DISTANCE * threshold) <= 1 ){
            return true;
        }

        return false;
    }

    // CIEDE2000による色差の計算
    private static double dif(double l1, double a1, double b1, double l2, double a2, double b2){

        double dld = l2 - l1;
        double lb = (l1 + l2) / 2;

        double cs1 = Math.hypot(a1, b1);
        double cs2 = Math.hypot(a2, b2);
        double cb = (cs1 + cs2) / 2;
        double cb7 = Math.pow(cb, 7);
        double ad1 = a1 + a1 / 2 * (1 - Math.sqrt(cb7 / (cb7 + v25_7)));
        double ad2 = a2 + a2 / 2 * (1 - Math.sqrt(cb7 / (cb7 + v25_7)));

        double cd1 = Math.hypot(ad1, b1);
        double cd2 = Math.hypot(ad2, b2);
        double cbd = (cd1 + cd2) / 2;
        double cbd7 = Math.pow(cbd, 7);

        double dcd = (cd2 - cd1);
        double hd1 = b1 == 0 && ad1 == 0 ? 0 : Math.atan2(b1, ad1);
        if(hd1 < 0){
            hd1 += Math.PI * 2;
        }
        double hd2 = b2 == 0 && ad2 == 0 ? 0 : Math.atan2(b2, ad2);
        if(hd2 < 0){
            hd2 += Math.PI * 2;
        }

        double dhd = hd2 - hd1;
        if(cd1 * cd2 == 0){
            dhd = 0;
        } else if(Math.abs(hd1 - hd2) > Math.PI) {
            if(hd2 <= hd1){
                dhd += Math.PI * 2;
            } else {
                dhd -= Math.PI * 2;
            }
        }


        double dhhd = 2 * Math.sqrt(cd1 * cd2) * Math.sin(dhd / 2);
        double hhbd = 0;
        if(cd1 * cd2 != 0){
            hhbd = Math.abs(hd1 - hd2) > Math.PI ? ( hd1 + hd2 + Math.PI * 2) / 2 : (hd1 + hd2) / 2;
        }

        double tt = 1
                - 0.17 * Math.cos(hhbd - d30)
                + 0.24 * Math.cos(2 * hhbd)
                + 0.32 * Math.cos(3 * hhbd + d6)
                - 0.20 * Math.cos(4 * hhbd - d63);
        double lb50_2 = Math.pow(lb - 50, 2);
        double ssl = 1 + (0.015 * lb50_2) / Math.sqrt(20 + lb50_2);
        double ssc = 1 + 0.045 * cbd;
        double ssh = 1 + 0.015 * cbd * tt;
        double rrt = -2d * Math.sqrt(cbd7 / (cbd7 +v25_7)) * Math.sin(d60 * Math.exp(- Math.pow((hhbd - d275)/ d25, 2)));
        double de = Math.pow(dld / (kl * ssl), 2)
                + Math.pow(dcd / (kc * ssc), 2)
                + Math.pow(dhhd / (kh * ssh), 2)
                + rrt * (dcd / (kc * ssc)) * (dhhd / (kh * ssh));

        return Math.sqrt(de);
    }

    public void undo(){
        dispMat = popUndoStack();
        pushRedoStack(dispMat.clone());
        invalidate();
    }

    public void redo(){
        dispMat = popRedoStack();
        pushUndoStack(dispMat.clone());
        invalidate();
    }

}
