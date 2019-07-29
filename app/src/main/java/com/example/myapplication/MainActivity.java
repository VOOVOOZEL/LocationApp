package com.example.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

import com.example.myapplication.handlers.DialogFragment;
import com.example.myapplication.handlers.ServerManager;
import com.example.myapplication.handlers.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    SessionManager session;
    private LocationManager locationManager;
    private final String[] PERMISSIONS={
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,};

    private Handler mHandler = new Handler();
    private  HashMap<String, String> user;

    @BindView(R.id.lbl_Name) TextView username;
    @BindView(R.id.tvEnabledGPS) TextView tvEnabledGPS;
    @BindView(R.id.tvStatusGPS) TextView tvStatusGPS;
    @BindView(R.id.tvLocationGPS) TextView tvLocationGPS;

    @BindView(R.id.tvEnabledNet) TextView tvEnabledNet;
    @BindView(R.id.tvStatusNet) TextView tvStatusNet;
    @BindView(R.id.tvLocationNet) TextView tvLocationNet;

    @BindView(R.id.tvRX) TextView RX;
    @BindView(R.id.tvTX) TextView TX;

    @BindView(R.id.tvTime) TextView tvTime;
    @BindView(R.id.btn_Logout) Button btnLogout;
    @BindView(R.id.btn_permissions) Button btnPerm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        ButterKnife.bind(this);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                long mStartRX = TrafficStats.getTotalRxBytes();
                long mStartTX = TrafficStats.getTotalTxBytes();

                long rxBytes = TrafficStats.getTotalRxBytes()- mStartRX;
                RX.setText(Long.toString(rxBytes));
                long txBytes = TrafficStats.getTotalTxBytes()- mStartTX;
                TX.setText(Long.toString(txBytes));

                mHandler.postDelayed(this,1000);
            }
        });
        thread.start();

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        session = new SessionManager(getApplicationContext());
        user = session.getUserDetails();
        session.checkLogin();

        String name = user.get(SessionManager.KEY_NAME);
        username.setText("Name: " + name);

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                session.logoutUser();
            }
        });

        btnPerm.setVisibility(View.GONE);
        if (!hasPermissions(getApplication(),PERMISSIONS)){
            btnPerm.setVisibility(View.VISIBLE);
            btnPerm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showAlertDialog();
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    1000 * 10, 10, locationListener);
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 1000 * 10, 10,
                    locationListener);
            checkEnabled();
        } catch (SecurityException e) {
            return;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    1000 * 10, 200, locationListener);
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 1000 * 10, 200,
                    locationListener);
            checkEnabled();
        } catch (SecurityException e) {
            return;
        }
    }

    private void showAlertDialog() {
        FragmentManager fm = getSupportFragmentManager();
        DialogFragment alertDialog = DialogFragment.newInstance("Ask permissions");
        alertDialog.show(fm, "fragment_alert");
    }

    private boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            showLocation(location);
            sendLocation(location);
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
            if (s.equals(LocationManager.GPS_PROVIDER)) {
                tvStatusGPS.setText("Status: " + String.valueOf(i));
            } else if (s.equals(LocationManager.NETWORK_PROVIDER)) {
                tvStatusNet.setText("Status: " + String.valueOf(i));
            }
        }

        @Override
        public void onProviderEnabled(String s) {
            checkEnabled();
            try {
                showLocation(locationManager.getLastKnownLocation(s));
            } catch (SecurityException e) {
                return;
            }
        }

        @Override
        public void onProviderDisabled(String s) {
            checkEnabled();
        }
    };

    private void showLocation(Location location) {
        if (location == null)
            return;
        if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            tvLocationGPS.setText(formatLocation(location));
        } else if (location.getProvider().equals(
                LocationManager.NETWORK_PROVIDER)) {
            tvLocationNet.setText(formatLocation(location));
        }
        tvTime.setText(
                String.format("%1$tF %1$tT", new Date(
                location.getTime())));
    }

    private String formatLocation(Location location) {
        if (location == null)
            return "";
        return String.format(
                "Coordinates: lat = %1$.4f, lon = %2$.4f",
                location.getLatitude(), location.getLongitude());
    }

    private void checkEnabled() {
        tvEnabledGPS.setText("Enabled: "
                + locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER));
        tvEnabledNet.setText("Enabled: "
                + locationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    private void sendLocation(Location location) {
        final String login = user.get(SessionManager.KEY_NAME);

        final JSONObject person = new JSONObject();
        try {
            person.accumulate("login",login);
        } catch (JSONException e){return;}
        try {
            person.accumulate("latitude", location.getLatitude());
        } catch (JSONException e){return;}
        try {
            person.accumulate("longitude", location.getLongitude());
        } catch (JSONException e){return;}
        try {
            person.accumulate("datetime", String.format("%1$tF %1$tT", new Date(
                    location.getTime())));
        } catch (JSONException e){return;}

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String message = person.toString();
                ServerManager.SendJsonToServer(
                        message,"POST","postCoords");
                final String msg;
                String err = ServerManager.GetStringErr();
                String resp = ServerManager.GetStringResponse();
                if (err.equals("")) {
                    msg = resp;
                } else {
                    msg = err;
                }
                Log.d("COORD_REQ", msg);
            }
        });
        thread.start();
    }
}
