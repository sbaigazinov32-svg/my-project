package kz.structure.reminder;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import java.text.SimpleDateFormat;
import java.util.*;
final class Diagnostics {
 static String line(String event){return new SimpleDateFormat("dd.MM HH:mm:ss",Locale.ROOT).format(new Date())+" | "+event+"\n";}
 static SharedPreferences current(Context c){
 SharedPreferences p=c.getSharedPreferences("diagnostics",0);
 try{
 PackageInfo info=c.getPackageManager().getPackageInfo(c.getPackageName(),0);
 long version=Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode;
 if(p.getLong("journalVersion",-1)!=version){
 p.edit().putLong("journalVersion",version).putString("events",line("Журнал очищен. Установлена версия "+version)).commit();
 }
 }catch(android.content.pm.PackageManager.NameNotFoundException ignored){}
 return p;
 }
 static synchronized void log(Context c,String event){
 SharedPreferences p=current(c);
 String s=line(event)+p.getString("events","");
 p.edit().putString("events",s.substring(0,Math.min(s.length(),24000))).apply();
 }
 static synchronized void clear(Context c){current(c).edit().putString("events",line("Журнал очищен вручную")).commit();}
 static synchronized String read(Context c){return current(c).getString("events","Событий пока нет");}
}