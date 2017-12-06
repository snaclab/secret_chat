/* Copyright (C) 2012 The Serval Project
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

/**
 * Settings - Accounts Settings screen
 *
 * @author Romana Challans <romana@servalproject.org>
 */

package org.servalproject.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.servalproject.R;
import org.servalproject.ServalBatPhoneApplication;
import org.servalproject.servaldna.keyring.KeyringIdentity;

public class AccountsSettingsActivity extends Activity {

	private static final String TAG = "AccountSettings";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.accountssetting);

		// Accounts Settings Screen
		Button btnphoneReset = (Button) this.findViewById(R.id.btnphoneReset);
		btnphoneReset.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				AccountsSettingsActivity.this.startActivity(new Intent(
						AccountsSettingsActivity.this,
						org.servalproject.wizard.SetPhoneNumber.class));

			}
		});


		// Set Textviews and blank strings
		TextView acSID = (TextView) this.findViewById(R.id.acsid);

		String PNid = "There is no phone number to display";
		String SIDid = "There is no ServalID to display";
		String NMid = "There is no name to display";

		try {
			KeyringIdentity identity = ServalBatPhoneApplication.context.server.getIdentity();
			if (identity.did !=null)
				PNid = identity.did;
			if (identity.name !=null)
				NMid = identity.name;
			if (identity.sid !=null)
				SIDid = identity.sid.abbreviation();
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
		}

		// set values to display
		acSID.setText(SIDid); // Serval ID

	}
}
