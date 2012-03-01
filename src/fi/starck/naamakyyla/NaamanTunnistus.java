package fi.starck.naamakyyla;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.SurfaceHolder;

class NaamanTunnistus extends KameranKaepistely {
    private static final String TAG = "Sample::FdView";
    private Mat                 mRgba;
    private Mat                 mGray;

    private CascadeClassifier   mCascade;

    public NaamanTunnistus(Context context) {
        super(context);

        try {
            InputStream is = context.getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            mCascade = new CascadeClassifier(cascadeFile.getAbsolutePath());
            if (mCascade.empty()) {
                Log.e(TAG, "Failed to load cascade classifier");
                mCascade = null;
            } else
                Log.i(TAG, "Loaded cascade classifier from " + cascadeFile.getAbsolutePath());

            cascadeFile.delete();
            cascadeDir.delete();

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
        super.surfaceChanged(_holder, format, width, height);

        synchronized (this) {
            mGray = new Mat();
            mRgba = new Mat();
        }
    }

    private Point inTheMiddle(Point a, Point b) {
    	return new Point(a.x+(b.x-a.x)/2, a.y+(b.y-a.y)/2);
    }

    @Override
    protected Bitmap processFrame(VideoCapture capture) {
        capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
        capture.retrieve(mGray, Highgui.CV_CAP_ANDROID_GREY_FRAME);

        if (mCascade != null) {
        	/* [width,height] = [768,432] */
            // int height = mGray.rows();
            // int faceSize = Math.round(height * FdActivity.minimiNaama);

            List<Rect> naamat = new LinkedList<Rect>();

            mCascade.detectMultiScale(mGray, naamat, 1.1, 2, 2, new Size(100, 100)); // Min sadan² pikselin naama

            for (Rect r : naamat) {
            	Point p = inTheMiddle(r.tl(), r.br());
            	Log.i("HIT", "(tl; br) = (" + r.tl().toString() + "; " + r.br().toString() + ")");

            	if (r.contains(new Point(384, 216))) {
                    Core.circle(mRgba, p, 20, new Scalar(255, 100, 100, 255), 3);
            	}
            	else {
                    Core.rectangle(mRgba, r.tl(), r.br(), new Scalar(127, 127, 127, 255), 2);
            	}

            	break;
            }
        }

        Bitmap bmp = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);

        if (Utils.matToBitmap(mRgba, bmp)) {
            return bmp;
        }

        bmp.recycle();

        return null;
    }

    @Override
    public void run() {
        super.run();

        synchronized (this) {
            if (mRgba != null) mRgba.release();
            if (mGray != null) mGray.release();

            mRgba = null;
            mGray = null;
        }
    }
}
