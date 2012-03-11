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

class NaamanTunnistus extends KameranKaepistely {
    private static final String TAG = "Tunnistus";

    // Laita tähän ohjattavan NXT-palikan Bluetooth MAC-osoite!
    private final String NXT_MAC = "00:16:53:0A:85:ED";

    private CascadeClassifier mCascade;

    private DataOutputStream virta;

    private Mat mRgba;
    private Mat mGray;

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

        /* Bluetooth-yhteyden alustus
         *
         * Valitaan oikea NXT-laite, joka tulee olla
         * ennestään paritettu puhelimen kanssa.
         *
         * Valinta tehdään mac-osoitteen perusteella
         * (ks. NXT_MAC).
         */

        BluetoothDevice vaerkki = null;
        BluetoothSocket pistoke = null;
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();

        if (bt == null) {
            Log.e(TAG, "Blutuut ei ole olemassa");
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
            Log.e(TAG, "Bluetuut-laite uupuu");
        }

        try {
            pistoke = vaerkki.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            pistoke.connect();
            virta = new DataOutputStream(pistoke.getOutputStream());
            // Log.i(TAG, "Blutuut skulaa");
        }
        catch (IOException ioe) {
            Log.e(TAG, "Blutuut yhteys feilas");
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
        try {
            Thread.sleep(100);
        } catch (InterruptedException pass) {}

        capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
        capture.retrieve(mGray, Highgui.CV_CAP_ANDROID_GREY_FRAME);

        if (mCascade != null) {
            List<Rect> naamat = new LinkedList<Rect>();

            mCascade.detectMultiScale(mGray, naamat, 1.1, 2, 2, new Size(100, 100)); // Min sadan² pikselin naama

            for (Rect r : naamat) {
                Point p = inTheMiddle(r.tl(), r.br());

                Log.i("HIT", "({tl} {br}) = (" + r.tl().toString() + " " + r.br().toString() + ")");

                if (r.contains(new Point(384, 216))) {
                    Core.circle(mRgba, p, 20, new Scalar(255, 100, 100, 255), 3);
                }
                else {
                    Core.rectangle(mRgba, r.tl(), r.br(), new Scalar(127, 127, 127, 255), 2);
                }

                try {
                    virta.writeInt((int)(p.x*p.y));
                }
                catch (IOException ioe) {
                    Log.e(TAG, "Blutuut-kirjoitus feilasi");
                }
                catch (Exception e) {
                    Log.e(TAG, "Jokin feilasi");
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
