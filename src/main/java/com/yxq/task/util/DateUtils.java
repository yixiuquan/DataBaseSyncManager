package com.yxq.task.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {

    public static final String yyyy_MM_dd_hh_mm_ss = "yyyy-MM-dd HH:mm:ss";

    public static final String yyyy_MM_dd = "yyyy-MM-dd";

    public static final String yyyyMMddHHmmss = "yyyyMMddHHmmss";

    public static final SimpleDateFormat dateFormat = new SimpleDateFormat(yyyyMMddHHmmss);

    public static final SimpleDateFormat dateFormat2 = new SimpleDateFormat(yyyy_MM_dd_hh_mm_ss);

    public static final SimpleDateFormat dateFormat3 = new SimpleDateFormat(yyyy_MM_dd);

    private static SimpleDateFormat dateFormat4;

    /**
     * 获取当前时间并加上2分25s，因为服务器时间比北京时间少2分25s
     *
     * @return
     */
    public static Date getCorrectDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 2);
        calendar.add(Calendar.SECOND, 25);
        return calendar.getTime();
    }

    /**
     * 按照指定格式返回 SimpleDateFormat 对象
     *
     * @param format 指定返回 SimpleDateFormat 对象的格式
     * @return 指定格式的 SimpleDateFormat 对象
     */
    public static SimpleDateFormat getDateFormat(String format) {

        // 如果参数指定的格式属于默认格式的一种，直接返回对应的 SimpleDateFormat 对象
        if (yyyyMMddHHmmss.equals(format)) {
            return dateFormat;
        } else if (yyyy_MM_dd_hh_mm_ss.equals(format)) {
            return dateFormat2;
        } else if (yyyy_MM_dd.equals(format)) {
            return dateFormat3;
        } else {

            // 不属于默认格式，此时先判断是否与现有对象格式相同，不相同才创建新对象
            if (dateFormat4 == null || !format.equals(dateFormat4.toPattern())) {
                dateFormat4 = new SimpleDateFormat(format);
            }
            return dateFormat4;
        }
    }

    /**
     * 将数据库抓取的时间long 转成String
     *
     * @param time
     * @return
     * @throws ParseException
     */
    public static String longToString(long time, String format)
            throws ParseException {
        Date date = new Date(time);
        String dateStr = date2String(date, format);
        return dateStr;
    }

    public static Date string2date(String time, String format)
            throws ParseException {
        Date date = null;
        SimpleDateFormat formatter = getDateFormat(format);
        date = formatter.parse(time);
        return date;
    }

    public static String date2String(Date date, String format) {
        SimpleDateFormat formatter = getDateFormat(format);
        return formatter.format(date);
    }

    public static long string2long(String time, String format)
            throws ParseException {
        Date date = string2date(time, format);
        return date.getTime();
    }

    /**
     * 取得 Date 类型表示的今天的开始时间
     *
     * @return 当天开始时间
     */
    public static Date getDayFirsttime() {
        return getDayFirsttime(new Date());
    }

    /**
     * 取得参数 date 所在那天的开始时间
     *
     * @param date 需要取得开始时间的日期
     * @return date 所在那天的开始时间
     */
    public static Date getDayFirsttime(Date date) {
        if (null == date) {
            return date;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTime();
    }

    /**
     * 取得今天开始时间，并按照指定格式转换成 String 类型
     *
     * @param format 转换的格式
     * @return 转换后的今天开始时间
     */
    public static String getDayFirsttime(String format) {
        return getDayFirsttime(new Date(), format);
    }

    /**
     * 取得 date 所在日期的开始时间，并按照指定格式转换成 String 类型
     *
     * @param date   需要取得开始时间的日期
     * @param format 转换的格式
     * @return date 所在那天的开始时间
     */
    public static String getDayFirsttime(Date date, String format) {
        date = getDayFirsttime(date);
        return date2String(date, format);
    }

    /**
     * 取得 Date 类型表示的这个月的开始时间
     *
     * @return 当月开始时间
     */
    public static Date getMonthFirsttime() {
        return getMonthFirsttime(new Date());
    }

    /**
     * 取得参数 date 所在那个月的开始时间
     *
     * @param date 需要取得开始时间的日期
     * @return date 所在那个月的开始时间
     */
    public static Date getMonthFirsttime(Date date) {
        if (null == date) {
            return date;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    /**
     * 取得这个月的开始时间，并按照指定格式转换成 String 类型
     *
     * @param format 转换的格式
     * @return 转换后的这个月开始时间
     */
    public static String getMonthFirsttime(String format) {
        return getMonthFirsttime(new Date(), format);
    }

    /**
     * 取得 date 所在月份的开始时间，并按照指定格式转换成 String 类型
     *
     * @param date   需要取得开始时间的日期
     * @param format 转换的格式
     * @return date 所在那个月的开始时间
     */
    public static String getMonthFirsttime(Date date, String format) {
        date = getMonthFirsttime(date);
        return date2String(date, format);
    }

    /**
     * 取得 Date 类型表示的今天的最晚时间
     *
     * @return 当天最晚时间
     */
    public static Date getDayLasttime() {
        return getDayLasttime(new Date());
    }

    /**
     * 取得参数 date 所在那天的开始时间
     *
     * @param date 需要取得开始时间的日期
     * @return date 所在那天的开始时间
     */
    public static Date getDayLasttime(Date date) {
        if (null == date) {
            return date;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    /**
     * 取得今天的最晚时间，并按照指定格式转换成 String 类型
     *
     * @param format 转换的格式
     * @return 转换后的今天最晚时间
     */
    public static String getDayLasttime(String format) {
        return getDayLasttime(new Date(), format);
    }

    /**
     * 取得 date 所在日期的最晚时间，并按照指定格式转换成 String 类型
     *
     * @param date   需要取得最晚时间的日期
     * @param format 转换的格式
     * @return date 所在那天的最晚时间
     */
    public static String getDayLasttime(Date date, String format) {
        date = getDayFirsttime(date);
        return date2String(date, format);
    }

    /**
     * 取得 Date 类型表示的这个月的最晚时间
     *
     * @return 当月最晚时间
     */
    public static Date getMonthLasttime() {
        return getMonthLasttime(new Date());
    }

    /**
     * 取得参数 date 所在那个月的最晚时间
     *
     * @param date 需要取得最晚时间的日期
     * @return date 所在那个月的最晚时间
     */
    public static Date getMonthLasttime(Date date) {
        if (null == date) {
            return date;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(getMonthFirsttime(date));
        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.MILLISECOND, -1);
        return calendar.getTime();
    }

    /**
     * 取得这个月的最晚时间，并按照指定格式转换成 String 类型
     *
     * @param format 转换的格式
     * @return 转换后的这个月最晚时间
     */
    public static String getMonthLasttime(String format) {
        return getMonthLasttime(new Date(), format);
    }

    /**
     * 取得 date 所在月份的最晚时间，并按照指定格式转换成 String 类型
     *
     * @param date   需要取得最晚时间的日期
     * @param format 转换的格式
     * @return date 所在那个月的最晚时间
     */
    public static String getMonthLasttime(Date date, String format) {
        date = getMonthLasttime(date);
        return date2String(date, format);
    }

    public static long getTodayAsLong(String format) throws ParseException {
        Date date = new Date();
        long dateLong = date.getTime();
        String dateStr = longToString(dateLong, format);
        date = string2date(dateStr, format);
        return date.getTime();
    }

    /**
     * 返回前几天的时间字符串
     *
     * @param otherDay 天数
     * @return
     * @throws ParseException
     * @throws ParseException
     */
    public static String getTheOtherDay(int otherDay) throws ParseException {
        long subTime = 1000 * 60 * 60 * 24 * otherDay;

        long todayLong = getTodayAsLong(yyyy_MM_dd_hh_mm_ss);

        long result = todayLong - subTime;
        Date resultDate = new Date(result);
        String resultStr = date2String(resultDate, yyyy_MM_dd_hh_mm_ss);
        return resultStr;

    }

    /**
     * data1在data2之后
     *
     * @param date1
     * @param date2
     * @param format
     * @return
     * @throws ParseException
     */
    public static boolean after(String date1, String date2, String format)
            throws ParseException {
        long date1Long = string2long(date1, format);
        long date2Long = string2long(date2, format);
        return after(date1Long, date2Long);
    }

    /**
     * data1在data2之后
     *
     * @param date1
     * @param date2
     * @return
     */
    public static boolean after(Date date1, Date date2) {
        long date1Long = date1.getTime();
        long date2Long = date1.getTime();
        return after(date1Long, date2Long);
    }

    /**
     * data1在data2之后
     *
     * @param data1
     * @param date2
     * @param format
     * @return
     * @throws ParseException
     */
    public static boolean after(Date data1, String date2, String format)
            throws ParseException {
        long date1Long = data1.getTime();
        long date2Long = string2long(date2, format);
        return after(date1Long, date2Long);
    }

    /**
     * data1在data2之后
     *
     * @param data1
     * @param date2
     * @return
     * @throws ParseException
     */
    public static boolean after(Date data1, long date2) throws ParseException {
        long date1Long = data1.getTime();
        return after(date1Long, date2);
    }

    /**
     * data1在data2之后
     *
     * @param date1
     * @param date2
     * @param format
     * @return
     * @throws ParseException
     */
    public static boolean after(long date1, String date2, String format)
            throws ParseException {
        long date2Long = string2long(date2, format);
        return after(date1, date2Long);
    }

    /**
     * 比较两个日期.如果data1>data2返回true; data1在data2之后
     *
     * @param data1
     * @param data2
     * @return
     */
    public static boolean after(long data1, long data2) {
        if (data1 - data2 > 0) {
            return true;
        } else {
            return false;
        }
    }

    public static String getCurrent() {

        return date2String(new Date(), yyyy_MM_dd_hh_mm_ss);
    }

//    public static double getUseHour(long date1, long date2) {
//        Double temp = new Double(date2 - date1).doubleValue();
//        temp = NumUtils.div(temp, 3600000, 2);
//        return temp;
//    }
//
//    public static double getUseHour(Date date1, Date date2)
//            throws ParseException {
//        long date1Long = date1.getTime();
//        long date2Long = date2.getTime();
//        return getUseHour(date1Long, date2Long);
//    }
//
//    public static double getUseHour(String date1, String date2, String format)
//            throws ParseException {
//        long date1Long = string2long(date1, format);
//        long date2Long = string2long(date2, format);
//        return getUseHour(date1Long, date2Long);
//    }

    public static String getISODateFormat(Date date) {
        String result;
        if (date == null) {
            result = "";
        } else {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
            result = formatter.format(date);
        }
        return result;
    }

    public static String getISODatetimeFormat(Date date) {
        String result;
        if (date == null) {
            result = "";
        } else {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            result = formatter.format(date);
        }
        return result;
    }

    public static Date parseISODateFormat(String dateString) throws ParseException {
        Date result;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        if (dateString == null || dateString.trim().isEmpty()) {
            result = null;
        } else {
            result = formatter.parse(dateString);
        }
        return result;
    }

    public static Date parseISODatetimeFormat(String dateString) throws ParseException {
        Date result;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        if (dateString == null || dateString.trim().isEmpty()) {
            result = null;
        } else {
            result = formatter.parse(dateString);
        }
        return result;
    }

    public static String printDateTime(Date dt) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return df.format(dt);
    }

    public static void main(String[] args) throws ParseException {
        String str = "1704882713000";
        long timestamp = Long.parseLong(str);
        Instant instant = Instant.ofEpochMilli(timestamp);
        // 指定时区
        ZoneId zoneId = ZoneId.of("UTC"); // 替换为您的时区ID
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, zoneId);
        // 定义日期时间格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        // 格式化日期时间为字符串
        String timeString = localDateTime.format(formatter);

        System.out.println("时间字符串：" + timeString);
    }
}

