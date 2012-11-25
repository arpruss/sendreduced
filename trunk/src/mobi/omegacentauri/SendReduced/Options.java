package mobi.omegacentauri.SendReduced;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Options extends PreferenceActivity {
	public static final String PREF_RESOLUTION = "resolution";
	public static final String PREF_QUALITY = "quality";
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		addPreferencesFromResource(R.xml.options);
	}
}
