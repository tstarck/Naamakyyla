package fi.starck.naamakyyla;

import android.app.Activity;
import android.os.Bundle;

/**
 * Android Activity for Naamakyyla.
 *
 * @author Tuomas Starck
 */
public class NaamakyylaActivity extends Activity {
    /**
     * Start face recognition and stuff when Activity is created.
     *
     * For non-Android developers:
     * This is like the infamous public static void main().
     */
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(new NaamanTunnistus(this));
    }

    /**
     * Finish it! With back button.
     */
    @Override
    public void onBackPressed() {
        finish();
    }
}
