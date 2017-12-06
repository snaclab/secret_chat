package org.servalproject.group;

import org.servalproject.group.secret.SubKey;

import java.util.Objects;

public class GroupMember {

    private String groupName;
    private Role role;
    private String sid;
    private String memberName;
    private String leader;
    private SubKey subKey = new SubKey("",0);

    public GroupMember(String groupName, String leader, Role role, String sid, String memberName) {
        this.groupName = groupName;
        this.leader = leader;
        this.role = role;
        this.sid = sid;
        this.memberName = memberName;
    }
    public GroupMember(
            String groupName,
            String leader,
            Role role,
            String sid,
            String memberName,
            SubKey subKey) {
        this.groupName = groupName;
        this.leader = leader;
        this.role = role;
        this.sid = sid;
        this.memberName = memberName;
        this.subKey = subKey;
    }

    public enum Role {
        NONE, LEADER, MEMBER
    }
    public SubKey getSubKey() {
        return subKey;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getLeader() {
        return leader;
    }

    public Role getRole() {
        return role;
    }

    public String getSid() {
        return sid;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public void setSubKey(SubKey subKey) {
        this.subKey = subKey;
    }
    @Override
    public boolean equals(Object o) {
        if(o instanceof GroupMember) {
            GroupMember toCompare = (GroupMember) o;
            return this.sid.equals(toCompare.getSid()) &&
                   this.role.equals(toCompare.getRole()) &&
                   this.leader.equals(toCompare.getLeader());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sid, leader, groupName);
    }

}
