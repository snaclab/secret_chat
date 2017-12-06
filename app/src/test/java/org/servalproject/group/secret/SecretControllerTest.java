package org.servalproject.group.secret;

import org.junit.Test;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;


public class SecretControllerTest {
    SecretController secretController = new SecretController();
    SecretKey masterKey = new SecretKey("");
    ArrayList<SubKey> subKeyList = new ArrayList<SubKey>();

    @Test
    public void testKey() {
        secretController.generateMasterKey();
        masterKey = secretController.getMasterKey();
        System.out.println("generate masterKey...");
        System.out.println(masterKey.getKey());
        secretController.generatePublicInfo(5, 3);
        System.out.println(secretController.getPublicInfo());
        System.out.println("generate sub keys with (5,3) scheme...");
        subKeyList = secretController.generateSubKeyList();
        for (SecretKey key: subKeyList) {
            System.out.println(key.getKey());
        }
        SecretKey reconstructedKey = secretController.reconstructMasterKey(subKeyList);
        System.out.println("reconstruct master keys...");
        System.out.println(reconstructedKey.getKey());
        System.out.println("update sub keys...");
        subKeyList = secretController.updateSubKeyList();
        for (SecretKey key: subKeyList) {
            System.out.println(key.getKey());
        }
        reconstructedKey = secretController.reconstructMasterKey(subKeyList);
        System.out.println("reconstruct master keys...");
        System.out.println(reconstructedKey.getKey());

        ArrayList<SubKey> enoughSubkeys =
            new ArrayList<SubKey>(subKeyList.subList(0,3));
        reconstructedKey = secretController.reconstructMasterKey(enoughSubkeys);
        System.out.println("reconstruct master keys with 3 sub keys...");
        System.out.println(reconstructedKey.getKey());

        ArrayList<SubKey> notEnoughSubkeys =
            new ArrayList<SubKey>(subKeyList.subList(0,1));
        System.out.println("reconstruct master keys with 2 sub keys...");
        reconstructedKey = secretController.reconstructMasterKey(notEnoughSubkeys);
        System.out.println(reconstructedKey.getKey());
        System.out.println("original data...");
        String oriData = "hello, world 我是秘密哈哈!@#&(*&(*)(#";
        System.out.println(oriData);
        String enData =  secretController.encryptData(oriData, masterKey);
        System.out.println("encrypted data...");
        System.out.println(enData);
        String data =  secretController.decryptData(enData, masterKey);
        System.out.println("decrypted data...");
        System.out.println(data);
        assertEquals(data, oriData);
    }

    @Test
    public void testEncrypt() {
        secretController.generateMasterKey();
        masterKey = secretController.getMasterKey();
        String oriText = "hello, snaclab";
        String enData = secretController.encryptData(oriText, masterKey);
        System.out.println(enData);
        String deData = secretController.decryptData(enData, masterKey);
        System.out.println(deData);
    }
}

