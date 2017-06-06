package vs.pharmacist;

import android.content.Context;
import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.Key;
import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import vs.pharmacist.objects.AppParams;
import vs.pharmacist.objects.Medicine;
import vs.pharmacist.objects.Recipe;

import static vs.pharmacist.constants.CryptConsts.CRYPT_KEY;

public class DataLoader {

    ArrayList<Medicine> medicines;
    ArrayList<Recipe> recipes;

    Context context;

    public DataLoader(Context context) {
        this.context = context;
        load();
    }

    private void load() {
        try {
            Key key = new SecretKeySpec(CRYPT_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);

            Gson g = new Gson();

            if (new File(context.getFilesDir() + "/data.bin").exists()) {

                FileInputStream fis = context.openFileInput("data.bin");
                byte inputData[] = new byte[fis.available()];
                fis.read(inputData);

                byte outputData[] = cipher.doFinal(inputData);

                if (new File(context.getFilesDir().getPath() + "/params.json").exists()) {
                    AppParams appParams;
                    FileInputStream params_fis = context.openFileInput("params.json");
                    byte params_raw[] = new byte[params_fis.available()];
                    params_fis.read(params_raw);
                    appParams = g.fromJson(new String(params_raw, "UTF-8"), AppParams.class);

                    if (!appParams.isFirstStart) {
                        medicines = new ArrayList<>();
                        recipes = new ArrayList<>();
                        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(outputData));
                        byte[] headerRaw = new byte[2];
                        headerRaw[0] = dis.readByte();
                        headerRaw[1] = dis.readByte();
                        String header = new String(headerRaw, "UTF8");
                        if (!header.equals("PH")) {
                            recipes = null;
                            medicines = null;
                        }
                        short countOfMedicines = dis.readShort();
                        for (int i = 0; i < countOfMedicines; i++) {
                            byte length = dis.readByte();
                            byte[] name = new byte[length];
                            for (int k = 0; k < length; k++)
                                name[k] = dis.readByte();
                            byte countType_raw = dis.readByte();
                            Medicine.CountType countType;
                            if (countType_raw == 0) countType = Medicine.CountType.COUNT_PCS;
                            else countType = Medicine.CountType.COUNT_MILLIS;
                            int count = dis.readInt();
                            long ID = dis.readLong();
                            medicines.add(new Medicine(new String(name, "UTF-8"), countType, count, ID));
                        }

                        short countOfRecipes = dis.readShort();
                        for (int i = 0; i < countOfRecipes; i++) {
                            long ID = dis.readLong();
                            Medicine medicine = null;
                            for (Medicine m : medicines)
                                if (m.getID() == ID) {
                                    medicine = m;
                                    break;
                                }
                            if (medicine == null)
                                medicine = new Medicine("НЕИЗВЕСТНО", Medicine.CountType.COUNT_PCS, 0, ID);
                            int count = dis.readInt();
                            byte notificationType = dis.readByte();
                            byte howManyTimes = dis.readByte();
                            ArrayList<Recipe.Time> times = new ArrayList<>();
                            for (int l = 0; l < howManyTimes; l++)
                                switch (notificationType) {
                                    case 2:
                                        times.add(new Recipe.Time(dis.readByte(), dis.readByte(), dis.readByte(), null));
                                        break;
                                    case 1:
                                        times.add(new Recipe.Time(dis.readByte(), dis.readByte(), dis.readByte()));
                                        break;
                                    case 0:
                                        times.add(new Recipe.Time(dis.readByte(), dis.readByte()));
                                }
                            recipes.add(new Recipe(medicine, times, count));
                        }
                        dis.close();
                    } else {
                        recipes = null;
                        medicines = null;
                    }
                } else {
                    recipes = null;
                    medicines = null;
                }
            } else {
                recipes = null;
                medicines = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
