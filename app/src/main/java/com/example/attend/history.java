package com.example.attend;

import android.content.Intent;
import android.os.Bundle;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Map;

public class history extends AppCompatActivity {
    private FirebaseAuth mAuth;
    String classCode;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Add toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        myToolbar.getOverflowIcon().setTint(ContextCompat.getColor(this, R.color.white));

        // declare views
        ListView classlistEl = findViewById(R.id.classList);

        // initialize firebase auth
        mAuth = FirebaseAuth.getInstance();

        // get data from lecActivity
        Intent intent = getIntent();
        ArrayList<String> classList = intent.getStringArrayListExtra("classList");
        ArrayList<Map> classData = (ArrayList<Map>) intent.getSerializableExtra("classData");

        ArrayAdapter adapter = new ArrayAdapter<>(history.this, android.R.layout.simple_list_item_1,classList);
        classlistEl.setAdapter(adapter);

        classlistEl.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
                classCode = adapterView.getItemAtPosition(pos).toString().substring(0,8);
                String classId = new String();

                for (Map c : classData) {
                    if (c.get("classCode").equals(classCode)) {
                        classId = (String) c.get("classId");
                    }
                }

                Intent gotoClassHistory = new Intent(getApplicationContext(), classHistory.class);
                gotoClassHistory.putExtra("classId", classId);
                startActivity(gotoClassHistory);
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
