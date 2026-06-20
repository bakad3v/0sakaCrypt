package com.igeltech.nevercrypt.android.settings.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.igeltech.nevercrypt.android.Logger;
import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.activities.SettingsBaseActivity;
import com.igeltech.nevercrypt.android.settings.PropertiesHostWithStateBundle;
import com.igeltech.nevercrypt.android.settings.fragments.OpeningOptionsFragment;
import com.google.android.material.appbar.MaterialToolbar;

public class OpeningOptionsActivity extends SettingsBaseActivity
{

    @Override
    protected int getSettingsLayoutResId()
    {
        return R.layout.activity_opening_options;
    }

    @Override
    protected int getSettingsContainerId()
    {
        return R.id.opening_options_container;
    }

    @Override
    protected void onSettingsContentViewCreated()
    {
        setupEdgeToEdgeToolbar();
    }

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

    private void setupEdgeToEdgeToolbar()
    {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        MaterialToolbar toolbar = findViewById(R.id.opening_options_toolbar);
        toolbar.setTitle(getTitle());
        setSupportActionBar(toolbar);

        View root = findViewById(R.id.opening_options_root);
        View content = findViewById(R.id.opening_options_container);

        int toolbarHeight = toolbar.getLayoutParams().height;
        int toolbarPaddingLeft = toolbar.getPaddingLeft();
        int toolbarPaddingTop = toolbar.getPaddingTop();
        int toolbarPaddingRight = toolbar.getPaddingRight();
        int toolbarPaddingBottom = toolbar.getPaddingBottom();

        int contentPaddingLeft = content.getPaddingLeft();
        int contentPaddingTop = content.getPaddingTop();
        int contentPaddingRight = content.getPaddingRight();
        int contentPaddingBottom = content.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());

            ViewGroup.LayoutParams toolbarLayoutParams = toolbar.getLayoutParams();
            toolbarLayoutParams.height = toolbarHeight + statusBars.top;
            toolbar.setLayoutParams(toolbarLayoutParams);
            toolbar.setPadding(
                    toolbarPaddingLeft + systemBars.left,
                    toolbarPaddingTop + statusBars.top,
                    toolbarPaddingRight + systemBars.right,
                    toolbarPaddingBottom);

            content.setPadding(
                    contentPaddingLeft + systemBars.left,
                    contentPaddingTop,
                    contentPaddingRight + systemBars.right,
                    contentPaddingBottom + systemBars.bottom);

            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }
}
