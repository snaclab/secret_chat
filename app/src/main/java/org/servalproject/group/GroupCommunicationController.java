package org.servalproject.group;

import android.os.AsyncTask;

import org.servalproject.ServalBatPhoneApplication;
import org.servalproject.servaldna.SubscriberId;
import org.servalproject.servaldna.keyring.KeyringIdentity;

import java.util.ArrayList;

/**
 * Created by sychen on 2017/3/31.
 */

public class GroupCommunicationController {
    private ServalBatPhoneApplication servalApp;
    private KeyringIdentity identity;
    private SubscriberId mySid;

    public GroupCommunicationController(ServalBatPhoneApplication servalApp, KeyringIdentity identity) {
        this.servalApp = servalApp;
        this.identity = identity;
        this.mySid = identity.sid;

    }

    public void unicast(String sender, String receiver, String messageText) {
        try {
            if (messageText==null || "".equals(messageText))
                return;

            new AsyncTask<Object, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Object... args) {
                    boolean result = false;
                    ServalBatPhoneApplication app = (ServalBatPhoneApplication) args[3];
                    try {
                        String sender = (String) args[0];
                        String receiver = (String) args[1];
                        String text = (String) args[2];
                        SubscriberId senderSid = new SubscriberId(sender);
                        SubscriberId receiverSid = new SubscriberId(receiver);
                        app.server.getRestfulClient().meshmsSendMessage(senderSid, receiverSid, text);
                        result = true;
                    } catch (Exception e) {
                        app.displayToastMessage(e.getMessage());
                    }
                    return result;
                }
            } .execute(sender, receiver, messageText, servalApp);
        } catch (Exception e) {
            servalApp.displayToastMessage(e.getMessage());
        }
    }

    public void multicast(GroupMember sender, Group group, String text) {
        try {
            ArrayList<GroupMember> members = group.getMembers();
            for(GroupMember member: members) {
                if(!member.equals(sender))
                    unicast(sender.getSid(), member.getSid(), text);
            }
        } catch (Exception e) {
            servalApp.displayToastMessage(e.getMessage());
        }
    }
    public void multicastExcludeLeader(
            GroupMember sender, Group group, String text) {

        try {
            ArrayList<GroupMember> members = group.getMembers();
            for(GroupMember member: members) {
                if(!member.equals(sender) &&
                        !member.getSid().equals(group.getLeader()))
                    unicast(sender.getSid(), member.getSid(), text);
            }
        } catch (Exception e) {
            servalApp.displayToastMessage(e.getMessage());
        }
    }
}
