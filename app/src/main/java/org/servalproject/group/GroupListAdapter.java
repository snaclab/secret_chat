package org.servalproject.group;

import org.servalproject.R;
import org.servalproject.ServalBatPhoneApplication;
import org.servalproject.group.activity.GroupActivity;
import org.servalproject.group.activity.GroupChatActivity;
import org.servalproject.servaldna.keyring.KeyringIdentity;

import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.util.Log;

import java.util.ArrayList;

import static org.servalproject.ServalBatPhoneApplication.context;

public class GroupListAdapter extends ArrayAdapter<Group> {

    private static final String TAG = "GroupListAdapter";
    public GroupListAdapter(Context context, ArrayList<Group> groups) {
        super(context, 0, groups);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Group group = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.group_list_item, parent, false);
        }
        final TextView tvName = (TextView) convertView.findViewById(R.id.text_view_group_name);
        if(group.getIsMyGroup()) {
            tvName.setText(group.getName() + "(OWN)");
        } else {
            tvName.setText(group.getName() + "(" + group.getLeaderAbbreviation() + ")");
        }
        tvName.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), GroupChatActivity.class);
                intent.putExtra("group_name", group.getName());
                intent.putExtra("leader_sid", group.getLeader());
                getContext().startActivity(intent);

            }
        });
        tvName.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final PopupMenu popupmenu = new PopupMenu(getContext(), tvName);
                popupmenu.getMenuInflater().inflate(R.menu.group_option_menu, popupmenu.getMenu());
                popupmenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.group_delete:
                                try {
                                    ServalBatPhoneApplication app = context;
                                    KeyringIdentity identity = app.server.getIdentity();
                                    String mySid = identity.sid.toString();
                                    GroupDAO groupDAO= new GroupDAO(context.getApplicationContext(), mySid);
                                    groupDAO.deleteGroup(group.getName(),group.getLeader());
                                    app.sendBroadcast(new Intent(GroupActivity.UPDATE_GROUP_LIST));

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
