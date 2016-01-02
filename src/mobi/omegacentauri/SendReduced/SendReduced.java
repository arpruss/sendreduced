package mobi.omegacentauri.SendReduced;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class SendReduced extends Activity {
	private static boolean DEBUG = true;
	
	public static void log(String s) {
		if (DEBUG )
			Log.v("SendReduced", s);
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		Intent i = getIntent();
		Bundle e = i.getExtras();
		if (e != null && e.containsKey(Utils.INTENT_FROM_ME)) {
			PackageManager pm = getPackageManager();
			pm.clearPackagePreferredActivities(getPackageName());
			Toast.makeText(this, "Oops: You just sent your photo from SendReduced to SendReduced. You need to set a different target from SendReduced to avoid an endless loop.", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		if (i.getAction().equals(Intent.ACTION_SEND)) {
			if (e != null &&
				e.containsKey(Intent.EXTRA_STREAM))  {
				new Utils(this).sendReduced(
						(Uri)e.getParcelable(Intent.EXTRA_STREAM));
			}
			finish();
		}
    }
}
