/**
    Copyright (C) 2019 Forrest Guice
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

package com.forrestguice.suntimeswidget.actions;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;

import com.forrestguice.suntimeswidget.R;
import com.forrestguice.suntimeswidget.settings.WidgetActions;

import java.util.Set;

/**
 * SaveIntentDialog
 */
public class SaveIntentDialog extends EditIntentDialog
{
    @Override
    public String getIntentTitle()
    {
        if (edit.text_label != null) {
            return edit.text_label.getText().toString();
        } else return null;
    }
    public void setIntentTitle(String value) {
        intentTitle = value;
    }

    public String getIntentID()
    {
        if (edit_intentID != null) {
            return edit_intentID.getText().toString();
        } else return intentID;
    }
    public void setIntentID(String id) {
        intentID = id;
    }
    public String suggestedIntentID(Context context)
    {
        int c = 0;
        String suggested;
        do {
            suggested = context.getString(R.string.addaction_custname, Integer.toString(c));
            c++;
        } while (intentIDs != null && intentIDs.contains(suggested));
        return suggested;
    }

    private String intentID = null, intentTitle = "";
    private Set<String> intentIDs;
    private EditIntentView edit;
    private AutoCompleteTextView edit_intentID;
    private TextView text_note;
    private ImageButton button_suggest;

    @Override
    protected void updateViews(Context context)
    {
        edit.setIntentTitle(intentTitle);
        edit_intentID.setText(intentID);
        text_note.setVisibility(View.GONE);

        if ((intentIDs.contains(intentID)))
        {
            edit.setIntentTitle(WidgetActions.loadActionLaunchPref(context, 0, intentID, WidgetActions.PREF_KEY_ACTION_LAUNCH_TITLE));
            text_note.setVisibility(View.VISIBLE);
            edit_intentID.selectAll();
            edit_intentID.requestFocus();
        }
    }

    @Override
    protected boolean validateInput()
    {
        String id = edit_intentID.getText().toString();
        String title = edit.getIntentTitle();

        if (id.trim().isEmpty() || id.contains(" ")) {
            edit_intentID.setError(getContext().getString(R.string.addaction_error_id));
            return false;
        } else edit_intentID.setError(null);

        if (title.trim().isEmpty()) {
            edit.text_label.setError(getContext().getString(R.string.addaction_error_title));
            return false;
        } else edit.text_label.setError(null);

        return true;
    }

    @Override
    protected void initViews(Context context, View dialogContent)
    {
        intentIDs = WidgetActions.loadActionLaunchList(context, 0);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, intentIDs.toArray(new String[0]));

        if (intentID == null) {
            intentID = suggestedIntentID(context);
        }

        edit = (EditIntentView) dialogContent.findViewById(R.id.edit_intent);
        edit.text_label.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                boolean validInput = validateInput();
                if (btn_accept != null) {
                    btn_accept.setEnabled(validInput);
                }
            }
        });

        text_note = (TextView) dialogContent.findViewById(R.id.text_note);

        edit_intentID = (AutoCompleteTextView) dialogContent.findViewById(R.id.edit_intent_id);
        edit_intentID.setAdapter(adapter);
        edit_intentID.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setIntentID((String)parent.getItemAtPosition(position));
            }
        });
        edit_intentID.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                text_note.setVisibility( (intentIDs.contains(s.toString())) ? View.VISIBLE : View.GONE );

                boolean validInput = validateInput();
                if (btn_accept != null) {
                    btn_accept.setEnabled(validInput);
                }
            }
        });

        button_suggest = (ImageButton) dialogContent.findViewById(R.id.edit_intent_reset);
        button_suggest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setIntentID(suggestedIntentID(getContext()));
                updateViews(getContext());
                edit_intentID.selectAll();
                edit_intentID.requestFocus();
            }
        });

        updateViews(context);
        super.initViews(context, dialogContent);
    }

    @Override
    protected int getLayoutID() {
        return R.layout.layout_dialog_intent_save;
    }
}