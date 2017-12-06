package org.servalproject.group;

import java.util.ArrayList;
import java.util.HashMap;

import android.util.Log;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.servalproject.group.secret.SubKey;

import static org.servalproject.group.GroupMessage.Type.*;
import static org.servalproject.group.GroupMember.Role.*;

public class GroupDAO {
    public static final String MESSAGES_TABLE_NAME = "messages";
    public static final String MESSAGES_COLUMN_ID = "id";
    public static final String MESSAGES_COLUMN_TYPE = "type";
    public static final String MESSAGES_COLUMN_FROM_WHO = "from_who";
    public static final String MESSAGES_COLUMN_TO_WHO = "to_who";
    public static final String MESSAGES_COLUMN_OBJECT_GROUP = "object_group";
    public static final String MESSAGES_COLUMN_TIMESTAMP = "timestamp";
    public static final String MESSAGES_COLUMN_DONE = "done";
    public static final String MESSAGES_COLUMN_CONTENT = "content";
    public static final String MESSAGES_COLUMN_LEADER = "leader";
    public static final String MESSAGES_CREATE_TABLE = "CREATE TABLE " +
            MESSAGES_TABLE_NAME + " (" +
            MESSAGES_COLUMN_ID + " INTEGER PRIMARY KEY, " +
            MESSAGES_COLUMN_TYPE + " TEXT, " +
            MESSAGES_COLUMN_FROM_WHO + " TEXT, " +
            MESSAGES_COLUMN_TO_WHO + " TEXT, " +
            MESSAGES_COLUMN_OBJECT_GROUP + " TEXT, " +
            MESSAGES_COLUMN_TIMESTAMP + " TEXT, " +
            MESSAGES_COLUMN_DONE + " INTEGER, " +
            MESSAGES_COLUMN_CONTENT + " TEXT, " +
            MESSAGES_COLUMN_LEADER + " TEXT) ";


    public static final String GROUPS_TABLE_NAME = "groups";
    public static final String GROUPS_COLUMN_ID = "id";
    public static final String GROUPS_COLUMN_NAME = "name";
    public static final String GROUPS_COLUMN_LEADER = "leader";
    public static final String GROUPS_COLUMN_MASTER_KEY_VERSION = "master_key_version";
    public static final String GROUPS_COLUMN_SUB_KEY_VERSION = "sub_key_version";
    public static final String GROUPS_COLUMN_NUMBER_OF_TOTAL_KEYs = "number_of_total_keys";
    public static final String GROUPS_COLUMN_KEY_THRESHOLD = "key_threshold";
    public static final String GROUPS_COLUMN_THRESHOLD_MODE = "threshold_mode";
    public static final String GROUPS_COLUMN_MODE_VALUE = "mode_value";
    public static final String GROUPS_COLUMN_UP_TO_DATE = "up_to_date";
    public static final String GROUPS_CREATE_TABLE = "CREATE TABLE " +
            GROUPS_TABLE_NAME + " (" +
            GROUPS_COLUMN_ID +  " INTEGER PRIMARY KEY, " +
            GROUPS_COLUMN_NAME +  " TEXT , " +
            GROUPS_COLUMN_LEADER + " TEXT, " +
            GROUPS_COLUMN_THRESHOLD_MODE + " TEXT, " +
            GROUPS_COLUMN_MODE_VALUE + " TEXT, " +
            GROUPS_COLUMN_MASTER_KEY_VERSION + " INTEGER , " +
            GROUPS_COLUMN_SUB_KEY_VERSION + " INTEGER , " +
            GROUPS_COLUMN_NUMBER_OF_TOTAL_KEYs + " INTEGER, " +
            GROUPS_COLUMN_KEY_THRESHOLD + " INTEGER, " +
            GROUPS_COLUMN_UP_TO_DATE + " INTEGER DEFAULT 1)" ;

    public static final String MEMBERS_TABLE_NAME = "members";
    public static final String MEMBERS_COLUMN_ID = "id";
    public static final String MEMBERS_COLUMN_ROLE = "role";
    public static final String MEMBERS_COLUMN_SID = "sid";
    public static final String MEMBERS_COLUMN_NAME = "name";
    public static final String MEMBERS_COLUMN_GROUP_ID = "group_id";
    public static final String MEMBERS_COLUMN_SUB_KEY_INDEX = "sub_key_index";
    public static final String MEMBERS_COLUMN_SUB_KEY = "sub_key";
    public static final String MEMBERS_CREATE_TABLE = "CREATE TABLE " +
            MEMBERS_TABLE_NAME + " (" +
            MEMBERS_COLUMN_ID +  " INTEGER PRIMARY KEY, " +
            MEMBERS_COLUMN_ROLE + " TEXT , " +
            MEMBERS_COLUMN_SID +  " TEXT , " +
            MEMBERS_COLUMN_NAME + " TEXT , " +
            MEMBERS_COLUMN_SUB_KEY_INDEX +  " INTEGER , " +
            MEMBERS_COLUMN_SUB_KEY + " TEXT , " +
            MEMBERS_COLUMN_GROUP_ID + " INTEGER)";

    private SQLiteDatabase db;
    private String mySid;
    public GroupDAO(Context context, String mySid) {
        this.mySid = mySid;
        db = GroupDbHelper.getDatabase(context);

    }
    public void close() {
        if(db != null) {
            db.close();
        }
    }

