package kz.structure.reminder;
import android.app.job.*;import android.content.*;
public class BootReceiver extends BroadcastReceiver {
 public void onReceive(Context c,Intent i){
 if(!CloudSync.prefs(c).contains("session"))return;
 try{CloudSync.enable(c);c.getSystemService(JobScheduler.class).schedule(new JobInfo.Builder(732,new ComponentName(c,CloudSync.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setOverrideDeadline(60000L).build());}catch(Exception ignored){}
 }
}