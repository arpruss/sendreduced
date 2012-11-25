package mobi.omegacentauri.SendReduced;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

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
		if (i.getAction().equals(Intent.ACTION_SEND)) {
			if (e != null &&
				e.containsKey(Intent.EXTRA_STREAM)) 
				Utils.sendReduced(
						this,
						(Uri)e.getParcelable(Intent.EXTRA_STREAM));
			finish();
		}
    }
}
