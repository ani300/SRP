package com.khacks.srp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.net.URLEncoder;

public class MapsActivity extends FragmentActivity {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private EditText mFromField;
    private EditText mToField;
    private Button mSend;

    private RequestQueue mQueue;
    private String initialRouteGuessUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();

        // Setup the layout elements
        mFromField = (EditText) findViewById(R.id.fromField);
        mToField = (EditText) findViewById(R.id.toField);
        mSend = (Button) findViewById(R.id.searchButton);
        mFromField.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP);
        mToField.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP);


        // Instantiate the RequestQueue.
        mQueue = Volley.newRequestQueue(this);

        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    initialRouteGuessUrl ="http://open.mapquestapi.com/directions/v2/route?key=Fmjtd%7Cluu8210ynq%2C8w%3Do5-94r504&ambiguities=ignore&avoidTimedConditions=false&outFormat=json&routeType=fastest&enhancedNarrative=false&shapeFormat=raw&generalize=0&locale=en_US&unit=m&from="+
                            URLEncoder.encode(mFromField.getText().toString(), "utf-8")+"&to="+
                            URLEncoder.encode(mToField.getText().toString(), "utf-8");
                }
                catch (Exception e) {
                }
                Log.v("LO", initialRouteGuessUrl);
                doGETRequest(initialRouteGuessUrl, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Context context = getApplicationContext();
                        int duration = Toast.LENGTH_SHORT;

                        Toast.makeText(context, response.toString(), duration).show();

                        //mTxtDisplay.setText("Response: " + response.toString());
                    }
                });
            }
        });


    }

    void doGETRequest(String url, Response.Listener<JSONObject> listener) {
        // Request a string response from the provided URL.
        Log.v("LO", "WOLO");
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, listener, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                        Log.v("LO", error.getMessage());
                    }
                });


        // Add the request to the RequestQueue.
        mQueue.add(jsObjRequest);
    }

    private void doPOSTRequest(String url, JSONObject query, Response.Listener<JSONObject> listener) {
        // Request a string response from the provided URL.
        Log.v("LO", "WOLO");
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.POST, url, query, listener, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                        Log.v("LO", error.getMessage());
                    }
                });
        // Add the request to the RequestQueue.
        mQueue.add(jsObjRequest);
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

            Polyline line = mMap.addPolyline(new PolylineOptions()
                    .add(new LatLng(51.5, -0.1), new LatLng(40.7, -74.0))
                    .width(5)
                    .color(Color.RED));
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }
}
