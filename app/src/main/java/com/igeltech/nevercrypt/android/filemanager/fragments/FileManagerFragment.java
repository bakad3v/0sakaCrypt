package com.igeltech.nevercrypt.android.filemanager.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.igeltech.nevercrypt.android.Logger;
import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.dialogs.AskOverwriteDialog;
import com.igeltech.nevercrypt.android.filemanager.activities.FileManagerActivity;
import com.igeltech.nevercrypt.android.filemanager.records.BrowserRecord;
import com.igeltech.nevercrypt.android.fragments.TaskFragment;
import com.igeltech.nevercrypt.android.helpers.CachedPathInfo;
import com.igeltech.nevercrypt.android.helpers.ProgressDialogTaskFragmentCallbacks;
import com.igeltech.nevercrypt.android.service.FileOpsService;
import com.igeltech.nevercrypt.android.settings.UserSettings;
import com.igeltech.nevercrypt.fs.Path;
import com.igeltech.nevercrypt.fs.util.SrcDstCollection;
import com.igeltech.nevercrypt.locations.Location;
import com.igeltech.nevercrypt.locations.LocationsManager;
import com.trello.rxlifecycle3.components.support.RxFragment;

import java.util.NavigableSet;
import java.util.TreeSet;

public class FileManagerFragment extends RxFragment implements PreviewFragment.Host
{
    public static final String TAG = "FileManagerFragment";

