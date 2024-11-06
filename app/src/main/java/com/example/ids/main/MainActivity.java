package com.example.ids.main;

import static com.example.ids.main.MapAPI.MY_PERMISSIONS_REQUEST_LOCATION;
import static com.example.ids.login.LoginActivity.userName;
import static com.example.ids.login.LoginActivity.userRole;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ids.DBModels.Stop;
import com.example.ids.main.Member.ParentsFragment;
import com.example.ids.R;
import com.example.ids.main.stopList.StopListFragment;
import com.example.ids.login.LoginActivity;
import com.example.ids.login.SocketHandler;
import com.google.android.gms.location.LocationRequest;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity{
    private BottomNavigationView bottomNavMenu;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    /**
     * imageView used to save the modified stops on the database
     */
    @SuppressLint("StaticFieldLeak")
    public static ImageView saveStopsImgView;
    /**
     * list of all the stops obtained from the database
     */
    public static List<Stop> stopList;
    /**
     * total number of stops saved in stopList
     */
    public static int totStop=0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        navigationView = findViewById(R.id.nav_view);

        View headerView = LayoutInflater.from(this).inflate(R.layout.nav_header, navigationView, true);


        Toolbar toolbar = findViewById(R.id.toolbar);
        drawer = findViewById(R.id.main_layout);
        bottomNavMenu = findViewById(R.id.bottom_nav_menu);
        saveStopsImgView = findViewById(R.id.saveStopsE_imgV);
        TextView userNameTxtV = headerView.findViewById(R.id.user_name_txtV);
        TextView userRoleTxtV = headerView.findViewById(R.id.user_role_txtV);
        ImageButton userImageImgBtn = headerView.findViewById(R.id.user_image_imgBtn);

        if (SocketHandler.getInstance().getSocket() == null)
            SocketHandler.getInstance().setSocket();

        try{
            SocketHandler.getInstance().getSocket().emit("getAllStops");

        } catch (Exception e) {
            e.printStackTrace();
        }

        loadStopListFromDB(this);

        setSupportActionBar(toolbar);

        userRoleTxtV.setText(userRole);
        userNameTxtV.setText(userName);

        if (!LoginActivity.permission.checkAdminPermission()) {
            navigationView.getMenu().findItem(R.id.nav_piedibus).setVisible(false);
        }
        navigationView.setNavigationItemSelectedListener(navListener);
        bottomNavMenu.setOnItemSelectedListener(bottomNavListener);
        bottomNavMenu.setVisibility(View.GONE);

        ActionBarDrawerToggle toogle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toogle);
        toogle.syncState();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
            navigationView.setCheckedItem(R.id.nav_home);
        }

        saveStopsImgView.setVisibility(View.GONE);

        saveStopsImgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(saveStopsImgView.getTag().equals("enable")){
                    List<JSONObject> stringList = new ArrayList<>();

                    for (Stop elem : MainActivity.stopList){
                        JSONObject obj = new JSONObject();
                        try {
                            obj.put("_id", elem.get_id());
                            obj.put("name_stop", elem.getName_stop());
                            obj.put("latitude", elem.getLatitude());
                            obj.put("longitude", elem.getLongitude());
                            obj.put("id_itinerary", elem.getId_itinerary());

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        stringList.add(obj);
                    }
                    JSONArray arr = new JSONArray(stringList);
                    SocketHandler.getInstance().getSocket().emit("deleteAndSaveStop", arr);
                    saveStopsImgView.setTag("disable");
                    saveStopsImgView.setImageResource(R.drawable.ic_save_stop_disable);
                } else {
                    Toast.makeText(MainActivity.this, "Nessuna modifica è stata fatta", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {

        if (drawer.isDrawerOpen(GravityCompat.START)){
            drawer.closeDrawer(GravityCompat.START);
        }  else {
            getSupportFragmentManager().popBackStack();
        }
    }

    private final NavigationView.OnNavigationItemSelectedListener navListener = new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()){
                case R.id.nav_home:
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
                    bottomNavMenu.setVisibility(View.GONE);
                    saveStopsImgView.setVisibility(View.GONE);
                    break;
                case R.id.nav_piedibus:
                    bottomNavMenu.setVisibility(View.VISIBLE);
                    bottomNavMenu.setOnItemSelectedListener(bottomNavListener);
                    saveStopsImgView.setVisibility(View.GONE);
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new PathFragment()).commit();
                    break;
                case R.id.nav_members:
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ParentsFragment()).commit();
                    bottomNavMenu.setVisibility(View.GONE);
                    saveStopsImgView.setVisibility(View.GONE);
                    break;
                case R.id.nav_logout:
                    saveStopsImgView.setVisibility(View.GONE);
                    saveStopsImgView.setVisibility(View.GONE);
                    bottomNavMenu.setVisibility(View.GONE);
                    SocketHandler.getInstance().getSocket().emit("clientDisconnected");
                    SocketHandler.getInstance().closeConnection();
                    Intent myIntent = new Intent(MainActivity.this, LoginActivity.class);
                    MainActivity.this.startActivity(myIntent);
                    break;
            }
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }
    };


    private final BottomNavigationView.OnItemSelectedListener bottomNavListener = new BottomNavigationView.OnItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Fragment selectedFragment = null;
            switch (item.getItemId()) {
                case R.id.nav_maps:
                    selectedFragment = new PathFragment();
                    saveStopsImgView.setVisibility(View.GONE);
                    navigationView.setVisibility(View.VISIBLE);
                    break;
                case R.id.nav_stop:
                    selectedFragment = new StopListFragment();
                    saveStopsImgView.setVisibility(View.VISIBLE);
                    navigationView.setVisibility(View.VISIBLE);
                    break;
            }
            if(selectedFragment == null)
                return false;
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
            return true;
        }
    };

    /**
     * save on stopList all stops from database
     *
     * @param fragmentActivity is the reference activity
     */
    public static void loadStopListFromDB(FragmentActivity fragmentActivity) {
        SocketHandler.getInstance().getSocket().on("getStops", new Emitter.Listener(){
            @Override
            public void call(Object... args) {
                if (fragmentActivity != null) {
                    fragmentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            stopList = new ArrayList<>();

                            try {
                                int i;
                                JSONObject data = (JSONObject) args[0];
                                String allStops = data.getString("stops");
                                JSONArray arr = new JSONArray(allStops);

                                for (i = 0; i < arr.length(); i++) {
                                    int num = Integer.parseInt(arr.getJSONObject(i).getString("_id"));
                                    Stop stop = new Stop(num,
                                            arr.getJSONObject(i).getString("name_stop"),
                                            arr.getJSONObject(i).getString("latitude"),
                                            arr.getJSONObject(i).getString("longitude"),
                                            arr.getJSONObject(i).getString("id_itinerary"));
                                    stopList.add(stop);
                                }
                                totStop = stopList.size();
                                Collections.sort(stopList);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LocationRequest.PRIORITY_HIGH_ACCURACY) {
            if (resultCode == Activity.RESULT_OK) {
                HomeFragment.gpsOn.setVisibility(View.VISIBLE);
                HomeFragment.gpsOff.setVisibility(View.GONE);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                MapAPI.locationPermissionGranted = true;
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Snackbar.make(findViewById(android.R.id.content), "Autorizzazione app negata.", Snackbar.LENGTH_LONG).setAction("Impostazioni", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        }
                    }).show();
                }
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        MapAPI.locationPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

}