package org.servalproject.group.service;

import android.app.Service;
import android.os.Bundle;
import android.os.AsyncTask;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.IBinder;
import android.text.TextUtils;

import org.servalproject.group.Group;
import org.servalproject.group.GroupCommunicationController;
import org.servalproject.group.GroupDAO;
import org.servalproject.group.GroupMember;
import org.servalproject.group.GroupMessage;
import static org.servalproject.group.GroupMessage.Type.*;
import static org.servalproject.group.GroupMember.Role.*;
import org.servalproject.group.activity.GroupActivity;
import org.servalproject.group.secret.SubKey;
import org.servalproject.servaldna.SubscriberId;
import org.servalproject.ServalBatPhoneApplication;
import org.servalproject.servaldna.keyring.KeyringIdentity;
import org.servalproject.servaldna.meshms.MeshMSMessage;
import org.servalproject.servaldna.meshms.MeshMSMessageList;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Stack;


public class GroupMessageService extends Service {

    private static final String TAG = "GroupMessageService";
    public static final String NEW_CHAT_MESSAGE_ACTION = "org.servalproject.group.NEW_CHAT_MESSGAE";
    public static final String UPDATE_GROUP_ACTION = "org.servalproject.group.UPDATE_GROUP";
    private ServalBatPhoneApplication app;
    private KeyringIdentity identity;
    private String mySid;



    @Override
    public void onCreate() {
        Log.d(TAG, "create group service");
        try {
            app = ServalBatPhoneApplication.context;
            identity = app.server.getIdentity();
            mySid = identity.sid.toString();

        } catch (Exception e ) {
            Log.e(TAG, e.getMessage(), e);
            app.displayToastMessage(e.getMessage());


        }
    }

