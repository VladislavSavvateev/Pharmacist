package vs.pharmacist.objects;

import android.support.annotation.Nullable;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class Recipe implements Serializable {
    private Medicine medicine;
    private ArrayList<Time> time;
    private int count;

    public Recipe(Medicine medicine, ArrayList<Time> time, int count) {
        this.medicine = medicine;
        this.time = time;
        this.count = count;
    }

    public Medicine getMedicine() {
        return medicine;
    }
    public ArrayList<Time> getTime() {
        return time;
    }
    public int getCount() {
        return count;
    }

    public void setMedicine(Medicine medicine) {
        this.medicine = medicine;
    }
    public void setTime(ArrayList<Time> time) {
        this.time = time;
    }
    public void setCount(int count) {
        this.count = count;
    }

    public static class Time implements Serializable {
        private byte dayInMonth, dayInWeek, hour, minute;

        public Time(byte dayInMonth, byte hour, byte minute, @Nullable Object shit) {
            this.hour = hour;
            this.minute = minute;
            this.dayInMonth = dayInMonth;
        }

        public Time(byte dayInWeek, byte hour, byte minute) {
            this.hour = hour;
            this.minute = minute;
            this.dayInWeek = dayInWeek;
        }

        public Time(byte hour, byte minute) {
            this.hour = hour;
            this.minute = minute;
        }

        public byte getHour() {
            return hour;
        }
        public byte getMinute() {
            return minute;
        }
        public byte getDayInMonth() {
            return dayInMonth;
        }
        public byte getDayInWeek() {
            return dayInWeek;
        }

        public void setHour(byte hour) {
            this.hour = hour;
        }
        public void setMinute(byte minute) {
            this.minute = minute;
        }
        public void setDayInMonth(byte dayInMonth) {
            this.dayInMonth = dayInMonth;
        }
        public void setDayInWeek(byte dayInWeek) {
            this.dayInWeek = dayInWeek;
        }

        public int compareToNow() {
            Date d = new Date(System.currentTimeMillis());
            d.setHours(hour);
            d.setMinutes(minute);
            d.setSeconds(0);

            Date d1 = new Date(System.currentTimeMillis());
            if (dayInWeek == 0 && dayInMonth == 0) {
                return compareTwoDates(d, d1);
            }
            if (dayInMonth != 0) {
                d.setDate(dayInMonth);
                return compareTwoDates(d, d1);
            } else {
                Calendar c = Calendar.getInstance();
                c.setTime(d);
                int dayofweek = c.get(Calendar.DAY_OF_WEEK) - 1;
                if (dayofweek == 0) dayofweek = 7;
                while (dayofweek == dayInWeek) {
                    c.add(Calendar.DATE, 1);
                    dayofweek = c.get(Calendar.DAY_OF_WEEK) - 1;
                    if (dayofweek == 0) dayofweek = 7;
                }
                return compareTwoDates(d, d1);
            }
        }

        int compareTwoDates(Date d1, Date d2) {
            /*String debug = "";
            debug += Integer.toString(d1.getMonth()) + "\t" + Integer.toString(d2.getMonth()) + "\n";
            debug += Integer.toString(d1.getDate()) + "\t" + Integer.toString(d2.getDate()) + "\n";
            debug += Integer.toString(d1.getHours()) + "\t" + Integer.toString(d2.getHours()) + "\n";
            debug += Integer.toString(d1.getMinutes()) + "\t" + Integer.toString(d2.getMinutes()) + "\n";
            Log.d("debug", "compareTwoDates: \n" + debug);*/
            if ((d1.getMonth() == d2.getMonth()) && (d1.getDate() == d2.getDate()) &&
                    (d1.getHours() == d2.getHours()) && (d1.getMinutes() == d2.getMinutes()))
                return 0;
            else return d1.compareTo(d2);
        }

        @Override
        public String toString() {
            String result = "";
            if (dayInWeek != 0)
                result += weekDays[dayInWeek - 1] + ", ";
            if (dayInMonth != 0)
                result += dayInMonth + " день, ";
            if (hour < 10) result += "0";
            result += hour;
            result += ":";
            if (minute < 10) result += "0";
            result += minute;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            Time t = (Time) obj;
            return (t.dayInMonth == this.dayInMonth && t.dayInWeek == this.dayInWeek &&
                    t.hour == this.hour && t.minute == this.minute);
        }

        final String[] weekDays = {"понедельник", "вторник", "среда", "четверг", "пятница", "суббота", "воскресенье"};
    }

    @Override
    public String toString() {
        if (time.get(0).dayInWeek == 0 && time.get(0).dayInMonth == 0)
            return "\"" + medicine.getName() + "\". Раз в день: " + time.size();
        else if (time.get(0).dayInMonth == 0)
            return "\"" + medicine.getName() + "\". Раз в неделю: " + time.size();
        else return "\"" + medicine.getName() + "\". Раз в месяц: " + time.size();
    }

    @Override
    public boolean equals(Object obj) {
        Recipe r = (Recipe) obj;
        if (r.getTime().size() != this.getTime().size()) return false;
        for (int i = 0; i < r.getTime().size(); i++)
            if (!r.getTime().get(i).equals(this.getTime().get(i))) return false;
        if (r.getMedicine().equals(this.getMedicine())) return false;
        if (r.getCount() != this.getCount()) return false;
        return true;
    }
}

