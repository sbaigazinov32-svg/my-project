package kz.structure.reminder;
import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.*;
final class Diagnostics {
 static synchronized void log(Context c,String event){
 android.content.SharedPreferences p=c.getSharedPreferences("diagnostics",0);
 String line=new SimpleDateFormat("dd.MM HH:mm:ss",Locale.ROOT).format(new Date())+" | "+event+"\n";
 String s=line+p.getString("events","");
 p.edit().putString("events",s.substring(0,Math.min(s.length(),24000))).apply();
 }
 static String read(Context c){return c.getSharedPreferences("diagnostics",0).getString("events","Событий пока нет");}
}