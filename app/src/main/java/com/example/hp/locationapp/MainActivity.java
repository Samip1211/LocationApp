package com.example.hp.locationapp;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;


public class MainActivity extends Activity  {
    private static final String AUTHORITY = "com.example.hp.locationapp.provider";
    private final String ACCOUNT_TYPE = "locapp.io";
    private final String ACCOUNT = "LocApp";
    private static Account mAccount;
    private final long SECONDS_PER_MINUTE = 50L;
    private final long SYNC_INTERVAL_IN_MINUTES = 1L;
    private final long SYNC_INTERVAL = SYNC_INTERVAL_IN_MINUTES * SECONDS_PER_MINUTE;
    private static Bundle settingsBundle;
    private SharedPreferences preferences;

    Button btnShowLocation;

    EditText editText;

    GPSTracker gps;

    private DbHelper mydb;

    int count;

    TextView textView;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Firebase.setAndroidContext(this);
        mAccount = CreateSyncAccount(this);
        preferences = getSharedPreferences("location_app", MODE_PRIVATE);

        WifiManager wifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        final WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        final String name = wifiInfo.getBSSID();
        String address = wifiInfo.getMacAddress();
        int netId = wifiInfo.getNetworkId();
        int ip=wifiInfo.getIpAddress();

        System.out.println("SSId"+wifiInfo.getSSID()+"  "+"Bssid"+wifiInfo.getBSSID()+" "+"netId"+wifiInfo.getNetworkId()+" "+wifiInfo.getLinkSpeed());
        long epoch = System.currentTimeMillis();

        btnShowLocation = (Button) findViewById(R.id.sendLocation);

        editText =(EditText) findViewById(R.id.Pid);

        textView =(TextView) findViewById(R.id.textView);


        mydb = new DbHelper(this);
        final Firebase myfirebaseref = new Firebase("https://locationappnirma.firebaseio.com/");
        settingsBundle = new Bundle();
        try {
            ContentResolver.requestSync(mAccount, AUTHORITY, settingsBundle);
            ContentResolver.setSyncAutomatically(mAccount, AUTHORITY, true);
            ContentResolver.addPeriodicSync(
                    mAccount,
                    AUTHORITY,
                    Bundle.EMPTY,
                    SYNC_INTERVAL);
            Log.e("sync", "requested");
        } catch (Exception e) {
            Log.e("sync", e.getMessage());
        }

        gps = new GPSTracker(MainActivity.this);

        btnShowLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user=editText.getText().toString();
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("user", user);
                editor.apply();

                if(user.isEmpty()){
                    Toast.makeText(getApplicationContext(),"Enter User",Toast.LENGTH_LONG).show();
                }
                if (gps.canGetLocation()) {

                    double latitude = gps.getLatitude();
                    double longitude = gps.getLongitude();

                    System.out.println("Starting to run");
                    Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();


                    //String pid = editText.getText().toString();

                    myfirebaseref.child(name).child(user).child("Latitude").setValue(latitude);
                    myfirebaseref.child(name).child(user).child("Longitude").setValue(longitude);

                    myfirebaseref.child("User").addChildEventListener(new ChildEventListener() {
                                                                          @Override
                                                                          public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                                                                              String user2 = (String) dataSnapshot.getValue();

                                                                              System.out.println(user2);

                                                                              try {

                                                                                  mydb.addNode(user2);

                                                                                  System.out.println("Added");

                                                                              } catch (Exception e) {

                                                                                  e.printStackTrace();
                                                                              }

                                                                              Cursor cursor = mydb.getNode();

                                                                              if (cursor.moveToFirst()) {

                                                                                  do {

                                                                                      String usernode = cursor.getString(cursor.getColumnIndex("USER_NODE"));

                                                                                      System.out.println(usernode);

                                                                                      myfirebaseref.child(name).child(usernode).addValueEventListener(new ValueEventListener() {
                                                                                          @Override
                                                                                          public void onDataChange(DataSnapshot dataSnapshot) {

                                                                                              System.out.println(dataSnapshot.child("Latitude").getValue());

                                                                                              if(!dataSnapshot.child("Latitude").getValue().toString().isEmpty()){
                                                                                                  count++;
                                                                                              }

                                                                                              textView.setText("Number of host present are" + count);

                                                                                          }

                                                                                          @Override
                                                                                          public void onCancelled(FirebaseError firebaseError) {

                                                                                          }
                                                                                      });
                                                                                  }
                                                                                  while (cursor.moveToNext());
                                                                              }

                                                                          }

                                                                          @Override
                                                                          public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                                                                              String user2 = (String) dataSnapshot.getValue();

                                                                              System.out.println("Changed " + user2);
                                                                          }

                                                                          @Override
                                                                          public void onChildRemoved(DataSnapshot dataSnapshot) {

                                                                          }

                                                                          @Override
                                                                          public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                                                                          }

                                                                          @Override
                                                                          public void onCancelled(FirebaseError firebaseError) {

                                                                          }
                                                                      }
                    );


                } else {

                    gps.showSettingsAlert();
                }


            }
        });


    }

    private Account CreateSyncAccount(Context context) {
        Account newAccount = new Account(ACCOUNT, ACCOUNT_TYPE);

        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        if (accountManager.addAccountExplicitly(newAccount, null, null)) {
            Log.e("account", "created");
        } else
            Log.e("account", "exists");

        return newAccount;
    }


}




