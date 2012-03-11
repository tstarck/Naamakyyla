package fi.starck.naamakyyla;

import android.app.Activity;
import android.os.Bundle;

public class NaamakyylaActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(new NaamanTunnistus(this));
    }

    /** Called when back button is pressed. */
    @Override
    public void onBackPressed() {
        finish();
    }
}
