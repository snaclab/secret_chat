package org.servalproject.rhizome;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import org.servalproject.R;
import org.servalproject.ServalBatPhoneApplication;
import org.servalproject.servald.Peer;
import org.servalproject.servaldna.ServalDCommand;
import org.servalproject.servaldna.keyring.KeyringIdentity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ShareFileActivity extends Activity {
	private static final String TAG = "ShareActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		String action = intent.getAction();

		// if this is from the share menu
		if (Intent.ACTION_SEND.equals(action)) {
			Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
			if (uri == null)
				uri = intent.getData();

			String text = intent.getStringExtra(Intent.EXTRA_TEXT);
			String type = intent.getType();
			Bundle extras = intent.getExtras();

			boolean displayToast = intent
					.getBooleanExtra("display_toast", true);

			if (extras != null && false) {
				for (String key : extras.keySet()) {
					Log.v(this.getClass().getName(),
							"Extra " + key + " = " + extras.get(key));
				}
			}

			if (text!=null){
				// Does the text include a market uri??
				// TODO - check that this still works with Google Play
				try{
					String marketUrl = "http://market.android.com/search?q=pname:";
					int x = text.indexOf(marketUrl);
					if (x>0){
						int end = text.indexOf(' ',x);
						if (end<0) end = text.length();
						String appPackage = text.substring(x + marketUrl.length(), end);
						Log.v(this.getClass().getName(), "App Package? \""
								+ appPackage + "\"");
						ApplicationInfo info = this.getPackageManager().getApplicationInfo(appPackage, 0);
						uri = Uri.fromFile(new File(info.sourceDir));
					}
				}catch(Exception e){
					Log.e(TAG, "Failed to parse "+text+"\n"+e.getMessage(), e);
				}
			}

			if (uri != null) {
				try {

					// Get resource path from intent callee
					String fileName = getRealPathFromURI(this, uri);
					File file = new File(fileName);

					Log.v(this.getClass().getName(), "Sharing " + fileName
							+ " ("
							+ uri + ")");

					addFile(this, file, displayToast);

				} catch (Exception e) {
					Log.e(this.getClass().getName(), e.toString(), e);
					ServalBatPhoneApplication.context.displayToastMessage(e
							.toString());
				}

			} else if (text != null) {
				Log.v(this.getClass().getName(), "Text content: \"" + text
						+ "\" (" + type
						+ ")");
				ServalBatPhoneApplication.context
						.displayToastMessage("sending of text not yet supported");
			} else {
				ServalBatPhoneApplication.context
						.displayToastMessage("Unable to send content, No uri or text found");
			}
		} else {
			ServalBatPhoneApplication.context.displayToastMessage("Intent "
					+ action + " not supported!");
		}
		finish();
	}

	static void addFile(final Context context, final File file,
			final boolean displayToast) {
		new AsyncTask<Void, Peer, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					List<String> args = new ArrayList<String>();

					if (file.getName().toLowerCase().endsWith(".apk")) {
						PackageManager pm = context.getPackageManager();
						PackageInfo info = pm.getPackageArchiveInfo(
								file.getAbsolutePath(), 0);
						if (info != null) {
							args.add("version=" + info.versionCode);

							// see http://code.google.com/p/android/issues/detail?id=9151
							if (info.applicationInfo.sourceDir == null)
								info.applicationInfo.sourceDir = file.getAbsolutePath();
							if (info.applicationInfo.publicSourceDir == null)
								info.applicationInfo.publicSourceDir = file.getAbsolutePath();

							CharSequence label = info.applicationInfo.loadLabel(pm);

							if (label != null && !"".equals(label))
								args.add("name=" + label + ".apk");
							else
								args.add("name=" + info.packageName + ".apk");
						}
					}
					KeyringIdentity identity = ServalBatPhoneApplication.context.server.getIdentity();
					ServalDCommand.rhizomeAddFile(file, null, null, identity.sid, null, args.toArray(new String[args.size()]));

					if (displayToast) {
						ServalBatPhoneApplication.context
								.displayToastMessage(ServalBatPhoneApplication.context
										.getResources()
										.getText(
												R.string.rhizome_share_file_toast)
										.toString());
					}
				} catch (Exception e) {
					Log.e("ShareActivity", e.getMessage(), e);
				}
				return null;
			}
		}.execute();
	}

	public static String getRealPathFromURI(Context context, Uri contentUri) {
		if (contentUri.getScheme().equals("file")) {
			return contentUri.getPath();
		}

		// can post image
		String[] proj = { MediaStore.Images.Media.DATA };
		Cursor cursor = context.getContentResolver().query(
				contentUri, proj, // Which columns to return
				null, // WHERE clause; which rows to return (all rows)
				null, // WHERE clause selection arguments (none)
				null); // Order-by clause (ascending by name)
		try {
			int column_index = cursor
					.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();

			return cursor.getString(column_index);
		} finally {
			cursor.close();
		}
	}

	public static byte[] readBytes(InputStream inputStream) throws Exception {
		ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
		// this is storage overwritten on each iteration with bytes
		int bufferSize = 1024;
		byte[] buffer = new byte[bufferSize];
		int len = 0;
		while ((len = inputStream.read(buffer)) != -1) {
			byteBuffer.write(buffer, 0, len);
		}
		return byteBuffer.toByteArray();
	}
}
