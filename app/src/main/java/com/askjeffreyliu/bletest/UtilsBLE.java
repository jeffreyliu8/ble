package com.askjeffreyliu.bletest;


import java.math.BigInteger;

/**
 * Created by Jeffrey Liu on 6/21/15.
 */
public class UtilsBLE {

    private UtilsBLE() {
    }

    public static String bytesToHex(byte[] bytes) {
        if (bytes == null)
            return null;
        return String.format("%0" + (bytes.length * 2) + "X", new BigInteger(1, bytes)).toLowerCase();
    }
}