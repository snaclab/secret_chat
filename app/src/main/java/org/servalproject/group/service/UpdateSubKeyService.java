package org.servalproject.group.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;

import org.servalproject.ServalBatPhoneApplication;
import org.servalproject.group.Group;
import org.servalproject.group.GroupCommunicationController;
import org.servalproject.group.GroupDAO;
import org.servalproject.group.GroupMember;
import org.servalproject.group.GroupMessage;
import static org.servalproject.group.GroupMessage.Type.*;
import org.servalproject.group.secret.SecretController;
import org.servalproject.group.secret.SecretKey;
import org.servalproject.group.secret.SubKey;
import org.servalproject.servaldna.keyring.KeyringIdentity;

import java.util.ArrayList;

public class UpdateSubKeyService extends Service {
    public static final String PREFS_NAME = "org.servalproject.group.PrefsFile";
    public static final String THRESHOLD_MODE= "secret_sharing_threshold_mode";
    public static final String THRESHOLD_VALUE = "secret_sharing_threshold_value";
    public static final String THRESHOLD_MODE_DEFAULT = "fix";
    public static final String THRESHOLD_VALUE_DEFAULT = "2";
    private ServalBatPhoneApplication app;
    private KeyringIdentity identity;
    private String mySid;
    private String groupName;
    private String leader;
    private ArrayList<SubKey> subKeyList;
    private SecretKey masterKey;
    private SecretController controller;
    private GroupCommunicationController groupCController;
    private Group myGroup;
    private GroupMember groupMemberMe;
    private GroupDAO groupDAO;
    private String thresholdMode;
    private String thresholdValue;

    public UpdateSubKeyService() {
    }
    @Override
    public void onCreate() {
        try {
            app = ServalBatPhoneApplication.context;
            identity = app.server.getIdentity();
            groupCController = new GroupCommunicationController(app, identity);
            mySid = identity.sid.toString();
            groupDAO = new GroupDAO(getApplicationContext(), mySid);
        } catch (Exception e) {
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
        groupName = (String) bundle.get("group_name");
        leader = (String) bundle.get("leader");
        thresholdMode = groupDAO.getGroupThresholdMode(groupName, leader).get("mode");
        thresholdValue = groupDAO.getGroupThresholdMode(groupName, leader).get("value");
        masterKey = new SecretKey((String) bundle.get("master_key"));
        myGroup = groupDAO.getSecureGroup(groupName, leader);
        groupMemberMe = groupDAO.getMember(groupName, leader, mySid);

        if ( updateSubKey()) {
            myGroup = groupDAO.getSecureGroup(groupName, leader);
            UpdateSecureGroupTask task = new UpdateSecureGroupTask(this);
            task.execute();
            groupDAO.setUpToDate(groupName, leader);
        }

        stopSelf();
        return START_STICKY;
    }
    private class UpdateSecureGroupTask extends AsyncTask<String, Void, Void> {
        private Context mContext;

        public UpdateSecureGroupTask(Context context) {
            mContext = context;
        }

        @Override
        protected Void doInBackground(String... args) {
            ArrayList<GroupMember> memberList = myGroup.getMembersExcludeLeader();
            for (GroupMember member : memberList) {
                Integer index = member.getSubKey().getIndex();
                for (SubKey subkey: subKeyList) {
                    if (subkey.getIndex().equals(index)) {
                        member.setSubKey(subkey);
                        break;
                    }
                }

                String message = GroupMessage.generateUnicastGroupMessage(
                        SECURE_GROUP_UPDATE,
                        myGroup,
                        groupMemberMe,
                        member);
                Long timestamp = System.currentTimeMillis();
                groupDAO.insertMessage(
                        new GroupMessage(SECURE_GROUP_UPDATE,
                                mySid,
                                "",
                                groupName,
                                timestamp,
                                1,
                                message,
                                leader)
                );
                groupCController.unicast(mySid, member.getSid(), message);
            }

            app.sendBroadcast(new Intent(GroupMessageService.NEW_CHAT_MESSAGE_ACTION));
            app.sendBroadcast(new Intent(GroupMessageService.UPDATE_GROUP_ACTION));
            return null;
        }

    }

    public boolean updateSubKey() {
        if (masterKey.getKey() != null) {
            Integer n = groupDAO.getGroupSize(groupName, leader);
            Integer k = 2;
            if(thresholdMode.equals("fix")) {
                k = Math.min(Integer.valueOf(thresholdValue), n);
            } else if (thresholdMode.equals("percentage")){
                Double threshold = Math.ceil(n * Double.valueOf(thresholdValue));
                k = Math.max(threshold.intValue(), 2);
            }
            if (n <= 1) {
                return false;
            }
            controller = new SecretController();
            controller.setMasterKey(masterKey);
            controller.generatePublicInfo(n, k);
            subKeyList = controller.generateSubKeyList();
            groupDAO.increaseSubKeyVersion(groupName, leader);
            groupDAO.setNumberOfTotalKeys(groupName, leader, n);
            groupDAO.setKeyThreshold(groupName, leader, k);
            groupDAO.setupSubKeys(groupName, leader, subKeyList);
            return true;
        } else {
            return false;
        }
    }
}
