package kz.structure.reminder;
import android.app.*;import android.content.*;import android.media.*;import android.net.Uri;import android.os.*;import android.provider.Settings;import android.view.*;import android.widget.*;import android.graphics.PixelFormat;import android.graphics.drawable.GradientDrawable;
public class AlarmReceiver extends BroadcastReceiver {
 public void onReceive(Context c,Intent i){
 int s=i.getIntExtra("stage",2),id=i.getIntExtra("id",0);
 String channel="alarm_v3_"+s;
 NotificationManager nm=c.getSystemService(NotificationManager.class);
 NotificationChannel ch=new NotificationChannel(channel,s==6?"За 6 часов":"За 2 часа",NotificationManager.IMPORTANCE_HIGH);
 ch.setSound(Uri.parse("android.resource://"+c.getPackageName()+"/raw/signal"+s),new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());ch.enableVibration(true);ch.setVibrationPattern(s==6?new long[]{0,280,270,280}:new long[]{0,280,270,280,270,280});nm.createNotificationChannel(ch);
 Intent a=new Intent(c,MainActivity.class).putExtras(i).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
 PendingIntent p=PendingIntent.getActivity(c,id,a,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 String time=new java.text.SimpleDateFormat("HH:mm · dd.MM.yyyy",java.util.Locale.getDefault()).format(new java.util.Date(i.getLongExtra("meeting",0)));
 String type=i.getStringExtra("meetingType"),guest=i.getStringExtra("guests");
 String detail=(type==null?"Встреча":type)+" · "+(guest==null?"":guest);
 boolean mentor=i.getBooleanExtra("mentor",false);
 String title=mentor?"Напоминание партнёру":"Моё напоминание";
 Notification n=new Notification.Builder(c,channel).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(title+" · "+time).setContentText(detail).setStyle(new Notification.BigTextStyle().bigText(detail)).setVisibility(Notification.VISIBILITY_PRIVATE).setCategory(Notification.CATEGORY_ALARM).setPriority(Notification.PRIORITY_MAX).setContentIntent(p).setFullScreenIntent(p,true).setAutoCancel(true).build();
 nm.notify(id,n);
 if(Build.VERSION.SDK_INT>=26&&Settings.canDrawOverlays(c)&&c.getSystemService(PowerManager.class).isInteractive()&&!c.getSystemService(KeyguardManager.class).isKeyguardLocked()){
 final PendingResult result=goAsync(); final Handler h=new Handler(Looper.getMainLooper());
 h.post(()->{WindowManager wm=c.getSystemService(WindowManager.class);LinearLayout box=new LinearLayout(c);box.setOrientation(LinearLayout.VERTICAL);int pad=(int)(16*c.getResources().getDisplayMetrics().density);box.setPadding(pad,pad,pad,pad);
 GradientDrawable bg=new GradientDrawable();bg.setColor(mentor?0xff493322:0xff153653);bg.setCornerRadius(pad);bg.setStroke(2,mentor?0xffffd58a:0xff80c7ff);box.setBackground(bg);
 TextView t=new TextView(c);t.setText(title+"\n"+time+"\n"+detail);t.setTextColor(0xffffffff);t.setTextSize(18);box.addView(t);
 Button open=new Button(c);open.setText("Открыть напоминание");box.addView(open);Button close=new Button(c);close.setText("Скрыть");box.addView(close);
 final boolean[] done={false};Runnable remove=()->{if(done[0])return;done[0]=true;try{wm.removeView(box);}catch(Exception ignored){}result.finish();};
 open.setOnClickListener(v->{remove.run();try{p.send();}catch(PendingIntent.CanceledException ignored){}});close.setOnClickListener(v->remove.run());
 WindowManager.LayoutParams lp=new WindowManager.LayoutParams(-1,-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);lp.gravity=Gravity.TOP;lp.y=pad;
 try{wm.addView(box,lp);h.postDelayed(remove,7000);}catch(Exception ex){remove.run();}
 });
 }
 }
}
