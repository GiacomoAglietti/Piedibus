package com.example.ids.main;

import static com.example.ids.main.MapAPI.DEFAULT_ZOOM;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.example.ids.DBModels.Stop;
import com.example.ids.R;
import com.example.ids.foregroundService.*;
import com.example.ids.login.LoginActivity;
import com.example.ids.login.SocketHandler;
import com.example.ids.main.Member.ParentsFragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;

import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.maps.GeoApiContext;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.emitter.Emitter;


public class HomeFragment extends Fragment implements OnMapReadyCallback{
    private static GoogleMap myMap;
    /**
     * button that checks if the localization permissions have been accepted by the user,
     * if not accepted a request is made to the user to activate them.
     * if the permissions have been accepted, the gps is checked,
     * if it's active it moves the view of the user's map to its current position,
     * otherwise it asks to the user to activate the gps
     */
    @SuppressLint("StaticFieldLeak")
    public static ImageButton gpsOn;
    /**
     * button that checks if the localization permissions have been accepted by the user,
     * if not accepted a request is made to the user to activate them.
     * if the permissions have been accepted, the gps is checked,
     * if it's active it activates the gpsOn button,
     * otherwise it asks to the user to activate the gps
     */
    @SuppressLint("StaticFieldLeak")
    public static ImageButton gpsOff;
    /**
     * button to get the current location of piedibus only while a guide is sharing his position
     */
    @SuppressLint("StaticFieldLeak")
    public static ImageButton buttonPiedibus;


