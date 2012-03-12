package fi.starck.naamakyyla;

import java.util.List;

import org.opencv.core.Size;
import org.opencv.highgui.VideoCapture;
import org.opencv.highgui.Highgui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * This is lightly modified OpenCV face detection sample
 * originally named SampleCvViewBase.
 */
public abstract class KameranKaepistely extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final String TAG = "KameranKaepistely";

    private final SurfaceHolder mHolder;
    private VideoCapture        mCamera;

    public KameranKaepistely(Context context) {
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
        synchronized (this) {
            if (mCamera != null && mCamera.isOpened()) {
                List<Size> sizes = mCamera.getSupportedPreviewSizes();
                int mFrameWidth = width;
                int mFrameHeight = height;

                // Selecting optimal camera preview size
                {
                    double minDiff = Double.MAX_VALUE;
                    for (Size size : sizes) {
                        if (Math.abs(size.height - height) < minDiff) {
                            mFrameWidth = (int) size.width;
                            mFrameHeight = (int) size.height;
                            minDiff = Math.abs(size.height - height);
                        }
                    }
                }

                mCamera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, mFrameWidth);
                mCamera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, mFrameHeight);
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = new VideoCapture(Highgui.CV_CAP_ANDROID);
        if (mCamera.isOpened()) {
            (new Thread(this)).start();
        } else {
            mCamera.release();
            mCamera = null;
            Log.e(TAG, "Failed to open native camera");
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            synchronized (this) {
                mCamera.release();
                mCamera = null;
            }
        }
    }

    protected abstract Bitmap processFrame(VideoCapture capture);

    public void run() {
        while (true) {
            Bitmap bmp = null;

            synchronized (this) {
                if (mCamera == null)
                    break;

                if (!mCamera.grab()) {
                    Log.e(TAG, "mCamera.grab() failed");
                    break;
                }

                bmp = processFrame(mCamera);
            }

            if (bmp != null) {
                Canvas canvas = mHolder.lockCanvas();
                if (canvas != null) {
                    canvas.drawBitmap(bmp, (canvas.getWidth() - bmp.getWidth()) / 2, (canvas.getHeight() - bmp.getHeight()) / 2, null);
                    mHolder.unlockCanvasAndPost(canvas);
                }
                bmp.recycle();
            }
        }
    }
}
