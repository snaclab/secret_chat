package org.servalproject.group;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import org.servalproject.R;
import org.servalproject.group.activity.GroupRequestActivity;

import java.util.ArrayList;

/**
 * Created by sychen on 2017/5/19.
 */

public class GroupRequestListAdapter extends ArrayAdapter<GroupRequest> {
    private Context context;

    public GroupRequestListAdapter(Context context, ArrayList<GroupRequest> requestList) {
        super(context, 0, requestList);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final GroupRequest request = getItem(position);

        if(convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.group_request_list_item, parent, false);
        }
        TextView tvText = (TextView) convertView.findViewById(R.id.text_content);
        Button buttonAgree = (Button) convertView.findViewById(R.id.button_agree);
        Button buttonDeny = (Button) convertView.findViewById(R.id.button_deny);

        if(request != null) {

            final String leader = request.getLeader();
            final String groupName = request.getGroupName();
            final String objectSid = request.getObjectSid();
            final int id = request.getId();
            String type = request.getType();
            String ShortLeader = "'";
            String shortObJectSid = "";
            if(leader.length() > 10){
                ShortLeader = leader.substring(0,10) + "*";
            }

            if(objectSid.length() > 10){
                shortObJectSid = objectSid.substring(0,10) + "*";
            }
            if(type.equals("JOIN"))
                type = "Join";
            else if (type.equals("LEAVE"))
                type = "Leave";
            else if (type.equals("RE_CREATE_GROUP"))
                type = "Re-create group";

            String content = type + " request from " +
                    shortObJectSid + " to group " +
                    groupName + "(" + ShortLeader + ")";
            tvText.setText(content);

            buttonAgree.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if(context instanceof GroupRequestActivity) {
                        if(request.getType().equals("JOIN")) {
                            ((GroupRequestActivity) context).addMember(groupName, leader, objectSid);
                        } else if (request.getType().equals("LEAVE")) {
                            ((GroupRequestActivity) context).removeMember(groupName, leader, objectSid);

                        } else if (request.getType().equals("RE_CREATE_GROUP")) {
                            ((GroupRequestActivity) context).reCreateGroupResponse(groupName, leader, objectSid);

                        }
                        ((GroupRequestActivity) context).doneMessage(id);
                    }

                }
            });

            buttonDeny.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if(context instanceof GroupRequestActivity) {

                        ((GroupRequestActivity) context).doneMessage(id);

                    }

                }
            });
        }
        return convertView;

    }
}
