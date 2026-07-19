package com.igeltech.nevercrypt.android;

import android.content.Context;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.igeltech.nevercrypt.android.errors.UserException;
import com.igeltech.nevercrypt.settings.GlobalConfig;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;

public class Logger implements UncaughtExceptionHandler
{
    public static final String TAG = "NeverCrypt";
    private static final Object _syncObject = new Object();
    private static Logger l;
    private static boolean _disableLog = false;
    private static UncaughtExceptionHandler _previousDefaultHandler;
    private static UncaughtExceptionHandler _previousMainThreadHandler;

    public static void disableLog(boolean val)
    {
        _disableLog = val;
    }

    public static boolean isLogDisabled()
    {
        return _disableLog;
    }

    public static void initLogger() throws IOException
    {
        if (_disableLog)
            return;
        Logger.l = new Logger();
        Thread t = Looper.getMainLooper().getThread();
        _previousMainThreadHandler = t.getUncaughtExceptionHandler();
        _previousDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        t.setUncaughtExceptionHandler(Logger.l);
        Thread.setDefaultUncaughtExceptionHandler(Logger.l);
    }

    public static void closeLogger()
    {
        Thread t = Looper.getMainLooper().getThread();
        t.setUncaughtExceptionHandler(_previousMainThreadHandler);
        Thread.setDefaultUncaughtExceptionHandler(_previousDefaultHandler);
        Logger.l = null;
        _previousDefaultHandler = null;
        _previousMainThreadHandler = null;
    }

    public static void showAndLog(Context context, Throwable err)
    {
        synchronized (_syncObject)
        {
            if (l != null)
                Logger.l.showAndLogError(context, err);
        }
    }

    public static void log(String message)
    {
        if (_disableLog)
            return;
        Log.i(TAG, message);
    }

    public static void log(Throwable e)
    {
        if (_disableLog)
            return;
        Log.e(TAG, "Error", e);
    }

    public static void debug(String message)
    {
        if (_disableLog)
            return;
        if (GlobalConfig.isDebug())
            Log.d(TAG, message);
    }

    public static String getExceptionMessage(Context context, Throwable err)
    {
        if (err instanceof UserException)
            ((UserException) err).setContext(context);
        String msg = err.getLocalizedMessage();
        if (msg == null)
        {
            msg = err.getMessage();
            if (msg == null)
                msg = context.getString(R.string.generic_error_message);
        }
        return msg;
    }

    public static void showErrorMessage(Context context, Throwable err)
    {
        String errm;
        if (err instanceof UserException)
            errm = getExceptionMessage(context, err);
        else if (err.getCause() instanceof UserException)
            errm = getExceptionMessage(context, err.getCause());
        else
            errm = context.getString(R.string.generic_error_message);
        Toast.makeText(context, errm, Toast.LENGTH_LONG).show();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex)
    {
        if (!Logger._disableLog)
            Log.e(TAG, "Uncaught main thread exception", ex);
        UncaughtExceptionHandler handler = _previousDefaultHandler;
        if (handler != null && handler != this)
            handler.uncaughtException(thread, ex);
        else
        {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(10);
        }
    }

    private void showAndLogError(Context context, Throwable err)
    {
        try
        {
            Log.w(TAG, err);
            if (context != null && Thread.currentThread().equals(Looper.getMainLooper().getThread()))
                showErrorMessage(context, err);
        }
        catch (Throwable ignored)
        {
        }
    }
}
