package org.servalproject.group;

/**
 * Created by sychen on 2017/5/19.
 */

public class GroupRequest {
    private String type = "";
    private String groupName = "";
    private String leader = "";
    private String objectSid = "";
    private int id = 0;

    GroupRequest(String type, String groupName, String leader, String objectSid, int id) {
        this.type = type;
        this.groupName = groupName;
        this.leader = leader;
        this.objectSid = objectSid;
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getLeader() {
        return leader;
    }

    public String getObjectSid() {
        return objectSid;
    }

    public int getId() {
        return id;
    }
}
