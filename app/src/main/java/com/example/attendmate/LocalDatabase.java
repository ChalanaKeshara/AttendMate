
package com.example.attendmate;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class LocalDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "AttendMate.db";
    private static final int DATABASE_VERSION = 2;

    public LocalDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS subjects (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT NOT NULL COLLATE NOCASE UNIQUE)"
        );

        createAttendanceTable(db);
    }

    private void createAttendanceTable(SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS attendance (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "subject_id INTEGER NOT NULL, " +
                        "date TEXT NOT NULL, " +
                        "status TEXT NOT NULL, " +
                        "UNIQUE(subject_id, date), " +
                        "FOREIGN KEY(subject_id) REFERENCES subjects(id))"
        );
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion) {

        if (oldVersion < 2) {
            createAttendanceTable(db);
        }
    }

    // Get all old subjects
    public ArrayList<Subject> getAllSubjects() {

        ArrayList<Subject> subjects = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT id, name FROM subjects ORDER BY name COLLATE NOCASE",
                null
        );

        try {
            while (cursor.moveToNext()) {

                subjects.add(new Subject(
                        cursor.getInt(0),
                        cursor.getString(1)
                ));
            }
        } finally {
            cursor.close();
        }

        return subjects;
    }

    // Get all old attendance records
    public ArrayList<AttendanceRecord> getAllAttendance() {

        ArrayList<AttendanceRecord> records = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT subject_id, date, status FROM attendance ORDER BY date",
                null
        );

        try {
            while (cursor.moveToNext()) {

                records.add(new AttendanceRecord(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2)
                ));
            }
        } finally {
            cursor.close();
        }

        return records;
    }

    // Subject model
    public static class Subject {

        public int id;
        public String name;

        public Subject(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    // Attendance model
    public static class AttendanceRecord {

        public int subjectId;
        public String date;
        public String status;

        public AttendanceRecord(
                int subjectId,
                String date,
                String status) {

            this.subjectId = subjectId;
            this.date = date;
            this.status = status;
        }
    }
}
