package vs.pharmacist;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;

import vs.pharmacist.objects.Place;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import vs.pharmacist.objects.PlacesList;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Location mLocation;
    private PlacesList mPlaceList;

    private AlertDialog.Builder mAlertDialogBuilder;
    private AlertDialog mAlertDialog;

    private ProgressDialog mProgressDialog;

    private int mChoosedView = 2;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Переключить вид");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getTitle().toString()) {
            case "Переключить вид":
                mAlertDialogBuilder = new AlertDialog.Builder(this);
                mAlertDialogBuilder.setTitle("Выберите вид");
                mAlertDialogBuilder.setSingleChoiceItems(new CharSequence[]{"Стандартный", "Вид со спутника", "Гибридный"},
                        mChoosedView, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d("DEBUG", "onClick: " + which);
                                mChoosedView = which;
                            }
                        });
                mAlertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d("DEBUG", "onClick: " + which);
                        switch (mChoosedView) {
                            case 0:
                                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                                break;
                            case 1:
                                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                                break;
                            case 2:
                                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                        }
                    }
                });
                mAlertDialog = mAlertDialogBuilder.show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        getSupportActionBar().setTitle("Ближайшие аптеки");

        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mAlertDialogBuilder = new AlertDialog.Builder(this);
            mAlertDialogBuilder.setTitle("Упс!");
            mAlertDialogBuilder.setMessage("Похоже, что у вас выключен GPS. Не хотите ли вы его включить? В противнном случае, поиск аптек невозможен.");
            mAlertDialogBuilder.setPositiveButton("ДА", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(i);
                    finish();
                }
            });
            mAlertDialogBuilder.setNegativeButton("НЕТ", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            mAlertDialogBuilder.setCancelable(false);
            mAlertDialogBuilder.show();
        }
        else {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setTitle("Подождите, пожалуйста...");
            mProgressDialog.setCancelable(false);

            mProgressDialog.setMessage("Дождитесь точного определения вашего местоположения...");
            mProgressDialog.show();

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                return;

            lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, new MyLocationListener(), Looper.myLooper());
        }
    }

    class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            mLocation = location;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    LatLng gpsLoc = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(gpsLoc).title("Ваше местоположение"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(gpsLoc, 15));
                    mProgressDialog.setMessage("Ищем ближайшие к вам аптеки...");
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                String mURLString = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=";
                                mURLString += Double.toString(mLocation.getLatitude());
                                mURLString += ",";
                                mURLString += Double.toString(mLocation.getLongitude());
                                mURLString += "&radius=10000&types=pharmacy&language=ru&key=";
                                mURLString += getString(R.string.google_maps_key);
                                URL mURL = new URL(mURLString);
                                HttpURLConnection mConnection = (HttpURLConnection) mURL.openConnection();
                                mConnection.setRequestMethod("GET");
                                mConnection.connect();
                                String mAnswer = "";
                                Scanner sc = new Scanner(mConnection.getInputStream());
                                while (sc.hasNext()) mAnswer += sc.next();
                                sc.close();
                                mConnection.disconnect();
                                Gson mGson = new Gson();
                                mPlaceList = mGson.fromJson(mAnswer, PlacesList.class);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mProgressDialog.hide();
                                        for (Place p : mPlaceList.results) {
                                            LatLng pharLoc = new LatLng(p.geometry.location.lat, p.geometry.location.lng);
                                            mMap.addMarker(new MarkerOptions().position(pharLoc).title(p.name));
                                        }
                                    }

                                });

                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    }.start();
                }
            });
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }
    }
}
