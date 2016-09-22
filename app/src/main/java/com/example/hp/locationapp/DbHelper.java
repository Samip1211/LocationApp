package com.example.hp.locationapp;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME ="Mydb.db";

    public DbHelper(Context context ) {

        super(context,DATABASE_NAME, null,2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE NODES(USER_NODE VARCHAR PRIMARY KEY)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DELETE TABLE IF EXISTS NODES");
    }

    public void addNode(String user_node){
        ContentValues cv= new ContentValues(1);

        cv.put("USER_NODE", user_node);

        getWritableDatabase().insert("NODES", "USER_NODE", cv);
    }

    public Cursor getNode(){
        Cursor cursor=getReadableDatabase().rawQuery("SELECT * FROM NODES",null);
        return cursor;
    }
}
