package org.looseend.director;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.StringRes;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.PlaceTypes;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.quinny898.library.persistentsearch.SearchBox;
import com.quinny898.library.persistentsearch.SearchResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import timber.log.Timber;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MapsActivity.class.getSimpleName();
    private GoogleMap mMap;
    /**
     * Request code for location permission request.
     *
     * @see #onRequestPermissionsResult(int, String[], int[])
     */
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GoogleApiClient googleApiClient;
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

    @Bind(R.id.searchbox)
    SearchBox searchBox;

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
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
        navigationView.setNavigationItemSelectedListener(this);

        setupActionBar();

        searchBox.setMenuListener(new SearchBox.MenuListener() {
            @Override
            public void onMenuClick() {
                Timber.d("MENU");
                if (drawerLayout.isDrawerOpen(navigationView)) {
                    drawerLayout.closeDrawer(navigationView);
                } else {
                    drawerLayout.openDrawer(navigationView);
                }
            }
        });

        searchBox.setSearchListener(new SearchBox.SearchListener() {
            @Override
            public void onSearchOpened() {
            }

            @Override
            public void onSearchCleared() {

            }

            @Override
            public void onSearchClosed() {

            }

            @Override
            public void onSearchTermChanged(String s) {
                searchBox.showLoading(true);
                Timber.d("Search term changed " + s);
                doGeoSearch(s);
            }

            @Override
            public void onSearch(String s) {
                Timber.d("Searched");
            }

            @Override
            public void onResultClick(SearchResult searchResult) {

            }
        });
    }

    private void doGeoSearch(final String query) {
        Runnable geoQueryWorker = new Runnable() {
            @Override
            public void run() {
                Geocoder geoCoder = new Geocoder(MapsActivity.this, Locale.UK);
                final ArrayList<SearchResult> results = new ArrayList<>();
                try {
                    List<Address> locations = geoCoder.getFromLocationName(query, 8);
                    for (Address a : locations) {
                        results.add(new SearchResult(a.getAddressLine(0)));
                        Timber.d(a.toString());
                    }
                } catch (IOException e) {
                    Timber.d("Looking up " + query + " :: " + e.getMessage());
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        searchBox.clearSearchable();
                        searchBox.addAllSearchables(results);
                        searchBox.updateResults();
                        searchBox.showLoading(false);
                        Timber.d("Updated search results");
                    }
                });

            }
        };

        new Thread(geoQueryWorker).start();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                Timber.d("Query: " + query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {

                Timber.d("Query text change: " + query);

                return true;

            }

        });

        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (ContactsContract.Intents.SEARCH_SUGGESTION_CLICKED.equals(intent.getAction())) {
            Toast.makeText(this, "Suggested: " + intent.getData(), Toast.LENGTH_LONG).show();
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Toast.makeText(this, "Query: " + query, Toast.LENGTH_LONG).show();
        }
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
                Timber.d("Long click, at" + latLng.toString());
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Timber.d("Result: " + resultCode + " Request " + requestCode);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onConnected(Bundle bundle) {
        connected = true;
        Timber.d("Connected");
        if (mMap != null) {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    googleApiClient);
            if (lastLocation != null) {
                Timber.d("Moovin and Zoomin");
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), 12.0f));
            } else {
                Timber.d("No Location");
            }
        } else {
            Timber.d("Map not created yet");
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Timber.d("Suspended");
        connected = false;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Timber.d("Failed");
        connected = false;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        if (connected) {
            if (item.getItemId() == R.id.action_nav) {
                if (destination != null) {
                    startNavigating();
                }
            } else {
                Timber.d("Selected " + item.getTitle());
            }
        } else {
            Toast.makeText(this, "Location services not currently available", Toast.LENGTH_LONG).show();
        }
        drawerLayout.closeDrawers();
        return false;
    }

    private void updateCompass() {
        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(
                googleApiClient);
        Location l = new Location("");
        l.setLatitude(destination.getPosition().latitude);
        l.setLongitude(destination.getPosition().longitude);
        float bearingTo = lastLocation.bearingTo(l);

        float ourBearing = lastLocation.getBearing();

        float requiredBearing;
        if (ourBearing > bearingTo) {
            requiredBearing = 360.0f - (ourBearing - bearingTo);
        } else {
            requiredBearing = bearingTo - ourBearing;
        }

        float distanceTo = l.distanceTo(lastLocation);

        Timber.d("BearingTo " + bearingTo + " My bearing " + ourBearing + " Required " + requiredBearing + " Distance " + distanceTo + "m");

        if (requiredBearing > 0.0f && requiredBearing <= 45.0f) {
            compass.setImageResource(R.drawable.sa);
        } else if (requiredBearing > 45.0f && requiredBearing <= 90.0f) {
            compass.setImageResource(R.drawable.sa_med);
        } else if (requiredBearing > 90.0f && requiredBearing <= 135.0f) {
            compass.setImageResource(R.drawable.sa_bad);
        } else if (requiredBearing > 135.0f && requiredBearing <= 180.0f) {
            compass.setImageResource(R.drawable.sa_bad);
        } else if (requiredBearing > 180.0f && requiredBearing <= 225.0f) {
            compass.setImageResource(R.drawable.sa_bad);
        } else if (requiredBearing > 225.0f && requiredBearing <= 270.0f) {
            compass.setImageResource(R.drawable.sa_bad);
        } else if (requiredBearing > 270.0f && requiredBearing <= 315.0f) {
            compass.setImageResource(R.drawable.sa_med);
        } else if (requiredBearing > 315.0f && requiredBearing <= 360.0f) {
            compass.setImageResource(R.drawable.sa);
        }

        compass.setRotation(requiredBearing);

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
    }

    private void startNavigating() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient, locationRequest, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        updateCompass();
                    }
                });
    }

}
