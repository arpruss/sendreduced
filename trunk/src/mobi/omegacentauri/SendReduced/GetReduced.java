package mobi.omegacentauri.SendReduced;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Rect;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class GetReduced extends Activity {
	private static final int REQUEST_LOAD_IMAGE = 1;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		Intent i = getIntent();
		if (i.getAction().equals(Intent.ACTION_GET_CONTENT) ||
				i.getAction().equals(Intent.ACTION_PICK)) {
			Intent pick = new Intent(Intent.ACTION_PICK,
					android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			startActivityForResult(pick, REQUEST_LOAD_IMAGE);
			
			
			
//			Uri output = Utils.fetchReduced(this);
//			if (output == null)
//				setResult(RESULT_CANCELED);
//			else
//				setResult(RESULT_OK, new Intent().setData(output));
//			finish();
}
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (requestCode != REQUEST_LOAD_IMAGE || 
    			resultCode != RESULT_OK ||
    			data == null) {
    		setResult(RESULT_CANCELED);
    	}
    	else {
    		Uri uri = Utils.offerReduced(this, data.getData());
    		if (uri == null)
    			setResult(RESULT_CANCELED);
    		else
    			setResult(RESULT_OK, new Intent().setData(uri));
    	}
    	finish();
    }
}
