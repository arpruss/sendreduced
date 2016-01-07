package mobi.omegacentauri.SendReduced;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

public class GetReduced extends Activity {
	private static final int REQUEST_LOAD_IMAGE = 1;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent i = getIntent();
		Bundle e = i.getExtras();

		if (e != null && e.containsKey(Utils.INTENT_FROM_ME)) {
			PackageManager pm = getPackageManager();
			pm.clearPackagePreferredActivities(getPackageName());
			Toast.makeText(this, "Oops: You just used SendReduced to get your photo from SendReduced. You need to set a different source from SendReduced to avoid an endless loop.", Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		if (i.getAction().equals(Intent.ACTION_GET_CONTENT) ||
				i.getAction().equals(Intent.ACTION_PICK)) {
			Intent pick = new Intent(Intent.ACTION_PICK,
					android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			pick.putExtra(Utils.INTENT_FROM_ME, true);
			startActivityForResult(Intent.createChooser(pick, "Choose source for photo to reduce"), REQUEST_LOAD_IMAGE);					
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
			Uri uri = new Utils(this).offerReduced(data.getData());
			if (uri == null)
				setResult(RESULT_CANCELED);
			else
				setResult(RESULT_OK, new Intent().setData(uri));
		}
		finish();
	}
}
