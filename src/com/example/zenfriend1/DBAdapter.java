package com.example.zenfriend1;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class DBAdapter {
	public static final String KEY_ROWID = "id";
	public static final String KEY_CALENDAR_VALUE = "calendar_value";
	public static final String KEY_DATE = "the_date";
	private static final String TAG = "DBAdapter";
	
	private static final String DATABASE_NAME = "ZenFriend_DB";
	private static final String DATABASE_TITLE = "Calendar";
	
	private static final int DATABASE_VERSION = 3;
	
	private static final String DATABASE_CREATE = "create table if not exists Calendar (id integer primary key autoincrement, " + "calendar_value VARCHAR not null, the_date date);";
	
	private final Context context;
	
	private DatabaseHelper DBHelper;
	private SQLiteDatabase db;
	
	public DBAdapter(Context ctx) {
		this.context = ctx;
		DBHelper = new DatabaseHelper(context);
	}
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			try {
				db.execSQL(DATABASE_CREATE);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS CALENDAR");
			onCreate(db);
		}
	}
	
	/////OPENS DATABASE//////
	public DBAdapter open() throws SQLException {
		db = DBHelper.getWritableDatabase();
		return this;
	}
	
	/////CLOSES DATABASE/////
	public void close() {
		DBHelper.close();
	}
	
	/////INSERT A RECORD INTO DATABASE/////
	public long insertRecord(String calendar_value, String the_date) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_CALENDAR_VALUE, calendar_value);
		initialValues.put(KEY_DATE, the_date);
		return db.insert(DATABASE_TITLE, null, initialValues);
	}
	
	////DELETES A PARTICULAR RECORD////
	public boolean deleteRecord(long rowId) {
		return db.delete(DATABASE_TITLE, KEY_ROWID + "=" + rowId, null) > 0;
	}
	
	////RETRIEVES ALL THE RECORDS/////
	public Cursor getAllRecords() {
		return db.query(DATABASE_TITLE, new String[] {KEY_ROWID, KEY_CALENDAR_VALUE, KEY_DATE}, null, null, null, null, null);
	}
	
	////RETRIEVES A PARTICULAR RECORD/////
	public Cursor getRecord(long rowId) throws SQLException {
		Cursor mCursor = db.query(true, DATABASE_TITLE, new String[] {KEY_ROWID,  KEY_CALENDAR_VALUE,  KEY_DATE}, KEY_ROWID + "=" + rowId, null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}
	
	
}
