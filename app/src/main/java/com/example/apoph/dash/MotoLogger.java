package com.example.apoph.dash;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by apoph on 16.4.2018.
 */

public class MotoLogger {
    private String TAG = "LOGGER";
    private File mDir;
    private File mFile;
    private FileWriter mOut;
    private String mFileTag;

    public MotoLogger(String tag) {
        Log.i(TAG, "Create log for: " + tag);

        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            Log.e(TAG, "Storage not available");
            return;
        }

        mDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), "MotoDash");

        if (!mDir.mkdirs()) {
            Log.i(TAG, "Directory not created");
        }

        mFileTag = tag;
    }

    private String getTimestamp(boolean timeOnly) {
        Calendar c = Calendar.getInstance();

        int y = c.get(Calendar.YEAR);
        int k = c.get(Calendar.MONTH);
        int d = c.get(Calendar.DAY_OF_MONTH);
        int h = c.get(Calendar.HOUR);
        int m = c.get(Calendar.MINUTE);
        int s = c.get(Calendar.SECOND);
        int ms = c.get(Calendar.MILLISECOND);

        if (timeOnly) {
            return String.format(Locale.ROOT, "%02d:%02d:%02d.%04d", h, m, s, ms);
        }
        else {
            return String.format(Locale.ROOT, "%d-%02d-%02dT%02d-%02d-%02d", y, k+1, d, h, m, s);
        }
    }

    public void start() {
        if (mDir != null) {
            String ts = getTimestamp(false);
            String name = mFileTag + "-" + ts + ".txt";
            mFile = new File(mDir, name);

            try {
                mOut = new FileWriter(mFile);
                Log.i(TAG, "Start log to: " + name);
            } catch (Exception e) {
                mOut = null;
            }
        }
    }

    public void stop() {
        if (mOut != null) {
            try {
                Log.i(TAG, "Stop log");
                mOut.close();
            } catch (Exception e) {}
            mOut = null;
        }
    }

    public void log(String txt) {
        if (mOut != null) {
            try {
                String ts = getTimestamp(true);
                mOut.append(ts + " " + txt);
            } catch (Exception e) {}
        }
    }
}

