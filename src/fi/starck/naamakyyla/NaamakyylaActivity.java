package fi.starck.naamakyyla;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class NaamakyylaActivity extends Activity {
    private static final String TAG         = "Sample::Activity";

    /*
    private MenuItem            mItemFace50;
    private MenuItem            mItemFace40;
    private MenuItem            mItemFace30;
    private MenuItem            mItemFace20;
    */

    // public static float minimiNaama = 0.3f;

    public NaamakyylaActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle state) {
        // Log.i(TAG, "onCreate");
        super.onCreate(state);
        // requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(new NaamanTunnistus(this));
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu");
        mItemFace50 = menu.add("Face size 50%");
        mItemFace40 = menu.add("Face size 40%");
        mItemFace30 = menu.add("Face size 30%");
        mItemFace20 = menu.add("Face size 20%");
        return true;
    }
    */

    /*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "Menu Item selected " + item);
        if (item == mItemFace50)
            minimiNaama = 0.5f;
        else if (item == mItemFace40)
            minimiNaama = 0.4f;
        else if (item == mItemFace30)
            minimiNaama = 0.3f;
        else if (item == mItemFace20)
            minimiNaama = 0.2f;
        return true;
    }
    */
}
