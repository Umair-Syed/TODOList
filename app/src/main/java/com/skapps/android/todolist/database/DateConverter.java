package com.skapps.android.todolist.database;

import java.util.Date;

import androidx.room.TypeConverter;

public class DateConverter {
    @TypeConverter
    public static Date toDate(Long timescamp) {
        return timescamp == null ? null : new Date(timescamp);
    }

    @TypeConverter
    public static Long toTimeStamp(Date date) {return date == null ? null : date.getTime();}

}
