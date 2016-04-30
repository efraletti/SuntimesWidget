/**
    Copyright (C) 2014 Forrest Guice
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

package com.forrestguice.suntimeswidget.settings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.forrestguice.suntimeswidget.R;

import java.util.ArrayList;
import java.util.Arrays;

public enum SolarEvents
{
    MORNING_ASTRONOMICAL("astronomical twilight", "morning astronomical twilight", R.drawable.ic_sunrise_large),
    MORNING_NAUTICAL("nautical twilight", "morning nautical twilight", R.drawable.ic_sunrise_large),
    MORNING_CIVIL("civil twilight", "morning civil twilight", R.drawable.ic_sunrise_large),
    SUNRISE("sunrise", "sunrise", R.drawable.ic_sunrise_large),
    NOON("solar noon", "solar noon", R.drawable.ic_noon_large),
    SUNSET("sunset", "sunset", R.drawable.ic_sunset_large),
    EVENING_CIVIL("civil twilight", "evening civil twilight", R.drawable.ic_sunset_large),
    EVENING_NAUTICAL("nautical twilight", "evening nautical twilight", R.drawable.ic_sunset_large),
    EVENING_ASTRONOMICAL("astronomical twilight", "evening astronomical twilight", R.drawable.ic_sunset_large);

    private int iconResource;
    private String shortDisplayString, longDisplayString;

    private SolarEvents(String shortDisplayString, String longDisplayString, int iconResource)
    {
        this.shortDisplayString = shortDisplayString;
        this.longDisplayString = longDisplayString;
        this.iconResource = iconResource;
    }

    public String toString()
    {
        return longDisplayString;
    }

    public int getIcon()
    {
        return iconResource;
    }

    public String getShortDisplayString()
    {
        return shortDisplayString;
    }

    public String getLongDisplayString()
    {
        return longDisplayString;
    }

    public void setDisplayString(String shortDisplayString, String longDisplayString)
    {
        this.shortDisplayString = shortDisplayString;
        this.longDisplayString = longDisplayString;
    }

    public static void initDisplayStrings(Context context)
    {
        String[] modes_short = context.getResources().getStringArray(R.array.solarevents_short);
        String[] modes_long = context.getResources().getStringArray(R.array.solarevents_long);

        MORNING_ASTRONOMICAL.setDisplayString(modes_short[0], modes_long[0]);
        MORNING_NAUTICAL.setDisplayString(modes_short[1], modes_long[1]);
        MORNING_CIVIL.setDisplayString(modes_short[2], modes_long[2]);
        SUNRISE.setDisplayString(modes_short[3], modes_long[3]);
        NOON.setDisplayString(modes_short[4], modes_long[4]);
        SUNSET.setDisplayString(modes_short[5], modes_long[5]);
        EVENING_CIVIL.setDisplayString(modes_short[6], modes_long[6]);
        EVENING_NAUTICAL.setDisplayString(modes_short[7], modes_long[7]);
        EVENING_ASTRONOMICAL.setDisplayString(modes_short[8], modes_long[8]);
    }

    public static SolarEventsAdapter createAdapter(Context context)
    {
        ArrayList<SolarEvents> choices = new ArrayList<>();
        choices.addAll(Arrays.asList(SolarEvents.values()));
        return new SolarEventsAdapter(context, choices);
    }

    /**
     * ArrayAdapter that displays SolarEvents items (with icon) as list or dropdown.
     */
    public static class SolarEventsAdapter extends ArrayAdapter<SolarEvents>
    {
        private final Context context;
        private final ArrayList<SolarEvents> choices;

        public SolarEventsAdapter(Context context, ArrayList<SolarEvents> choices)
        {
            super(context, R.layout.layout_listitem_solarevent, choices);
            this.context = context;
            this.choices = choices;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            return alarmItemView(position, convertView, parent);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent)
        {
            return alarmItemView(position, convertView, parent);
        }

        private View alarmItemView(int position, View convertView, ViewGroup parent)
        {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.layout_listitem_solarevent, parent, false);

            ImageView icon = (ImageView) view.findViewById(android.R.id.icon1);
            icon.setImageResource(choices.get(position).getIcon());

            TextView text = (TextView) view.findViewById(android.R.id.text1);
            text.setText(choices.get(position).getLongDisplayString());

            return view;
        }
    }
}