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

package com.forrestguice.suntimeswidget.cards;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.forrestguice.suntimeswidget.R;
import com.forrestguice.suntimeswidget.SuntimesUtils;
import com.forrestguice.suntimeswidget.calculator.SuntimesMoonData;
import com.forrestguice.suntimeswidget.calculator.SuntimesRiseSetDataset;
import com.forrestguice.suntimeswidget.calculator.core.SuntimesCalculator;
import com.forrestguice.suntimeswidget.settings.AppSettings;
import com.forrestguice.suntimeswidget.settings.SolarEvents;
import com.forrestguice.suntimeswidget.settings.WidgetSettings;
import com.forrestguice.suntimeswidget.themes.SuntimesTheme;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;

public class CardAdapter extends RecyclerView.Adapter<CardViewHolder>
{
    private static SuntimesUtils utils = new SuntimesUtils();

    private WeakReference<Context> contextRef;
    private CardAdapterOptions options = new CardAdapterOptions();

    public CardAdapter(Context context)
    {
        contextRef = new WeakReference<>(context);
        initTheme(context);
        SuntimesUtils.initDisplayStrings(context);
        CardViewHolder.utils = utils;
    }

    private void initTheme(Context context)
    {
        int[] attrs = new int[] { android.R.attr.textColorPrimary, R.attr.buttonPressColor, R.attr.text_disabledColor };
        TypedArray a = context.obtainStyledAttributes(attrs);
        options.color_textTimeDelta = ContextCompat.getColor(context, a.getResourceId(0, Color.WHITE));
        options.color_enabled = options.color_textTimeDelta;
        options.color_pressed = ContextCompat.getColor(context, a.getResourceId(1, R.color.btn_tint_pressed_dark));
        options.color_disabled = ContextCompat.getColor(context, a.getResourceId(2, R.color.text_disabled_dark));
        a.recycle();
    }

    public static final int MAX_POSITIONS = 2000;
    public static final int TODAY_POSITION = (MAX_POSITIONS / 2);      // middle position is today
    private HashMap<Integer, Pair<SuntimesRiseSetDataset, SuntimesMoonData>> data = new HashMap<>();

    @Override
    public int getItemCount() {
        return MAX_POSITIONS;
    }

    public void initData(Context context, SuntimesRiseSetDataset sunSeed, SuntimesMoonData moonSeed)
    {
        data.clear();
        options.init(context, sunSeed, moonSeed);
        initData(context, TODAY_POSITION - 1);
        initData(context, TODAY_POSITION);
        initData(context, TODAY_POSITION + 1);
        initData(context, TODAY_POSITION + 2);
        notifyDataSetChanged();
    }

    protected Pair<SuntimesRiseSetDataset, SuntimesMoonData> initData(Context context, int position)
    {
        Pair<SuntimesRiseSetDataset, SuntimesMoonData> dataPair = data.get(position);
        if (dataPair == null) {
            data.put(position, dataPair = createData(context, position));   // data is removed in onViewRecycled
            Log.d("DEBUG", "add data " + position);
        }
        return dataPair;
    }

    protected Pair<SuntimesRiseSetDataset, SuntimesMoonData> createData(Context context, int position)
    {
        Calendar date = Calendar.getInstance(options.timezone);
        if (options.dateMode != WidgetSettings.DateMode.CURRENT_DATE) {
            date.set(options.dateInfo.getYear(), options.dateInfo.getMonth(), options.dateInfo.getDay());
        }
        date.add(Calendar.DATE, position - TODAY_POSITION);

        SuntimesRiseSetDataset sun = new SuntimesRiseSetDataset(context);
        sun.setTodayIs(date);
        sun.calculateData();

        SuntimesMoonData moon = new SuntimesMoonData(context, 0, "moon");
        moon.setTodayIs(date);
        moon.calculate();

        return new Pair<>(sun, moon);
    }


    /**
     * onViewRecycled
     * @param holder
     */
    @Override
    public void onViewRecycled(CardViewHolder holder)
    {
        detachClickListeners(holder);
        if (holder.position >= 0 && (holder.position < TODAY_POSITION - 1 || holder.position > TODAY_POSITION + 2))
        {
            data.remove(holder.position);
            Log.d("DEBUG", "remove data " + holder.position);
        } else
        holder.position = RecyclerView.NO_POSITION;
    }

