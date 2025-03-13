package com.example.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ActivityRegistration extends AppCompatActivity {

    private EditText newEmail, newName, newPassword;
    private Button registryButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        newEmail = findViewById(R.id.NewEmail);
        newName = findViewById(R.id.NewName);
        newPassword = findViewById(R.id.NewPassword);
        registryButton = findViewById(R.id.RegistryButt);

        registryButton.setOnClickListener(v -> {
            String email = newEmail.getText().toString().trim();
            String name = newName.getText().toString().trim();
            String password = newPassword.getText().toString().trim();

            if (email.isEmpty() || name.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Заполните все поля!", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("username", name);
                            userData.put("email", email);

                            db.collection("users").document(user.getUid())
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(ActivityRegistration.this, MainMenuActivity.class);
                                        intent.putExtra("USER_ID", user.getUid());
                                        startActivity(intent);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Ошибка сохранения данных", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(this, "Ошибка регистрации", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}