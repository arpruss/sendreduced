package mobi.omegacentauri.SendReduced;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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
import android.provider.MediaStore.Images;
import android.util.Log;

public class Utils {
	static final int BUFSIZE = 16384;
	static final int INVALID_ROTATION = -360000;
	static final String PREFIX = "Image";
	static final long CLEAN_TIME = (2 * 3600 * 1000l);
	Activity activity;
	ContentResolver cr;
	SharedPreferences options;
	private int outResolution;
	private int outQuality;
	private int sequencePos;
	static final String MIME_TYPE = "image/jpeg"; //"text/plain";
	public static final String INTENT_FROM_ME = "mobi.omegacentauri.Send" + "Reduced.INTENT_FROM_ME";
	private String curCacheDir;
	private long curTime;
	
	public Utils(Activity a) {
		activity = a;
		cr = a.getContentResolver();
		options = PreferenceManager.getDefaultSharedPreferences(a);
		outResolution =  Integer.parseInt(options.getString(Options.PREF_RESOLUTION, "1024"));
		outQuality = Integer.parseInt(options.getString(Options.PREF_QUALITY, "85"));
		sequencePos = 1;
		
		curTime = System.currentTimeMillis();
		cleanCache(a, curTime);
		curCacheDir = null;
		String base = getCacheDir(a).getPath() + "/" + curTime;
		for (int i = 0 ; i < 1000000 ; i++) {
			String n = base + "-" + i;
			if (! new File(n).exists()) {
				curCacheDir = n;
				break;
			}
		}
		if (curCacheDir == null) 
			curCacheDir = getCacheDir(a).getPath() + "/0";
		new File(curCacheDir).mkdirs();
	}
	
	public Uri reduce(Uri uri) {
		ReducedImage image = new ReducedImage(uri);
		if (image.bmp == null)
			return null;
		SendReduced.log("Reduced to "+image.bmp.getWidth()+"x"+image.bmp.getHeight());
		String path = image.saveImage();
		if (path == null) 
			return null;
		return Uri.fromFile(new File(path));		
	}
	
