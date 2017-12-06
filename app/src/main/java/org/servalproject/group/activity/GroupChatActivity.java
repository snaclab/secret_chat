package org.servalproject.group.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.EditText;

import org.servalproject.R;
import org.servalproject.ServalBatPhoneApplication;
import org.servalproject.group.Group;
import org.servalproject.group.GroupChat;
import org.servalproject.group.GroupChatListAdapter;
import org.servalproject.group.GroupCommunicationController;
import org.servalproject.group.GroupDAO;
import org.servalproject.group.GroupMember;
import org.servalproject.group.GroupMessage;
import org.servalproject.group.service.ChangeLeaderService;
import org.servalproject.group.service.GroupMessageService;
import static org.servalproject.group.GroupMessage.Type.*;
import static org.servalproject.group.GroupMember.Role.*;
import org.servalproject.group.service.ReCreateGroupRequestService;
import org.servalproject.group.secret.SecretController;
import org.servalproject.group.secret.SecretKey;
import org.servalproject.group.service.SecretKeyService;
import org.servalproject.group.service.SetupSecureGroupService;
import org.servalproject.group.service.SubKeyRequestService;
import org.servalproject.group.service.UpdateSubKeyService;
import org.servalproject.servaldna.keyring.KeyringIdentity;

import java.util.ArrayList;

public class GroupChatActivity extends Activity {

