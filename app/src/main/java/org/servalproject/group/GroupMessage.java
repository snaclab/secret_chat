package org.servalproject.group;

import org.servalproject.group.secret.SubKey;

import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;


public class GroupMessage {

    private Type type;
    private String fromWho;
    private String toWho;
    private String objectGroup;
    private Long timestamp;
    private Integer done;
    private String content;
    private String groupLeader;
    public static final String SID = "sid";
    public static final String SUB_KEY_INDEX = "sub_key_index";

    public GroupMessage(Type type, String fromWho, String toWho, String objectGroup,
                        Long timestamp, Integer done, String content, String groupLeader) {

        this.type = type;
        this.fromWho = fromWho;
        this.toWho = toWho;
        this.objectGroup = objectGroup;
        this.timestamp = timestamp;
        this.done = done;
        this.content = content;
        this.groupLeader = groupLeader;
    }

    public enum Type {
        JOIN, LEAVE, DONE_LEAVE, CHAT,
        SECURE_GROUP_UPDATE, SUB_KEY_REQUEST, SUB_KEY_RESPONSE,
        RE_CREATE_GROUP_REQUEST, RE_CREATE_GROUP_RESPONSE,
        CHANGE_LEADER, CHANGE_LEADER_RESPONSE,
        DELETE_GROUP, DELETE_MEMBER, DELETE_MESSAGE
    }

    public Type getType() {
        return type;
    }

    public String getFromWho() {
        return fromWho;
    }

    public String getToWho() {
        return toWho;
    }

    public String getObjectGroup() {
        return objectGroup;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public Integer getDone() {
        return done;
    }

    public String getContent() {
        return content;
    }

    public String getGroupLeader() {
        return groupLeader;
    }

    public static ArrayList<String> extractGroupMessage(String message) {
        ArrayList<String> result = new ArrayList<String>();
        Pattern pattern = Pattern.compile("^Group Message:.*");
        Matcher matcher = pattern.matcher(message);
        if(matcher.matches()) {
            String groupMessage = message.substring(14, message.length());
            String[] groupMessages = groupMessage.split(",");
            for(int i = 0; i < groupMessages.length; i++) {
                result.add(groupMessages[i]);
            }
            return result;
        }
        return null;
    }

    public static boolean isGroupMessage(String message) {
        Pattern pattern = Pattern.compile("^Group Message:.*");
        Matcher matcher = pattern.matcher(message);
        return matcher.matches();
    }

    public static String generateUnicastGroupMessage(Type type, String groupName, String leader, String content) {
        String message = "";
        switch(type) {
            case JOIN:
                message =
                        "Group Message:JOIN," +
                                groupName + "," +
                                leader + "," +
                                content;
                break;
            case LEAVE:
                message =
                        "Group Message:LEAVE," +
                                groupName + "," +
                                leader + "," +
                                content;
                break;
            case DONE_LEAVE:
                message =
                        "Group Message:DONE_LEAVE," +
                                groupName + "," +
                                leader + "," +
                                content;
                break;
            case CHANGE_LEADER:
                message =
                        "Group Message:CHANGE_LEADER," +
                                groupName + "," +
                                leader + "," +
                                content;
                break;
            case RE_CREATE_GROUP_RESPONSE:
                message =
                        "Group Message:RE_CREATE_GROUP_RESPONSE," +
                                groupName + "," +
                                leader + "," +
                                content;
                break;
            case CHANGE_LEADER_RESPONSE:
                message =
                        "Group Message:CHANGE_LEADER_RESPONSE," +
                                groupName + "," +
                                leader + "," +
                                content;
                break;
        }
        return message;
    }

    public static String generateUnicastGroupMessage(Type type, Group group, GroupMember me, GroupMember receiver) {
        String message = "";
        String groupName = group.getName();
        String leader = group.getLeader();
        String membersList = group.getSecureMembersString();
        String masterKeyVersion = group.getMasterKeyVersion();
        String subKeyVersion = group.getSubKeyVersion();
        Integer numberOfTotalKeys = group.getNumberOfTotalKeys();
        Integer keyThreshold = group.getKeyThreshold();

        switch(type) {
            case SECURE_GROUP_UPDATE:
                SubKey subKey = receiver.getSubKey();
                message =
                        "Group Message:SECURE_GROUP_UPDATE," +
                                groupName + "," +
                                leader  + "," +
                                numberOfTotalKeys + "," +
                                keyThreshold + "," +
                                masterKeyVersion + "," +
                                subKeyVersion + "," +
                                subKey.getKey() + "," +
                                membersList ;
                break;
            case SUB_KEY_RESPONSE:
                if (me != null && !me.getSubKey().getKey().equals("")) {
                    message =
                            "Group Message:SUB_KEY_RESPONSE," +
                                    groupName + "," +
                                    leader + "," +
                                    me.getSubKey().getKey();
                }
                break;
        }

        return message;
    }

    public static String generateMulticastGroupMessage(Type type, Group group, GroupMember me, String content) {
        String message = "";
        String groupName = group.getName();
        String leader = group.getLeader();
        String masterKeyVersion = group.getMasterKeyVersion();
        String subKeyVersion = group.getSubKeyVersion();

        switch(type) {
            case CHAT:
                message =
                        "Group Message:CHAT," +
                                groupName + "," +
                                leader + "," +
                                content;
                break;
            case SUB_KEY_REQUEST:
                message =
                        "Group Message:SUB_KEY_REQUEST," +
                                groupName + "," +
                                leader + "," +
                                masterKeyVersion + "," +
                                subKeyVersion;
                break;
            case RE_CREATE_GROUP_REQUEST:
                message =
                        "Group Message:RE_CREATE_GROUP_REQUEST," +
                                groupName + "," +
                                leader + "," +
                                content;
                break;

        }
        return message;
    }

    public static HashMap<String, String> parseSecureMember(String text) {
        String sid = "";
        String subKeyIndex = "";
        Pattern pattern = Pattern.compile("\\w+\\[\\d+\\]");
        Matcher matcher = pattern.matcher(text);
        if(matcher.matches()) {
            sid = text.substring(0, text.indexOf("["));
            subKeyIndex = text.substring(text.indexOf("[")+1, text.indexOf("]"));
        }
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(SID, sid);
        map.put(SUB_KEY_INDEX, subKeyIndex);
        return map;
    }

}
