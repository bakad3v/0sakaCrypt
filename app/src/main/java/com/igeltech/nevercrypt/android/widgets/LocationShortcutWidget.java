package com.igeltech.nevercrypt.android.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.filemanager.activities.FileManagerActivity;
import com.igeltech.nevercrypt.android.locations.activities.LocationManagerActivity;
import com.igeltech.nevercrypt.android.settings.UserSettings;
import com.igeltech.nevercrypt.locations.Location;
import com.igeltech.nevercrypt.locations.LocationsManager;
import com.igeltech.nevercrypt.settings.Settings;

public class LocationShortcutWidget extends AppWidgetProvider
{
    public static void setWidgetLayout(Context context, AppWidgetManager appWidgetManager, int widgetId, Settings.LocationShortcutWidgetInfo prefs, boolean isContainerOpen)
    {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        views.setTextViewText(R.id.widgetTitleTextView, prefs.widgetTitle);
        views.setImageViewResource(R.id.widgetLockImageButton, isContainerOpen ? R.drawable.widget_unlocked : R.drawable.widget_locked);
        Intent intent = new Intent(context, LocationManagerActivity.class);
        intent.setData(Uri.parse(prefs.locationUriString));
        PendingIntent pendingIntent = PendingIntent.getActivity(context, widgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetLockImageButton, pendingIntent);
        appWidgetManager.updateAppWidget(widgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        setWidgetsState(context, appWidgetManager, appWidgetIds, null);
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent)
    {
        super.onReceive(context, intent);
        if (LocationsManager.BROADCAST_LOCATION_CHANGED.equals(intent.getAction()))
            setWidgetsState(context, intent.getParcelableExtra(LocationsManager.PARAM_LOCATION_URI));
    }

    private void setWidgetsState(Context context, Uri locationUri)
    {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, LocationShortcutWidget.class));
        setWidgetsState(context, appWidgetManager, appWidgetIds, locationUri);
    }

    private void setWidgetsState(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, Uri locationUri)
    {
        LocationsManager lm = LocationsManager.getLocationsManager(context);
        if (lm == null)
            setWidgetsState(context, appWidgetManager, appWidgetIds, false);
        else
            try
            {
                UserSettings settings = UserSettings.getSettings(context);
                for (int widgetId : appWidgetIds)
                {
                    Settings.LocationShortcutWidgetInfo widgetInfo = settings.getLocationShortcutWidgetInfo(widgetId);
                    if (widgetInfo != null)
                    {
                        Location widgetLoc = lm.findExistingLocation(Uri.parse(widgetInfo.locationUriString));
                        if (widgetLoc != null)
                        {
                            if (locationUri != null)
                            {
                                Location changedLoc = lm.getLocation(locationUri);
                                if (changedLoc != null && changedLoc.getId().equals(widgetLoc.getId()))
                                    setWidgetLayout(context, appWidgetManager, widgetId, widgetInfo, widgetLoc);
                            }
                            else
                                setWidgetLayout(context, appWidgetManager, widgetId, widgetInfo, widgetLoc);
                        }
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
    }

    public void setWidgetLayout(Context context, AppWidgetManager appWidgetManager, int widgetId, Settings.LocationShortcutWidgetInfo widgetInfo, Location loc)
    {
        setWidgetLayout(context, appWidgetManager, widgetId, widgetInfo, LocationsManager.isOpen(loc));
    }

    private void setWidgetsState(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, boolean isOpen)
    {
        UserSettings settings = UserSettings.getSettings(context);
        for (int widgetId : appWidgetIds)
        {
            Settings.LocationShortcutWidgetInfo widgetInfo = settings.getLocationShortcutWidgetInfo(widgetId);
            if (widgetInfo != null)
                setWidgetLayout(context, appWidgetManager, widgetId, widgetInfo, isOpen);
        }
    }
}
