package com.khacks.srp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;

public class MapsActivity extends FragmentActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private final double RADIUS = 6372.8; // In kilometers

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private EditText mFromField;
    private EditText mToField;
    private Button mSend;
    private Button mSafeSearch;
    private Button mTravelButton;

    private RequestQueue mQueue;
    private String mMapQuestUrl;

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;

    // Wether if a Yo has been sent for his mark
    private ArrayList<Boolean> markerVisited;
    private ArrayList<LatLng> markerLocation;
    private ArrayList<String> markerName;
    private JSONObject queryControlPoints;

    // True if there is tracking navigation
    private boolean mTracking = false;
    // Distance to detect the mark, in km.
    final private double mMarkMinDist = 2.0;

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    // Function to hide keyboard
    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        buildGoogleApiClient();
        setUpMapIfNeeded();

        // Setup the layout elements
        mFromField = (EditText) findViewById(R.id.fromField);
        mToField = (EditText) findViewById(R.id.toField);
        mSend = (Button) findViewById(R.id.searchButton);
        mSafeSearch = (Button) findViewById(R.id.safeButton);
        mTravelButton = (Button) findViewById(R.id.travelButton);

        // Instantiate the RequestQueue.
        mQueue = Volley.newRequestQueue(this);
        final Activity mActivity = this;

        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Desactivate navigating mode
                mTracking = false;

                markerVisited = new ArrayList<>();
                markerLocation = new ArrayList<>();
                markerName = new ArrayList<>();

                String from = mFromField.getText().toString();
                String to = mToField.getText().toString();
                if (!from.isEmpty() && !to.isEmpty()) {
                    // Hide keyboard
                    hideSoftKeyboard(mActivity);
                    // Initialize variables
                    try {
                        mMapQuestUrl ="http://open.mapquestapi.com/directions/v2/route?key=Fmjtd%7Cluu8210ynq%2C8w%3Do5-94r504&ambiguities=ignore&avoidTimedConditions=false&outFormat=json&routeType=fastest&enhancedNarrative=false&shapeFormat=raw&generalize=0&locale=en_US&unit=m&from="+
                                URLEncoder.encode(from, "utf-8")+"&to="+
                                URLEncoder.encode(to, "utf-8");
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(getApplicationContext(), "Calculating route", Toast.LENGTH_SHORT).show();

                    doGETRequest(mMapQuestUrl, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            drawJSONDirection(response);
                            mSafeSearch.setVisibility(View.VISIBLE);
                            mSafeSearch.setEnabled(true);
                            callJSolaServer(response);
                        }
                    });
                }
                else {
                    // One of the fields is empty, create a toast
                    Context context = getApplicationContext();
                    int duration = Toast.LENGTH_SHORT;
                    if (from.isEmpty()) Toast.makeText(context, "Origin is missing!", duration).show();
                    else Toast.makeText(context, "Destination is missing!", duration).show();
                }
            }
        });

        mTravelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTracking = true;
            }
        });

        mSafeSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getApplicationContext();
                int duration = Toast.LENGTH_SHORT;
                Toast.makeText(context, "Calculating SAFE route", duration).show();
                String url = "http://open.mapquestapi.com/directions/v2/route?key=Fmjtd%7Cluu8210ynq%2C8w%3Do5-94r504&ambiguities=ignore&inFormat=json";
                JSONObject query = new JSONObject();
                try {
                    String from = mFromField.getText().toString();
                    String to = mToField.getText().toString();
                    query.accumulate("locations", new JSONArray());
                    query.getJSONArray("locations").put(from);
                    query.getJSONArray("locations").put(to);
                    query.accumulate("options", new JSONObject());
                    query.getJSONObject("options").accumulate("avoidTimedConditions", false);
                    query.getJSONObject("options").accumulate("routeType","fastest");
                    query.getJSONObject("options").accumulate("enhancedNarrative",false);
                    query.getJSONObject("options").accumulate("shapeFormat","raw");
                    query.getJSONObject("options").accumulate("generalize",0);
                    query.getJSONObject("options").accumulate("locale","en_US");
                    query.getJSONObject("options").accumulate("unit","m");
                    query.getJSONObject("options").accumulate("routeControlPointCollection",
                            queryControlPoints.get("routeControlPointCollection"));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                Log.v("LO", url);
                Log.v("LO", query.toString());

                doPOSTRequest(url, query, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        drawJSONDirection(response);

                        JSONArray array2 = new JSONArray();
                        try {
                            //array2 = response.getJSONObject("route").getJSONArray("shapePoints");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    void callJSolaServer(JSONObject response) {
        String url = "http://37.187.81.177:8000/point/route";
        JSONArray array = new JSONArray();
        JSONObject query = new JSONObject();
        try {
            array = response.getJSONObject("route").getJSONArray("legs").
                    getJSONObject(0).getJSONArray("maneuvers");
            for (int i = 0; i < array.length(); ++i) {
                array.getJSONObject(i).remove("signs");
                array.getJSONObject(i).remove("maneuverNotes");
                array.getJSONObject(i).remove("index");
                array.getJSONObject(i).remove("narrative");
                array.getJSONObject(i).remove("direction");
                array.getJSONObject(i).remove("iconUrl");
                array.getJSONObject(i).remove("time");
                array.getJSONObject(i).remove("distance");
                array.getJSONObject(i).remove("linkIds");
                array.getJSONObject(i).remove("transportMode");
                array.getJSONObject(i).remove("attributes");
                array.getJSONObject(i).remove("formattedTime");
                array.getJSONObject(i).remove("directionName");
                array.getJSONObject(i).remove("mapUrl");
                array.getJSONObject(i).remove("turnType");
            }
            query.accumulate("maneuvers", array);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        //Log.v("LO", response.toString());
        doPOSTRequest(url, query, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                searchControlPoints(response);
            }
        });
    }
    void doGETRequest(String url, Response.Listener<JSONObject> listener) {
        // Request a string response from the provided URL.
        //Log.v("LO", "WOLO");
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, listener, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                        if (error != null) {
                            if (error.getMessage() != null) {
                                error.printStackTrace();
                                Toast.makeText(getApplicationContext(),
                                        "There was an error, try again", Toast.LENGTH_SHORT).show();
                            }
                            else {
                                Toast.makeText(getApplicationContext(),
                                        "There was a small error, try again", Toast.LENGTH_SHORT).show();
                            }
                        }
                        else {
                            Toast.makeText(getApplicationContext(),
                                    "There was a big error, try again", Toast.LENGTH_SHORT).show();
                        }
                    }
                });


        // Add the request to the RequestQueue.
        mQueue.add(jsObjRequest);
    }

    private void doPOSTRequest(String url, JSONObject query, Response.Listener<JSONObject> listener) {
        // Request a string response from the provided URL.
        //Log.v("LO", url);
        //Log.v("LO", query.toString());
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.POST, url, query, listener, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error != null) {
                            if (error.getMessage() != null) {
                                Toast.makeText(getApplicationContext(),
                                        "There was an error, try again", Toast.LENGTH_SHORT).show();
                            }
                            else {
                                Toast.makeText(getApplicationContext(),
                                        "There was a small error, try again", Toast.LENGTH_SHORT).show();
                            }
                        }
                        else {
                            Toast.makeText(getApplicationContext(),
                                    "There was a big error, try again", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        // Add the request to the RequestQueue.
        mQueue.add(jsObjRequest);
    }

    private void searchControlPoints(JSONObject jsonObject) {
        try {
            queryControlPoints = new JSONObject();
            queryControlPoints.accumulate("routeControlPointCollection", new JSONArray());
            JSONArray array = jsonObject.getJSONArray("blackPoints");
            for (int i = 0; i < array.length(); i++) {
                JSONObject info = new JSONObject();
                double lat = array.getJSONObject(i).getJSONObject("location").getDouble("lat");
                double lng = array.getJSONObject(i).getJSONObject("location").getDouble("lon");
                String markerText = array.getJSONObject(i).getString("road") + ", km "
                        + array.getJSONObject(i).getString("km") + ", mortality: "
                        + array.getJSONObject(i).getString("rate");
                info.put("lat",lat);
                info.put("lng",lng);
                info.put("weight",100);
                info.put("radius",0.250);
                queryControlPoints.getJSONArray("routeControlPointCollection").put(info);
                markerLocation.add(new LatLng(lat, lng));
                markerName.add(markerText);
                markerVisited.add(false);
                // Print them as we just received them
                addBlueMarker(new LatLng(lat, lng), markerText);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    // Distance between two points in a sphere (in lat, lon) in km.
    public double dist(LatLng pos1, LatLng pos2) {
        double dLat = Math.toRadians(pos2.latitude - pos1.latitude);
        double dLon = Math.toRadians(pos2.longitude - pos1.longitude);
        double lat1 = Math.toRadians(pos1.latitude);
        double lat2 = Math.toRadians(pos2.latitude);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return (RADIUS * c);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    // Starts travel by enabling tracking
    private void startTravel() {
        mTracking = true;
    }

    /**
     * Checks for a close Mark to current position, and if it's the first
     * time that is closer than minDist, send a Yo
     */
    private void checkCloseMarks() {
        Location mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        LatLng ownPos = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(),
                mLastLocation.getLongitude()), 18));
        if (mLocation != null) {
            Log.v("Marks", "checking close marks");
            for (int i = 0; i < markerLocation.size(); ++i) {
                LatLng pos = markerLocation.get(i);
                // If the object is close, send a YO and mark as visited
                if (dist(ownPos, pos) <= mMarkMinDist && !markerVisited.get(i)) {
                    markerVisited.set(i, true);
                    String YoUrl = "http://37.187.81.177:8000/yo";

                    // Request a string response from the provided URL.
                    StringRequest stringRequest = new StringRequest(Request.Method.GET, YoUrl,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    // empty
                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                        }
                    });
// Add the request to the RequestQueue.
                    mQueue.add(stringRequest);

                }
            }
        }
    }


    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                if (mTracking) {
                    checkCloseMarks();
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };

        // Register the listener with the Location Manager to receive location updates
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            mMap.getUiSettings().setCompassEnabled(false);

            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * Draw the route from the JSON object in the map
     */
    private void drawJSONDirection(JSONObject direction) {
        Polyline line = mMap.addPolyline(new PolylineOptions()
                //.add(new LatLng(51.5, -0.1), new LatLng(40.7, -74.0))
                .width(5)
                .color(Color.RED));

        JSONArray jArray = null;
        ArrayList<LatLng> points = new ArrayList<LatLng>();

        try {
            jArray = direction.getJSONObject("route").getJSONObject("shape").
                    getJSONArray("shapePoints");
            if (jArray != null) {
                for (int i=0;i<jArray.length();i+=2){
                    points.add(new LatLng((double)jArray.get(i), (double)jArray.get(i+1)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Remove previous roads and marks
        mMap.clear();
        // Print map and all markers
        mMap.addPolyline(new PolylineOptions().addAll(points).width(5).color(Color.RED));

        for (int i = 0; i < markerLocation.size(); ++i) {
            addBlueMarker(markerLocation.get(i), markerName.get(i));
        }
        focusCameraOnPath(points);
    }

    /**
     * Zooms the camera so that the path fits in the screen
     * @param path set of points that form the path
     */
    private void focusCameraOnPath(ArrayList<LatLng> path) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : path) {
            builder.include(point);
        }

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        final int width = size.x;
        final int height = size.y;
        final int padding = 40;
        // avoid animateCamera if points is empty
        if (path.size() > 0) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(
                    builder.build(), width, height, padding));
        }
    }

    private void addBlueMarker(LatLng coord, String markerString) {
        if (markerString == null) markerString = "Blue Marker";
        mMap.addMarker(new MarkerOptions().position(coord).title(markerString).
                icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        // Connected to Google Play services!
        // The good stuff goes here.
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(),
                    mLastLocation.getLongitude()), 13));
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
        Log.v("LO", "WOLO :(");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // This callback is important for handling errors that
        // may occur while attempting to connect with Google.
        //
        // More about this in the next section.
        Log.v("LO", "WOLO :((((((((");
    }

}