    private final BroadcastReceiver _updatePathReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            rereadCurrentLocation();
        }
    };
    protected boolean _isLargeScreenLayout;
    FileManagerFragmentArgs _args;
    Location _currentLocation;

    public static Intent getOverwriteRequestIntent(Context context, boolean move, SrcDstCollection records)
    {
        Intent i = new Intent(context, FileManagerActivity.class);
        i.setAction(FileManagerActivity.ACTION_ASK_OVERWRITE);
        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra(AskOverwriteDialog.ARG_MOVE, move);
        i.putExtra(AskOverwriteDialog.ARG_PATHS, records);
        return i;
    }

    public boolean isSingleSelectionMode()
    {
        return !getActivity().getIntent().getBooleanExtra(FileManagerActivity.EXTRA_ALLOW_MULTIPLE, false);
    }

    public boolean allowFileSelect()
    {
        return getActivity().getIntent().getBooleanExtra(FileManagerActivity.EXTRA_ALLOW_FILE_SELECT, true);
    }

    public boolean allowFolderSelect()
    {
        return getActivity().getIntent().getBooleanExtra(FileManagerActivity.EXTRA_ALLOW_FOLDER_SELECT, true);
    }

    public void goTo(Location location, int scrollPosition)
    {
        Logger.debug(TAG + ": goTo");
        closeIntegratedViewer();
        FileListViewFragment f = getFileListViewFragment();
        if (f != null)
            f.goTo(location, scrollPosition, true);
    }

    public void goTo(Path path)
    {
        Location prevLocation = getLocation();
        if (prevLocation != null)
        {
            Location newLocation = prevLocation.copy();
            newLocation.setCurrentPath(path);
            goTo(newLocation, 0);
        }
    }

    public void rereadCurrentLocation()
    {
        FileListViewFragment f = getFileListViewFragment();
        if (f != null)
            f.rereadCurrentLocation();
    }

    public boolean isWideScreenLayout()
    {
        return _isLargeScreenLayout;
    }

    @Override
    public NavigableSet<? extends CachedPathInfo> getCurrentFiles()
    {
        FileListDataFragment f = getFileListDataFragment();
        return f != null ? f.getFileList() : new TreeSet<>();
    }

    @Override
    public Object getFilesListSync()
    {
        FileListDataFragment f = getFileListDataFragment();
        return f != null ? f.getFilesListSync() : new Object();
    }

    public Location getLocation()
    {
        return _currentLocation;
    }

    public boolean hasSelectedFiles()
    {
        FileListDataFragment f = getFileListDataFragment();
        return f != null && f.hasSelectedFiles();
    }

    public void showProperties(BrowserRecord currentFile, boolean allowInplace)
    {
        if (!hasSelectedFiles() && currentFile == null)
        {
            Logger.debug(TAG + ": showProperties (hide)");
            if (getChildFragmentManager().findFragmentByTag(FilePropertiesFragment.TAG) != null)
                hideSecondaryFragment();
        }
        else if (_isLargeScreenLayout || !allowInplace)
        {
            Logger.debug(TAG + ": showProperties");
            showPropertiesFragment(currentFile);
        }
    }

    public void showPhoto(BrowserRecord currentFile, boolean allowInplace)
    {
        Logger.debug(TAG + ": showPhoto");
        Path contextPath = currentFile == null ? null : currentFile.getPath();
        if (!hasSelectedFiles() && contextPath == null)
            hideSecondaryFragment();
        else if (_isLargeScreenLayout || !allowInplace)
            showPreviewFragment(contextPath);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        ContextCompat.registerReceiver(requireContext().getApplicationContext(), _updatePathReceiver, new IntentFilter(FileOpsService.BROADCAST_FILE_OPERATION_COMPLETED), ContextCompat.RECEIVER_EXPORTED);
        _isLargeScreenLayout = !UserSettings.getSettings(getContext()).disableLargeSceenLayouts() && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        // Get arguments passed by NavController
        _args = FileManagerFragmentArgs.fromBundle(requireArguments());
        // Add panel with ListView
        FragmentTransaction trans = getChildFragmentManager().beginTransaction();
        trans.add(new FileListDataFragment(), FileListDataFragment.TAG);
        trans.add(R.id.fragment1, new FileListViewFragment(), FileListViewFragment.TAG);
        trans.disallowAddToBackStack();
        trans.commitNow();
        // Back button
        final OnBackPressedCallback callback = new OnBackPressedCallback(true)
        {
            @Override
            public void handleOnBackPressed()
            {
                Fragment f = getChildFragmentManager().findFragmentById(R.id.fragment2);
                if (f != null)
                {
                    if (f.isVisible() && !_isLargeScreenLayout)
                    {
                        // If there is a preview pane, close it
                        closeIntegratedViewer();
                        return;
                    }
                }
                // Try to navigate one folder up, or else return to previous activity
                f = getFileListViewFragment();
                if (!((FileListViewFragment) f).goToPrevLocation())
                {
                    setEnabled(false);
                    requireActivity().onBackPressed();
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_filemanager, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        _currentLocation = LocationsManager.getLocationsManager(getActivity()).getFromBundle(_args.getLocation(), null);
        goTo(_currentLocation, _args.getScrollPosition());
    }

    @Override
    public void onToggleFullScreen()
    {
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
    }

    public FileListDataFragment getFileListDataFragment()
    {
        FileListDataFragment f = (FileListDataFragment) getChildFragmentManager().findFragmentByTag(FileListDataFragment.TAG);
        return f != null && f.isAdded() ? f : null;
    }

    public FileListViewFragment getFileListViewFragment()
    {
        FileListViewFragment f = (FileListViewFragment) getChildFragmentManager().findFragmentByTag(FileListViewFragment.TAG);
        return f != null && f.isAdded() ? f : null;
    }

    protected void showSecondaryFragment(Fragment f, String tag)
    {
        View panel;
        if (!_isLargeScreenLayout)
        {
            panel = getView().findViewById(R.id.fragment1);
            if (panel != null)
                panel.setVisibility(View.GONE);
        }
        panel = getView().findViewById(R.id.fragment2);
        if (panel != null)
            panel.setVisibility(View.VISIBLE);
        // Replace contents
        FragmentTransaction trans = getChildFragmentManager().beginTransaction();
        trans.replace(R.id.fragment2, f, tag);
        trans.commitNowAllowingStateLoss();
    }

    protected boolean hideSecondaryFragment()
    {
        Logger.debug(TAG + ": hideSecondaryFragment");
        FragmentManager fm = getChildFragmentManager();
        Fragment f = fm.findFragmentById(R.id.fragment2);
        if (f != null)
        {
            FragmentTransaction trans = fm.beginTransaction();
            trans.remove(f);
            trans.commit();
            View panel = getView().findViewById(R.id.fragment1);
            if (panel != null)
                panel.setVisibility(View.VISIBLE);
            if (!_isLargeScreenLayout)
            {
                panel = getView().findViewById(R.id.fragment2);
                if (panel != null)
                    panel.setVisibility(View.GONE);
            }
            requireActivity().invalidateOptionsMenu();
            return true;
        }
        return false;
    }

    private void showPropertiesFragment(BrowserRecord currentFile)
    {
        FilePropertiesFragment f = FilePropertiesFragment.newInstance(currentFile == null ? null : currentFile.getPath());
        showSecondaryFragment(f, FilePropertiesFragment.TAG);
    }

    private void showPreviewFragment(Path currentImage)
    {
        PreviewFragment f = PreviewFragment.newInstance(currentImage);
        showSecondaryFragment(f, PreviewFragment.TAG);
    }

    private void closeIntegratedViewer()
    {
        Logger.debug(TAG + ": closeIntegratedViewer");
        hideSecondaryFragment();
    }

    public boolean isSelectAction()
    {
        String action = getActivity().getIntent().getAction();
        return Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action);
    }

    public TaskFragment.TaskCallbacks getCheckStartPathCallbacks()
    {
        return new ProgressDialogTaskFragmentCallbacks(getActivity(), R.string.loading)
        {
            @Override
            public void onCompleted(Bundle args, TaskFragment.Result result)
            {
                try
                {
                    Location locToOpen = (Location) result.getResult();
                    if (locToOpen != null)
                    {
                        _host.setIntent(new Intent(Intent.ACTION_MAIN, locToOpen.getLocationUri()));
                        FileListDataFragment df = getFileListDataFragment();
                        if (df != null)
                            df.loadLocation(null, true);
                    }
                    else
                        _host.setIntent(new Intent());
                }
                catch (Throwable e)
                {
                    Logger.showAndLog(_host, e);
                    _host.setIntent(new Intent());
                }
            }
        };
    }
}

