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

import java.util.Locale;

/**
 * Created by apoph on 26.3.2018.
 */

public class GearingDlg {
    private final String TAG = "DLGGEAR";
    private AlertDialog mDlg;
    private final int[] mGearIds = {R.id.gear_1, R.id.gear_2, R.id.gear_3, R.id.gear_4, R.id.gear_5, R.id.gear_6};
    private final int[] mRatioIds = {R.id.ratio_1, R.id.ratio_2, R.id.ratio_3, R.id.ratio_4, R.id.ratio_5, R.id.ratio_6};
    private SharedPreferences mPrefs;
    private float[] mRatios = {0.f, 0.f, 0.f, 0.f, 0.f, 0.f};
    private float mCurrentRatio = 0.f;

    private View.OnClickListener mOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            setHighlight(id);

            for (int i = 0; i < mGearIds.length; i++) {
                if (id == mGearIds[i]) {
                    mRatios[i] = mCurrentRatio;
                    TextView tmp = mDlg.findViewById(mRatioIds[i]);
                    tmp.setText(String.format(Locale.ROOT, "%.3f", mCurrentRatio));
                    break;
                }
            }
        }
    };

    public GearingDlg(Context ctx, SharedPreferences prefs) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);

        LayoutInflater inflater = ((AppCompatActivity)ctx).getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dlg_gearing, null));

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
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
            tmp = mDlg.findViewById(mGearIds[i]);
            tmp.setOnClickListener(mOnClick);

            mRatios[i] = mPrefs.getFloat(String.format("gear%d", i), 0.f);
            tmp = mDlg.findViewById(mRatioIds[i]);
            tmp.setText(String.format(Locale.ROOT,"%.3f", mRatios[i]));
        }
    }

    public void updateRatio(float r) {
        TextView tmp = mDlg.findViewById(R.id.gear_ratio);
        if (tmp != null) {
            tmp.setText(String.format(Locale.ROOT,"%.3f", r));
        }
        mCurrentRatio = r;
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
        for (int i = 0; i < mRatios.length; i++) {
            editor.putFloat(String.format("gear%d", i), mRatios[i]);
        }
        editor.commit();
    }
}

