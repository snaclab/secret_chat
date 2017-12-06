package org.servalproject.group;

import org.servalproject.R;
import org.servalproject.ServalBatPhoneApplication;
import org.servalproject.group.activity.GroupMemberActivity;
import org.servalproject.servaldna.keyring.KeyringIdentity;

import android.content.Intent;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;

import java.util.ArrayList;


public class GroupMemberListAdapter extends ArrayAdapter<GroupMember> {
    private GroupDAO groupDAO;
    private ServalBatPhoneApplication app;
    private KeyringIdentity identity;
    private String mySid;
    private Context context;
    public static final String TAG = "GroupMemberListAdapter";

    public GroupMemberListAdapter(Context context, ArrayList<GroupMember> members) {
        super(context, 0, members);
        try {
            app = ServalBatPhoneApplication.context;
            identity = app.server.getIdentity();
            mySid = identity.sid.toString();
        } catch (Exception e){
            Log.e(TAG,e.getMessage(),e);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final GroupMember member = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.group_member_list_item, parent, false);
        }
        final TextView tvName = (TextView) convertView.findViewById(R.id.text_view_group_member_name);
        String sid = member.getSid();
        final String groupName = member.getGroupName();
        final String leader = member.getLeader();
        String AbbName;

        if(5 < sid.length()){
            if (!sid.equals(mySid) && sid.equals(leader)) {
                AbbName = sid.substring(0, 5) + "* (Leader)";
            } else if (!sid.equals(mySid)) {
                AbbName = sid.substring(0, 5) + "* ";
            } else if (sid.equals(leader)) {
                AbbName = sid.substring(0, 5) + "* (Me, Leader)";
            } else {
                AbbName = sid.substring(0, 5) + "* (Me)";
            }
        } else {
            if (!sid.equals(mySid) && sid.equals(leader)) {
                AbbName = sid + " (Leader)";
            } else if (!sid.equals(mySid)) {
                AbbName = sid ;
            } else if (sid.equals(leader)) {
                AbbName = sid + " (Me, Leader)";
            } else {
                AbbName = sid + " (Me)";
            }
        }

        tvName.setText(AbbName);

        tvName.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final PopupMenu popupmenu = new PopupMenu(getContext(), tvName);
                popupmenu.getMenuInflater().inflate(R.menu.group_member_option_menu, popupmenu.getMenu());
                popupmenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.group_member_delete:
                                try {
                                    if(mySid.equals(leader) && !member.getSid().equals(mySid)) {
                                        groupDAO= new GroupDAO(app, mySid);

                                        int remaining_sub_keys = groupDAO.getGroupSize(groupName, leader) - 1;
                                        Group group = groupDAO.getSecureGroup(groupName, leader);

                                        if(remaining_sub_keys < group.getKeyThreshold() ) {
                                            app.displayToastMessage("Error: The number of members will be less than the threshold");
                                        } else {
                                            groupDAO.deleteMember(member);
                                            groupDAO.setNotUpToDate(groupName, leader);
                                            app.sendBroadcast(new Intent(GroupMemberActivity.UPDATE_MEMBER_LIST));
                                        }
                                    }
                                } catch (Exception e){
                                    Log.e(TAG,e.getMessage(),e);
                                }
                                break;
                            default:
                                break;
                        }
                        return true;
                    }
                });
                popupmenu.show();
                return true;
            }
        });
        return convertView;


    }
}
