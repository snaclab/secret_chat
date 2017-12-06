package org.servalproject.group.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import org.servalproject.ServalBatPhoneApplication;
import org.servalproject.group.Group;
import org.servalproject.group.GroupCommunicationController;
import org.servalproject.group.GroupDAO;
import org.servalproject.group.GroupMember;
import org.servalproject.group.secret.SecretController;
import org.servalproject.group.secret.SecretKey;
import org.servalproject.group.secret.SubKey;
import org.servalproject.servaldna.keyring.KeyringIdentity;

import java.util.ArrayList;

public class SecretKeyService extends Service {
    public static final String ACTION = "action";
    public static final String NEW_SUB_KEY_RESPONSE = "new_sub_key_response";
    public static final String MASTER_KEY = "master_key";
    private static final String TAG = "SecretKeyService";
    private ArrayList<SubKey> subKeyList = new ArrayList<SubKey>();
    private String groupName;
    private String leader;
    private GroupDAO groupDAO;
    private ServalBatPhoneApplication app;
    private KeyringIdentity identity;
    private String mySid;
    private GroupMember groupMemberMe;
    private GroupCommunicationController groupCController;
    private Group myGroup;
    private SubKey mySubKey;
    private Integer keyThreshold;
    private Integer numberOfTotalKeys;
    private String oriText;


    BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(SecretKeyService.NEW_SUB_KEY_RESPONSE)) {
                Bundle bundle = intent.getExtras();
                String groupNameIntent = (String) bundle.get("group_name");
                String leaderIntent = (String) bundle.get("leader");
                if (groupName.equals(groupNameIntent) &&
                        leader.equals(leaderIntent)) {
                    Log.d(TAG, "received sub key ");
                    String subKeyIndex = (String) bundle.get("subKeyIndex");
                    String subKey = (String) bundle.get("subKey");
                    subKeyList.add(new SubKey(subKey, Integer.valueOf(subKeyIndex)));
                    if (subKeyList.size() >= keyThreshold) {
                        SecretController secretController = new SecretController();
                        secretController.generatePublicInfo(numberOfTotalKeys, keyThreshold);
                        SecretKey masterKey = secretController.reconstructMasterKey(subKeyList);
                        if(!masterKey.getKey().equals("")) {
                            Intent intentMasterKey = new Intent(SecretKeyService.MASTER_KEY);
                            intentMasterKey.putExtra("master_key", masterKey.getKey());
                            app.sendBroadcast(intentMasterKey);
                            stopSelf();
                        }
                    }
                }
            }
        }

    };

    public SecretKeyService() {
    }

    @Override
    public void onCreate() {
        try {
            Log.d(TAG, "create service");
            app = ServalBatPhoneApplication.context;
            identity = app.server.getIdentity();
            mySid = identity.sid.toString();
            groupCController = new GroupCommunicationController(app, identity);
            groupDAO = new GroupDAO(getApplicationContext(), mySid);
            IntentFilter filter = new IntentFilter();
            filter.addAction(SecretKeyService.NEW_SUB_KEY_RESPONSE);
            this.registerReceiver(receiver, filter);
        } catch (Exception e ) {
            app.displayToastMessage(e.getMessage());

        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "start service");
        Bundle bundle = intent.getExtras();
        groupName = (String) bundle.get("group_name");
        leader = (String) bundle.get("leader");
        oriText = (String) bundle.get("text");
        groupMemberMe = groupDAO.getMember(groupName, leader, mySid);
        myGroup = groupDAO.getSecureGroup(groupName, leader);

        mySubKey = groupMemberMe.getSubKey();
        keyThreshold = myGroup.getKeyThreshold();
        numberOfTotalKeys = myGroup.getNumberOfTotalKeys();
        subKeyList.add(mySubKey);

        if (groupName != null && leader != null) {
        } else {
            stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "destroy service");
        this.unregisterReceiver(receiver);
        super.onDestroy();
    }

}
