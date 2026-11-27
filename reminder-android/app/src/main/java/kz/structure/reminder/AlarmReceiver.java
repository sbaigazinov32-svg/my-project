package kz.structure.reminder;
import android.app.*;import android.content.*;import android.media.*;import android.net.Uri;import android.os.*;import android.provider.Settings;import android.view.*;import android.widget.*;import android.graphics.PixelFormat;import android.graphics.drawable.GradientDrawable;
public class AlarmReceiver extends BroadcastReceiver {
 public void onReceive(Context c,Intent i){
 Diagnostics.log(c,"Получен сигнал AlarmManager");
 int s=i.getIntExtra("stage",2),id=i.getIntExtra("id",0);
 String plan=i.getStringExtra("cloudPlan");
 if(plan!=null&&i.getBooleanExtra("snooze",false)){PendingAlarms.add(c,i);CloudSync.prefs(c).edit().remove("snooze:"+plan+":"+s).remove("snoozeMeeting:"+plan+":"+s).commit();}
 if(plan!=null&&!i.getBooleanExtra("snooze",false)){
 if(CloudSync.prefs(c).getLong("delivered:"+plan+":"+s,-1)==i.getLongExtra("meeting",0)){Diagnostics.log(c,"Повторный сигнал пропущен: "+id);return;}
 PendingAlarms.add(c,i);
 CloudSync.prefs(c).edit().putLong("delivered:"+plan+":"+s,i.getLongExtra("meeting",0)).commit();
 }
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
 nm.notify(id,n);if(plan!=null)CloudSync.prefs(c).edit().remove("offlineAlarm:"+id).commit();Diagnostics.log(c,"Уведомление передано Android");
 if(c.getSystemService(AudioManager.class).getMode()==AudioManager.MODE_IN_COMMUNICATION){playDuringCall(c,s);}
 if(Settings.canDrawOverlays(c)&&c.getSystemService(PowerManager.class).isInteractive()&&!c.getSystemService(KeyguardManager.class).isKeyguardLocked()){try{c.startActivity(a);Diagnostics.log(c,"Запрошено открытие окна");}catch(Exception ex){Diagnostics.log(c,"Ошибка открытия окна: "+ex.getClass().getSimpleName());}}
 if(c.getSystemService(AudioManager.class).getMode()!=AudioManager.MODE_IN_COMMUNICATION&&Build.VERSION.SDK_INT>=26&&Settings.canDrawOverlays(c)&&c.getSystemService(PowerManager.class).isInteractive()&&!c.getSystemService(KeyguardManager.class).isKeyguardLocked()){
 final PendingResult result=goAsync(); final Handler h=new Handler(Looper.getMainLooper());
 h.post(()->{WindowManager wm=c.getSystemService(WindowManager.class);LinearLayout box=new LinearLayout(c);box.setOrientation(LinearLayout.VERTICAL);int pad=(int)(16*c.getResources().getDisplayMetrics().density);box.setPadding(pad,pad,pad,pad);
 GradientDrawable bg=new GradientDrawable();bg.setColor(mentor?0xff493322:0xff153653);bg.setCornerRadius(pad);bg.setStroke(2,mentor?0xffffd58a:0xff80c7ff);box.setBackground(bg);
 TextView t=new TextView(c);t.setText(title+"\n"+time+"\n"+detail);t.setTextColor(0xffffffff);t.setTextSize(18);box.addView(t);
 Button open=new Button(c);open.setText("Открыть напоминание");box.addView(open);Button close=new Button(c);close.setText("Скрыть");box.addView(close);
 final boolean[] done={false};Runnable remove=()->{if(done[0])return;done[0]=true;try{wm.removeView(box);}catch(Exception ignored){}result.finish();};
 open.setOnClickListener(v->{remove.run();try{p.send();}catch(PendingIntent.CanceledException ignored){}});close.setOnClickListener(v->remove.run());
 WindowManager.LayoutParams lp=new WindowManager.LayoutParams(-1,-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);lp.gravity=Gravity.TOP;lp.y=pad;
 try{wm.addView(box,lp);Diagnostics.log(c,"Резервное окно показано");h.postDelayed(remove,7000);}catch(Exception ex){Diagnostics.log(c,"Ошибка резервного окна: "+ex.getClass().getSimpleName());remove.run();}
 });
 }
 }
 private void playDuringCall(Context c,int stage){
 final AudioManager am=c.getSystemService(AudioManager.class);
 final Handler h=new Handler(Looper.getMainLooper());
 final PendingResult pending=goAsync();
 MediaPlayer player=null;
 AudioFocusRequest focus=null;
 try{
 AudioAttributes attrs=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
 focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(attrs).setOnAudioFocusChangeListener(change->{}).build();
 am.requestAudioFocus(focus);
 int raw=c.getResources().getIdentifier("signal"+stage,"raw",c.getPackageName());
 player=MediaPlayer.create(c,raw,attrs,0);
 if(player==null)throw new IllegalStateException("No alarm sound");
 final MediaPlayer mp=player;final AudioFocusRequest req=focus;final boolean[] finished={false};
 Runnable cleanup=()->{if(finished[0])return;finished[0]=true;try{mp.release();}finally{am.abandonAudioFocusRequest(req);pending.finish();}};
 mp.setOnCompletionListener(v->cleanup.run());mp.setOnErrorListener((v,w,e)->{cleanup.run();return true;});
 mp.setVolume(1f,1f);mp.start();h.postDelayed(cleanup,5000);
 }catch(Exception e){if(player!=null)player.release();if(focus!=null)am.abandonAudioFocusRequest(focus);pending.finish();}
 }
}
