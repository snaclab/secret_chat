package org.servalproject.group.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.widget.ListView;
import android.content.Intent;
import android.widget.TextView;

import org.servalproject.R;
import org.servalproject.ServalBatPhoneApplication;
import org.servalproject.group.GroupCommunicationController;
import org.servalproject.group.GroupDAO;
import org.servalproject.group.GroupMember;
import org.servalproject.group.GroupMemberListAdapter;
import org.servalproject.servald.PeerListService;
import org.servalproject.servaldna.keyring.KeyringIdentity;

import java.util.ArrayList;


public class GroupMemberActivity extends Activity {
    private static final String TAG = "GroupMemberActivity";
    private ArrayList<GroupMember> members = new ArrayList<GroupMember>();
    private String groupName;
    private String groupLeader;
    private GroupMemberListAdapter adapter;
    private ListView groupMemberListView;
    private TextView tvTotal;
    private TextView tvThreshold;
    private GroupDAO groupDAO;
    private ServalBatPhoneApplication app;
    private KeyringIdentity identity;
    private GroupCommunicationController groupCController;
    public static final String UPDATE_MEMBER_LIST = "update_member_list";

    BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(UPDATE_MEMBER_LIST)) {
                setupGroupMemberList();
            }
        }

    };
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.group_member_list);
        try {
            app = ServalBatPhoneApplication.context;
            this.identity = app.server.getIdentity();
            Intent intent = getIntent();
            groupName =  intent.getStringExtra("group_name");
            groupLeader = intent.getStringExtra("group_leader");
            groupDAO= new GroupDAO(getApplicationContext(),identity.sid.toString());
            members = groupDAO.getSecureGroup(groupName, groupLeader).getMembers();
            tvTotal = (TextView) findViewById(R.id.text_view_total_number);
            tvThreshold = (TextView) findViewById(R.id.text_view_threshold);
            String threshold = "Threshold: " +
                    String.valueOf(groupDAO.getSecureGroup(groupName, groupLeader).getKeyThreshold());
            String total = "Group Size: " +
                    String.valueOf(groupDAO.getGroupSize(groupName,groupLeader));
            tvTotal.setText(total);
            tvThreshold.setText(threshold);
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    protected void onResume() {
        setupGroupMemberList();
        IntentFilter filter = new IntentFilter();
        filter.addAction(UPDATE_MEMBER_LIST);
        this.registerReceiver(receiver, filter);
        super.onResume();
    }

    @Override
    protected void onPause() {
        this.unregisterReceiver(receiver);
        super.onPause();
    }
    void setupGroupMemberList() {
        members = groupDAO.getSecureGroup(groupName, groupLeader).getMembers();
        adapter = new GroupMemberListAdapter(this, members);
        groupMemberListView = (ListView) findViewById(R.id.list_view_group_member);
        groupMemberListView.setAdapter(adapter);
    }




}
