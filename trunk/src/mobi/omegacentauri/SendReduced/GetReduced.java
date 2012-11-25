package mobi.omegacentauri.SendReduced;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

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
