package mobi.omegacentauri.SendReduced;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;

public class Utils {
	static final int BUFSIZE = 16384;
	static final int INVALID_ROTATION = -360000;
	static final String PREFIX = "Image";
	static final long CLEAN_TIME = (2 * 86400l * 1000l);
	
	public static boolean sendReduced(Activity a, Uri uri) {
		ContentResolver cr = a.getContentResolver();
		SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(a);
		int resolution =  Integer.parseInt(options.getString(Options.PREF_RESOLUTION, "1024"));
		ReducedImage image = new ReducedImage(cr, uri, resolution);
		if (image.bmp == null)
			return false;
		SendReduced.log("Reduced to "+image.bmp.getWidth()+"x"+image.bmp.getHeight());
		int quality = Integer.parseInt(options.getString(Options.PREF_QUALITY, "85"));
		SendReduced.log("JPEG compression quality "+quality);
		String path = saveImage(a, image.bmp, quality);
		if (path == null) 
			return false;
		Intent i = new Intent(android.content.Intent.ACTION_SEND);
		i.setType("text/plain");
		i.putExtra(android.content.Intent.EXTRA_STREAM, Uri.fromFile(new File(path)));
		a.startActivity(i);
		return true;
	}
	
	private static File getCacheDir(Context c) {
		File storage;
		if (Build.VERSION.SDK_INT >= 8)
			storage = c.getExternalCacheDir();
		else
			storage = new File(Environment.getExternalStorageDirectory().getPath()+"/SendReduced");
		storage.mkdir();
		return storage;
	}
	
	private static String saveImage(Context c, Bitmap bmp, int compress) {
		cleanCache(c);
		
		File storage = getCacheDir(c);
		
		try {
			File temp = File.createTempFile(PREFIX, ".jpg", storage);
			SendReduced.log("Compressing to "+temp);
			FileOutputStream out = new FileOutputStream(temp);
			boolean status = bmp.compress(Bitmap.CompressFormat.JPEG, compress, out);
			out.close();
			if (!status) {
				temp.delete();
				return null;
			}
			return temp.getPath();
		} catch (IOException e) {
			SendReduced.log("Error writing "+e);
			return null;
		}
		
	}
	
	public static void cleanCache(Context c) {		
		File[] list = getCacheDir(c).listFiles(new FilenameFilter(){

			@Override
			public boolean accept(File dir, String filename) {
				return filename.toLowerCase().endsWith(".jpg") && 
					filename.toLowerCase().startsWith(PREFIX.toLowerCase());
			}});
		
		long t = System.currentTimeMillis();
		for (File f: list) {			
			if (f.lastModified() < t - CLEAN_TIME ||
					t + CLEAN_TIME < f.lastModified()) {
				SendReduced.log("cleaning up "+f);
				f.delete();
			}
		}
	}

	static private int getOrientation(ContentResolver cr, Uri uri) {
		Cursor c = 
			android.provider.MediaStore.Images.Media.query(cr, uri, 
					new String[]{MediaStore.Images.Media.ORIENTATION});
		if (c == null)
			return INVALID_ROTATION;
		c.moveToFirst();
		int o = c.getInt(c.getColumnIndex(MediaStore.Images.Media.ORIENTATION));
		c.close();
		return o;
	}

//	static private String getPath(ContentResolver cr, Uri uri) {
//		Cursor c = 
//			android.provider.MediaStore.Images.Media.query(cr, uri, 
//					new String[]{MediaStore.Images.Media.DATA});
//		if (c == null)
//			return null;
//		c.moveToFirst();
//		String path = c.getString(c.getColumnIndex(MediaStore.Images.Media.DATA));
//		c.close();
//		return path;
//	}

	static private byte[] getStreamBytes(InputStream stream) {
		List<byte[]> chunks = new ArrayList<byte[]>();
		byte[] buffer = new byte[BUFSIZE];

		int total = 0;
		int read;
		try {
			while (0 <= (read = stream.read(buffer))) {
				byte[] chunk = new byte[read];
				System.arraycopy(buffer, 0, chunk, 0, read);
				chunks.add(chunk);
				total += read;
			}
		} catch (IOException e) {
			SendReduced.log("error reading: "+e);
			return null;
		}
		
		byte[] data = new byte[total];
		int pos = 0;
		
		for (byte[] chunk: chunks) {
			System.arraycopy(chunk, 0, data, pos, chunk.length);
			pos += chunk.length;
		}
		
		return data;
	}

	public static Uri offerReduced(Activity a, Uri uriIn) {
		ContentResolver cr = a.getContentResolver();
		int orientation = getOrientation(cr, uriIn);
		SendReduced.log("orientation: "+orientation);
		SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(a);
		int resolution =  Integer.parseInt(options.getString(Options.PREF_RESOLUTION, "1024"));
		ReducedImage image = new ReducedImage(cr, uriIn, resolution);
		if (image.bmp == null)
			return null;
		SendReduced.log("Reduced to "+image.bmp.getWidth()+"x"+image.bmp.getHeight());
		int quality = Integer.parseInt(options.getString(Options.PREF_QUALITY, "85"));
		SendReduced.log("JPEG compression quality "+quality);
		String path = saveImage(a, image.bmp, quality);
		if (path == null) 
			return null;
		else
			return Uri.fromFile(new File(path));
	}

	static class ReducedImage {
		long date;
		Bitmap bmp;
		
		public ReducedImage(ContentResolver cr, Uri uri, int resolution) {
			bmp = null;
			
			SendReduced.log("Reducing "+uri);
			byte[] data;
			try {
				data = getStreamBytes(cr.openInputStream(uri));
			} catch (FileNotFoundException e) {
				SendReduced.log("error reading: "+e);
				return;
			}
			
			if (data == null) 
				return;
			
			date = -1;
			if (uri.getScheme().equalsIgnoreCase("file")) {
				File f = new File(uri.getPath());
				date = f.lastModified();
			}
			
			SendReduced.log("need to decode "+data.length+" bytes");

			int o = getOrientation(cr, uri);
			SendReduced.log("orientation = "+o);
			
			Bitmap inBmp = BitmapFactory.decodeByteArray(data, 0, data.length);
			
			if (inBmp == null) {
				SendReduced.log("error decoding");
				return;
			}
			
			int h = inBmp.getHeight();
			int w = inBmp.getWidth();
			
			boolean transform = false;
			
			Matrix m = new Matrix();
			
			if (h > resolution || w > resolution) {
				if (h>w)
					m.postScale(resolution / (float)h, resolution / (float)h);
				else
					m.postScale(resolution / (float)w, resolution / (float)w);
				transform = true;
			}
			
			if (INVALID_ROTATION != o) {
				m.postRotate(o);
				transform = true;
			}
					
			SendReduced.log("image: "+w+"x"+h+" ["+o+"]");

			if (transform)
				bmp = Bitmap.createBitmap(inBmp, 0, 0, w, h, m, true);
			else
				bmp = inBmp;
		}
	}
}
