package com.igeltech.nevercrypt.android.filemanager.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.navigation.Navigation;

import com.igeltech.nevercrypt.android.activities.EdgeToEdgeToolbarActivity;
import com.igeltech.nevercrypt.android.Logger;
import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.dialogs.AskOverwriteDialog;
import com.igeltech.nevercrypt.locations.LocationsManager;
import com.trello.rxlifecycle3.components.support.RxAppCompatActivity;

public class FileManagerActivity extends RxAppCompatActivity implements EdgeToEdgeToolbarActivity
{
    public static final String ACTION_ASK_OVERWRITE = "com.igeltech.nevercrypt.android.ACTION_ASK_OVERWRITE";
    public static final String EXTRA_ALLOW_MULTIPLE = Intent.EXTRA_ALLOW_MULTIPLE;
    public static final String EXTRA_ALLOW_FILE_SELECT = "com.igeltech.nevercrypt.android.ALLOW_FILE_SELECT";
    public static final String EXTRA_ALLOW_FOLDER_SELECT = "com.igeltech.nevercrypt.android.ALLOW_FOLDER_SELECT";
    public static final String EXTRA_ALLOW_CREATE_NEW_FILE = "com.igeltech.nevercrypt.android.ALLOW_CREATE_NEW_FILE";
    public static final String EXTRA_ALLOW_CREATE_NEW_FOLDER = "com.igeltech.nevercrypt.android.ALLOW_CREATE_NEW_FOLDER";
    public static final String TAG = "FileManagerActivity";

    private final BroadcastReceiver _closeAllReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            finish();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContentView(R.layout.content_filemanager);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Logger.debug("fm start activity: " + getIntent());
        checkAction();
        // Set navigation graph manually to pass parameters to start fragment
        Navigation.findNavController(this, R.id.nav_host_filemanager).setGraph(R.navigation.nav_filemanager, getIntent().getExtras());
    }

    @Override
    public void onStart()
    {
        super.onStart();
        // Broadcasts
        ContextCompat.registerReceiver(this, _closeAllReceiver, new IntentFilter(LocationsManager.BROADCAST_ALL_CONTAINERS_CLOSED), ContextCompat.RECEIVER_EXPORTED);
        Logger.debug("FileManagerFragment has started");
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        unregisterReceiver(_closeAllReceiver);
        Logger.debug("FileManagerFragment has stopped");
    }

    @Override
    public boolean onSupportNavigateUp()
    {
        finish();
        return true;
    }

    protected void checkAction()
    {
        String action = getIntent().getAction();
        if (action == null)
            action = "";
        Logger.log("FileManagerActivity action is " + action);
        try
        {
            if (action == ACTION_ASK_OVERWRITE)
            {
                AskOverwriteDialog.showDialog(getSupportFragmentManager(), getIntent().getExtras());
                setIntent(new Intent());
            }
        }
        catch (Exception e)
        {
            Logger.showAndLog(this, e);
            finish();
        }
    }
}
