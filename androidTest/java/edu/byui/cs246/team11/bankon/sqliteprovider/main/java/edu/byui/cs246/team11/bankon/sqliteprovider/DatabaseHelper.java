package edu.byui.cs246.team11.bankon.sqliteprovider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.text.TextUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

class DatabaseHelper extends android.database.sqlite.SQLiteOpenHelper {
    public static final String DATABASE_NAME = "bankon.db";
    public static final int DATABASE_VERSION = 1;
    public static final String SCRIPT_CREATE_DATABASE = "create_database.sql";
    public static final String TAG = DatabaseHelper.class.getName();

    private Context context;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            executeScript(db, getScript(SCRIPT_CREATE_DATABASE));
        } catch (IOException e) {
            throw new LocalSQLiteException("Unable to create database", e);
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            for (int i = oldVersion; i < newVersion; i++) {
                executeScript(db, getUpdateScript(i, i + 1));
            }
        } catch (IOException e) {
            throw new LocalSQLiteException("Unable to update database", e);
        }
    }

    public void executeScript(SQLiteDatabase db, String script) {
        db.beginTransaction();
        try {
            String[] commands = splitScript(script);
            for (String command : commands) {
                db.execSQL(command);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private String getScript(String fileName) throws IOException {
        InputStream inputStream = context.getResources().getAssets().open(fileName);
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            ArrayList<String> lines = new ArrayList<String>();
            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line);
            }
            return TextUtils.join("\n", lines);
        } finally {
            inputStream.close();
        }
    }

    private String getUpdateScript(int oldVersion, int newVersion) throws IOException {
        return getScript(String.format(Locale.getDefault(), "update_%d_%d.sql", oldVersion, newVersion));
    }

    public String[] splitScript(String script) {
        ArrayList<String> list = new ArrayList<String>();
        Scanner scanner = new Scanner(script);
        try {
            String command = "";
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith("--") || line.isEmpty()) continue;
                command += (command.isEmpty() ? "" : "\n") + line;
                if (command.endsWith(";")) {
                    list.add(command);
                    command = "";
                }
            }
        } finally {
            scanner.close();
        }
        return list.toArray(new String[0]);
    }

    public static class LocalSQLiteException extends SQLiteException {

        public LocalSQLiteException(String error, Exception innerException) {
            super(error);
            this.setStackTrace(innerException.getStackTrace());
        }

    }
}
