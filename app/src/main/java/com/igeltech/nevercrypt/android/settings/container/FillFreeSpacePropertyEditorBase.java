package com.igeltech.nevercrypt.android.settings.container;

import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.textview.MaterialTextView;
import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.locations.fragments.CreateContainerFragmentBase;
import com.igeltech.nevercrypt.android.locations.tasks.CreateContainerTaskFragmentBase;
import com.igeltech.nevercrypt.android.settings.SwitchPropertyEditor;

public abstract class FillFreeSpacePropertyEditorBase extends SwitchPropertyEditor
{
    protected MaterialTextView _titleTextView;

    public FillFreeSpacePropertyEditorBase(CreateContainerFragmentBase createContainerFragment)
    {
        super(createContainerFragment, R.string.fill_free_space_with_random_data, 0);
    }

    @Override
    public View createView(ViewGroup parent)
    {
        View view = super.createView(parent);
        _titleTextView = view.findViewById(R.id.title_edit);
        return view;
    }

    @Override
    protected void saveValue(boolean value)
    {
        getHostFragment().getState().putBoolean(CreateContainerTaskFragmentBase.ARG_FILL_FREE_SPACE, value);
    }

    @Override
    protected boolean onChecked(boolean isChecked)
    {
        getHostFragment().getState().putBoolean(CreateContainerTaskFragmentBase.ARG_FILL_FREE_SPACE_USER_SET, true);
        return super.onChecked(isChecked);
    }

    @Override
    protected boolean loadValue()
    {
        _titleTextView.setText(R.string.fill_free_space_with_random_data);
        return getHostFragment().getState().getBoolean(CreateContainerTaskFragmentBase.ARG_FILL_FREE_SPACE);
    }

    protected CreateContainerFragmentBase getHostFragment()
    {
        return (CreateContainerFragmentBase) getHost();
    }
}
