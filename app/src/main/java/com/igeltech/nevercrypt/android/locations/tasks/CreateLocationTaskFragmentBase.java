package com.igeltech.nevercrypt.android.locations.tasks;

import android.content.Context;
import android.os.PowerManager;

import androidx.fragment.app.FragmentActivity;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.activities.SettingsBaseActivity;
import com.igeltech.nevercrypt.android.errors.InputOutputException;
import com.igeltech.nevercrypt.android.errors.UserException;
import com.igeltech.nevercrypt.android.errors.WrongPasswordOrBadContainerException;
import com.igeltech.nevercrypt.android.locations.fragments.CreateLocationFragment;
import com.igeltech.nevercrypt.container.LocationFormatter;
import com.igeltech.nevercrypt.crypto.SecureBuffer;
import com.igeltech.nevercrypt.fs.errors.WrongImageFormatException;
import com.igeltech.nevercrypt.locations.Location;
import com.igeltech.nevercrypt.locations.LocationsManager;
import com.igeltech.nevercrypt.locations.Openable;

import java.io.IOException;

public abstract class CreateLocationTaskFragmentBase extends com.igeltech.nevercrypt.android.fragments.TaskFragment
{
    public static final String TAG = "com.igeltech.nevercrypt.android.locations.tasks.CreateEDSLocationTaskFragment";
    public static final String ARG_LOCATION = "com.igeltech.nevercrypt.android.LOCATION";
    public static final String ARG_OVERWRITE = "com.igeltech.nevercrypt.android.OVERWRITE";
    public static final int RESULT_REQUEST_OVERWRITE = 1;
    protected Context _context;
    protected LocationsManager _locationsManager;

    @Override
    public void initTask(FragmentActivity activity)
    {
        _context = activity.getApplicationContext();
        _locationsManager = LocationsManager.getLocationsManager(_context);
    }

    @Override
    protected TaskCallbacks getTaskCallbacks(FragmentActivity activity)
    {
        CreateLocationFragment f = (CreateLocationFragment) getFragmentManager().findFragmentByTag(SettingsBaseActivity.SETTINGS_FRAGMENT_TAG);
        return f == null ? null : f.getCreateLocationTaskCallbacks();
    }

    @Override
    protected void doWork(TaskState state) throws Exception
    {
        state.setResult(0);
        Location location = _locationsManager.getLocation(getArguments().getParcelable(ARG_LOCATION));
        if (!checkParams(state, location))
            return;
        PowerManager pm = (PowerManager) _context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, toString());
        wl.acquire();
        try
        {
            createEDSLocation(state, location);
        }
        finally
        {
            wl.release();
        }
    }

    protected void createEDSLocation(TaskState state, Location locationLocation) throws Exception
    {
        LocationFormatter f = createFormatter();
        SecureBuffer password = getArguments().getParcelable(Openable.PARAM_PASSWORD);
        try
        {
            initFormatter(state, f, password);
            f.format(locationLocation);
        }
        catch (WrongImageFormatException e)
        {
            WrongPasswordOrBadContainerException e1 = new WrongPasswordOrBadContainerException(_context);
            e1.initCause(e);
            throw e1;
        }
        catch (IOException e)
        {
            throw new InputOutputException(_context, e);
        }
        catch (Exception e)
        {
            throw new UserException(_context, R.string.err_failed_creating_container, e);
        }
    }

    protected abstract LocationFormatter createFormatter();

    protected void initFormatter(final TaskState state, final LocationFormatter formatter, SecureBuffer password) throws Exception
    {
        formatter.setContext(_context);
        formatter.setPassword(password);
        formatter.setProgressReporter(prc -> {
            state.updateUI(prc);
            return !state.isTaskCancelled();
        });
    }

    protected boolean checkParams(TaskState state, Location locationLocation) throws Exception
    {
        if (!getArguments().getBoolean(ARG_OVERWRITE, false))
        {
            if (locationLocation.getCurrentPath().exists())
            {
                state.setResult(RESULT_REQUEST_OVERWRITE);
                return false;
            }
        }
        return true;
    }
}