    private FusedLocationProviderClient fusedLocationProviderClient;
    /**
     * button used only by admins or guides to share position of the piedibus
     */
    private Button startPiedibus;
    /**
     * button used only by admins or guides to stop sharing position of the piedibus
     */
    private Button stopPiedibus;
    private SupportMapFragment mapFragment;
    private LocationCallback locationCallback;
    private LocationManager manager;
    /**
     * it checks if user is sharing his position
     */
    private Boolean isSharing;
    private GeoApiContext geoApiContext = null;
    /**
     * it creates a marker on piedibus location
     */
    private Marker piedibus;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);


        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_maps_fragment);
        mapFragment.getMapAsync(this);

        if (geoApiContext == null)
            geoApiContext = new GeoApiContext.Builder().apiKey(getString(R.string.google_maps_key)).build();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        Places.initialize(getContext(), getString(R.string.google_maps_key));
        PlacesClient placesClient = Places.createClient(requireActivity());

        isSharing = false;

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        gpsOn = getView().findViewById(R.id.img_btn_gps_on);
        gpsOff = getView().findViewById(R.id.img_btn_gps_off);
        buttonPiedibus = getView().findViewById(R.id.img_btn_piedibus);
        startPiedibus = getView().findViewById(R.id.startPiedibus);
        stopPiedibus = getView().findViewById(R.id.stopPiedibus);

        receivePiedibusLocation();
        stopUpdatePiedibusLocation();


        manager =  (LocationManager) getActivity().getSystemService( Context.LOCATION_SERVICE );

        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            gpsOff.setVisibility(View.VISIBLE);
            gpsOn.setVisibility(View.GONE);
        }
        else {
            gpsOn.setVisibility(View.VISIBLE);
            gpsOff.setVisibility(View.GONE);
        }

        gpsOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
                        MapAPI.activateGPS(requireActivity());
                    } else {
                        gpsOff.setVisibility(View.GONE);
                        gpsOn.setVisibility(View.VISIBLE);
                    }
                }
                else {
                    MapAPI.checkLocationPermission(requireActivity());
                }
            }
        });

        gpsOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    if (!manager.isProviderEnabled( LocationManager.GPS_PROVIDER )) {
                        gpsOff.setVisibility(View.VISIBLE);
                        gpsOn.setVisibility(View.GONE);
                    } else {
                        MapAPI.getDeviceLocation(true,requireActivity(), fusedLocationProviderClient, myMap);
                    }
                }
                else {
                    MapAPI.checkLocationPermission(requireActivity());
                }
            }
        });

        buttonPiedibus.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                  if (piedibus != null) {
                      myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(piedibus.getPosition(), DEFAULT_ZOOM));
                  }
              }
        });


        if (LoginActivity.permission.checkGuidePermission() || LoginActivity.permission.checkAdminPermission()) {
            startPiedibus.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    if (!isSharing) {
                        isSharing = true;
                        startUpdateOfGPSLocation();
                        startService();
                        startPiedibus.setVisibility(View.GONE);
                        stopPiedibus.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
        else {
            startPiedibus.setVisibility(View.GONE);
        }

        stopPiedibus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isSharing = false;
                stopUpdateOfGPSLocation();
                stopService();
                startPiedibus.setVisibility(View.VISIBLE);
                stopPiedibus.setVisibility(View.GONE);
            }
        });
    }

    /**
     * if the guide stops sharing the position, the piedibus position is no longer updated
     */
    private void stopUpdatePiedibusLocation() {
        SocketHandler.getInstance().getSocket().on("stopUpdateLocation", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (LoginActivity.permission.checkGuidePermission() || LoginActivity.permission.checkAdminPermission()) {
                                    startPiedibus.setEnabled(true);
                                }

                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * receives the current position of the piedibus when a guide or admin has started the piedibus
     */
    private void receivePiedibusLocation() {
        SocketHandler.getInstance().getSocket().on("receiveLocation", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONObject data = (JSONObject) args[0];
                                String latStr = data.getString("guideLatitude");
                                Double latitude = Double.parseDouble(latStr);
                                JSONObject data2 = (JSONObject) args[1];
                                String lonStr = data2.getString("guideLongitude");
                                Double longitude = Double.parseDouble(lonStr);

                                LatLng piedibusLocation = new LatLng(latitude, longitude);

                                if((LoginActivity.permission.checkAdminPermission() || LoginActivity.permission.checkGuidePermission()) && startPiedibus.isEnabled())
                                    startPiedibus.setEnabled(false);

                                if(piedibus!=null)
                                    piedibus.remove();

                                MarkerOptions markerOptions = new MarkerOptions().position(piedibusLocation).title("Piedibus");
                                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                                piedibus = myMap.addMarker(markerOptions);

                            } catch (JSONException ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * every 5 seconds it emits a socket event where it sends the current location of the guide of the piedibus
     */
    private void startUpdateOfGPSLocation(){
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setInterval(5000);
            locationRequest.setFastestInterval(2500);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    //Showing the latitude, longitude and accuracy on the home screen.
                    for (Location location : locationResult.getLocations()) {
                        LatLng a = new LatLng(location.getLatitude(), location.getLongitude());
                        SocketHandler.getInstance().getSocket().emit("sendLocation", a.latitude, a.longitude);
                    }
                }
            };
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    /**
     * it stops sending the current guide position
     */
    private void stopUpdateOfGPSLocation() {
        SocketHandler.getInstance().getSocket().emit("stopSharingLocation");
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);

    }

    /**
     *it starts the PiedibusService
     */
    public void startService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent serviceIntent = new Intent(requireActivity(), PiedibusService.class);
            requireActivity().startForegroundService(serviceIntent);
        }

    }

    /**
     * it stops the PiedibusService
     */
    public void stopService() {
        Intent serviceIntent = new Intent(requireActivity(), PiedibusService.class);
        requireActivity().stopService(serviceIntent);
    }

    /**
     * set the map, then if the device has the GPS active, the map view is moved to the user's current position
     * and then loads the Stops as markers on the map by connecting them with a polyline
     * @param googleMap is the reference map
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        boolean isSettedCameraOnFirstPos = false;
        Marker markerDest = null;
        Marker markerStart = null;

        myMap = googleMap;

        if (manager.isProviderEnabled( LocationManager.GPS_PROVIDER ))
            MapAPI.getDeviceLocation(true,requireActivity(),fusedLocationProviderClient,myMap);

        if (MainActivity.stopList != null) {
            for (Stop elem : MainActivity.stopList) {
                LatLng location = new LatLng(Double.parseDouble(elem.getLatitude()), Double.parseDouble(elem.getLongitude()));

                if (!isSettedCameraOnFirstPos) {
                    MarkerOptions markerOptions = new MarkerOptions().position(location).title(elem.getName_stop());
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    markerStart = myMap.addMarker(markerOptions);
                    myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, DEFAULT_ZOOM));
                    isSettedCameraOnFirstPos = true;
                } else {
                    markerDest = myMap.addMarker(new MarkerOptions().position(location).title(elem.getName_stop()));
                    MapAPI.calculateDirections(markerStart,markerDest,geoApiContext,myMap);
                    markerStart = markerDest;
                }
            }
        }
    }


    private boolean checkIfNull(Fragment fragment){
        return fragment != null;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (checkIfNull(mapFragment))
            mapFragment.onStart();

    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkIfNull(mapFragment))
            mapFragment.onResume();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (checkIfNull(mapFragment))
            mapFragment.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (checkIfNull(mapFragment))
            mapFragment.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (checkIfNull(mapFragment))
            mapFragment.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (checkIfNull(mapFragment))
            mapFragment.onLowMemory();
    }

}