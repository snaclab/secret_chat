package org.servalproject.group.activity;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import org.servalproject.R;
import org.servalproject.ServalBatPhoneApplication;
import org.servalproject.group.Group;
import org.servalproject.group.GroupCommunicationController;
import org.servalproject.group.GroupDAO;
import org.servalproject.group.GroupMember;
import org.servalproject.group.GroupMessage;
import static org.servalproject.group.GroupMessage.Type.*;
import static org.servalproject.group.GroupMember.Role.*;
import org.servalproject.group.GroupRequest;
import org.servalproject.group.GroupRequestListAdapter;
import org.servalproject.servaldna.keyring.KeyringIdentity;

import java.util.ArrayList;

public class GroupRequestActivity extends Activity {
    private final String TAG = "GroupRequestActivity";
    private ServalBatPhoneApplication app;
    private KeyringIdentity identity;
    private String mySid;
    private GroupDAO groupDAO;
    private GroupCommunicationController groupCController;
    private ListView list;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_request);
        try {
            app = ServalBatPhoneApplication.context;
            identity = app.server.getIdentity();
            mySid = identity.sid.toString();
            groupCController = new GroupCommunicationController(app, identity);
            groupDAO= new GroupDAO(getApplicationContext(), mySid);
            list = (ListView) findViewById(R.id.list_view_group_request);
        } catch(Exception e) {
            Log.e(TAG,e.getMessage(),e);
            app.displayToastMessage(e.getMessage());
            this.finish();
        }
    }
    @Override
    public void onResume() {
        populateList();
        super.onResume();
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

        UpdateRequestTask task = new UpdateRequestTask(this);
        task.execute();

    }
    private class UpdateRequestTask extends AsyncTask<String, Void, ArrayList<GroupRequest>> {
        private GroupDAO groupDAO;
        private Context context;

        public UpdateRequestTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPostExecute(ArrayList<GroupRequest> requestList) {
            GroupRequestListAdapter adapter = new GroupRequestListAdapter(context, requestList);
            list.setAdapter(adapter);
            list.setSelection(adapter.getCount() - 1);
        }

        @Override
        protected ArrayList<GroupRequest> doInBackground(String... params) {
            groupDAO = new GroupDAO(getApplicationContext(), identity.sid.toString());
            ArrayList<GroupRequest> requestList =  groupDAO.getNewJoinList();
            requestList.addAll(groupDAO.getNewLeaveList());
            requestList.addAll(groupDAO.getReCreateRequestList());
            return requestList;
        }
    }

    public void addMember(String groupName, String leader, String member) {

        if(groupDAO.isMyGroup(groupName, leader)) {
            if(groupDAO.getMemberId(groupName, leader, member) == null) {
                groupDAO.insertMember(new GroupMember(groupName, mySid, MEMBER, member, ""));
                groupDAO.setNotUpToDate(groupName, leader);
                Log.d(TAG, member + " joined!");
            }
        }

    }

    public void removeMember(String groupName, String leader, String member) {

        if(groupDAO.isMyGroup(groupName, leader)) {
            if(groupDAO.getMemberId(groupName, leader, member) != null) {
                Group myGroup = groupDAO.getSecureGroup(groupName, leader);
                int threshold = myGroup.getKeyThreshold();
                int groupSize = groupDAO.getGroupSize(groupName, leader);
                if(groupSize - 1 < threshold) {
                    app.displayToastMessage("Error: The number of members will be less than the threshold");
                } else {
                    groupDAO.deleteMember(new GroupMember(groupName, mySid, MEMBER, member, ""));
                    groupDAO.setNotUpToDate(groupName, leader);
                    String text =
                            GroupMessage.generateUnicastGroupMessage(
                                    DONE_LEAVE, groupName, mySid, "");
                    groupCController.unicast(mySid, member, text);
                    Log.d(TAG, member + " leave!");
                }
            }
        }

    }

    public void reCreateGroupResponse(String groupName, String leader, String newLeader) {
        String text =
                GroupMessage.generateUnicastGroupMessage(
                        RE_CREATE_GROUP_RESPONSE,
                        groupName,
                        leader,
                        "");
        groupDAO.insertMessage(new GroupMessage(RE_CREATE_GROUP_RESPONSE, mySid, newLeader,
                groupName, System.currentTimeMillis(), 1, "", newLeader));
        groupCController.unicast(mySid, newLeader, text);

    }

    public void doneMessage(int id) {
        groupDAO.doneMessage(id);
        populateList();
    }
}
