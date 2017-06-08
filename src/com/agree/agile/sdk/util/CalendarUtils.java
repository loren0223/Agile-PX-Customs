/*
 * 在 2006/6/12 建立
 *
 * 若要變更這個產生的檔案的範本，請移至
 * 視窗 > 喜好設定 > Java > 程式碼產生 > 程式碼和註解
 */
package com.agree.agile.sdk.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author maxwell
 * 
 * 若要變更這個產生的類別註解的範本，請移至 視窗 > 喜好設定 > Java > 程式碼產生 > 程式碼和註解
 */
public class CalendarUtils {
   private static final String[] zeroStrs = { "0", "00", "000" };

   //private static Calendar cal = Calendar.getInstance();

   private static int dateno = -1;

   private static String yearmonthdatestr = null;

   private static String datestr = null;

   public CalendarUtils() {

   }
   
   public static long getDateDiff(String startDate , String endDate) throws ParseException{
	   SimpleDateFormat format = new SimpleDateFormat ("yyyy-MM-dd") ;
	   Date date1 = format.parse(startDate); 
	   Date date2 = format.parse(endDate);
	   long l = date2.getTime()-date1.getTime() ;
	   long s = l/(24*60*60*1000);
	   return s ;
   }

   public static String getYearMonthDateStr() {
      Calendar newcal = Calendar.getInstance();
      int no = newcal.get(Calendar.DATE);
      if (no != dateno) {
         recreateYearMonDateStr(newcal);
      }
      return yearmonthdatestr;
   }

   private static void recreateYearMonDateStr(Calendar newcal) {
      int yearno = newcal.get(Calendar.YEAR);
      int monthno = newcal.get(Calendar.MONTH) + 1;
      dateno = newcal.get(Calendar.DATE);
      String yearstr = null;
      String monstr = null;

      yearstr = String.valueOf(yearno);

      if (monthno < 10) {
         monstr = zeroStrs[0] + String.valueOf(monthno);
      } else
         monstr = String.valueOf(monthno);

      if (dateno < 10) {
         datestr = zeroStrs[0] + String.valueOf(dateno);
      } else
         datestr = String.valueOf(dateno);

      yearmonthdatestr = yearstr + monstr + datestr;
   }

   public synchronized static String getDateStr() {
      Calendar newcal = Calendar.getInstance();
      int no = newcal.get(Calendar.DATE);
      if (no != dateno) {
         recreateYearMonDateStr(newcal);
      }
      return datestr;
   }

   public synchronized static String getTimeSecMinSec() {
      String hourstr, minstr, secstr, msecstr;

      Calendar cal = Calendar.getInstance();
      int hour = cal.get(Calendar.HOUR_OF_DAY);
      int min = cal.get(Calendar.MINUTE);
      int sec = cal.get(Calendar.SECOND);
      int msec = cal.get(Calendar.MILLISECOND);

      if (hour < 10)
         hourstr = zeroStrs[0] + String.valueOf(hour);
      else
         hourstr = String.valueOf(hour);

      if (min < 10)
         minstr = zeroStrs[0] + String.valueOf(min);
      else
         minstr = String.valueOf(min);

      if (sec < 10)
         secstr = zeroStrs[0] + String.valueOf(sec);
      else
         secstr = String.valueOf(sec);

      if (msec < 10)
         msecstr = zeroStrs[1] + String.valueOf(msec);
      else if (msec < 100)
         msecstr = zeroStrs[0] + String.valueOf(msec);
      else
         msecstr = String.valueOf(msec);

      return hourstr + minstr + secstr + msecstr;
   }
   
   public synchronized static String getTimeSec(){
      String hourstr, minstr, secstr;

      Calendar cal = Calendar.getInstance();
      int hour = cal.get(Calendar.HOUR_OF_DAY);
      int min = cal.get(Calendar.MINUTE);
      int sec = cal.get(Calendar.SECOND);
      //int msec = cal.get(Calendar.MILLISECOND);

      if (hour < 10)
         hourstr = zeroStrs[0] + String.valueOf(hour);
      else
         hourstr = String.valueOf(hour);

      if (min < 10)
         minstr = zeroStrs[0] + String.valueOf(min);
      else
         minstr = String.valueOf(min);

      if (sec < 10)
         secstr = zeroStrs[0] + String.valueOf(sec);
      else
         secstr = String.valueOf(sec); 
      
      return hourstr + minstr + secstr; 
   }
   
   public static String getIntervalDate(String unit, int i){
   	  String datestr = null;
   	  
   	  Date d = new Date();
   	  SimpleDateFormat sdfmt = new SimpleDateFormat("yyyyMMdd");
   	  Calendar cal = Calendar.getInstance();
   	  d = cal.getTime();
   	  
   	  if(unit.toUpperCase().equals("YEAR")){
   	  	cal.add(Calendar.YEAR, i);
   	  }
   	  else if(unit.toUpperCase().equals("MONTH")){
		cal.add(Calendar.MONTH, i);
   	  }
   	  else if(unit.toUpperCase().equals("DATE")){
		cal.add(Calendar.DATE, i);
   	  }
   	  //else datestr = "00000000";
   	  
   	  d = cal.getTime();
   	  datestr = sdfmt.format(d);
   	  
   	  return datestr;
   }

   public static void main(String[] args) throws InterruptedException {
      Calendar cal = Calendar.getInstance();
      int date1 = cal.get(Calendar.SECOND);
      Thread.sleep(5000);
      int date2 = Calendar.getInstance().get(Calendar.SECOND);

      System.out.println("1 : " + date1);
      System.out.println("2 : " + date2);
   }
   
	// convert Date Format
	public static Calendar convertDateFormat(String datestr) {
		Calendar tmpCal = Calendar.getInstance();
		int year = Integer.parseInt(datestr.substring(0, 4));
		int month = Integer.parseInt(datestr.substring(5, 7)) - 1;
		int date = Integer.parseInt(datestr.substring(8, 10));
		int hour = Integer.parseInt(datestr.substring(11, 13));			
		int minute = Integer.parseInt(datestr.substring(14, 16));
		int second = Integer.parseInt(datestr.substring(17, 19));
		tmpCal.set(year, month, date, hour, minute, second);
		//20080625  - Fix Agile ACS date bug !(ACS default TimeZone: +0 hour)
		tmpCal.add(Calendar.HOUR, 8); //for SMT TimeZone: +8 hour
		return tmpCal;
	}

}