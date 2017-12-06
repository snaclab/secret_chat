package org.servalproject.group;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Objects;

public class Group {
    private String name = "";
    private String leader;
    private boolean isMyGroup;
    private String masterKeyVersion;
    private String subKeyVersion;
    private Integer numberOfTotalKeys;
    private Integer keyThreshold;
    private ArrayList<GroupMember> members = new ArrayList<GroupMember>();

    public Group(String name) {
        this.name = name;
    }

    public Group(String name, ArrayList<GroupMember> members, String leader, boolean isMyGroup) {
        this.name = name;
        this.members = members;
        this.leader = leader;
        this.isMyGroup = isMyGroup;
    }

    public Group(
            String name,
            ArrayList<GroupMember> members,
            String leader,
            boolean isMyGroup,
            String masterKeyVersion,
            String subKeyVersion,
            Integer numberOfTotalKeys,
            Integer keyThreshold) {
        this.name = name;
        this.members = members;
        this.leader = leader;
        this.isMyGroup = isMyGroup;
        this.masterKeyVersion = masterKeyVersion;
        this.subKeyVersion = subKeyVersion;
        this.numberOfTotalKeys = numberOfTotalKeys;
        this.keyThreshold = keyThreshold;
    }

    public String getMasterKeyVersion() {
        return masterKeyVersion;
    }

    public String getSubKeyVersion() {
        return subKeyVersion;
    }

    public Integer getNumberOfTotalKeys() {
        return numberOfTotalKeys;
    }

    public Integer getKeyThreshold() {
        return keyThreshold;
    }

    public String getName() {
        return name;
    }

    public ArrayList<GroupMember> getMembers() {
        return members;
    }

    public ArrayList<GroupMember> getMembersExcludeLeader() {
        ArrayList<GroupMember> membersExcludeLeader = new ArrayList<GroupMember>();
        for (GroupMember member: members) {
            if (!member.getSid().equals(leader)) {
                membersExcludeLeader.add(member);
            }
        }
        return membersExcludeLeader;
    }

    public String getLeader() {
        return leader;
    }

    public boolean getIsMyGroup() {
        return isMyGroup;
    }

    public String getLeaderAbbreviation() {
        return leader.substring(0, 5) + "*";
    }

    public String getSecureMembersString() {

        ArrayList<String> memberList= new ArrayList<String>();
        for (GroupMember member: members) {
            memberList.add(member.getSid() + "[" + member.getSubKey().getIndex() + "]");
        }

        return TextUtils.join(",",memberList);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Group) {
            Group toCompare = (Group) o;
            return this.leader.equals(toCompare.getLeader()) &&
                   this.name.equals(toCompare.getName());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, leader);
    }



}
