package com.test.fugatakase.opencvtest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.test.fugatakase.opencvtest.com.test.fugatakase.opencvtest.View.MyImageEditView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("opencv_java3");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        Bitmap src = BitmapFactory.decodeResource(getResources(), R.drawable.image1);


        LinearLayout layout = (LinearLayout)findViewById(R.id.layout);


        Display disp = this.getWindowManager().getDefaultDisplay();
        Point point = new Point();
        disp.getSize(point);
        MyImageEditView editView = new MyImageEditView(this, null, point.x, point.y, src);
        editView.setBackgroundColor(Color.TRANSPARENT);

        layout.addView(editView);
        editView.invalidate();
    }
}
