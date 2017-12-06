package org.servalproject.group.secret;

import java.math.BigInteger;

public class SecretKey {
    protected String mKey;
    protected BigInteger mKeyBigInteger;

    public SecretKey(String key) {
        mKey = key;
        if (!key.equals("")) {
            mKeyBigInteger = new BigInteger(key, 32);
        }
    }

    public SecretKey(BigInteger key) {
        mKeyBigInteger = key;
        mKey = key.toString(32);
    }
    public String getKey() {
        return mKey;
    }
    public BigInteger getKeyBigInteger() {
        return mKeyBigInteger;
    }
}
