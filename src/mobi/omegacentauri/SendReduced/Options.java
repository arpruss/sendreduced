package mobi.omegacentauri.SendReduced;

import mobi.omegacentauri.SendReduced.R;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

public class Options extends PreferenceActivity {
	public static final String PREF_RESOLUTION = "resolution";
	public static final String PREF_QUALITY = "quality";
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		addPreferencesFromResource(R.xml.options);
	}
}
