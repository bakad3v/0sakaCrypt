package com.igeltech.nevercrypt.android.activities;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.igeltech.nevercrypt.android.helpers.CompatHelper;
import com.igeltech.nevercrypt.android.settings.UserSettings;

public abstract class SettingsBaseActivity extends AppCompatActivity
{
    public static final String SETTINGS_FRAGMENT_TAG = "com.igeltech.nevercrypt.android.locations.SETTINGS_FRAGMENT";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        int layoutResId = getSettingsLayoutResId();
        if (layoutResId != 0)
        {
            setContentView(layoutResId);
            onSettingsContentViewCreated();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
        if (UserSettings.getSettings(this).isFlagSecureEnabled())
            CompatHelper.setWindowFlagSecure(this);
        if (savedInstanceState == null)
            getSupportFragmentManager().
                    beginTransaction().
                    add(getSettingsContainerId(), getSettingsFragment(), SETTINGS_FRAGMENT_TAG).
                    commit();
    }

    @Override
    public boolean onSupportNavigateUp()
    {
        finish();
        return true;
    }

    protected abstract Fragment getSettingsFragment();

    protected int getSettingsLayoutResId()
    {
        return 0;
    }

    protected int getSettingsContainerId()
    {
        return android.R.id.content;
    }

    protected void onSettingsContentViewCreated()
    {
    }
}
