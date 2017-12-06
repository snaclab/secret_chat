package org.servalproject.group.secret;

import android.content.Intent;

import java.math.BigInteger;

/**
 * Created by sychen on 2017/3/30.
 */

public class SubKey extends SecretKey {
    Integer index;

    public SubKey(String key, Integer index){
        super(key);
        this.index = index;
    }

    public SubKey(BigInteger key, Integer index){
        super(key);
        this.index = index;
    }
    public Integer getIndex() {
        return index;
    }
     public void setIndex(Integer index) {
         this.index = index;
     }
}
