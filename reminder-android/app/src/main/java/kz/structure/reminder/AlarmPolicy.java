package kz.structure.reminder;
import java.util.Calendar;
final class AlarmPolicy {
 static long scheduled(long meeting,int stage,boolean call){
  long when=meeting-(call?1800000L:stage*3600000L);
  Calendar cal=Calendar.getInstance();cal.setTimeInMillis(meeting);
  if(stage==6&&cal.get(Calendar.HOUR_OF_DAY)<14){cal.add(Calendar.DATE,-1);cal.set(Calendar.HOUR_OF_DAY,20);cal.set(Calendar.MINUTE,0);cal.set(Calendar.SECOND,0);cal.set(Calendar.MILLISECOND,0);when=cal.getTimeInMillis();}
  return when;
 }
 static boolean restoreAllowed(long meeting,int stage,boolean snooze,long delivered,long now){
  return snooze||(meeting>now&&delivered!=meeting&&(stage!=6||meeting-7200000L>now));
 }
}
