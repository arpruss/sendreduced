package mobi.omegacentauri.SendReduced;

import java.util.ArrayList;
import java.util.Collections;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class SendReduced extends Activity {
	private static boolean DEBUG = true;

	public static void log(String s) {
		if (DEBUG )
			Log.v("SendReduced", s);
	}
	
	public static boolean pro(Context context) {
		return context.getPackageName().toLowerCase().endsWith("pro");
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(this);

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
		else if (i.getAction().equals(Intent.ACTION_SEND_MULTIPLE)) {
			if (e != null &&
					e.containsKey(Intent.EXTRA_STREAM)) {
				ArrayList<Uri> in = e.getParcelableArrayList(Intent.EXTRA_STREAM);
				ArrayList<Uri> out = new ArrayList<Uri>();
				Utils utils = new Utils(this);

				for (Uri uri: in) {
					Uri reduced = utils.reduce(uri);
					if (reduced != null)
						out.add(reduced);
				}
				
				if (out.size()>0) {
					if (! options.getString(Options.PREF_NAME, Options.OPT_NAME_RANDOM).equals(Options.OPT_NAME_RANDOM))
						Collections.sort(out);
					
					Intent go = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
					go.setType("text/plain");
					go.putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, out);
					startActivity(go);
				}
			}
		}
		else {
			finish();
		}
	}
}
