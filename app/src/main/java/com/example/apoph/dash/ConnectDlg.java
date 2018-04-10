package com.example.apoph.dash;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import java.util.Map;

/**
 * Created by apoph on 26.3.2018.
 */

public class ConnectDlg {
    private final String TAG = "DLGCONN";
    private final int BD_ADDR_LENGTH = 17;
    private AlertDialog mDlg;
    String[] mDevList;

    public ConnectDlg(Context ctx, Map devices, DialogInterface.OnClickListener onItemClick) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle("Select device");
        builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked Close button
            }
        });

        if (devices != null && devices.size() > 0) {
            mDevList = new String[devices.size()];

            int i = 0;
            for (Object key : devices.keySet()) {
                mDevList[i] = (String) key;

                if (devices.get(key) != null) {
                    mDevList[i] += " (" + (String) devices.get(key) + ")";
                }
                ++i;
            }

            builder.setItems(mDevList, onItemClick);
        }
        else
        {
            builder.setMessage("No devices found");
        }

        mDlg = builder.create();
    }

    public void show() {
        mDlg.show();
    }

    public String getDeviceAddr(int which) {
        return mDevList[which].substring(0, BD_ADDR_LENGTH);
    }
}

