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
import org.servalproject.group.activity.GroupActivity;
import org.servalproject.group.activity.GroupChatActivity;
import org.servalproject.servaldna.keyring.KeyringIdentity;

import java.util.ArrayList;

import static org.servalproject.group.GroupMessage.Type.CHANGE_LEADER;

/**
 * Created by sychen on 2017/5/25.
 */

public class ChangeLeaderService extends Service {
    public static final String CHANGE_LEADER_RESPONSE = "change_leader_response";
    private static final String TAG = "ChangeLeaderService";
    private ServalBatPhoneApplication app;
    private KeyringIdentity identity;
    private String mySid;
    private String groupName;
    private String leader;
    private String masterKey;
    private GroupCommunicationController groupCController;
    private Group myGroup;
    private GroupMember groupMemberMe;
    private GroupDAO groupDAO;
    private int count_responses = 0;
    private int threshold_responses;

    BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(CHANGE_LEADER_RESPONSE)) {
                Log.d(TAG, "got new response");
                Bundle bundle = intent.getExtras();
                String groupNameIntent = (String) bundle.get("group_name");
                String leaderIntent = (String) bundle.get("leader");

                if (groupName.equals(groupNameIntent) &&
                        leader.equals(leaderIntent)) {
                    count_responses += 1;
                    if (count_responses >= threshold_responses ) {
                        Log.d(TAG, "responses is enough");
                        updateSubKey(groupName, mySid, ChangeLeaderService.this);
                        stopSelf();
                    }
                }
            }
        }


    };
    public ChangeLeaderService () {

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

        changeLeader();

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
            filter.addAction(CHANGE_LEADER_RESPONSE);
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


    private void changeLeader() {
        String text =
                GroupMessage.generateUnicastGroupMessage(
                        CHANGE_LEADER, groupName, leader, mySid);
        ArrayList<GroupMember> members = myGroup.getMembersExcludeLeader();
        for(GroupMember member : members) {
            if (!member.getSid().equals(mySid)) {
                groupCController.unicast(mySid, member.getSid(), text);
                groupDAO.insertMessage(new GroupMessage(CHANGE_LEADER, mySid, member.getSid(),
                        groupName, System.currentTimeMillis(), 1, "", leader));
            }
        }
        groupDAO.changeLeader(groupName, leader, mySid);
        app.sendBroadcast(new Intent(GroupChatActivity.FINISH_CHAT));
        app.displayToastMessage("THe group was re-created successfully");
    }

    private boolean updateSubKey(String groupName, String leader, Context context) {
        if(!masterKey.equals("")) {
            Intent intent = new Intent(context, UpdateSubKeyService.class);
            intent.putExtra("group_name", groupName);
            intent.putExtra("leader", leader);
            intent.putExtra("master_key", masterKey);
            startService(intent);
            return true;
        } else {
            app.displayToastMessage("No secret key yet.");
            return false;
        }
    }
}
