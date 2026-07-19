package com.igeltech.nevercrypt.android.service;

import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;

import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.igeltech.nevercrypt.android.CryptoApplication;
import com.igeltech.nevercrypt.android.Logger;
import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.errors.InputOutputException;
import com.igeltech.nevercrypt.android.errors.UserException;
import com.igeltech.nevercrypt.android.providers.MainContentProvider;
import com.igeltech.nevercrypt.android.settings.UserSettings;
import com.igeltech.nevercrypt.fs.Path;
import com.igeltech.nevercrypt.fs.util.PathUtil;
import com.igeltech.nevercrypt.fs.util.SrcDstCollection;
import com.igeltech.nevercrypt.fs.util.StringPathUtil;
import com.igeltech.nevercrypt.locations.CryptoLocation;
import com.igeltech.nevercrypt.locations.DeviceBasedLocation;
import com.igeltech.nevercrypt.locations.Location;
import com.igeltech.nevercrypt.locations.LocationsManager;
import com.igeltech.nevercrypt.settings.GlobalConfig;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CancellationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class FileOpsServiceBase extends IntentService
{
    public static final String INTENT_PARAM_TASK_ID = "TASK_ID";
    public static final String BROADCAST_FILE_OPERATION_COMPLETED = "com.igeltech.nevercrypt.android.FILE_OPERATION_COMPLETED";
    public static final String ARG_NOTIFICATION_ID = "com.igeltech.nevercrypt.NOTIFICATION_ID";
    public static final String ARG_TASK_COMPLETED = "com.igeltech.nevercrypt.android.TASK_COMPLETED";
    public static final String ARG_ORIG_INTENT = "com.igeltech.nevercrypt.android.ORIG_INTENT";
    public static final String ACTION_COPY = "copy";
    public static final String ACTION_MOVE = "move";
    public static final String ACTION_RECEIVE = "receive";
    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_WIPE = "wipe";
    public static final String ACTION_SAVE_CHANGED_FILE = "save_changed_file";
    public static final String ACTION_CLEAR_TEMP_FOLDER = "clear_temp_folder";
    protected static final String TAG = "FileOpsService";
    protected static final String ACTION_START_TEMP_FILE = "start_temp_file";
    protected static final String ACTION_SEND_TASK = "send";
    protected static final String ACTION_CANCEL_TASK = "cancel_task";
    protected static final String ACTION_CLOSE_CONTAINER = "close_container";
    static final String ARG_RECORDS = "src_dst_records";
    static final String ARG_OVERWRITE = "overwrite";
    public static int NOTIFICATION_COUNTER = 1000;
    protected boolean _taskCancelled;
    private Task _currentTask;

    public FileOpsServiceBase()
    {
        super(TAG);
    }

    public static String getFullPathFromUri(Uri uri)
    {
        String actualResult;
        String path = uri.getPath().substring(5);
        int index = 0;
        StringBuilder result = new StringBuilder("/storage");
        for (int i = 0; i < path.length(); i++)
        {
            if (path.charAt(i) != ':')
                result.append(path.charAt(i));
            else
            {
                index = ++i;
                result.append('/');
                break;
            }
        }
        for (int i = index; i < path.length(); i++)
            result.append(path.charAt(i));
        if (result.substring(9, 16).equalsIgnoreCase("primary"))
            actualResult = result.substring(0, 8) + "/emulated/0/" + result.substring(17);
        else
            actualResult = result.toString();
        return actualResult;
    }

    public static Location getSecTempFolderLocation(String workDir, Context context) throws IOException
    {
        Location res = null;
        if (workDir != null && !workDir.isEmpty())
        {
            try
            {
                res = LocationsManager.getLocationsManager(context).getLocation(Uri.parse(workDir));
                if (!res.getCurrentPath().isDirectory())
                    res = null;
            }
            catch (Exception e)
            {
                Logger.log(e);
                res = null;
            }
        }
        if (res == null)
        {
            File extDir = context.getExternalFilesDir(null);
            if (extDir == null)
                extDir = context.getFilesDir();
            if (extDir == null)
                extDir = context.getCacheDir();
            res = new DeviceBasedLocation(UserSettings.getSettings(context), extDir.getAbsolutePath());
        }
        res.setCurrentPath(PathUtil.getDirectory(res.getCurrentPath(), "temp").getPath());
        return res;
    }

    public static Location getMirrorLocation(String workDir, Context context, String locationId) throws IOException
    {
        Location secTempLocation = getSecTempFolderLocation(workDir, context);
        secTempLocation.setCurrentPath(PathUtil.getDirectory(secTempLocation.getCurrentPath(), "mirror", locationId).getPath());
        return secTempLocation;
    }

    public static Location getMonitoredMirrorLocation(String workDir, Context context, String locationId) throws IOException
    {
        Location mirrorLocation = getMirrorLocation(workDir, context, locationId);
        mirrorLocation.setCurrentPath(PathUtil.getDirectory(mirrorLocation.getCurrentPath(), "mon").getPath());
        return mirrorLocation;
    }

    public static Location getNonMonitoredMirrorLocation(String workDir, Context context, String locationId) throws IOException
    {
        Location mirrorLocation = getMirrorLocation(workDir, context, locationId);
        mirrorLocation.setCurrentPath(PathUtil.getDirectory(mirrorLocation.getCurrentPath(), "nomon").getPath());
        return mirrorLocation;
    }

    public static String getMimeTypeFromExtension(Context context, Path path) throws IOException
    {
        return getMimeTypeFromExtension(context, path.getFile());
    }

    public static String getMimeTypeFromExtension(Context context, com.igeltech.nevercrypt.fs.File file) throws IOException
    {
        return getMimeTypeFromExtension(context, new StringPathUtil(file.getName()).getFileExtension());
    }

    public static String getMimeTypeFromExtension(Context context, String filenameExtension)
    {
        filenameExtension = filenameExtension.toLowerCase(context.getResources().getConfiguration().locale);
        UserSettings settings = UserSettings.getSettings(context);
        String mime;
        String custMimes = settings.getExtensionsMimeMapString();
        if (custMimes.length() > 0)
        {
            try
            {
                Pattern p = Pattern.compile("^\\s*" + filenameExtension + "\\s+([^\\s]+)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
                Matcher m = p.matcher(custMimes);
                if (m.find())
                    return m.group(1);
            }
            catch (Exception e)
            {
                Logger.showAndLog(context, e);
            }
        }
        mime = CryptoApplication.getMimeTypesMap(context).get(filenameExtension); //MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        return mime == null ? "application/octet-stream" : mime;
    }

    public static PendingIntent getCancelTaskActionPendingIntent(Context context, int taskId)
    {
        Intent i = new Intent(context, FileOpsService.class);
        i.setAction(ACTION_CANCEL_TASK);
        i.putExtra(INTENT_PARAM_TASK_ID, taskId);
        return PendingIntent.getService(context, taskId, i, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void startFileViewer(Context context, Location fileLocation) throws UserException
    {
        try
        {
            Uri uri = fileLocation.getDeviceAccessibleUri(fileLocation.getCurrentPath());
            if (uri == null)
                uri = MainContentProvider.getContentUriFromLocation(fileLocation);
            FileOpsService.startFileViewer(context, uri, getMimeTypeFromExtension(context, fileLocation.getCurrentPath().getFile()));
        }
        catch (IOException e)
        {
            throw new InputOutputException(context, e);
        }
    }

    public static void startFileViewer(Context context, Uri uri, String mimeType) throws UserException
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);// ,Uri.fromFile(_targetFile.devicePath));
        if (mimeType != null && !mimeType.isEmpty())
            intent.setDataAndType(uri, mimeType);
        else
            intent.setData(uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                // | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
                // | Intent.FLAG_ACTIVITY_NO_HISTORY
                // | Intent.FLAG_ACTIVITY_CLEAR_TOP
        );
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        //	intent.setClipData(ClipData.newUri(context.getContentResolver(), "", uri));
        try
        {
            context.startActivity(intent);
        }
        catch (ActivityNotFoundException e)
        {
            throw new UserException(context, R.string.err_no_application_found, e);
        }
    }

    public static synchronized int getNewNotificationId()
    {
        return NOTIFICATION_COUNTER++;
    }

    public static void copyFiles(Context context, SrcDstCollection files, boolean forceOverwrite)
    {
        Intent i = new Intent(context, FileOpsService.class);
        i.setAction(ACTION_COPY);
        i.putExtra(ARG_RECORDS, files);
        i.putExtra(ARG_OVERWRITE, forceOverwrite);
        context.startService(i);
    }

    public static void moveFiles(Context context, SrcDstCollection files, boolean forceOverwrite)
    {
        Intent i = new Intent(context, FileOpsService.class);
        i.setAction(ACTION_MOVE);
        i.putExtra(ARG_RECORDS, files);
        i.putExtra(ARG_OVERWRITE, forceOverwrite);
        context.startService(i);
    }

    public static void receiveFiles(Context context, SrcDstCollection files, boolean forceOverwrite)
    {
        Intent i = new Intent(context, FileOpsService.class);
        i.setAction(ACTION_RECEIVE);
        i.putExtra(ARG_RECORDS, files);
        i.putExtra(ARG_OVERWRITE, forceOverwrite);
        context.startService(i);
    }

    public static void deleteFiles(Context context, SrcDstCollection files)
    {
        Intent i = new Intent(context, FileOpsService.class);
        i.setAction(ACTION_DELETE);
        i.putExtra(ARG_RECORDS, files);
        context.startService(i);
    }

    public static void wipeFiles(Context context, SrcDstCollection files)
    {
        Intent i = new Intent(context, FileOpsService.class);
        i.setAction(ACTION_WIPE);
        i.putExtra(ARG_RECORDS, files);
        context.startService(i);
    }

    public static void closeContainer(Context context, CryptoLocation container)
    {
        Intent i = new Intent(context, FileOpsService.class);
        i.setAction(ACTION_CLOSE_CONTAINER);
        LocationsManager.storePathsInIntent(i, container, null);
        context.startService(i);
    }

    public static void clearTempFolder(Context context, boolean exitProgram)
    {
        Intent i = new Intent(context, FileOpsService.class);
        i.setAction(ACTION_CLEAR_TEMP_FOLDER);
        i.putExtra(ClearTempFolderTask.ARG_EXIT_PROGRAM, exitProgram);
        context.startService(i);
    }

    public static void startTempFile(Context context, Location srcLocation)
    {
        try
        {
            Intent i = new Intent(context, FileOpsService.class);
            i.setAction(ACTION_START_TEMP_FILE);
            LocationsManager.storePathsInIntent(i, srcLocation, Collections.singletonList(srcLocation.getCurrentPath()));
            context.startService(i);
        }
        catch (IOException e)
        {
            Logger.showAndLog(context, e);
        }
    }

    public static void saveChangedFile(Context context, SrcDstCollection files)
    {
        Intent i = new Intent(context, FileOpsService.class);
        i.setAction(ACTION_SAVE_CHANGED_FILE);
        i.putExtra(ARG_RECORDS, files);
        context.startService(i);
    }

    public static void sendFile(Context context, String mimeType, Location srcLocation, Collection<? extends Path> paths)
    {
        Intent i = new Intent(context, FileOpsService.class);
        i.setAction(ACTION_SEND_TASK);
        LocationsManager.storePathsInIntent(i, srcLocation, paths);
        if (mimeType != null)
            i.putExtra(ActionSendTask.ARG_MIME_TYPE, mimeType);
        context.startService(i);
    }

    public static void cancelTask(Context context)
    {
        Intent i = new Intent(context, FileOpsService.class);
        i.setAction(ACTION_CANCEL_TASK);
        context.startService(i);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (ACTION_CANCEL_TASK.equals(intent.getAction()))
        {
            if (_currentTask != null)
            {
                _currentTask.cancel();
                _taskCancelled = true;
            }
            return Service.START_NOT_STICKY;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        _currentTask = null;
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        int notificationId = intent.getIntExtra(ARG_NOTIFICATION_ID, -1);
        if (notificationId >= 0)
        {
            NotificationManagerCompat.from(this).cancel(notificationId);
        }
        _taskCancelled = false;
        Task task = getTask(intent);
        if (task == null)
            Logger.log("Unsupported action: " + intent.getAction());
        else
        {
            _currentTask = task;
            Result result = null;
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FileOpTask " + intent.toString());
            wakeLock.acquire();
            try
            {
                if (GlobalConfig.isDebug())
                {
                    LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getApplicationContext());
                    Intent notifIntent = new Intent(intent.getAction());
                    notifIntent.putExtra(ARG_ORIG_INTENT, intent);
                    lbm.sendBroadcast(notifIntent);
                }
                Object res = _currentTask.doWork(this, intent);
                result = _taskCancelled ? new Result(new CancellationException(), true) : new Result(res);
            }
            catch (Throwable e)
            {
                result = new Result(e, false);
            }
            finally
            {
                try
                {
                    wakeLock.release();
                    _currentTask.onCompleted(result);
                    if (GlobalConfig.isDebug())
                    {
                        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getApplicationContext());
                        Intent notifIntent = new Intent(intent.getAction());
                        notifIntent.putExtra(ARG_ORIG_INTENT, intent);
                        notifIntent.putExtra(ARG_TASK_COMPLETED, true);
                        lbm.sendBroadcast(notifIntent);
                    }
                }
                catch (Throwable e)
                {
                    Logger.log(e);
                }
                finally
                {
                    _currentTask = null;
                }
            }
        }
    }

    protected Task getTask(Intent intent)
    {
        switch (intent.getAction())
        {
            case ACTION_COPY:
                return new CopyFilesTask();
            case ACTION_MOVE:
                return new MoveFilesTask();
            case ACTION_RECEIVE:
                return new ReceiveFilesTask();
            case ACTION_DELETE:
                return new DeleteFilesTask();
            case ACTION_WIPE:
                return new WipeFilesTask(true);
            case ACTION_CLEAR_TEMP_FOLDER:
                return new ClearTempFolderTask();
            case ACTION_START_TEMP_FILE:
                return new StartTempFileTask();
            case ACTION_SAVE_CHANGED_FILE:
                return new SaveTempFileChangesTask();
            case ACTION_SEND_TASK:
                return new ActionSendTask();
            case ACTION_CLOSE_CONTAINER:
                return new CloseContainerTask();
        }
        return null;
    }
}
