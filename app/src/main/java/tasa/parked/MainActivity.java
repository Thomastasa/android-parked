package tasa.parked;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity {


    final int ACCESS_LOCATION_CODE = 11;
    final int PLAY_SERVICES_CODE = 77;
    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    LocationListener mLocationListener;
    GoogleApiClient.ConnectionCallbacks mGoogleApiClientConnected;
    GoogleApiClient.OnConnectionFailedListener mGoogleApiClientConnectionFailed;
    boolean permissionGranted = false;

    Button btnSetLocation;
    Button btnViewLocation;
    Button btnGoToLocation;

    ProgressDialog processing;
    boolean isProcessing = false;
    Drawable processingCircle;
    Location loc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSetLocation = (Button) findViewById(R.id.btn_set_loc);
        btnViewLocation = (Button) findViewById(R.id.btn_view_loc);
        btnGoToLocation = (Button) findViewById(R.id.btn_goto_loc);

        // set processing circle
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            processingCircle = ContextCompat.getDrawable(this, R.drawable.progress_circle);
        } else {
            processingCircle = getResources().getDrawable(R.drawable.progress_circle);
        }

        btnSetLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getLocation();
            }
        });

        btnViewLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(loc != null){
                    // open in maps app
                    String uriString = "google.navigation:q="+loc.getLatitude()+","+loc.getLongitude();
                    Uri mapsUri = Uri.parse(uriString);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapsUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    if (mapIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(mapIntent);
                    }else{
                        showAlert("You do not have google maps app installed.");
                    }
                }else{
                    showAlert("Please set your parked location first.");
                }
            }
        });

        btnGoToLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(loc != null){
                    // open in maps app
                    String uriString = "google.navigation:q="+loc.getLatitude()+","+loc.getLongitude()+"&mode=w";
                    Uri mapsUri = Uri.parse(uriString);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapsUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    if (mapIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(mapIntent);
                    }else{
                        showAlert("You do not have google maps app installed.");
                    }
                }else{
                    showAlert("Please set your parked location first.");
                }
            }
        });

        // google api client connection callbacks
        mGoogleApiClientConnected = new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(@Nullable Bundle bundle) {
                showProcessing("Getting Current Location");
                // request location update
                startLocationRequest();
            }
            @Override
            public void onConnectionSuspended(int i) {
                hideProcessing();
            }
        };

        // google api client connection failed callback
        mGoogleApiClientConnectionFailed = new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                hideProcessing();
                showAlert("Error getting location. Please try again.");
            }
        };

        // location listener callback
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                hideProcessing();
                stopLocationRequest();
                if(location != null){
                    loc = location;
                    showAlert("Parked Location Set");
                }else{
                    showAlert("Error getting location. Please try again.");
                }

            }
        };

    }

    // show processing dialog
    public void showProcessing(String msg){
        if(processing == null){
            processing = new ProgressDialog(this);
            processing.setIndeterminateDrawable(processingCircle);
        }
        processing.setMessage(msg);
        processing.setCancelable(false);
        processing.show();
        isProcessing = true;
    }

    // hide processing dialog
    public void hideProcessing(){
        if(processing != null && processing.isShowing()){
            processing.dismiss();
        }
        processing = null;
        isProcessing = false;
    }

    public void showAlert(String msg){
        Log.e("__alert",msg);
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage(msg);
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alert.show();
    }

    // check permissions and ask for location
    private void getLocation(){
        // check location permission granted
        if(permissionGranted || checkLocationPermission()){
            // check google play services enabled
            if(checkPlayServices()){
                // build google api client
                buildGoogleApiClient();
            }
        }
    }

    // check permission: location
    private boolean checkLocationPermission(){
        // check if location permission granted or ask for permission
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_LOCATION_CODE);
            return false;
        }
        return true;
    }

    // check enabled: google play services
    private boolean checkPlayServices(){
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if(result != ConnectionResult.SUCCESS){
            if(googleAPI.isUserResolvableError(result)){
                googleAPI.getErrorDialog(this, result, PLAY_SERVICES_CODE).show();
            }
            return false;
        }
        return true;
    }

    // create google api client and connect
    protected synchronized void buildGoogleApiClient() {
        // create google api client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(mGoogleApiClientConnected)
                .addOnConnectionFailedListener(mGoogleApiClientConnectionFailed)
                .addApi(LocationServices.API)
                .build();
        // start google api client
        mGoogleApiClient.connect();
    }

    // request location
    private void startLocationRequest() {
        // create location request
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(10000);
        if(mGoogleApiClient != null && mGoogleApiClient.isConnected() && mLocationListener != null) {
            try {
                // start location updates
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, mLocationListener);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    // stop location request
    private void stopLocationRequest(){
        // stop location updates and stop google api client
        if(mGoogleApiClient != null && mGoogleApiClient.isConnected() && mLocationListener != null){
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mLocationListener);
            mGoogleApiClient.disconnect();

        }
    }

    // permission request results callbacks: location
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean found = false;
        switch(requestCode){
            case ACCESS_LOCATION_CODE:
                // check permission request results
                found = true;
                if(grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionGranted = true;
                    getLocation();
                }else{
                    permissionGranted = false;
                }
                break;
        }
        if(!found){
            showAlert("Error granting permissions. Please try again");
        }
    }

    // activity results callbacks: play services
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PLAY_SERVICES_CODE:
                // check play services request results
                if(resultCode == RESULT_OK){
                    getLocation();
                }
                break;
        }
    }

    @Override
    public void onStop() {
        stopLocationRequest();
        super.onStop();
    }

    @Override
    public void onPause() {
        stopLocationRequest();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            startLocationRequest();
        }
    }


}
