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
import org.servalproject.group.GroupMessage;
import static org.servalproject.group.GroupMessage.Type.*;

import org.servalproject.group.activity.GroupChatActivity;
import org.servalproject.servaldna.keyring.KeyringIdentity;

import java.util.ArrayList;

public class ReCreateGroupRequestService extends Service {
    public static final String RE_CREATE_GROUP_RESPONSE = "re_create_group_response";
    private static final String TAG = "ReCreateGroup";
    private ServalBatPhoneApplication app;
    private KeyringIdentity identity;
    private String mySid;
    private String groupName;
    private String leader;
    private GroupCommunicationController groupCController;
    private Group myGroup;
    private GroupMember groupMemberMe;
    private GroupDAO groupDAO;
    private String masterKey;
    private int count_responses = 0;
    private int threshold_responses;

    BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(RE_CREATE_GROUP_RESPONSE)) {
                Log.d(TAG, "got new response");
                Bundle bundle = intent.getExtras();
                String groupNameIntent = (String) bundle.get("group_name");
                String leaderIntent = (String) bundle.get("leader");

                if (groupName.equals(groupNameIntent) &&
                        leader.equals(leaderIntent)) {
                    count_responses += 1;
                    if (count_responses >= threshold_responses ) {
                        Log.d(TAG, "responses is enough");
                        stopService(new Intent(ReCreateGroupRequestService.this, ChangeLeaderService.class));
                        Intent intentChangeLeader = new Intent(ReCreateGroupRequestService.this, ChangeLeaderService.class);
                        intentChangeLeader.putExtra("master_key", masterKey);
                        intentChangeLeader.putExtra("group_name", groupName);
                        intentChangeLeader.putExtra("leader", leader);
                        startService(intentChangeLeader);
                        stopSelf();
                        }
                    }
                }
            }


    };

    public ReCreateGroupRequestService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle bundle = intent.getExtras();
        groupName = (String) bundle.get("group_name");
        leader = (String) bundle.get("leader");
        masterKey = (String) bundle.get("master_key");
        myGroup = groupDAO.getSecureGroup(groupName, leader);
        groupMemberMe = groupDAO.getMember(groupName, leader, mySid);
        threshold_responses = groupDAO.getGroupSize(groupName, leader) - 2;
        if (threshold_responses <= 0) {
            stopSelf();
        } else {
            generateRequest();
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        try {
            app = ServalBatPhoneApplication.context;
            identity = app.server.getIdentity();
            groupCController = new GroupCommunicationController(app, identity);
            mySid = identity.sid.toString();
            groupDAO = new GroupDAO(getApplicationContext(), mySid);
            IntentFilter filter = new IntentFilter();
            filter.addAction(RE_CREATE_GROUP_RESPONSE);
            this.registerReceiver(receiver, filter);
        } catch (Exception e) {
            app.displayToastMessage(e.getMessage());

        }
    }

    @Override
    public void onDestroy() {
        this.unregisterReceiver(receiver);
        super.onDestroy();
    }

    private void generateRequest(){

        String message = GroupMessage.generateMulticastGroupMessage(
                RE_CREATE_GROUP_REQUEST,
                myGroup,
                groupMemberMe,
                "");
        groupCController.multicastExcludeLeader(groupMemberMe, myGroup, message);
        groupDAO.insertMessage(
                new GroupMessage(RE_CREATE_GROUP_REQUEST,
                        mySid,
                        "",
                        groupName,
                        System.currentTimeMillis(),
                        1,
                        "",
                        leader)
        );
    }


}
