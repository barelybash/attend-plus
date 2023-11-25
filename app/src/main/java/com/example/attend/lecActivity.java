package com.example.attend;

import static android.content.ContentValues.TAG;

import static java.lang.Integer.parseInt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class lecActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    ArrayList<String> spinnerData = new ArrayList<>();
    String BSSID;
    String classCode;
    ArrayList<Map> classData = new ArrayList<>();
    ArrayList<Map> classCodesAndIds = new ArrayList<>();
    DocumentReference classRef;
    ArrayList<Map> students = new ArrayList<>();
    Integer activeTime;
    String currentDate = new SimpleDateFormat("MM-dd-yyyy").format(new Date());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lec);

        // Add toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        myToolbar.getOverflowIcon().setTint(ContextCompat.getColor(this, R.color.white));

        // declare views
        Button sendCodes = findViewById(R.id.sendCodes);
        EditText activeTimeEl = findViewById(R.id.activeTime);
        Spinner spinner = findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(this);

        // initialize firebase auth
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        String userId = mAuth.getCurrentUser().getUid();

        // request permissions
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_WIFI_STATE, android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION}, PackageManager.PERMISSION_GRANTED);

        DocumentReference lecturerRef = db.document(String.format("users/%s", userId));
        Query classesRef = db.collection("classes").whereEqualTo("lecturer", lecturerRef);

        classesRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()) {
                    for(QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                        String classCode = documentSnapshot.getString("classCode");
                        String className = documentSnapshot.getString("className");
                        String classId = documentSnapshot.getId();
                        ArrayList<DocumentReference> studentRefs = (ArrayList<DocumentReference>) documentSnapshot.get("students");

                        Map <String, Object> classObj = new HashMap<>();
                        classObj.put("classCode", classCode);
                        classObj.put("classId", classId);
                        classObj.put("studentRefs", studentRefs);

                        Map <String, Object> classCodeAndId = new HashMap<>();
                        classCodeAndId.put("classCode", classCode);
                        classCodeAndId.put("classId", classId);

                        classData.add(classObj);
                        classCodesAndIds.add(classCodeAndId);
                        

                        String spinnerItem = classCode + " - " + className;
                        spinnerData.add(spinnerItem);
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(lecActivity.this, android.R.layout.simple_spinner_dropdown_item, spinnerData);
                    spinner.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                }
                else {
                    Log.d(TAG, "Error getting documents: ", task.getException());
                }
            }
            });

        sendCodes.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // check if attendance for that date exists
                                Query attendanceRef = db.collection("attendance").whereEqualTo("class", classRef).whereEqualTo("date", currentDate);
                                attendanceRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful()) {
                                            // Get the QuerySnapshot object
                                            QuerySnapshot querySnapshot = task.getResult();
                                            // Check if the querySnapshot is empty
                                            if (querySnapshot.isEmpty()) {
                                                // No matching documents found
                                                //Check if location is enabled
                                                LocationManager mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                                                String GPSOn = String.valueOf(mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));

                                                if(GPSOn.equals("false")) {
                                                    Toast.makeText(lecActivity.this, "Please turn on your location services.", Toast.LENGTH_SHORT).show();
                                                } else if(GPSOn.equals("true")) {
                                                    BSSID = getBSSID();

                                                    // handle send codes button click
                                                    Map<String, Object> attendRec = new HashMap<>();
                                                    attendRec.put("bssid", BSSID);
                                                    attendRec.put("date", currentDate);
                                                    attendRec.put("class", classRef);
                                                    attendRec.put("students", students);
                                                    attendRec.put("timesUp", false);

                                                    db.collection("attendance")
                                                            .add(attendRec)
                                                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                                @Override
                                                                public void onSuccess(DocumentReference documentReference) {
                                                                    Toast.makeText(lecActivity.this, "Codes generated successfully!", Toast.LENGTH_SHORT).show();

                                                                    String attendDocId = documentReference.getId();

                                                                    if(activeTimeEl.getText().toString().isEmpty()) {
                                                                        Toast.makeText(lecActivity.this,"Please enter time to keep attendance signing active.", Toast.LENGTH_SHORT).show();
                                                                    } else {
                                                                        activeTime = parseInt(activeTimeEl.getText().toString());

                                                                        // start timer
                                                                        CountDownTimer timer = new CountDownTimer(TimeUnit.MINUTES.toMillis(activeTime), 1000) {
                                                                            @Override
                                                                            public void onTick(long l) {
                                                                            }

                                                                            @Override
                                                                            public void onFinish() {
                                                                                DocumentReference attendDocRef = db.collection("attendance").document(attendDocId);

                                                                                attendDocRef.update("timesUp", true)
                                                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                            @Override
                                                                                            public void onSuccess(Void aVoid) {
                                                                                                Log.d(TAG, "Document successfully updated!");
                                                                                            }
                                                                                        })
                                                                                        .addOnFailureListener(new OnFailureListener() {
                                                                                            @Override
                                                                                            public void onFailure(@NonNull Exception e) {
                                                                                                Log.w(TAG, "Error updating document", e);
                                                                                            }
                                                                                        });

                                                                            }
                                                                        };
                                                                        timer.start();
                                                                    }
                                                                }
                                                            })
                                                            .addOnFailureListener(new OnFailureListener() {
                                                                @Override
                                                                public void onFailure(@NonNull Exception e) {
                                                                    Log.w(TAG, "Error adding document", e);
                                                                }
                                                            });
                                                }
                                            } else {
                                                // Matching documents found
                                                Toast.makeText(lecActivity.this, "Attendance record already exists for " + classCode +" today!", Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            // Query failed
                                            Log.d(TAG, "Error getting documents: ", task.getException());
                                        }
                                    }
                                });
                            }
                        });
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

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
        classCode = adapterView.getItemAtPosition(pos).toString().substring(0,8);

        // get classRef
        for (Map c : classData) {
            if(c.get("classCode").equals(classCode)) {
                String classId = (String) c.get("classId");
                students.clear();

                for (DocumentReference docRef: (ArrayList<DocumentReference>) c.get("studentRefs")) {
                    // generate random 6 digit code
                    String randomCode = String.format("%06d", new Random().nextInt(1000000));
                    classRef = db.document(String.format("classes/%s", classId));

                    Map<String, Object> studentMap = new HashMap<>();
                    studentMap.put("student", docRef);
                    studentMap.put("code", randomCode);
                    studentMap.put("present", false);

                    students.add(studentMap);
                }
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.logOut) {
            mAuth.signOut();
            Intent gotoLogin = new Intent(getApplicationContext(), login.class);
            startActivity(gotoLogin);
        }
        if (item.getItemId() == R.id.history) {
            Intent gotoHistory = new Intent(getApplicationContext(), history.class);
            gotoHistory.putStringArrayListExtra("classList", (ArrayList<String>) spinnerData);
            gotoHistory.putExtra("classData", (Serializable) classCodesAndIds);
            startActivity(gotoHistory);
        }
        return super.onOptionsItemSelected(item);
    }
}