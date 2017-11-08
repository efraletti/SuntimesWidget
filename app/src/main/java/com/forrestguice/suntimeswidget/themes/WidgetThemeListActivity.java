/**
    Copyright (C) 2017 Forrest Guice
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

package com.forrestguice.suntimeswidget.themes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import android.widget.AdapterView;

import android.widget.GridView;

import android.widget.Toast;

import com.forrestguice.suntimeswidget.R;
import com.forrestguice.suntimeswidget.settings.AppSettings;
import com.forrestguice.suntimeswidget.settings.WidgetSettings;
import com.forrestguice.suntimeswidget.settings.WidgetThemes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import static com.forrestguice.suntimeswidget.themes.WidgetThemeConfigActivity.ADD_THEME_REQUEST;
import static com.forrestguice.suntimeswidget.themes.WidgetThemeConfigActivity.EDIT_THEME_REQUEST;

public class WidgetThemeListActivity extends AppCompatActivity
{
    public static final int PICK_THEME_REQUEST = 1;
    public static final String ADAPTER_MODIFIED = "isModified";

    private boolean adapterModified = false;
    private GridView gridView;
    private ActionBar actionBar;

    protected ActionMode actionMode = null;
    private WidgetThemeActionCompat themeActions;

    public WidgetThemeListActivity()
    {
        super();
    }

    @Override
    public void onCreate(Bundle icicle)
    {
        setTheme(AppSettings.loadTheme(this));
        super.onCreate(icicle);
        initLocale();
        setResult(RESULT_CANCELED);
        setContentView(R.layout.layout_themelist);

        /**Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null)
        {
        }*/

        WidgetThemes.initThemes(this);
        initViews(this);
    }

    private void initLocale()
    {
        AppSettings.initLocale(this);
        WidgetSettings.initDefaults(this);
        WidgetSettings.initDisplayStrings(this);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    protected void initViews( Context context )
    {
        initActionBar(context);
        gridView = (GridView)findViewById(R.id.themegrid);
        initThemeAdapter(context);
        themeActions = new WidgetThemeActionCompat(context);
    }

    protected void initActionBar( Context context )
    {
        Toolbar menuBar = (Toolbar) findViewById(R.id.app_menubar);
        setSupportActionBar(menuBar);
        actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private WidgetThemes.ThemeGridAdapter adapter;

    protected void initThemeAdapter(Context context)
    {
        adapter = new WidgetThemes.ThemeGridAdapter(this, WidgetThemes.values());
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id)
            {
                if (position == 0)
                {
                    addTheme();

                } else {
                    SuntimesTheme.ThemeDescriptor theme = (SuntimesTheme.ThemeDescriptor) adapter.getItem(position);
                    triggerActionMode(v, theme);
                }
            }
        });
    }

    private boolean triggerActionMode(View view, SuntimesTheme.ThemeDescriptor themeDesc)
    {
        if (actionMode == null)
        {
            if (themeDesc != null)
            {
                themeActions.setTheme(this, themeDesc);
                actionMode = startSupportActionMode(themeActions);
                actionMode.setTitle(themeDesc.displayString());
            }
            return true;

        } else {
            actionMode.finish();
            triggerActionMode(view, themeDesc);
            return false;
        }
    }

    protected void addTheme()
    {
        if (actionMode != null)
        {
            actionMode.finish();
        }

        Intent intent = new Intent(this, WidgetThemeConfigActivity.class);
        intent.putExtra(WidgetThemeConfigActivity.PARAM_MODE, WidgetThemeConfigActivity.UIMode.ADD_THEME);
        startActivityForResult(intent, ADD_THEME_REQUEST);
    }

    protected void editTheme( SuntimesTheme theme )
    {
        if (theme.isDefault())
        {
            copyTheme(theme);

        } else {
            Intent intent = new Intent(this, WidgetThemeConfigActivity.class);
            intent.putExtra(WidgetThemeConfigActivity.PARAM_MODE, WidgetThemeConfigActivity.UIMode.EDIT_THEME);
            intent.putExtra(SuntimesTheme.THEME_NAME, theme.themeName());
            startActivityForResult(intent, EDIT_THEME_REQUEST);
        }
    }

    protected void copyTheme( SuntimesTheme theme )
    {
        Intent intent = new Intent(this, WidgetThemeConfigActivity.class);
        intent.putExtra(WidgetThemeConfigActivity.PARAM_MODE, WidgetThemeConfigActivity.UIMode.ADD_THEME);
        intent.putExtra(SuntimesTheme.THEME_NAME, theme.themeName());
        startActivityForResult(intent, ADD_THEME_REQUEST);
    }

    protected void deleteTheme(final SuntimesTheme theme)
    {
        if (!theme.isDefault())
        {
            final Context context = this;
            AlertDialog.Builder confirm = new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.deletetheme_dialog_title))
                    .setMessage(getString(R.string.deletetheme_dialog_message, theme.themeName()))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(getString(R.string.deletetheme_dialog_ok), new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int whichButton)
                        {
                            if (WidgetThemes.removeValue(context, theme.themeDescriptor()))
                            {
                                adapterModified = true;
                                initThemeAdapter(context);
                                Toast.makeText(context, context.getString(R.string.deletetheme_toast_success, theme.themeName()), Toast.LENGTH_LONG).show();
                            }
                        }
                    })
                    .setNegativeButton(getString(R.string.deletetheme_dialog_cancel), null);

            confirm.show();
        }
    }

    protected void selectTheme( SuntimesTheme theme )
    {
        Intent intent = new Intent();
        intent.putExtra(SuntimesTheme.THEME_NAME, theme.themeName());
        intent.putExtra(ADAPTER_MODIFIED, adapterModified);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    protected void importThemes()
    {
        File f = new File("test.xml");
        WidgetThemes.importThemes(this, f);
    }

    protected void exportThemes()
    {
        File f = new File("test.xml");
        WidgetThemes.exportThemes(this, f);
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent();
        intent.putExtra(ADAPTER_MODIFIED, adapterModified);
        setResult(((adapterModified) ? Activity.RESULT_OK : Activity.RESULT_CANCELED), intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.themelist, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.importThemes:
                importThemes();
                return true;

            case R.id.exportThemes:
                exportThemes();
                return true;

            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * WidgetThemeActionCompat
     */
    private class WidgetThemeActionCompat implements android.support.v7.view.ActionMode.Callback
    {
        private SuntimesTheme theme = null;

        public WidgetThemeActionCompat(Context context) {}

        public void setTheme(Context context, SuntimesTheme.ThemeDescriptor themeDesc )
        {
            this.theme = WidgetThemes.loadTheme(context, themeDesc.name());
        }

        @Override
        public boolean onCreateActionMode(android.support.v7.view.ActionMode mode, Menu menu)
        {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.themecontext, menu);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode)
        {
            actionMode = null;
        }

        @Override
        public boolean onPrepareActionMode(android.support.v7.view.ActionMode mode, Menu menu)
        {
            MenuItem deleteItem = menu.findItem(R.id.deleteTheme);
            deleteItem.setVisible( !theme.isDefault() );  // not allowed to delete default

            MenuItem editItem = menu.findItem(R.id.editTheme);
            editItem.setVisible( !theme.isDefault() );    // not allowed to edit default

            return false;
        }

        @Override
        public boolean onActionItemClicked(android.support.v7.view.ActionMode mode, MenuItem item)
        {
            mode.finish();
            if (theme != null)
            {
                switch (item.getItemId())
                {
                    case R.id.selectTheme:
                        selectTheme(theme);
                        return true;

                    case R.id.editTheme:
                        editTheme(theme);
                        return true;

                    case R.id.copyTheme:
                        copyTheme(theme);
                        return true;

                    case R.id.deleteTheme:
                        deleteTheme(theme);
                        return true;
                }
            }
            return false;
        }
    }

    /**
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode)
        {
            case ADD_THEME_REQUEST:
                onAddThemeResult(resultCode, data);
                break;

            case EDIT_THEME_REQUEST:
                onEditThemeResult(resultCode, data);
                break;
        }
    }

    protected void onAddThemeResult(int resultCode, Intent data)
    {
        if (resultCode == RESULT_OK)
        {
            adapterModified = true;
            initThemeAdapter(this);

            if (data != null)
            {
                String themeName = data.getStringExtra(SuntimesTheme.THEME_NAME);
                SuntimesTheme.ThemeDescriptor theme = (SuntimesTheme.ThemeDescriptor) adapter.getItem(adapter.ordinal(themeName));
                triggerActionMode(null, theme);
                Toast.makeText(this, getString(R.string.addtheme_toast_success, themeName), Toast.LENGTH_LONG).show();
            }
        }
    }

    protected void onEditThemeResult(int resultCode, Intent data)
    {
        if (resultCode == RESULT_OK)
        {
            adapterModified = true;
            initThemeAdapter(this);

            if (data != null)
            {
                String themeName = data.getStringExtra(SuntimesTheme.THEME_NAME);
                SuntimesTheme.ThemeDescriptor theme = (SuntimesTheme.ThemeDescriptor) adapter.getItem(adapter.ordinal(themeName));
                triggerActionMode(null, theme);
                Toast.makeText(this, getString(R.string.edittheme_toast_success, themeName), Toast.LENGTH_LONG).show();
            }
        }
    }

}