    @Override
    public  IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Log.d(TAG,"NEW MESSAGE!!");
            Bundle bundle = intent.getExtras();
            String senderSidText = (String) bundle.get("sender");
            Log.d(TAG,"from: " +  senderSidText);
            SubscriberId senderSid = new SubscriberId(senderSidText);
            updateGroupMessageList(identity.sid, senderSid);
        }
        catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
            app.displayToastMessage(e.getMessage());

        }
        stopSelf();
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"Destroy group service");
    }

    private void updateGroupMessageList(SubscriberId receiver, SubscriberId sender) {
        GroupMessageTask task = new GroupMessageTask(this);
        task.execute(receiver, sender);
    }

    private class GroupMessageTask extends AsyncTask<SubscriberId, Void, Void> {
        private Context mContext;
        private GroupDAO groupDAO;
        private GroupCommunicationController groupCController;

        public GroupMessageTask(Context context) {
            mContext = context;
        }

        @Override
        protected Void doInBackground(SubscriberId... args) {
            try {

                SubscriberId receiver = args[0];
                SubscriberId sender =  args[1];
                MeshMSMessageList results = app.server.getRestfulClient().meshmsListMessages(identity.sid, sender);
                MeshMSMessage item;
                LinkedList<Object> listItems = new LinkedList<Object>();
                DateFormat df = DateFormat.getDateInstance();
                DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT);
                DateFormat tff = DateFormat.getTimeInstance(DateFormat.LONG);
                String lastDate = df.format(new Date());
                groupDAO = new GroupDAO(getApplicationContext(),identity.sid.toString());
                groupCController = new GroupCommunicationController(app, identity);
                Long lastMessageTime = groupDAO.getLastMessageTimestamp(sender.toString());
                Stack<MeshMSMessage> itemStack = new Stack<MeshMSMessage>();
                while((item = results.nextMessage())!=null) {

                    if(item.type == MeshMSMessage.Type.MESSAGE_RECEIVED && GroupMessage.extractGroupMessage(item.text) != null) {
                        Long currentTime = System.currentTimeMillis();

                        if(item.timestamp*1000 <= lastMessageTime)
                            continue;
                        itemStack.push(item);
                    }
                }
                while(!itemStack.empty()) {
                    processGroupMessage(sender.toString(), receiver.toString(), itemStack.pop());
                }
            } catch(Exception e) {
                Log.e(TAG, e.getMessage(), e);
                app.displayToastMessage(e.getMessage());
            }
            return null;
        }

        private void processGroupMessage(String sender, String receiver, MeshMSMessage item) {
            ArrayList<String> extractResults = GroupMessage.extractGroupMessage(item.text);
            GroupMessage.Type type = GroupMessage.Type.valueOf(extractResults.get(0));
            String groupName = extractResults.get(1);
            String groupLeaderSid = extractResults.get(2);
            Long timestamp = item.timestamp*1000;

            if(type.equals(JOIN)) {
                groupDAO.insertMessage(new GroupMessage(JOIN, sender, receiver,
                                                        groupName, timestamp, 0, "", groupLeaderSid));
                if(groupDAO.getGroupId(groupName, groupLeaderSid) != null)
                    app.displayToastMessage("New join request");
            } else if(type.equals(LEAVE)) {
                groupDAO.insertMessage(new GroupMessage(LEAVE, sender, receiver,
                                                        groupName, timestamp, 0, "", groupLeaderSid));
                if(groupDAO.getGroupId(groupName, groupLeaderSid) != null)
                    app.displayToastMessage("New leave request");
            } else if(type.equals(CHAT)) {

                String text = TextUtils.join(",",extractResults.subList(3, extractResults.size()));
                groupDAO.insertMessage(new GroupMessage(CHAT, sender, receiver,
                                                        groupName, timestamp, 1, text, groupLeaderSid));
                Intent intent = new Intent(NEW_CHAT_MESSAGE_ACTION);
                app.sendBroadcast(intent);
            } else if(type.equals(DONE_LEAVE)) {
                groupDAO.deleteGroup(groupName, groupLeaderSid);
                groupDAO.insertMessage(new GroupMessage(DONE_LEAVE, sender, receiver,
                                                        groupName, timestamp, 1, "", groupLeaderSid));
                app.sendBroadcast(new Intent(GroupActivity.UPDATE_GROUP_LIST));
            } else if (type.equals(SECURE_GROUP_UPDATE)) {
                if (groupDAO.haveSentJoinRequest(groupName, groupLeaderSid) ||
                        groupDAO.haveSentReCreateResponse(groupName, groupLeaderSid)) {
                    Log.d(TAG, "Update Secure group");
                    Integer numberOfTotalKeys = Integer.valueOf(extractResults.get(3));
                    Integer keyThreshold = Integer.valueOf(extractResults.get(4));
                    String masterKeyVersion = extractResults.get(5);
                    String subKeyVersion = extractResults.get(6);
                    String subKey = extractResults.get(7);
                    ArrayList<GroupMember> members = new ArrayList<GroupMember>();
                    for (String member : extractResults.subList(8, extractResults.size())) {
                        HashMap<String, String> map = GroupMessage.parseSecureMember(member);
                        String sid = map.get(GroupMessage.SID);
                        Integer subKeyIndex = Integer.valueOf(map.get(GroupMessage.SUB_KEY_INDEX));
                        GroupMember memberTemp = new GroupMember(groupName, groupLeaderSid, MEMBER, sid, "",
                                new SubKey("", subKeyIndex));
                        if (sid.equals(mySid)) {
                            memberTemp.setSubKey(new SubKey(subKey, subKeyIndex));
                        }
                        if (sid.equals(groupLeaderSid)) {
                            memberTemp.setRole(LEADER);
                        }

                        members.add(memberTemp);
                    }
                    Group group = new Group(
                            groupName,
                            members,
                            groupLeaderSid,
                            false,
                            masterKeyVersion,
                            subKeyVersion,
                            numberOfTotalKeys,
                            keyThreshold);
                    Log.d(TAG, "Update Secure group: finished");
                    groupDAO.updateSecureGroup(group);
                    //groupDAO.insertMessage(new GroupMessage("UPDATE", sender, receiver,
                    //        groupName, timestamp, 1, "", groupLeaderSid));
                    app.sendBroadcast(new Intent(UPDATE_GROUP_ACTION));
                    app.sendBroadcast(new Intent(GroupActivity.UPDATE_GROUP_LIST));
                    groupDAO.insertMessage(new GroupMessage(SECURE_GROUP_UPDATE, sender, receiver,
                            groupName, timestamp, 1, "", groupLeaderSid));
                }
            } else if (type.equals(SUB_KEY_REQUEST)) {
                String masterKeyVersion = extractResults.get(3);
                String subKeyVersion = extractResults.get(4);
                Group myGroup = groupDAO.getSecureGroup(groupName, groupLeaderSid);

                if (myGroup != null) {
                    String myMasterKeyVersion =
                            myGroup.getMasterKeyVersion();
                    String mySubKeyVersion =
                            myGroup.getSubKeyVersion();

                    if (masterKeyVersion.equals(myMasterKeyVersion) &&
                            subKeyVersion.equals(mySubKeyVersion)) {
                        Log.d(TAG, "got key request");
                        groupDAO.insertMessage(new GroupMessage(SUB_KEY_REQUEST, sender, receiver,
                                groupName, timestamp, 1, "", groupLeaderSid));
                        try {
                            GroupCommunicationController groupCController = new GroupCommunicationController(app, identity);
                            GroupMember groupMemberMe = groupDAO.getMember(groupName, groupLeaderSid, mySid);
                            GroupMember member = new GroupMember(myGroup.getName(), myGroup.getLeader(), NONE, sender, "");
                            String message = GroupMessage.generateUnicastGroupMessage(
                                    SUB_KEY_RESPONSE,
                                    myGroup,
                                    groupMemberMe,
                                    member);

                            groupCController.unicast(mySid, member.getSid(), message);
                            Log.d(TAG, "send key response");
                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage(), e);
                            app.displayToastMessage(e.getMessage());
                        }

                    } else {
                        groupDAO.insertMessage(new GroupMessage(SUB_KEY_REQUEST, sender, receiver,
                                groupName, timestamp, 1, "", groupLeaderSid));
                    }
                } else {

                    groupDAO.insertMessage(new GroupMessage(SUB_KEY_REQUEST, sender, receiver,
                            groupName, timestamp, 1, "", groupLeaderSid));
                }

            } else if (type.equals(SUB_KEY_RESPONSE)) {
                String subKey = extractResults.get(3);
                GroupMember member = groupDAO.getMember(groupName,groupLeaderSid,sender);
                if (member != null) {
                    Log.d(TAG, "got key response");
                    String subKeyIndex = String.valueOf(member.getSubKey().getIndex());
                    Intent intent = new Intent(SecretKeyService.NEW_SUB_KEY_RESPONSE);
                    intent.putExtra("group_name", groupName);
                    intent.putExtra("leader", groupLeaderSid);
                    intent.putExtra("subKeyIndex", subKeyIndex);
                    intent.putExtra("subKey", subKey);
                    app.sendBroadcast(intent);
                }
                groupDAO.insertMessage(new GroupMessage(SUB_KEY_RESPONSE, sender, receiver,
                        groupName, timestamp, 1, "", groupLeaderSid));

            } else if (type.equals(RE_CREATE_GROUP_REQUEST)) {
                groupDAO.insertMessage(new GroupMessage(RE_CREATE_GROUP_REQUEST, sender, receiver,
                        groupName, timestamp, 0, "", groupLeaderSid));
                if(groupDAO.getGroupId(groupName, groupLeaderSid) != null)
                    app.displayToastMessage("New re-create group request");
            } else if (type.equals(RE_CREATE_GROUP_RESPONSE)) {
                Intent intent = new Intent(ReCreateGroupRequestService.RE_CREATE_GROUP_RESPONSE);
                intent.putExtra("group_name", groupName);
                intent.putExtra("leader", groupLeaderSid);
                app.sendBroadcast(intent);
                groupDAO.insertMessage(new GroupMessage(RE_CREATE_GROUP_RESPONSE, sender, receiver,
                        groupName, System.currentTimeMillis(), 1, "", groupLeaderSid));
            } else if (type.equals(CHANGE_LEADER)) {
                groupDAO.changeLeader(groupName, groupLeaderSid, sender);
                app.sendBroadcast(new Intent(GroupActivity.UPDATE_GROUP_LIST));
                String text =
                        GroupMessage.generateUnicastGroupMessage(
                                CHANGE_LEADER_RESPONSE, groupName, groupLeaderSid, "");
                groupCController.unicast(mySid, sender, text);
                groupDAO.insertMessage(new GroupMessage(CHANGE_LEADER, sender, receiver,
                        groupName, System.currentTimeMillis(), 1, "", groupLeaderSid));
            }else if (type.equals(CHANGE_LEADER_RESPONSE)) {
                Intent intent = new Intent(ChangeLeaderService.CHANGE_LEADER_RESPONSE);
                intent.putExtra("group_name", groupName);
                intent.putExtra("leader", groupLeaderSid);
                app.sendBroadcast(intent);
                groupDAO.insertMessage(new GroupMessage(CHANGE_LEADER_RESPONSE, sender, receiver,
                        groupName, System.currentTimeMillis(), 1, "", groupLeaderSid));
            }

        }
    }

}