    public boolean insertMessage(GroupMessage gm) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MESSAGES_COLUMN_TYPE, gm.getType().toString());
        contentValues.put(MESSAGES_COLUMN_FROM_WHO, gm.getFromWho());
        contentValues.put(MESSAGES_COLUMN_TO_WHO, gm.getToWho());
        contentValues.put(MESSAGES_COLUMN_OBJECT_GROUP, gm.getObjectGroup());
        contentValues.put(MESSAGES_COLUMN_TIMESTAMP, gm.getTimestamp());
        contentValues.put(MESSAGES_COLUMN_DONE, gm.getDone());
        contentValues.put(MESSAGES_COLUMN_CONTENT, gm.getContent());
        contentValues.put(MESSAGES_COLUMN_LEADER, gm.getGroupLeader());
        db.insert(MESSAGES_TABLE_NAME, null, contentValues);
        Log.d("GroupDAO", "insert message");
        return true;
    }

    public boolean deleteMessage(String id) {
        return db.delete(MESSAGES_TABLE_NAME, MESSAGES_COLUMN_ID + " = " + id, null) > 0;
    }

    public boolean doneMessage(Integer id) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MESSAGES_COLUMN_DONE, 1);
        db.update(MESSAGES_TABLE_NAME, contentValues, MESSAGES_COLUMN_ID+ " = ?", new String[] {Integer.toString(id)});
        Log.d("GroupDAO", "done message");
        return true;

    }

    public Long getLastMessageTimestamp(String sid ) {

        Long result = (long) 0;
        Cursor c = db.rawQuery(
                       "SELECT " + MESSAGES_COLUMN_TIMESTAMP + ", MAX(" + MESSAGES_COLUMN_TIMESTAMP + ") " +
                       " FROM " + MESSAGES_TABLE_NAME +
                       " WHERE  " + MESSAGES_COLUMN_FROM_WHO + " = ? "
                       , new String[] {sid});
        c.moveToFirst();
        if(c.getCount() > 0) {
            result = c.getLong(c.getColumnIndexOrThrow(MESSAGES_COLUMN_TIMESTAMP));
        }
        c.close();
        return result;
    }

    public ArrayList<GroupRequest> getNewJoinList() {
        ArrayList<GroupRequest> list = new ArrayList<GroupRequest>();

        Cursor c = db.query(MESSAGES_TABLE_NAME,
                null,
                MESSAGES_COLUMN_TYPE + " = ? AND " + MESSAGES_COLUMN_DONE + " = ? ", new String[] {"JOIN", "0"}, null, null, null );

        while(c.moveToNext()) {
            String objectSid = c.getString(c.getColumnIndexOrThrow(MESSAGES_COLUMN_FROM_WHO));
            String groupName = c.getString(c.getColumnIndexOrThrow(MESSAGES_COLUMN_OBJECT_GROUP));
            String leader = c.getString(c.getColumnIndexOrThrow(MESSAGES_COLUMN_LEADER));
            int id = c.getInt(c.getColumnIndexOrThrow(MESSAGES_COLUMN_ID));
            String type = "JOIN";
            if(getGroupId(groupName, leader) != null && leader.equals(mySid))
                list.add(new GroupRequest(type, groupName, leader, objectSid, id));

        }
        c.close();
        return list;
    }

    public ArrayList<GroupRequest> getNewLeaveList() {
        ArrayList<GroupRequest> list = new ArrayList<GroupRequest>();

        Cursor c = db.query(MESSAGES_TABLE_NAME,
                            null,
                            MESSAGES_COLUMN_TYPE + " = ? AND " + MESSAGES_COLUMN_DONE + " = ? ", new String[] {"LEAVE", "0"}, null, null, null );

        while(c.moveToNext()) {
            String objectSid = c.getString(c.getColumnIndexOrThrow(MESSAGES_COLUMN_FROM_WHO));
            String groupName = c.getString(c.getColumnIndexOrThrow(MESSAGES_COLUMN_OBJECT_GROUP));
            String leader = c.getString(c.getColumnIndexOrThrow(MESSAGES_COLUMN_LEADER));
            int id = c.getInt(c.getColumnIndexOrThrow(MESSAGES_COLUMN_ID));
            String type = "LEAVE";
            if(getGroupId(groupName, leader) != null && leader.equals(mySid))
                list.add(new GroupRequest(type, groupName, leader, objectSid, id));

        }
        c.close();
        return list;
    }
    public ArrayList<GroupRequest> getReCreateRequestList() {
        ArrayList<GroupRequest>  list = new ArrayList<GroupRequest>();

        Cursor c = db.query(MESSAGES_TABLE_NAME,
                null,
                MESSAGES_COLUMN_TYPE + " = ? AND " + MESSAGES_COLUMN_DONE + " = ? AND " + MESSAGES_COLUMN_TO_WHO +" = ? ",
                new String[] {"RE_CREATE_GROUP_REQUEST", "0", mySid}, null, null, null );

        while(c.moveToNext()) {
            String objectSid = c.getString(c.getColumnIndexOrThrow(MESSAGES_COLUMN_FROM_WHO));
            String groupName = c.getString(c.getColumnIndexOrThrow(MESSAGES_COLUMN_OBJECT_GROUP));
            String leader = c.getString(c.getColumnIndexOrThrow(MESSAGES_COLUMN_LEADER));
            int id = c.getInt(c.getColumnIndexOrThrow(MESSAGES_COLUMN_ID));
            String type = "RE_CREATE_GROUP";
            if(getGroupId(groupName, leader) != null)
                list.add(new GroupRequest(type, groupName, leader, objectSid, id));
        }
        c.close();
        return list;
    }


    public ArrayList<GroupChat> getChatList(String group, String leaderSid) {
        ArrayList<GroupChat> chatList = new ArrayList<GroupChat>();
        Cursor c = db.query(MESSAGES_TABLE_NAME, null, MESSAGES_COLUMN_TYPE + " = ? AND " +
                            MESSAGES_COLUMN_OBJECT_GROUP + " = ? AND " + MESSAGES_COLUMN_LEADER + " = ? ",
                            new String[] {"CHAT", group, leaderSid}, null, null, null);

        while(c.moveToNext()) {
            String groupName = c.getString(c.getColumnIndexOrThrow(MESSAGES_COLUMN_OBJECT_GROUP));
            String from = c.getString(c.getColumnIndexOrThrow(MESSAGES_COLUMN_FROM_WHO));
            String to = c.getString(c.getColumnIndexOrThrow(MESSAGES_COLUMN_TO_WHO));
            Long timestamp = c.getLong(c.getColumnIndexOrThrow(MESSAGES_COLUMN_TIMESTAMP));
            boolean done = c.getInt(c.getColumnIndexOrThrow(MESSAGES_COLUMN_DONE))>0;
            String content = c.getString(c.getColumnIndexOrThrow(MESSAGES_COLUMN_CONTENT));
            if(from.equals(mySid)) {
                GroupChat chat = new GroupChat(groupName, mySid, content, timestamp, done, true);
                chatList.add(chat);
            } else if(to.equals(mySid)) {
                GroupChat chat = new GroupChat(groupName, from, content, timestamp, done, false);
                chatList.add(chat);
            }
        }
        c.close();
        return chatList;
    }

    public ArrayList<GroupMember> getNewSubKeyRequest(String group, String leaderSid) {
        ArrayList<GroupMember> requestMemberList = new ArrayList<GroupMember>();
        Cursor c = db.query(
                MESSAGES_TABLE_NAME,
                null,
                MESSAGES_COLUMN_TYPE + " = ? AND " +
                        MESSAGES_COLUMN_OBJECT_GROUP + " = ? AND " +
                        MESSAGES_COLUMN_LEADER + " = ? AND " +
                        MESSAGES_COLUMN_DONE + " = ? ",
                new String[] {
                        "SUB_KEY_REQUEST",
                        group,
                        leaderSid,
                        "0"}, null, null, null);

        while(c.moveToNext()) {
            Integer id = c.getInt(c.getColumnIndexOrThrow(MESSAGES_COLUMN_ID));
            String from = c.getString(c.getColumnIndexOrThrow(MESSAGES_COLUMN_FROM_WHO));
            if(getMemberId(group, leaderSid, from) != null)
                requestMemberList.add(new GroupMember(group, leaderSid, NONE, from, ""));
            doneMessage(id);

        }
        c.close();
        return requestMemberList;
    }

    public boolean IsKeyUpToDate(String groupName, String leader) {
        boolean result = false;
        String groupId = getGroupId(groupName, leader);
        if (groupId != null) {
            Cursor c = db.query(
                    GROUPS_TABLE_NAME,
                    null,
                    GROUPS_COLUMN_ID + " = ? ",
                    new String[] {groupId}, null, null, null, null
            );
            c.moveToFirst();
            if(c.getCount() > 0) {
                int upToDate = c.getInt(c.getColumnIndexOrThrow(GROUPS_COLUMN_UP_TO_DATE));
                c.close();

                if(upToDate == 1)
                    result = true;

            }
        }
        return result;
    }

    public void setNotUpToDate(String groupName, String leader) {
        String groupId = getGroupId(groupName, leader);
        if (groupId != null) {
            Cursor cGroup = db.query(GROUPS_TABLE_NAME, null,
                            GROUPS_COLUMN_ID + " = ? ",
                    new String[] {groupId}, null, null, null);
            cGroup.moveToFirst();
            if (cGroup.getCount() > 0) {
                String id = cGroup.getString(cGroup.getColumnIndexOrThrow(GROUPS_COLUMN_ID));
                ContentValues contentValues = new ContentValues();
                contentValues.put(GROUPS_COLUMN_UP_TO_DATE, 0);
                db.update(GROUPS_TABLE_NAME, contentValues, GROUPS_COLUMN_ID+ " = " + id, null);
            }
            cGroup.close();
        }
    }
    public void setUpToDate(String groupName, String leader) {
        String groupId = getGroupId(groupName, leader);
        if (groupId != null) {
            Cursor cGroup = db.query(GROUPS_TABLE_NAME, null,
                    GROUPS_COLUMN_ID + " = ? ",
                    new String[] {groupId}, null, null, null);
            cGroup.moveToFirst();
            if (cGroup.getCount() > 0) {
                String id = cGroup.getString(cGroup.getColumnIndexOrThrow(GROUPS_COLUMN_ID));
                ContentValues contentValues = new ContentValues();
                contentValues.put(GROUPS_COLUMN_UP_TO_DATE, 1);
                db.update(GROUPS_TABLE_NAME, contentValues, GROUPS_COLUMN_ID+ " = " + id, null);
            }
            cGroup.close();
        }
    }

    public void changeLeader(String groupName, String oldLeaderSid, String newLeaderSid) {
        String groupId = getGroupId(groupName, oldLeaderSid);
        if (groupId != null) {

            Cursor cGroup = db.query(GROUPS_TABLE_NAME, null,
                    GROUPS_COLUMN_NAME + " = ? AND " +
                            GROUPS_COLUMN_LEADER + " = ? ",
                    new String[] {groupName, oldLeaderSid}, null, null, null);
            cGroup.moveToFirst();
            if (cGroup.getCount() > 0) {
                String id = cGroup.getString(cGroup.getColumnIndexOrThrow(GROUPS_COLUMN_ID));
                ContentValues contentValues = new ContentValues();
                contentValues.put(GROUPS_COLUMN_LEADER, newLeaderSid);
                db.update(GROUPS_TABLE_NAME, contentValues, GROUPS_COLUMN_ID+ " = " + id, null);
            }
            cGroup.close();

            Cursor cMember = db.query(MEMBERS_TABLE_NAME, null,
                                MEMBERS_COLUMN_GROUP_ID + " = ? ",
                                new String[] {groupId}, null, null, null);
            while(cMember.moveToNext()) {
                String id = cMember.getString(cMember.getColumnIndexOrThrow(MEMBERS_COLUMN_ID));
                String sid = cMember.getString(cMember.getColumnIndexOrThrow(MEMBERS_COLUMN_SID));
                String role = cMember.getString(cMember.getColumnIndexOrThrow(MEMBERS_COLUMN_ROLE));
                String subKeyIndex = cMember.getString(cMember.getColumnIndexOrThrow(MEMBERS_COLUMN_SUB_KEY_INDEX));
                if(sid.equals(oldLeaderSid)) {
                    deleteMember(id);
                    insertMessage(new GroupMessage(DELETE_MEMBER,
                            sid, mySid, "", System.currentTimeMillis(), 1, "", ""));
                    continue;
                } else if(sid.equals(newLeaderSid)) {
                    role = LEADER.toString();
                }

                ContentValues contentValues = new ContentValues();
                contentValues.put(MEMBERS_COLUMN_ROLE, role);
                contentValues.put(MEMBERS_COLUMN_SID, sid);
                contentValues.put(MEMBERS_COLUMN_NAME, "");
                contentValues.put(MEMBERS_COLUMN_SUB_KEY_INDEX, subKeyIndex);
                db.update(MEMBERS_TABLE_NAME, contentValues, MEMBERS_COLUMN_ID+ " = " + id, null);

            }
            cMember.close();

            Cursor cMessage = db.query(MESSAGES_TABLE_NAME, null,
                    MESSAGES_COLUMN_OBJECT_GROUP + " = ? AND " +
                    MESSAGES_COLUMN_LEADER + " = ? ",
                    new String[] {groupName, oldLeaderSid}, null, null, null);

            while(cMessage.moveToNext()) {
                String id = cMessage.getString(cMessage.getColumnIndexOrThrow(MESSAGES_COLUMN_ID));
                String type = cMessage.getString(cMessage.getColumnIndexOrThrow(MESSAGES_COLUMN_TYPE));
                String sender = cMessage.getString(cMessage.getColumnIndexOrThrow(MESSAGES_COLUMN_FROM_WHO));
                if(type.equals(CHAT.toString())) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(MESSAGES_COLUMN_LEADER, newLeaderSid);
                    db.update(MESSAGES_TABLE_NAME, contentValues, MESSAGES_COLUMN_ID + " = " + id, null);
                } else {
                    deleteMessage(id);
                    insertMessage(new GroupMessage(DELETE_MESSAGE,
                            sender, mySid, "", System.currentTimeMillis(), 1, "", ""));
                }
            }
            cMessage.close();
        }
    }

    public void setGroupThresholdMode(String groupName, String leader, String mode, String value) {
        String id = getGroupId(groupName, leader);
        if (id != null) {
            Cursor c = db.query(
                    GROUPS_TABLE_NAME,
                    null,
                    GROUPS_COLUMN_ID + " = ?",
                    new String[] {id}, null, null, null);
            c.moveToFirst();
            if (c.getCount() > 0) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(GROUPS_COLUMN_THRESHOLD_MODE, mode);
                contentValues.put(GROUPS_COLUMN_MODE_VALUE, value);
                db.update(GROUPS_TABLE_NAME, contentValues, GROUPS_COLUMN_ID + " = " + id, null);
                c.close();
            }
        }
    }

    public HashMap<String, String> getGroupThresholdMode(String groupName, String leader) {
        String id = getGroupId(groupName, leader);
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("mode", "fix");
        map.put("value", "2");
        if (id != null) {
            Cursor cGroup = db.query(GROUPS_TABLE_NAME, null,
                    GROUPS_COLUMN_NAME + " = ? AND " +
                            GROUPS_COLUMN_LEADER + " = ? ",
                    new String[] {groupName, leader}, null, null, null);
            cGroup.moveToFirst();
            if (cGroup.getCount() > 0) {
                String mode =
                        cGroup.getString(cGroup.getColumnIndexOrThrow(GROUPS_COLUMN_THRESHOLD_MODE));
                String value =
                        cGroup.getString(cGroup.getColumnIndexOrThrow(GROUPS_COLUMN_MODE_VALUE));

                if(mode != null && value != null) {
                    map.clear();
                    map.put("mode", mode);
                    map.put("value", value);
                } else {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(GROUPS_COLUMN_THRESHOLD_MODE, "fix");
                    contentValues.put(GROUPS_COLUMN_MODE_VALUE, "2");
                    db.update(GROUPS_TABLE_NAME, contentValues, GROUPS_COLUMN_ID + " = " + id, null);
                }
                cGroup.close();
            }
        }
        return map;
    }

    public void createGroup(String groupName, String leader) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(GROUPS_COLUMN_NAME, groupName);
            contentValues.put(GROUPS_COLUMN_LEADER, leader);
            contentValues.put(GROUPS_COLUMN_MASTER_KEY_VERSION, 0);
            contentValues.put(GROUPS_COLUMN_SUB_KEY_VERSION, 0);
            contentValues.put(GROUPS_COLUMN_NUMBER_OF_TOTAL_KEYs, 0);
            contentValues.put(GROUPS_COLUMN_KEY_THRESHOLD, 0);
            db.insert(GROUPS_TABLE_NAME, null, contentValues);
            insertMember(new GroupMember(groupName, leader, LEADER, leader, ""));
    }

    public void createSecureGroup(Group group) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(GROUPS_COLUMN_NAME, group.getName());
        contentValues.put(GROUPS_COLUMN_LEADER, group.getLeader());
        contentValues.put(GROUPS_COLUMN_MASTER_KEY_VERSION, group.getMasterKeyVersion());
        contentValues.put(GROUPS_COLUMN_SUB_KEY_VERSION, group.getSubKeyVersion());
        contentValues.put(GROUPS_COLUMN_NUMBER_OF_TOTAL_KEYs, group.getNumberOfTotalKeys());
        contentValues.put(GROUPS_COLUMN_KEY_THRESHOLD, group.getKeyThreshold());
        db.insert(GROUPS_TABLE_NAME, null, contentValues);
        insertMember(new GroupMember(
                group.getName(),
                group.getLeader(),
                LEADER,
                group.getLeader(),
                "",
                new SubKey("", 1)));
    }

    public String getGroupId(String name, String leader) {
        Cursor c = db.query(
                GROUPS_TABLE_NAME,
                new String[] {GROUPS_COLUMN_ID},
                GROUPS_COLUMN_NAME + " = ? AND " + GROUPS_COLUMN_LEADER + " = ? ",
                new String[] {name, leader}, null, null, null, null
        );
        c.moveToFirst();
        if(c.getCount() > 0) {
            String id = c.getString(c.getColumnIndexOrThrow(GROUPS_COLUMN_ID));
            c.close();
            return id;
        } else {
            return null;
        }
    }

    public Group getSecureGroup(String name, String leader) {
        String id = getGroupId(name, leader);
        String masterKeyVersion = "";
        String subKeyVersion = "";
        Integer numberOfTotalKeys = 0;
        Integer keyThreshold = 0;
        if(id != null) {
            Cursor cGroup = db.query(GROUPS_TABLE_NAME, null,
                    GROUPS_COLUMN_NAME + " = ? AND " +
                            GROUPS_COLUMN_LEADER + " = ? ",
                    new String[] {name, leader}, null, null, null);
            cGroup.moveToFirst();
            if (cGroup.getCount() > 0) {
                masterKeyVersion =
                        cGroup.getString(cGroup.getColumnIndexOrThrow(GROUPS_COLUMN_MASTER_KEY_VERSION));
                subKeyVersion =
                        cGroup.getString(cGroup.getColumnIndexOrThrow(GROUPS_COLUMN_SUB_KEY_VERSION));
                numberOfTotalKeys =
                        cGroup.getInt(cGroup.getColumnIndexOrThrow(GROUPS_COLUMN_NUMBER_OF_TOTAL_KEYs));
                keyThreshold =
                        cGroup.getInt(cGroup.getColumnIndexOrThrow(GROUPS_COLUMN_KEY_THRESHOLD));
                cGroup.close();
            } else {
                return null;
            }

            ArrayList<GroupMember> gmList = new ArrayList<GroupMember>();
            Cursor cMember = db.query(
                    MEMBERS_TABLE_NAME,
                    null,
                    MEMBERS_COLUMN_GROUP_ID + " = ? ",
                    new String[]{id}, null, null, null, null);
            while (cMember.moveToNext()) {
                String memberSid = cMember.getString(cMember.getColumnIndexOrThrow(MEMBERS_COLUMN_SID));
                String memberRole = cMember.getString(cMember.getColumnIndexOrThrow(MEMBERS_COLUMN_ROLE));
                String subKeyStr = cMember.getString(cMember.getColumnIndexOrThrow(MEMBERS_COLUMN_SUB_KEY));
                Integer subKeyIndex = cMember.getInt(cMember.getColumnIndexOrThrow(MEMBERS_COLUMN_SUB_KEY_INDEX));
                SubKey subKey;

                if (subKeyStr == null)
                    subKeyStr = "";
                subKey = new SubKey(subKeyStr, subKeyIndex);
                gmList.add(new GroupMember(name, leader, GroupMember.Role.valueOf(memberRole), memberSid, "", subKey));

            }
            cMember.close();
            return new Group(
                    name,
                    gmList,
                    leader,
                    false,
                    masterKeyVersion,
                    subKeyVersion,
                    numberOfTotalKeys,
                    keyThreshold);

        } else {
            return null;
        }
    }

    public void updateSecureGroup(Group group) {
        Group oldGroup = getSecureGroup(group.getName(), group.getLeader());
        String id = getGroupId(group.getName(), group.getLeader());
        if(id != null) {

            Cursor c = db.query(
                    GROUPS_TABLE_NAME,
                    null,
                    GROUPS_COLUMN_ID + " = ?",
                    new String[] {id}, null, null, null);
            c.moveToFirst();
            if (c.getCount() > 0) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(GROUPS_COLUMN_KEY_THRESHOLD, group.getKeyThreshold());
                contentValues.put(GROUPS_COLUMN_NUMBER_OF_TOTAL_KEYs, group.getNumberOfTotalKeys());
                contentValues.put(GROUPS_COLUMN_MASTER_KEY_VERSION, group.getMasterKeyVersion());
                contentValues.put(GROUPS_COLUMN_SUB_KEY_VERSION, group.getSubKeyVersion());
                db.update(GROUPS_TABLE_NAME, contentValues, GROUPS_COLUMN_ID + " = " + id, null);
                c.close();
            }
            for(GroupMember gm: oldGroup.getMembersExcludeLeader()) {
                if(!group.getMembersExcludeLeader().contains(gm))
                    deleteMember(gm);
            }
            for(GroupMember gm: group.getMembers()) {
                if(!oldGroup.getMembers().contains(gm))
                    insertMember(gm);
                else
                    updateMember(gm);
            }

        } else {
            createSecureGroup(group);
            for (GroupMember gm : group.getMembersExcludeLeader()) {
                insertMember(gm);
            }
        }
    }

    public void deleteGroup(String groupName, String groupLeader) {

        String groupId = getGroupId(groupName, groupLeader);
        if (groupId != null) {

            Cursor cMessage = db.query(
                    MESSAGES_TABLE_NAME,
                    null,
                    MESSAGES_COLUMN_OBJECT_GROUP + " = ? AND " +
                            MESSAGES_COLUMN_LEADER + " = ? ",
                    new String[]{groupName, groupLeader}, null, null, null);
            while (cMessage.moveToNext()) {
                String id = cMessage.getString(cMessage.getColumnIndexOrThrow(MESSAGES_COLUMN_ID));
                deleteMessage(id);
            }
            cMessage.close();

            Cursor cMember = db.query(
                    MEMBERS_TABLE_NAME,
                    null,
                    MEMBERS_COLUMN_GROUP_ID + " = ? ",
                    new String[]{groupId}, null, null, null);
            while (cMember.moveToNext()) {
                String id = cMember.getString(cMember.getColumnIndexOrThrow(MEMBERS_COLUMN_ID));
                String sid = cMember.getString(cMember.getColumnIndexOrThrow(MEMBERS_COLUMN_SID));
                deleteMember(id);
                insertMessage(new GroupMessage(DELETE_MEMBER,
                        sid, mySid, "", System.currentTimeMillis(), 1, "", ""));
            }
            cMember.close();

            insertMessage(new GroupMessage(DELETE_GROUP,
                    mySid, "", "", System.currentTimeMillis(), 1, "", ""));

            db.delete(GROUPS_TABLE_NAME, GROUPS_COLUMN_ID + " = " + groupId, null);
        }
    }

    public void deleteGroupAll() {
        Cursor c = db.query(
                GROUPS_TABLE_NAME,
                null, null, null, null, null, null);
        while(c.moveToNext()) {
            String id = c.getString(c.getColumnIndexOrThrow(GROUPS_COLUMN_ID));
            db.delete(GROUPS_TABLE_NAME, GROUPS_COLUMN_ID + " = " + id, null);
        }
        c.close();
    }

    public boolean insertMember(GroupMember gm) {
        String id = getGroupId(gm.getGroupName(), gm.getLeader());
        if (id != null) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MEMBERS_COLUMN_ROLE, gm.getRole().toString());
            contentValues.put(MEMBERS_COLUMN_SID, gm.getSid());
            contentValues.put(MEMBERS_COLUMN_GROUP_ID, Integer.valueOf(id));
            contentValues.put(MEMBERS_COLUMN_SUB_KEY_INDEX, gm.getSubKey().getIndex());
            contentValues.put(MEMBERS_COLUMN_SUB_KEY, gm.getSubKey().getKey());
            Long rowId = db.insert(MEMBERS_TABLE_NAME, null, contentValues);
            Log.d("GroupDAO", "insert member");
            return !rowId.equals(-1L);
        } else {
            return false;
        }
    }

    public boolean updateMember(GroupMember gm) {
        String groupId = getGroupId(gm.getGroupName(), gm.getLeader());
        if (groupId != null) {
            String id = getMemberId(gm.getGroupName(), gm.getLeader(), gm.getSid());
            ContentValues contentValues = new ContentValues();
            contentValues.put(MEMBERS_COLUMN_ROLE, gm.getRole().toString());
            contentValues.put(MEMBERS_COLUMN_SID, gm.getSid());
            contentValues.put(MEMBERS_COLUMN_GROUP_ID, groupId);
            contentValues.put(MEMBERS_COLUMN_SUB_KEY_INDEX, gm.getSubKey().getIndex());
            contentValues.put(MEMBERS_COLUMN_SUB_KEY, gm.getSubKey().getKey());
            db.update(MEMBERS_TABLE_NAME, contentValues, MEMBERS_COLUMN_ID + " = " + id ,null);
            Log.d("GroupDAO", "update member");
            return true;
        } else {
            return false;
        }
    }


    public String getMemberId(String groupName, String leader, String sid) {

        String groupId = getGroupId(groupName, leader);
        if (groupId != null) {
            Cursor c = db.query(
                    MEMBERS_TABLE_NAME,
                    new String[]{MEMBERS_COLUMN_ID},
                            MEMBERS_COLUMN_SID + "= ? AND " +
                            MEMBERS_COLUMN_GROUP_ID + " = ? ",
                    new String[]{sid, groupId}, null, null, null);
            c.moveToFirst();
            if (c.getCount() > 0) {
                String id = c.getString(c.getColumnIndexOrThrow(MEMBERS_COLUMN_ID));
                c.close();
                return id;

            } else {
                c.close();
                return null;
            }

        } else {
            return null;
        }
    }

    public GroupMember getMember(String groupName, String leader, String sid) {
        String id = getMemberId(groupName, leader, sid);
        if (id != null) {
            Cursor c = db.query(
                    MEMBERS_TABLE_NAME,
                    null,
                    MEMBERS_COLUMN_ID+ " = ? ",
                    new String[]{id}, null, null, null);
            c.moveToFirst();
            if (c.getCount() > 0) {
                String role = c.getString(c.getColumnIndexOrThrow(MEMBERS_COLUMN_ROLE));
                Integer subKeyIndex = c.getInt(c.getColumnIndexOrThrow(MEMBERS_COLUMN_SUB_KEY_INDEX));
                String subKey = c.getString(c.getColumnIndexOrThrow(MEMBERS_COLUMN_SUB_KEY));
                if (subKey == null) {
                    subKey = "";
                }
                c.close();
                return new GroupMember(
                        groupName,
                        leader,
                        GroupMember.Role.valueOf(role),
                        sid,
                        "",
                        new SubKey(subKey, subKeyIndex));
            } else {
                c.close();
                return null;
            }
        } else {
            return null;
        }
    }

    public void deleteMember(String id) {

        db.delete(MEMBERS_TABLE_NAME, MEMBERS_COLUMN_ID + " = " + id, null);

    }

    public void deleteMember(GroupMember gm) {
        String id = getMemberId(gm.getGroupName(), gm.getLeader(), gm.getSid());
        if (id != null) {
            deleteMember(id);
            insertMessage(new GroupMessage(DELETE_MEMBER,
                    gm.getSid(), mySid, "", System.currentTimeMillis(), 1, "", ""));
        }
    }

    public void deleteMemberAll() {
        Cursor c = db.query(
                MEMBERS_TABLE_NAME,
                null, null, null, null, null, null);
        while(c.moveToNext()) {
            String id = c.getString(c.getColumnIndexOrThrow(MEMBERS_COLUMN_ID));
            db.delete(MEMBERS_TABLE_NAME, MEMBERS_COLUMN_ID + " = " + id, null);
        }
        c.close();
    }

    public ArrayList<String> getOtherMemberList(String groupName, String groupLeader) {
        ArrayList<String> list = new ArrayList<String>();
        String groupId = getGroupId(groupName, groupLeader);
        if (groupId != null) {

            Cursor c = db.query(
                    MEMBERS_TABLE_NAME,
                    null,
                    MEMBERS_COLUMN_GROUP_ID + " = ? ",
                    new String[]{groupId},
                    null, null, null, null);
            while (c.moveToNext()) {
                String member = c.getString(c.getColumnIndexOrThrow(MEMBERS_COLUMN_SID));
                if (!member.equals(mySid)) {
                    list.add(member);
                }
            }
            c.close();
        }
        return list;
    }

    public Integer getGroupSize(String groupName, String groupLeader) {
        Integer count = 0;
        String groupId = getGroupId(groupName, groupLeader);
        if (groupId != null) {
            Cursor c = db.rawQuery(
                    "SELECT COUNT(*) FROM " + MEMBERS_TABLE_NAME +
                            " WHERE " + MEMBERS_COLUMN_GROUP_ID + " = ? "
                    , new String[] {groupId});
            c.moveToFirst();
            count = c.getInt(0);
            c.close();
        }
        return count;
    }

    public ArrayList<String> getAbbreviationAllMemberList(String groupName, String groupLeader) {
        ArrayList<String> list = new ArrayList<String>();
        String groupId = getGroupId(groupName, groupLeader);
        if (groupId != null) {
            Cursor c = db.query(
                    MEMBERS_TABLE_NAME,
                    null,
                    MEMBERS_COLUMN_GROUP_ID + " = ? ",
                    new String[]{groupId},
                    null, null, null, null);
            while (c.moveToNext()) {
                String member = c.getString(c.getColumnIndexOrThrow("sid"));
                if(5 < member.length()){
                    if (!member.equals(mySid) && member.equals(groupLeader)) {
                        list.add(member.substring(0, 5) + "* (Leader)");
                    } else if (!member.equals(mySid)) {
                        list.add(member.substring(0, 5) + "* ");
                    } else if (member.equals(groupLeader)) {
                        list.add(member.substring(0, 5) + "* (Me, Leader)");
                    } else {
                        list.add(member.substring(0, 5) + "* (Me)");
                    }
                } else {
                    if (!member.equals(mySid) && member.equals(groupLeader)) {
                        list.add(member + " (Leader)");
                    } else if (!member.equals(mySid)) {
                        list.add(member);
                    } else if (member.equals(groupLeader)) {
                        list.add(member + " (Me, Leader)");
                    } else {
                        list.add(member + " (Me)");
                    }
                }

            }
            c.close();
        }
        return list;
    }



    public ArrayList<Group> getMyGroupList() {
        ArrayList<Group> groupList = new ArrayList<Group>();
        Cursor c = db.query(
                GROUPS_TABLE_NAME,
                null,
                GROUPS_COLUMN_LEADER + "= ?",
                new String[] {mySid}, null, null, null);
        while(c.moveToNext()) {
            String groupName = c.getString(c.getColumnIndexOrThrow(GROUPS_COLUMN_NAME));
            Group group = new Group(groupName, new ArrayList<GroupMember>(), mySid, true);
            groupList.add(group);
        }
        return groupList;
    }

    public ArrayList<Group> getOtherGroupList() {
        ArrayList<Group> groupList = new ArrayList<Group>();
        Cursor c = db.query(
                GROUPS_TABLE_NAME,
                null, null, null, null, null, null);
        while(c.moveToNext()) {
            String groupName = c.getString(c.getColumnIndexOrThrow(GROUPS_COLUMN_NAME));
            String leaderSid = c.getString(c.getColumnIndexOrThrow(GROUPS_COLUMN_LEADER));
            if (!leaderSid.equals(mySid)) {
                Group group = new Group(groupName, new ArrayList<GroupMember>(), leaderSid, false);
                groupList.add(group);
            }
        }
        return groupList;
    }
    public boolean isMyGroup(String groupName, String leader) {
        boolean result = false;
        if (leader.equals(mySid)) {
            Cursor c = db.query(
                    GROUPS_TABLE_NAME,
                    new String[] {GROUPS_COLUMN_ID},
                    GROUPS_COLUMN_NAME + "= ? AND " +
                            GROUPS_COLUMN_LEADER + " = ?",
                    new String[] {groupName, mySid}, null, null, null);
            if (c.getCount() > 0) {
                result = true;
            }
            c.close();
        }
        return result;
    }

    public boolean haveSentJoinRequest(String groupName, String leader) {
        boolean result = false;
        Cursor c = db.query(
                MESSAGES_TABLE_NAME,
                new String[] {MESSAGES_COLUMN_ID},
                MESSAGES_COLUMN_TYPE + " = ? AND " +
                        MESSAGES_COLUMN_FROM_WHO+ " = ? AND " +
                        MESSAGES_COLUMN_TO_WHO+ " = ? AND " +
                        MESSAGES_COLUMN_LEADER + " = ? AND " +
                        MESSAGES_COLUMN_OBJECT_GROUP + " = ? ",
                new String[] {"JOIN", mySid, leader, leader, groupName}, null, null, null);
        if (c.getCount() > 0) {
            result = true;
        }
        c.close();
        return result;
    }

    public boolean haveSentReCreateResponse(String groupName, String leader) {
        boolean result = false;
        Cursor c = db.query(
                MESSAGES_TABLE_NAME,
                new String[] {MESSAGES_COLUMN_ID},
                MESSAGES_COLUMN_TYPE + " = ? AND " +
                        MESSAGES_COLUMN_FROM_WHO+ " = ? AND " +
                        MESSAGES_COLUMN_TO_WHO+ " = ? AND " +
                        MESSAGES_COLUMN_LEADER + " = ? AND " +
                        MESSAGES_COLUMN_OBJECT_GROUP + " = ? ",
                new String[] {"RE_CREATE_GROUP_RESPONSE", mySid, leader, leader, groupName}, null, null, null);
        if (c.getCount() > 0) {
            result = true;
        }
        c.close();
        return result;
    }

    public boolean increaseMasterKeyVersion(String groupName, String leader) {
        boolean result = false;
        String id = getGroupId(groupName, leader);
        if(id != null) {
            Cursor c = db.query(
                    GROUPS_TABLE_NAME,
                    new String[] {GROUPS_COLUMN_MASTER_KEY_VERSION},
                    GROUPS_COLUMN_ID + " = ?",
                    new String[] {id}, null, null, null);
            c.moveToFirst();
            if (c.getCount() > 0) {
                Integer masterKeyVersion = c.getInt(c.getColumnIndexOrThrow(GROUPS_COLUMN_MASTER_KEY_VERSION));
                ContentValues contentValues = new ContentValues();
                contentValues.put(GROUPS_COLUMN_MASTER_KEY_VERSION, masterKeyVersion + 1);
                db.update(GROUPS_TABLE_NAME, contentValues, GROUPS_COLUMN_ID + " = " + id, null);
                c.close();
                result = true;
            } else {
                c.close();
            }
        }
        return result;
    }

    public boolean increaseSubKeyVersion(String groupName, String leader) {
        boolean result = false;
        String id = getGroupId(groupName, leader);
        if(id != null) {
            Cursor c = db.query(
                    GROUPS_TABLE_NAME,
                    new String[] {GROUPS_COLUMN_SUB_KEY_VERSION},
                    GROUPS_COLUMN_ID + " = ?",
                    new String[] {id}, null, null, null);
            c.moveToFirst();
            if (c.getCount() > 0) {
                Integer subKeyVersion = c.getInt(c.getColumnIndexOrThrow(GROUPS_COLUMN_SUB_KEY_VERSION));
                ContentValues contentValues = new ContentValues();
                contentValues.put(GROUPS_COLUMN_SUB_KEY_VERSION, subKeyVersion + 1);
                db.update(GROUPS_TABLE_NAME, contentValues, GROUPS_COLUMN_ID + " = " + id, null);
                c.close();
                result = true;
            } else {
                c.close();
            }
        }
        return result;
    }

    public boolean setNumberOfTotalKeys(String groupName, String leader, Integer n) {
        boolean result = false;
        String id = getGroupId(groupName, leader);
        if(id != null) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(GROUPS_COLUMN_NUMBER_OF_TOTAL_KEYs, n);
            db.update(GROUPS_TABLE_NAME, contentValues, GROUPS_COLUMN_ID + " = " + id, null);
            result = true;
        }
        return result;
    }

    public boolean setKeyThreshold(String groupName, String leader, Integer k) {
        boolean result = false;
        String id = getGroupId(groupName, leader);
        if(id != null) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(GROUPS_COLUMN_KEY_THRESHOLD, k);
            db.update(GROUPS_TABLE_NAME, contentValues, GROUPS_COLUMN_ID + " = " + id, null);
            result = true;
        }
        return result;
    }

    public boolean setupSubKeys(String groupName, String leader, ArrayList<SubKey> subKeyList) {
        ArrayList<SubKey> subKeyListTemp = new ArrayList<SubKey>(subKeyList);
        String groupId = getGroupId(groupName, leader);
        if (groupId != null) {
            Cursor c = db.query(
                    MEMBERS_TABLE_NAME,
                    null,
                    MEMBERS_COLUMN_GROUP_ID + " = ? ",
                    new String[]{groupId},
                    null, null, null, null);

            while (c.moveToNext() && subKeyListTemp.size() > 0) {
                String id = c.getString(c.getColumnIndexOrThrow(MEMBERS_COLUMN_ID));
                String role = c.getString(c.getColumnIndexOrThrow(MEMBERS_COLUMN_ROLE));
                ContentValues contentValues = new ContentValues();
                if(role.equals("LEADER")) {
                    SubKey subKey = subKeyListTemp.remove(0);
                    contentValues.put(MEMBERS_COLUMN_SUB_KEY, subKey.getKey());
                    contentValues.put(MEMBERS_COLUMN_SUB_KEY_INDEX, subKey.getIndex());
                } else {
                    SubKey subKey = subKeyListTemp.remove(subKeyListTemp.size()-1);
                    contentValues.put(MEMBERS_COLUMN_SUB_KEY_INDEX, subKey.getIndex());
                }
                db.update(MEMBERS_TABLE_NAME, contentValues, MEMBERS_COLUMN_ID + " = " + id ,null);
            }
            c.close();
            return true;
        } else {
            return false;
        }
    }
}
