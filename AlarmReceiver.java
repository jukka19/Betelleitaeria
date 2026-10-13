package br.com.betelleitaria.app;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;

public class AlarmReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "betel_leitaria_lembretes";
    public static final String ACTION_REMINDER = "br.com.betelleitaria.REMINDER";
    public static final String ACTION_RESTORE = "br.com.betelleitaria.RESTORE_REMINDERS";

    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        if (ACTION_RESTORE.equals(intent.getAction()) || Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            String json = context.getSharedPreferences("betel_reminders", Context.MODE_PRIVATE).getString("items", "[]");
            scheduleJson(context, json);
            return;
        }
        if (!ACTION_REMINDER.equals(intent.getAction())) return;
        createChannel(context);
        int id = intent.getIntExtra("id", 1);
        String title = intent.getStringExtra("title");
        String body = intent.getStringExtra("body");
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        Intent open = new Intent(context, MainActivity.class);
        PendingIntent content = PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        android.app.Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new android.app.Notification.Builder(context, CHANNEL_ID)
                : new android.app.Notification.Builder(context);
        String safeBody = body == null ? "Confira os cuidados do rebanho." : body;
        android.app.Notification notification = builder
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title == null ? "Betel Leitaria" : title)
                .setContentText(safeBody)
                .setStyle(new android.app.Notification.BigTextStyle().bigText(safeBody))
                .setAutoCancel(true).setContentIntent(content).build();
        try { nm.notify(id, notification); } catch (SecurityException ignored) { }
    }

    static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(new NotificationChannel(CHANNEL_ID, "Lembretes da leitaria", NotificationManager.IMPORTANCE_DEFAULT));
        }
    }

    static void scheduleJson(Context context, String json) {
        try {
            JSONArray items = new JSONArray(json);
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            for (int i=0; i<items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                int id = item.optInt("id", 1);
                String date = item.optString("date", "");
                if (date.length() != 10) continue;
                String title = item.optString("title", "Lembrete Betel Leitaria");
                String body = item.optString("body", "Confira o cuidado previsto para o rebanho.");
                String[] p = date.split("-");
                Calendar c = Calendar.getInstance();
                c.set(Calendar.YEAR, Integer.parseInt(p[0])); c.set(Calendar.MONTH, Integer.parseInt(p[1])-1); c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(p[2]));
                c.set(Calendar.HOUR_OF_DAY, 9); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
                if (c.getTimeInMillis() <= System.currentTimeMillis()) continue; // Datas passadas não geram alarmes atrasados.
                Intent alarm = new Intent(context, AlarmReceiver.class).setAction(ACTION_REMINDER);
                alarm.putExtra("id", id); alarm.putExtra("title", title); alarm.putExtra("body", body);
                PendingIntent pi = PendingIntent.getBroadcast(context, id, alarm, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
                else am.set(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
            }
        } catch (Exception ignored) { }
    }
}
