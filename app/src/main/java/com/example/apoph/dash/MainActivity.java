package com.example.apoph.dash;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.Menu;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
//import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
//import com.google.android.gms.maps.model.MarkerOptions;

import android.support.v4.app.ActivityCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.Locale;

interface OnDashDataCallback {
    void onDashData(String type, byte[] data);
}

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, OnDashDataCallback {
    final String ECU_ADDR = "C5:DF:7C:68:8A:D7";
    final String TAG = "DASH";

    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private boolean mAutoCenter = true;
    private boolean mShowGpsSpeed = false;
    private String mGpsSpeed = "0";
    private BtUart mBtUart;
    private Menu mOptionsMenu;
    private EcuData mEcuData;
    private ConnectDlg mConnectDlg;
    private GearingDlg mGearingDlg;
    private SharedPreferences mPrefs;
    private MotoLogger mEcuLogger;
    private MotoLogger mGpsLogger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPrefs = getPreferences(Context.MODE_PRIVATE);

        mEcuData = new EcuData(mPrefs);
        updateDash();

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        MapFragment mMapFragment;
        mMapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                1);

        mBtUart = new BtUart();
        if (!mBtUart.init(getApplicationContext(), this)) {
            Toast.makeText(this, "Bluetooth fail", Toast.LENGTH_SHORT).show();
            mBtUart = null;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.activity_main);
        }
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_main);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        CameraPosition cp = mMap.getCameraPosition();
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putFloat("zoom", cp.zoom);
        editor.commit();

        if (mBtUart != null) {
            mBtUart.close();
        }

        if (mEcuLogger != null) {
            mEcuLogger.stop();
        }

        if (mGpsLogger != null) {
            mGpsLogger.stop();
        }

        Log.i(TAG, "onDestroy");
    }

    protected void initLocation() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || !mAutoCenter) {
                    return;
                }

                Location lastl = locationResult.getLastLocation();

                if (mMap != null && lastl != null) {
                    LatLng nl = new LatLng(lastl.getLatitude(), lastl.getLongitude());
                    //mMap.moveCamera(CameraUpdateFactory.newLatLng(nl));
                    CameraPosition.Builder cb = new CameraPosition.Builder(mMap.getCameraPosition());
                    cb.target(nl);
                    cb.tilt(0);
                    cb.bearing(lastl.getBearing());
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cb.build()));

                    // Convert to kmh
                    mGpsSpeed = "" + (int) (3.6f * lastl.getSpeed());

                    if (mGpsLogger != null) {
                        mGpsLogger.log(String.format(Locale.ROOT, "%.6f,%.6f,%.0f,%s,%.0f\n",
                                lastl.getLatitude(),
                                lastl.getLongitude(),
                                lastl.getAltitude(),
                                mGpsSpeed,
                                lastl.getBearing()));
                    }
                }
            }
        };

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(2000);
        mLocationRequest.setFastestInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                null /* Looper */);
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        boolean grantLoc = false;
        boolean grantFile = false;

        if (grantResults.length >= 2) {
            grantLoc = (grantResults[0] == PackageManager.PERMISSION_GRANTED);
            grantFile = (grantResults[1] == PackageManager.PERMISSION_GRANTED);
        }

        if (grantLoc) {
            initLocation();
        }

        if (grantFile) {
            mEcuLogger = new MotoLogger("ECU");
            mGpsLogger = new MotoLogger("GPS");
        }
    }

    public void onDashData(String type, byte[] data) {
        switch (type) {
            case "CONNECTED":
                onConnect(true);
                break;
            case "DISCONNECTED":
                onConnect(false);
                break;
            case "SCANSTART":
                onScan(true);
                break;
            case "SCANSTOP":
                onScan(false);
                break;
            case "INCOMPATIBLE": {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBtUart.resetConnected();
                        Toast.makeText(getApplicationContext(), "Incompatible", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            }
            case "DATA":
                if (mEcuData.feed(data)) {
                    updateDash();
                }
                break;
        }
    }

    public void onScan(final boolean state) {
        runOnUiThread(new Runnable(){
            @Override
            public void run() {
                if (state) {
                    Toast.makeText(getApplicationContext(), "Scanning", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(getApplicationContext(), "Scan ended", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void onConnect(final boolean state)
    {
        runOnUiThread(new Runnable(){
            @Override
            public void run() {
                MenuItem m;

                if (state) {
                    m = mOptionsMenu.findItem(R.id.action_connect);
                    m.setEnabled(false);
                    m = mOptionsMenu.findItem(R.id.action_disconnect);
                    m.setEnabled(true);
                    Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                else {
                    m = mOptionsMenu.findItem(R.id.action_connect);
                    m.setEnabled(true);
                    m = mOptionsMenu.findItem(R.id.action_disconnect);
                    m.setEnabled(false);
                    Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
                    //getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
        });
    }

    protected void updateDash() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mEcuData.mMessage != null) {
                    Toast.makeText(getApplicationContext(), mEcuData.mMessage, Toast.LENGTH_SHORT).show();
                    mEcuData.mMessage = null;
                }

                TextView tv;
                tv = findViewById(R.id.rpm);
                tv.setText(mEcuData.mRpm);
                tv = findViewById(R.id.speed);
                if (mShowGpsSpeed) {
                    tv.setText(mGpsSpeed);
                    tv.setBackgroundColor(0xffdddddd);
                }
                else {
                    tv.setText(mEcuData.mSpeed);
                    tv.setBackgroundColor(0x00000000);
                }
                tv = findViewById(R.id.gear);
                tv.setText(mEcuData.mGear);
                if (mEcuData.mGear == "N" || mEcuData.mGear == "P") {
                    tv.setBackgroundColor(0xff00cc00);
                }
                else {
                    tv.setBackgroundColor(0x0000cc00);
                }
                tv = findViewById(R.id.gear_big);
                if (mEcuData.mSpeedBin > 4 && "123456-".contains(mEcuData.mGear)) {
                    tv.setText(mEcuData.mGear);
                    tv.setVisibility(View.VISIBLE);
                }
                else {
                    tv.setVisibility(View.INVISIBLE);
                }
                tv = findViewById(R.id.air_t);
                tv.setText(mEcuData.mAirTemp);
                tv = findViewById(R.id.coolant_t);
                tv.setText(mEcuData.mCoolantTemp);
                tv = findViewById(R.id.battery_v);
                tv.setText(mEcuData.mBatteryVoltage);

                ProgressBar pb = findViewById(R.id.tps);
                pb.setProgress(mEcuData.mTps);

                if (mGearingDlg != null) {
                    mGearingDlg.updateRatio(mEcuData.mRatio);
                }
            }
        });
    }

    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng kni = new LatLng(64.232, 27.782);
        //mMap.addMarker(new MarkerOptions().position(kni).title("Koti"));
        mMap.setMyLocationEnabled(true);
        float zoom = mPrefs.getFloat("zoom", 13.0f);
        mMap.moveCamera(CameraUpdateFactory.zoomTo(zoom));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(kni));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        mOptionsMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_connect:
                if (mBtUart != null) {
                    DialogInterface.OnClickListener cb = new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mBtUart.connect(mConnectDlg.getDeviceAddr(which));
                        }
                    };

                    if (!mBtUart.connect()) {
                        mConnectDlg = new ConnectDlg(this, mBtUart.getBleDevices(), cb);
                        mConnectDlg.show();
                    }
                }
                return true;
            case R.id.action_disconnect:
                if (mBtUart != null) {
                    mBtUart.disconnect();
                }
                return true;
            case R.id.action_center:
                item.setChecked(!item.isChecked());
                mAutoCenter = item.isChecked();
                return true;
            case R.id.action_gps_speed:
                item.setChecked(!item.isChecked());
                mShowGpsSpeed = item.isChecked();
                return true;
            case R.id.action_gearing:
                mGearingDlg = new GearingDlg(this, mPrefs);
                mGearingDlg.show();
                return true;
            case R.id.action_big_gear:
                item.setChecked(!item.isChecked());
                //if (item.isChecked()) {
                //    setContentView(R.layout.activity_main_gear);
                //}
                //else {
                //    setContentView(R.layout.activity_main);
                //}
                return true;
            case R.id.action_log:
                item.setChecked(!item.isChecked());
                if (mEcuLogger != null && mGpsLogger != null) {
                    if (item.isChecked()) {
                        mEcuLogger.start();
                        mGpsLogger.start();
                        mEcuData.setLogger(mEcuLogger);
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    } else {
                        mEcuLogger.stop();
                        mGpsLogger.stop();
                        mEcuData.setLogger(null);
                    }
                }
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

}

