package mobi.omegacentauri.SendReduced;

import java.util.ArrayList;

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
					Intent go = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
					go.setType(Utils.MIME_TYPE);
					go.putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, out);
					startActivity(go);
				}
			}
		}
    }
}
