package com.example.marcello.firstapp;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.marcello.firstapp.axmlrpc.XMLRPCCallback;
import com.example.marcello.firstapp.axmlrpc.XMLRPCClient;
import com.example.marcello.firstapp.axmlrpc.XMLRPCException;
import com.example.marcello.firstapp.axmlrpc.XMLRPCServerException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.Arrays;

public class ActivityViewFreq extends AppCompatActivity {
    NfcAdapter nfcAdapter;
    private Intent myIntent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_freq);
        myIntent = new Intent(this, MainActivity.class);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        final Button button = findViewById(R.id.done);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendListFreqServer("sendFreqList", MainActivity.jsonListFreq.toString());
            }
        });
    }


    @Override
    protected void onNewIntent(Intent intent) {
        Toast.makeText(this, "Nfc identificado", Toast.LENGTH_LONG).show();
        try {
            getTagInfo(intent);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        super.onNewIntent(intent);
    }

    private void getTagInfo(Intent intent) throws IOException, JSONException {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        byte[] idStudent = tag.getId();
        insertIdInArray(idStudent);
    }

    private void insertIdInArray(byte[] idStudent) throws JSONException {
        Bundle extras = getIntent().getExtras();
        String idClass = extras.getString("idClass");
        JSONArray arr = MainActivity.jsonListFreq.getJSONArray(idClass);
        if(arr.length() > 0 ) {
            for(int i = 0; i<arr.length(); i++) {
                byte[] arrItem = (byte[]) arr.get(i);
                boolean podeInserir = false;
                for (int j = 0; j < ((byte[]) arrItem).length; j++) {
                    if (arrItem[j] != idStudent[j]) podeInserir = true;
                }
                if (podeInserir) arr.put(idStudent);
            }
        } else arr.put(idStudent);
    }


    @Override
    protected void onResume(){

        Intent intent = new Intent(this, ActivityViewFreq.class);
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, intent, 0);
        IntentFilter[] intentFilter = new IntentFilter[] {};

        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilter, null);
        super.onResume();
    }
//
    @Override
    protected void onPause(){
        nfcAdapter.disableForegroundDispatch(this);
        super.onPause();
    }

    public void sendListFreqServer(String method, String params) {
        try {
            URL url = new URL("http://192.168.0.19:2000/");

            XMLRPCCallback listener = new XMLRPCCallback() {
                public void onResponse(long id, Object result) throws JSONException {
                    finishActivity(200);
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


}
