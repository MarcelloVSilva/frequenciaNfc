package com.example.marcello.firstapp;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.marcello.firstapp.axmlrpc.XMLRPCCallback;
import com.example.marcello.firstapp.axmlrpc.XMLRPCClient;
import com.example.marcello.firstapp.axmlrpc.XMLRPCException;
import com.example.marcello.firstapp.axmlrpc.XMLRPCServerException;
import com.snatik.polygon.Point;
import com.snatik.polygon.Polygon;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

public class MainActivity extends Activity {
//    SQLiteDatabase myDB;

    LocationManager locationManager;
    double[] profPos = new double[2];
    private Object teacher = null;
    public static JSONObject jsonListFreq;
    private Intent myIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myIntent = new Intent(this, ActivityViewFreq.class);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        setContentView(R.layout.activity_main);

        connectRpcServerCb("getInfoTeacher", "marcelloId");

        getLocationDeviceProf();

        final Button button = findViewById(R.id.aulas);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    createBtn();
                    button.setVisibility(View.GONE);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }


    public boolean profIsInTheClassroom(JSONArray polygonSala) throws JSONException {
        if(polygonSala.length() > 0) {
            Polygon polygon = Polygon.Builder().addVertex(new Point((Double) polygonSala.getJSONArray(0).get(0), (Double) polygonSala.getJSONArray(0).get(1))).addVertex(new Point((Double) polygonSala.getJSONArray(1).get(0), (Double) polygonSala.getJSONArray(1).get(1))).addVertex(new Point((Double) polygonSala.getJSONArray(2).get(0), (Double) polygonSala.getJSONArray(2).get(1))).addVertex(new Point((Double) polygonSala.getJSONArray(3).get(0), (Double) polygonSala.getJSONArray(3).get(1))).build();

            double lat = profPos[0];
            double lon = profPos[1];

            Point point = new Point(lat, lon);
            boolean contains = polygon.contains(point);
            return contains;
        } else return true;
    }

    public class SynchronizedObjectTeacher {
        private Object _objTeacher = null;
        public void set(Object objTeacher) throws JSONException {
            _objTeacher = objTeacher;
        }
        public Object get() {
            return _objTeacher;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        getLocation();
    }

    private void getLocationDeviceProf(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    3);
        else getLocation();
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            String provider = LocationManager.PASSIVE_PROVIDER;
            Location location = locationManager.getLastKnownLocation(provider);
            updateWithNewLocation(location);
        }
    }

    private void updateWithNewLocation(Location location) {
        if (location != null) {
            double lat = location.getLatitude();
            double lon = location.getLongitude();
            profPos[0] = lat;
            profPos[1] = lon;
        } else Toast.makeText(this, "locations null", Toast.LENGTH_SHORT).show();

    }

    public void connectRpcServerCb(String method, String params) {
        try {
            URL url = new URL("http://192.168.0.19:2000/"); //192.168.6.72 - inf / 192.168.15.24 - trabalho / 192.168.0.19 - casa

            XMLRPCCallback listener = new XMLRPCCallback() {
                public void onResponse(long id, Object result) {
                    if(result!=null)
                        teacher = result;
                }

                public void onError(long id, XMLRPCException error) {

                    error.printStackTrace();
                }

                public void onServerError(long id, XMLRPCServerException error) {
                    error.printStackTrace();
                }
            };

            XMLRPCClient client = new XMLRPCClient(url);
            long id = client.callAsync(listener, method, params);

        } catch(Exception ex) {
            // Any other exception
        }
    }

    private void createBtn() throws JSONException {
        JSONObject jsonteacher = new JSONObject((String) teacher);
        JSONArray classrooms = jsonteacher.getJSONArray("classrooms");
        final LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayout);

        while (classrooms.length()>0) {
            JSONObject item = (JSONObject) classrooms.get(classrooms.length() - 1);
            classrooms.remove(classrooms.length() - 1);
            final Button btn = new Button(this);
            layout.addView(btn);
            btn.setText(item.getString("description"));
            btn.setTag(item.getString("id"));
            btn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String idClass = (String) btn.getTag();
                    JSONArray polygonSala = new JSONArray();
                    try {
                        JSONObject jsonteacher = new JSONObject((String) teacher);
                        JSONArray classrooms = jsonteacher.getJSONArray("classrooms");
                        boolean x = true;
                        int cont = classrooms.length()-1;
                        while(x){
                            if(btn.getTag().equals(classrooms.getJSONObject(cont).getString("id"))){
                                polygonSala = classrooms.getJSONObject(cont).getJSONArray("polygon");
                                x=false;
                            } else cont--;
                        }
                        if(profIsInTheClassroom(polygonSala)){
                            jsonListFreq = new JSONObject("{"+idClass+":[]}");
                            myIntent.putExtra("idClass",idClass);
                            startActivityForResult(myIntent, '0');
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

    }

}
