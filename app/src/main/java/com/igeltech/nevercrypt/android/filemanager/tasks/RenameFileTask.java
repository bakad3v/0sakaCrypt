package com.igeltech.nevercrypt.android.filemanager.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import com.igeltech.nevercrypt.android.Logger;
import com.igeltech.nevercrypt.android.fragments.TaskFragment;
import com.igeltech.nevercrypt.android.service.FileOpsService;
import com.igeltech.nevercrypt.fs.Path;
import com.igeltech.nevercrypt.locations.Location;
import com.igeltech.nevercrypt.locations.LocationsManager;

public class RenameFileTask extends TaskFragment
{
    public static final String TAG = "RenameFileTask";
    public static final String ARG_NEW_NAME = "com.igeltech.nevercrypt.android.NEW_NAME";
    private Context _context;

    public static RenameFileTask newInstance(Location target, String newName)
    {
        Bundle args = new Bundle();
        args.putString(ARG_NEW_NAME, newName);
        LocationsManager.storePathsInBundle(args, target, null);
        RenameFileTask f = new RenameFileTask();
        f.setArguments(args);
        return f;
    }

    @Override
    public void initTask(FragmentActivity activity)
    {
        _context = activity.getApplicationContext();
    }

    @Override
    protected void doWork(TaskState state) throws Exception
    {
        String newName = getArguments().getString(ARG_NEW_NAME);
        Location target = LocationsManager.getLocationsManager(_context).getFromBundle(getArguments(), null);
        Path path = target.getCurrentPath();
        if (path.isFile())
            path.getFile().rename(newName);
        else if (path.isDirectory())
            path.getDirectory().rename(newName);
    }

    @Override
    protected TaskCallbacks getTaskCallbacks(final FragmentActivity activity)
    {
        return new TaskCallbacks()
        {
            @Override
            public void onUpdateUI(Object state)
            {
            }

            @Override
            public void onPrepare(Bundle args)
            {
            }

            @Override
            public void onResumeUI(Bundle args)
            {
            }

            @Override
            public void onSuspendUI(Bundle args)
            {
            }

            @Override
            public void onCompleted(Bundle args, Result result)
            {
                try
                {
                    result.getResult();
                    Intent intent = new Intent(FileOpsService.BROADCAST_FILE_OPERATION_COMPLETED);
                    intent.setPackage(activity.getPackageName());
                    activity.sendBroadcast(intent);
                }
                catch (Throwable e)
                {
                    Logger.showAndLog(activity, e);
                }
            }
        };
    }
}
