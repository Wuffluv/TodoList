package com.example.todolist;

// Импорты необходимых библиотек и классов
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import android.app.NotificationManager;
import android.util.Log;

public class ReminderBroadcastReceiver extends BroadcastReceiver {
    // Метод, вызываемый при получении широковещательного сообщения
    @Override
    public void onReceive(Context context, Intent intent) {
        // Извлечение данных из Intent
        String description = intent.getStringExtra("description"); // Описание задачи
        String taskId = intent.getStringExtra("taskId"); // ID задачи
        Log.d("Notification", "Received broadcast for taskId=" + taskId + ", description=" + description);

        // Проверка, что описание задачи не пустое
        if (description == null) {
            Log.w("Notification", "Description is null, cannot show notification");
            return;
        }

        // Создание уведомления
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "reminder_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Установка иконки уведомления
                .setContentTitle("Напоминание о задаче") // Заголовок уведомления
                .setContentText(description) // Текст уведомления (описание задачи)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Установка приоритета уведомления
                .setAutoCancel(true); // Автоматическое закрытие уведомления при нажатии
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(taskId.hashCode(), builder.build()); // Отображение уведомления с уникальным ID
        Log.d("Notification", "Notification shown for taskId=" + taskId);
    }
}