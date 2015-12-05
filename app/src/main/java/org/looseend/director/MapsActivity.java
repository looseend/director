package org.looseend.director;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import butterknife.Bind;
import butterknife.ButterKnife;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MapsActivity.class.getSimpleName();
    private GoogleMap mMap;
    /**
     * Request code for location permission request.
     *
     * @see #onRequestPermissionsResult(int, String[], int[])
     */
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GoogleApiClient mGoogleApiClient;
    private boolean connected;


    @Bind(R.id.nav_view)
    NavigationView navigationView;
    @Bind(R.id.drawer_layout)
    DrawerLayout drawerLayout;
    @Bind(R.id.compass)
    ImageView compass;
    @Bind(R.id.distance)
    TextView distance;
    @Bind(R.id.navigator)
    RelativeLayout navigator;
    private ActionBarDrawerToggle drawerToggle;
    private Marker destination;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        ButterKnife.bind(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
        navigationView.setNavigationItemSelectedListener(this);

        setupActionBar();

    }

    private void setupActionBar() {
        drawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                drawerLayout,         /* DrawerLayout object */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
        ) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };

        // Set the drawer toggle as the DrawerListener
        drawerLayout.setDrawerListener(drawerToggle);

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        enableMyLocation();

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                if (destination != null) {
                    destination.remove();
                }
                destination = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dest)));
                Log.d(TAG, "Long click, at" + latLng.toString());
            }
        });
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }


    @Override
    public void onConnected(Bundle bundle) {
        connected = true;
        Log.d(TAG, "Connected");
        if (mMap != null) {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            if (lastLocation != null) {
                Log.d(TAG, "Moovin and Zoomin");
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), 12.0f));
            } else {
                Log.d(TAG, "No Location");
            }
        } else {
            Log.d(TAG, "Map not created yet");
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Suspended");
        connected = false;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Failed");
        connected = false;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_nav) {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            Location l = new Location("");
            l.setLatitude(destination.getPosition().latitude);
            l.setLongitude(destination.getPosition().longitude);
            float bearingTo = lastLocation.bearingTo(l);

            float ourBearing = lastLocation.getBearing();

            float requiredBearing;
            if(ourBearing > bearingTo) {
                requiredBearing = 360.0f - (ourBearing - bearingTo);
            } else {
                requiredBearing = bearingTo - ourBearing;
            }

            float distanceTo = l.distanceTo(lastLocation);

            Log.d(TAG, "BearingTo " + bearingTo + " My bearing " + ourBearing + " Required " + requiredBearing + " Distance " + distanceTo + "m");

            if (requiredBearing > 0.0f && requiredBearing <= 45.0f ) {
                compass.setImageResource(R.drawable.nne);
            } else if (requiredBearing > 45.0f && requiredBearing <= 90.0f ) {
                compass.setImageResource(R.drawable.ene);
            } else if (requiredBearing > 90.0f && requiredBearing <= 135.0f ) {
                compass.setImageResource(R.drawable.ese);
            } else if (requiredBearing > 135.0f && requiredBearing <= 180.0f ) {
                compass.setImageResource(R.drawable.sse);
            } else if (requiredBearing > 180.0f && requiredBearing <= 225.0f ) {
                compass.setImageResource(R.drawable.ssw);
            } else if (requiredBearing > 225.0f && requiredBearing <= 270.0f ) {
                compass.setImageResource(R.drawable.wsw);
            } else if (requiredBearing > 270.0f && requiredBearing <= 315.0f ) {
                compass.setImageResource(R.drawable.wnw);
            } else if (requiredBearing > 315.0f && requiredBearing <= 360.0f ) {
                compass.setImageResource(R.drawable.nnw);
            }

            Resources res = getResources();
            if (distanceTo >= 1000) {
                distance.setText(String.format(res.getString(R.string.distancekm), (distanceTo / 1000.0f)));
            } else {
                distance.setText(String.format(res.getString(R.string.distancm), distanceTo));
            }

            compass.setImageAlpha(127);
            navigator.setVisibility(View.VISIBLE);

            CameraPosition pos = new CameraPosition.Builder(mMap.getCameraPosition())
                .bearing(ourBearing)
                .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos));

        } else {
            Log.d(TAG, "Selected " + item.getTitle());
        }
        drawerLayout.closeDrawers();
        return false;
    }
}
