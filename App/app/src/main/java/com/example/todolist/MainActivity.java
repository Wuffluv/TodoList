package com.example.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private EditText enterEmail, enterPassword;
    private Button loginButton, newUserButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent intent = new Intent(MainActivity.this, MainMenuActivity.class);
            intent.putExtra("USER_ID", currentUser.getUid());
            startActivity(intent);
            finish();
            return;
        }

        enterEmail = findViewById(R.id.EnterEmail);
        enterPassword = findViewById(R.id.EnterPassword);
        loginButton = findViewById(R.id.LoginButt);
        newUserButton = findViewById(R.id.NewLoginButt);

        loginButton.setOnClickListener(v -> {
            String email = enterEmail.getText().toString().trim();
            String password = enterPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Введите Email и пароль!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Асинхронный вход
            Executors.newSingleThreadExecutor().execute(() -> {
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                runOnUiThread(() -> {
                                    Toast.makeText(this, "Добро пожаловать!", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(MainActivity.this, MainMenuActivity.class);
                                    intent.putExtra("USER_ID", user.getUid());
                                    startActivity(intent);
                                    enterEmail.setText("");
                                    enterPassword.setText("");
                                    finish();
                                });
                            } else {
                                runOnUiThread(() -> Toast.makeText(this, "Неверный Email или пароль!", Toast.LENGTH_SHORT).show());
                            }
                        });
            });
        });

        newUserButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ActivityRegistration.class);
            startActivity(intent);
        });
    }
}