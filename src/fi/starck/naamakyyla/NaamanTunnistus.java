package fi.starck.naamakyyla;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * <p>Face recognition and Bluetooth communication.</p>
 *
 * This is heavily modified OpenCV Android face detection
 * sample class originally called FdView.
 *
 * @fixme Bluetooth stuff should really be put to its own class,
 *        but I have no more hardware to test with and
 *        refactoring blindly may not be a good idea.
 *
 * @author OpenCV developers
 * @author Tuomas Starck
 */
class NaamanTunnistus extends KameranKaepistely {
    private static final String TAG = "NaamanTunnistus";

    // Bluetooth MAC address for the NXT brick:
    private final String NXT_MAC = "00:16:53:0A:85:ED";

    // Coordinates for the target squares:
    // kohde  - final target, robot stops
    // keskus - middle, no need to turn
    private final Rect kohde  = new Rect(300, 0, 200, 120);
    private final Rect keskus = new Rect(300, 0, 200, 480);

    // Limit for the minimum size of the face (px*px):
    private final Size minimi = new Size(100, 100);

    private CascadeClassifier mCascade;

    private DataOutputStream virta;

    private Mat mRgba;
    private Mat mGray;

    /**
     * Face recognition and Bluetooth initialization.
     *
     * @param context Android Activity.
     */
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
            }
            else {
                Log.i(TAG, "Loaded cascade classifier from " + cascadeFile.getAbsolutePath());
            }

            cascadeFile.delete();
            cascadeDir.delete();

        }
        catch (IOException e) {
            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
            return;
        }

        /* Initialization of Bluetooth connection
         *
         * NXT brick must already be paired with Android device.
         * Correct device is selected by its BT MAC address.
         */
        BluetoothDevice vaerkki = null;
        BluetoothSocket pistoke = null;
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // SPP

        if (bt == null) {
            Log.e(TAG, "Blutuut ei ole olemassa :-o");
            return;
        }

        Set<BluetoothDevice> paritetut = bt.getBondedDevices();

        for (BluetoothDevice laite : paritetut) {
            // Log.i(TAG, "Laite: '" + laite.getName() + "' (" + laite.getBluetoothClass().toString() + ") " + laite.getAddress());

            if (laite.getAddress().equals(NXT_MAC)) {
                Log.i(TAG, "Valittu BT " + laite.getName());
                vaerkki = laite;
                break;
            }
        }

        if (vaerkki == null) {
            Log.e(TAG, "Bluetuut-laite uupuu :-(");
        }

        try {
            pistoke = vaerkki.createRfcommSocketToServiceRecord(uuid);
            pistoke.connect();
            virta = new DataOutputStream(pistoke.getOutputStream());
            // Log.i(TAG, "Blutuut skulaa");
        }
        catch (IOException ioe) {
            Log.e(TAG, "Blutuut yhteys feilas :-/");
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

    /**
     * @param r OpenCV Rect object.
     *
     * @return Point in the middle of the given rectangle.
     */
    private Point inTheMiddle(Rect r) {
        return inTheMiddle(r.tl(), r.br());
    }

    /**
     * @param a OpenCV Point object.
     * @param b OpenCV Point object.
     *
     * @return Point in between the two given points.
     */
    private Point inTheMiddle(Point a, Point b) {
        return new Point(a.x+(b.x-a.x)/2, a.y+(b.y-a.y)/2);
    }

    /**
     * Do face detection and Bluetooth communication.
     */
    @Override
    protected Bitmap processFrame(VideoCapture capture) {
        int viesti = 0;

        try {
            Thread.sleep(100);
            // Stetson-Harrison at work
        } catch (InterruptedException pass) {}

        capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
        capture.retrieve(mGray, Highgui.CV_CAP_ANDROID_GREY_FRAME);

        if (mCascade != null) {
            List<Rect> naamat = new LinkedList<Rect>();

            // Face detection heavy lifting
            mCascade.detectMultiScale(mGray, naamat, 1.1, 2, 2, minimi);

            // Draw cyan boxes to indicate targets
            Core.rectangle(mRgba, kohde.tl(), kohde.br(), new Scalar(0, 191, 255, 255), 2);
            Core.rectangle(mRgba, keskus.tl(), keskus.br(), new Scalar(0, 191, 255, 255), 1);

            for (Rect r : naamat) {
                Point naama = inTheMiddle(r);

                if (kohde.contains(naama)) {
                    // Everything's jolly good. Do nothing.
                    break;
                }
                else if (keskus.contains(naama)) {
                    // Face is on the right sector. We should drive forward.
                    // And draw a nice red circle.
                    Core.circle(mRgba, naama, 20, new Scalar(255, 100, 100, 255), 3);
                    viesti = 1;
                }
                else {
                    // Face is on the side. Maneuvering required.
                    // Gray box this time.
                    Core.rectangle(mRgba, r.tl(), r.br(), new Scalar(127, 127, 127, 255), 2);
                    viesti = (int) naama.x/9;
                    viesti <<= 8;
                    viesti |= 2;
                }

                break;
            }

            /* Speak Bluetooth. Hope for the best.
             */
            try {
                virta.writeInt(viesti);
            }
            catch (IOException ioe) {
                Log.e(TAG, "IO-juttu feilasi");
            }
            catch (Exception e) {
                Log.e(TAG, "Jotain feilasi");
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
