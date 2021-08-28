package mobi.omegacentauri.SendReduced;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
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
import androidx.exifinterface.media.ExifInterface;
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
		if (fileProvider()) {
			return FileProvider.getUriForFile(activity, activity.getPackageName(), new File(path));
		}
		else
			return Uri.fromFile(new File(path));		
	}
	
	public boolean sendReduced(Uri uri) {
		Uri out = reduce(uri);
		if (out == null)
			return false;
		Intent i = new Intent(android.content.Intent.ACTION_SEND);
		i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		i.putExtra(android.content.Intent.EXTRA_STREAM, out);
		i.setType(MIME_TYPE);
//		i.putExtra(Intent.EXTRA_TEXT, " ");
		SendReduced.log("uri: "+out);
		startWithChooser(activity, new Uri[] { out }, i);
		return true;
	}
	
	static void startActivityForResultWithChooser(Activity activity, Intent i, String title, int code) {
		i.putExtra(Utils.INTENT_FROM_ME, true);
		activity.startActivityForResult(i, code);
	}
	
	static void startWithChooser(Activity activity, Uri[] grant, Intent i) {
		startWithChooser(activity, grant, i, "Share reduced photo with...");
	}
	
	@SuppressLint("WrongConstant")
	private static void startWithChooser(Activity activity, Uri[] grant, Intent i, String title) {
		i.putExtra(INTENT_FROM_ME, true);
		i.setFlags(PackageManager.MATCH_DEFAULT_ONLY); //TODO:fix
		if (grant != null) {
			List<ResolveInfo> possibles = activity.getPackageManager().queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY);
			for (ResolveInfo r : possibles) {
			    for (Uri uri : grant)
			    	activity.grantUriPermission(r.activityInfo.packageName, uri, 
			    			Intent.FLAG_GRANT_READ_URI_PERMISSION);
			}
		}

		if (PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(Options.PREF_INCLUDE_DIRECT, true)) {
			i = Intent.createChooser(i, title);
			if (Build.VERSION.SDK_INT >= 24) {
				i.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, new ComponentName[]{activity.getComponentName()});
			}
		}
		activity.startActivity(i);
	}
	
	public boolean fileProvider() {
		Boolean f = Options.useContentProvider(options);
		SendReduced.log("Using file provider? " + f);
		return f;
	}
	
	private static File getCacheDir(Context c) {
		File storage;

		if (Build.VERSION.SDK_INT >= 8)
			storage = c.getExternalCacheDir();
		else
			storage = new File(Environment.getExternalStorageDirectory().getPath()+"/SendReduced");
		
		storage.mkdirs();
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
					for (File f : d.listFiles()) {
						f.delete();
						try {
							c.revokeUriPermission(FileProvider.getUriForFile(c, c.getPackageName(), f), Intent.FLAG_GRANT_READ_URI_PERMISSION);
						} catch(Exception e) {}
					}
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
		int columnIndex = c.getColumnIndex(MediaStore.Images.Media.ORIENTATION);
		if (columnIndex == -1)
			return INVALID_ROTATION;
		int o = c.getInt(columnIndex);
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
//		int orientation = getOrientation(cr, uriIn);
//		SendReduced.log("orientation: "+orientation);
		ReducedImage image = new ReducedImage(uriIn);
		if (image.bmp == null)
			return null;
		SendReduced.log("Reduced to "+image.bmp.getWidth()+"x"+image.bmp.getHeight());
		String path = image.saveImage();
		if (path == null) 
			return null;
		else if (fileProvider())
			return FileProvider.getUriForFile(activity, activity.getPackageName(), new File(path));
		else
			return Uri.fromFile(new File(path));
	}

	private static class SuperStripStream extends OutputStream {
		private enum jpegState {
			DEFAULT,
			COPYING,
			SKIPPING,
			ECS,
			ECS_MARKER,
			MARKER,
			DONE,
			COPYING_BEFORE_ECS;
		};

		jpegState state = jpegState.DEFAULT;

		private final OutputStream rawStream;
		private int copyLength;
		private int skipLength;
		private int[] markerData = new int[18];
		private int markerPosition;
		private boolean haveJFIF = false;

		public SuperStripStream(OutputStream os) {
			this.rawStream = os;
			this.state = jpegState.DEFAULT;
		}

		int markerDataGet16Bits(int position) {
			return ((0xFF & markerData[position]) << 8) | (0xFF & markerData[position+1]);
		}

		@Override
		public void write(int b) throws IOException {
			switch(this.state) {
				case DEFAULT:
					if ((b & 0xFF) == 0xFF) {
						this.state = jpegState.MARKER;
						markerData[0] = b;
						markerPosition = 1;
					}
					break;
				case COPYING:
					this.rawStream.write(b);
					copyLength--;
					if (copyLength <= 0)
						this.state = jpegState.DEFAULT;
					break;
				case COPYING_BEFORE_ECS:
					this.rawStream.write(b);
					copyLength--;
					if (copyLength <= 0) {
						this.state = jpegState.ECS;
					}
					break;
				case SKIPPING:
					skipLength--;
					if (skipLength <= 0)
						this.state = jpegState.DEFAULT;
					break;
				case ECS_MARKER:
					this.rawStream.write(b);
					if ((b & 0xFF) == 0xD9) {
						this.state = jpegState.DONE;
					}
					else {
						this.state = jpegState.ECS;
					}
					break;
				case ECS:
					this.rawStream.write(b);
					if ((b & 0xFF) == 0xFF)
						this.state = jpegState.ECS_MARKER;
					break;
				case MARKER:
					boolean writeMarker = false;
					markerData[markerPosition] = b;
					markerPosition++;
					if (markerPosition == 2) {
						if ((b & 0xFF) == 0xD8) {
							writeMarker = true;
							this.state = jpegState.DEFAULT;
						}
					}
					else if (markerPosition == 4) {
						int type = markerData[1] & 0xff;
						if (0xE1 <= type && type <= 0xEF) {
							skipLength = markerDataGet16Bits(2)-2;
							this.state = jpegState.SKIPPING;
						}
						else if (0xE0 != type) {
							writeMarker = true;
							copyLength = markerDataGet16Bits(2)-2;
							if (0xDA == type) {
								this.state = jpegState.COPYING_BEFORE_ECS;
							}
							else {
								this.state = jpegState.COPYING;
							}
						}
						else if (0xE0 == type) {
							// APP0
							if (haveJFIF) {
								// JFXX
								skipLength = markerDataGet16Bits(2)-2;
								this.state = jpegState.SKIPPING;
							}
							else {
								haveJFIF = true;
							}
						}
					}
					else if (markerPosition == 18) {
						// JFIF
						markerData[16] = 0;
						markerData[17] = 0;
						skipLength = markerDataGet16Bits(2)-16;
						markerData[2] = 0;
						markerData[3] = 18-2;
						writeMarker = true;
						state = jpegState.SKIPPING;
					}

					if (writeMarker) {
						for (int i=0; i<markerPosition; i++)
							this.rawStream.write(markerData[i]);
					}

					if ((jpegState.SKIPPING == state && skipLength <= 0) || (jpegState.COPYING == state && copyLength <= 0))
						state = jpegState.DEFAULT;

					break;
				case DONE:
					break;
			}
		}

		@Override
		public void flush() throws IOException {
			this.rawStream.flush();
		}

		@Override
		public void close() throws IOException {
			this.rawStream.close();
		}
	}

	class ReducedImage {
		long origDate;
		Bitmap bmp;
		String origName;
		private boolean superStrip;
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
		private final String[] SETTINGS_TAGS = { ExifInterface.TAG_APERTURE_VALUE, ExifInterface.TAG_EXPOSURE_TIME, ExifInterface.TAG_FLASH, ExifInterface.TAG_FOCAL_LENGTH,
				ExifInterface.TAG_ISO_SPEED, ExifInterface.TAG_WHITE_BALANCE };

		private String mode;
		private Map<String,String> exifLocation;
		private Map<String,String> exifMake;
		private Map<String,String> exifDate;
		private Map<String,String> exifSettings;
		
		public ReducedImage(Uri uri) {
			Boolean pro = SendReduced.pro(activity);
			mode = pro ? options.getString(Options.PREF_NAME, Options.OPT_NAME_RANDOM) : Options.OPT_NAME_RANDOM;
			preserveExifLocation = options.getBoolean(Options.PREF_EXIF_LOCATION, false);
			preserveExifMake = options.getBoolean(Options.PREF_EXIF_MAKE_MODEL, false);
			preserveExifDate = options.getBoolean(Options.PREF_EXIF_DATETIME, false);
			preserveExifSettings = options.getBoolean(Options.PREF_EXIF_SETTINGS, false);
			superStrip = pro && options.getBoolean(Options.PREF_SUPER_STRIP, false);
			haveExif = pro && ( preserveExifLocation || preserveExifMake ||
					preserveExifDate || preserveExifSettings ) && ! superStrip;

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
				origName = f.getName();
				try {
					origDate = f.lastModified();
				}
				catch(Exception e) {}
			}

			if (origDate < 0 || uri.getScheme().equalsIgnoreCase("content")) {
				try {
					Cursor c = cr.query(uri, null, null, null, null);
					if (c != null) {
						if (c.moveToFirst()) {
							try {
								int id = c.getColumnIndex(Images.Media.DATA);
								if (id != -1) {
									origPath = c.getString(id);
									if (origName != null)
										origName = new File(origPath).getName();
								}

								if (origDate < 0) {
									id = c.getColumnIndex(Images.Media.DATE_ADDED);
									SendReduced.log("date id "+id);
									if (id != -1) {
										origDate = c.getLong(id) * 1000;
									} else {
										id = c.getColumnIndex(Images.Media.DATE_MODIFIED);
										SendReduced.log("date id2 "+id);
										if (id != -1)
											origDate = c.getLong(id) * 1000;
									}
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

			int orientation = getOrientation(cr, uri);

			InputStream input = new ByteArrayInputStream(data);

			if (haveExif || orientation == INVALID_ROTATION || (origDate < 0 && mode.equals(Options.OPT_NAME_DATE_TIME)) ) {
				try {
					ExifInterface ei = new ExifInterface(input);
					if (orientation == INVALID_ROTATION) {
						try {
							int o = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
							SendReduced.log("exif orientation " + o);
							switch (o) {
								case ExifInterface.ORIENTATION_ROTATE_270:
									orientation = 270;
									break;
								case ExifInterface.ORIENTATION_ROTATE_180:
									orientation = 180;
									break;
								case ExifInterface.ORIENTATION_ROTATE_90:
									orientation = 90;
									break;
								case ExifInterface.ORIENTATION_NORMAL:
									orientation = 0;
									break;
							}
						} catch (Exception e) {
						}
					}

					if (origDate < 0) {
						String date = ei.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
						if (date == null) {
							date = ei.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED);
							if (date == null) {
								date = ei.getAttribute(ExifInterface.TAG_DATETIME);
							}
						}
						if (date != null) {
							origDate = parseExifDate(date);
						}
					}

					if (preserveExifDate)
						exifDate = getTags(ei, DATETIME_TAGS);
					if (preserveExifLocation)
						exifLocation = getTags(ei, LOCATION_TAGS);
					if (preserveExifMake)
						exifMake = getTags(ei, MAKE_TAGS);
					if (preserveExifSettings)
						exifSettings = getTags(ei, SETTINGS_TAGS);
				}
				catch (Exception e) {
					SendReduced.log("exif " + e);
				}
			}

			SendReduced.log("orientation = "+orientation);
			
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
			
			if (INVALID_ROTATION != orientation && orientation != 0) {
				m.postRotate(orientation);
				transform = true;
			}
					
			SendReduced.log("image: "+w+"x"+h+" ["+orientation+"]");

			if (transform) {
				bmp = Bitmap.createBitmap(inBmp, 0, 0, w, h, m, true);
				if (bmp != inBmp)
					inBmp.recycle();
			}
			else {
				bmp = inBmp;
			}
		}

		private long parseExifDate(String date) {
			SimpleDateFormat format = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
			try {
				Date d = format.parse(date);
				SendReduced.log("parsing "+date+" to "+d.toString());
				return d.getTime();
			} catch (ParseException e) {
				return -1;
			}
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
			File outFile = null;
			SendReduced.log(mode);

			if (mode.equals(Options.OPT_NAME_SEQUENTIAL)) {
				outFile = new File(curCacheDir + "/" + String.format("%04d.jpg", sequencePos));
				sequencePos++;
			}
			else if (mode.equals(Options.OPT_NAME_DATE_TIME) && origDate > 0) {
				Date d = new Date(origDate);
				String base = curCacheDir + "/" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(d);
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
				temp.setReadable(true, false);
				SendReduced.log("Compressing to "+temp);
				OutputStream out = new FileOutputStream(temp);
				if (superStrip)
					out = new SuperStripStream(out);
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
