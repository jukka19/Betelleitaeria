package br.com.betelleitaria.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.webkit.JavascriptInterface;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Set;

public class ReminderBridge {
    private final Context context;
    public ReminderBridge(Context context) { this.context = context.getApplicationContext(); }

    @JavascriptInterface public void sync(String json) {
        try {
            JSONArray next = new JSONArray(json);
            Set<Integer> ids = new HashSet<>();
            for (int i=0; i<next.length(); i++) ids.add(next.getJSONObject(i).optInt("id", 1));
            String oldJson = context.getSharedPreferences("betel_reminders", Context.MODE_PRIVATE).getString("items", "[]");
            JSONArray old = new JSONArray(oldJson);
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) for (int i=0; i<old.length(); i++) {
                int id = old.getJSONObject(i).optInt("id", 1);
                if (!ids.contains(id)) {
                    Intent alarm = new Intent(context, AlarmReceiver.class).setAction(AlarmReceiver.ACTION_REMINDER);
                    PendingIntent pi = PendingIntent.getBroadcast(context, id, alarm, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    am.cancel(pi); pi.cancel();
                }
            }
            context.getSharedPreferences("betel_reminders", Context.MODE_PRIVATE).edit().putString("items", next.toString()).apply();
            AlarmReceiver.scheduleJson(context, next.toString());
        } catch (Exception ignored) { }
    }
}
