package org.servalproject.group;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Created by sychen on 2017/3/30.
 */
public class GroupMessageTest {
    @Test
    public void parseSecureMember() throws Exception {
        HashMap<String, String> map = GroupMessage.parseSecureMember("d1as5a1c53a[10]");
        assertTrue(("d1as5a1c53a".equals(map.get(GroupMessage.SID)))
                &&("10".equals(map.get(GroupMessage.SUB_KEY_INDEX))));
    }

}