package com.igeltech.nevercrypt.android.filemanager.activities;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import com.igeltech.nevercrypt.android.Logger;
import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.filemanager.fragments.FileListDataFragment;
import com.igeltech.nevercrypt.android.filemanager.fragments.PreviewFragment;
import com.igeltech.nevercrypt.android.fragments.TaskFragment;
import com.igeltech.nevercrypt.android.fragments.TaskFragment.Result;
import com.igeltech.nevercrypt.android.helpers.CachedPathInfo;
import com.igeltech.nevercrypt.android.helpers.CachedPathInfoBase;
import com.igeltech.nevercrypt.android.helpers.CompatHelper;
import com.igeltech.nevercrypt.android.helpers.ProgressDialogTaskFragmentCallbacks;
import com.igeltech.nevercrypt.android.settings.UserSettings;
import com.igeltech.nevercrypt.fs.Path;
import com.igeltech.nevercrypt.fs.util.Util;
import com.igeltech.nevercrypt.locations.Location;
import com.igeltech.nevercrypt.locations.LocationsManager;
import com.igeltech.nevercrypt.settings.Settings;

import java.util.ArrayList;
import java.util.NavigableSet;
import java.util.TreeSet;

public class ImageViewerActivity extends AppCompatActivity implements PreviewFragment.Host
{
    public static final String INTENT_PARAM_CURRENT_PATH = "current_path";
    private TreeSet<CachedPathInfo> _files;
    private Location _location;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        UserSettings us = UserSettings.getSettings(this);
        if (us.isImageViewerFullScreenModeEnabled())
            enableFullScreen();
        _location = LocationsManager.getLocationsManager(this).getFromIntent(getIntent(), null);
        getSupportFragmentManager().beginTransaction().add(RestorePathsTask.newInstance(), RestorePathsTask.TAG).commit();
    }

    @Override
    public NavigableSet<? extends CachedPathInfo> getCurrentFiles()
    {
        return _files;
    }

    @Override
    public Location getLocation()
    {
        return _location;
    }

    @Override
    public Object getFilesListSync()
    {
        return new Object();
    }

    @Override
    public void onToggleFullScreen()
    {
        CompatHelper.restartActivity(this);
    }

    public TaskFragment.TaskCallbacks getRestorePathsTaskCallbacks()
    {
        return new ProgressDialogTaskFragmentCallbacks(this, R.string.loading)
        {
            @Override
            public void onCompleted(Bundle args, Result result)
            {
                super.onCompleted(args, result);
                try
                {
                    _files = (TreeSet<CachedPathInfo>) result.getResult();
                }
                catch (Throwable e)
                {
                    Logger.showAndLog(_host, result.getError());
                }
                if (getPreviewFragment() == null)
                    showFragment(getIntent().getStringExtra(INTENT_PARAM_CURRENT_PATH));
            }
        };
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        PreviewFragment pf = (PreviewFragment) getSupportFragmentManager().findFragmentByTag(PreviewFragment.TAG);
        if (pf != null)
            pf.updateImageViewFullScreen();
    }

    private PreviewFragment getPreviewFragment()
    {
        return (PreviewFragment) getSupportFragmentManager().findFragmentByTag(PreviewFragment.TAG);
    }

    private void showFragment(String currentImagePathString)
    {
        PreviewFragment f = PreviewFragment.newInstance(currentImagePathString);
        getSupportFragmentManager().beginTransaction().add(android.R.id.content, f, PreviewFragment.TAG).commit();
    }

    private void enableFullScreen()
    {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        invalidateOptionsMenu();
    }

    public static class RestorePathsTask extends TaskFragment
    {
        public static final String TAG = "RestorePathsTask";
        private Location _loc;
        private ArrayList<String> _pathStrings;
        private Settings _settings;

        public static RestorePathsTask newInstance()
        {
            return new RestorePathsTask();
        }

        protected void initTask(AppCompatActivity activity)
        {
            _loc = ((ImageViewerActivity) activity).getLocation();
            _pathStrings = activity.getIntent().getStringArrayListExtra(LocationsManager.PARAM_PATHS);
            _settings = UserSettings.getSettings(activity);
        }

        @Override
        protected void doWork(TaskState state) throws Exception
        {
            ArrayList<Path> paths = Util.restorePaths(_loc.getFS(), _pathStrings);
            TreeSet<CachedPathInfo> res = new TreeSet(FileListDataFragment.getComparator(_settings));
            for (Path p : paths)
            {
                CachedPathInfoBase cpi = new CachedPathInfoBase();
                cpi.init(p);
                res.add(cpi);
            }
            state.setResult(res);
        }

        @Override
        protected TaskCallbacks getTaskCallbacks(FragmentActivity activity)
        {
            return ((ImageViewerActivity) activity).getRestorePathsTaskCallbacks();
        }
    }
}
