/**
 * Copyright (C) 2011 The Serval Project
 *
 * This file is part of Serval Software (http://www.servalproject.org)
 *
 * Serval Software is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.servalproject;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import org.servalproject.ServalBatPhoneApplication.State;
import org.servalproject.batphone.CallDirector;
import org.servalproject.rhizome.RhizomeMain;
import org.servalproject.servald.PeerListService;
import org.servalproject.servald.ServalD;
import org.servalproject.servaldna.keyring.KeyringIdentity;
import org.servalproject.ui.Networks;
import org.servalproject.ui.ShareUsActivity;
import org.servalproject.ui.help.HtmlHelp;
import org.servalproject.wizard.Wizard;
import org.servalproject.group.activity.GroupActivity;
/**
 *
 * Main activity which presents the Serval launcher style screen. On the first
 * time Serval is installed, this activity ensures that a warning dialog is
 * presented and the user is taken through the setup wizard. Once setup has been
 * confirmed the user is taken to the main screen.
 *
 * @author Paul Gardner-Stephen <paul@servalproject.org>
 * @author Andrew Bettison <andrew@servalproject.org>
 * @author Corey Wallis <corey@servalproject.org>
 * @author Jeremy Lakeman <jeremy@servalproject.org>
 * @author Romana Challans <romana@servalproject.org>
 */
public class Main extends Activity implements OnClickListener {
	public ServalBatPhoneApplication app;
	private static final String TAG = "Main";
	private TextView buttonToggle;
	private ImageView buttonToggleImg;
	private Drawable powerOnDrawable;
	private Drawable powerOffDrawable;

	private void openMaps() {
		// check to see if maps is installed
		try {
			PackageManager mManager = getPackageManager();
			mManager.getApplicationInfo("org.servalproject.maps",
					PackageManager.GET_META_DATA);

			Intent mIntent = mManager
					.getLaunchIntentForPackage("org.servalproject.maps");
			mIntent.addCategory(Intent.CATEGORY_LAUNCHER);
			startActivity(mIntent);

		} catch (NameNotFoundException e) {
			startActivity(new Intent(getApplicationContext(),
					org.servalproject.ui.MapsActivity.class));
		}
	}

	@Override
	public void onClick(View view) {
		// Do nothing until upgrade finished.
		if (app.getState() != State.Running)
			return;

		switch (view.getId()){
		case R.id.powerLabel:
			startActivity(new Intent(getApplicationContext(),
					Networks.class));
			break;
        case R.id.groupLabel:
            startActivity(new Intent(getApplicationContext(),
                GroupActivity.class));
            break;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.app = (ServalBatPhoneApplication) this.getApplication();

		setContentView(R.layout.main);

		// adjust the power button label on startup
		buttonToggle = (TextView) findViewById(R.id.btntoggle);
		buttonToggleImg = (ImageView) findViewById(R.id.powerLabel);
		buttonToggleImg.setOnClickListener(this);

		// load the power drawables
		powerOnDrawable = getResources().getDrawable(
				R.drawable.ic_wifi_black_48dp);
		powerOffDrawable = getResources().getDrawable(
				R.drawable.ic_signal_wifi_off_black_48dp);

		int listenTo[] = {
            R.id.groupLabel
		};
		for (int i = 0; i < listenTo.length; i++) {
			this.findViewById(listenTo[i]).setOnClickListener(this);
		}
	}

	BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int stateOrd = intent.getIntExtra(
					ServalBatPhoneApplication.EXTRA_STATE, 0);
			State state = State.values()[stateOrd];
			stateChanged(state);
		}
	};

	boolean registered = false;

	private void stateChanged(State state) {
		switch (state){
			case Running: case Upgrading: case Starting:
				// change the image for the power button
				buttonToggleImg.setImageDrawable(
						app.isEnabled()?powerOnDrawable:powerOffDrawable);
				break;
			case RequireDidName: case NotInstalled: case Installing:
				this.startActivity(new Intent(this, Wizard.class));
				finish();
				app.startBackgroundInstall();
				break;
			case Broken:
				// TODO display error?
				break;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (!registered) {
			IntentFilter filter = new IntentFilter();
			filter.addAction(ServalBatPhoneApplication.ACTION_STATE);
			this.registerReceiver(receiver, filter);
			registered = true;
		}

		stateChanged(app.getState());
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (registered) {
			this.unregisterReceiver(receiver);
			registered = false;
		}
	}
}
