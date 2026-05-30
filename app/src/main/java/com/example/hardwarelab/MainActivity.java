package com.example.hardwarelab;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private EditText editTextTask;
    private TimePicker timePicker;
    private Button btnSchedule, btnCancel, btnVibrate, btnCamera;
    private static final int REQUEST_CODE_CAMERA = 200;

    private final ActivityResultLauncher<String> requestNotificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) scheduleAlarm();
                else Toast.makeText(this, "Разрешение на уведомления отклонено", Toast.LENGTH_SHORT).show();
            });

    private final ActivityResultLauncher<String> requestCameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) startActivity(new Intent(this, CameraActivity.class));
                else Toast.makeText(this, "Доступ к камере запрещён", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextTask = findViewById(R.id.editTextTask);
        timePicker = findViewById(R.id.timePicker);
        btnSchedule = findViewById(R.id.btnSchedule);
        btnCancel = findViewById(R.id.btnCancel);
        btnVibrate = findViewById(R.id.btnVibrate);

        btnSchedule.setOnClickListener(v -> checkNotificationPermission());
        btnCancel.setOnClickListener(v -> cancelAlarm());

        btnCamera = findViewById(R.id.btnCamera);

        btnVibrate.setOnClickListener(v -> vibrateDevice());
        btnCamera.setOnClickListener(v -> checkCameraPermission());

        Button btnTest = findViewById(R.id.btnTest);
        btnTest.setOnClickListener(v -> {
            Intent intent = new Intent(this, AlarmReceiver.class);
            intent.putExtra("TASK_MESSAGE", "ТЕСТОВОЕ УВЕДОМЛЕНИЕ");
            sendBroadcast(intent);
        });

        createNotificationChannel();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
        }
        scheduleAlarm();
    }

    private void checkCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestCameraPermission.launch(Manifest.permission.CAMERA);
                return;
            }
        }
        startActivity(new Intent(this, CameraActivity.class));
    }

    private void scheduleAlarm() {
        String taskText = editTextTask.getText().toString().trim().isEmpty() ? "Напоминание" : editTextTask.getText().toString();
        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("TASK_MESSAGE", taskText);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(this, "Включите «Точные напоминания» в настройках", Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
                return;
            }
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                Toast.makeText(this, "Напоминание установлено на " + hour + ":" + minute, Toast.LENGTH_LONG).show();
            } catch (SecurityException e) {
                Log.e("ALARM", "Ошибка: " + e.getMessage());
            }
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            Toast.makeText(this, "Напоминание установлено", Toast.LENGTH_LONG).show();
        }
    }

    private void cancelAlarm() {
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pendingIntent != null) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarmManager != null) alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Toast.makeText(this, "Напоминание отменено", Toast.LENGTH_SHORT).show();
        }
    }

    // Задание 4: Метод вибрации по паттерну
    private void vibrateDevice() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            // Паттерн: 0мс пауза - 500мс вибрация - 200мс пауза - 500мс вибрация
            long[] pattern = {0, 500, 200, 500};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                vibrator.vibrate(pattern, -1);
            }
        } else {
            Toast.makeText(this, "Вибрация не поддерживается на этом устройстве", Toast.LENGTH_SHORT).show();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Планировщик";
            String description = "Канал для напоминаний";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("reminder_channel", name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}