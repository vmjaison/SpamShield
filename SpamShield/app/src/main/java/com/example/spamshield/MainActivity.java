package com.example.spamshield;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import java.util.ArrayList;

public class MainActivity extends Activity {
 static final int IMPORT_INDIA=90;
 SpamDb db; LinearLayout root, entries; TextView status, stats;
 @Override public void onCreate(Bundle b) { super.onCreate(b); db=new SpamDb(this); draw(); }
 TextView text(String s,int size) {TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.rgb(20,30,45));t.setPadding(0,12,0,12);return t;}
 void draw() {
  ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(1);root.setPadding(28,38,28,30);root.setBackgroundColor(Color.rgb(246,248,252));scroll.addView(root);setContentView(scroll);
  root.addView(text("🛡 SpamShield",28));root.addView(text("Offline call blocking · No account required",14));
  status=text("",16);root.addView(status);
  Button role=new Button(this);role.setText("Enable call screening");root.addView(role);
  role.setOnClickListener(v->{RoleManager rm=(RoleManager)getSystemService(ROLE_SERVICE);if(rm!=null && rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING),1);else Toast.makeText(this,"Call screening role unavailable",Toast.LENGTH_LONG).show();});
  stats=text("",16);root.addView(stats);root.addView(text("Block a phone number",20));
  EditText input=new EditText(this);input.setHint("e.g. +919876543210");input.setInputType(android.text.InputType.TYPE_CLASS_PHONE);root.addView(input);
  Button add=new Button(this);add.setText("Add to blocklist");root.addView(add);
  add.setOnClickListener(v->{try{db.block(input.getText().toString());input.setText("");refresh();}catch(Exception ex){Toast.makeText(this,ex.getMessage(),Toast.LENGTH_SHORT).show();}});
  root.addView(text("Community spam database",20));
  root.addView(text("Source: CallShield public spam list. Download only when you choose; no phone numbers are uploaded. Coverage may be limited in India.",13));
  Button sync=new Button(this);sync.setText("Download / update spam database");root.addView(sync);
  sync.setOnClickListener(v->{sync.setEnabled(false);sync.setText("Downloading...");new Thread(()->{try{int count=ReputationSync.sync(getApplicationContext());runOnUiThread(()->{sync.setEnabled(true);sync.setText("Download / update spam database");Toast.makeText(this,"Loaded "+count+" numbers",Toast.LENGTH_LONG).show();refresh();});}catch(Exception ex){runOnUiThread(()->{sync.setEnabled(true);sync.setText("Download / update spam database");Toast.makeText(this,"Update failed: "+ex.getMessage(),Toast.LENGTH_LONG).show();});}}).start();});
  root.addView(text("India community scam reports",20));
  root.addView(text("Source: makash/scam-calls-india (Unlicense). This small public CSV contains unverified allegations. Automatic blocking is OFF by default to avoid rejecting legitimate callers.",13));
  Button indiaSync=new Button(this);indiaSync.setText("Download / update Indian list");root.addView(indiaSync);
  indiaSync.setOnClickListener(v->{indiaSync.setEnabled(false);indiaSync.setText("Downloading Indian list...");new Thread(()->{try{int count=IndiaSpamSync.sync(getApplicationContext());runOnUiThread(()->{indiaSync.setEnabled(true);indiaSync.setText("Download / update Indian list");Toast.makeText(this,"Imported "+count+" Indian numbers",Toast.LENGTH_LONG).show();refresh();});}catch(Exception ex){runOnUiThread(()->{indiaSync.setEnabled(true);indiaSync.setText("Download / update Indian list");Toast.makeText(this,"Indian update failed: "+ex.getMessage(),Toast.LENGTH_LONG).show();});}}).start();});
  CheckBox indiaAuto=new CheckBox(this);indiaAuto.setText("Automatically reject numbers on Indian community list (unverified)");indiaAuto.setChecked(db.indiaAutoBlockEnabled());root.addView(indiaAuto);
  indiaAuto.setOnCheckedChangeListener((buttonView,checked)->{db.setIndiaAutoBlockEnabled(checked);refresh();});
  root.addView(text("Import additional Indian spam datasets",20));
  root.addView(text("Choose a CSV or TXT file containing Indian mobile numbers. CSV must have a number, phone_number, phone or mobile column. The list is unverified, and automatic rejection is OFF by default.",13));
  Button importFile=new Button(this);importFile.setText("Import Indian CSV / TXT");root.addView(importFile);
  importFile.setOnClickListener(v->{Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);pick.addCategory(Intent.CATEGORY_OPENABLE);pick.setType("*/*");pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(pick,IMPORT_INDIA);});
  CheckBox importedAuto=new CheckBox(this);importedAuto.setText("Automatically reject numbers in imported Indian lists (unverified)");importedAuto.setChecked(db.importedAutoBlockEnabled());root.addView(importedAuto);
  importedAuto.setOnCheckedChangeListener((buttonView,checked)->{db.setImportedAutoBlockEnabled(checked);refresh();});
  root.addView(text("Trust a number (overrides community list)",20));
  EditText trustedInput=new EditText(this);trustedInput.setHint("+919876543210");trustedInput.setInputType(android.text.InputType.TYPE_CLASS_PHONE);root.addView(trustedInput);
  Button trust=new Button(this);trust.setText("Allow this number");root.addView(trust);trust.setOnClickListener(v->{try{db.trust(trustedInput.getText().toString());trustedInput.setText("");Toast.makeText(this,"Trusted number saved",Toast.LENGTH_SHORT).show();}catch(Exception ex){Toast.makeText(this,ex.getMessage(),Toast.LENGTH_SHORT).show();}});
  root.addView(text("Blocked numbers (tap Unblock to remove)",20));entries=new LinearLayout(this);entries.setOrientation(1);root.addView(entries);refresh();
 }
 void refresh() {
  RoleManager rm=(RoleManager)getSystemService(ROLE_SERVICE);
  boolean active=rm!=null && rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
  status.setText(active?"● Protection enabled":"● Protection disabled — enable call screening above");
  status.setTextColor(active?Color.rgb(22,125,70):Color.rgb(185,85,30));
  ArrayList<String> nums=db.blocked();stats.setText(nums.size()+" manually blocked  •  "+db.count("reputation")+" international entries  •  "+db.count("india_reputation")+" Indian feed entries  •  "+db.count("imported_india")+" imported Indian entries  •  "+db.eventCount()+" rejected calls");entries.removeAllViews();
  if(nums.isEmpty())entries.addView(text("No blocked numbers yet.",14));
  for(String num:nums){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);TextView label=text(num,15);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));Button remove=new Button(this);remove.setText("Unblock");remove.setOnClickListener(v->{db.unblock(num);refresh();});row.addView(remove);entries.addView(row);}
 }
 @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
  super.onActivityResult(requestCode,resultCode,data);
  if(requestCode==IMPORT_INDIA&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null){
   android.net.Uri uri=data.getData();new Thread(()->{try{int count=IndiaFileImport.load(getApplicationContext(),uri);runOnUiThread(()->{Toast.makeText(this,"Imported "+count+" Indian numbers",Toast.LENGTH_LONG).show();refresh();});}
   catch(Exception ex){runOnUiThread(()->Toast.makeText(this,"Import failed: "+ex.getMessage(),Toast.LENGTH_LONG).show());}}).start();
  }
 }
 @Override protected void onResume(){super.onResume();if(status!=null)refresh();}
 @Override protected void onDestroy(){db.close();super.onDestroy();}
}
