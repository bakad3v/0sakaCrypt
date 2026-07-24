package com.igeltech.nevercrypt.android.activities;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public abstract class SettingsBaseActivity extends AppCompatActivity implements EdgeToEdgeToolbarActivity
{
    public static final String SETTINGS_FRAGMENT_TAG = "com.igeltech.nevercrypt.android.locations.SETTINGS_FRAGMENT";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContentView(getSettingsContentLayoutResId());
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
        if (savedInstanceState == null)
            getSupportFragmentManager().
                    beginTransaction().
                    add(getEdgeToEdgeContentId(), getSettingsFragment(), SETTINGS_FRAGMENT_TAG).
                    commit();
    }

    @Override
    public boolean onSupportNavigateUp()
    {
        finish();
        return true;
    }

    protected abstract Fragment getSettingsFragment();

    protected int getSettingsContentLayoutResId()
    {
        return 0;
    }
}
