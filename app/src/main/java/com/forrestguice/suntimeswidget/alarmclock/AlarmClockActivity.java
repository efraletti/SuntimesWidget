/**
    Copyright (C) 2018 Forrest Guice
    This file is part of SuntimesWidget.

    SuntimesWidget is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SuntimesWidget is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SuntimesWidget.  If not, see <http://www.gnu.org/licenses/>.
*/ 

package com.forrestguice.suntimeswidget.alarmclock;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.forrestguice.suntimeswidget.AboutDialog;
import com.forrestguice.suntimeswidget.AlarmDialog;
import com.forrestguice.suntimeswidget.HelpDialog;
import com.forrestguice.suntimeswidget.R;
import com.forrestguice.suntimeswidget.SuntimesActivity;
import com.forrestguice.suntimeswidget.SuntimesUtils;
import com.forrestguice.suntimeswidget.calculator.SuntimesMoonData;
import com.forrestguice.suntimeswidget.calculator.SuntimesRiseSetData;
import com.forrestguice.suntimeswidget.calculator.SuntimesRiseSetDataset;
import com.forrestguice.suntimeswidget.settings.AppSettings;
import com.forrestguice.suntimeswidget.settings.SolarEvents;
import com.forrestguice.suntimeswidget.settings.WidgetSettings;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

public class AlarmClockActivity extends AppCompatActivity
{
    public static final String EXTRA_SHOWBACK = "showBack";
    public static final String EXTRA_SOLAREVENT = "solarevent";
    public static final int REQUEST_RINGTONE = 10;

    private static final String DIALOGTAG_EVENT_FAB = "eventfab";
    private static final String DIALOGTAG_EVENT = "event";
    private static final String DIALOGTAG_HELP = "help";
    private static final String DIALOGTAG_ABOUT = "about";

    private static final String KEY_SELECTED_ROWID = "selectedID";
    private static final String KEY_SELECTED_LOCATION = "selectedLocation";
    private static final String KEY_LISTVIEW_TOP = "alarmlisttop";
    private static final String KEY_LISTVIEW_INDEX = "alarmlistindex";

    private ActionBar actionBar;
    private ListView alarmList;
    private View emptyView;
    private FloatingActionButton actionButton;

    private AlarmClockAdapter adapter = null;
    private Long t_selectedItem = null;
    private WidgetSettings.Location t_selectedLocation = null;

    private AlarmClockListTask updateTask = null;
    private static final SuntimesUtils utils = new SuntimesUtils();

    public AlarmClockActivity()
    {
        super();
    }

    @Override
    protected void attachBaseContext(Context newBase)
    {
        Context context = AppSettings.initLocale(newBase);
        super.attachBaseContext(context);
    }

    /**
     * OnCreate: the Activity initially created
     * @param icicle a Bundle containing saved state
     */
    @Override
    public void onCreate(Bundle icicle)
    {
        setTheme(AppSettings.loadTheme(this));
        super.onCreate(icicle);
        initLocale(this);
        setContentView(R.layout.layout_activity_alarmclock);
        initViews(this);
        handleIntent(getIntent());
    }

