package kz.structure.reminder;
import android.app.*;import android.os.*;import android.content.*;import android.provider.Settings;import android.net.Uri;import android.graphics.Color;import android.widget.*;import java.util.*;import java.text.*;
public class MainActivity extends Activity {
 final ArrayDeque<Intent> alarmQueue=new ArrayDeque<>();
 LinearLayout layout; LinearLayout actions; Intent visibleAlarm; EditText partner,guests; Calendar date=Calendar.getInstance(); TextView time;
 TextView label(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.WHITE);t.setPadding(8,12,8,12);layout.addView(t);return t;}
 void base(){ScrollView scroll=new ScrollView(this);layout=new LinearLayout(this);layout.setOrientation(1);layout.setPadding(24,48,24,32);layout.setBackgroundColor(0xff101820);scroll.addView(layout);setContentView(scroll);}
 void button(String s,Runnable r){Button b=new Button(this);b.setText(s);layout.addView(b);b.setOnClickListener(v->r.run());}
 EditText field(String hint){EditText e=new EditText(this);e.setHint(hint);layout.addView(e);return e;}
 String fmt(long t,String f){return new SimpleDateFormat(f,new Locale("ru","RU")).format(new Date(t));}
 public void onCreate(Bundle b){super.onCreate(b);Diagnostics.read(this);ArrayList<Intent> pending=PendingAlarms.list(this);if(!pending.isEmpty()){alarmQueue.addAll(pending);showAlarm(alarmQueue.removeFirst());return;}if(b!=null){ArrayList<Intent> q=b.getParcelableArrayList("alarmQueue");if(q!=null)for(Intent x:q)if(!x.hasExtra("cloudPlan"))alarmQueue.add(x);}if(b!=null&&b.getParcelable("visibleAlarm")!=null){Intent x=(Intent)b.getParcelable("visibleAlarm");if(!x.hasExtra("cloudPlan")){showAlarm(x);return;}}if(getIntent().hasExtra("stage")&&!getIntent().hasExtra("cloudPlan"))showAlarm(getIntent());else home();}
 protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);for(Intent x:PendingAlarms.list(this))enqueueAlarm(x);if(i.hasExtra("stage")&&!i.hasExtra("cloudPlan"))enqueueAlarm(i);else if(visibleAlarm==null)home();}
 boolean sameAlarm(Intent a,Intent b){if(a.hasExtra("cloudPlan")||b.hasExtra("cloudPlan"))return a.hasExtra("cloudPlan")&&b.hasExtra("cloudPlan")&&PendingAlarms.key(a).equals(PendingAlarms.key(b));return a.getIntExtra("id",0)==b.getIntExtra("id",0);}
 void enqueueAlarm(Intent i){if(visibleAlarm==null){showAlarm(i);return;}int id=i.getIntExtra("id",0);if(sameAlarm(visibleAlarm,i))return;for(Intent q:alarmQueue)if(sameAlarm(q,i))return;alarmQueue.addLast(new Intent(i));Diagnostics.log(this,"Добавлено в очередь: "+id+"; ожидают: "+alarmQueue.size());}
 void home(){permissionStatus=null;permissionButton=null;visibleAlarm=null;actions=null;if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(false);setTurnScreenOn(false);}getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);base();label("СОЗДАНИЕ СТРУКТУРЫ",22);label("Напоминания о встречах",28);button("Обновить приложение",()->checkUpdate());button("Журнал диагностики",()->new AlertDialog.Builder(this).setTitle("Журнал диагностики").setMessage(Diagnostics.read(this)).setNeutralButton("Очистить",(d,w)->{Diagnostics.clear(this);new AlertDialog.Builder(this).setTitle("Журнал диагностики").setMessage(Diagnostics.read(this)).setPositiveButton("Закрыть",null).show();}).setPositiveButton("Закрыть",null).show());button("Войти и проверить синхронизацию",()->cloudLogin());button("Обновить планы сейчас",()->{new Thread(()->{try{String result=CloudSync.run(getApplicationContext(),null);runOnUiThread(()->{if(!isFinishing())new AlertDialog.Builder(this).setTitle("Синхронизация").setMessage(result).setPositiveButton("Закрыть",null).show();});}catch(Exception e){runOnUiThread(()->Toast.makeText(this,"Не удалось обновить. Проверьте интернет или повторите вход.",1).show());}}).start();});partner=field("Имя партнёра");guests=field("Гости (по одному на строку)");date=Calendar.getInstance();date.add(Calendar.HOUR,7);time=label(fmt(date.getTimeInMillis(),"dd.MM.yyyy HH:mm"),30);button("Выбрать дату и время",()->new DatePickerDialog(this,(v,y,m,d)->{date.set(y,m,d);new TimePickerDialog(this,(w,h,min)->{date.set(Calendar.HOUR_OF_DAY,h);date.set(Calendar.MINUTE,min);date.set(Calendar.SECOND,0);time.setText(fmt(date.getTimeInMillis(),"dd.MM.yyyy HH:mm"));},date.get(Calendar.HOUR_OF_DAY),date.get(Calendar.MINUTE),true).show();},date.get(Calendar.YEAR),date.get(Calendar.MONTH),date.get(Calendar.DAY_OF_MONTH)).show());permissionPanel();button("Показ поверх Zoom и других приложений",()->{try{startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));}catch(ActivityNotFoundException ex){Toast.makeText(this,"Откройте разрешение поверх других приложений в настройках",1).show();}});button("Сохранить встречу",()->schedule());button("Тест WhatsApp: напоминание партнёру через 10 секунд",()->testMentor());button("Предпросмотр наставника",()->showAlarm(data(2,date.getTimeInMillis(),900).putExtra("mentor",true)));button("Предпросмотр запроса подтверждения",()->showAlarm(data(2,date.getTimeInMillis(),901).putExtra("mentor",true).putExtra("approval",true)));button("Тест: два коротких через 10 секунд",()->test(6));button("Тест: три коротких через 10 секунд",()->test(2));refreshPermissions();}
 Intent data(int stage,long meeting,int id){return new Intent(this,AlarmReceiver.class).putExtra("stage",stage).putExtra("meeting",meeting).putExtra("partner",partner.getText().toString()).putExtra("guests",guests.getText().toString()).putExtra("id",id);}
 boolean set(Intent i,long when){AlarmManager a=getSystemService(AlarmManager.class);if(Build.VERSION.SDK_INT>=31&&!a.canScheduleExactAlarms()){Toast.makeText(this,"Разрешите точные будильники",1).show();return false;}PendingIntent p=PendingIntent.getBroadcast(this,i.getIntExtra("id",0),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);PendingIntent show=PendingIntent.getActivity(this,i.getIntExtra("id",0),new Intent(this,MainActivity.class).putExtras(i),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);a.setAlarmClock(new AlarmManager.AlarmClockInfo(when,show),p);CloudSync.rememberAlarm(this,i,when);return true;}
 void cancelTests(){for(int id:new int[]{2,6,906}){PendingIntent p=PendingIntent.getBroadcast(this,id,new Intent(this,AlarmReceiver.class),PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);if(p!=null){getSystemService(AlarmManager.class).cancel(p);p.cancel();}getSystemService(NotificationManager.class).cancel(id);}alarmQueue.removeIf(i->!i.hasExtra("cloudPlan")&&(i.getIntExtra("id",0)==2||i.getIntExtra("id",0)==6||i.getIntExtra("id",0)==906));}
 void test(int stage){cancelTests();long when=System.currentTimeMillis()+10000;int id=stage;if(set(data(stage,when+stage*3600000L,id),when))Diagnostics.log(this,"Назначен тест "+stage+" ID "+id+" на "+fmt(when,"HH:mm:ss"));}
 void schedule(){long m=date.getTimeInMillis();int count=0;for(int s:new int[]{6,2}){long t=m-s*3600000L;if(t>System.currentTimeMillis()&&set(data(s,m,(int)(m/60000)*10+s),t))count++;}Toast.makeText(this,"Запланировано сигналов: "+count,1).show();}

 String shortName(String name){if(name==null)return "";String[] a=name.trim().split("\\s+");return a.length>=2?a[1]+" "+a[0].substring(0,1)+".":name;}
 void action(String s,int color,Runnable run){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextSize(18);b.setTextColor(Color.WHITE);android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColors(new int[]{color==0xff168b40?0xff39c94e:0xff666970,color});bg.setOrientation(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM);bg.setStroke(dp(1),color==0xff168b40?0xff70e88a:0xffc1b28e);bg.setCornerRadius(dp(14));b.setBackground(bg);if(actions==null){actions=new LinearLayout(this);actions.setOrientation(wide()?0:1);actions.setGravity(17);layout.addView(actions,new LinearLayout.LayoutParams(-1,-2));} LinearLayout.LayoutParams lp=wide()?new LinearLayout.LayoutParams(0,-2,1):new LinearLayout.LayoutParams(-1,-2);lp.setMargins(dp(5),dp(6),dp(5),dp(6));b.setMinHeight(dp(48));b.setTextSize(wide()?15:18);actions.addView(b,lp);b.setOnClickListener(v->run.run());}
 void centered(String s,int size,int color){TextView t=label(s,size);t.setGravity(17);t.setTypeface(null,1);t.setTextColor(color);}
 void dismissAlarm(Intent i){closeWindow(i);}
 void comment(Intent i){if(i.hasExtra("cloudPlan")){Toast.makeText(this,"Изменение облачного плана пока не подключено",1).show();return;}EditText e=new EditText(this);e.setHint("Причина / комментарий");new AlertDialog.Builder(this).setTitle("Закрыть с комментарием").setView(e).setNegativeButton("Отмена",null).setPositiveButton("Сохранить локально",(d,w)->{if(e.getText().toString().trim().isEmpty()){Toast.makeText(this,"Укажите причину",0).show();return;}getPreferences(0).edit().putString("comment_"+i.getIntExtra("id",0),e.getText().toString()).apply();dismissAlarm(i);}).show();}
 void showAlarm(Intent i){Diagnostics.log(this,"Открыто окно напоминания");visibleAlarm=i;actions=null;if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(true);setTurnScreenOn(true);}else getWindow().addFlags(0x80000|0x200000);getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);base();boolean mentor=i.getBooleanExtra("mentor",false),approval=i.getBooleanExtra("approval",false);styleAlarm(mentor);int gold=mentor?0xffffd58a:0xffb8ddff;long m=i.getLongExtra("meeting",0);String names=shortName(i.getStringExtra("partner"))+" — "+i.getStringExtra("guests");if(approval){centered("Встреча состоялась! Прошу подтверждение?",wide()?22:17,gold);}else{if(!mentor)centered("МОЁ НАПОМИНАНИЕ",22,gold);centered(fmt(m,"HH:mm"),wide()?56:71,Color.WHITE);centered(fmt(m,"d MMMM · EEEE"),27,Color.WHITE);}centered((i.getStringExtra("meetingType")==null?"ОРР":i.getStringExtra("meetingType"))+"  |  "+names,wide()?22:23,gold);if(approval)centered(fmt(m,"d MMMM · EEEE · HH:mm"),22,Color.WHITE);if(mentor){if(approval){action("Подтверждаю",0xff168b40,()->Toast.makeText(this,"Предпросмотр: база не подключена",1).show());action("Отклонить",0xff40444a,()->comment(i));actions=null;action("Закрыть",0xff40444a,()->closeWindow(i));}else{action("Напомнить в WhatsApp",0xff168b40,()->whatsapp(i));action("Связаться с партнёром",0xff40444a,()->Toast.makeText(this,"Номер партнёра ещё не подключён",1).show());action("Открыть его план",0xff40444a,()->Toast.makeText(this,"Ссылка на план ещё не подключена",1).show());if(wide())actions=null;action("Закрыть",0xff40444a,()->closeOptions(i));}}else{action("Подтвердить встречу",0xff168b40,()->confirmLocal(i));action("Отложить на 10 минут",0xff245a92,()->snoozeWindow(i,10));action("Закрыть с комментарием",0xff40444a,()->comment(i));}}
 int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 boolean wide(){return getResources().getConfiguration().orientation==2;}
 void styleAlarm(boolean mentor){
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
  int edge=mentor?0xffe9b85c:0xff77bfff;
  scroll.setBackgroundColor(mentor?0xff21150c:0xff081525);
  android.widget.FrameLayout frame=new android.widget.FrameLayout(this);
  scroll.addView(frame,new ScrollView.LayoutParams(-1,-2));
  layout=new LinearLayout(this);layout.setOrientation(1);layout.setGravity(17);
  layout.setPadding(dp(16),dp(wide()?10:24),dp(16),dp(wide()?10:24));
  android.graphics.drawable.GradientDrawable panel=new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,new int[]{mentor?0xff493019:0xff17395a,mentor?0xff24170d:0xff081b30});
  panel.setCornerRadius(dp(24));panel.setStroke(dp(2),edge);layout.setBackground(panel);
  int width=Math.min((int)(getResources().getDisplayMetrics().widthPixels*0.91),dp(780));
  android.widget.FrameLayout.LayoutParams lp=new android.widget.FrameLayout.LayoutParams(width,-2,17);lp.setMargins(0,dp(12),0,dp(12));frame.addView(layout,lp);
  setContentView(scroll);
  if(Build.VERSION.SDK_INT>=30)scroll.setOnApplyWindowInsetsListener((v,in)->{android.graphics.Insets bars=in.getInsets(android.view.WindowInsets.Type.systemBars());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return in;});
 }
 @Override public void onConfigurationChanged(android.content.res.Configuration config){super.onConfigurationChanged(config);if(visibleAlarm!=null)showAlarm(visibleAlarm);}
 @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putParcelableArrayList("alarmQueue",new ArrayList<>(alarmQueue));if(visibleAlarm!=null)out.putParcelable("visibleAlarm",visibleAlarm);}

 void closeWindow(Intent i){PendingAlarms.remove(this,i);getSystemService(NotificationManager.class).cancel(i.getIntExtra("id",0));visibleAlarm=null;alarmQueue.removeIf(x->x.hasExtra("cloudPlan")&&!PendingAlarms.contains(this,x));for(Intent x:PendingAlarms.list(this)){boolean found=false;for(Intent y:alarmQueue)if(PendingAlarms.key(x).equals(PendingAlarms.key(y))){found=true;break;}if(!found)alarmQueue.addLast(x);}if(!alarmQueue.isEmpty()){Diagnostics.log(this,"Открывается следующее напоминание из очереди");showAlarm(alarmQueue.removeFirst());return;}if(i.getIntExtra("id",0)==900||i.getIntExtra("id",0)==901){home();}else{finish();}}
 void snoozeWindow(Intent i,int minutes){Intent n=new Intent(this,AlarmReceiver.class).putExtras(i);long when=System.currentTimeMillis()+minutes*60000L;n.putExtra("snooze",true);try{if(!set(n,when))return;if(i.hasExtra("cloudPlan"))CloudSync.prefs(this).edit().putLong("snooze:"+i.getStringExtra("cloudPlan")+":"+i.getIntExtra("stage",2),when).putLong("snoozeMeeting:"+i.getStringExtra("cloudPlan")+":"+i.getIntExtra("stage",2),i.getLongExtra("meeting",0)).commit();}catch(RuntimeException ex){Toast.makeText(this,"Не удалось установить повторное напоминание",1).show();return;}getSystemService(NotificationManager.class).cancel(i.getIntExtra("id",0));boolean mentor=i.getBooleanExtra("mentor",false);styleAlarm(mentor);actions=null;int accent=mentor?0xffffd58a:0xffb8ddff;TextView check=label("✓",26);check.setGravity(android.view.Gravity.END);check.setTextColor(0xff69df85);check.setTypeface(null,1);check.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));check.setPadding(0,0,0,0);TextView heading=label(mentor?"НАПОМИНАНИЕ\nПАРТНЁРУ":"МОЁ\nНАПОМИНАНИЕ",24);heading.setGravity(17);heading.setTypeface(null,1);heading.setTextColor(accent);heading.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));android.view.View line=new android.view.View(this);line.setBackgroundColor(mentor?0xff98713b:0xff42688a);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(1));lp.setMargins(dp(8),dp(8),dp(8),dp(8));layout.addView(line,lp);centered("Повторный сигнал через",18,Color.WHITE);centered(minutes+" минут",36,Color.WHITE);new Handler(Looper.getMainLooper()).postDelayed(()->{if(!isFinishing()&&visibleAlarm==i)closeWindow(i);},3000);}
 void closeOptions(Intent i){
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Напомнить через").setItems(new String[]{"5 минут","10 минут","15 минут","Закрыть без повтора"},(d,w)->{if(w==3)closeWindow(i);else snoozeWindow(i,new int[]{5,10,15}[w]);}).setNegativeButton("Отмена",null).create();
  dialog.show();
 }
 TextView permissionStatus;
 Button permissionButton;
 boolean notificationsReady(){return getSystemService(NotificationManager.class).areNotificationsEnabled();}
 boolean exactReady(){return Build.VERSION.SDK_INT<31||getSystemService(AlarmManager.class).canScheduleExactAlarms();}
 boolean fullReady(){return Build.VERSION.SDK_INT<34||getSystemService(NotificationManager.class).canUseFullScreenIntent();}

 int pendingPermission=0;
 boolean activationRunning=false;
 boolean ready(){return notificationsReady()&&exactReady()&&fullReady();}
 void permissionPanel(){permissionStatus=label("",18);permissionButton=new Button(this);permissionButton.setAllCaps(false);permissionButton.setTextSize(20);permissionButton.setMinHeight(dp(58));layout.addView(permissionButton);permissionButton.setOnClickListener(v->{activationRunning=true;advancePermissions();});}
 void refreshPermissions(){if(visibleAlarm!=null||permissionStatus==null||permissionButton==null)return;boolean n=notificationsReady(),e=exactReady(),f=fullReady(),ok=n&&e&&f;permissionStatus.setText((ok?"✓ Напоминания готовы":"Для начала нажмите зелёную кнопку")+"\n"+(n?"✓":"✗")+" Уведомления\n"+(e?"✓":"✗")+" Точные будильники\n"+(f?"✓":"✗")+" Полноэкранные оповещения\n"+(Settings.canDrawOverlays(this)?"✓":"○")+" Показ поверх других приложений");permissionStatus.setTextColor(ok?0xff83e6a2:0xffffd18a);
 for(int j=0;j<layout.getChildCount();j++){android.view.View v=layout.getChildAt(j);if(v!=permissionButton&&(v instanceof Button||v instanceof EditText)){boolean enabled=ok||(v instanceof Button&&((Button)v).getText().toString().matches("Войти и проверить синхронизацию|Обновить приложение|Журнал диагностики"));v.setEnabled(enabled);v.setAlpha(enabled?1f:0.4f);}}
 permissionButton.setText(ok?"✓ Напоминания активированы":"Активировать напоминания");permissionButton.setEnabled(!ok&&!activationRunning);permissionButton.setTextColor(Color.WHITE);android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,new int[]{ok?0xff666970:0xff42d85e,ok?0xff45484c:0xff13832c});bg.setCornerRadius(dp(12));bg.setStroke(dp(2),ok?0xff777b80:0xff9affad);permissionButton.setBackground(bg);}
 void advancePermissions(){if(visibleAlarm!=null){activationRunning=false;return;}if(ready()){activationRunning=false;pendingPermission=0;refreshPermissions();return;}refreshPermissions();try{if(!notificationsReady()){pendingPermission=1;if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED&&!getPreferences(0).getBoolean("notificationAsked",false)){getPreferences(0).edit().putBoolean("notificationAsked",true).apply();requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},41);}else startActivityForResult(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()),42);}else if(!exactReady()){pendingPermission=2;startActivityForResult(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())),42);}else if(!fullReady()){pendingPermission=3;startActivityForResult(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,Uri.parse("package:"+getPackageName())),42);}}catch(ActivityNotFoundException ex){activationRunning=false;pendingPermission=0;refreshPermissions();Toast.makeText(this,"Не удалось открыть настройки разрешения",1).show();}}
 void permissionReturned(){int step=pendingPermission;pendingPermission=0;if(!activationRunning||step==0)return;boolean granted=step==1?notificationsReady():step==2?exactReady():fullReady();if(granted){advancePermissions();}else{activationRunning=false;refreshPermissions();Toast.makeText(this,"Разрешение не включено. Нажмите активацию для продолжения.",1).show();}}
 
 void checkUpdate(){
 Toast.makeText(this,"Проверка обновлений…",0).show();
 new Thread(()->{try{
 java.net.HttpURLConnection conn=(java.net.HttpURLConnection)new java.net.URL("https://api.github.com/repos/sbaigazinov32-svg/my-project/releases/latest").openConnection();
 conn.setConnectTimeout(10000);conn.setReadTimeout(10000);conn.setRequestProperty("Accept","application/vnd.github+json");
 String raw;try(java.io.InputStream in=conn.getInputStream();java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>1000000)throw new java.io.IOException("Response too large");}raw=out.toString("UTF-8");}finally{conn.disconnect();}
 org.json.JSONObject release=new org.json.JSONObject(raw);String tag=release.getString("tag_name");if(!tag.matches("reminder-[0-9]+-[0-9]+"))throw new java.io.IOException("Unknown version");
 int remote=Integer.parseInt(tag.split("-")[1]);int current=getPackageManager().getPackageInfo(getPackageName(),0).versionCode;
 runOnUiThread(()->{if(isFinishing())return;if(remote<=current){new AlertDialog.Builder(this).setMessage("Установлена последняя версия").setPositiveButton("OK",null).show();}else new AlertDialog.Builder(this).setTitle("Доступно обновление").setMessage("Версия "+remote+". Ваши данные сохранятся.").setPositiveButton("Обновить",(d,w)->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/sbaigazinov32-svg/my-project/releases/download/"+tag+"/StructureReminder.apk")));}catch(Exception e){Toast.makeText(this,"Не удалось открыть загрузку",1).show();}}).setNegativeButton("Позже",null).show();});
 }catch(Exception e){runOnUiThread(()->Toast.makeText(this,"Не удалось проверить обновление. Проверьте интернет.",1).show());}}).start();
 }
 final Handler foregroundSync=new Handler(Looper.getMainLooper());
 final Runnable foregroundTick=new Runnable(){public void run(){if(CloudSync.prefs(MainActivity.this).contains("session")&&ready()&&System.currentTimeMillis()-CloudSync.prefs(MainActivity.this).getLong("lastSync",0)>60000L){new Thread(()->{try{CloudSync.run(getApplicationContext(),null);}catch(Exception e){Diagnostics.log(getApplicationContext(),"Ошибка синхронизации: "+e.getClass().getSimpleName());}}).start();}foregroundSync.postDelayed(this,60000L);}};
 @Override protected void onResume(){super.onResume();refreshPermissions();foregroundSync.removeCallbacks(foregroundTick);foregroundSync.post(foregroundTick);}
 @Override protected void onPause(){foregroundSync.removeCallbacks(foregroundTick);if(System.currentTimeMillis()-CloudSync.prefs(this).getLong("lastSync",0)>60000L)CloudSync.requestSoon(this);super.onPause();}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==42)new Handler(Looper.getMainLooper()).postDelayed(()->permissionReturned(),350);}
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==41)permissionReturned();}
 void confirmLocal(Intent i){
  if(i.hasExtra("cloudPlan")){new AlertDialog.Builder(this).setTitle("Тестовое напоминание").setMessage("Подтверждение в основной программе пока не подключено.").setPositiveButton("Закрыть сигнал",(d,w)->closeWindow(i)).setNegativeButton("Отмена",null).show();return;}
  long meeting=i.getLongExtra("meeting",0);
  String key="confirmed_"+meeting+"_"+i.getStringExtra("partner")+"_"+i.getStringExtra("guests");
  if(!getPreferences(0).edit().putLong(key,System.currentTimeMillis()).commit()){Toast.makeText(this,"Не удалось сохранить подтверждение",1).show();return;}
  getSystemService(NotificationManager.class).cancel(i.getIntExtra("id",0));
  styleAlarm(false);actions=null;
  TextView check=label("✓",26);check.setGravity(android.view.Gravity.END);check.setTextColor(0xff69df85);check.setTypeface(null,1);check.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));
  centered("МОЁ НАПОМИНАНИЕ",24,0xffb8ddff);
  centered("Встреча подтверждена",26,0xff69df85);
  new Handler(Looper.getMainLooper()).postDelayed(()->{if(!isFinishing()&&visibleAlarm==i)closeWindow(i);},3000);
 }
 void whatsapp(Intent i){
  Runnable open=()->{
   String phone=i.getStringExtra("phone");
   if(phone==null||phone.trim().isEmpty()){
    EditText field=new EditText(this);field.setInputType(android.text.InputType.TYPE_CLASS_PHONE);field.setHint("+7...");
    new AlertDialog.Builder(this).setTitle("Тест: номер партнёра").setMessage("До подключения базы введите номер для проверки WhatsApp.").setView(field).setNegativeButton("Отмена",null).setPositiveButton("Открыть WhatsApp",(d,w)->launchWhatsApp(i,field.getText().toString())).show();
   }else launchWhatsApp(i,phone);
  };
  KeyguardManager k=getSystemService(KeyguardManager.class);
  if(k.isKeyguardLocked()){
   if(Build.VERSION.SDK_INT>=26)k.requestDismissKeyguard(this,new KeyguardManager.KeyguardDismissCallback(){@Override public void onDismissSucceeded(){runOnUiThread(open);}@Override public void onDismissError(){Toast.makeText(MainActivity.this,"Разблокируйте телефон и повторите",1).show();}});
   else Toast.makeText(this,"Разблокируйте телефон и повторите",1).show();
  }else open.run();
 }
 void launchWhatsApp(Intent i,String phone){
  String digits=phone.replaceAll("[^0-9]","");
  if(digits.length()==11&&digits.startsWith("8")&&!phone.trim().startsWith("+"))digits="7"+digits.substring(1);
  if(digits.length()==10&&!phone.trim().startsWith("+"))digits="7"+digits;
  if(digits.length()<8||digits.length()>15){Toast.makeText(this,"Введите номер с кодом страны",1).show();return;}
  long m=i.getLongExtra("meeting",0);
  String type=i.getStringExtra("meetingType");if(type==null||type.isEmpty())type="ОРР";
  String message="Напоминаю! ❤️\n*"+fmt(m,"HH:mm")+"* — "+fmt(m,"d MMMM")+"\n"+type+" | "+i.getStringExtra("partner")+" — "+i.getStringExtra("guests");
  Uri uri=Uri.parse("whatsapp://send?phone="+digits+"&text="+Uri.encode(message));
  String[] packages={"com.whatsapp","com.whatsapp.w4b"};
  for(String pkg:packages){try{startActivity(new Intent(Intent.ACTION_VIEW,uri).setPackage(pkg));return;}catch(ActivityNotFoundException ex){}}
  Uri fallback=Uri.parse("https://wa.me/"+digits+"?text="+Uri.encode(message));
  for(String pkg:packages){try{startActivity(new Intent(Intent.ACTION_VIEW,fallback).setPackage(pkg));return;}catch(ActivityNotFoundException ex){}}
  Toast.makeText(this,"WhatsApp не установлен или недоступен",1).show();
 }
 void testMentor(){cancelTests();
  long when=System.currentTimeMillis()+10000L;
  Intent i=data(6,when+6*3600000L,906).putExtra("mentor",true).putExtra("approval",false);
  if(set(i,when))Toast.makeText(this,"Тест установлен. Заблокируйте телефон.",1).show();
 }

 String cloudRequest(String path,String token,org.json.JSONObject body)throws Exception{
  javax.net.ssl.HttpsURLConnection c=(javax.net.ssl.HttpsURLConnection)new java.net.URL("https://chgpyjguxzdrzfbergzu.supabase.co"+path).openConnection();
  try{c.setConnectTimeout(15000);c.setReadTimeout(15000);c.setInstanceFollowRedirects(false);c.setRequestProperty("apikey","sb_publishable_stOmxI6S5WzPYdfth_38SA_WLz8Kgwi");if(token!=null)c.setRequestProperty("Authorization","Bearer "+token);
  if(body!=null){c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(java.io.OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes("UTF-8"));}}
  int status=c.getResponseCode();if(status<200||status>=300)throw new java.io.IOException("HTTP "+status);
  try(java.io.InputStream in=c.getInputStream();java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){out.write(buf,0,n);if(out.size()>2000000)throw new java.io.IOException("Ответ слишком большой");}return out.toString("UTF-8");}
  }finally{c.disconnect();}
 }
 void cloudLogin(){
  LinearLayout form=new LinearLayout(this);form.setOrientation(1);form.setPadding(dp(20),dp(8),dp(20),0);
  EditText id=new EditText(this);id.setHint("ID партнёра");id.setSingleLine(true);form.addView(id);
  EditText pass=new EditText(this);pass.setHint("Пароль");pass.setInputType(129);form.addView(pass);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Вход в Создание структуры").setMessage("Проверка подключения: только чтение личных планов.").setView(form).setNegativeButton("Отмена",null).setPositiveButton("Войти",null).create();
  dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(w->{
   String login=id.getText().toString().trim().toLowerCase(Locale.ROOT),password=pass.getText().toString();
   if(!login.matches("kza[a-z0-9-]+")||password.isEmpty()){pass.setError("Проверьте ID и пароль");return;}
   pass.setText("");dialog.dismiss();Toast.makeText(this,"Подключение…",0).show();
   new Thread(()->{try{
    org.json.JSONObject body=new org.json.JSONObject().put("email",login+"@networkplan.local").put("password",password);
    org.json.JSONObject session=new org.json.JSONObject(cloudRequest("/auth/v1/token?grant_type=password",null,body));
    String result=CloudSync.run(getApplicationContext(),session);
    runOnUiThread(()->{if(!isFinishing())new AlertDialog.Builder(this).setTitle("Автосинхронизация включена").setMessage(result).setPositiveButton("Закрыть",null).show();});
   }catch(Exception ex){runOnUiThread(()->{if(!isFinishing())new AlertDialog.Builder(this).setTitle("Не удалось подключиться").setMessage("Проверьте ID, пароль и интернет. "+(ex instanceof java.io.IOException?ex.getMessage():"Ошибка обработки ответа")).setPositiveButton("Закрыть",null).show();});}}).start();
  }));dialog.show();
 }



}