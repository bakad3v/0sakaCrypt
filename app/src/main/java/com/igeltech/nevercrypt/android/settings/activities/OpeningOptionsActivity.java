package com.igeltech.nevercrypt.android.settings.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;

import com.igeltech.nevercrypt.android.Logger;
import com.igeltech.nevercrypt.android.activities.SettingsBaseActivity;
import com.igeltech.nevercrypt.android.settings.PropertiesHostWithStateBundle;
import com.igeltech.nevercrypt.android.settings.fragments.OpeningOptionsFragment;

public class OpeningOptionsActivity extends SettingsBaseActivity
{

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true)
            {
                @Override
                public void handleOnBackPressed()
                {
                    finishAfterSavingResult();
                }
            });
    }

    @Override
    public boolean onSupportNavigateUp()
    {
        if (!finishWithResult())
            return false;
        return super.onSupportNavigateUp();
    }

    private void finishAfterSavingResult()
    {
        if (finishWithResult())
            finish();
    }

    private boolean finishWithResult()
    {
        PropertiesHostWithStateBundle frag = (PropertiesHostWithStateBundle) getSupportFragmentManager().findFragmentByTag(SETTINGS_FRAGMENT_TAG);
        if (frag == null)
            return true;
        try
        {
            frag.getPropertiesView().saveProperties();
            Intent res = new Intent();
            res.putExtras(frag.getState());
            setResult(RESULT_OK, res);
            return true;
        }
        catch (Exception e)
        {
            Logger.showAndLog(this, e);
            return false;
        }
    }

    @Override
    protected Fragment getSettingsFragment()
    {
        return getOpeningOptionsFragment();
    }

    private Fragment getOpeningOptionsFragment()
    {
        return new OpeningOptionsFragment();
    }
}