    @Override
    public void onNewIntent( Intent intent )
    {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    protected void handleIntent(Intent intent)
    {
        String param_action = intent.getAction();
        intent.setAction(null);

        if (param_action != null)
        {
            if (param_action.equals(AlarmClock.ACTION_SET_ALARM))
            {
                String param_label = intent.getStringExtra(AlarmClock.EXTRA_MESSAGE);
                int param_hour = intent.getIntExtra(AlarmClock.EXTRA_HOUR, -1);
                int param_minute = intent.getIntExtra(AlarmClock.EXTRA_MINUTES, -1);

                boolean param_vibrate = getDefaultVibrate();
                Uri param_ringtoneUri = getDefaultRingtoneUri();
                if (Build.VERSION.SDK_INT >= 19)
                {
                    param_vibrate = intent.getBooleanExtra(AlarmClock.EXTRA_VIBRATE, param_vibrate);
                    String param_ringtoneUriString = intent.getStringExtra(AlarmClock.EXTRA_RINGTONE);
                    if (param_ringtoneUriString != null) {
                        param_ringtoneUri = Uri.parse(param_ringtoneUriString);
                    }
                }

                SolarEvents param_event = SolarEvents.valueOf(intent.getStringExtra(AlarmClockActivity.EXTRA_SOLAREVENT), null);
                param_event = null;  // test clock mode :TODO remove line

                Log.i("AlarmClockActivity", "ACTION_SET_ALARM :: " + param_label + ", " + param_hour + ", " + param_minute + ", " + param_event);
                addAlarm(param_label, param_event, param_hour, param_minute, param_vibrate, param_ringtoneUri);
            }
        }
    }

    private void initLocale(Context context)
    {
        WidgetSettings.initDefaults(context);
        WidgetSettings.initDisplayStrings(context);
        SuntimesUtils.initDisplayStrings(context);
        SolarEvents.initDisplayStrings(context);
    }

    /**
     * OnStart: the Activity becomes visible
     */
    @Override
    public void onStart()
    {
        super.onStart();
        updateViews(this);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        FragmentManager fragments = getSupportFragmentManager();
        AlarmDialog eventDialog0 = (AlarmDialog) fragments.findFragmentByTag(DIALOGTAG_EVENT_FAB);
        if (eventDialog0 != null)
        {
            initEventDialog(eventDialog0, null);
            eventDialog0.setOnAcceptedListener(onActionButtonAccepted);
        }

        AlarmDialog eventDialog1 = (AlarmDialog) fragments.findFragmentByTag(DIALOGTAG_EVENT);
        if (eventDialog1 != null)
        {
            initEventDialog(eventDialog1, t_selectedLocation);
            eventDialog1.setOnAcceptedListener(onSolarEventChanged);
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public void onStop()
    {
        super.onStop();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }


    @Override
    public void onSaveInstanceState( Bundle outState )
    {
        super.onSaveInstanceState(outState);
        saveListViewPosition(outState);

        if (t_selectedItem != null) {
            outState.putString(KEY_SELECTED_ROWID, t_selectedItem.toString());
        }

        if (t_selectedLocation != null) {
            outState.putParcelable(KEY_SELECTED_LOCATION, t_selectedLocation);
        }
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedState)
    {
        super.onRestoreInstanceState(savedState);
        restoreListViewPosition(savedState);

        String idString = savedState.getString(KEY_SELECTED_ROWID);
        if (idString == null) {
            t_selectedItem = null;
        } else {
            try {
                t_selectedItem = Long.parseLong(idString);
            } catch (NumberFormatException e) {
                Log.w("onRestoreInstanceState", "KEY_SELECTED_ROWID is invalid! not a Long.. ignoring: " + idString);
                t_selectedItem = null;
            }
        }

        t_selectedLocation = savedState.getParcelable(KEY_SELECTED_LOCATION);
    }

    /**
     * ..based on stack overflow answer by ian
     * https://stackoverflow.com/questions/3014089/maintain-save-restore-scroll-position-when-returning-to-a-listview
     */
    private void saveListViewPosition( Bundle outState)
    {
        int i = alarmList.getFirstVisiblePosition();
        outState.putInt(KEY_LISTVIEW_INDEX, i);

        int top = 0;
        View firstItem = alarmList.getChildAt(0);
        if (firstItem != null)
        {
            top = firstItem.getTop() - alarmList.getPaddingTop();
        }
        outState.putInt(KEY_LISTVIEW_TOP, top);
    }

    private void restoreListViewPosition(@NonNull Bundle savedState )
    {
        int i = savedState.getInt(KEY_LISTVIEW_INDEX, -1);
        if (i >= 0)
        {
            int top = savedState.getInt(KEY_LISTVIEW_TOP, 0);
            alarmList.setSelectionFromTop(i, top);
        }
    }

    /**
     * initialize ui/views
     * @param context a context used to access resources
     */
    protected void initViews(Context context)
    {
        SuntimesUtils.initDisplayStrings(context);

        Toolbar menuBar = (Toolbar) findViewById(R.id.app_menubar);
        setSupportActionBar(menuBar);
        actionBar = getSupportActionBar();

        if (actionBar != null)
        {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            boolean showBack = getIntent().getBooleanExtra(EXTRA_SHOWBACK, false);
            if (!showBack) {
                actionBar.setHomeAsUpIndicator(R.drawable.ic_action_suntimes);
            }
        }

        actionButton = (FloatingActionButton) findViewById(R.id.btn_addAlarm);
        actionButton.setOnClickListener(onActionButtonClick);

        alarmList = (ListView)findViewById(R.id.alarmList);
        emptyView = findViewById(android.R.id.empty);
        emptyView.setOnClickListener(onEmptyViewClick);
    }

    /**
     * onActionButtonClick
     */
    private View.OnClickListener onActionButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v)
        {
            FragmentManager fragments = getSupportFragmentManager();
            AlarmDialog eventDialog0 = (AlarmDialog) fragments.findFragmentByTag(DIALOGTAG_EVENT_FAB);
            if (eventDialog0 == null)
            {
                final AlarmDialog dialog = new AlarmDialog();
                initEventDialog(dialog, null);
                dialog.setChoice(SolarEvents.SUNRISE);
                dialog.setOnAcceptedListener(onActionButtonAccepted);
                dialog.show(getSupportFragmentManager(), DIALOGTAG_EVENT_FAB);
            }
        }
    };

    private DialogInterface.OnClickListener onActionButtonAccepted = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface d, int which)
        {
            addAlarm();
        }
    };

    protected void addAlarm()
    {
        FragmentManager fragments = getSupportFragmentManager();
        AlarmDialog dialog = (AlarmDialog) fragments.findFragmentByTag(DIALOGTAG_EVENT_FAB);
        addAlarm("", dialog.getChoice(), -1, -1, getDefaultVibrate(), getDefaultRingtoneUri());
    }
    protected void addAlarm(String label, SolarEvents event, int hour, int minute, boolean vibrate, Uri ringtoneUri)
    {
        AlarmClockUpdateTask task = new AlarmClockUpdateTask(AlarmClockActivity.this, true);
        task.setTaskListener(new AlarmClockUpdateTask.AlarmClockUpdateTaskListener()
        {
            @Override
            public void onFinished(Boolean result)
            {
                if (result) {
                    updateViews(AlarmClockActivity.this);
                }
            }
        });

        final AlarmClockItem alarm = new AlarmClockItem();
        alarm.enabled = getDefaultNewAlarmsEnabled();
        alarm.label = label;

        alarm.hour = hour;
        alarm.minute = minute;
        alarm.event = event;
        alarm.location = WidgetSettings.loadLocationPref(AlarmClockActivity.this, 0);

        alarm.repeating = false;

        alarm.vibrate = vibrate;
        alarm.ringtoneURI = (ringtoneUri != null ? ringtoneUri.toString() : null);
        if (alarm.ringtoneURI != null)
        {
            Ringtone ringtone = RingtoneManager.getRingtone(AlarmClockActivity.this, ringtoneUri);
            alarm.ringtoneName = ringtone.getTitle(AlarmClockActivity.this);
            ringtone.stop();
        }

        alarm.modified = true;
        task.execute(alarm);
    }

    public boolean getDefaultNewAlarmsEnabled()
    {
        return true;
    }

    public Uri getDefaultRingtoneUri()
    {
        return RingtoneManager.getActualDefaultRingtoneUri(AlarmClockActivity.this, RingtoneManager.TYPE_ALARM);
    }

    public boolean getDefaultVibrate()
    {
        return false;
    }

    /**
     * onSolarEventChanged
     */
    private DialogInterface.OnClickListener onSolarEventChanged = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface d, int which)
        {
            AlarmClockUpdateTask task = new AlarmClockUpdateTask(AlarmClockActivity.this);
            task.setTaskListener(onUpdateItem);

            AlarmClockItem item = adapter.findItem(t_selectedItem);
            t_selectedItem = null;

            if (item != null)
            {
                FragmentManager fragments = getSupportFragmentManager();
                AlarmDialog dialog = (AlarmDialog) fragments.findFragmentByTag(DIALOGTAG_EVENT);
                item.event = dialog.getChoice();
                item.modified = true;
                updateAlarmTime(AlarmClockActivity.this, item);
                task.execute(item);
            }
        }
    };

    /**
     * onEmptyViewClick
     */
    private View.OnClickListener onEmptyViewClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showHelp();
        }
    };

    /**
     * onUpdateFinished
     * The update task completed creating the adapter; set a listener on the completed adapter.
     */
    private AlarmClockListTask.AlarmClockListTaskListener onUpdateFinished = new AlarmClockListTask.AlarmClockListTaskListener()
    {
        @Override
        public void onFinished(AlarmClockAdapter result)
        {
            adapter = result;
            adapter.setAdapterListener(onAdapterAction);
        }
    };

    /**
     * onAdapterAction
     * An action was performed on an AlarmItem managed by the adapter; respond to it.
     */
    private AlarmClockAdapter.AlarmClockAdapterListener onAdapterAction = new AlarmClockAdapter.AlarmClockAdapterListener()
    {
        @Override
        public void onRequestLabel(AlarmClockItem forItem)
        {
            pickLabel(forItem);
        }

        @Override
        public void onRequestRingtone(AlarmClockItem forItem)
        {
            pickRingtone(forItem);
        }

        @Override
        public void onRequestSolarEvent(AlarmClockItem forItem)
        {
            pickSolarEvent(forItem);
        }

        @Override
        public void onRequestLocation(AlarmClockItem forItem)
        {
            pickLocation(forItem);
        }

        @Override
        public void onRequestTime(AlarmClockItem forItem)
        {
            pickTime(forItem);
        }
    };

    /**
     * onUpdateItem
     */
    private AlarmClockUpdateTask.AlarmClockUpdateTaskListener onUpdateItem = new AlarmClockUpdateTask.AlarmClockUpdateTaskListener()
    {
        @Override
        public void onFinished(Boolean result)
        {
            if (result && adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    };

    /**
     * updateViews
     * @param context context
     */
    protected void updateViews(Context context)
    {
        if (updateTask != null) {
            updateTask.cancel(true);
            updateTask = null;
        }

        updateTask = new AlarmClockListTask(this, alarmList, emptyView);
        updateTask.setTaskListener(onUpdateFinished);
        updateTask.execute();
    }

    /**
     * pickSolarEvent
     * @param item apply selected solar event to supplied AlarmClockItem
     */
    protected void pickSolarEvent(@NonNull AlarmClockItem item)
    {
        final AlarmDialog dialog = new AlarmDialog();
        initEventDialog(dialog, item.location);
        dialog.setChoice(item.event);
        dialog.setOnAcceptedListener(onSolarEventChanged);

        t_selectedItem = item.rowID;
        t_selectedLocation = item.location;
        dialog.show(getSupportFragmentManager(), DIALOGTAG_EVENT);
    }

    private void initEventDialog(AlarmDialog dialog, WidgetSettings.Location forLocation)
    {
        SuntimesRiseSetDataset sunData = new SuntimesRiseSetDataset(this, 0);
        SuntimesMoonData moonData = new SuntimesMoonData(this, 0);

        if (forLocation != null) {
            sunData.setLocation(forLocation);
            moonData.setLocation(forLocation);
        }

        sunData.calculateData();
        moonData.calculate();
        dialog.setData(this, sunData, moonData);
    }

    protected void pickLocation(@NonNull AlarmClockItem item)
    {
        t_selectedItem = item.rowID;
        // TODO
        //t_selectedItem = null;
    }

    /**
     * onPickTime
     */
    private TimePickerDialog.OnTimeSetListener onPickTime = new TimePickerDialog.OnTimeSetListener()
    {
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute)
        {
            AlarmClockItem item = adapter.findItem(t_selectedItem);
            t_selectedItem = null;

            if (item != null)
            {
                item.event = null;
                item.hour = hourOfDay;
                item.minute = minute;
                item.modified = true;
                updateAlarmTime(AlarmClockActivity.this, item);

                AlarmClockUpdateTask task = new AlarmClockUpdateTask(AlarmClockActivity.this);
                task.setTaskListener(onUpdateItem);
                task.execute(item);
            }
        }
    };

    /**
     * pickTime
     * @param item apply selected time to supplied AlarmClockItem
     */
    protected void pickTime(@NonNull AlarmClockItem item)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(item.timestamp);

        int hour = item.hour;
        if (hour < 0 || hour >= 24) {
            hour = calendar.get(Calendar.HOUR_OF_DAY);
        }

        int minute = item.minute;
        if (minute < 0 || minute >= 60) {
            minute = calendar.get(Calendar.MINUTE);
        }

        TimePickerDialog dialog = new TimePickerDialog(AlarmClockActivity.this, onPickTime, hour, minute, SuntimesUtils.is24());
        t_selectedItem = item.rowID;
        dialog.show();
    }

    /**
     * pickLabel
     * @param item apply selected label to supplied AlarmClockItem
     */
    protected void pickLabel(@NonNull AlarmClockItem item)
    {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(getString(R.string.alarmlabel_dialog_title));

        final EditText labelEdit = new EditText(this);
        labelEdit.setText(item.label);
        labelEdit.selectAll();
        labelEdit.requestFocus();
        dialog.setView(labelEdit);

        dialog.setPositiveButton(getString(R.string.alarmlabel_dialog_ok), new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                AlarmClockItem item = adapter.findItem(t_selectedItem);
                t_selectedItem = null;

                if (item != null)
                {
                    item.label = labelEdit.getText().toString();
                    item.modified = true;
                    AlarmClockUpdateTask task = new AlarmClockUpdateTask(AlarmClockActivity.this);
                    task.setTaskListener(onUpdateItem);
                    task.execute(item);
                }
            }
        });
        dialog.setNegativeButton(getString(R.string.alarmlabel_dialog_cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {}
        });

        final AlertDialog alertDialog = dialog.create();
        Window w = alertDialog.getWindow();
        if (w != null) {
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }

        t_selectedItem = item.rowID;
        alertDialog.show();
    }

    /**
     * pickRingtone
     * @param item apply selected ringtone to supplied AlarmClockItem
     */
    protected void pickRingtone(@NonNull AlarmClockItem item)
    {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, getDefaultRingtoneUri());
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (item.ringtoneURI != null ? Uri.parse(item.ringtoneURI) : null));
        t_selectedItem = item.rowID;
        startActivityForResult(intent, REQUEST_RINGTONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        AlarmClockItem item = adapter.findItem(t_selectedItem);
        switch (requestCode)
        {
            case REQUEST_RINGTONE:
                if (resultCode == RESULT_OK && item != null && data != null)
                {
                    Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
                    String ringtoneName = ringtone.getTitle(this);
                    ringtone.stop();

                    item.ringtoneName = (uri != null ? ringtoneName : null);
                    item.ringtoneURI = (uri != null ? uri.toString() : null);
                    item.modified = true;
                    Log.d("DEBUG", "uri: " + item.ringtoneURI + ", title: " + ringtoneName);

                    AlarmClockUpdateTask task = new AlarmClockUpdateTask(this);
                    task.setTaskListener(onUpdateItem);
                    task.execute(item);
                }
                t_selectedItem = null;
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * clearAlarms
     */
    protected void clearAlarms()
    {
        final Context context = this;
        AlertDialog.Builder confirm = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.clearalarms_dialog_title))
                .setMessage(context.getString(R.string.clearalarms_dialog_message))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(context.getString(R.string.clearalarms_dialog_ok), new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int whichButton)
                    {
                        AlarmClockDeleteTask clearTask = new AlarmClockDeleteTask(context);
                        clearTask.setTaskListener(new AlarmClockDeleteTask.AlarmClockDeleteTaskListener()
                        {
                            @Override
                            public void onFinished(Boolean result)
                            {
                                if (result)
                                {
                                    Toast.makeText(context, context.getString(R.string.clearalarms_toast_success), Toast.LENGTH_LONG).show();
                                    updateViews(context);
                                }
                            }
                        });
                        clearTask.execute();
                    }
                })
                .setNegativeButton(context.getString(R.string.clearalarms_dialog_cancel), null);
        confirm.show();
    }

    /**
     * showHelp
     */
    protected void showHelp()
    {
        HelpDialog helpDialog = new HelpDialog();
        helpDialog.setContent(getString(R.string.help_alarmclock));
        helpDialog.show(getSupportFragmentManager(), DIALOGTAG_HELP);
    }

    /**
     * showAbout
     */
    protected void showAbout()
    {
        AboutDialog aboutDialog = new AboutDialog();
        aboutDialog.show(getSupportFragmentManager(), DIALOGTAG_ABOUT);
    }

    /**
     * AlarmClockUpdateTask
     */
    public static class AlarmClockUpdateTask extends AsyncTask<AlarmClockItem, Void, Boolean>
    {
        protected AlarmClockDatabaseAdapter db;
        private boolean flag_add = false;

        public AlarmClockUpdateTask(Context context)
        {
            db = new AlarmClockDatabaseAdapter(context.getApplicationContext());
        }

        public AlarmClockUpdateTask(Context context, boolean flag_add)
        {
            db = new AlarmClockDatabaseAdapter(context.getApplicationContext());
            this.flag_add = flag_add;
        }

        @Override
        protected Boolean doInBackground(AlarmClockItem... items)
        {
            db.open();
            boolean updated = true;
            for (AlarmClockItem item : items) {
                updated = updated && ((flag_add
                        ? (db.addAlarm(item.asContentValues(false)) > 0)
                        : (db.updateAlarm(item.rowID, item.asContentValues(false)))));
            }
            db.close();
            return updated;
        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            if (listener != null)
                listener.onFinished(result);
        }

        protected AlarmClockUpdateTaskListener listener = null;
        public void setTaskListener( AlarmClockUpdateTaskListener l )
        {
            listener = l;
        }

        public static abstract class AlarmClockUpdateTaskListener
        {
            public void onFinished(Boolean result) {}
        }
    }

    /**
     * AlarmClockDeleteTask
     */
    public static class AlarmClockDeleteTask extends AsyncTask<Long, Void, Boolean>
    {
        protected AlarmClockDatabaseAdapter db;

        public AlarmClockDeleteTask(Context context)
        {
            db = new AlarmClockDatabaseAdapter(context.getApplicationContext());
        }

        @Override
        protected Boolean doInBackground(Long... rowIDs)
        {
            db.open();
            boolean removed = true;
            if (rowIDs.length > 0)
            {
                for (long rowID : rowIDs) {
                    removed = removed && db.removeAlarm(rowID);
                }
            } else {
                removed = db.clearAlarms();
            }
            db.close();
            return removed;
        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            if (listener != null)
                listener.onFinished(result);
        }

        protected AlarmClockDeleteTaskListener listener = null;
        public void setTaskListener( AlarmClockDeleteTaskListener l )
        {
            listener = l;
        }

        public static abstract class AlarmClockDeleteTaskListener
        {
            public void onFinished(Boolean result) {}
        }
    }

    /**
     * AlarmClockListTask
     */
    public static class AlarmClockListTask extends AsyncTask<String, AlarmClockItem, AlarmClockAdapter>
    {
        private AlarmClockDatabaseAdapter db;
        private WeakReference<Context> contextRef;
        private WeakReference<ListView> alarmListRef;
        private WeakReference<View> emptyViewRef;

        public AlarmClockListTask(Context context, ListView list, View emptyView)
        {
            contextRef = new WeakReference<>(context);
            alarmListRef = new WeakReference<>(list);
            emptyViewRef = new WeakReference<>(emptyView);
            db = new AlarmClockDatabaseAdapter(context.getApplicationContext());
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected AlarmClockAdapter doInBackground(String... strings)
        {
            ArrayList<AlarmClockItem> items = new ArrayList<>();

            db.open();
            Cursor cursor = db.getAllAlarms(0, true);
            while (!cursor.isAfterLast())
            {
                ContentValues entryValues = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(cursor, entryValues);

                AlarmClockItem item = new AlarmClockItem(entryValues);
                updateAlarmTime(contextRef.get(), item);
                items.add(item);
                publishProgress(item);

                cursor.moveToNext();
            }
            db.close();

            Context context = contextRef.get();
            if (context != null)
                return new AlarmClockAdapter(context, items);
            else return null;
        }

        @Override
        protected void onProgressUpdate(AlarmClockItem... item) {}

        @Override
        protected void onPostExecute(AlarmClockAdapter result)
        {
            if (result != null)
            {
                ListView alarmList = alarmListRef.get();
                if (alarmList != null)
                {
                    alarmList.setAdapter(result);
                    View emptyView = emptyViewRef.get();
                    if (emptyView != null) {
                        alarmList.setEmptyView(emptyView);
                    }
                }

                if (taskListener != null) {
                    taskListener.onFinished(result);
                }
            }
        }

        protected AlarmClockListTaskListener taskListener;
        public void setTaskListener( AlarmClockListTaskListener l )
        {
            taskListener = l;
        }

        public static abstract class AlarmClockListTaskListener
        {
            public void onFinished(AlarmClockAdapter result) {}
        }
    }

    /**
     * updateAlarmTime
     * @param item AlarmClockItem
     */
    protected static void updateAlarmTime(Context context, final AlarmClockItem item)
    {
        Calendar now = Calendar.getInstance();
        Calendar eventTime;
        if (item.location != null && item.event != null)
        {
            // Event Mode; set timestamp based on SolarEvent
            switch (item.event.getType())
            {
                case SolarEvents.TYPE_MOON:
                    SuntimesMoonData moonData = new SuntimesMoonData(context, 0);
                    moonData.setLocation(item.location);
                    moonData.calculate();
                    eventTime = (item.event.isRising() ? moonData.moonriseCalendarToday() : moonData.moonsetCalendarToday());
                    if (now.after(eventTime)) {
                        eventTime = (item.event.isRising() ? moonData.moonriseCalendarTomorrow() : moonData.moonsetCalendarTomorrow());
                    }
                    break;

                case SolarEvents.TYPE_SUN:
                default:
                    SuntimesRiseSetData sunData = new SuntimesRiseSetData(context, 0);
                    sunData.setLocation(item.location);
                    WidgetSettings.TimeMode timeMode = item.event.toTimeMode();
                    sunData.setTimeMode(timeMode != null ? timeMode : WidgetSettings.TimeMode.OFFICIAL);
                    sunData.calculate();
                    eventTime = (item.event.isRising() ? sunData.sunriseCalendarToday() : sunData.sunsetCalendarToday());
                    if (now.after(eventTime)) {
                        eventTime = (item.event.isRising() ? sunData.sunriseCalendarOther() : sunData.sunsetCalendarOther());
                    }
                    break;
            }
            item.hour = eventTime.get(Calendar.HOUR_OF_DAY);
            item.minute = eventTime.get(Calendar.MINUTE);

        } else {
            // Clock Mode; set timestamp from hour and minute
            eventTime = Calendar.getInstance();
            if (item.hour >= 0 && item.hour < 24) {
                eventTime.set(Calendar.HOUR_OF_DAY, item.hour);
            }
            if (item.minute >= 0 && item.minute < 60) {
                eventTime.set(Calendar.MINUTE, item.minute);
            }
            while (now.after(eventTime)) {
                eventTime.add(Calendar.DAY_OF_YEAR, 1);
            }
        }

        item.timestamp = eventTime.getTimeInMillis();
        item.modified = true;
    }

    /**
     * AlarmClockAdapter
     */
    @SuppressWarnings("Convert2Diamond")
    public static class AlarmClockAdapter extends ArrayAdapter<AlarmClockItem>
    {
        private Context context;
        private ArrayList<AlarmClockItem> items;
        private int iconAlarm, iconNotification, iconSoundEnabled, iconSoundDisabled;
        private int alarmEnabledColor, alarmDisabledColor;

        public AlarmClockAdapter(Context context)
        {
            super(context, R.layout.layout_listitem_alarmclock);
            initAdapter(context);
            this.items = new ArrayList<>();
        }

        public AlarmClockAdapter(Context context, ArrayList<AlarmClockItem> items)
        {
            super(context, R.layout.layout_listitem_alarmclock, items);
            initAdapter(context);
            this.items = items;
        }

        @SuppressLint("ResourceType")
        private void initAdapter(Context context)
        {
            this.context = context;

            int[] attrs = { R.attr.alarmCardEnabled, R.attr.alarmCardDisabled, R.attr.icActionAlarm, R.attr.icActionNotification, R.attr.icActionSoundEnabled, R.attr.icActionSoundDisabled};
            TypedArray a = context.obtainStyledAttributes(attrs);
            alarmEnabledColor = ContextCompat.getColor(context, a.getResourceId(0, R.color.alarm_enabled_dark));
            alarmDisabledColor = ContextCompat.getColor(context, a.getResourceId(1, R.color.alarm_disabled_dark));
            iconAlarm = a.getResourceId(2, R.drawable.ic_action_alarms);
            iconNotification = a.getResourceId(3, R.drawable.ic_action_notification);
            iconSoundEnabled = a.getResourceId(4, R.drawable.ic_action_soundenabled);
            iconSoundDisabled = a.getResourceId(5, R.drawable.ic_action_sounddisabled);
            a.recycle();
        }

        @Override
        public void add(AlarmClockItem item)
        {
            this.items.add(item);
            super.add(item);
        }

        @Override
        public void addAll (AlarmClockItem... items)
        {
            this.items.addAll(0, Arrays.asList(items));
            super.addAll(items);
        }

        /**
         * Retrieve an AlarmClockItem from the adapter using its rowID.
         * @param rowID the item's rowID
         * @return an AlarmClockItem or null if not found
         */
        public AlarmClockItem findItem( Long rowID )
        {
            if (rowID != null) {
                for (AlarmClockItem item : items) {
                    if (item != null && item.rowID == rowID) {
                        return item;
                    }
                }
            }
            return null;
        }

        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent)
        {
            return itemView(position, convertView, parent);
        }

        @Override
        public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent)
        {
            return itemView(position, convertView, parent);
        }

        private View itemView(int position, View convertView, @NonNull final ViewGroup parent)
        {
            LayoutInflater inflater = LayoutInflater.from(context);
            final View view = inflater.inflate(R.layout.layout_listitem_alarmclock, parent, false);  // always re-inflate (ignore convertView)
            final AlarmClockItem item = ((position >= 0 && position < items.size()) ? items.get(position) : null);
            if (item == null)
            {
                Log.d("DEBUG", "position " + position + " is null!");
                view.setVisibility(View.GONE);
                return view;
            }

            //ImageView icon = (ImageView) view.findViewById(android.R.id.icon1);
            //icon.setImageResource(item.icon);

            final View card = view.findViewById(R.id.layout_alarmcard);
            if (card != null)
            {
                card.setBackgroundColor(item.enabled ? alarmEnabledColor : alarmDisabledColor);
            }

            final ImageButton typeButton = (ImageButton) view.findViewById(R.id.type_menu);
            typeButton.setImageDrawable(ContextCompat.getDrawable(context, (item.type == AlarmClockItem.AlarmType.ALARM ? iconAlarm : iconNotification)));
            typeButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    showAlarmTypeMenu(item, typeButton, view);
                }
            });

            final TextView text = (TextView) view.findViewById(android.R.id.text1);
            if (text != null)
            {
                String emptyLabel = ((item.type == AlarmClockItem.AlarmType.ALARM) ? context.getString(R.string.alarmMode_alarm) : context.getString(R.string.alarmMode_notification));
                text.setText((item.label == null || item.label.isEmpty()) ? emptyLabel : item.label);
                text.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        if (adapterListener != null) {
                            adapterListener.onRequestLabel(item);
                        }
                    }
                });
            }

            final TextView text2 = (TextView) view.findViewById(android.R.id.text2);
            if (text2 != null)
            {
                final String clockTime = context.getString(R.string.alarmOption_solarevent_none);
                text2.setText(item.event != null ? item.event.getLongDisplayString() : clockTime);
                text2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v)
                    {
                        if (adapterListener != null)
                        {
                            adapterListener.onRequestSolarEvent(item);
                        }
                    }
                });
            }

            TextView text_datetime = (TextView) view.findViewById(R.id.text_datetime);
            if (text_datetime != null)
            {
                Calendar alarmTime = Calendar.getInstance();
                alarmTime.setTimeInMillis(item.timestamp);

                SuntimesUtils.TimeDisplayText timeText = utils.calendarTimeShortDisplayString(context, alarmTime, false);
                if (SuntimesUtils.is24())
                {
                    text_datetime.setText(timeText.getValue());

                } else {
                    String timeString = timeText.getValue() + " " + timeText.getSuffix();
                    SpannableString timeDisplay = SuntimesUtils.createRelativeSpan(null, timeString, " " + timeText.getSuffix(), 0.40f);
                    text_datetime.setText(timeDisplay);
                }

                text_datetime.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        if (adapterListener != null) {
                            adapterListener.onRequestTime(item);
                        }
                    }
                });
            }

            final TextView text_location = (TextView) view.findViewById(R.id.text_location_label);
            if (text_location != null)
            {
                AlarmDialog.updateLocationLabel(context, text_location, item.location);
                text_location.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        if (adapterListener != null) {
                            adapterListener.onRequestLocation(item);
                        }
                    }
                });
            }

            Switch switch_enabled = (Switch) view.findViewById(R.id.switch_enabled);
            if (switch_enabled != null)
            {
                switch_enabled.setChecked(item.enabled);
                switch_enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
                {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                    {
                        enableAlarm(item, card, isChecked);
                    }
                });
            }

            final TextView text_ringtone = (TextView) view.findViewById(R.id.text_ringtone);
            if (text_ringtone != null)
            {
                int iconID = item.ringtoneName != null ? iconSoundEnabled : iconSoundDisabled;
                ImageSpan icon = SuntimesUtils.createImageSpan(context, iconID, 28, 28, 0);

                final String none = context.getString(R.string.alarmOption_ringtone_none);
                String ringtoneName = (item.ringtoneName != null ? item.ringtoneName : none);

                String ringtoneLabel = context.getString(R.string.alarmOption_ringtone_label, ringtoneName);
                SpannableStringBuilder ringtoneDisplay = SuntimesUtils.createSpan(context, ringtoneLabel, "[icon]", icon);

                text_ringtone.setText(ringtoneDisplay);
                text_ringtone.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        if (adapterListener != null) {
                            adapterListener.onRequestRingtone(item);
                        }
                    }
                });
            }

            CheckBox check_vibrate = (CheckBox) view.findViewById(R.id.check_vibrate);
            if (check_vibrate != null)
            {
                check_vibrate.setChecked(item.vibrate);
                check_vibrate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
                {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                    {
                        item.vibrate = isChecked;
                        item.modified = true;
                        onAlarmModified(item);
                    }
                });
            }

            CheckBox check_repeat = (CheckBox) view.findViewById(R.id.check_repeat);
            if (check_repeat != null)
            {
                check_repeat.setChecked(item.repeating);
                check_repeat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
                {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                    {
                        item.repeating = isChecked;
                        item.modified = true;
                        onAlarmModified(item);
                    }
                });
            }

            ImageButton overflow = (ImageButton) view.findViewById(R.id.overflow_menu);
            if (overflow != null)
            {
                overflow.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        showOverflowMenu(item, v, view);
                    }
                });
            }

            return view;
        }

        /**
         * @param item associated AlarmClockItem
         * @param buttonView button that triggered menu
         * @param itemView view associated with item
         */
        protected void showOverflowMenu(final AlarmClockItem item, final View buttonView, final View itemView)
        {
            PopupMenu menu = new PopupMenu(context, buttonView);
            MenuInflater inflater = menu.getMenuInflater();
            inflater.inflate(R.menu.alarmcontext, menu.getMenu());

            menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
            {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem)
                {
                    switch (menuItem.getItemId())
                    {
                        case R.id.deleteAlarm:
                            deleteAlarm(item, itemView);
                            return true;

                        default:
                            return false;
                    }
                }
            });

            SuntimesUtils.forceActionBarIcons(menu.getMenu());
            menu.show();
        }

        /**
         * showAlarmTypeMenu
         * @param item AlarmClockItem
         * @param buttonView button that triggered menu
         * @param itemView view associated with item
         */
        protected void showAlarmTypeMenu(final AlarmClockItem item, final View buttonView, final View itemView)
        {
            PopupMenu menu = new PopupMenu(context, buttonView);
            MenuInflater inflater = menu.getMenuInflater();
            inflater.inflate(R.menu.alarmtype, menu.getMenu());

            menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
            {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem)
                {
                    switch (menuItem.getItemId())
                    {
                        case R.id.alarmTypeNotification:
                            item.type = AlarmClockItem.AlarmType.NOTIFICATION;
                            break;

                        case R.id.alarmTypeAlarm:
                        default:
                            item.type = AlarmClockItem.AlarmType.ALARM;
                            break;
                    }
                    onAlarmModified(item);
                    notifyDataSetChanged();
                    return true;
                }
            });

            SuntimesUtils.forceActionBarIcons(menu.getMenu());
            menu.show();
        }

        /**
         * onAlarmModified
         * @param item AlarmClockItem
         * @return true modifications were saved
         */
        protected void onAlarmModified(final AlarmClockItem item)
        {
            if (item.modified)
            {
                AlarmClockUpdateTask task = new AlarmClockUpdateTask(context);
                task.execute(item);
            }
        }

        /**
         * enableAlarm
         * @param item AlarmClockItem
         * @param enabled enabled/disabled
         */
        protected void enableAlarm(final AlarmClockItem item, View itemView, boolean enabled)
        {
            item.enabled = enabled;
            item.modified = true;
            onAlarmModified(item);

            itemView.setBackgroundColor(enabled ? alarmEnabledColor : alarmDisabledColor);
            if (enabled)
            {
                Toast msg = Toast.makeText(context, "alarm " + item.rowID + " enabled", Toast.LENGTH_SHORT);  // TODO
                msg.show();
            }
        }

        /**
         * deleteAlarm
         * @param item AlarmClockItem
         */
        protected void deleteAlarm(final AlarmClockItem item, final View itemView)
        {
            AlertDialog.Builder confirm = new AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.deletealarm_dialog_title))
                    .setMessage(context.getString(R.string.deletealarm_dialog_message, item.rowID + ""))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(context.getString(R.string.deletealarm_dialog_ok), new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int whichButton)
                        {
                            AlarmClockDeleteTask deleteTask = new AlarmClockDeleteTask(context);
                            deleteTask.setTaskListener(new AlarmClockDeleteTask.AlarmClockDeleteTaskListener()
                            {
                                @Override
                                public void onFinished(Boolean result)
                                {
                                    if (result)
                                    {
                                        final Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_out_right);
                                        animation.setAnimationListener(new Animation.AnimationListener()
                                        {
                                            @Override
                                            public void onAnimationStart(Animation animation) {}
                                            @Override
                                            public void onAnimationRepeat(Animation animation) {}
                                            @Override
                                            public void onAnimationEnd(Animation animation)
                                            {
                                                items.remove(item);
                                                notifyDataSetChanged();
                                                Toast.makeText(context, context.getString(R.string.deletealarm_toast_success, item.rowID + ""), Toast.LENGTH_LONG).show();
                                            }
                                        });
                                        itemView.startAnimation(animation);
                                    }
                                }
                            });
                            deleteTask.execute(item.rowID);
                        }
                    })
                    .setNegativeButton(context.getString(R.string.deletealarm_dialog_cancel), null);
            confirm.show();
        }

        protected AlarmClockAdapterListener adapterListener;
        public void setAdapterListener(AlarmClockAdapterListener l)
        {
            adapterListener = l;
        }

        public static abstract class AlarmClockAdapterListener
        {
            public void onRequestLabel(AlarmClockItem forItem) {}
            public void onRequestRingtone(AlarmClockItem forItem) {}
            public void onRequestSolarEvent(AlarmClockItem forItem) {}
            public void onRequestLocation(AlarmClockItem forItem) {}
            public void onRequestTime(AlarmClockItem forItem) {}
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.alarmclock, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_clear:
                clearAlarms();
                return true;

            case R.id.action_help:
                showHelp();
                return true;

            case R.id.action_about:
                showAbout();
                return true;

            case android.R.id.home:
                boolean showBack = getIntent().getBooleanExtra(EXTRA_SHOWBACK, false);
                if (showBack) {
                    onBackPressed();
                } else {
                    onHomePressed();
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * onHomePressed
     */
    protected void onHomePressed()
    {
        Intent intent = new Intent(this, SuntimesActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @SuppressWarnings("RestrictedApi")
    @Override
    protected boolean onPrepareOptionsPanel(View view, Menu menu)
    {
        SuntimesUtils.forceActionBarIcons(menu);
        return super.onPrepareOptionsPanel(view, menu);
    }

}
