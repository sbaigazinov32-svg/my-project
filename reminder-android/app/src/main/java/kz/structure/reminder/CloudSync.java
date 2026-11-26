package kz.structure.reminder;
import android.app.*;import android.app.job.*;import android.content.*;import android.os.*;import java.util.*;import java.text.*;import org.json.*;
public class CloudSync extends JobService {
 static final Object LOCK=new Object(); Context ctx; final java.util.Set<JobParameters> active=java.util.Collections.synchronizedSet(new java.util.HashSet<>());
 static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("cloud_sync",0);}
 static javax.crypto.SecretKey key() throws Exception {java.security.KeyStore ks=java.security.KeyStore.getInstance("AndroidKeyStore");ks.load(null);if(!ks.containsAlias("cloud_session")){javax.crypto.KeyGenerator g=javax.crypto.KeyGenerator.getInstance("AES","AndroidKeyStore");g.init(new android.security.keystore.KeyGenParameterSpec.Builder("cloud_session",3).setBlockModes("GCM").setEncryptionPaddings("NoPadding").build());g.generateKey();}return (javax.crypto.SecretKey)ks.getKey("cloud_session",null);}
 static void save(Context c,JSONObject s)throws Exception {javax.crypto.Cipher x=javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");x.init(1,key());String v=android.util.Base64.encodeToString(x.getIV(),2)+":"+android.util.Base64.encodeToString(x.doFinal(s.toString().getBytes("UTF-8")),2);if(!prefs(c).edit().putString("session",v).commit())throw new java.io.IOException("Session storage");}
 static JSONObject load(Context c)throws Exception {String[] v=prefs(c).getString("session","").split(":");javax.crypto.Cipher x=javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");x.init(2,key(),new javax.crypto.spec.GCMParameterSpec(128,android.util.Base64.decode(v[0],2)));return new JSONObject(new String(x.doFinal(android.util.Base64.decode(v[1],2)),"UTF-8"));}
 static void requestSoon(Context c){if(!prefs(c).contains("session"))return;try{if(c.getSystemService(JobScheduler.class).getPendingJob(733)!=null)return;int result=c.getSystemService(JobScheduler.class).schedule(new JobInfo.Builder(733,new ComponentName(c,CloudSync.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).build());Diagnostics.log(c,"Запрошена внеочередная синхронизация: "+result);}catch(Exception e){Diagnostics.log(c,"Ошибка запуска синхронизации: "+e.getClass().getSimpleName());}}
 static void enable(Context c){int result=c.getSystemService(JobScheduler.class).schedule(new JobInfo.Builder(731,new ComponentName(c,CloudSync.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(15*60000L).setPersisted(true).build());if(result!=JobScheduler.RESULT_SUCCESS)throw new IllegalStateException("JobScheduler rejected job");}
 static String run(Context c,JSONObject login)throws Exception {synchronized(LOCK){Diagnostics.log(c,"Начало синхронизации");JSONObject s=login;if(s==null){s=load(c);if(s.optLong("expires_at",0)<=System.currentTimeMillis()/1000L+120)s=new JSONObject(cloudRequest("/auth/v1/token?grant_type=refresh_token",null,new JSONObject().put("refresh_token",s.getString("refresh_token"))));}try{save(c,s);}catch(Exception e){throw new java.io.IOException("Сохранение сессии: "+e.getClass().getSimpleName());}String background="";if(login!=null||c.getSystemService(JobScheduler.class).getPendingJob(731)==null){try{enable(c);}catch(Exception e){background="\nФоновая синхронизация не запущена: "+e.getClass().getSimpleName();}}String token=s.getString("access_token"),uid=s.getJSONObject("user").getString("id");if(!uid.matches("[a-fA-F0-9-]{36}"))throw new java.io.IOException("Invalid ID");String today=new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(new Date());JSONArray rows=new JSONArray(cloudRequest("/rest/v1/plans?select=id,action_type,planned_date,planned_time,cancelled_at,superseded_by_plan_id,guest_declined,contacts(full_name)&owner_id=eq."+uid+"&planned_date=gte."+today+"&completed=eq.false&order=planned_date.asc,planned_time.asc&limit=100",token,null));JSONArray profile=new JSONArray(cloudRequest("/rest/v1/profiles?select=full_name&id=eq."+uid,token,null));SchedulerEngine worker=new SchedulerEngine(c);String result=worker.syncPersonal(rows,profile.length()>0?profile.getJSONObject(0).optString("full_name"):"");prefs(c).edit().putLong("lastSync",System.currentTimeMillis()).apply();Diagnostics.log(c,"Синхронизация завершена, планов: "+rows.length());return result+background;}}
 public boolean onStartJob(JobParameters p){active.add(p);new Thread(()->{boolean retry=false;try{run(getApplicationContext(),null);}catch(Exception e){Diagnostics.log(getApplicationContext(),"Ошибка фоновой синхронизации: "+e.getClass().getSimpleName());retry=true;}if(active.remove(p))jobFinished(p,retry);}).start();return true;}
 public boolean onStopJob(JobParameters p){active.remove(p);return true;}
 static class Engine { final Context ctx; Engine(Context c){ctx=c;} 
 boolean ready(){return Build.VERSION.SDK_INT<31||ctx.getSystemService(AlarmManager.class).canScheduleExactAlarms();}
 String fmt(long t,String f){return new SimpleDateFormat(f,new Locale("ru","RU")).format(new Date(t));}
 }
 static String cloudRequest(String path,String token,org.json.JSONObject body)throws Exception{
  javax.net.ssl.HttpsURLConnection c=(javax.net.ssl.HttpsURLConnection)new java.net.URL("https://chgpyjguxzdrzfbergzu.supabase.co"+path).openConnection();
  try{c.setConnectTimeout(15000);c.setReadTimeout(15000);c.setInstanceFollowRedirects(false);c.setRequestProperty("apikey","sb_publishable_stOmxI6S5WzPYdfth_38SA_WLz8Kgwi");if(token!=null)c.setRequestProperty("Authorization","Bearer "+token);
  if(body!=null){c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(java.io.OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes("UTF-8"));}}
  int status=c.getResponseCode();if(status<200||status>=300)throw new java.io.IOException("HTTP "+status);
  try(java.io.InputStream in=c.getInputStream();java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){out.write(buf,0,n);if(out.size()>2000000)throw new java.io.IOException("Ответ слишком большой");}return out.toString("UTF-8");}
  }finally{c.disconnect();}
 }

 static class SchedulerEngine extends Engine { SchedulerEngine(Context c){super(c);} 
 boolean set(Intent i,long when){AlarmManager a=ctx.getSystemService(AlarmManager.class);if(Build.VERSION.SDK_INT>=31&&!a.canScheduleExactAlarms()){return false;}PendingIntent p=PendingIntent.getBroadcast(ctx,i.getIntExtra("id",0),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);PendingIntent show=PendingIntent.getActivity(ctx,i.getIntExtra("id",0),new Intent(ctx,MainActivity.class).putExtras(i),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);a.setAlarmClock(new AlarmManager.AlarmClockInfo(when,show),p);return true;}

 String syncPersonal(org.json.JSONArray rows,String name){
  if(!ready())throw new IllegalStateException("Разрешения не включены");
  android.content.SharedPreferences prefs=ctx.getSharedPreferences("cloud_sync",0);
  Set<String> old=new HashSet<>(prefs.getStringSet("cloudAlarmIds",Collections.emptySet()));
  Set<String> next=new HashSet<>();StringBuilder summary=new StringBuilder();int count=0, skipped=0; StringBuilder details=new StringBuilder();
  SimpleDateFormat parser=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.ROOT);parser.setLenient(false);
  long now=System.currentTimeMillis();long catchup=now+3000L;
  for(int j=0;j<rows.length();j++){try{
   org.json.JSONObject row=rows.getJSONObject(j);
   org.json.JSONObject contact=row.optJSONObject("contacts");String guest=contact==null?"":contact.optString("full_name");
   details.append("\n").append(guest).append(" | ").append(row.optString("action_type")).append(" | ").append(row.optString("planned_date")).append(" ").append(row.optString("planned_time")).append("\n");
   if(!row.isNull("cancelled_at")||!row.isNull("superseded_by_plan_id")||row.optBoolean("guest_declined")){details.append("Отменён, заменён или отказ гостя\n");skipped++;continue;}
   String type=row.getString("action_type"),plan=row.getString("id");
   long meeting=parser.parse(row.getString("planned_date")+" "+row.getString("planned_time").substring(0,8)).getTime();
   if(meeting<=now){details.append("Время встречи прошло\n");skipped++;continue;}
   int before=count;
   boolean call=type.toLowerCase(Locale.ROOT).contains("звон")||type.toLowerCase(Locale.ROOT).contains("сближен");
   for(int stage:new int[]{6,2}){
    if(stage==6&&call)continue;
    long when=meeting-(call?1800000L:stage*3600000L);
    Calendar cal=Calendar.getInstance();cal.setTimeInMillis(meeting);
    if(stage==6&&cal.get(Calendar.HOUR_OF_DAY)<14){cal.add(Calendar.DATE,-1);cal.set(Calendar.HOUR_OF_DAY,20);cal.set(Calendar.MINUTE,0);cal.set(Calendar.SECOND,0);cal.set(Calendar.MILLISECOND,0);when=cal.getTimeInMillis();}
    String sk=plan+":"+stage;long snooze=prefs.getLong("snooze:"+sk,0);boolean deferred=snooze>0&&prefs.getLong("snoozeMeeting:"+sk,-1)==meeting;
    if(!deferred&&prefs.getLong("delivered:"+plan+":"+stage,-1)==meeting){details.append("Сигнал уже доставлен\n");continue;}
    if(deferred){when=Math.max(snooze,catchup);if(snooze<=now)catchup+=12000L;}else if(when<=now){if(stage==6&&meeting-7200000L<=now)continue;long previous=prefs.getLong("catchup:"+sk+":"+meeting,0);when=previous>now?previous:catchup;catchup=Math.max(catchup,when+12000L);if(when>=meeting)continue;prefs.edit().putLong("catchup:"+sk+":"+meeting,when).apply();details.append("Запоздалый сигнал: восстановлен\n");}
    int alarmId=0x40000000|( (plan+":"+stage).hashCode() & 0x3fffffff);
    Intent i=new Intent(ctx,AlarmReceiver.class).putExtra("stage",stage).putExtra("meeting",meeting).putExtra("partner",name).putExtra("guests",guest).putExtra("id",alarmId).putExtra("meetingType",type).putExtra("cloudPlan",plan).putExtra("snooze",deferred);
    if(set(i,when)){if(prefs.getLong("scheduled:"+alarmId,-1)!=when)Diagnostics.log(ctx,"Назначен сигнал "+fmt(when,"dd.MM HH:mm:ss"));prefs.edit().putLong("scheduled:"+alarmId,when).apply();next.add(Integer.toString(alarmId));count++;details.append("Сигнал установлен: ").append(fmt(when,"dd.MM HH:mm")).append("\n");summary.append(fmt(when,"dd.MM HH:mm")).append(" — ").append(type).append(" · ").append(guest).append("\n");}else details.append("Ошибка установки сигнала\n");
   }
   if(count==before)skipped++;
  }catch(Exception ex){skipped++;details.append("Ошибка обработки плана: ").append(ex.getClass().getSimpleName()).append("\n");}}
  if(rows.length()<100){old.removeAll(next);for(String s:old){try{PendingIntent pi=PendingIntent.getBroadcast(ctx,Integer.parseInt(s),new Intent(ctx,AlarmReceiver.class),PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);if(pi!=null){ctx.getSystemService(AlarmManager.class).cancel(pi);pi.cancel();prefs.edit().remove("scheduled:"+s).apply();}}catch(Exception ignored){}}}else next.addAll(old);
  prefs.edit().putStringSet("cloudAlarmIds",next).apply();
  return "Установлено сигналов: "+count+"\n"+"Получено личных незавершённых планов (до 100): "+rows.length()+"\nПланов без сигналов: "+skipped+"\nВремя синхронизации: "+fmt(now,"dd.MM HH:mm:ss")+"\nЧасовой пояс: "+TimeZone.getDefault().getID()+"\n\nУстановленные сигналы:\n"+summary+"\nВсе полученные планы и причины пропуска:\n"+details;
 }

}
}