package com.example.attend;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.common.collect.Table;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class attendanceRecord extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    ArrayList<Map> studAttendanceIdRecs = new ArrayList<>();
    ArrayList<Map> attendanceRecs = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_record);

        // declare views
        TableLayout attendanceTable = findViewById(R.id.attendanceTable);

        // Add toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        myToolbar.getOverflowIcon().setTint(ContextCompat.getColor(this, R.color.white));

        // initialize firebase auth
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // get data from lecActivity
        Intent intent = getIntent();
        String dateRecId = intent.getStringExtra("dateRecId");

        DocumentReference attendanceRecRef = db.collection("attendance").document(dateRecId);
        attendanceRecRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    Map <String, Object> doc = task.getResult().getData();

                    ArrayList<Map> studentRecs = (ArrayList<Map>) doc.get("students");
                    ArrayList<String> studentIds = new ArrayList<>();
//
                    for (Map studentAttendanceRec : studentRecs) {
                        DocumentReference studentRef = (DocumentReference) studentAttendanceRec.get("student");
                        String studentId = studentRef.getId();
                        Boolean present = (Boolean) studentAttendanceRec.get("present");
                        studentIds.add(studentId);

                        Map<String, Object> studentAttendanceIdRec = new HashMap<>();
                        studentAttendanceIdRec.put("studentId", studentId);
                        studentAttendanceIdRec.put("present", present);

                        studAttendanceIdRecs.add(studentAttendanceIdRec);
                    }

                    db.collection("users").whereIn(FieldPath.documentId(), studentIds).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if(task.isSuccessful()) {
                                for (QueryDocumentSnapshot documentSnapshot: task.getResult()) {
                                    String fullName = documentSnapshot.getString("fullName");
                                    String docId = documentSnapshot.getId();

                                    for(Map studAttendanceIdRec: studAttendanceIdRecs) {
                                        if(studAttendanceIdRec.get("studentId").equals(docId)) {
                                            Map <String, Object> attendanceRec = new HashMap<>();
                                            attendanceRec.put("fullName", fullName);
                                            attendanceRec.put("present", studAttendanceIdRec.get("present"));

                                            attendanceRecs.add(attendanceRec);
                                        }
                                    }
                                }

                                for (Map attendanceRec: attendanceRecs) {
                                    TableRow row = new TableRow(attendanceRecord.this);
                                    TableRow.LayoutParams nameParams = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.65f);
                                    nameParams.gravity = Gravity.CENTER;
                                    TableRow.LayoutParams statusParams = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.35f);
                                    statusParams.gravity = Gravity.CENTER;

                                    String name = (String) attendanceRec.get("fullName");
                                    Boolean isPresent = (Boolean) attendanceRec.get("present");
                                    String status = new String();

                                    TextView nameView = new TextView(attendanceRecord.this);
                                    nameView.setText(name);
                                    nameView.setTextColor(Color.rgb(0,0,0));
                                    nameView.setTextSize(18);
                                    nameView.setPadding(20,20,20,20);
                                    nameView.setLayoutParams(nameParams);

                                    TextView statusView = new TextView(attendanceRecord.this);

                                    if(isPresent == true) {
                                        status = "Present";
                                        statusView.setTextColor(Color.rgb(0, 255, 0));
                                    } else {
                                        status = "Absent";
                                        statusView.setTextColor(Color.rgb(255, 0, 0));
                                    }
                                    statusView.setText(status);
                                    statusView.setTextSize(18);
                                    statusView.setGravity(Gravity.CENTER);
                                    statusView.setPadding(0,20,20,20);
                                    statusView.setLayoutParams(statusParams);

                                    row.addView(nameView);
                                    row.addView(statusView);

                                    attendanceTable.addView(row);
                                }
                            } else {
                                Log.d(TAG, "Error getting documents: ", task.getException());
                            }
                        }
                    });
                } else {
                    // Query failed
                    Log.d(TAG, "Error getting documents: ", task.getException());
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

         // hide history options menu item
        MenuItem historyItem = menu.findItem(R.id.history);
        historyItem.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.logOut) {
            mAuth.signOut();
            Intent gotoLogin = new Intent(getApplicationContext(), login.class);
            startActivity(gotoLogin);
        }
        return super.onOptionsItemSelected(item);
    }
}
