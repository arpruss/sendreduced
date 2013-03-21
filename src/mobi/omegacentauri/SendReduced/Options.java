package mobi.omegacentauri.SendReduced;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Options extends PreferenceActivity {
	public static final String PREF_RESOLUTION = "resolution";
	public static final String PREF_QUALITY = "quality";
	public static final String PREF_NAME = "name";
	public static final String OPT_NAME_DATE_TIME = "date and time";
	public static final String OPT_NAME_RANDOM = "random";
	public static final String OPT_NAME_SEQUENTIAL = "sequential";
	public static final String PREF_SEQUENCE_POSITION = "sequencePosition";
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		addPreferencesFromResource(R.xml.options);
		Utils.cleanCache(this);
	}
}
