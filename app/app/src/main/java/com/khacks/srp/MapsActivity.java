package com.khacks.srp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
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
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
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

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private EditText mFromField;
    private EditText mToField;
    private Button mSend;

    private RequestQueue mQueue;
    private String mMapQuestUrl;

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;

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

        // Instantiate the RequestQueue.
        mQueue = Volley.newRequestQueue(this);
        final Activity mActivity = this;

        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                    Log.v("LO", mMapQuestUrl);
                    Toast.makeText(getApplicationContext(), "Calculating route", Toast.LENGTH_SHORT).show();
                    doGETRequest(mMapQuestUrl, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Context context = getApplicationContext();
                            int duration = Toast.LENGTH_SHORT;

                            Toast.makeText(context, response.toString(), duration).show();
                            drawJSONDirection(response);
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
                                Log.v("LO", error.getMessage());
                            }
                            else {
                                Log.v("LO", "WOLO :(");
                            }
                        }
                        else {
                            Log.v("LO", "WOLO :(((((");
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
                                Log.v("LO", error.getMessage());
                            }
                            else {
                                Log.v("LO", "WOLO :(");
                            }
                        }
                        else {
                            Log.v("LO", "WOLO :(((((");
                        }
                    }
                });
        // Add the request to the RequestQueue.
        mQueue.add(jsObjRequest);
    }

    private void searchControlPoints(JSONObject jsonObject) {
        try {
            JSONObject query = new JSONObject();
            JSONArray array = jsonObject.getJSONArray("blackPoints");
            for (int i = 0; i < array.length(); i++) {
                JSONObject info = new JSONObject();
                double lat = array.getJSONObject(i).getJSONObject("location").getDouble("lat");
                double lng = array.getJSONObject(i).getJSONObject("location").getDouble("lng");
                info.put("lat",lat);
                info.put("lng",lng);
                info.put("weight",100);
                info.put("radius",5);
                query.accumulate("routeControlPointCollection", info);
                addBlueMarker(new LatLng(lat, lng), "Point " + i);
            }
            doPOSTRequest(mMapQuestUrl, query, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Context context = getApplicationContext();
                    int duration = Toast.LENGTH_SHORT;

                    Toast.makeText(context, "CONTROL POINTS!", duration).show();

                    JSONArray array2 = new JSONArray();
                    try {
                        array2 = response.getJSONObject("route").getJSONArray("shapePoints");
                    }
                    catch (Exception e) {
                    }
                }
            });
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
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
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
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
    }

    /**
     * Draw the route from the JSON object in the map
     */
    private void drawJSONDirection(JSONObject direction) {
        Polyline line = mMap.addPolyline(new PolylineOptions()
                .add(new LatLng(51.5, -0.1), new LatLng(40.7, -74.0))
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
        mMap.addPolyline(new PolylineOptions().addAll(points).width(5).color(Color.RED));
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
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(
                builder.build(), width, height, padding));
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
        Log.v("LO", "WOLO");
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
