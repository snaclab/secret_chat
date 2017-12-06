package org.servalproject.group.secret;

import com.tiemens.secretshare.engine.SecretShare;
import com.tiemens.secretshare.exceptions.SecretShareException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringEscapeUtils;

public class SecretController {

    private SecretKey mMasterKey;
    private SecretShare.PublicInfo mPublicInfo;
    private ArrayList<SubKey> mSubKeyList;
    private static final int KEY_LENGTH = 4096;
    public SecretController() {

    }

    public ArrayList<SubKey> generateSubKeyList() {
        mSubKeyList = new ArrayList<SubKey>();
        if (masterKeyExist() && mPublicInfo != null) {
            SecretShare ss = new SecretShare(mPublicInfo);
            SecureRandom random = new SecureRandom();
            SecretShare.SplitSecretOutput out = ss.split(mMasterKey.getKeyBigInteger(), random);
            List<SecretShare.ShareInfo> shareInfo = out.getShareInfos();
            for (SecretShare.ShareInfo share : shareInfo) {
                mSubKeyList.add(new SubKey(share.getShare(), share.getIndex()));
            }
        }
        return mSubKeyList;
    }

    public void generatePublicInfo(Integer n, Integer k) {
        mPublicInfo = new SecretShare.PublicInfo(
            n, k, SecretShare.getPrimeUsedFor4096bigSecretPayload(), "");
    }
    
    public String getPublicInfo() {
        return mPublicInfo.toString();
    }

    public SecretKey reconstructMasterKey(
        ArrayList<SubKey> subKeyList) {

        SecretKey masterKey = new SecretKey("");
        SecretShare solver = new SecretShare(mPublicInfo);
        ArrayList<SecretShare.ShareInfo> shareInfoList =
            new ArrayList<SecretShare.ShareInfo>();
        for (int i = 0; i < subKeyList.size(); i++) {
            shareInfoList.add(new SecretShare.ShareInfo(subKeyList.get(i).getIndex(), subKeyList.get(i).getKeyBigInteger(), mPublicInfo));
        }
        try {
            SecretShare.CombineOutput solved = solver.combine(shareInfoList);
            masterKey = new SecretKey(solved.getSecret());
        } catch (SecretShareException e) {
            System.out.println("Not enough keys");
        }
        mMasterKey = masterKey;
        return masterKey;
    }

    public ArrayList<SubKey> updateSubKeyList() {

        ArrayList<SubKey> keyList =
            generateSubKeyList();
        return keyList;
    }

    public ArrayList<SubKey> getSubkeyList() {
        return mSubKeyList;
    }

    public SecretKey getMasterKey() {
        return mMasterKey;
    }
    public void generateMasterKey() {
        SecureRandom random = new SecureRandom();
        mMasterKey = new SecretKey(new BigInteger(KEY_LENGTH, random).toString(32));

    }
    public void setMasterKey(SecretKey masterKey) {
        mMasterKey = masterKey;
    }
    public boolean masterKeyExist() {
        return (mMasterKey != null) ? true : false;
    }

    public String encryptData(String input, SecretKey key) {

        StringBuffer output = new StringBuffer();
        String keyString = key.getKey();
        String inputUnescape = StringEscapeUtils.unescapeJava(input);
        for (int i = 0; i < inputUnescape.length(); i++) {
            char code= (char) (inputUnescape.charAt(i) ^ keyString.charAt(i % keyString.length()));
            output.append(code);
        }

        return StringEscapeUtils.escapeJava(output.toString());
    }
    public String decryptData(String input, SecretKey key) {
        return encryptData(input, key);
    }
}
