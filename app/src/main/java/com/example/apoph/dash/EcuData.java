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
    public String mMessage;
    public int mTps;

    private int mNeutral = 0;
    private int mRpmBin = 0;
    private int mSpeedBin = 0;
    private String msgBuf = "";

    public EcuData() {
        mRpm = "0";
        mSpeed = "0";
        mAirTemp = "-";
        mCoolantTemp = "-";
        mBatteryVoltage = "-";
        mGear = "N";
        mEngine = "0";
        mTps = 0;
    }

    private int getShortValue(int at) {
        //Log.i(TAG, "parse at: " + at + " data: " + msgBuf.substring(at*2, at*2+4));
        return Integer.parseInt(msgBuf.substring(at*2, at*2+4), 16);
    }

    private int getByteValue(int at) {
        //Log.i(TAG, "parse at: " + at + " data: " + msgBuf.substring(at*2, at*2+2));
        return Integer.parseInt(msgBuf.substring(at*2, at*2+2), 16);
    }

    private void calculateGear() {
        int v = mNeutral & 0x0f;

        if (v == 0x03) {
            mGear = "P";    // Kickstand (like Park)
        }
        else if (v == 0x01) {
            mGear = "N";    // Neutral or clutch
        }
        else {
            // Calculate gear from speed / rpm ratio
            if (mRpmBin > 0) {
                float ratio = (float) mSpeedBin / (float) mRpmBin;
                // TODO: find out the ratios...
                mGear = String.format("%.3f", ratio);
            }
        }
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

        if (table == TABLE_11) {
            mRpmBin = getShortValue(4+0);
            mRpm = "" + mRpmBin;
            mCoolantTemp = "" + (getByteValue(4+5) - 40);
            mAirTemp = "" + (getByteValue(4+7) - 40);
            mBatteryVoltage = "" + ((float) getByteValue(4+12) / 10.f);
            mSpeedBin = getByteValue(4+13);
            mSpeed = "" + mSpeedBin;
            mTps = getByteValue(4+3);
        }

        if (table == TABLE_D1) {
            mNeutral = getByteValue(4+0);
            mEngine = "" + getByteValue(4+4);
            calculateGear();
        }

        return true;
    }

    public boolean feed(byte[] data) {
        if (data == null || data.length < MIN_DATA_LENGTH)
            return false;

        boolean res = false;
        byte t = data[0];

        // Start of new message
        if (t == ':') {
            res = parseMessage();
            msgBuf = new String(data, 1, data.length-1);
        }

        // More data to message
        if ((t >= '0' && t <= '9') || (t >= 'A' && t <= 'F')) {
            msgBuf += new String(data);
        }

        // Lower level message that user should see
        if (t == '#') {
            mMessage = "ECU: " + new String(data, 1, data.length-1);
            res = true;
        }

        return res;
    }
}
