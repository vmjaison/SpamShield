package com.example.spamshield;

import android.content.Context;
import java.io.*;
import java.net.*;
import java.util.*;

/** Downloads the Unlicense-licensed community CSV. Entries are allegations, not verified facts. */
public final class IndiaSpamSync {
 public static final String SOURCE="https://raw.githubusercontent.com/makash/scam-calls-india/main/spam-calls.csv";
 private IndiaSpamSync(){}
 public static int sync(Context context) throws Exception {
  HttpURLConnection connection=(HttpURLConnection)new URL(SOURCE).openConnection();
  connection.setConnectTimeout(12000);connection.setReadTimeout(20000);
  connection.setRequestProperty("Accept","text/csv,text/plain");
  Set<String> numbers=new HashSet<>();
  try {
   if(connection.getResponseCode()!=200)throw new IOException("Indian feed HTTP "+connection.getResponseCode());
   try(BufferedReader reader=new BufferedReader(new InputStreamReader(new LimitedInputStream(connection.getInputStream(),2*1024*1024),"UTF-8"))){
    String header=reader.readLine();
    if(header==null || !header.toLowerCase(Locale.ROOT).contains("phone_number") || !header.toLowerCase(Locale.ROOT).contains("country_code"))throw new IOException("Unexpected Indian CSV format");
    String[] fields=header.replace("\uFEFF","").split(",",-1);int phone=-1,country=-1;
    for(int i=0;i<fields.length;i++){String f=fields[i].trim();if(f.equals("phone_number"))phone=i;if(f.equals("country_code"))country=i;}
    if(phone<0||country<0)throw new IOException("Missing CSV columns");
    String line;int rows=0;
    while((line=reader.readLine())!=null){if(++rows>100000)throw new IOException("Too many records");String[] cols=line.split(",",-1);
     if(cols.length<=Math.max(phone,country))continue;
     String prefix=cols[country].trim().replaceAll("[^0-9]","");
     String local=cols[phone].trim().replaceAll("[^0-9]","");
     // Only Indian 10-digit mobile numbers, not international or malformed rows.
     if(prefix.equals("91") && local.matches("[6-9][0-9]{9}"))numbers.add("+91"+local);
    }
   }
   if(numbers.isEmpty())throw new IOException("No valid Indian numbers; previous data kept");
   try(SpamDb db=new SpamDb(context)){return db.replaceIndia(numbers);}
  }finally{connection.disconnect();}
 }
 static class LimitedInputStream extends FilterInputStream {
  int remaining;LimitedInputStream(InputStream in,int max){super(in);remaining=max;}
  @Override public int read()throws IOException{if(remaining<=0)throw new IOException("Feed too large");int n=super.read();if(n>=0)remaining--;return n;}
  @Override public int read(byte[] b,int off,int len)throws IOException{if(remaining<=0)throw new IOException("Feed too large");int n=super.read(b,off,Math.min(len,remaining));if(n>0)remaining-=n;return n;}
 }
}
