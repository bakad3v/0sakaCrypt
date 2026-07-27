package com.igeltech.nevercrypt.android.helpers;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.os.Build;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.fs.Path;

import java.io.IOException;
import java.io.InputStream;

public class CompatHelperBase
{
    private static String serviceRunningNotificationsChannelId;
    private static String fileOperationsNotificationsChannelId;

    public static void setWindowFlagSecure(Activity act)
    {
        setWindowFlagSecure(act, true);
    }

    public static void setWindowFlagSecure(Activity act, boolean enabled)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            act.setRecentsScreenshotEnabled(!enabled);
        if (enabled)
            act.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        else
            act.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }

    public static void restartActivity(AppCompatActivity activity)
    {
        activity.recreate();
    }

    public static void storeTextInClipboard(Context context, String text)
    {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Text", text);
        clipboard.setPrimaryClip(clip);
    }

    public static Bitmap loadBitmapRegion(Path path, int sampleSize, Rect regionRect) throws IOException
    {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;
        try (InputStream data = path.getFile().getInputStream())
        {
            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(data, true);
            try
            {
                return decoder.decodeRegion(regionRect, options);
            }
            finally
            {
                decoder.recycle();
            }
        }
    }

    public static synchronized String getServiceRunningNotificationsChannelId(Context context)
    {
        if (serviceRunningNotificationsChannelId == null)
        {
            serviceRunningNotificationsChannelId = "com.igeltech.nevercrypt.SERVICE_RUNNING_CHANNEL2";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                NotificationChannel channel = new NotificationChannel(serviceRunningNotificationsChannelId, context.getString(R.string.service_notifications_channel_name), NotificationManager.IMPORTANCE_LOW);
                channel.enableLights(false);
                channel.enableVibration(false);
                NotificationManagerCompat.from(context).createNotificationChannel(channel);
            }
        }
        return serviceRunningNotificationsChannelId;
    }

    public static synchronized String getFileOperationsNotificationsChannelId(Context context)
    {
        if (fileOperationsNotificationsChannelId == null)
        {
            fileOperationsNotificationsChannelId = "com.igeltech.nevercrypt.FILE_OPERATIONS_CHANNEL2";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                NotificationChannel channel = new NotificationChannel(fileOperationsNotificationsChannelId, context.getString(R.string.file_operations_notifications_channel_name), NotificationManager.IMPORTANCE_LOW);
                channel.enableLights(false);
                channel.enableVibration(false);
                NotificationManagerCompat.from(context).createNotificationChannel(channel);
            }
        }
        return fileOperationsNotificationsChannelId;
    }
}
