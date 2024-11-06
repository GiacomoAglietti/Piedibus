package com.example.ids.main;


import static com.example.ids.main.MapAPI.DEFAULT_ZOOM;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Bundle;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;


import com.example.ids.DBModels.Stop;
import com.example.ids.R;
import com.example.ids.login.SocketHandler;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
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
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.maps.GeoApiContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


public class PathFragment extends Fragment implements OnMapReadyCallback {
    private GoogleMap myMap;
    private SupportMapFragment mapFragment;
    /**
     * list used to store the Place that is searched with the Google search view (AutocompleteSupportFragment)
     */
    private List<Place> placeList = null;
    private LocationManager manager;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private GeoApiContext geoApiContext = null;
    /**
     * button used to save the Place (saved on placeList) on stopList and store it as new Stop on database
     */
    private Button saveBtn;
    /**
     * marker created when a position is searched. it's draggable
     */
    private Marker newStopMarker;

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



    private final String id_itinerary = "it-01";



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        if (geoApiContext == null)
            geoApiContext = new GeoApiContext.Builder().apiKey(getString(R.string.google_maps_key)).build();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        return inflater.inflate(R.layout.fragment_path, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        newStopMarker =null;

        gpsOn = getView().findViewById(R.id.img_btn_gps_on);
        gpsOff = getView().findViewById(R.id.img_btn_gps_off);

        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_maps_fragment2);
        mapFragment.getMapAsync(this);

        if(getView() != null)
            saveBtn = getView().findViewById(R.id.saveNewStop_btn);


        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), getString(R.string.google_maps_key));
        }

        PlacesClient placesClient = Places.createClient(requireContext());

        placeList = new ArrayList<>();

        manager =  (LocationManager) getActivity().getSystemService( Context.LOCATION_SERVICE );

        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            gpsOff.setVisibility(View.VISIBLE);
            gpsOn.setVisibility(View.GONE);
        }
        else {
            gpsOn.setVisibility(View.VISIBLE);
            gpsOff.setVisibility(View.GONE);
        }

        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment) getChildFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        autocompleteFragment.setTypeFilter(TypeFilter.GEOCODE);
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.PHOTO_METADATAS, Place.Field.LAT_LNG));

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                placeList.add(0, place);

                LatLng lating = new LatLng(place.getLatLng().latitude,place.getLatLng().longitude);

                MarkerOptions markerOptions = new MarkerOptions().position(lating).title(place.getName());

                if(MainActivity.stopList.isEmpty())
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

                newStopMarker = myMap.addMarker(markerOptions);
                newStopMarker.setDraggable(true);

                myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lating, DEFAULT_ZOOM));

            }

            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(requireContext(), "Posizione non trovata", Toast.LENGTH_SHORT).show();
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (placeList==null || placeList.size() <= 0 || newStopMarker == null) {
                    Toast.makeText(requireActivity(), "Posizione non valida", Toast.LENGTH_SHORT).show();
                }
                else {
                    try{
                        LatLng latLng = newStopMarker.getPosition();
                        int numStop = 1;
                        if(!MainActivity.stopList.isEmpty())
                            numStop = MainActivity.stopList.get(MainActivity.totStop - 1).get_id() + 1;

                        String latitude = String.valueOf(latLng.latitude);
                        String longitude = String.valueOf(latLng.longitude);

                        Stop stop = new Stop(numStop, newStopMarker.getTitle(), latitude, longitude, id_itinerary);
                        MainActivity.stopList.add(stop);

                        String numStopS = String.valueOf(numStop);

                        SocketHandler.getInstance().getSocket().emit("saveLocation", latLng.latitude, latLng.longitude, newStopMarker.getTitle(), numStopS);

                        Toast.makeText(requireActivity(), "Posizione salvata", Toast.LENGTH_SHORT).show();
                        MainActivity.totStop++;
                        newStopMarker.setDraggable(false);


                    } catch (Exception e){
                        e.printStackTrace();
                    }


                }
            }
        });

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

    }

    private boolean checkIfNull(Fragment fragment){
        return fragment != null;
    }

    /**
     * set the map, then loads the Stops as markers on the map by connecting them with a polyline
     * and then, if the device has the GPS active and it is connected to the server, move the map view to the starting stop
     * @param googleMap is the reference map
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

        boolean isSettedCameraOnFirstPos = false;
        Marker markerDest = null;
        Marker markerStart = null;

        myMap = googleMap;

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

        myMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDrag(@NonNull Marker marker) {
            }

            @Override
            public void onMarkerDragEnd(@NonNull Marker marker) {
                String address = " ";
                Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(marker.getPosition().latitude, marker.getPosition().longitude, 1);
                    address = addresses.get(0).getAddressLine(0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                marker.setTitle(address);
            }

            @Override
            public void onMarkerDragStart(@NonNull Marker marker) {
            }
        });

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