package kz.structure.reminder;
import java.time.*;
import java.util.TimeZone;
public class AlarmPolicyTest {
 static long t(String s){return LocalDateTime.parse(s).atZone(ZoneId.of("Asia/Almaty")).toInstant().toEpochMilli();}
 static int count=0;
 static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);count++;System.out.println("PASS "+name);}
 public static void main(String[] args){
 TimeZone.setDefault(TimeZone.getTimeZone("Asia/Almaty"));
 long m=t("2026-12-02T16:00:00"),morning=t("2026-12-02T10:00:00"),now=t("2026-12-02T09:00:00");
 check(AlarmPolicy.scheduled(m,6,false)==t("2026-12-02T10:00:00"),"six hours");
 check(AlarmPolicy.scheduled(m,2,false)==t("2026-12-02T14:00:00"),"two hours");
 check(AlarmPolicy.scheduled(m,2,true)==t("2026-12-02T15:30:00"),"call thirty minutes");
 check(AlarmPolicy.scheduled(morning,6,false)==t("2026-12-01T20:00:00"),"morning previous evening");
 check(AlarmPolicy.scheduled(t("2026-12-02T14:00:00"),6,false)==t("2026-12-02T08:00:00"),"14 hour boundary");
 check(AlarmPolicy.restoreAllowed(m,6,false,-1,now),"future restore");
 check(!AlarmPolicy.restoreAllowed(m,2,false,m,now),"delivered suppressed");
 check(!AlarmPolicy.restoreAllowed(morning,2,false,-1,m),"expired suppressed");
 check(!AlarmPolicy.restoreAllowed(morning,6,false,-1,now),"obsolete first stage");
 check(AlarmPolicy.restoreAllowed(morning,2,true,morning,m),"explicit snooze retained");
 check(AlarmPolicy.restoreAllowed(m,2,false,morning,now),"rescheduled meeting");
 System.out.println("Passed "+count+" tests");
 }
}
