package edu.byui.cs246.team11.bankon.sqliteprovider;

import android.database.Cursor;
import edu.byui.cs246.team11.bankon.core.RepositoryException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

abstract class BaseRepository {

    protected DatabaseHelper databaseHelper;

    public static final String TAG = BaseRepository.class.getName();
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    public BaseRepository(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    protected Calendar stringToCalendar(String value) throws RepositoryException {
        try {
            Date date = new SimpleDateFormat(DATE_TIME_FORMAT).parse(value);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            return calendar;
        } catch (Exception e) {
            throw new RepositoryException("StringToCalendar failed", e);
        }
    }

    protected String calendarToString(Calendar value) {
        return new SimpleDateFormat(DATE_TIME_FORMAT).format(value.getTime());
    }

    protected abstract class ObjectLoader<T> {

        public T ObjectFromCursor(Cursor cursor) throws RepositoryException {
            if (!cursor.moveToFirst()) return null;
            return FromCursor(cursor);
        }

        public List<T> ObjectsFromCursor(Cursor cursor) throws RepositoryException {
            List<T> list = new ArrayList<T>();
            while (cursor.moveToNext()) {
                list.add(FromCursor(cursor));
            }
            return list;
        }

        protected abstract T FromCursor(Cursor cursor) throws RepositoryException;
    }
}