	public boolean sendReduced(Uri uri) {
		Uri out = reduce(uri);
		if (out == null)
			return false;
		Intent i = new Intent(android.content.Intent.ACTION_SEND);
		i.putExtra(android.content.Intent.EXTRA_STREAM, out);
		i.setType(MIME_TYPE);
		i.putExtra(Intent.EXTRA_TEXT, " ");
		i.putExtra(INTENT_FROM_ME, true);
		activity.startActivity(i);
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
	
	public static void cleanCache(Context c, Long curTime) {		
		SendReduced.log("cleanCache "+getCacheDir(c));
		File[] dirs = getCacheDir(c).listFiles(new FileFilter(){

			@Override
			public boolean accept(File f) {
				return f.isDirectory();
			}});

		// previous version files
		for (File f : getCacheDir(c).listFiles()) {
			if (f.getPath().toLowerCase().endsWith(".jpg"))
				f.delete();
		}
		
		for (File d : dirs) {
			String n = d.getName();
			try {
				SendReduced.log("checking "+n);
				long t = Long.parseLong(n.split("-")[0]);
				SendReduced.log("parsed to "+t+" vs "+curTime);
				if (Math.abs(t-curTime) >= CLEAN_TIME) {
					SendReduced.log("cleaning up "+d.getPath());
					for (File f : d.listFiles())
						f.delete();
					d.delete();
				}
			}
			catch(Exception e) {
			}
		}
	}

	static private int getOrientation(ContentResolver cr, Uri uri) {
		Cursor c = null;
		try {
			c = android.provider.MediaStore.Images.Media.query(cr, uri, 
						new String[]{MediaStore.Images.Media.ORIENTATION});
		}
		catch (Exception e) {}
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

	public Uri offerReduced(Uri uriIn) {
		ContentResolver cr = activity.getContentResolver();
		int orientation = getOrientation(cr, uriIn);
		SendReduced.log("orientation: "+orientation);
		ReducedImage image = new ReducedImage(uriIn);
		if (image.bmp == null)
			return null;
		SendReduced.log("Reduced to "+image.bmp.getWidth()+"x"+image.bmp.getHeight());
		String path = image.saveImage();
		if (path == null) 
			return null;
		else
			return Uri.fromFile(new File(path));
	}

	class ReducedImage {
		long origDate;
		Bitmap bmp;
		String origName;
		private boolean preserveExifLocation;
		private boolean preserveExifMake;
		private boolean preserveExifDate;
		private boolean preserveExifSettings;
		private boolean haveExif;
		private final String[] DATETIME_TAGS = { ExifInterface.TAG_DATETIME }; // TODO? GPS Time? No: That might give location away
		private final String[] LOCATION_TAGS = { ExifInterface.TAG_GPS_ALTITUDE, ExifInterface.TAG_GPS_ALTITUDE_REF,
			ExifInterface.TAG_GPS_LATITUDE, ExifInterface.TAG_GPS_LATITUDE_REF, ExifInterface.TAG_GPS_LONGITUDE,
			ExifInterface.TAG_GPS_LONGITUDE_REF, ExifInterface.TAG_GPS_PROCESSING_METHOD };
		private final String[] MAKE_TAGS = { ExifInterface.TAG_MAKE, ExifInterface.TAG_MODEL };
		private final String[] SETTINGS_TAGS = { ExifInterface.TAG_APERTURE, ExifInterface.TAG_EXPOSURE_TIME, ExifInterface.TAG_FLASH, ExifInterface.TAG_FOCAL_LENGTH, 
				ExifInterface.TAG_ISO, ExifInterface.TAG_WHITE_BALANCE };
		
		private Map<String,String> exifLocation;
		private Map<String,String> exifMake;
		private Map<String,String> exifDate;
		private Map<String,String> exifSettings;
		
		public ReducedImage(Uri uri) {
			preserveExifLocation = options.getBoolean(Options.PREF_EXIF_LOCATION, false);
			preserveExifMake = options.getBoolean(Options.PREF_EXIF_MAKE_MODEL, false);
			preserveExifDate = options.getBoolean(Options.PREF_EXIF_DATETIME, false);
			preserveExifSettings = options.getBoolean(Options.PREF_EXIF_SETTINGS, false);
			haveExif = SendReduced.pro(activity) && ( preserveExifLocation || preserveExifMake || 
					preserveExifDate || preserveExifSettings );

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
			
			origDate = -1;
			origName = null;
			String origPath = null;
			
			if (uri.getScheme().equalsIgnoreCase("file")) {
				origPath = uri.getPath();
				File f = new File(origPath);
				origDate = f.lastModified();
				origName = f.getName();
			}
			else if (uri.getScheme().equalsIgnoreCase("content")) {
				try {
					Cursor c = cr.query(uri, null, null, null, null);
					if (c != null) {
						if (c.moveToFirst()) {
							try {
								int id = c.getColumnIndex(Images.Media.DATA);
								if (id != -1) {
									origPath = c.getString(id);
									origName = new File(origPath).getName();
								}
	
								id = c.getColumnIndex(Images.Media.DATE_ADDED);
								if (id != -1) {
									origDate = c.getLong(id)  * 1000;
								}
								else {
									id = c.getColumnIndex(Images.Media.DATE_MODIFIED);
									if (id != -1)
										origDate = c.getLong(id) * 1000;
								}
							}
							catch(Exception e) {
								SendReduced.log("Error "+e);
							}
						}
						c.close();
					}
				}
				catch(Exception e) {					
					SendReduced.log("Error "+e);
				}
			}
			
			SendReduced.log("Orig date "+origDate);
			if (origName != null)
				SendReduced.log("Name "+origName);
			SendReduced.log("need to decode "+data.length+" bytes");

			if (origPath == null)
				haveExif = false;
			
			if (haveExif) {
				try {
					ExifInterface ei = new ExifInterface(origPath);
					if (preserveExifDate)
						exifDate = getTags(ei, DATETIME_TAGS);
					if (preserveExifLocation)
						exifDate = getTags(ei, LOCATION_TAGS);
					if (preserveExifMake)
						exifMake = getTags(ei, MAKE_TAGS);
					if (preserveExifSettings)
						exifSettings = getTags(ei, SETTINGS_TAGS);
				} catch (IOException e) {
				}
			}
			
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
			
			if (h > outResolution || w > outResolution) {
				if (h>w)
					m.postScale(outResolution / (float)h, outResolution / (float)h);
				else
					m.postScale(outResolution / (float)w, outResolution / (float)w);
				transform = true;
			}
			
			if (INVALID_ROTATION != o) {
				m.postRotate(o);
				transform = true;
			}
					
			SendReduced.log("image: "+w+"x"+h+" ["+o+"]");

			if (transform) {
				bmp = Bitmap.createBitmap(inBmp, 0, 0, w, h, m, true);
				inBmp.recycle();
			}
			else
				bmp = inBmp;
		}
		
		private Map<String, String> getTags(ExifInterface ei,
				String[] tags) {
			Map<String, String> map = new HashMap<String, String>();
			for (String tag : tags)
				map.put(tag, ei.getAttribute(tag));
			return map;
		}

		private void setTags(ExifInterface ei,
				Map<String, String> map) {
			if (map == null)
				return;
			for (String tag : map.keySet()) {
				String v = map.get(tag);
				if (v != null)
					ei.setAttribute(tag, map.get(tag));
			}
		}

		File createOutFile() throws IOException {
			String mode = options.getString(Options.PREF_NAME, Options.OPT_NAME_RANDOM);
			File outFile = null;
			SendReduced.log(mode);

			if (mode.equals(Options.OPT_NAME_SEQUENTIAL)) {
				outFile = new File(curCacheDir + "/" + String.format("%04d.jpg", sequencePos));
				sequencePos++;
			}
			else if (mode.equals(Options.OPT_NAME_DATE_TIME) && origDate > 0) {
				SendReduced.log("zz "+origDate);
				Date d = new Date(origDate);
				String base = curCacheDir + "/" + new SimpleDateFormat("yyyyMMDD_HHmmss").format(d);
				File f = new File(base+".jpg");
				if (! f.exists()) {
					outFile = f;					
				}
				else {
					for (int i = 2 ; i < 100000 ; i++) {
						f = new File(base+"-"+i+".jpg");
						if (! f.exists()) {
							outFile = f;
							break;
						}
					}
				}
			}
			else if (mode.equals(Options.OPT_NAME_PRESERVE) && origName != null) {
				if (origName.toLowerCase().endsWith(".jpg") || origName.toLowerCase().endsWith(".jpeg")) {
					outFile = new File(curCacheDir + "/" + origName);
				}
				else {
					outFile = new File(curCacheDir + "/" + origName + ".jpg");
				}
			}
			
			if (outFile != null) {
				SendReduced.log("Trying "+outFile);
				try {
					if (outFile.createNewFile())
						return outFile;
				} catch (IOException e) {
				}
			}
			return File.createTempFile(PREFIX, ".jpg", new File(curCacheDir));
		}

		String saveImage() {
			try {
				File temp = createOutFile(); //File.createTempFile(PREFIX, ".jpg", storage);
				SendReduced.log("Compressing to "+temp);
				FileOutputStream out = new FileOutputStream(temp);
				boolean status = bmp.compress(Bitmap.CompressFormat.JPEG, outQuality, out);
				out.close();
				if (!status) {
					temp.delete();
					return null;
				}
				if (haveExif) {
					ExifInterface ei = new ExifInterface(temp.getPath());
					setTags(ei, exifDate);
					setTags(ei, exifLocation);
					setTags(ei, exifMake);
					setTags(ei, exifSettings);
					ei.saveAttributes();
				}
				return temp.getPath();
			} catch (IOException e) {
				SendReduced.log("Error writing "+e);
				return null;
			}
			finally {
				bmp.recycle();
			}
			
		}
		

	}
}
