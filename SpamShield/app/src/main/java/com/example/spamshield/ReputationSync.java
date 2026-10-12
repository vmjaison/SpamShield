package com.example.spamshield;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;

/** User-triggered download, NEVER run on the call screening thread. */
public final class ReputationSync {
 public static final String SOURCE="https://raw.githubusercontent.com/SysAdminDoc/CallShield/master/data/spam_numbers.json";
 private static final int MAX_BYTES=128*1024*1024;
 private ReputationSync(){}
 public static int sync(Context ctx) throws Exception {
  URL url=new URL(SOURCE);HttpURLConnection c=(HttpURLConnection)url.openConnection();
  c.setConnectTimeout(12000);c.setReadTimeout(20000);c.setInstanceFollowRedirects(false);c.setRequestProperty("Accept","application/json");
  try {
   if(c.getResponseCode()!=200)throw new IOException("Feed HTTP "+c.getResponseCode());
   ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;
   try(InputStream in=c.getInputStream()){while((n=in.read(buf))!=-1){if(bytes.size()+n>MAX_BYTES)throw new IOException("Feed exceeds 32MB");bytes.write(buf,0,n);}}
   String json=bytes.toString("UTF-8");Object root=new JSONTokener(json).nextValue();Set<String> result=new HashSet<>();
   if(root instanceof JSONArray)parseArray((JSONArray)root,result);
   else if(root instanceof JSONObject){JSONObject o=(JSONObject)root;
    for(String key:new String[]{"numbers","spam_numbers","entries","data","records"}){Object value=o.opt(key);if(value instanceof JSONArray)parseArray((JSONArray)value,result);else if(value instanceof JSONObject)parseKeys((JSONObject)value,result);}
    // Legacy feeds may use phone numbers as object keys.
    if(result.isEmpty())parseKeys(o,result);
   }
   if(result.size()<100)throw new IOException("Feed format unrecognized or too few valid numbers; old database retained");
   try(SpamDb db=new SpamDb(ctx)){return db.replaceReputation(result);}
  }finally{c.disconnect();}
 }
 private static void parseKeys(JSONObject o,Set<String> result){Iterator<String> keys=o.keys();while(keys.hasNext())add(keys.next(),result);}
 private static void parseArray(JSONArray a,Set<String> result){for(int i=0;i<a.length();i++){Object v=a.opt(i);if(v instanceof String)add((String)v,result);else if(v instanceof JSONObject){JSONObject o=(JSONObject)v;for(String key:new String[]{"number","phone","phone_number","phoneNumber","e164"}){String s=o.optString(key,"");if(!s.isEmpty()){add(s,result);break;}}}}}
 private static void add(String raw,Set<String> result){String n=SpamDb.normalize(raw);if(n.matches("\\+?[0-9]{7,15}"))result.add(n);}
}
