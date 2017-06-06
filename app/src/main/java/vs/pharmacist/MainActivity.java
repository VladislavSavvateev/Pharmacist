package vs.pharmacist;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import ru.yandex.speechkit.Error;
import ru.yandex.speechkit.Recognition;
import ru.yandex.speechkit.Recognizer;
import ru.yandex.speechkit.RecognizerListener;
import ru.yandex.speechkit.SpeechKit;
import vs.pharmacist.objects.Medicine;
import vs.pharmacist.objects.Recipe;
import vs.pharmacist.objects.SpinnerMedicineAdapter;

public class MainActivity extends AppCompatActivity {

    TableLayout table_recipes;
    TableLayout table_inventory;
    ArrayList<Integer> positions;

    ArrayList<Medicine> medicines;
    ArrayList<Recipe> recipes;

    final Context context = this;

    TabHost th;

    Medicine selectedSpinnerMedicine;

    SpeechKit speech;

    FloatingActionButton fab;
    int selected_items;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        speech = SpeechKit.getInstance();
        speech.configure(this, "31880ffc-05bc-4a21-8285-af0bc321ed68");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnTouchListener(new FAB_onTouchListener());

        Intent i = getIntent();

        medicines = (ArrayList<Medicine>) i.getSerializableExtra("medicines");
        if (medicines == null) medicines = new ArrayList<>();
        table_inventory = (TableLayout) findViewById(R.id.tableInventory);
        fillTableWithMedicines(medicines, table_inventory);

        recipes = (ArrayList<Recipe>) i.getSerializableExtra("recipes");
        if (recipes == null) recipes = new ArrayList<>();
        table_recipes = (TableLayout) findViewById(R.id.tableRecipes);
        fillTableWithRecipes(recipes, table_recipes);

