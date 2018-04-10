package com.example.apoph.dash;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 * Created by apoph on 26.3.2018.
 */

public class GearingDlg {
    private final String TAG = "DLGGEAR";
    private AlertDialog mDlg;
    private final int[] mGearIds = {R.id.gear_1, R.id.gear_2, R.id.gear_3, R.id.gear_4, R.id.gear_5, R.id.gear_6};
    private final int[] mRatioIds = {R.id.ratio_1, R.id.ratio_2, R.id.ratio_3, R.id.ratio_4, R.id.ratio_5, R.id.ratio_6};
    private SharedPreferences mPrefs;
    private int mCurrentGear = -1;
    private float[] mRatios = {0.f, 0.f, 0.f, 0.f, 0.f, 0.f};
    private float mCurrentRatio = 0.f;

    private View.OnClickListener mOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            setHighlight(id);

            int i;
            for (i = 0; i < mGearIds.length; i++) {
                if (id == mGearIds[i]) {
                    break;
                }
            }

            if (mCurrentGear != -1 && mCurrentGear != i) {
                Log.i(TAG, "Set gear ratio for " + mCurrentGear+1);
                mRatios[mCurrentGear] = mCurrentRatio;
            }

            mCurrentGear = i;
        }
    };

    public GearingDlg(Context ctx, SharedPreferences prefs) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);

        LayoutInflater inflater = ((AppCompatActivity)ctx).getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dlg_gearing, null));

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            // User clicked Save button
            if (mCurrentGear != -1) {
                mRatios[mCurrentGear] = mCurrentRatio;
            }
            saveRatios();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            // User clicked Cancel button
            }
        });

        mDlg = builder.create();
        mPrefs = prefs;
    }

    public void show() {
        mDlg.show();
        TextView tmp;

        for (int i = 0; i < mGearIds.length; i++) {
            int gid = mGearIds[i];
            tmp = mDlg.findViewById(gid);
            tmp.setOnClickListener(mOnClick);
        }
    }

    public void updateRatio(float r) {
        if (mCurrentGear != -1)
        {
            mCurrentRatio = r;
            TextView tmp;
            tmp = mDlg.findViewById(mRatioIds[mCurrentGear]);
            tmp.setText(String.format("%.3f", r));
        }
    }

    private void setHighlight(int id) {
        TextView tmp;

        for (int i = 0; i < mGearIds.length; i++) {
            int gid = mGearIds[i];
            tmp = mDlg.findViewById(gid);
            if (gid != id) {
                tmp.setBackgroundColor(0x00000000);
            }
            else {
                tmp.setBackgroundColor(0xffdddddd);
            }
        }
    }

    private void saveRatios() {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putFloat("gear1", mRatios[0]);
        editor.putFloat("gear2", mRatios[1]);
        editor.putFloat("gear3", mRatios[2]);
        editor.putFloat("gear4", mRatios[3]);
        editor.putFloat("gear5", mRatios[4]);
        editor.putFloat("gear6", mRatios[5]);
        editor.commit();
    }
}

