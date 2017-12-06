package org.servalproject.group.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import org.servalproject.ServalBatPhoneApplication;
import org.servalproject.group.Group;
import org.servalproject.group.GroupCommunicationController;
import org.servalproject.group.GroupDAO;
import org.servalproject.group.GroupMember;
import org.servalproject.group.GroupMessage;
import static org.servalproject.group.GroupMessage.Type.*;
import org.servalproject.servaldna.keyring.KeyringIdentity;

public class SubKeyRequestService extends Service {
    private static final String TAG = "SubKeyRequestService";
    private ServalBatPhoneApplication app;
    private KeyringIdentity identity;
    private String mySid;
    private Group myGroup;
    private GroupMember groupMemberMe;
    private GroupDAO groupDAO;
    private GroupCommunicationController groupCController;


    public static final String ACTION = "action";
    public static final String ACTION_SUBKEY_REQUEST = "sub_key_request";

    public SubKeyRequestService() {
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "create secure service");
        try {
            app = ServalBatPhoneApplication.context;
            identity = app.server.getIdentity();
            mySid = identity.sid.toString();
            groupDAO = new GroupDAO(getApplicationContext(), mySid);
            groupCController = new GroupCommunicationController(app, identity);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            app.displayToastMessage(e.getMessage());

        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle bundle = intent.getExtras();
        String action = (String) bundle.get(ACTION);
        String groupName = (String) bundle.get("group_name");
        String leader = (String) bundle.get("leader");
        myGroup = groupDAO.getSecureGroup(groupName, leader);
        groupMemberMe = groupDAO.getMember(groupName, leader, mySid);

        if (action != null && groupName != null && leader != null) {
            if (action.equals(ACTION_SUBKEY_REQUEST)) {
                SubKeyRequestService.SubKeyRequestTask task = new SubKeyRequestService.SubKeyRequestTask(this);
                task.execute(groupName, leader);
            }
        }
        stopSelf();
        return START_STICKY;
    }

    private class SubKeyRequestTask extends AsyncTask<String, Void, Void> {
        private Context mContext;
        private GroupDAO groupDAO;

        public SubKeyRequestTask(Context context) {
            mContext = context;
        }

        @Override
        protected Void doInBackground(String... args) {
            String groupName = args[0];
            String leader = args[1];

            String message = GroupMessage.generateMulticastGroupMessage(
                    SUB_KEY_REQUEST,
                    myGroup,
                    groupMemberMe,
                    "");
            groupCController.multicast(groupMemberMe, myGroup, message);
            return null;
        }

    }

}
