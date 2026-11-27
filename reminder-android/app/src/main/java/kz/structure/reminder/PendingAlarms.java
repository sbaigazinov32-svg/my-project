package kz.structure.reminder;
import android.content.*;
import java.util.*;
import org.json.*;
final class PendingAlarms {
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("pending_alarms",0);}
 static String key(Intent i){return i.getStringExtra("cloudPlan")+":"+i.getIntExtra("stage",2)+":"+i.getLongExtra("meeting",0);}
 static JSONArray read(Context c){try{return new JSONArray(prefs(c).getString("queue","[]"));}catch(JSONException e){throw new IllegalStateException("Invalid pending queue",e);}}
 static void write(Context c,JSONArray a){if(!prefs(c).edit().putString("queue",a.toString()).commit())throw new IllegalStateException("Pending queue storage");}
 static synchronized void add(Context c,Intent i){
 if(!i.hasExtra("cloudPlan"))return;
 JSONArray a=read(c);String k=key(i);
 for(int j=0;j<a.length();j++)if(k.equals(a.optJSONObject(j).optString("key")))return;
 try{a.put(new JSONObject().put("key",k).put("intent",i.toUri(Intent.URI_INTENT_SCHEME)));write(c,a);}catch(JSONException e){throw new IllegalStateException(e);}
 }
 static synchronized void remove(Context c,Intent i){
 if(!i.hasExtra("cloudPlan"))return;
 JSONArray a=read(c),next=new JSONArray();String k=key(i);
 for(int j=0;j<a.length();j++)if(!k.equals(a.optJSONObject(j).optString("key")))next.put(a.optJSONObject(j));
 write(c,next);
 }
 static synchronized ArrayList<Intent> list(Context c){
 ArrayList<Intent> result=new ArrayList<>();JSONArray a=read(c);
 for(int j=0;j<a.length();j++)try{result.add(Intent.parseUri(a.getJSONObject(j).getString("intent"),Intent.URI_INTENT_SCHEME));}catch(Exception e){Diagnostics.log(c,"Ошибка чтения очереди: "+e.getClass().getSimpleName());}
 return result;
 }
 static synchronized boolean contains(Context c,Intent i){for(Intent q:list(c))if(key(q).equals(key(i)))return true;return false;}
}
