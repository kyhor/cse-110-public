// Team Number: 31
// Project: CoupleTones MileStone
// FileName: MapsActivity.java
// Description: Main activity for this app, calls other classes and methods
//              This app will send SMS to the user's partner when a user
//              visits his/her saved favorite locations.

package com.example.khor_000.testapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.drm.DrmStore;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    // constants
    private static final double ONE_TENTH_MILE = 160.9;
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 100;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 101;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 102;

    // variables
    private GoogleMap mMap;
    private View setLocat;
    private Button showList;
    private String locationName;
    private ArrayAdapter<LocationItem> arrayAdapter;
    //private ArrayList<String> arrayList;
    private ListView listView;
    private Marker addFavor;
    private List<LocationItem> myLocations = new ArrayList<LocationItem>();
    private List<Marker> locMakers = new ArrayList<Marker>();
    private String pastLocation = "";

    // communicate data with service
    private Intent service;

    private Button partner;
    private String myname = "";
    private String partnerName = "";
    private View editPartner;

    Firebase myfireref;

    /**
         * ATTENTION: This was auto-generated to implement the App Indexing API.
         * See https://g.co/AppIndexing/AndroidStudio for more information.
         */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // start the firebase to use server
        Firebase.setAndroidContext(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        // initialize my firebase sever
        myfireref = new Firebase("https://coupletonescse110.firebaseio.com/"+myname);
        // use the service to receive notification partner's arrive or leave fav location
        //test
        Toast.makeText(this, "no partner", Toast.LENGTH_SHORT).show();
        service = new Intent (MapsActivity.this, NotificationService.class);
        service.putExtra("partnername", partnerName);
        startService(service);
        //  set up buttons and view adapter
        showList = (Button) findViewById(R.id.listButton);
        partner=(Button) findViewById(R.id.partner);
        arrayAdapter = new MyLocAdapter();
        listView = (ListView) findViewById(R.id.lv);
        listView.setAdapter(arrayAdapter);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
        // call method to monitor click event on a location
        locationClickHandler();
    }
    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Maps Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.example.khor_000.testapp/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Maps Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.example.khor_000.testapp/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    // method to set object to listview
    private class MyLocAdapter extends ArrayAdapter<LocationItem> {
        public MyLocAdapter() {
            super(MapsActivity.this, R.layout.single_location, myLocations);
        }

        public void add(LocationItem newLoc) {
            super.add(newLoc);
            super.notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //make sure we have view if null
            View itemView = convertView;
            if (itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.single_location, parent, false);
            }

            //find the location to work with
            LocationItem currLoc = myLocations.get(position);

            //fill the view and location name
            TextView makeText = (TextView) itemView.findViewById(R.id.locName);
            makeText.setText(currLoc.getName());

            //latitude
            TextView latText = (TextView) itemView.findViewById(R.id.latTxt);
            Double latVal = currLoc.getLatitude();
            latVal = Double.parseDouble(new DecimalFormat("##.###").format(latVal));
            latText.setText("LAT: " + latVal + "\t");

            //longitude
            TextView lngText = (TextView) itemView.findViewById(R.id.lngTxt);
            Double lngVal = currLoc.getLatitude();
            lngVal = Double.parseDouble(new DecimalFormat("##.###").format(lngVal));
            lngText.setText("LON: " + lngVal);

            return itemView;
        }
    };

    // method to delete a location on favorite locations
    private void locationClickHandler(){
        ListView list = (ListView) findViewById(R.id.lv);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View viewClicked,
                                    int position, long id ) {

                if (!myLocations.isEmpty()) {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(MapsActivity.this);
                    builder1.setMessage("Confirm Delete " + myLocations.get(position).getName() + " ?");

                    final int index = position;

                    builder1.setPositiveButton("YES", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {

                            String deleName = myLocations.get(index).getName();

                            myLocations.remove(index);

                            locMakers.get(index).remove();

                            locMakers.remove(index);

                            arrayAdapter.notifyDataSetChanged();

                            Toast.makeText(getApplicationContext(), "" + deleName + " Deleted", Toast.LENGTH_SHORT).show();

                        }                    // close the prompt after click cancel
                    }).setNegativeButton("No", null).setCancelable(true);  // cancelable even using back key

                    AlertDialog alert = builder1.create();
                    alert.show();

                }
            }

        });

    }

    public void goHistList(View view){

        Intent i = new Intent(this, DailyLocList.class);

        this.startActivity(i);
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
        // Add a marker in San Diego and move the camera
        LatLng sanDiego = new LatLng(32.7157, -117.1611);
        addFavor = mMap.addMarker(new MarkerOptions().position(sanDiego).title("Where am I"));
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // show the location list when clicked
        showList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                listView.setVisibility(View.VISIBLE);
                showList.setVisibility(View.GONE);
            }
        });

        // method to collect partner info
        partner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editPartner = LayoutInflater.from(MapsActivity.this).inflate(R.layout.partnerwindow, null);
                //LayoutInflater inflater= MapsActivity.this.getLayoutInflater();
                final EditText partnameEdit = (EditText) editPartner.findViewById(R.id.partnername);
                final EditText mynameEdit = (EditText) editPartner.findViewById(R.id.myname);
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapsActivity.this); //Read Update
                alertDialog.setView(editPartner);
                alertDialog.setTitle("Partner please");


                alertDialog.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        stopService(service);
                        partnerName = partnameEdit.getText().toString();
                        myname = mynameEdit.getText().toString();
                        // to renew the partner insider the thread
                        SharedPreferences sp = getSharedPreferences("usersInfo", MODE_PRIVATE );
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("partner", partnerName);
                        editor.putString("user", myname);
                        editor.apply();
                        startService(service);
                        Log.v("V", myname);

                    }
                }).setNegativeButton("", null).setCancelable(true);

                AlertDialog alert1 = alertDialog.create();
                alert1.show();  //<-- See This!
            }
        });

        // handle on click event on map
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(final LatLng point) {

                listView.setVisibility(View.GONE);
                showList.setVisibility(View.VISIBLE);
                //  show the alert box
                addFavor.setPosition(point);
                setLocat = LayoutInflater.from(MapsActivity.this).inflate(R.layout.set_location, null);
                AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                builder.setMessage("Assign A Name To This Location");
                final EditText favName = (EditText) setLocat.findViewById(R.id.nameOfLocation);

                builder.setView(setLocat).setPositiveButton("Add", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        // set it to Data - base
                        locationName = favName.getText().toString();

                        if (!locationName.isEmpty()) {

                            //save location with given info
                            LocationItem tempLoc = new LocationItem(locationName, point.latitude, point.longitude);

                            arrayAdapter.add(tempLoc);

                            //add a marker on the map
                            addFavor = mMap.addMarker(new MarkerOptions().position(point).title("Where am I"));
                            Marker temp = addFavor;
                            locMakers.add(temp);

                            Toast.makeText(getApplicationContext(), "Added To Favorite", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "No Input Detected", Toast.LENGTH_SHORT).show();
                            addFavor.remove();

                        }

                    }                    // close the prompt after click cancel
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getApplicationContext(), "Canceled", Toast.LENGTH_SHORT).show();
                        addFavor.remove();

                    }                    // close the prompt after click cancel
                }).setCancelable(true);  // cancelable even using back key

                AlertDialog alert = builder.create();
                alert.show();
            }
        });


        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //check if location is in the range of any of the favorite locations
                for (LocationItem i : myLocations) {
                    if (getRange(location.getLatitude(), location.getLongitude(), i.getLatitude(),
                            i.getLongitude()) < ONE_TENTH_MILE && pastLocation != i.getName()) {
                        // only send msg if visit different fav location
                        if ( pastLocation != i.getName()) {
                            Toast.makeText(getApplicationContext(), "Visit: " + i.getName(),
                                    Toast.LENGTH_SHORT).show();

                            //checks for phone number
                            if (!myname.isEmpty()) {
                                // when user arrive or leave loc, it will notify sever then to partner phones
                                // send out the notification to database
                                myfireref.setValue(i.getName());

                                pastLocation = i.getName();
                            }
                        }

                    }

                    //reset past location when user leaves a farvorite location
                    if( getRange(location.getLatitude(), location.getLongitude(), i.getLatitude(), i.getLongitude()) > ONE_TENTH_MILE) {
                        pastLocation = "";
                    }
                }
            }

            // calculate the distance between two points
            public double getRange(double lat1, double lon1, double lat2, double lon2) {
                Double R = 6378.137; // Radius of earth in KM
                Double dLat = (lat2 - lat1) * Math.PI / 180;
                Double dLon = (lon2 - lon1) * Math.PI / 180;

                Double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                        Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
                                Math.sin(dLon / 2) * Math.sin(dLon / 2);

                Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
                Double d = R * c;
                d = d * 1000; // meters
                return d;
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        String locationProvider = LocationManager.GPS_PROVIDER;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

            Log.d("test1", "ins");
            return;

        } else if (mMap != null) {
            Log.d("test2", "outs");
            mMap.setMyLocationEnabled(true);
        }

        locationManager.requestLocationUpdates(locationProvider, 0, 0, locationListener);

    }
/*
    // send SMS to partner
    public void sendSMS(String message, String phoneNo){
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, message, null, null);
            Toast.makeText(getApplicationContext(), "SMS sent.", Toast.LENGTH_LONG).show();
        }
        catch (Exception e) {
            Toast.makeText(getApplicationContext(), "SMS failed, please try again.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_SEND_SMS: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(this, "Until you grant the permission, we cannot send notification " +
                            "to your partner", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(this, "Until you grant the permission, we cannot get your " +
                            "current location", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(this, "Until you grant the permission, we cannot get your " +
                            "current location", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }
}


