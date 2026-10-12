package com.example.spamshield;
import android.content.Context;
import android.net.Uri;
import java.io.*;
import java.util.*;
public final class IndiaFileImport {
 private IndiaFileImport(){}
 public static int load(Context ctx,Uri uri)throws Exception {
  Set<String> numbers=new HashSet<>();
  try(InputStream raw=ctx.getContentResolver().openInputStream(uri)){
   if(raw==null)throw new IOException("File unavailable");
   BufferedReader r=new BufferedReader(new InputStreamReader(new IndiaSpamSync.LimitedInputStream(raw,5*1024*1024),"UTF-8"));
   String first=r.readLine();if(first==null)throw new IOException("Empty file");
   first=first.replace("\uFEFF","").trim();
   String[] heads=first.toLowerCase(Locale.ROOT).split(",",-1);int column=-1;
   for(int i=0;i<heads.length;i++){String h=heads[i].trim().replace("\"","");if(h.equals("number")||h.equals("phone_number")||h.equals("phone")||h.equals("mobile"))column=i;}
   if(column<0)parse(first,numbers);
   String line;int rows=0;
   while((line=r.readLine())!=null){if(++rows>150000)throw new IOException("Too many rows");
    if(column>=0){String[] cols=line.split(",",-1);if(column<cols.length)parse(cols[column],numbers);}else parse(line,numbers);
   }
  }
  if(numbers.isEmpty())throw new IOException("No valid Indian mobile numbers found; old list preserved");
  try(SpamDb db=new SpamDb(ctx)){return db.replaceImportedIndia(numbers);}
 }
 private static void parse(String value,Set<String> numbers){
  String s=value.trim().replace("\"","").replaceAll("[\\s()\\-]","");
  if(s.matches("[6-9][0-9]{9}"))numbers.add("+91"+s);
  else if(s.matches("91[6-9][0-9]{9}"))numbers.add("+"+s);
  else if(s.matches("\\+91[6-9][0-9]{9}"))numbers.add(s);
 }
}
