package com.example.attendmate;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private String uid;

    private LinearLayout subjectListContainer;
    private TextView txtPercentage, txtSubjects, txtPresent, txtAbsent;
    private CircularProgressView circularProgress;

    private final int WHITE = Color.WHITE;
    private final int LIGHT_TEXT = Color.rgb(184, 201, 226);
    private final int BLUE = Color.rgb(22, 119, 232);
    private final int DARK_TEXT = Color.rgb(20, 35, 55);

    private final List<Subject> subjects = new ArrayList<>();
    private final List<AttendanceRecord> attendanceRecords =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            openLogin();
            return;
        }

        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        uid = auth.getCurrentUser().getUid();

        subjectListContainer = findViewById(R.id.subjectListContainer);
        txtPercentage = findViewById(R.id.txtPercentage);
        txtSubjects = findViewById(R.id.txtSubjects);
        txtPresent = findViewById(R.id.txtPresent);
        txtAbsent = findViewById(R.id.txtAbsent);
        circularProgress = findViewById(R.id.attendanceCircularProgress);

        Button addButton = findViewById(R.id.btnAddSubject);
        addButton.setOnClickListener(v -> showAddSubjectDialog());

        Button logoutButton = findViewById(R.id.btnLogout);
        logoutButton.setOnClickListener(v -> logout());

        migrateOldData();
    }

    private void openLogin() {
        startActivity(new android.content.Intent(
                this, LoginActivity.class));
        finish();
    }

    private void logout() {
        auth.signOut();
        openLogin();
    }

    // ---------------- FIREBASE PATHS ----------------

    private com.google.firebase.firestore.CollectionReference
    subjectsRef() {
        return firestore.collection("users")
                .document(uid).collection("subjects");
    }

    private com.google.firebase.firestore.CollectionReference
    attendanceRef() {
        return firestore.collection("users")
                .document(uid).collection("attendance");
    }

    private com.google.firebase.firestore.DocumentReference
    migrationRef() {
        return firestore.collection("users")
                .document(uid).collection("metadata")
                .document("migration");
    }

    // ---------------- OLD SQLITE MIGRATION ----------------

    private void migrateOldData() {

        migrationRef().get().addOnSuccessListener(marker -> {

            if (Boolean.TRUE.equals(
                    marker.getBoolean("sqliteV2Imported"))) {
                loadFirebaseData();
                return;
            }

            LocalDatabase localDB = new LocalDatabase(this);

            ArrayList<LocalDatabase.Subject> oldSubjects =
                    localDB.getAllSubjects();

            ArrayList<LocalDatabase.AttendanceRecord> oldRecords =
                    localDB.getAllAttendance();

            WriteBatch batch = firestore.batch();

            for (LocalDatabase.Subject old : oldSubjects) {

                String subjectId = String.valueOf(old.id);

                Map<String, Object> subjectData = new HashMap<>();
                subjectData.put("id", subjectId);
                subjectData.put("name", old.name);

                batch.set(subjectsRef().document(subjectId), subjectData);
            }

            for (LocalDatabase.AttendanceRecord old : oldRecords) {

                String subjectId = String.valueOf(old.subjectId);
                String documentId = subjectId + "_" + old.date;

                Map<String, Object> attendanceData = new HashMap<>();
                attendanceData.put("id", documentId);
                attendanceData.put("subjectId", subjectId);
                attendanceData.put("date", old.date);
                attendanceData.put("status", old.status);

                batch.set(attendanceRef().document(documentId),
                        attendanceData);
            }

            batch.commit().addOnSuccessListener(unused -> {

                Map<String, Object> migrationData = new HashMap<>();
                migrationData.put("sqliteV2Imported", true);
                migrationData.put("migratedAt", FieldValue.serverTimestamp());

                migrationRef().set(migrationData)
                        .addOnSuccessListener(result -> {
                            Toast.makeText(this,
                                    "Old records migrated successfully",
                                    Toast.LENGTH_LONG).show();
                            loadFirebaseData();
                        })
                        .addOnFailureListener(error ->
                                showError("Migration marker failed: "
                                        + error.getMessage()));

            }).addOnFailureListener(error ->
                    showError("Migration failed. Old records are safe. "
                            + error.getMessage()));

        }).addOnFailureListener(error ->
                showError("Unable to check migration: "
                        + error.getMessage()));
    }

    // ---------------- LOAD FIREBASE DATA ----------------

    private void loadFirebaseData() {

        subjectsRef().get().addOnSuccessListener(subjectSnapshot -> {

            subjects.clear();

            for (DocumentSnapshot document : subjectSnapshot) {
                Subject subject = new Subject(
                        document.getId(),
                        document.getString("name"));
                subjects.add(subject);
            }

            attendanceRef().get().addOnSuccessListener(attSnapshot -> {

                attendanceRecords.clear();

                for (DocumentSnapshot document : attSnapshot) {
                    AttendanceRecord record = new AttendanceRecord(
                            document.getString("subjectId"),
                            document.getString("date"),
                            document.getString("status"));
                    attendanceRecords.add(record);
                }

                Collections.sort(attendanceRecords,
                        (a, b) -> b.date.compareTo(a.date));

                loadSubjects();
                updateDashboard();

            }).addOnFailureListener(error ->
                    showError(error.getMessage()));

        }).addOnFailureListener(error ->
                showError(error.getMessage()));
    }

    // ---------------- ADD SUBJECT ----------------

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

                    for (Subject subject : subjects) {
                        if (subject.name.equalsIgnoreCase(name)) {
                            showToast("This subject already exists");
                            return;
                        }
                    }

                    String id = subjectsRef().document().getId();

                    Map<String, Object> data = new HashMap<>();
                    data.put("id", id);
                    data.put("name", name);

                    subjectsRef().document(id).set(data)
                            .addOnSuccessListener(unused -> {
                                showToast("Subject added successfully");
                                loadFirebaseData();
                            })
                            .addOnFailureListener(error ->
                                    showError(error.getMessage()));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private EditText createInput(String hint) {

        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setTextColor(DARK_TEXT);
        input.setHintTextColor(Color.GRAY);
        input.setPadding(24, 16, 24, 16);

        return input;
    }

    // ---------------- SUBJECT LIST ----------------

    private void loadSubjects() {

        subjectListContainer.removeAllViews();

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
            card.setBackground(createBackground(
                    Color.rgb(23, 68, 119), 18));

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);

            params.setMargins(0, 0, 0, 14);
            card.setLayoutParams(params);

            TextView name = new TextView(this);
            name.setText(subject.name);
            name.setTextColor(WHITE);
            name.setTextSize(17);
            name.setTypeface(null, Typeface.BOLD);

            int present = countForSubject(subject.id, "Present");
            int absent = countForSubject(subject.id, "Absent");
            int total = present + absent;

            int percentage = total == 0 ? 0 :
                    Math.round(present * 100f / total);

            TextView details = new TextView(this);
            details.setText("Attendance: " + percentage + "%   |   "
                    + present + " Present   |   "
                    + absent + " Absent");
            details.setTextColor(LIGHT_TEXT);
            details.setTextSize(12);
            details.setPadding(0, 8, 0, 0);

            card.addView(name);
            card.addView(details);

            card.setOnClickListener(v ->
                    showSubjectOptions(subject));

            subjectListContainer.addView(card);
        }
    }

    // ---------------- SUBJECT OPTIONS ----------------

    private void showSubjectOptions(Subject subject) {

        String[] options = {
                "View Subject Attendance Summary",
                "Mark Attendance",
                "View Attendance History",
                "Rename Subject",
                "Remove Subject"
        };

        new AlertDialog.Builder(this)
                .setTitle(subject.name)
                .setItems(options, (dialog, which) -> {

                    if (which == 0) {
                        showSubjectSummary(subject);
                    } else if (which == 1) {
                        showMarkAttendanceDialog(subject);
                    } else if (which == 2) {
                        showAttendanceHistory(subject);
                    } else if (which == 3) {
                        showRenameDialog(subject);
                    } else {
                        showRemoveDialog(subject);
                    }
                })
                .show();
    }

    private void showSubjectSummary(Subject subject) {

        int present = countForSubject(subject.id, "Present");
        int absent = countForSubject(subject.id, "Absent");
        int total = present + absent;

        int percentage = total == 0 ? 0 :
                Math.round(present * 100f / total);

        new AlertDialog.Builder(this)
                .setTitle(subject.name + " - Summary")
                .setMessage("Attendance Percentage: " + percentage + "%\n\n"
                        + "Total Lectures: " + total + "\n"
                        + "Present: " + present + "\n"
                        + "Absent: " + absent)
                .setPositiveButton("Close", null)
                .show();
    }

    private void showRenameDialog(Subject subject) {

        EditText input = createInput("Enter new subject name");
        input.setText(subject.name);

        new AlertDialog.Builder(this)
                .setTitle("Rename Subject")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {

                    String name = input.getText().toString().trim();

                    if (name.isEmpty()) {
                        showToast("Please enter a subject name");
                        return;
                    }

                    for (Subject item : subjects) {
                        if (!item.id.equals(subject.id)
                                && item.name.equalsIgnoreCase(name)) {
                            showToast("This subject already exists");
                            return;
                        }
                    }

                    Map<String, Object> update = new HashMap<>();
                    update.put("name", name);

                    subjectsRef().document(subject.id).update(update)
                            .addOnSuccessListener(unused -> {
                                showToast("Subject renamed successfully");
                                loadFirebaseData();
                            })
                            .addOnFailureListener(error ->
                                    showError(error.getMessage()));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRemoveDialog(Subject subject) {

        new AlertDialog.Builder(this)
                .setTitle("Remove Subject")
                .setMessage("Remove " + subject.name
                        + " and its attendance records?")
                .setPositiveButton("Remove", (dialog, which) -> {

                    attendanceRef()
                            .whereEqualTo("subjectId", subject.id)
                            .get()
                            .addOnSuccessListener(snapshot -> {

                                WriteBatch batch = firestore.batch();

                                for (DocumentSnapshot document : snapshot) {
                                    batch.delete(document.getReference());
                                }

                                batch.delete(subjectsRef()
                                        .document(subject.id));

                                batch.commit()
                                        .addOnSuccessListener(unused -> {
                                            showToast("Subject removed");
                                            loadFirebaseData();
                                        })
                                        .addOnFailureListener(error ->
                                                showError(error.getMessage()));
                            })
                            .addOnFailureListener(error ->
                                    showError(error.getMessage()));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ---------------- MARK ATTENDANCE ----------------

    private void showMarkAttendanceDialog(Subject subject) {

        Calendar calendar = Calendar.getInstance();

        DatePickerDialog picker = new DatePickerDialog(this,
                (view, year, month, day) -> {

                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, day);

                    String date = new SimpleDateFormat(
                            "yyyy-MM-dd", Locale.getDefault())
                            .format(selected.getTime());

                    showAttendanceStatusDialog(subject, date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        picker.getDatePicker().setMaxDate(System.currentTimeMillis());
        picker.setTitle("Select Lecture Date");
        picker.show();
    }

    private void showAttendanceStatusDialog(
            Subject subject, String date) {

        String[] statuses = {"Present", "Absent"};

        new AlertDialog.Builder(this)
                .setTitle("Mark Attendance - " + date)
                .setItems(statuses, (dialog, which) ->
                        saveAttendance(subject, date, statuses[which]))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String attendanceDocumentId(String subjectId, String date) {
        return subjectId + "_" + date;
    }

    private void saveAttendance(
            Subject subject, String date, String status) {

        String documentId = attendanceDocumentId(subject.id, date);

        Map<String, Object> data = new HashMap<>();
        data.put("id", documentId);
        data.put("subjectId", subject.id);
        data.put("date", date);
        data.put("status", status);

        attendanceRef().document(documentId).get()
                .addOnSuccessListener(document -> {

                    if (document.exists()
                            && status.equals(document.getString("status"))) {
                        showToast("Already marked as " + status);
                        return;
                    }

                    attendanceRef().document(documentId).set(data)
                            .addOnSuccessListener(unused -> {
                                showToast("Attendance saved");
                                loadFirebaseData();
                            })
                            .addOnFailureListener(error ->
                                    showError(error.getMessage()));
                })
                .addOnFailureListener(error ->
                        showError(error.getMessage()));
    }

    // ---------------- ATTENDANCE HISTORY ----------------

    private void showAttendanceHistory(Subject subject) {

        List<AttendanceRecord> records = new ArrayList<>();

        for (AttendanceRecord record : attendanceRecords) {
            if (record.subjectId.equals(subject.id)) {
                records.add(record);
            }
        }

        Collections.sort(records,
                (a, b) -> b.date.compareTo(a.date));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 12, 16, 12);

        if (records.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No attendance records available.");
            empty.setTextColor(DARK_TEXT);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(12, 30, 12, 30);
            layout.addView(empty);
        }

        for (AttendanceRecord record : records) {

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(16, 14, 16, 14);

            int color = record.status.equals("Present")
                    ? Color.rgb(225, 248, 235)
                    : Color.rgb(255, 232, 238);

            card.setBackground(createBackground(color, 14));

            TextView date = new TextView(this);
            date.setText(record.date + "   -   " + record.status);
            date.setTextColor(DARK_TEXT);
            date.setTextSize(15);
            date.setTypeface(null, Typeface.BOLD);

            LinearLayout buttons = new LinearLayout(this);
            buttons.setOrientation(LinearLayout.HORIZONTAL);

            Button edit = new Button(this);
            edit.setText("Edit");
            edit.setAllCaps(false);

            Button delete = new Button(this);
            delete.setText("Delete");
            delete.setAllCaps(false);

            LinearLayout.LayoutParams buttonParams =
                    new LinearLayout.LayoutParams(0, -2, 1);

            buttons.addView(edit, buttonParams);
            buttons.addView(delete, buttonParams);

            edit.setOnClickListener(v ->
                    showEditAttendanceDialog(subject, record));

            delete.setOnClickListener(v ->
                    confirmDeleteAttendance(subject, record));

            card.addView(date);
            card.addView(buttons);

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(-1, -2);
            params.setMargins(0, 0, 0, 12);
            card.setLayoutParams(params);

            layout.addView(card);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);

        new AlertDialog.Builder(this)
                .setTitle(subject.name + " - Attendance History")
                .setView(scroll)
                .setPositiveButton("Close", null)
                .show();
    }

    // ---------------- EDIT ATTENDANCE ----------------

    private void showEditAttendanceDialog(
            Subject subject, AttendanceRecord record) {

        String[] statuses = {"Present", "Absent"};

        Calendar calendar = Calendar.getInstance();

        try {
            calendar.setTime(new SimpleDateFormat(
                    "yyyy-MM-dd", Locale.getDefault())
                    .parse(record.date));
        } catch (Exception e) {
            showError("Invalid attendance date");
            return;
        }

        DatePickerDialog picker = new DatePickerDialog(this,
                (view, year, month, day) -> {

                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, day);

                    String newDate = new SimpleDateFormat(
                            "yyyy-MM-dd", Locale.getDefault())
                            .format(selected.getTime());

                    new AlertDialog.Builder(this)
                            .setTitle("Select New Status")
                            .setSingleChoiceItems(statuses,
                                    record.status.equals("Present") ? 0 : 1,
                                    (dialog, which) -> {

                                        dialog.dismiss();

                                        updateAttendanceRecord(
                                                subject, record,
                                                newDate, statuses[which]);
                                    })
                            .setNegativeButton("Cancel", null)
                            .show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        picker.getDatePicker().setMaxDate(System.currentTimeMillis());
        picker.show();
    }

    private void updateAttendanceRecord(
            Subject subject,
            AttendanceRecord oldRecord,
            String newDate,
            String newStatus) {

        String oldId = attendanceDocumentId(
                subject.id, oldRecord.date);

        String newId = attendanceDocumentId(
                subject.id, newDate);

        attendanceRef().document(newId).get()
                .addOnSuccessListener(existing -> {

                    if (existing.exists()
                            && !newDate.equals(oldRecord.date)) {
                        showToast("A record already exists for this date");
                        return;
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("id", newId);
                    data.put("subjectId", subject.id);
                    data.put("date", newDate);
                    data.put("status", newStatus);

                    WriteBatch batch = firestore.batch();

                    if (!oldId.equals(newId)) {
                        batch.delete(attendanceRef().document(oldId));
                    }

                    batch.set(attendanceRef().document(newId), data);

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                showToast("Attendance updated");
                                loadFirebaseData();
                            })
                            .addOnFailureListener(error ->
                                    showError(error.getMessage()));
                })
                .addOnFailureListener(error ->
                        showError(error.getMessage()));
    }

    // ---------------- DELETE ATTENDANCE ----------------

    private void confirmDeleteAttendance(
            Subject subject, AttendanceRecord record) {

        new AlertDialog.Builder(this)
                .setTitle("Delete Attendance")
                .setMessage("Delete " + record.status
                        + " record for " + record.date + "?")
                .setPositiveButton("Delete", (dialog, which) -> {

                    String id = attendanceDocumentId(
                            subject.id, record.date);

                    attendanceRef().document(id).delete()
                            .addOnSuccessListener(unused -> {
                                showToast("Attendance deleted");
                                loadFirebaseData();
                            })
                            .addOnFailureListener(error ->
                                    showError(error.getMessage()));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ---------------- DASHBOARD ----------------

    private void updateDashboard() {

        int present = 0;
        int absent = 0;

        for (AttendanceRecord record : attendanceRecords) {
            if ("Present".equals(record.status)) {
                present++;
            } else if ("Absent".equals(record.status)) {
                absent++;
            }
        }

        int total = present + absent;

        int percentage = total == 0 ? 0 :
                Math.round(present * 100f / total);

        txtSubjects.setText(String.valueOf(subjects.size()));
        txtPresent.setText(String.valueOf(present));
        txtAbsent.setText(String.valueOf(absent));
        txtPercentage.setText(percentage + "%");

        circularProgress.setProgress(percentage);
    }

    private int countForSubject(String subjectId, String status) {

        int count = 0;

        for (AttendanceRecord record : attendanceRecords) {
            if (record.subjectId.equals(subjectId)
                    && record.status.equals(status)) {
                count++;
            }
        }

        return count;
    }

    // ---------------- UI HELPERS ----------------

    private GradientDrawable createBackground(int color, int radius) {

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        drawable.setStroke(1, Color.rgb(36, 102, 168));

        return drawable;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    // ---------------- DATA MODELS ----------------

    private static class Subject {
        String id;
        String name;

        Subject(String id, String name) {
            this.id = id;
            this.name = name == null ? "" : name;
        }
    }

    private static class AttendanceRecord {
        String subjectId;
        String date;
        String status;

        AttendanceRecord(String subjectId, String date, String status) {
            this.subjectId = subjectId == null ? "" : subjectId;
            this.date = date == null ? "" : date;
            this.status = status == null ? "" : status;
        }
    }
}
