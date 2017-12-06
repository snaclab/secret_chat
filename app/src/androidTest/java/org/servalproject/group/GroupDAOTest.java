package org.servalproject.group;

import static org.servalproject.group.GroupMember.Role.*;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.ArrayList;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.*;

/**
 * Created by sychen on 2017/3/24.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GroupDAOTest {
    GroupDAO groupDAO;

    @Before
    public void initGroupDAO() {
        groupDAO = new GroupDAO(getInstrumentation().getTargetContext(), "MySid");
    }

    @Test
    public void stage_001_deleteGroupAll() throws Exception {
        groupDAO.deleteGroupAll();
    }

    @Test
    public void stage_002_deleteMemberAll() throws Exception {
        groupDAO.deleteMemberAll();
    }

    @Test
    public void stage_003_createGroup() throws Exception {
        groupDAO.createGroup("Sinica", "123456789");
    }

    @Test
    public void stage_004_getGroupId() throws Exception {
        String id = groupDAO.getGroupId("Sinica", "123456789");
        assertEquals("1", id);
    }

    @Test
    public void stage_005_getGroup() throws Exception {
        Group group = groupDAO.getSecureGroup("Sinica", "123456789");
        assertEquals("123456789", group.getLeader());
    }

    @Test
    public void stage_006_insertMember() throws Exception {
        assertTrue(groupDAO.insertMember(new GroupMember("Sinica","123456789", MEMBER,"001","")));
    }

    @Test
    public void stage_007_getMemberId() throws Exception {
        String id = groupDAO.getMemberId("Sinica","123456789","001");
        assertEquals("2", id);
    }

    @Test
    public void stage_008_getMemberList() throws Exception {
        ArrayList<String> memberList = groupDAO.getOtherMemberList("Sinica","123456789");
        assertTrue(memberList.contains("001"));
    }

    @Test
    public void stage_009_deleteMember() throws Exception {
        groupDAO.deleteMember(new GroupMember("Sinica","123456789", MEMBER,"001",""));
        String id = groupDAO.getMemberId("Sinica","123456789","001");
        assertNull(id);
    }

    @Test
    public void stage_010_updateGroup() throws Exception {
        ArrayList<GroupMember> members = new ArrayList<GroupMember>();
        members.add(new GroupMember("Sinica","123456789", MEMBER,"002",""));
        groupDAO.updateSecureGroup(new Group("Sinica", members,"123456789",false));
        String id = groupDAO.getMemberId("Sinica","123456789","002");
        assertNotNull(id);
    }

    @Test
    public void stage_011_changeLeader() throws Exception {
        groupDAO.insertMember(new GroupMember("Sinica","123456789", MEMBER,"456789123",""));
        groupDAO.changeLeader("Sinica","123456789","456789123");
        Group group = groupDAO.getSecureGroup("Sinica","456789123");
        String id = groupDAO.getMemberId("Sinica","456789123","002");
        assertTrue(
                ("456789123".equals(group.getLeader()))&&
                (groupDAO.getGroupId("Sinica","123456789") == null) &&
                        (id != null)
        );
    }

    @Test
    public void stage_012_deleteGroup() throws Exception {
        groupDAO.deleteGroup("Sinica", "456789123");
        String groupId = groupDAO.getGroupId("Sinica", "456789123");
        String memberId = groupDAO.getMemberId("Sinica","456789123","002");
        assertTrue((groupId == null ) && (memberId == null));
    }


}