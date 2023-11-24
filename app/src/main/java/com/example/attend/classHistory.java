package com.example.attend;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

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
import java.util.Map;

public class classHistory extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    ArrayList<String> dateList = new ArrayList<>();
    ArrayList<Map> dateRecs = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_history);

        // Add toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        myToolbar.getOverflowIcon().setTint(ContextCompat.getColor(this, R.color.white));

        // declare views
        ListView dateListEl = findViewById(R.id.dateList);

        // initialize firebase auth
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // get data from lecActivity
        Intent intent = getIntent();
        String classId = intent.getStringExtra("classId");

        DocumentReference classRef = db.document(String.format("classes/%s", classId));

        Query attendanceRef = db.collection("attendance").whereEqualTo("class", classRef);
        attendanceRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                        String date = documentSnapshot.getString("date");
                        String attendanceDateRecId = documentSnapshot.getId();

                        Map<String, Object> dateRec = new HashMap();
                        dateRec.put("date", date);
                        dateRec.put("dateRecId", attendanceDateRecId);

                        dateList.add(date);
                        dateRecs.add(dateRec);
                    }

                    ArrayAdapter adapter = new ArrayAdapter<>(classHistory.this, android.R.layout.simple_list_item_1, dateList);
                    dateListEl.setAdapter(adapter);

                    dateListEl.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
                            String date = adapterView.getItemAtPosition(pos).toString();
                            String dateRecId = new String();

                            for (Map dateRec : dateRecs) {
                                if (dateRec.get("date").equals(date)) {
                                    dateRecId = (String) dateRec.get("dateRecId");
                                }
                            }

                            Intent gotoAttendaceRec = new Intent(getApplicationContext(), attendanceRecord.class);
                            gotoAttendaceRec.putExtra("dateRecId", dateRecId);
                            startActivity(gotoAttendaceRec);
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