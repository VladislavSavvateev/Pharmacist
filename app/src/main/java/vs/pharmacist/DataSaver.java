package vs.pharmacist;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.security.Key;
import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import vs.pharmacist.objects.Medicine;
import vs.pharmacist.objects.Recipe;

public class DataSaver {

    public static void save(Context context, ArrayList<Medicine> medicines, ArrayList<Recipe> recipes) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.write("PH".getBytes("UTF-8"));
        dos.writeShort(medicines.size());
        for (Medicine m: medicines) {
            byte[] nameRaw = m.getName().getBytes("UTF-8");
            dos.writeByte(nameRaw.length);
            dos.write(nameRaw);
            if (m.getCountType() == Medicine.CountType.COUNT_MILLIS) dos.writeByte(1);
            else dos.writeByte(0);
            dos.writeInt(m.getCount());
            dos.writeLong(m.getID());
        }
        dos.writeShort(recipes.size());
        for (Recipe r: recipes) {
            dos.writeLong(r.getMedicine().getID());
            dos.writeInt(r.getCount());
            int type = -1;
            if (r.getTime().get(0).getDayInWeek() == 0 && r.getTime().get(0).getDayInMonth() == 0) type = 0;
            else if (r.getTime().get(0).getDayInMonth() == 0) type = 1;
            else type = 2;
            dos.writeByte(type);
            dos.writeByte(r.getTime().size());
            for (Recipe.Time t: r.getTime()) {
                switch (type) {
                    case 0:
                        dos.writeByte(t.getHour());
                        dos.writeByte(t.getMinute());
                        break;
                    case 1:
                        dos.writeByte(t.getDayInWeek());
                        dos.writeByte(t.getHour());
                        dos.writeByte(t.getMinute());
                        break;
                    case 2:
                        dos.writeByte(t.getDayInMonth());
                        dos.writeByte(t.getHour());
                        dos.writeByte(t.getMinute());
                        break;
                }
            }
        }
        dos.close();
        Key key = new SecretKeySpec("PSK_n6hvh7667j96".getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        FileOutputStream fos = context.openFileOutput("data.bin", Context.MODE_PRIVATE);
        fos.write(cipher.doFinal(baos.toByteArray()));
        fos.close();
        baos.close();
    }
}
