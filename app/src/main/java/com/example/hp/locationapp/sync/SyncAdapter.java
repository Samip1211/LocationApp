package com.example.hp.locationapp.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.widget.Toast;

import com.example.hp.locationapp.DbHelper;
import com.example.hp.locationapp.GPSTracker;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private ContentResolver contentResolver;
    private Context syncContext;
    private SharedPreferences preferences;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        contentResolver = context.getContentResolver();
        syncContext = context;
        preferences = context.getSharedPreferences("location_app", Context.MODE_PRIVATE);
    }

    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        contentResolver = context.getContentResolver();
        syncContext = context;
        preferences = context.getSharedPreferences("location_app", Context.MODE_PRIVATE);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        GPSTracker gps = new GPSTracker(syncContext);
        WifiManager wifiMgr = (WifiManager) syncContext.getSystemService(Context.WIFI_SERVICE);
        final WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        final String name = wifiInfo.getBSSID();
        String address = wifiInfo.getMacAddress();
        int netId = wifiInfo.getNetworkId();
        int ip=wifiInfo.getIpAddress();
        System.out.println("SSId"+wifiInfo.getSSID()+"  "+"Bssid"+wifiInfo.getBSSID()+" "+"netId"+wifiInfo.getNetworkId()+" "+wifiInfo.getLinkSpeed());
        long epoch = System.currentTimeMillis();
        final DbHelper mydb = new DbHelper(syncContext);
        final Firebase myfirebaseref = new Firebase("https://locationappnirma.firebaseio.com/");

        if (gps.canGetLocation()) {

            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();

            System.out.println("Starting to run");


            //String pid = editText.getText().toString();

            myfirebaseref.child(name).child(preferences.getString("user","")).child("Latitude").setValue(latitude);
            myfirebaseref.child(name).child(preferences.getString("user","")).child("Longitude").setValue(longitude);

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

}
