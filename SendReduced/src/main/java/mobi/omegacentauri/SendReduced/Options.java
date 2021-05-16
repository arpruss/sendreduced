package mobi.omegacentauri.SendReduced;

import java.util.Arrays;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.Toast;

public class Options extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	public static final String PREF_RESOLUTION = "resolution";
	public static final String PREF_QUALITY = "quality";
	public static final String PREF_NAME = "output";
	public static final String OPT_NAME_DATE_TIME = "date and time";
	public static final String OPT_NAME_RANDOM = "random";
	public static final String OPT_NAME_SEQUENTIAL = "sequential";
	public static final String OPT_NAME_PRESERVE = "preserve";
	public static final String PREF_EXIF_LOCATION = "exifLocation";
	public static final String PREF_EXIF_MAKE_MODEL = "exifMake";
	public static final String PREF_EXIF_DATETIME = "exifDateTime";
	public static final String PREF_EXIF_SETTINGS = "exifSettings";
	public static final String PREF_INCLUDE_DIRECT = "includeDirect";
	private static final String PREF_CONTENT_PROVIDER = "contentProvider2";
	
	public static final String[] proKeys = { PREF_NAME, PREF_EXIF_LOCATION, PREF_EXIF_MAKE_MODEL, PREF_EXIF_DATETIME, "outputPrivacy" };
	
	private static String[] summaryKeys = { PREF_RESOLUTION, PREF_QUALITY, PREF_NAME };
	private static int[] summaryEntryValues = { R.array.resolutions, R.array.qualities, R.array.outputs };
	private static int[] summaryEntries = { R.array.resolutions, R.array.qualities, R.array.outputs }; 
	private static String[] summaryDefaults = { "1024", "85", "random" };

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		addPreferencesFromResource(R.xml.options);
		Utils.cleanCache(this, System.currentTimeMillis());
	}
	@Override
	public void onResume() {
		super.onResume();

		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		customizeDisplay();
	}
	
	@Override
	public void onStop() {
		super.onStop();
	}

	
	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	static private boolean defaultContentProviderSetting() {
		return Build.VERSION.SDK_INT >= 23;
	}

	static public boolean useContentProvider(SharedPreferences options) {
		return Build.VERSION.SDK_INT >= 23;
/*		if (Build.VERSION.SDK_INT >= 28)
			return true;
		else if (Build.VERSION.SDK_INT < 23)
			return false;
		else
			return options.getBoolean(PREF_CONTENT_PROVIDER, defaultContentProviderSetting()); */
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences options, String key) {
		if (! SendReduced.pro(this) && Arrays.asList(proKeys).contains(key)) {
			Toast.makeText(this, "This setting only works in the Pro version", Toast.LENGTH_LONG).show();
		}
		setSummary(key);
	}

	public static String getString(SharedPreferences options, String key) {
		for (int i=0; i<summaryKeys.length; i++)
			if (summaryKeys[i].equals(key)) 
				return options.getString(key, summaryDefaults[i]);
		
		return options.getString(key, "");
	}
	
	public void customizeDisplay() {
		for (int i=0; i<summaryKeys.length; i++) {
			setSummary(i);
		}

		if (true || Build.VERSION.SDK_INT < 23 || Build.VERSION.SDK_INT >= 28) {
			Preference pref = findPreference(PREF_CONTENT_PROVIDER);
			if (pref != null)
				getPreferenceScreen().removePreference(pref);
		}

		SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(this);
		if (! options.contains(PREF_CONTENT_PROVIDER)) {
			SharedPreferences.Editor ed = options.edit();
			ed.putBoolean(PREF_CONTENT_PROVIDER, defaultContentProviderSetting());
			ed.commit();
		}

		PreferenceScreen upgrade = (PreferenceScreen) findPreference("upgrade");
		if (SendReduced.pro(this)) {
			if (upgrade != null)
				getPreferenceScreen().removePreference(upgrade);
		}
		else {
			Intent intent = new Intent(Intent.ACTION_VIEW);
	    	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    	if (MarketDetector.detect(this) == MarketDetector.APPSTORE) {
	            // string split up to fool switcher.sh
	      		intent.setData(Uri.parse("http://www.amazon.com/gp/mas/dl/android?p=mobi.omegacentauri.Send"+"Reduced_"+"pro"));
	      	}
	      	else {
	            // string split up to fool switcher.sh
	      		intent.setData(Uri.parse("market://details?id=mobi.omegacentauri.Send" +"Reduced_"+"pro"));
	      	}
	    	
			if (upgrade != null)
				upgrade.setIntent(intent);		
			
			for (String p : proKeys) {
				Preference pref = findPreference(p);
				if (pref != null) {
					String title = pref.getTitle().toString();
					if (! title.endsWith("[pro]")) 
						pref.setTitle(title + " [pro]");
				}
			}
	
			PreferenceScreen ps = getPreferenceScreen();
			int n = ps.getPreferenceCount();
			for (int i = 0 ; i < n ; i++) {
				Preference p = ps.getPreference(i);
				String title = p.getTitle().toString();
				if (title != null && title.contains(" [pro]")) {
					p.setTitle(title.replace(" [pro]", ""));
				}
			}
		}
	}
	
	public void setSummary(String key) {		
		for (int i=0; i<summaryKeys.length; i++) {
			if (summaryKeys[i].equals(key)) {
				setSummary(i);
				return;
			}
		}
	}
	
	public void setSummary(int i) {
		SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(this);
		Resources res = getResources();
		
		Preference pref = findPreference(summaryKeys[i]);
		String value = options.getString(summaryKeys[i], summaryDefaults[i]);
		
		String[] valueArray = res.getStringArray(summaryEntryValues[i]);
		String[] entryArray = res.getStringArray(summaryEntries[i]);
		
		for (int j=0; j<valueArray.length; j++) 
			if (valueArray[j].equals(value)) {
				pref.setSummary(entryArray[j]);
				return;
			}
	}
}

