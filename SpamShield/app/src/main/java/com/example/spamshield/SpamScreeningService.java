package com.example.spamshield;

import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.net.Uri;

public class SpamScreeningService extends CallScreeningService {
 @Override public void onScreenCall(Call.Details details) {
  Uri handle=details.getHandle();
  String number=handle==null?"":handle.getSchemeSpecificPart();
  boolean blocked=false;
  if(number!=null && !number.isEmpty()) {
   try(SpamDb db=new SpamDb(this)) {
    blocked=db.isBlocked(number);
    if(blocked) db.logBlocked(number);
   } catch(Exception ignored) { blocked=false; }
  }
  CallResponse response=new CallResponse.Builder()
    .setDisallowCall(blocked).setRejectCall(blocked)
    .setSkipCallLog(false).setSkipNotification(blocked).build();
  respondToCall(details,response);
 }
}
