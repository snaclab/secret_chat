package org.servalproject.group.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.servalproject.R;
import org.servalproject.ServalBatPhoneApplication;
import org.servalproject.group.Group;
import org.servalproject.group.GroupCommunicationController;
import org.servalproject.group.GroupDAO;
import org.servalproject.group.GroupListAdapter;
import org.servalproject.group.GroupMessage;
import static org.servalproject.group.GroupMessage.Type.*;
import org.servalproject.servaldna.SubscriberId;
import org.servalproject.servaldna.keyring.KeyringIdentity;
import org.servalproject.servald.IPeerListListener;
import org.servalproject.servald.Peer;
import org.servalproject.servald.PeerComparator;
import org.servalproject.servald.PeerListService;

import java.util.ArrayList;
import java.util.Collections;

public class GroupActivity extends Activity {

    private static final String TAG = "GroupActivity";
    private ServalBatPhoneApplication app;
    private KeyringIdentity identity;
    private String mySid;
    private ArrayList<Group> groups = new ArrayList<Group>();
    private ArrayAdapter<String> groupPeerListAdapter;
    private GroupListAdapter groupListAdapter;
    private ArrayList<Peer> peers = new ArrayList<Peer>();
    private ArrayList<String> peersList = new ArrayList<String>();
    private ListView groupListView;
    private ListView groupPeerListView;
    private Button buttonCreateGroup;
    private Button buttonJoinGroup;
    private Button buttonRequest;
    private EditText etCreateGroup;
    private EditText etGroupPeer;
    private TextView tvIdHeader;
    private GroupDAO groupDAO;
    private GroupCommunicationController groupCController;
    public static final String UPDATE_GROUP_LIST = "update_group_list";

    BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(UPDATE_GROUP_LIST)) {
                setupGroupList();
                app.displayToastMessage("The Secret group has been updated.");
            }
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.group_list);
        buttonCreateGroup = (Button) findViewById(R.id.button_create_group);
        buttonJoinGroup = (Button) findViewById(R.id.button_join_group);
        buttonRequest = (Button) findViewById(R.id.button_group_request);
        etCreateGroup = (EditText) findViewById(R.id.edit_text_create_group);
        etGroupPeer = (EditText) findViewById(R.id.edit_text_group_peer);
        tvIdHeader = (TextView) findViewById(R.id.text_view_id_header);
        try {
            app = ServalBatPhoneApplication.context;
            identity = app.server.getIdentity();
            mySid = identity.sid.toString();

            if(mySid.length() > 10){
                String shortId = mySid.substring(0,10) + "*";
                tvIdHeader.setText(shortId);
            } else {
                tvIdHeader.setText(mySid);
            }

            groupDAO= new GroupDAO(getApplicationContext(), mySid);
            groupCController = new GroupCommunicationController(app, identity);
            setupButtonListener();
            setupGroupPeerList();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            app.displayToastMessage(e.getMessage());
            this.finish();
        }
    }

    @Override
    protected void onResume() {
        setupGroupList();
        IntentFilter filter = new IntentFilter();
        filter.addAction(UPDATE_GROUP_LIST);
        this.registerReceiver(receiver, filter);
        super.onResume();

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                PeerListService.addListener(listener);
                return null;
            }

        } .execute();
    }

    @Override
    protected void onPause() {
        this.unregisterReceiver(receiver);
        PeerListService.removeListener(listener);
        peers.clear();
        peersList.clear();
        super.onPause();
    }


    private void setupGroupList() {
        groups = groupDAO.getMyGroupList();
        groups.addAll(groupDAO.getOtherGroupList());
        groupListAdapter = new GroupListAdapter(this, groups);
        groupListView = (ListView) findViewById(R.id.list_view_group);
        groupListView.setAdapter(groupListAdapter);
    }

    private void setupGroupPeerList() {
        groupPeerListAdapter =
            new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, peersList);
        groupPeerListView = (ListView) findViewById(R.id.list_view_group_peer);
        groupPeerListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        groupPeerListView.setAdapter(groupPeerListAdapter);
    }

    private void setupButtonListener() {

        buttonRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), GroupRequestActivity.class));
            }
        });

        buttonCreateGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etCreateGroup.getText().toString();
                createGroup(name);
                setupGroupList();
                etCreateGroup.setText("");
            }
        }
                                            );

        buttonJoinGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String groupName = etGroupPeer.getText().toString();
                int len = groupPeerListView.getCount();
                SparseBooleanArray checked = groupPeerListView.getCheckedItemPositions();
                for (int i = 0; i < len; i++) {
                    if (checked.get(i)) {
                        SubscriberId peerSid  = peers.get(i).getSubscriberId();
                        String peerString = peerSid.toString();
                        String text =
                                GroupMessage.generateUnicastGroupMessage(
                                        JOIN, groupName, peerString,"");
                        groupDAO.insertMessage(new GroupMessage(JOIN, mySid, peerString,
                                groupName, System.currentTimeMillis(), 1, "", peerString));
                        groupCController.unicast(mySid, peerString, text);
                    }
                }
                etGroupPeer.setText("");
            }
        });


    }


    private void peerUpdated(Peer p) {
        if (!peers.contains(p)) {
            if (!p.isReachable())
                return;
            peers.add(p);
            Log.d(TAG,"New peer: "+ p.toString());
        }
        Collections.sort(peers, new PeerComparator());
        GroupActivity.this.peersList.clear();
        for(int i = 0; i < peers.size(); i++) {
            GroupActivity.this.peersList.add(peers.get(i).getSubscriberId().abbreviation());
        }
        GroupActivity.this.groupPeerListAdapter.notifyDataSetChanged();
    }

    private IPeerListListener listener = new IPeerListListener() {
        @Override
        public void peerChanged(final Peer p) {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    peerUpdated(p);
                };

            });
        }
    };

    public void createGroup(String name) {
        if (!name.equals("")) {
            if (groupDAO.getGroupId(name, mySid)!= null)
                app.displayToastMessage("The group already exists.");
            else
                groupDAO.createGroup(name, mySid);
        }
    }


}
