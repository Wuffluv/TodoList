package com.example.todolist;

import android.os.Bundle;
import android.widget.ProgressBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.todolist.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ProgressBar dailyProgressBar;
    private int totalTasks = 10; // Общее количество задач на день
    private int completedTasks = 0; // Количество выполненных задач

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Инициализация навигации
        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        // Инициализация прогресс-бара
        dailyProgressBar = findViewById(R.id.dailyProgressBar);

        // Обновление прогресса при запуске
        updateDailyProgress();

        // Пример: изменение количества выполненных задач
        markTaskAsCompleted();
        markTaskAsCompleted();
    }

    // Метод для обновления прогресса на основе выполненных задач
    private void updateDailyProgress() {
        int progress = (int) ((completedTasks / (float) totalTasks) * 100);
        dailyProgressBar.setProgress(progress);
    }

    // Метод для отметки задачи как выполненной
    private void markTaskAsCompleted() {
        if (completedTasks < totalTasks) {
            completedTasks++;
            updateDailyProgress();
        }
    }

    // Метод для отметки задачи как невыполненной (если потребуется)
    private void markTaskAsIncomplete() {
        if (completedTasks > 0) {
            completedTasks--;
            updateDailyProgress();
        }
    }
}
