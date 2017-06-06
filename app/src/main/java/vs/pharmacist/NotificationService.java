package vs.pharmacist;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import vs.pharmacist.objects.Medicine;
import vs.pharmacist.objects.Recipe;

public class NotificationService extends Service {

    public NotificationService() {}

    public static boolean isStarted;
    public static Intent startingIntent;

    private ArrayList<Medicine> medicines;
    private ArrayList<Recipe> recipes_arr;
    private HashMap<Recipe.Time, ArrayList<Recipe>> recipes;


    private Context context = this;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        load();
        isStarted = true;
    }

    private void load() {
        recipes = new HashMap<>();
        DataLoader d = new DataLoader(this);
        medicines = d.medicines;
        recipes_arr = d.recipes;
        for (Recipe r: d.recipes)
            for (Recipe.Time t: r.getTime())
                if (t.compareToNow() > 0) {
                    boolean isExists = false;
                    for (Recipe.Time time: recipes.keySet())
                        if (time.equals(t)) {
                            recipes.get(time).add(r);
                            isExists = true;
                            break;
                        }
                    if (!isExists) {
                        ArrayList<Recipe> r_arr = new ArrayList<>();
                        r_arr.add(r);
                        recipes.put(t, r_arr);
                    }
                }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startingIntent = intent;
        new Thread(new Runnable() {
            @Override
            public void run() {
                    while (true) {
                        try {
                            long now = System.currentTimeMillis();
                            Recipe.Time t = null;
                            for (Recipe.Time time : recipes.keySet()) {
                                if (time.compareToNow() == 0) {
                                    for (Recipe r: recipes.get(time)) {
                                        if (r.getMedicine().getCount() - r.getCount() >= 0)
                                            r.getMedicine().setCount(r.getMedicine().getCount() - r.getCount());
                                        else r.getMedicine().setCount(0);
                                    }
                                    Intent i = new Intent(context, NotificationActivity.class);
                                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    i.putExtra("recipes", recipes.get(time));
                                    startActivity(i);
                                    t = time;
                                    DataSaver.save(NotificationService.this, medicines, recipes_arr);
                                    break;
                                }
                            }
                            if (t != null) {
                                recipes.remove(t);
                                t = null;
                            }
                            if (recipes.size() == 0) load();
                            Thread.sleep(1000 - (now - System.currentTimeMillis()));
                        } catch (Exception ex) { ex.printStackTrace(); }
                    }
            }
        }).start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isStarted = false;
    }

    public static class BootEventReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            context.startService(new Intent(context, NotificationService.class));
        }
    }
}

