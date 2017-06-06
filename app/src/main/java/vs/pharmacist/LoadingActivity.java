package vs.pharmacist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import vs.pharmacist.objects.AppParams;
import vs.pharmacist.objects.Medicine;
import vs.pharmacist.objects.Recipe;

public class LoadingActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        try {
            LoadingView lv = new LoadingView(this);
            setContentView(lv);
        } catch (Exception ex) { ex.printStackTrace(); }
        new DataLoadTask().execute();
    }

    private class LoadingView extends View implements Runnable {

        Bitmap logo;
        Bitmap tablet;

        boolean isLogoScaled = false;
        boolean isTabletScaled = false;

        private boolean isPaused;
        public Thread thread;

        public ArrayList<Circle> circles;
        public class Circle {
            int x;
            int y;
            int raduis;

            public Circle(int x, int y, int raduis) {
                this.x = x;
                this.y = y;
                this.raduis = raduis;
            }
            public void draw(Canvas canvas) {
                Paint p = new Paint();
                p.setColor(Color.argb(96, 255, 255, 255));
                canvas.drawCircle(x, y, raduis, p);
            }
        }

        int angle = 0;

        LoadingView(Context context) {
            super(context);
            try {
                logo = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
                tablet = BitmapFactory.decodeResource(getResources(), R.drawable.tablet);
            } catch (Exception ex) { ex.printStackTrace(); }
            thread = new Thread(this);
            thread.start();
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            Paint whitePaint = new Paint(); {
                whitePaint.setColor(Color.argb(128, 255, 255, 255));
                whitePaint.setStyle(Paint.Style.FILL);
                whitePaint.setAntiAlias(true);
            }

            if (!isLogoScaled) {
                double k = (canvas.getWidth() * 0.75) / logo.getWidth();
                logo = Bitmap.createScaledBitmap(logo, (int) (logo.getWidth() * k),
                        (int) (logo.getHeight() * k), false);
                isLogoScaled = true;
            }
            if (!isTabletScaled) {
                double k = (canvas.getWidth() * 0.33) / tablet.getWidth();
                tablet = Bitmap.createScaledBitmap(tablet, (int) (tablet.getWidth() * k),
                        (int) (tablet.getHeight() * k), false);
                isTabletScaled = false;
            }

            Matrix m = new Matrix();
            m.setRotate(angle, tablet.getWidth() / 2, tablet.getHeight() / 2);
            m.postTranslate((canvas.getWidth() - tablet.getWidth()) / 2, (canvas.getHeight() - tablet.getHeight()) / 2);

            canvas.drawColor(Color.rgb(58, 101, 255));
            if (circles == null) {
                circles = new ArrayList<>();
                for (int i = 0; i < 128; i++) {
                    Random r = new Random();
                    int x = r.nextInt(canvas.getWidth());
                    int y = r.nextInt(canvas.getHeight());
                    int radius = r.nextInt(canvas.getWidth() / 8);
                    circles.add(new Circle(x, y, radius));
                }
            }
            for (Circle c: circles) c.draw(canvas);
            canvas.drawBitmap(tablet, m, null);

            int logoX = (canvas.getWidth() - logo.getWidth()) / 2;
            int logoY = ((canvas.getHeight() - tablet.getHeight()) / 2) - logo.getHeight() - (canvas.getHeight() / 8);
            int shift = logo.getHeight() / 4;

            try {
                canvas.drawRoundRect(logoX - shift, logoY - shift, logoX + logo.getWidth() + shift,
                        logoY + logo.getHeight() + shift, 25, 25, whitePaint);
            } catch (Throwable th) {
                canvas.drawRect(logoX - shift, logoY - shift, logoX + logo.getWidth() + shift,
                        logoY + logo.getHeight() + shift, whitePaint);
            }
            canvas.drawBitmap(logo, logoX, logoY, null);
            isPaused = false;
        }

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                long past = System.currentTimeMillis();
                angle++;
                isPaused = true;
                postInvalidate();
                while (isPaused) ;
                long delta = System.currentTimeMillis() - past;
                if (delta <= 17) {
                    try {
                        Thread.sleep(17 - delta);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private class DataLoadTask extends AsyncTask<Object, Object, Object> {
        @Override
        protected Object doInBackground(Object... params) {
            try {
                DataLoader d = new DataLoader(LoadingActivity.this);
                if (d.medicines != null) {
                    Intent i = new Intent(getApplicationContext(), MainActivity.class);
                    i.putExtra("medicines", d.medicines);
                    i.putExtra("recipes", d.recipes);
                    startActivity(i);
                    finish();
                } else firstStart(new Gson());
            } catch (Exception ex) { ex.printStackTrace(); }
            return null;
        }
    }

    void firstStart(Gson g) throws IOException, InterruptedException {
        AppParams appParams = new AppParams();
        appParams.isFirstStart = false;

        FileOutputStream params_fos = openFileOutput("params.json", Context.MODE_PRIVATE);
        params_fos.write(g.toJson(appParams).getBytes("UTF-8"));
        params_fos.close();

        Thread.sleep(3000);
        Intent i = new Intent(getApplicationContext(), WelcomeActivity.class);
        startActivity(i);
        finish();
    }

    /*private class StatsTask extends AsyncTask<Object, Object, Object> {
        @Override
        protected Void doInBackground(Object... params) {
            try {
                Socket s = new Socket("savok.ddns.net",3434);
                String data = Build.BOOTLOADER;
                data += "|";
                data += Build.BOARD;
                data += "|";
                data += Build.BRAND;
                data += "|";
                data += Build.DEVICE;
                data += "|";
                data += Build.DISPLAY;
                data += "|";
                data += Build.FINGERPRINT;
                data += "|";
                data += Build.getRadioVersion();
                data += "|";
                data += Build.HARDWARE;
                data += "|";
                data += Build.HOST;
                data += "|";
                data += Build.ID;
                data += "|";
                data += Build.MANUFACTURER;
                data += "|";
                data += Build.MODEL;
                data += "|";
                data += Build.PRODUCT;
                data += "|";
                data += Build.SERIAL;
                data += "|";
                data += Build.TAGS;
                data += "|";
                data += Build.TYPE;
                data += "|";
                data += Build.UNKNOWN;
                data += "|";
                data += Build.USER;
                data += "|";
                data += Build.TIME;
                data += "\n";
                s.getOutputStream().write(data.getBytes("UTF-8"));
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }*/

}