    /**
     * onCreateViewHolder
     * @param parent
     * @param viewType
     * @return
     */
    @Override
    public CardViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        LayoutInflater layout = LayoutInflater.from(parent.getContext());
        View view = layout.inflate(R.layout.info_time_card1, parent, false);
        return new CardViewHolder(view);
    }

    /**
     * onBindViewHolder
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(CardViewHolder holder, int position)
    {
        Context context = (contextRef != null ? contextRef.get() : null);
        if (context == null) {
            Log.w("CardAdapter", "onBindViewHolder: null context!");
            return;
        }
        if (holder == null) {
            Log.w("CardAdapter", "onBindViewHolder: null view holder!");
            return;
        }
        holder.bindDataToPosition(context, position, initData(context, position), options);
        attachClickListeners(holder, position);
    }

    /**
     * Highlight next occurring event (and removes any previous highlight).
     * @param event SolarEvents enum
     * @return the event's card position if event was found and highlighted, -1 otherwise
     */
    public int highlightField(Context context, SolarEvents event)
    {
        options.highlightEvent = null;
        options.highlightPosition = -1;

        Calendar[] eventCalendars;
        int position = TODAY_POSITION;
        do {
            Pair<SuntimesRiseSetDataset, SuntimesMoonData> dataPair = initData(context, position);
            SuntimesRiseSetDataset sun = dataPair.first;
            SuntimesMoonData moon = dataPair.second;
            Calendar now = sun.now();

            boolean found;
            switch (event) {
                case MOONRISE: case MOONSET:
                    eventCalendars = moon.getRiseSetEvents(event);  // { yesterday, today, tomorrow }
                    found = now.before(eventCalendars[1]) && now.after(eventCalendars[0]);
                    break;
                default:
                    eventCalendars = sun.getRiseSetEvents(event);  // { today, tomorrow }
                    found = now.before(eventCalendars[0]);
                    break;
            }

            if (found) {
                options.highlightEvent = event;
                options.highlightPosition = position;
                break;
            }
            position++;
        } while (position < TODAY_POSITION + 2);

        notifyDataSetChanged();
        return options.highlightPosition;
    }

    /**
     * setThemeOverride
     * @param theme SuntimesTheme
     */
    public void setThemeOverride(@NonNull SuntimesTheme theme) {
        options.themeOverride = theme;
    }

    /**
     * setCardAdapterListener
     * @param listener
     */
    public void setCardAdapterListener( @NonNull CardAdapterListener listener ) {
        adapterListener = listener;
    }
    private CardAdapterListener adapterListener = new CardAdapterListener();

    private void attachClickListeners(@NonNull CardViewHolder holder, int position)
    {
        holder.txt_date.setOnClickListener(onDateClick(position));
        holder.txt_date.setOnLongClickListener(onDateLongClick(position));
        holder.sunriseHeader.setOnClickListener(onSunriseHeaderClick(position));
        holder.sunriseHeader.setOnLongClickListener(onSunriseHeaderLongClick(position));
        holder.sunsetHeader.setOnClickListener(onSunsetHeaderClick(position));
        holder.sunsetHeader.setOnLongClickListener(onSunsetHeaderLongClick(position));
        holder.moonClickArea.setOnClickListener(onMoonHeaderClick(position));
        holder.moonClickArea.setOnLongClickListener(onMoonHeaderLongClick(position));
        holder.btn_flipperNext.setOnClickListener(onNextClick(position));
        holder.btn_flipperPrev.setOnClickListener(onPrevClick(position));
    }

    private void detachClickListeners(@NonNull CardViewHolder holder)
    {
        holder.txt_date.setOnClickListener(null);
        holder.txt_date.setOnLongClickListener(null);
        holder.sunriseHeader.setOnClickListener(null);
        holder.sunriseHeader.setOnLongClickListener(null);
        holder.sunsetHeader.setOnClickListener(null);
        holder.sunsetHeader.setOnLongClickListener(null);
        holder.moonClickArea.setOnClickListener(null);
        holder.moonClickArea.setOnLongClickListener(null);
        holder.btn_flipperNext.setOnClickListener(null);
        holder.btn_flipperPrev.setOnClickListener(null);
    }

    private View.OnClickListener onDateClick(final int position) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapterListener.onDateClick(CardAdapter.this, position);
            }
        };
    }
    private View.OnLongClickListener onDateLongClick(final int position) {
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return adapterListener.onDateLongClick(CardAdapter.this, position);
            }
        };
    }
    private View.OnClickListener onSunriseHeaderClick(final int position) {
        return  new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapterListener.onSunriseHeaderClick(CardAdapter.this, position);
            }
        };
    }
    private View.OnLongClickListener onSunriseHeaderLongClick(final int position)
    {
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return adapterListener.onSunriseHeaderLongClick(CardAdapter.this, position);
            }
        };
    }
    private View.OnClickListener onSunsetHeaderClick(final int position) {
        return  new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapterListener.onSunsetHeaderClick(CardAdapter.this, position);
            }
        };
    }
    private View.OnLongClickListener onSunsetHeaderLongClick(final int position)
    {
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return adapterListener.onSunsetHeaderLongClick(CardAdapter.this, position);
            }
        };
    }
    private View.OnClickListener onMoonHeaderClick(final int position) {
        return  new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapterListener.onMoonHeaderClick(CardAdapter.this, position);
            }
        };
    }
    private View.OnLongClickListener onMoonHeaderLongClick(final int position)
    {
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return adapterListener.onMoonHeaderLongClick(CardAdapter.this, position);
            }
        };
    }
    private View.OnClickListener onNextClick(final int position) {
        return  new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapterListener.onNextClick(CardAdapter.this, position);
            }
        };
    }
    private View.OnClickListener onPrevClick(final int position) {
        return  new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapterListener.onPrevClick(CardAdapter.this, position);
            }
        };
    }

    /**
     * CardAdapterListener
     */
    public static class CardAdapterListener
    {
        public void onDateClick(CardAdapter adapter, int position) {}
        public boolean onDateLongClick(CardAdapter adapter, int position)
        {
            return false;
        }

        public void onSunriseHeaderClick(CardAdapter adapter, int position) {}
        public boolean onSunriseHeaderLongClick(CardAdapter adapter, int position)
        {
            return false;
        }

        public void onSunsetHeaderClick(CardAdapter adapter, int position) {}
        public boolean onSunsetHeaderLongClick(CardAdapter adapter, int position)
        {
            return false;
        }

        public void onMoonHeaderClick(CardAdapter adapter, int position) {}
        public boolean onMoonHeaderLongClick(CardAdapter adapter, int position)
        {
            return false;
        }

        public void onNextClick(CardAdapter adapter, int position) {}
        public void onPrevClick(CardAdapter adapter, int position) {}
    }

    /**
     * CardViewDecorator
     */
    public static class CardViewDecorator extends RecyclerView.ItemDecoration
    {
        private int marginPx;

        public CardViewDecorator( Context context ) {
            marginPx = (int)context.getResources().getDimension(R.dimen.activity_margin);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state)
        {
            outRect.left = outRect.right = marginPx;
            outRect.top = outRect.bottom = 0;
        }
    }

    /**
     * CardViewScroller
     */
    public static class CardViewScroller extends LinearSmoothScroller
    {
        private static final float MILLISECONDS_PER_INCH = 125f;

        public CardViewScroller(Context context) {
            super(context);
        }

        @Override
        protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
            return MILLISECONDS_PER_INCH / displayMetrics.densityDpi;
        }
    }

    /**
     * CardAdapterOptions
     */
    public static class CardAdapterOptions
    {
        private WidgetSettings.DateInfo dateInfo = null;
        public WidgetSettings.DateMode dateMode = WidgetSettings.DateMode.CURRENT_DATE;
        public TimeZone timezone = null;

        public boolean supportsGoldBlue = false;
        public boolean showSeconds = false;
        public boolean showWarnings = false;

        public boolean[] showFields = null;
        public boolean showActual = true;
        public boolean showCivil = true;
        public boolean showNautical = true;
        public boolean showAstro = true;
        public boolean showNoon = true;
        public boolean showGold = false;
        public boolean showBlue = false;

        public SuntimesTheme themeOverride = null;
        public int color_textTimeDelta, color_enabled, color_disabled, color_pressed;

        public int highlightPosition = -1;
        public SolarEvents highlightEvent = null;

        public void init(Context context, SuntimesRiseSetDataset sunSeed, SuntimesMoonData moonSeed)
        {
            dateMode = WidgetSettings.loadDateModePref(context, 0);
            dateInfo = WidgetSettings.loadDatePref(context, 0);
            timezone = sunSeed.timezone();

            supportsGoldBlue = sunSeed.calculatorMode().hasRequestedFeature(SuntimesCalculator.FEATURE_GOLDBLUE);
            showSeconds = WidgetSettings.loadShowSecondsPref(context, 0);
            showWarnings = AppSettings.loadShowWarningsPref(context);

            showFields = AppSettings.loadShowFieldsPref(context);
            showActual = showFields[AppSettings.FIELD_ACTUAL];
            showCivil = showFields[AppSettings.FIELD_CIVIL];
            showNautical = showFields[AppSettings.FIELD_NAUTICAL];
            showAstro = showFields[AppSettings.FIELD_ASTRO];
            showNoon = showFields[AppSettings.FIELD_NOON];
            showGold = showFields[AppSettings.FIELD_GOLD] && supportsGoldBlue;
            showBlue = showFields[AppSettings.FIELD_BLUE] && supportsGoldBlue;
        }
    }
}