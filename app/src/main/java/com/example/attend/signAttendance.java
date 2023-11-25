package com.example.attend;

import static android.content.ContentValues.TAG;

import static java.lang.Integer.parseInt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Map;

public class signAttendance extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    String lecBSSID;
    ArrayList<Map> studentAttendanceRecs = new ArrayList<>();
    String randomCode;
    String studentBSSID;
    String attendDocId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_attendance);

        // Add toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        myToolbar.getOverflowIcon().setTint(ContextCompat.getColor(this, R.color.white));

        // declare views
        TextView title = findViewById(R.id.title);
        TextView codeText = findViewById(R.id.codeText);
        EditText codeInput = findViewById(R.id.codeInput);
        Button signAttendance = findViewById(R.id.signAttendance);

        // initialize firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // get data from studentActivity
        Intent intent = getIntent();
        String studentId = intent.getStringExtra("studentId");
        String classId = intent.getStringExtra("classId");

        DocumentReference classRef = db.document(String.format("classes/%s", classId));

        Query attendanceRef = db.collection("attendance").whereEqualTo("class", classRef).whereEqualTo("timesUp", false).limit(1);

        attendanceRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    if(task.getResult().isEmpty()) {
                        title.setVisibility(View.INVISIBLE);
                        signAttendance.setVisibility(View.INVISIBLE);
                        codeInput.setVisibility(View.INVISIBLE);
                        codeText.setText("Attendance is not open for this class");
                    }

                    for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                        lecBSSID = (String) documentSnapshot.get("bssid");
                        studentAttendanceRecs = (ArrayList<Map>) documentSnapshot.get("students");
                        attendDocId = documentSnapshot.getId();
                    }

                    for (Map studentRec: studentAttendanceRecs){
                        DocumentReference studDocRef = (DocumentReference) studentRec.get("student");
                        if(studDocRef.getId().equals(studentId)) {
                            randomCode = (String) studentRec.get("code");
                            codeText.setText(randomCode);
                        }
                    }

                } else {
                    Log.d(TAG, "Error getting documents: ", task.getException());
                }
            }
        });

        signAttendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Run checks to verify eligibility to sign attendance
                runEligibilityChecks(codeInput, studentAttendanceRecs, attendDocId, studentId);
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

    public void runEligibilityChecks(EditText codeInput, ArrayList<Map> StudentAttendanceRecs, String attendDocId, String studentId) {
        //Check if location is enabled
        LocationManager mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        String GPSOn = String.valueOf(mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));

        if(GPSOn.equals("false")) {
            Toast.makeText(signAttendance.this, "Please turn on your location services.", Toast.LENGTH_SHORT).show();
        } else if(GPSOn.equals("true")) {
            studentBSSID = getBSSID();

            // Check if lecturer and student BSSIDs match
            if(studentBSSID.equals(lecBSSID)) {
                if(codeInput.getText().toString().isEmpty()) {
                    Toast.makeText(this, "You must enter the code to sign attendance!", Toast.LENGTH_SHORT).show();
                } else {
                    if(parseInt(codeInput.getText().toString()) != parseInt(randomCode)) {
                        Toast.makeText(this, "The code you entered doesn't match the displayed code", Toast.LENGTH_SHORT).show();
                    } else {
                        for(Map student:studentAttendanceRecs) {
                            DocumentReference studDocRef = (DocumentReference) student.get("student");
                            if(studDocRef.getId().equals(studentId)) {
                                student.put("present", true);
                            }
                        }

                        DocumentReference attendDocRef = db.collection("attendance").document(attendDocId);

                        attendDocRef.update("students", studentAttendanceRecs)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d(TAG, "Document successfully updated!");
                                        Toast.makeText(signAttendance.this, "You have signed attendance!", Toast.LENGTH_SHORT).show();
                                        Intent gotoStudent = new Intent(getApplicationContext(), studentActivity.class);
                                        startActivity(gotoStudent);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w(TAG, "Error updating document", e);
                                    }
                                });
                    }
                }
            } else {
                Toast.makeText(this, "You must be connected to the same WiFi as the lecturer to sign attendance!", Toast.LENGTH_SHORT).show();
            }
        }
    }

        public String getBSSID() {
            WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo;

            wifiInfo = wifiManager.getConnectionInfo();

            if(wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                return wifiInfo.getBSSID();
            }
            return null;
        }
}
