package com.example.locationtrackerapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.osmdroid.api.*;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.views.overlay.simplefastpoint.*;

import java.util.ArrayList;
import java.util.List;

public class Map extends AppCompatActivity {
    MapView map = null;
    IMapController mapController;
    List<IGeoPoint> people = new ArrayList<>();
    List<String> people_labels = new ArrayList<>();
    Paint textStyle = new Paint();
    MyLocationNewOverlay me = null;
    AlertDialog dialog;
    ArrayAdapter adapter;
    BroadcastReceiver broadcastReceiver;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_map);

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        mapController = map.getController();

        mapController.setZoom(7.0);
        // children locations
        textStyle.setTextSize(50);

        SimpleFastPointOverlayOptions options = SimpleFastPointOverlayOptions.getDefaultStyle().setTextStyle(textStyle);
        SimplePointTheme theme = new SimplePointTheme(people, true);

        map.getOverlays().add(new SimpleFastPointOverlay(theme, options));

        if (checkSelfPermission(android.Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ){
            requestPermissions(
                    new String [] {android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.RECEIVE_SMS},
                    200);
            return;
        }
        myLocation();
        addReceiver();
    }



    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(permissions, 200);
            return;
        }
        
        myLocation();
        addReceiver();
    }

    public void myLocation() {
        List<Overlay> mapOverlays = map.getOverlays();
        me = new MyLocationNewOverlay(map);
        me.enableMyLocation();

        Bitmap myIcon = BitmapFactory.decodeResource(getResources(), org.osmdroid.library.R.drawable.person);
        me.setDirectionArrow( myIcon, myIcon);

        mapOverlays.add(me);
        me.enableFollowLocation();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        menu.findItem(R.id.me).setVisible(true);
        menu.findItem(R.id.others).setVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        GeoPoint goTo;
        int itemId = item.getItemId();
        if (itemId == R.id.me) {

            goTo = me.getMyLocation();

            if (goTo == null) mapController.animateTo((IGeoPoint) me.getLastFix());
            else mapController.animateTo(goTo);
        } else if(itemId == R.id.others) {

            if (people_labels.size() == 0) {
                Toast.makeText(getBaseContext(),
                        "No locations to show, make sure the app is running in the background and has access to SmS",
                        Toast.LENGTH_SHORT).show();
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.READ_SMS}, 200);
                return true;
            } else dialog.show();
        }
        return super.onOptionsItemSelected(item);
    }

    void addReceiver() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
                    SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                    for (SmsMessage message : messages) {
                        if (me != null) me.disableFollowLocation();
                        String messageBody = message.getMessageBody();
                        String sender = message.getOriginatingAddress();

                        // sms format: geolocation\nAltitude\nLatitude
                        String[] messageBody_ = messageBody.split("\n");

                        if (messageBody_.length == 3 && messageBody_[0].equals("geolocation")) {
                            for(int i = 0; i < people_labels.size(); i++) {
                                if(people_labels.get(i).equals(sender)) {
                                    people.remove(i);
                                    people_labels.remove(i);
                                    adapter.notifyDataSetChanged();
                                    break;
                                }
                            }
                            people.add(new StyledLabelledGeoPoint(Float.parseFloat(messageBody_[1]),
                                    Float.parseFloat(messageBody_[2]), sender));
                            people_labels.add(sender);

                            mapController.animateTo(new GeoPoint(Float.parseFloat(messageBody_[1]),
                                    Float.parseFloat(messageBody_[2])));
                        }
                    }
                }
            }
        };
        
        try {
            registerReceiver(broadcastReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
        } catch (Exception e) {}
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Children");
        View locations = getLayoutInflater().inflate(R.layout.locations, null);
        ListView list = locations.findViewById(R.id.location);

        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, people_labels);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mapController.animateTo(people.get(i));
                dialog.dismiss();
            }
        });
        builder.setView(locations);
        dialog = builder.create();
    }
}
