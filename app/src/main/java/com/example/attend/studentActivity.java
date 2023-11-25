package com.example.attend;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class studentActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    ArrayList<String> classListItems = new ArrayList<>();
    ArrayList<Map> classData = new ArrayList<>();
    String classCode;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student);

        // Add toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        myToolbar.getOverflowIcon().setTint(ContextCompat.getColor(this, R.color.white));

        // declare views
        ListView classlistEl = findViewById(R.id.classList);

        // initialize firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

    String userId = mAuth.getCurrentUser().getUid();

        // request permissions
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_WIFI_STATE, android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION}, PackageManager.PERMISSION_GRANTED);

        DocumentReference studentRef = db.document(String.format("users/%s", userId));
        Query classesRef = db.collection("classes").whereArrayContains("students", studentRef);

        classesRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()) {
                    for(QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                        String classCode = documentSnapshot.getString("classCode");
                        String className = documentSnapshot.getString("className");
                        String classId = documentSnapshot.getId();

                        String listItem = classCode + " - " + className;
                        classListItems.add(listItem);

                        Map<String, Object> classObj = new HashMap<>();
                        classObj.put("classCode", classCode);
                        classObj.put("classId", classId);

                        classData.add(classObj);
                    }

                    ArrayAdapter adapter = new ArrayAdapter<>(studentActivity.this, android.R.layout.simple_list_item_1,classListItems);
                    classlistEl.setAdapter(adapter);

                    classlistEl.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
                            classCode = adapterView.getItemAtPosition(pos).toString().substring(0,8);

                            for (Map c: classData) {
                                if (c.get("classCode").equals(classCode)) {
                                    String classId = (String) c.get("classId");

                                    Intent gotoSignAttendance = new Intent(getApplicationContext(), signAttendance.class);
                                    gotoSignAttendance.putExtra("classId", classId);
                                    gotoSignAttendance.putExtra("studentId", userId);
                                    startActivity(gotoSignAttendance);
                                }
                            }
                        }
                    });
                }
                else {
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