    private final String TAG = "GroupChatActivity";
    public static final String FINISH_CHAT = "finish_chat";
    private ServalBatPhoneApplication app;
    private KeyringIdentity identity;
    private String mySid;
    private GroupMember groupMemberMe;
    private Group myGroup;
    private ArrayList<GroupChat> chatList = new ArrayList<GroupChat>();
    private ArrayList<String> members = new ArrayList<String>();
    private ListView list;
    private Button buttonSendGroupMessage;
    private Button buttonShowGroupMember;
    private Button buttonSetupSecureGroup;
    private Button buttonReCreateGroup;
    private Button buttonUpdateSubkey;
    private Button buttonLeaveGroup;
    private Button buttonSetupThreshold;
    private EditText etGroupMessageContent;
    private TextView tvTitle;
    private TextView tvMasterKeyVersion;
    private TextView tvSubKeyVersion;
    private TextView tvCurrentGroupSize;
    private TextView tvCurrentThreshold;
    private String groupName;
    private String groupLeaderSid;
    private GroupDAO groupDAO;
    private GroupCommunicationController groupCController;
    private SecretController secretController;
    private String masterKey="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.group_chat);
        try {
            app = ServalBatPhoneApplication.context;
            identity = app.server.getIdentity();
            mySid = identity.sid.toString();
            buttonSendGroupMessage = (Button) findViewById(R.id.button_send_group_message);
            buttonShowGroupMember = (Button) findViewById(R.id.button_show_group_member);
            buttonSetupSecureGroup = (Button) findViewById(R.id.button_setup_secure_group);
            buttonReCreateGroup = (Button) findViewById(R.id.button_re_create_group);
            buttonUpdateSubkey = (Button) findViewById(R.id.button_update_sub_key);
            buttonLeaveGroup = (Button) findViewById(R.id.button_leave_group);
            buttonSetupThreshold = (Button) findViewById(R.id.button_setup_threshold);
            etGroupMessageContent = (EditText) findViewById(R.id.edit_text_group_message_content);
            tvTitle = (TextView) findViewById(R.id.group_chat_title);
            tvMasterKeyVersion = (TextView) findViewById(R.id.text_view_master_key_version);
            tvSubKeyVersion = (TextView) findViewById(R.id.text_view_sub_key_version);
            tvCurrentGroupSize = (TextView) findViewById(R.id.text_view_group_size);
            tvCurrentThreshold = (TextView) findViewById(R.id.text_view_current_threshold);
            list = (ListView) findViewById(R.id.list_view_group_chat);
            Intent intent = getIntent();
            groupName =  intent.getStringExtra("group_name");
            groupLeaderSid = intent.getStringExtra("leader_sid");
            getMasterKey(groupName, groupLeaderSid, this);
            String shortSid = groupLeaderSid;
            if(shortSid.equals(mySid))
                shortSid = "OWN";
            else if(shortSid.length() > 5)
                shortSid = shortSid.substring(0, 5) + "*";
            String title = groupName + "(" + shortSid + ")";
            tvTitle.setText(title);
            groupDAO= new GroupDAO(getApplicationContext(), mySid);
            secretController = new SecretController();
            groupCController = new GroupCommunicationController(app, identity);
            groupMemberMe = groupDAO.getMember(groupName, groupLeaderSid, mySid);
            myGroup = groupDAO.getSecureGroup(groupName, groupLeaderSid);
            renderTextView();
            setupButtonListener();
            updateGroupMembers();
        } catch(Exception e) {
            Log.e(TAG,e.getMessage(),e);
            app.displayToastMessage(e.getMessage());
            this.finish();
        }

    }

    @Override
    public void onResume() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(GroupMessageService.NEW_CHAT_MESSAGE_ACTION);
        filter.addAction(GroupMessageService.UPDATE_GROUP_ACTION);
        filter.addAction(SecretKeyService.MASTER_KEY);
        filter.addAction(FINISH_CHAT);
        this.registerReceiver(receiver, filter);
        populateList();
        renderButton();
        super.onResume();
    }

    @Override
    public void onPause() {
        this.unregisterReceiver(receiver);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        stopService(new Intent(this, ReCreateGroupRequestService.class));
        stopService(new Intent(this, SecretKeyService.class));
        super.onDestroy();
    }
    BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(GroupMessageService.NEW_CHAT_MESSAGE_ACTION)) {
                populateList();
            } else if(intent.getAction().equals(GroupMessageService.UPDATE_GROUP_ACTION)) {
                masterKey = "";
                getMasterKey(groupName, groupLeaderSid, GroupChatActivity.this);
                app.displayToastMessage("The Secret group has been updated.");
                updateGroupMembers();
                populateList();
                renderTextView();
                renderButton();
            } else if (intent.getAction().equals(SecretKeyService.MASTER_KEY)) {
                Bundle bundle = intent.getExtras();
                masterKey = (String) bundle.get("master_key");
                app.displayToastMessage("Get the secret key!");
                populateList();
            } else if (intent.getAction().equals(FINISH_CHAT)) {
                finish();
            }
        }

    };

    private void setupButtonListener() {

        buttonSendGroupMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = etGroupMessageContent.getText().toString();
                groupChat(text);
                etGroupMessageContent.setText("");
            }
        });

        buttonShowGroupMember.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GroupChatActivity.this, GroupMemberActivity.class);
                intent.putExtra("group_name", groupName);
                intent.putExtra("group_leader", groupLeaderSid);
                GroupChatActivity.this.startActivity(intent);
            }
        });


        if(groupDAO.isMyGroup(groupName, groupLeaderSid)) {
            buttonSetupSecureGroup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setupSecureGroup(groupName, groupLeaderSid, GroupChatActivity.this);
                }
            });
            buttonUpdateSubkey.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateSubKey(groupName, groupLeaderSid, GroupChatActivity.this);
                }
            });

            buttonSetupThreshold.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setupThreshold();

                }
            });
            buttonReCreateGroup.setEnabled(false);
            buttonReCreateGroup.setVisibility(View.GONE);
            buttonLeaveGroup.setEnabled(false);
            buttonLeaveGroup.setVisibility(View.GONE);
        } else {

            buttonSetupSecureGroup.setEnabled(false);
            buttonSetupSecureGroup.setVisibility(View.GONE);
            buttonUpdateSubkey.setEnabled(false);
            buttonUpdateSubkey.setVisibility(View.GONE);
            buttonSetupThreshold.setEnabled(false);
            buttonSetupThreshold.setVisibility(View.GONE);

            buttonReCreateGroup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    reCreateGroupRequest(groupName, groupLeaderSid);

                }
            });

            buttonLeaveGroup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    leaveGroupRequest();
                }
            });
        }

    }

    private void updateGroupMembers() {
        members = groupDAO.getOtherMemberList(groupName, groupLeaderSid);
    }

    private void populateList() {
        if(!app.isMainThread()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    populateList();
                }
            });
            return;
        }

        UpdateChatTask task = new UpdateChatTask(this);
        task.execute(masterKey);

    }

    private class UpdateChatTask extends AsyncTask<String, Void, ArrayList<GroupChat>> {
        private GroupDAO groupDAO;
        private Context context;

        public UpdateChatTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPostExecute(ArrayList<GroupChat> chatList) {
            GroupChatListAdapter adapter = new GroupChatListAdapter(context, chatList);
            list.setAdapter(adapter);
            list.setSelection(adapter.getCount() - 1);
        }

        @Override
        protected ArrayList<GroupChat> doInBackground(String... params) {
            String masterKey = params[0];
            groupDAO = new GroupDAO(getApplicationContext(), identity.sid.toString());
            ArrayList<GroupChat> chatList = new ArrayList<GroupChat>();
            chatList =  groupDAO.getChatList(groupName, groupLeaderSid);
            if(!masterKey.equals("")) {
                decryptGroupChat(chatList, masterKey);
            }
            return chatList;
        }
    }

    private boolean setupSecureGroup(String groupName, String leader, Context context) {
        Intent intent = new Intent(context, SetupSecureGroupService.class);
        intent.putExtra("group_name", groupName);
        intent.putExtra("leader", leader);
        startService(intent);
        return true;
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
    private void reCreateGroupRequest(String groupName, String leader) {
        if(groupDAO.getGroupId(groupName, mySid) != null) {
            app.displayToastMessage("The group already exists.");
            return;
        }
        if(!masterKey.equals("")) {
            Intent intent = new Intent(this, ReCreateGroupRequestService.class);
            intent.putExtra("group_name", groupName);
            intent.putExtra("leader", leader);
            intent.putExtra("master_key", masterKey);
            startService(intent);
        } else {
            app.displayToastMessage("No secret key yet.");

        }

    }

    private void leaveGroupRequest() {

                String text =
                        GroupMessage.generateUnicastGroupMessage(
                                LEAVE, groupName, groupLeaderSid,"");
                groupCController.unicast(mySid, groupLeaderSid, text);
                groupDAO.insertMessage(new GroupMessage(LEAVE, mySid, groupLeaderSid,
                        groupName, System.currentTimeMillis(), 1, "", groupLeaderSid));

    }


    private boolean subKeyRequest(String groupName, String leader, Context context) {
        Intent intent = new Intent(context, SubKeyRequestService.class);
        intent.putExtra(SubKeyRequestService.ACTION, SubKeyRequestService.ACTION_SUBKEY_REQUEST);
        intent.putExtra("group_name", groupName);
        intent.putExtra("leader", leader);
        startService(intent);
        return true;
    }
    private boolean getMasterKey(String groupName, String leader, Context context) {
        Log.d(TAG, "start getting master key");
        stopService(new Intent(context, SecretKeyService.class));
        Intent intent = new Intent(context, SecretKeyService.class);
        intent.putExtra("group_name", groupName);
        intent.putExtra("leader", leader);
        startService(intent);
        subKeyRequest(groupName, groupLeaderSid, this);
        return true;
    }

    private void setupThreshold() {

        if(groupDAO.getGroupSize(groupName, groupLeaderSid) < 2) {
            app.displayToastMessage("The group size is less than 2");
        } else {
            Intent intent = new Intent(this, GroupSettingActivity.class);
            intent.putExtra("group_name", groupName);
            intent.putExtra("leader_sid", groupLeaderSid);
            startActivity(intent);
        }

    }

    public void groupChat(String text) {
        if(!masterKey.equals("")) {
            new sendEnMessageTask(){}.execute(masterKey, text);
        } else {
            app.displayToastMessage("No secret key yet.");
        }
    }

    private class sendEnMessageTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... args) {
            String masterKey = args[0];
            String text = args[1];
            String enText = secretController.encryptData(
                    text,
                    new SecretKey(masterKey)
            );
            String message = GroupMessage.generateMulticastGroupMessage(
                    CHAT,
                    myGroup,
                    groupMemberMe,
                    enText);
            groupCController.multicast(groupMemberMe, myGroup, message);
            Long timestamp = System.currentTimeMillis();
            groupDAO.insertMessage(
                    new GroupMessage(CHAT,
                            mySid,
                            "",
                            groupName,
                            timestamp,
                            1,
                            enText,
                            groupLeaderSid)
            );
            app.sendBroadcast(new Intent(GroupMessageService.NEW_CHAT_MESSAGE_ACTION));
            return null;
        }

    }

    private void decryptGroupChat(ArrayList<GroupChat> chatList, String masterKey){
        for(GroupChat chat: chatList) {
            chat.setContent(
                    secretController.decryptData(
                            chat.getContent(),
                            new SecretKey(masterKey)
                    )
            );
        }
    }

    private void renderTextView() {
        Group group = groupDAO.getSecureGroup(groupName, groupLeaderSid);
        if(group != null) {
            String masterKeyVersion = "Master-key version: " + group.getMasterKeyVersion();
            String subKeyVersion = "Sub-Key version: " + group.getSubKeyVersion();
            String groupSize = "Group size: " + groupDAO.getGroupSize(groupName, groupLeaderSid);
            String threshold = "Key threshold: " + group.getKeyThreshold();
            tvMasterKeyVersion.setText(masterKeyVersion);
            tvSubKeyVersion.setText(subKeyVersion);
            tvCurrentGroupSize.setText(groupSize);
            tvCurrentThreshold.setText(threshold);
        }
    }

    private void renderButton() {
        if(groupDAO.isMyGroup(groupName, groupLeaderSid)) {
            if (groupDAO.IsKeyUpToDate(groupName, groupLeaderSid)) {
                buttonUpdateSubkey.getBackground().clearColorFilter();
                buttonSetupSecureGroup.getBackground().clearColorFilter();
            } else {
                buttonUpdateSubkey.getBackground().setColorFilter(0xC6E2FFcc, PorterDuff.Mode.MULTIPLY);
                buttonSetupSecureGroup.getBackground().setColorFilter(0xC6E2FFcc, PorterDuff.Mode.MULTIPLY);
            }
        }
    }
}
