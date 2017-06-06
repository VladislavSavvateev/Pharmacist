package vs.pharmacist;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.ArrayList;

import vs.pharmacist.objects.Medicine;
import vs.pharmacist.objects.Recipe;

public class NotificationActivity extends AppCompatActivity {

    MediaPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_notification);

        ArrayList<Recipe> recipes = (ArrayList<Recipe>) getIntent().getSerializableExtra("recipes");

        String text = "";
        for (Recipe r: recipes) {
            text += r.getMedicine().getName();
            text += " в количестве ";
            text += r.getCount();
            if (r.getMedicine().getCountType() == Medicine.CountType.COUNT_PCS) text += " шт. (останется ";
            else text += " мл. (останется ";
            int count = r.getMedicine().getCount();
            if (count < 0) text += "0 ";
            else text += Integer.toString(count) + " ";
            if (r.getMedicine().getCountType() == Medicine.CountType.COUNT_PCS) text += "шт.) ";
            else text += "мл.) ";
            if (count < 15) text += "<b><font color=\"#FF0000\">ЛЕКАРСТВО ЗАКАНЧИВАЕТСЯ!</font></b>";
            text += "\n";
        }

        TextView tv = (TextView) findViewById(R.id.reminder_text);
        tv.setText(Html.fromHtml(text));

        new Thread(new Runnable() {
            @Override
            public void run() {
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.setStreamVolume(AudioManager.STREAM_MUSIC,
                        am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);

                player = MediaPlayer.create(NotificationActivity.this, R.raw.ring);
                player.setLooping(true);
                player.setScreenOnWhilePlaying(true);
                player.start();
            }
        }).start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        while (player == null);
        player.stop();
        return super.onTouchEvent(event);
    }
}