        th = (TabHost) findViewById(R.id.mainTabHost);
        th.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                new CheckBox_OnClickListener().onCheckedChanged(null, false);
            }
        });
        th.setup();
        TabHost.TabSpec ts = th.newTabSpec("tabInventory");
        ts.setContent(R.id.tabInventory);
        ts.setIndicator("Инвентарь");
        th.addTab(ts);
        ts = th.newTabSpec("tabRecipes");
        ts.setContent(R.id.tabRecipes);
        ts.setIndicator("Рецепты");
        th.addTab(ts);
        th.setCurrentTab(0);

    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            saveAll();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            stopService(NotificationService.startingIntent);
        } catch (Exception ignored) {
        }
        startService(new Intent(this, NotificationService.class));
    }

    public void fillTableWithMedicines(ArrayList<Medicine> medicines, TableLayout table) {
        CheckBox_OnClickListener checkBox_onClickListener = new CheckBox_OnClickListener();
        for (Medicine m : medicines) {
            LinearLayout ll = new LinearLayout(this);
            ll.setTag(m);
            CheckBox cb = new CheckBox(this);
            cb.setText(m.toString());
            cb.setOnCheckedChangeListener(checkBox_onClickListener);
            ll.addView(cb);
            table.addView(ll);
        }
    }
    public void fillTableWithRecipes(ArrayList<Recipe> recipes, TableLayout table) {
        CheckBox_OnClickListener checkBox_onClickListener = new CheckBox_OnClickListener();
        for (Recipe r : recipes) {
            LinearLayout ll = new LinearLayout(this);
            ll.setTag(r);
            CheckBox cb = new CheckBox(this);
            cb.setText(r.toString());
            cb.setOnCheckedChangeListener(checkBox_onClickListener);
            ll.addView(cb);
            table.addView(ll);
        }
    }
    public class CheckBox_OnClickListener implements CompoundButton.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            selected_items = 0;
            positions = new ArrayList<>();

            if (th.getCurrentTab() == 1)
                for (int i = 0; i < table_recipes.getChildCount(); i++) {
                    LinearLayout ll1 = (LinearLayout) table_recipes.getChildAt(i);
                    if (((CheckBox) ll1.getChildAt(0)).isChecked()) {
                        selected_items++;
                        positions.add(i);
                    }
                }
            else
                for (int i = 0; i < table_inventory.getChildCount(); i++) {
                    LinearLayout ll1 = (LinearLayout) table_inventory.getChildAt(i);
                    if (((CheckBox) ll1.getChildAt(0)).isChecked()) {
                        selected_items++;
                        positions.add(i);
                    }
                }

            switch (selected_items) {
                case 0:
                    fab.setImageResource(R.drawable.ic_action_keyboard_voice);
                    break;
                case 1:
                    fab.setImageResource(R.drawable.ic_action_edit);
                    break;
                default:
                    fab.setImageResource(R.drawable.ic_action_delete);
            }
        }
    }
    public class FAB_onTouchListener implements View.OnTouchListener {

        SpeechButton_OnTouchListener sb_otl;

        FAB_onTouchListener() {
            sb_otl = new SpeechButton_OnTouchListener();
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (selected_items) {
                case 0:
                    sb_otl.onTouch(v, event);
                    break;
                case 1:
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Необходим выбор действия")
                            .setMessage("Вы хотите удалить или отредактировать элемент?")
                            .setPositiveButton("ИЗМЕНИТЬ", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {editHandler();}
                            })
                            .setNegativeButton("УДАЛИТЬ", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) { deleteHandler(); }
                            })
                            .create().show();
                    }
                    break;
                default:
                    if (event.getAction() == MotionEvent.ACTION_UP) deleteHandler();
            }
            new CheckBox_OnClickListener().onCheckedChanged(null, false);
            return true;
        }
    }
    public void editHandler() {
        if (th.getCurrentTab() == 0) {
            LayoutInflater li = LayoutInflater.from(this);
            final View editMedicineView = li.inflate(R.layout.activity_add_medicine, null);
            Medicine m = medicines.get(positions.get(0));
            editMedicineView.setTag(m);
            final EditText mCount = (EditText) editMedicineView.findViewById(R.id.mCount);
            final EditText mName = (EditText) editMedicineView.findViewById(R.id.mName);
            final RadioButton rbPcs = (RadioButton) editMedicineView.findViewById(R.id.rb_pcs);
            final RadioButton rbMillis = (RadioButton) editMedicineView.findViewById(R.id.rb_millis);

            mCount.setText(Integer.toString(m.getCount()));
            mName.setText(m.getName());
            if (m.getCountType() == Medicine.CountType.COUNT_PCS) rbPcs.setChecked(true);
            else rbMillis.setChecked(true);

            new AlertDialog.Builder(this).setView(editMedicineView).
                    setTitle("Редактирование лекарственного средства").
                    setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (mName.getText().toString().length() == 0) {
                                Toast.makeText(context, "Имя не может быть пустым!", Toast.LENGTH_LONG).show();
                                return;
                            }
                            if (mCount.getText().toString().length() == 0) {
                                Toast.makeText(context, "Значение количества не может быть неопределённым!", Toast.LENGTH_LONG).show();
                                return;
                            }
                            Medicine m = (Medicine) editMedicineView.getTag();
                            m.setName(mName.getText().toString());
                            m.setCount(Integer.parseInt(mCount.getText().toString()));
                            if (rbPcs.isChecked())
                                m.setCountType(Medicine.CountType.COUNT_PCS);
                            else m.setCountType(Medicine.CountType.COUNT_MILLIS);
                            Toast.makeText(context, "Успешно отредактировано!", Toast.LENGTH_SHORT).show();
                            table_inventory.removeAllViews();
                            table_recipes.removeAllViews();
                            fillTableWithMedicines(medicines, table_inventory);
                            fillTableWithRecipes(recipes, table_recipes);
                        }
                    }).setNegativeButton("ОТМЕНА", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {}
            }).show();
        } else {
            final Recipe r = (Recipe) table_recipes.getChildAt(positions.get(0)).getTag();
            LayoutInflater li = LayoutInflater.from(this);
            View editRecipeView = li.inflate(R.layout.activity_add_recipe, null);

            LinearLayout[] ll_arr = new LinearLayout[table_inventory.getChildCount()];
            int position = -1;
            for (int c = 0; c < table_inventory.getChildCount(); c++) {
                ll_arr[c] = (LinearLayout) table_inventory.getChildAt(c);
                Medicine m = (Medicine) ll_arr[c].getTag();
                if (position == -1 && m.equals(r.getMedicine())) {
                    position = c;
                }
            }
            final SpinnerMedicineAdapter sma = new SpinnerMedicineAdapter(this,
                    R.layout.support_simple_spinner_dropdown_item, ll_arr);
            final Spinner medicineSpinner = (Spinner) editRecipeView.findViewById(R.id.addRecipe_medicines);
            medicineSpinner.setAdapter(sma);
            medicineSpinner.setSelection(position);

            final EditText count = (EditText) editRecipeView.findViewById(R.id.addRecipe_count);
            count.setText(Integer.toString(r.getCount()));
            final EditText day = (EditText) editRecipeView.findViewById(R.id.addRecipe_day);
            final EditText hour = (EditText) editRecipeView.findViewById(R.id.addRecipe_hour);
            final EditText minute = (EditText) editRecipeView.findViewById(R.id.addRecipe_minute);

            final RadioButton rbDaily = (RadioButton) editRecipeView.findViewById(R.id.addRecipe_rb_daily);
            final RadioButton rbWeekly = (RadioButton) editRecipeView.findViewById(R.id.addRecipe_rb_weekly);
            final RadioButton rbMonthly = (RadioButton) editRecipeView.findViewById(R.id.addRecipe_rb_monthly);

            final LinearLayout timeTable = (LinearLayout) editRecipeView.findViewById(R.id.addrecipe_timeTable);
            boolean dataGot = false;
            for (Recipe.Time t : r.getTime()) {
                if (!dataGot) {
                    if (t.getDayInWeek() == 0 && t.getDayInMonth() == 0) {
                        rbDaily.setChecked(true);
                        rbWeekly.setEnabled(false);
                        rbMonthly.setEnabled(false);
                        day.setHint("День");
                        day.setEnabled(false);
                    } else if (t.getDayInWeek() == 0) {
                        rbDaily.setEnabled(false);
                        rbWeekly.setEnabled(false);
                        rbMonthly.setChecked(true);
                        day.setHint("День месяца");
                        day.setEnabled(true);
                    } else {
                        rbDaily.setEnabled(false);
                        rbWeekly.setChecked(true);
                        rbMonthly.setEnabled(false);
                        day.setHint("День недели");
                        day.setEnabled(true);
                    }
                    dataGot = true;
                }
                LinearLayout ll = new LinearLayout(context);
                ll.setTag(t);
                CheckBox cb = new CheckBox(context);
                TextView tv = new TextView(context);
                tv.setText(t.toString());
                ll.addView(cb);
                ll.addView(tv);
                timeTable.addView(ll);
            }

            final Button add = (Button) editRecipeView.findViewById(R.id.addRecipe_add);
            final Button remove = (Button) editRecipeView.findViewById(R.id.addRecipe_remove);
            remove.setEnabled(true);

            add.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (hour.getText().toString().length() == 0) {
                        Toast.makeText(MainActivity.this, "Введите значение часа!", Toast.LENGTH_LONG).show();
                        return;
                    } else if (returnByteValue(hour) > 23) {
                        Toast.makeText(MainActivity.this, "Значение часа не может превышать 23!", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (minute.getText().toString().length() == 0) {
                        Toast.makeText(MainActivity.this, "Введите значение минут!", Toast.LENGTH_LONG).show();
                        return;
                    } else if (returnByteValue(minute) > 59) {
                        Toast.makeText(MainActivity.this, "Значение минуты не может превышать 59!", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (rbMonthly.isChecked() || rbWeekly.isChecked()) {
                        if (day.getText().toString().length() == 0) {
                            Toast.makeText(MainActivity.this, "Введите значение дня!", Toast.LENGTH_LONG).show();
                            return;
                        } else if (rbMonthly.isChecked()) {
                            if (returnByteValue(day) > 28) {
                                Toast.makeText(MainActivity.this, "Значение дня месяца не может превышать 28!", Toast.LENGTH_LONG).show();
                                return;
                            }
                            if (returnByteValue(day) == 0) {
                                Toast.makeText(MainActivity.this, "Значение дня месяца не может быть равным нулю!", Toast.LENGTH_LONG).show();
                                return;
                            }
                        } else if (rbWeekly.isChecked()) {
                            if (returnByteValue(day) > 7) {
                                Toast.makeText(MainActivity.this, "Значение дня недели не может превышать 7!", Toast.LENGTH_LONG).show();
                                return;
                            }
                            if (returnByteValue(day) == 0) {
                                Toast.makeText(MainActivity.this, "Значение дня недели не может быть равным нулю!", Toast.LENGTH_LONG).show();
                                return;
                            }
                        }
                    }
                    Recipe.Time t;
                    if (rbMonthly.isChecked()) {
                        t = new Recipe.Time(returnByteValue(day),
                                returnByteValue(hour), returnByteValue(minute), null);
                        rbWeekly.setEnabled(false);
                        rbDaily.setEnabled(false);
                    } else if (rbWeekly.isChecked()) {
                        t = new Recipe.Time(returnByteValue(day),
                                returnByteValue(hour), returnByteValue(minute));
                        rbMonthly.setEnabled(false);
                        rbDaily.setEnabled(false);
                    } else {
                        t = new Recipe.Time(returnByteValue(hour), returnByteValue(minute));
                        rbMonthly.setEnabled(false);
                        rbWeekly.setEnabled(false);
                    }

                    boolean isExists = false;
                    for (int i = 0; i < timeTable.getChildCount(); i++) {
                        Recipe.Time t1 = (Recipe.Time) timeTable.getChildAt(i).getTag();
                        if (t1.equals(t)) {
                            isExists = true;
                            break;
                        }
                    }
                    if (isExists) {
                        Toast.makeText(MainActivity.this, "Такое время уже существует!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    LinearLayout ll = new LinearLayout(context);
                    ll.setTag(t);
                    CheckBox cb = new CheckBox(context);
                    TextView tv = new TextView(context);
                    tv.setText(t.toString());
                    ll.addView(cb);
                    ll.addView(tv);
                    timeTable.addView(ll);
                    remove.setEnabled(true);
                }
            });

            remove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (int i = timeTable.getChildCount() - 1; i >= 0; i--) {
                        LinearLayout ll = (LinearLayout) timeTable.getChildAt(i);
                        CheckBox cb = (CheckBox) ll.getChildAt(0);
                        if (cb.isChecked()) timeTable.removeView(ll);
                    }
                    if (timeTable.getChildCount() == 0) {
                        remove.setEnabled(false);
                        rbDaily.setEnabled(true);
                        rbMonthly.setEnabled(true);
                        rbWeekly.setEnabled(true);
                    }
                }
            });

            rbDaily.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        day.setEnabled(false);
                        day.setHint("День");
                    }
                    day.setText("");
                }
            });
            rbMonthly.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        day.setEnabled(true);
                        day.setHint("День месяца");
                    }
                    day.setText("");
                }
            });
            rbWeekly.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        day.setEnabled(true);
                        day.setHint("День недели");
                    }
                    day.setText("");
                }
            });

            new AlertDialog.Builder(this).setTitle("Изменение рецепта").setView(editRecipeView).
                    setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (count.getText().toString().length() == 0) {
                                Toast.makeText(context, "Значение количества не может быть неопределённым!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (timeTable.getChildCount() == 0) {
                                Toast.makeText(context, "Должно быть хотя бы одно время!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            ArrayList<Recipe.Time> times = new ArrayList<>();
                            for (int i = 0; i < timeTable.getChildCount(); i++) {
                                Recipe.Time t = (Recipe.Time) timeTable.getChildAt(i).getTag();
                                times.add(t);
                            }
                            Medicine m = (Medicine) sma.getItem(medicineSpinner.getSelectedItemPosition()).getTag();
                            r.setMedicine(m);
                            r.setTime(times);
                            r.setCount(returnIntegerValue(count));
                            table_recipes.removeAllViews();
                            fillTableWithRecipes(recipes, table_recipes);
                        }
                    })
                    .show();
        }
    }
    public void deleteHandler() {
        if (th.getCurrentTab() == 1) {
            for (int k = positions.size() - 1; k >= 0; k--) {
                Recipe r = (Recipe) table_recipes.getChildAt(positions.get(k)).getTag();
                for (int l = recipes.size() - 1; l >= 0; l--) {
                    if (r == recipes.get(l)) recipes.remove(l);
                }
                table_recipes.removeViewAt(positions.get(k));
            }
        } else
            for (int k = positions.size() - 1; k >= 0; k--) {
                LinearLayout ll = (LinearLayout) table_inventory.getChildAt(positions.get(k));
                Medicine m = (Medicine) ll.getTag();
                for (int l = 0; l < medicines.size(); l++)
                    if (medicines.get(l).equals(m))
                        medicines.remove(l);

                for (int l = recipes.size() - 1; l >= 0; l--)
                    if (recipes.get(l).getMedicine().equals(m))
                        recipes.remove(l);

                for (int l = table_recipes.getChildCount() - 1; l >= 0; l--) {
                    Recipe r = (Recipe) table_recipes.getChildAt(l).getTag();
                    if (r.getMedicine().equals(m))
                        table_recipes.removeViewAt(l);
                }
                table_inventory.removeViewAt(positions.get(k));
            }
        Log.d("DELETE", "medicines: " + medicines.size() + "; recipes: " + recipes.size());
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        MenuInflater mi = new MenuInflater(this);
        mi.inflate(R.menu.menu_main, menu);

        int checkedCount = 0;
        positions = new ArrayList<>();

        if (th.getCurrentTab() == 1) {
            for (int i = 0; i < table_recipes.getChildCount(); i++) {
                LinearLayout ll1 = (LinearLayout) table_recipes.getChildAt(i);
                if (((CheckBox) ll1.getChildAt(0)).isChecked()) {
                    checkedCount++;
                    positions.add(i);
                }
            }
        } else {
            for (int i = 0; i < table_inventory.getChildCount(); i++) {
                LinearLayout ll1 = (LinearLayout) table_inventory.getChildAt(i);
                if (((CheckBox) ll1.getChildAt(0)).isChecked()) {
                    checkedCount++;
                    positions.add(i);
                }
            }
        }

        if (checkedCount == 0)
            return super.onPrepareOptionsMenu(menu);
        if (checkedCount == 1) {
            menu.add("Изменить");
        }
        menu.add("Удалить");

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        fixConnections();

        // проверяем, выбран ли элемент меню...
        switch (item.getItemId()) {
            case R.id.pharm_find:
                Intent i = new Intent(this, MapsActivity.class);
                startActivity(i);
                return super.onOptionsItemSelected(item);
        }

        // или выбран элемент выпадающего меню
        switch (item.getTitle().toString()) {
            case "Удалить":

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void addNewRecipe(View v) {
        if (medicines.size() == 0) {
            Toast.makeText(this, "У вас нет лекарств! Чтобы добавить новый рецепт, пожалуйста, добавьте хотя бы одно лекарство!", Toast.LENGTH_LONG).show();
            return;
        }

        fixConnections();
        LayoutInflater li = LayoutInflater.from(this);
        View addRecipeView = li.inflate(R.layout.activity_add_recipe, null);

        final RadioButton rbDaily = (RadioButton) addRecipeView.findViewById(R.id.addRecipe_rb_daily);
        final RadioButton rbWeekly = (RadioButton) addRecipeView.findViewById(R.id.addRecipe_rb_weekly);
        final RadioButton rbMonthly = (RadioButton) addRecipeView.findViewById(R.id.addRecipe_rb_monthly);

        final EditText day = (EditText) addRecipeView.findViewById(R.id.addRecipe_day);
        final EditText hour = (EditText) addRecipeView.findViewById(R.id.addRecipe_hour);
        final EditText minute = (EditText) addRecipeView.findViewById(R.id.addRecipe_minute);

        final Button add = (Button) addRecipeView.findViewById(R.id.addRecipe_add);
        final Button remove = (Button) addRecipeView.findViewById(R.id.addRecipe_remove);

        final Spinner medicinesSpinner = (Spinner) addRecipeView.findViewById(R.id.addRecipe_medicines);
        final LinearLayout timeTable = (LinearLayout) addRecipeView.findViewById(R.id.addrecipe_timeTable);

        final EditText count = (EditText) addRecipeView.findViewById(R.id.addRecipe_count);

        LinearLayout[] ll_arr = new LinearLayout[table_inventory.getChildCount()];
        for (int i = 0; i < table_inventory.getChildCount(); i++) {
            LinearLayout ll = (LinearLayout) table_inventory.getChildAt(i);
            ll_arr[i] = ll;
        }
        final SpinnerMedicineAdapter sma = new SpinnerMedicineAdapter(this, R.layout.support_simple_spinner_dropdown_item, ll_arr);
        medicinesSpinner.setAdapter(sma);
        medicinesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSpinnerMedicine = (Medicine) sma.getItem(position).getTag();
                if (selectedSpinnerMedicine.getCountType() == Medicine.CountType.COUNT_PCS)
                    count.setHint("Количество (в штуках)");
                else count.setHint("Количество (в миллилитрах)");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        rbDaily.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    day.setEnabled(false);
                    day.setHint("День");
                }
                day.setText("");
            }
        });
        rbMonthly.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    day.setEnabled(true);
                    day.setHint("День месяца");
                }
                day.setText("");
            }
        });
        rbWeekly.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    day.setEnabled(true);
                    day.setHint("День недели");
                }
                day.setText("");
            }
        });

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hour.getText().toString().length() == 0) {
                    Toast.makeText(MainActivity.this, "Введите значение часа!", Toast.LENGTH_LONG).show();
                    return;
                } else if (returnByteValue(hour) > 23) {
                    Toast.makeText(MainActivity.this, "Значение часа не может превышать 23!", Toast.LENGTH_LONG).show();
                    return;
                }
                if (minute.getText().toString().length() == 0) {
                    Toast.makeText(MainActivity.this, "Введите значение минут!", Toast.LENGTH_LONG).show();
                    return;
                } else if (returnByteValue(minute) > 59) {
                    Toast.makeText(MainActivity.this, "Значение минуты не может превышать 59!", Toast.LENGTH_LONG).show();
                    return;
                }
                if (rbMonthly.isChecked() || rbWeekly.isChecked()) {
                    if (day.getText().toString().length() == 0) {
                        Toast.makeText(MainActivity.this, "Введите значение дня!", Toast.LENGTH_LONG).show();
                        return;
                    } else if (rbMonthly.isChecked()) {
                        if (returnByteValue(day) > 28) {
                            Toast.makeText(MainActivity.this, "Значение дня месяца не может превышать 28!", Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (returnByteValue(day) == 0) {
                            Toast.makeText(MainActivity.this, "Значение дня месяца не может быть равным нулю!", Toast.LENGTH_LONG).show();
                            return;
                        }
                    } else if (rbWeekly.isChecked()) {
                        if (returnByteValue(day) > 7) {
                            Toast.makeText(MainActivity.this, "Значение дня недели не может превышать 7!", Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (returnByteValue(day) == 0) {
                            Toast.makeText(MainActivity.this, "Значение дня недели не может быть равным нулю!", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                }
                Recipe.Time t;
                if (rbMonthly.isChecked()) {
                    t = new Recipe.Time(returnByteValue(day),
                            returnByteValue(hour), returnByteValue(minute), null);
                    rbWeekly.setEnabled(false);
                    rbDaily.setEnabled(false);
                } else if (rbWeekly.isChecked()) {
                    t = new Recipe.Time(returnByteValue(day),
                            returnByteValue(hour), returnByteValue(minute));
                    rbMonthly.setEnabled(false);
                    rbDaily.setEnabled(false);
                } else {
                    t = new Recipe.Time(returnByteValue(hour), returnByteValue(minute));
                    rbMonthly.setEnabled(false);
                    rbWeekly.setEnabled(false);
                }

                boolean isExists = false;
                for (int i = 0; i < timeTable.getChildCount(); i++) {
                    Recipe.Time t1 = (Recipe.Time) timeTable.getChildAt(i).getTag();
                    if (t1.equals(t)) {
                        isExists = true;
                        break;
                    }
                }
                if (isExists) {
                    Toast.makeText(MainActivity.this, "Такое время уже существует!", Toast.LENGTH_LONG).show();
                    return;
                }

                LinearLayout ll = new LinearLayout(context);
                ll.setTag(t);
                CheckBox cb = new CheckBox(context);
                TextView tv = new TextView(context);
                tv.setText(t.toString());
                ll.addView(cb);
                ll.addView(tv);
                timeTable.addView(ll);
                remove.setEnabled(true);
            }
        });
        remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = timeTable.getChildCount() - 1; i >= 0; i--) {
                    LinearLayout ll = (LinearLayout) timeTable.getChildAt(i);
                    CheckBox cb = (CheckBox) ll.getChildAt(0);
                    if (cb.isChecked()) timeTable.removeView(ll);
                }
                if (timeTable.getChildCount() == 0) {
                    remove.setEnabled(false);
                    rbDaily.setEnabled(true);
                    rbMonthly.setEnabled(true);
                    rbWeekly.setEnabled(true);
                }
            }
        });

        new AlertDialog.Builder(this).setView(addRecipeView).
                setTitle("Добавление рецепта").setPositiveButton("ОК", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (count.getText().toString().length() == 0) {
                    Toast.makeText(context, "Значение количества не может быть неопределённым!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (Integer.parseInt(count.getText().toString()) == 0) {
                    Toast.makeText(context, "Значение количества не может быть равно нулю!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (timeTable.getChildCount() == 0) {
                    Toast.makeText(context, "Должно быть хотя бы одно время!", Toast.LENGTH_SHORT).show();
                    return;
                }
                ArrayList<Recipe.Time> times = new ArrayList<>();
                for (int i = 0; i < timeTable.getChildCount(); i++) {
                    Recipe.Time t = (Recipe.Time) timeTable.getChildAt(i).getTag();
                    times.add(t);
                }
                Recipe r = new Recipe(selectedSpinnerMedicine, times, returnIntegerValue(count));
                recipes.add(r);
                table_recipes.removeAllViews();
                fillTableWithRecipes(recipes, table_recipes);
            }
        }).setNegativeButton("ОТМЕНА", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        }).show();
    }

    private void codeOfAddingMedicine(@Nullable String name, @Nullable Integer count) {
        fixConnections();
        LayoutInflater li = LayoutInflater.from(this);
        View addMedicineView = li.inflate(R.layout.activity_add_medicine, null);
        final EditText mCount = (EditText) addMedicineView.findViewById(R.id.mCount);
        if (count != null) mCount.setText(count.toString());
        final EditText mName = (EditText) addMedicineView.findViewById(R.id.mName);
        if (name != null) mName.setText(name);
        final RadioButton rbPcs = (RadioButton) addMedicineView.findViewById(R.id.rb_pcs);
        new AlertDialog.Builder(this).setView(addMedicineView).
                setTitle("Добавление в инвентарь лекарственного средства").
                setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mName.getText().toString().length() == 0) {
                            Toast.makeText(context, "Имя не может быть пустым!", Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (mCount.getText().toString().length() == 0) {
                            Toast.makeText(context, "Значение количества не может быть неопределённым!", Toast.LENGTH_LONG).show();
                            return;
                        }
                        Medicine.CountType ct;
                        if (rbPcs.isChecked()) ct = Medicine.CountType.COUNT_PCS;
                        else ct = Medicine.CountType.COUNT_MILLIS;
                        Medicine m = new Medicine(mName.getText().toString(), ct, Integer.parseInt(mCount.getText().toString()));
                        medicines.add(m);
                        table_inventory.removeAllViews();
                        fillTableWithMedicines(medicines, table_inventory);
                    }
                }).setNegativeButton("ОТМЕНА", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        }).show();
    }
    public void addNewMedicine(View v) {
        codeOfAddingMedicine(null, null);
    }

    public class SpeechButton_OnTouchListener implements View.OnTouchListener {

        long now;

        Recognizer mRecognizer;
        ProgressDialog mPD;

        boolean isRecorded = false;
        boolean isError = false;

        SpeechButton_OnTouchListener() {}

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    long duration = System.currentTimeMillis() - now;
                    if (isRecorded && !isError) {
                        if (duration < 500) {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Информация!")
                                    .setMessage("Чтобы добавить новое лекарство, скажите \"купил(а) *название лекарства*\".")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {}
                                    })
                                    .create().show();
                            mRecognizer.cancel();
                            return false;
                        }
                        mRecognizer.finishRecording();
                        mPD = new ProgressDialog(MainActivity.this);
                        mPD.setTitle("Подождите, пожалуйста...");
                        mPD.setMessage("Идёт распознавание...");
                        mPD.show();
                        isRecorded = false;
                    } else isError = false;
                    break;
                case MotionEvent.ACTION_DOWN:
                    now = System.currentTimeMillis();
                    mRecognizer = Recognizer.create(Recognizer.Language.RUSSIAN, Recognizer.Model.NOTES, new RecognizerListener() {
                        @Override
                        public void onRecordingBegin(Recognizer recognizer) {

                        }
                        @Override
                        public void onSpeechDetected(Recognizer recognizer) {

                        }
                        @Override
                        public void onSpeechEnds(Recognizer recognizer) {

                        }
                        @Override
                        public void onRecordingDone(Recognizer recognizer) {}
                        @Override
                        public void onSoundDataRecorded(Recognizer recognizer, byte[] bytes) {

                        }
                        @Override
                        public void onPowerUpdated(Recognizer recognizer, float v) {

                        }
                        @Override
                        public void onPartialResults(Recognizer recognizer, Recognition recognition, boolean b) {

                        }
                        @Override
                        public void onRecognitionDone(Recognizer recognizer, Recognition recognition) {
                            if (mPD != null) {
                                mPD.hide();
                                recognizer.cancel();
                                String[] args = recognition.getBestResultText().split(" ");
                                String result = "";
                                if (args.length > 1) {
                                    for (int i = 1; i < args.length; i++) {
                                        result += args[i];
                                        if (i != args.length - 1) result += " ";
                                    }
                                }
                                codeOfAddingMedicine(result, null);
                            }
                        }

                        @Override
                        public void onError(Recognizer recognizer, Error error) {
                            isError = true;
                            if (mPD != null) mPD.hide();
                            if (error.getCode() == Error.ERROR_CANCELED) return;
                            new AlertDialog.Builder(MainActivity.this).setTitle("Ошибка!")
                                    .setMessage(error.getString())
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {}
                                    })
                                    .create().show();
                        }
                    });
                    if (ActivityCompat.checkSelfPermission(MainActivity.this,
                            android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return false;
                    isRecorded = true;
                    Vibrator vibra = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    vibra.vibrate(new long[] {100, 100, 100}, -1);
                    mRecognizer.start();
                    break;
            }
            return false;
        }
    }

    public Byte returnByteValue(EditText editText) {
        return Byte.parseByte(editText.getText().toString());
    }
    public Integer returnIntegerValue(EditText editText) {
        return Integer.parseInt(editText.getText().toString());
    }

    // честно говоря, это такая костылина,
    // которую я бы в своей жизни никогда
    // бы не использовал, но увы пришлось
    public void fixConnections() {
        if (recipes != null && medicines != null)
            for (Recipe r: recipes) {
                Medicine medicine = null;
                for (Medicine m: medicines) {
                    if (m.equals(r.getMedicine())) {
                        medicine = m;
                        break;
                    }
                }
                if (medicine != null) {
                    r.setMedicine(medicine);
                }
            }
    }
    public void saveAll() throws Exception {
        DataSaver.save(this, medicines, recipes);
    }
}
