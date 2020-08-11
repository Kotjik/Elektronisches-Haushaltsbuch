package com.example.e_haushaltsbuch.backend;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class AppDatabase {
  //database of the application
  private static final String DATABASE_NAME = "finances.db";
  private static final int DATABASE_VERSION = 1;
  private static final String TABLE_NAME = "financeItem";

  public static final String KEY_ID = "_id";
  public static final String KEY_NAME = "name";
  public static final String KEY_VALUE = "value";
  public static final String KEY_DATE = "date";
  public static final String KEY_NOTE = "note";
  public static final String KEY_CATEGORY = "category";
  public static final String KEY_IMAGE = "image";
  public static final String KEY_STATUS = "status";
  public static final String KEY_TIME = "time";

  public static final int COLUMN_NAME_INDEX = 1;
  public static final int COLUMN_VALUE_INDEX = 2;
  public static final int COLUMN_DATE_INDEX = 3;
  public static final int COLUMN_NOTE_INDEX = 4;
  public static final int COLUMN_CATEGORY_INDEX = 5;
  public static final int COLUMN_IMAGE_INDEX = 6;
  public static final int COLUMN_STATUS_INDEX = 7;
  public static final int COLUMN_TIME_INDEX = 8;

  private float sumExpenditure;
  private float sumRevenue;

  private SQLiteDatabase database;
  private DbOpenHelper dbHelper;

  public AppDatabase(Context context, String dbName) {
    dbHelper = new DbOpenHelper(context, dbName, null, DATABASE_VERSION);
  }

  /**opens the database.*/
  public void open() throws SQLException {
    try {
      database = dbHelper.getWritableDatabase();
    } catch (SQLException e) {
      database = dbHelper.getReadableDatabase();
    }
  }

  public void close() {
    database.close();
  }

  /**inserts a new item to the database.
   * each item in the database has a name, value, date, note, category, image, status and time */
  public long insertItem(FinanceItem item) {
    ContentValues entry = new ContentValues();
    entry.put(KEY_NAME, item.getName());
    entry.put(KEY_VALUE, item.getValue());
    entry.put(KEY_DATE, item.getDate());
    entry.put(KEY_NOTE, item.getNote());
    entry.put(KEY_CATEGORY, item.getCategory());
    entry.put(KEY_IMAGE, item.getImage());
    entry.put(KEY_STATUS, item.getStatus());
    entry.put(KEY_TIME, item.getTime());

    return database.insert(TABLE_NAME, null, entry);
  }

  /**removes finance item with a specific name.*/
  public void removeItem(FinanceItem item) {
    String[] deleteArguments = new String[] {
      item.getName(), item.getValue(), item.getDate(), item.getTime(), item.getNote(), item.getCategory(), item.getImage()
    };
    database.delete(TABLE_NAME, "name=? and value=? and date=? and time=? and note=? and category=? and image=?", deleteArguments);
  }

  /**returns all finance items which are saved in the database.*/
  public ArrayList<FinanceItem> getAllItems() {
    ArrayList<FinanceItem> items = new ArrayList<FinanceItem>();
    Cursor cursor = database.query(TABLE_NAME, new String[] {
        KEY_ID, KEY_NAME, KEY_VALUE, KEY_DATE, KEY_NOTE, KEY_CATEGORY, KEY_IMAGE, KEY_STATUS, KEY_TIME
    }, null, null, null, null, null);
    resetSum();
    if (cursor.moveToFirst()) {
      do {
        String name = cursor.getString(COLUMN_NAME_INDEX);
        String value = cursor.getString(COLUMN_VALUE_INDEX);
        String date = cursor.getString(COLUMN_DATE_INDEX);
        String note = cursor.getString(COLUMN_NOTE_INDEX);
        String category = cursor.getString(COLUMN_CATEGORY_INDEX);
        String image = cursor.getString(COLUMN_IMAGE_INDEX);
        boolean status = cursor.getInt(COLUMN_STATUS_INDEX) > 0;
        String time = cursor.getString(COLUMN_TIME_INDEX);
        items.add(new FinanceItem(name, value, date, note, category, image, status, time));

        sumUp(status, value);

      } while (cursor.moveToNext());
    }
    return items;
  }

  private void sumUp(boolean status, String value) {
    if (status) {
      sumRevenue += Float.valueOf(value);
    } else {
      sumExpenditure += Float.valueOf(value);
    }
  }

  private void resetSum() {
    sumExpenditure = 0;
    sumRevenue = 0;
  }

  private class DbOpenHelper extends SQLiteOpenHelper {

    private static final String DATABASE_CREATE = "create table "
          + TABLE_NAME + " (" + KEY_ID + " integer primary key autoincrement, " + KEY_NAME + " text not null, "
            + KEY_VALUE + " text not null, " + KEY_DATE + " text not null, " + KEY_NOTE + " text not null, "
            + KEY_CATEGORY + " text not null, " + KEY_IMAGE + " text not null, " + KEY_STATUS + " integer not null, "
            + KEY_TIME + " text not null);";

    public DbOpenHelper(Context c, String dbname, SQLiteDatabase.CursorFactory factory, int version) {
      super(c, dbname, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
  }
}