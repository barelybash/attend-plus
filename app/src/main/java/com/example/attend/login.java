package com.example.attend;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.w3c.dom.Text;

import java.util.Objects;

public class login extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    String userType;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // bind views
        TextInputEditText emailInput = findViewById(R.id.email);
        TextInputEditText passwordInput = findViewById(R.id.password);
        Button login = findViewById(R.id.login);

        // initialize firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = Objects.requireNonNull(emailInput.getText()).toString().trim();
                String password = Objects.requireNonNull(passwordInput.getText()).toString().trim();

                // authenticate user
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Sign in success, update UI with the signed-in user's information
                                    Log.d(TAG, "signInWithEmail:success");

                                    checkUserType();
                                } else {
                                    // If sign in fails, display a message to the user.
                                    Log.w(TAG, "signInWithEmail:failure", task.getException());
                                    Toast.makeText(login.this, "Authentication failed.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser != null){
            checkUserType();
        }
    }

    public void checkUserType() {
        String userId = mAuth.getCurrentUser().getUid();

        DocumentReference usersRef = db.collection("users").document(userId);

        usersRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();

                    userType = doc.getString("userType");

                    if(userType.equals("lecturer")) {
                        Intent gotoLecActivity = new Intent(getApplicationContext(), lecActivity.class);
                        startActivity(gotoLecActivity);
                    }
                    else if(userType.equals("student")) {
                        Intent gotoStudentActivity = new Intent(getApplicationContext(), studentActivity.class);
                        startActivity(gotoStudentActivity);
                    }
                }
                else {
                    Log.d(TAG, "Error getting document", task.getException());
                }
            }
        });
    }
}