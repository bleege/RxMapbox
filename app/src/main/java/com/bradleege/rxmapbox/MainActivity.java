package com.bradleege.rxmapbox;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.commons.ServicesException;
import com.mapbox.services.commons.models.Position;
import com.mapbox.services.geocoding.v5.GeocodingCriteria;
import com.mapbox.services.geocoding.v5.MapboxGeocoding;
import com.mapbox.services.geocoding.v5.models.GeocodingResponse;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private MapView mapView = null;
    private MapboxMap mapboxMap = null;

    private FloatingActionButton fab = null;

    // PERMISSION
    public static final int PERMISSIONS_LOCATION = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mapView = (MapView)findViewById(R.id.mainMapView);
        mapView.setAccessToken(getString(R.string.access_token));
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap map) {
                mapboxMap = map;

                mapboxMap.setOnMapLongClickListener(new MapboxMap.OnMapLongClickListener() {
                    @Override
                    public void onMapLongClick(@NonNull LatLng point) {
                        geocode(point);
                    }
                });
            }
        });

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleGps(!mapboxMap.isMyLocationEnabled());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    public void toggleGps(boolean enableGps) {
        if (enableGps) {
            if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                    (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_LOCATION);
            } else {
                toggleLocationEnabled(true);
            }
        } else {
            toggleLocationEnabled(false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    toggleLocationEnabled(true);
                }
            }
        }
    }

    private void toggleLocationEnabled(boolean enabled) {
        if (enabled) {
            mapboxMap.setOnMyLocationChangeListener(new MapboxMap.OnMyLocationChangeListener() {
                @Override
                public void onMyLocationChange(@Nullable Location location) {
                    if (location != null) {
                        mapboxMap.setCameraPosition(new CameraPosition.Builder()
                                .target(new LatLng(location))
                                .zoom(16)
                                .bearing(0)
                                .tilt(0)
                                .build());
                        mapboxMap.setOnMyLocationChangeListener(null);
                    }
                }
            });
            fab.setImageResource(R.drawable.ic_gps_off_24dp);
        } else {
            fab.setImageResource(R.drawable.ic_gps_fixed_24dp);
        }
        mapboxMap.setMyLocationEnabled(enabled);
    }

    private void geocode(LatLng point) {

        try {
            Position position = Position.fromCoordinates(point.getLongitude(), point.getLatitude(), point.getAltitude());

            Observable<GeocodingResponse> geoObservable = new MapboxGeocoding.Builder()
                                                        .setAccessToken(mapboxMap.getAccessToken())
                                                        .setType(GeocodingCriteria.TYPE_ADDRESS)
                                                        .setCoordinates(position)
                                                        .build()
                                                        .getObservable();

            geoObservable.observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<GeocodingResponse>() {
                @Override
                public void call(GeocodingResponse geocodingResponse) {
                    Log.i(TAG, "Geocoding Response = " + geocodingResponse);
                }
            });

        } catch (ServicesException e) {
            Log.e(TAG, "Error During Geocode: " + e);
        }
    }
}
