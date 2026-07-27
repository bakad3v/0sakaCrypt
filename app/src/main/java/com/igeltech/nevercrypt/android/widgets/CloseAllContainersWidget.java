package com.igeltech.nevercrypt.android.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.filemanager.activities.FileManagerActivity;
import com.igeltech.nevercrypt.android.locations.activities.CloseLocationsActivity;
import com.igeltech.nevercrypt.android.locations.activities.LocationManagerActivity;
import com.igeltech.nevercrypt.locations.CryptoLocation;
import com.igeltech.nevercrypt.locations.LocationsManager;

public class CloseAllContainersWidget extends AppWidgetProvider
{
    public static void setWidgetLayout(Context context, AppWidgetManager appWidgetManager, int widgetId, boolean haveOpenContainers)
    {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.close_all_containers_widget);
        views.setImageViewResource(R.id.containersClosedImageButton, haveOpenContainers ? R.drawable.widget_unlocked_all : R.drawable.widget_locked_all);
        Intent i;
        if (haveOpenContainers)
        {
            i = new Intent(context, CloseLocationsActivity.class);
            LocationsManager.storeLocationsInIntent(i, LocationsManager.getLocationsManager(context, true).getLocationsClosingOrder());
        }
        else
        {
            i = new Intent(context, LocationManagerActivity.class);
            i.setAction(Intent.ACTION_MAIN);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, widgetId, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.containersClosedImageButton, pendingIntent);
        appWidgetManager.updateAppWidget(widgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        setWidgetsState(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent)
    {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (LocationsManager.BROADCAST_LOCATION_CHANGED.equals(action) || LocationsManager.BROADCAST_ALL_CONTAINERS_CLOSED.equals(action))
            setWidgetsState(context);
    }

    private void setWidgetsState(Context context)
    {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, CloseAllContainersWidget.class));
        setWidgetsState(context, appWidgetManager, appWidgetIds);
    }

    private void setWidgetsState(Context context, AppWidgetManager appWidgetManager, int[] widgetIds)
    {
        boolean haveOpenContainers = haveOpenContainers(context);
        for (int widgetId : widgetIds)
            setWidgetLayout(context, appWidgetManager, widgetId, haveOpenContainers);
    }

    private boolean haveOpenContainers(Context context)
    {
        LocationsManager lm = LocationsManager.getLocationsManager(context);
        if (lm != null)
        {
            for (CryptoLocation cbl : lm.getLoadedCryptoLocations(false))
                if (cbl.isOpenOrMounted())
                    return true;
        }
        return false;
    }
}
