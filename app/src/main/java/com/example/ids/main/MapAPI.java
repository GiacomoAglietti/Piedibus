package com.example.ids.main;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.ids.R;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.TravelMode;

import java.util.ArrayList;
import java.util.List;

public class MapAPI {

    /**
     * to check if the user has accepted the geolocation permissions
     */
    public static final int  MY_PERMISSIONS_REQUEST_LOCATION = 1;
    /**
     * it is true if the permissions are granted, otherwise false
     */
    public static boolean locationPermissionGranted;
    /**
     * last known location of the user
     */
    private static Location lastKnownLocation;
    /**
     * a default zoom
     */
    public static final int DEFAULT_ZOOM = 15;


    /**
     * it checks if the localization permissions have been accepted,
     * if they have not yet been accepted, a request is made to the user to activate them
     * @param activity is the reference activity
     */
    public static void checkLocationPermission(FragmentActivity activity){
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setMessage("Questa app necessita dei permessi di localizzazione, per favore accetta per usare le funzionalità di localizzazione");
                builder.setCancelable(false);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestLocationPermission(activity);
                    }
                });
                builder.setNegativeButton("Annulla", null);
                builder.show();
            } else {
                requestLocationPermission(activity);
            }
        }
    }

    /**
     * if the version of the Android device is Q or higher it asks to activate ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION,
     * if lower than version Q it asks to activate only ACCESS_FINE_LOCATION
     * @param activity is the reference activity
     */
    public static void requestLocationPermission(FragmentActivity activity){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
        } else {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_LOCATION);
        }
    }

    /**
     * gets the location of the device only if the location permissions are granted
     * @param zoom if true it zoom to the current location
     * @param activity is the reference activity
     * @param fusedLocationProviderClient is the reference fused location provider client
     * @param myMap is the reference map
     */
    public static void getDeviceLocation(boolean zoom, FragmentActivity activity, FusedLocationProviderClient fusedLocationProviderClient, GoogleMap myMap) {

        try {
            if (locationPermissionGranted) {
                fusedLocationProviderClient.getLastLocation().addOnCompleteListener(activity, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                if (zoom) {
                                    myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                                }
                                LatLng latLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                                myMap.setMyLocationEnabled(true);
                                myMap.getUiSettings().setMyLocationButtonEnabled(false);
                            }
                        }
                    }
                });
            }
        }
        catch (SecurityException e)  {
            e.printStackTrace();
        }

    }



    /**
     * Asks to the user for permission to activate geolocation if not already active
     */
    public static void activateGPS(FragmentActivity activity) {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);

        builder.setAlwaysShow(true);

        Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(activity).checkLocationSettings(builder.build());

        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {

                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                } catch (ApiException e) {

                    switch (e.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:

                            try {
                                ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                                resolvableApiException.startResolutionForResult(activity,
                                        LocationRequest.PRIORITY_HIGH_ACCURACY);
                            } catch (IntentSender.SendIntentException ex) {
                                ex.printStackTrace();
                            }
                            break;

                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            Toast.makeText(activity, "GPS non attivato", Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            }
        });
    }

    /**
     * calculates the shortest direction from a start marker to a destination marker. The calculation is done through a request to google
     * and the result of the request is used to draw a polyline between the two marker
     * @param markerOrigin marker from which it starts to calculate the shortest direction
     * @param markerDest destination marker to calculate the direction
     * @param geoApiContext is the reference api context
     * @param myMap the map where you want calculate the direction
     */
    public static void calculateDirections(@NonNull Marker markerOrigin, @NonNull Marker markerDest, GeoApiContext geoApiContext, GoogleMap myMap){

        com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(
                markerDest.getPosition().latitude,
                markerDest.getPosition().longitude
        );
        DirectionsApiRequest directions = new DirectionsApiRequest(geoApiContext);
        directions.mode(TravelMode.WALKING);
        directions.origin(new com.google.maps.model.LatLng(markerOrigin.getPosition().latitude, markerOrigin.getPosition().longitude));

        directions.destination(destination).setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
                addPolylinesToMap(result,myMap);
            }

            @Override
            public void onFailure(Throwable e) {

            }
        });
    }

    /**
     * draw a polyline between two position
     * @param result is that it returns from the request made to google
     * @param myMap is the reference map
     */
    public static void addPolylinesToMap(final DirectionsResult result,GoogleMap myMap){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {

                for(DirectionsRoute route: result.routes){
                    List<com.google.maps.model.LatLng> decodedPath = PolylineEncoding.decode(route.overviewPolyline.getEncodedPath());

                    List<LatLng> newDecodedPath = new ArrayList<>();

                    for(com.google.maps.model.LatLng latLng: decodedPath){
                        newDecodedPath.add(new LatLng(
                                latLng.lat,
                                latLng.lng
                        ));
                    }
                    Polyline polyline = myMap.addPolyline(new PolylineOptions().addAll(newDecodedPath));
                    polyline.setColor(R.color.blue);

                }
            }
        });
    }

}
