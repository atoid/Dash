package com.example.apoph.dash;

import android.util.Log;

/**
 * Created by apoph on 25.3.2018.
 */

public class EcuData {
    private static final String TAG = "ECUDATA";
    private static final int MIN_DATA_LENGTH = 1;
    private static final int TABLE_11 = 0x11;
    private static final int TABLE_D1 = 0xd1;

    public String mRpm;
    public String mSpeed;
    public String mAirTemp;
    public String mCoolantTemp;
    public String mBatteryVoltage;
    public String mGear;
    public String mEngine;

    private String msgBuf = "";

    public EcuData() {
        mRpm = "0";
        mSpeed = "0";
        mAirTemp = "-";
        mCoolantTemp = "-";
        mBatteryVoltage = "-";
        mGear = "N";
        mEngine = "0";
    }

    private int getShortValue(int at) {
        //Log.i(TAG, "parse at: " + at + " data: " + msgBuf.substring(at*2, at*2+4));
        return Integer.parseInt(msgBuf.substring(at*2, at*2+4), 16);
    }

    private int getByteValue(int at) {
        //Log.i(TAG, "parse at: " + at + " data: " + msgBuf.substring(at*2, at*2+2));
        return Integer.parseInt(msgBuf.substring(at*2, at*2+2), 16);
    }

    private boolean parseMessage() {
        if (msgBuf.isEmpty())
            return false;

        // Verify message integrity after BT transfer
        int len = getByteValue(1);
        if (len > (msgBuf.length() / 2)) {
            Log.w(TAG,"length mismatch");
            return false;
        }

        int table = getByteValue(3);

        if (table == TABLE_11)
        {
            mRpm = "" + getShortValue(4+0);
            mCoolantTemp = "" + (getByteValue(4+5) - 40);
            mAirTemp = "" + (getByteValue(4+7) - 40);
            mBatteryVoltage = "" + ((float) getByteValue(4+12) / 10.f);
            mSpeed = "" + getByteValue(4+13);
        }

        if (table == TABLE_D1)
        {
            int v = getByteValue(4+0) & 0x3;
            mGear = "" + v;
            mEngine = "" + getByteValue(4+4);
        }

        return true;
    }

    public boolean feed(byte[] data) {
        if (data == null || data.length < MIN_DATA_LENGTH)
            return false;

        boolean res = false;
        byte t = data[0];

        // Start of new message
        if (t == ':')
        {
            res = parseMessage();
            msgBuf = new String(data, 1, data.length-1);
        }

        if ((t >= '0' && t <= '9') || (t >= 'A' && t <= 'F'))
        {
            msgBuf += new String(data);
        }

        return res;
    }
}
