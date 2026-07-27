package com.igeltech.nevercrypt.android.activities;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.igeltech.nevercrypt.android.R;

public interface EdgeToEdgeToolbarActivity
{
    default void setEdgeToEdgeContentView(@LayoutRes int contentLayoutResId)
    {
        AppCompatActivity activity = requireAppCompatActivity();
        activity.setContentView(R.layout.activity_with_toolbar);
        if (contentLayoutResId != 0)
        {
            ViewGroup content = activity.findViewById(getEdgeToEdgeContentId());
            LayoutInflater.from(activity).inflate(contentLayoutResId, content, true);
        }
        setupEdgeToEdgeToolbar();
    }

    @IdRes
    private int getEdgeToEdgeRootId()
    {
        return R.id.activity_root;
    }

    @IdRes
    private int getEdgeToEdgeToolbarId()
    {
        return R.id.activity_toolbar;
    }

    @IdRes
    default int getEdgeToEdgeContentId()
    {
        return R.id.activity_content;
    }

    private void setupEdgeToEdgeToolbar()
    {
        AppCompatActivity activity = requireAppCompatActivity();
        activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
        activity.getWindow().setNavigationBarColor(Color.TRANSPARENT);
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);

        MaterialToolbar toolbar = activity.findViewById(getEdgeToEdgeToolbarId());
        toolbar.setTitle(activity.getTitle());
        activity.setSupportActionBar(toolbar);

        View root = activity.findViewById(getEdgeToEdgeRootId());
        View content = activity.findViewById(getEdgeToEdgeContentId());

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

    default AppCompatActivity requireAppCompatActivity()
    {
        if (!(this instanceof AppCompatActivity))
            throw new IllegalStateException("EdgeToEdgeToolbarActivity must be implemented by AppCompatActivity");
        return (AppCompatActivity) this;
    }
}
