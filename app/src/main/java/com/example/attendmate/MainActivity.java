package com.example.attendmate;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private LinearLayout subjectListContainer;
    private TextView txtPercentage, txtSubjects, txtPresent, txtAbsent;
    private ProgressBar attendanceProgress;
    private SubjectDatabase database;

    private final int BLUE = Color.rgb(22, 119, 232);
    private final int BRIGHT_BLUE = Color.rgb(57, 165, 255);
    private final int WHITE = Color.WHITE;
    private final int LIGHT_TEXT = Color.rgb(184, 201, 226);
    private final int DARK_TEXT = Color.rgb(20, 35, 55);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        database = new SubjectDatabase(this);

        subjectListContainer = findViewById(R.id.subjectListContainer);
        txtPercentage = findViewById(R.id.txtPercentage);
        txtSubjects = findViewById(R.id.txtSubjects);
        txtPresent = findViewById(R.id.txtPresent);
        txtAbsent = findViewById(R.id.txtAbsent);
        attendanceProgress = findViewById(R.id.attendanceProgress);

        Button btnAddSubject = findViewById(R.id.btnAddSubject);
        btnAddSubject.setOnClickListener(v -> showAddSubjectDialog());

        loadSubjects();
        updateDashboard();
    }

    private void showAddSubjectDialog() {
        EditText input = createInput("Enter subject name");

        new AlertDialog.Builder(this)
                .setTitle("Add New Subject")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = input.getText().toString().trim();

                    if (name.isEmpty()) {
                        showToast("Please enter a subject name");
                        return;
                    }

                    if (database.addSubject(name)) {
                        showToast("Subject added successfully");
                        loadSubjects();
                        updateDashboard();
                    } else {
                        showToast("This subject already exists");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private EditText createInput(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setTextColor(DARK_TEXT);
        input.setHintTextColor(Color.rgb(105, 120, 140));
        input.setPadding(24, 16, 24, 16);
        input.setBackgroundTintList(
                ColorStateList.valueOf(BRIGHT_BLUE)
        );
        return input;
    }

    private void loadSubjects() {
        subjectListContainer.removeAllViews();

        ArrayList<Subject> subjects = database.getAllSubjects();

        if (subjects.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No subjects added yet.\nTap Add Subject to get started.");
            empty.setTextColor(LIGHT_TEXT);
            empty.setTextSize(14);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(16, 32, 16, 32);
            subjectListContainer.addView(empty);
            return;
        }

        for (Subject subject : subjects) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(20, 18, 20, 18);
            card.setBackground(createRoundedBackground(
                    Color.rgb(23, 68, 119), 18
            ));

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
            params.setMargins(0, 0, 0, 14);
            card.setLayoutParams(params);

            TextView name = new TextView(this);
            name.setText(subject.name);
            name.setTextColor(WHITE);
            name.setTextSize(17);
            name.setTypeface(null, Typeface.BOLD);

            TextView details = new TextView(this);
            details.setText("Tap to manage subject");
            details.setTextColor(LIGHT_TEXT);
            details.setTextSize(12);
            details.setPadding(0, 6, 0, 0);

            card.addView(name);
            card.addView(details);

            card.setOnClickListener(v -> showSubjectOptions(subject));
            subjectListContainer.addView(card);
        }
    }

    private GradientDrawable createRoundedBackground(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        drawable.setStroke(1, Color.rgb(36, 102, 168));
        return drawable;
    }

    private void showSubjectOptions(Subject subject) {
        String[] options = {
                "Mark Attendance",
                "View Attendance History",
                "Rename Subject",
                "Remove Subject"
        };

        new AlertDialog.Builder(this)
                .setTitle(subject.name)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showMarkAttendanceDialog(subject);
                    } else if (which == 1) {
                        showAttendanceHistory(subject);
                    } else if (which == 2) {
                        showRenameSubjectDialog(subject);
                    } else {
                        showRemoveSubjectDialog(subject);
                    }
                })
                .show();
    }

    private void showRenameSubjectDialog(Subject subject) {
        EditText input = createInput("Enter new subject name");
        input.setText(subject.name);

        new AlertDialog.Builder(this)
                .setTitle("Rename Subject")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText().toString().trim();

                    if (newName.isEmpty()) {
                        showToast("Please enter a subject name");
                        return;
                    }

                    if (database.updateSubject(subject.id, newName)) {
                        showToast("Subject renamed successfully");
                        loadSubjects();
                    } else {
                        showToast("This subject name already exists");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRemoveSubjectDialog(Subject subject) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Subject")
                .setMessage("Are you sure you want to remove " +
                        subject.name + " and its attendance records?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    if (database.deleteSubject(subject.id)) {
                        showToast("Subject removed successfully");
                        loadSubjects();
                        updateDashboard();
                    } else {
                        showToast("Unable to remove subject");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showMarkAttendanceDialog(Subject subject) {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);

                    String date = formatDate(selected);
                    showAttendanceStatusDialog(subject, date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePicker.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePicker.setTitle("Select Lecture Date");
        datePicker.show();
    }

    private void showAttendanceStatusDialog(Subject subject, String date) {
        String[] statuses = {"Present", "Absent"};

        new AlertDialog.Builder(this)
                .setTitle("Mark Attendance - " + date)
                .setItems(statuses, (dialog, which) -> {
                    saveAttendance(subject, date, statuses[which]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveAttendance(
            Subject subject, String date, String status) {

        String existingStatus =
                database.getAttendanceStatus(subject.id, date);

        if (existingStatus != null &&
                existingStatus.equals(status)) {
            showToast("Attendance already marked as " + status + "!");
            return;
        }

        boolean saved = database.saveAttendance(
                subject.id, date, status);

        if (saved) {
            if (existingStatus != null) {
                showToast("Attendance updated successfully");
            } else {
                showToast("Attendance saved successfully");
            }
            loadSubjects();
            updateDashboard();
        } else {
            showToast("Unable to save attendance");
        }
    }

    // ---------------- ATTENDANCE HISTORY DESIGN ----------------

    private void showAttendanceHistory(Subject subject) {
        ArrayList<AttendanceRecord> records =
                database.getAttendanceHistory(subject.id);

        LinearLayout historyLayout = new LinearLayout(this);
        historyLayout.setOrientation(LinearLayout.VERTICAL);
        historyLayout.setPadding(16, 12, 16, 12);

        if (records.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No attendance records available.");
            empty.setTextColor(DARK_TEXT);
            empty.setTextSize(15);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(12, 35, 12, 35);
            historyLayout.addView(empty);
        } else {
            for (AttendanceRecord record : records) {
                historyLayout.addView(
                        createHistoryCard(subject, record)
                );
            }
        }

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.addView(historyLayout);

        new AlertDialog.Builder(this)
                .setTitle(subject.name + " - Attendance History")
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .show();
    }

    private LinearLayout createHistoryCard(
            Subject subject, AttendanceRecord record) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 15, 16, 15);

        int cardColor = record.status.equals("Present")
                ? Color.rgb(230, 250, 239)
                : Color.rgb(255, 235, 239);

        card.setBackground(createRoundedBackground(cardColor, 14));

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        cardParams.setMargins(0, 0, 0, 12);
        card.setLayoutParams(cardParams);

        // Date and status row
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView dateText = new TextView(this);
        dateText.setText(record.date);
        dateText.setTextColor(DARK_TEXT);
        dateText.setTextSize(15);
        dateText.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams dateParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );
        dateText.setLayoutParams(dateParams);

        TextView statusBadge = new TextView(this);
        statusBadge.setText(record.status.toUpperCase(Locale.getDefault()));
        statusBadge.setTextSize(11);
        statusBadge.setTypeface(null, Typeface.BOLD);
        statusBadge.setGravity(Gravity.CENTER);
        statusBadge.setPadding(13, 8, 13, 8);

        if (record.status.equals("Present")) {
            statusBadge.setTextColor(Color.rgb(15, 115, 65));
            statusBadge.setBackground(
                    createRoundedBackground(
                            Color.rgb(190, 240, 211), 30
                    )
            );
        } else {
            statusBadge.setTextColor(Color.rgb(175, 45, 75));
            statusBadge.setBackground(
                    createRoundedBackground(
                            Color.rgb(255, 205, 216), 30
                    )
            );
        }

        topRow.addView(dateText);
        topRow.addView(statusBadge);

        // Edit and Delete buttons
        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.CENTER_VERTICAL);
        buttonRow.setPadding(0, 14, 0, 0);

        Button editButton = new Button(this);
        editButton.setText("Edit");
        editButton.setTextSize(12);
        editButton.setAllCaps(false);
        editButton.setTextColor(WHITE);
        editButton.setBackgroundTintList(
                ColorStateList.valueOf(BLUE)
        );

        LinearLayout.LayoutParams editParams =
                new LinearLayout.LayoutParams(
                        0,
                        dp(42),
                        1
                );
        editParams.setMargins(0, 0, 6, 0);
        editButton.setLayoutParams(editParams);

        Button deleteButton = new Button(this);
        deleteButton.setText("Delete");
        deleteButton.setTextSize(12);
        deleteButton.setAllCaps(false);
        deleteButton.setTextColor(WHITE);
        deleteButton.setBackgroundTintList(
                ColorStateList.valueOf(Color.rgb(205, 65, 90))
        );

        LinearLayout.LayoutParams deleteParams =
                new LinearLayout.LayoutParams(
                        0,
                        dp(42),
                        1
                );
        deleteParams.setMargins(6, 0, 0, 0);
        deleteButton.setLayoutParams(deleteParams);

        editButton.setOnClickListener(v ->
                showEditAttendanceDatePicker(subject, record)
        );

        deleteButton.setOnClickListener(v ->
                showDeleteAttendanceConfirmation(subject, record)
        );

        buttonRow.addView(editButton);
        buttonRow.addView(deleteButton);

        card.addView(topRow);
        card.addView(buttonRow);

        return card;
    }

    private int dp(int value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    // ---------------- EDIT ATTENDANCE ----------------

    private void showEditAttendanceDatePicker(
            Subject subject, AttendanceRecord record) {

        Calendar calendar = Calendar.getInstance();

        try {
            Date parsedDate = new SimpleDateFormat(
                    "yyyy-MM-dd", Locale.getDefault()
            ).parse(record.date);

            if (parsedDate != null) {
                calendar.setTime(parsedDate);
            }
        } catch (ParseException e) {
            showToast("Unable to read the attendance date");
            return;
        }

        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);

                    String newDate = formatDate(selected);

                    showEditAttendanceStatusDialog(
                            subject, record, newDate
                    );
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePicker.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePicker.setTitle("Choose Correct Date");
        datePicker.show();
    }

    private void showEditAttendanceStatusDialog(
            Subject subject,
            AttendanceRecord record,
            String newDate) {

        String[] statuses = {"Present", "Absent"};

        new AlertDialog.Builder(this)
                .setTitle("Select Attendance Status")
                .setSingleChoiceItems(
                        statuses,
                        record.status.equals("Present") ? 0 : 1,
                        (dialog, which) -> {
                            String newStatus = statuses[which];

                            int result = database.updateAttendance(
                                    subject.id,
                                    record.date,
                                    newDate,
                                    newStatus
                            );

                            dialog.dismiss();

                            if (result == 1) {
                                showToast("Attendance updated successfully");
                                loadSubjects();
                                updateDashboard();
                                showAttendanceHistory(subject);
                            } else if (result == -1) {
                                showToast(
                                        "An attendance record already exists for this date"
                                );
                            } else {
                                showToast("Unable to update attendance");
                            }
                        }
                )
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ---------------- DELETE ATTENDANCE ----------------

    private void showDeleteAttendanceConfirmation(
            Subject subject, AttendanceRecord record) {

        new AlertDialog.Builder(this)
                .setTitle("Delete Attendance")
                .setMessage("Are you sure you want to delete the " +
                        record.status + " record for " + record.date + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (database.deleteAttendance(
                            subject.id, record.date)) {

                        showToast("Attendance record deleted");
                        loadSubjects();
                        updateDashboard();
                        showAttendanceHistory(subject);

                    } else {
                        showToast("Unable to delete attendance");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String formatDate(Calendar calendar) {
        return new SimpleDateFormat(
                "yyyy-MM-dd", Locale.getDefault()
        ).format(calendar.getTime());
    }

    // ---------------- DASHBOARD ----------------

    private void updateDashboard() {
        int subjectCount = database.getSubjectCount();
        int presentCount = database.getAttendanceCount("Present");
        int absentCount = database.getAttendanceCount("Absent");

        int total = presentCount + absentCount;
        int percentage = total == 0
                ? 0
                : Math.round((presentCount * 100f) / total);

        txtSubjects.setText(String.valueOf(subjectCount));
        txtPresent.setText(String.valueOf(presentCount));
        txtAbsent.setText(String.valueOf(absentCount));
        txtPercentage.setText(percentage + "%");
        attendanceProgress.setProgress(percentage);
    }

    private void showToast(String message) {
        Toast.makeText(
                this, message, Toast.LENGTH_SHORT
        ).show();
    }

    // ---------------- DATABASE ----------------

    private static class SubjectDatabase extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "AttendMate.db";
        private static final int DATABASE_VERSION = 2;

        SubjectDatabase(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(
                    "CREATE TABLE subjects (" +
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
                SQLiteDatabase db, int oldVersion, int newVersion) {

            if (oldVersion < 2) {
                createAttendanceTable(db);
            }
        }

        boolean addSubject(String name) {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("name", name);

            return db.insert("subjects", null, values) != -1;
        }

        boolean updateSubject(int id, String newName) {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("name", newName);

            return db.update(
                    "subjects",
                    values,
                    "id=?",
                    new String[]{String.valueOf(id)}
            ) > 0;
        }

        boolean deleteSubject(int id) {
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();

            try {
                db.delete(
                        "attendance",
                        "subject_id=?",
                        new String[]{String.valueOf(id)}
                );

                int deleted = db.delete(
                        "subjects",
                        "id=?",
                        new String[]{String.valueOf(id)}
                );

                db.setTransactionSuccessful();
                return deleted > 0;

            } finally {
                db.endTransaction();
            }
        }

        ArrayList<Subject> getAllSubjects() {
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

        int getSubjectCount() {
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.rawQuery(
                    "SELECT COUNT(*) FROM subjects", null
            );

            int count = 0;

            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }

            cursor.close();
            return count;
        }

        boolean saveAttendance(
                int subjectId, String date, String status) {

            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("subject_id", subjectId);
            values.put("date", date);
            values.put("status", status);

            return db.insertWithOnConflict(
                    "attendance",
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
            ) != -1;
        }

        String getAttendanceStatus(int subjectId, String date) {
            SQLiteDatabase db = getReadableDatabase();

            Cursor cursor = db.rawQuery(
                    "SELECT status FROM attendance " +
                            "WHERE subject_id=? AND date=?",
                    new String[]{
                            String.valueOf(subjectId), date
                    }
            );

            String status = null;

            if (cursor.moveToFirst()) {
                status = cursor.getString(0);
            }

            cursor.close();
            return status;
        }

        ArrayList<AttendanceRecord> getAttendanceHistory(int subjectId) {
            ArrayList<AttendanceRecord> records = new ArrayList<>();
            SQLiteDatabase db = getReadableDatabase();

            Cursor cursor = db.rawQuery(
                    "SELECT date, status FROM attendance " +
                            "WHERE subject_id=? ORDER BY date DESC",
                    new String[]{String.valueOf(subjectId)}
            );

            try {
                while (cursor.moveToNext()) {
                    records.add(new AttendanceRecord(
                            cursor.getString(0),
                            cursor.getString(1)
                    ));
                }
            } finally {
                cursor.close();
            }

            return records;
        }

        int updateAttendance(
                int subjectId,
                String oldDate,
                String newDate,
                String newStatus) {

            SQLiteDatabase db = getWritableDatabase();

            Cursor cursor = db.rawQuery(
                    "SELECT id FROM attendance " +
                            "WHERE subject_id=? AND date=?",
                    new String[]{
                            String.valueOf(subjectId), newDate
                    }
            );

            boolean duplicate = cursor.moveToFirst()
                    && !oldDate.equals(newDate);

            cursor.close();

            if (duplicate) {
                return -1;
            }

            ContentValues values = new ContentValues();
            values.put("date", newDate);
            values.put("status", newStatus);

            return db.update(
                    "attendance",
                    values,
                    "subject_id=? AND date=?",
                    new String[]{
                            String.valueOf(subjectId), oldDate
                    }
            ) > 0 ? 1 : 0;
        }

        boolean deleteAttendance(int subjectId, String date) {
            SQLiteDatabase db = getWritableDatabase();

            return db.delete(
                    "attendance",
                    "subject_id=? AND date=?",
                    new String[]{
                            String.valueOf(subjectId), date
                    }
            ) > 0;
        }

        int getAttendanceCount(String status) {
            SQLiteDatabase db = getReadableDatabase();

            Cursor cursor = db.rawQuery(
                    "SELECT COUNT(*) FROM attendance WHERE status=?",
                    new String[]{status}
            );

            int count = 0;

            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }

            cursor.close();
            return count;
        }
    }

    private static class Subject {
        int id;
        String name;

        Subject(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private static class AttendanceRecord {
        String date;
        String status;

        AttendanceRecord(String date, String status) {
            this.date = date;
            this.status = status;
        }
    }
}
