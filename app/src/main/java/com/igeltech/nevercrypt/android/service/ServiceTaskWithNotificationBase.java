package com.igeltech.nevercrypt.android.service;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.igeltech.nevercrypt.android.Logger;
import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.helpers.CompatHelper;

public abstract class ServiceTaskWithNotificationBase implements Task
{
    protected Context _context;
    NotificationCompat.Builder _notificationBuilder;
    private boolean _isCancelled;
    private long _prevUpdateTime;
    private int _taskId;

    ServiceTaskWithNotificationBase()
    {
    }

    @Override
    public Object doWork(Context context, Intent i) throws Throwable
    {
        initTask(context, i);
        return null;
    }

    @Override
    public void onCompleted(Result result)
    {
        removeNotification();
    }

    @Override
    public void cancel()
    {
        _isCancelled = true;
    }

    public boolean isCancelled()
    {
        return _isCancelled;
    }

    protected void initTask(Context context, Intent i) throws Exception
    {
        _context = context;
        _taskId = FileOpsService.getNewNotificationId();
        _notificationBuilder = initNotification();
    }

    protected void updateUI()
    {
        updateNotification();
    }

    protected String getErrorMessage(Throwable ex)
    {
        return Logger.getExceptionMessage(_context, ex);
    }

    private String getErrorDetails(Throwable ex)
    {
        String msg = ex.getLocalizedMessage();
        if (msg == null)
        {
            msg = ex.getMessage();
            if (msg == null)
                msg = "";
        }
        return msg;
    }

    void reportError(Throwable err)
    {
        Logger.log(err);
        showNotificationMessage(getErrorMessage(err), getErrorDetails(err));
    }

    private void showNotificationMessage(String title, String message)
    {
        if (title == null)
            return;
        NotificationCompat.Builder nb = new NotificationCompat.Builder(_context, CompatHelper.getFileOperationsNotificationsChannelId(_context)).setSmallIcon(R.drawable.ic_notification_new).setOngoing(false).setAutoCancel(true).setContentTitle(title).setContentText(message);
        //Gingerbread compatibility
        final Intent emptyIntent = new Intent();
        PendingIntent pi = PendingIntent.getActivity(_context, 0, emptyIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        nb.setContentIntent(pi);
        NotificationManagerCompat nm = NotificationManagerCompat.from(_context);
        if (nm != null)
            nm.notify(FileOpsService.getNewNotificationId(), nb.build());
    }

    protected NotificationCompat.Builder initNotification()
    {
        NotificationCompat.Builder nb = new NotificationCompat.Builder(_context, CompatHelper.getFileOperationsNotificationsChannelId(_context)).setContentTitle(_context.getString(R.string.app_name)).setSmallIcon(R.drawable.ic_notification_new).setOngoing(true).setAutoCancel(false).addAction(R.drawable.ic_action_cancel, _context.getString(android.R.string.cancel), FileOpsService.getCancelTaskActionPendingIntent(_context, _taskId));
        return nb;
    }

    void removeNotification()
    {
        NotificationManagerCompat nm = NotificationManagerCompat.from(_context);
        if (nm != null)
            nm.cancel(_taskId);
    }

    private void updateNotification()
    {
        NotificationManagerCompat nm = NotificationManagerCompat.from(_context);
        if (nm != null)
            nm.notify(_taskId, _notificationBuilder.build());
    }

    void updateUIOnTime()
    {
        long time = SystemClock.uptimeMillis();
        if (time - _prevUpdateTime > 1000)
        {
            updateUI();
            _prevUpdateTime = time;
        }
    }
}
