package mobi.omegacentauri.SendReduced;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
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
	static boolean DEBUG = false;

	public static void log(String s) {
		if (DEBUG)
			Log.v("SendReduced", s);
	}
	
	public static boolean pro(Context context) {
		return context.getPackageName().toLowerCase().endsWith("pro");
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (DEBUG)
			crashLogHandler();

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
					//go.setType("text/plain");
					go.setType(Utils.MIME_TYPE);
					go.putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, out);
					Uri[] grant = new Uri[out.size()];
					out.toArray(grant);
					Utils.startWithChooser(this, grant, go);
//					startActivity(go);
				}
			}
		}
		else {
			finish();
		}
//		String alpha;
//		alpha = null;
//		Log.v("",""+alpha.length());
		
	}

	private void crashLogHandler() {
		if(!(Thread.getDefaultUncaughtExceptionHandler() instanceof MyCrashHandler)) 
		    Thread.setDefaultUncaughtExceptionHandler(new MyCrashHandler());
	}
		
	class MyCrashHandler implements UncaughtExceptionHandler {

		@Override
		public void uncaughtException(Thread thread, Throwable ex) {
			Intent i = new Intent(Intent.ACTION_SEND);
			i.putExtra(Intent.EXTRA_SUBJECT, "crash report for "+getPackageName());
			i.putExtra(Intent.EXTRA_EMAIL, new String[] {"arpruss@gmail.com"} );
			StringWriter tw = new StringWriter();
			ex.printStackTrace(new PrintWriter(tw));
			i.putExtra(Intent.EXTRA_TEXT, ex.getMessage()+"\n"+tw.toString());
			i.setType("message/rfc822");
			startActivity(i);
		}		
	}
}
