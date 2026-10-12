package com.example.spamshield;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.telephony.PhoneNumberUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SpamDb extends SQLiteOpenHelper {
 public SpamDb(Context ctx) { super(ctx,"spamshield.db",null,4); context=ctx.getApplicationContext(); }
 @Override public void onCreate(SQLiteDatabase db) {
  db.execSQL("CREATE TABLE blocked(number TEXT PRIMARY KEY, created_at INTEGER NOT NULL)");
  db.execSQL("CREATE TABLE events(id INTEGER PRIMARY KEY AUTOINCREMENT,number TEXT NOT NULL,created_at INTEGER NOT NULL)");
  db.execSQL("CREATE TABLE reputation(number TEXT PRIMARY KEY)");
  db.execSQL("CREATE TABLE trusted(number TEXT PRIMARY KEY)");
  db.execSQL("CREATE TABLE india_reputation(number TEXT PRIMARY KEY)");
  db.execSQL("CREATE TABLE imported_india(number TEXT PRIMARY KEY)");
 }
 @Override public void onUpgrade(SQLiteDatabase db,int oldV,int newV) {
  if(oldV<2) {db.execSQL("CREATE TABLE reputation(number TEXT PRIMARY KEY)");db.execSQL("CREATE TABLE trusted(number TEXT PRIMARY KEY)");}
  if(oldV<3)db.execSQL("CREATE TABLE india_reputation(number TEXT PRIMARY KEY)");
  if(oldV<4)db.execSQL("CREATE TABLE imported_india(number TEXT PRIMARY KEY)");
 }
 public static String normalize(String s) {return PhoneNumberUtils.normalizeNumber(s==null?"":s);}
 public void block(String number) {String n=normalize(number);if(n.length()<7)throw new IllegalArgumentException("Enter a valid number");ContentValues v=new ContentValues();v.put("number",n);v.put("created_at",System.currentTimeMillis());getWritableDatabase().insertWithOnConflict("blocked",null,v,SQLiteDatabase.CONFLICT_IGNORE);}
 public void unblock(String number){getWritableDatabase().delete("blocked","number=?",new String[]{normalize(number)});}
 public void trust(String number){String n=normalize(number);if(n.length()<7)throw new IllegalArgumentException("Enter a valid number");ContentValues v=new ContentValues();v.put("number",n);getWritableDatabase().insertWithOnConflict("trusted",null,v,SQLiteDatabase.CONFLICT_IGNORE);}
 public void untrust(String number){getWritableDatabase().delete("trusted","number=?",new String[]{normalize(number)});}
 private boolean exists(String table,String n){try(Cursor c=getReadableDatabase().rawQuery("SELECT 1 FROM "+table+" WHERE number=? LIMIT 1",new String[]{n})){return c.moveToFirst();}}
 public boolean isBlocked(String number){String n=normalize(number);boolean indiaEnabled=indiaAutoBlockEnabled();return exists("blocked",n)||(!exists("trusted",n)&&(exists("reputation",n)||(indiaEnabled&&exists("india_reputation",toIndiaE164(n)))||(importedAutoBlockEnabled()&&exists("imported_india",toIndiaE164(n)))));}
 public boolean importedAutoBlockEnabled(){return preferences().getBoolean("imported_auto_block",false);}
 public void setImportedAutoBlockEnabled(boolean enabled){preferences().edit().putBoolean("imported_auto_block",enabled).apply();}
 public int replaceImportedIndia(Set<String> numbers){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{db.execSQL("DELETE FROM imported_india");android.database.sqlite.SQLiteStatement stmt=db.compileStatement("INSERT OR IGNORE INTO imported_india(number) VALUES(?)");for(String n:numbers){stmt.clearBindings();stmt.bindString(1,n);stmt.executeInsert();}db.setTransactionSuccessful();}finally{db.endTransaction();}return numbers.size();}
 public boolean indiaAutoBlockEnabled(){return preferences().getBoolean("india_auto_block",false);}
 public void setIndiaAutoBlockEnabled(boolean enabled){preferences().edit().putBoolean("india_auto_block",enabled).apply();}
 private android.content.SharedPreferences preferences(){return context.getSharedPreferences("settings",Context.MODE_PRIVATE);}
 private Context context;
 private static String toIndiaE164(String n){if(n.matches("[6-9][0-9]{9}"))return "+91"+n;if(n.matches("91[6-9][0-9]{9}"))return "+"+n;return n;}
 public boolean inIndia(String n){return exists("india_reputation",toIndiaE164(normalize(n)));}
 public int replaceIndia(Set<String> numbers){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{db.execSQL("DELETE FROM india_reputation");android.database.sqlite.SQLiteStatement stmt=db.compileStatement("INSERT OR IGNORE INTO india_reputation(number) VALUES(?)");for(String n:numbers){stmt.clearBindings();stmt.bindString(1,n);stmt.executeInsert();}db.setTransactionSuccessful();}finally{db.endTransaction();}return numbers.size();}
 public boolean inReputation(String number){return exists("reputation",normalize(number));}
 public void logBlocked(String number){ContentValues v=new ContentValues();v.put("number",normalize(number));v.put("created_at",System.currentTimeMillis());getWritableDatabase().insert("events",null,v);}
 public ArrayList<String> blocked(){ArrayList<String> r=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT number FROM blocked ORDER BY created_at DESC",null)){while(c.moveToNext())r.add(c.getString(0));}return r;}
 public int count(String table){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM "+table,null)){return c.moveToFirst()?c.getInt(0):0;}}
 public int eventCount(){return count("events");}
 public int replaceReputation(Set<String> numbers){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{db.execSQL("DELETE FROM reputation");android.database.sqlite.SQLiteStatement stmt=db.compileStatement("INSERT OR IGNORE INTO reputation(number) VALUES(?)");for(String n:numbers){stmt.clearBindings();stmt.bindString(1,n);stmt.executeInsert();}db.setTransactionSuccessful();}finally{db.endTransaction();}return numbers.size();}
}
