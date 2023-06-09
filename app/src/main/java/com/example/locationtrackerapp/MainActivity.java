package com.example.locationtrackerapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {
    //initializing variables
    RecyclerView recyclerView;
    ArrayList<ContactModel> arrayList = new ArrayList<>();
    MainAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //assign variable
        recyclerView = findViewById(R.id.recycler_view);

        //check permission
        checkPermissions();
    }

    private void checkPermissions() {
        //check condition
        if(ContextCompat.checkSelfPermission(MainActivity.this,
                android.Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED){
            //when permission is not granted => request again
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.SEND_SMS}, 100);
            } else {
            getContactList();
        }
    }

    private void getContactList() {
        //Initialize Uri
        Uri uri = ContactsContract.Contacts.CONTENT_URI;
        //Sort by alphabet
        String sort = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC";
        //Initialize cursor
        Cursor cursor = getContentResolver().query(
                uri, null, null, null, sort
        );
        //Check condition
        if (cursor.getCount() > 0){
            //when count is greater than 0 => use while loop
            while (cursor.moveToNext()){
                //cursor move to next => get contact id
                @SuppressLint("Range") String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                //get contact name
                @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                //initialize phone uri
                Uri uriPhone = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
                //intializ selection
                String selection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID+" =?";
                //intialize phone cursor
                Cursor phoneCursor = getContentResolver().query(uriPhone, null, selection, new String[]{id}, null);
                //check condition
                if (phoneCursor.moveToNext()){
                    //when phone cursor move to next
                    @SuppressLint("Range") String number = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    //initialize contact model
                    ContactModel model = new ContactModel();
                    //Set name & number
                    model.setName(name);
                    model.setNumber(number);
                    //add model in array list
                    arrayList.add(model);
                    //Close phone curso
                    phoneCursor.close();
                }
            }
            //close cursor
            cursor.close();
        }
        //set layout manager
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        //intialize adapter
        adapter = new MainAdapter(this, arrayList);
        //set adapter
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //check condition
        if(requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            //when permission is granted => call method
            getContactList();
        } else{
            //else => Display toast
            Toast.makeText(MainActivity.this, "Permossion Denied.", Toast.LENGTH_SHORT).show();
            //call check permission method
            checkPermissions();
        }
    }
}
