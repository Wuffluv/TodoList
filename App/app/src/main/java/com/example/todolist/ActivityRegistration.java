package com.example.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class ActivityRegistration extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        // Инициализация кнопки Registry
        Button registryButton = findViewById(R.id.RegistryButt);

        // Обработчик нажатия
        registryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Переход на MainMenuActivity
                Intent intent = new Intent(ActivityRegistration.this, MainMenuActivity.class);
                startActivity(intent);
            }
        });
    }
}
