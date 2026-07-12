package com.igeltech.nevercrypt.android.settings.container;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.locations.fragments.CreateContainerFragmentBase;
import com.igeltech.nevercrypt.android.locations.tasks.CreateContainerTaskFragmentBase;
import com.igeltech.nevercrypt.android.settings.SwitchPropertyEditor;

public class CreateHiddenVolumePropertyEditor extends SwitchPropertyEditor
{
    /**
     * Creates the switch that marks the new outer volume as a hidden-volume host.
     */
    public CreateHiddenVolumePropertyEditor(CreateContainerFragmentBase createContainerFragment)
    {
        super(createContainerFragment, R.string.create_hidden_volume, R.string.create_hidden_volume_desc);
    }

    /**
     * Loads whether the user requested the hidden-volume second step.
     */
    @Override
    protected boolean loadValue()
    {
        return getHostFragment().getState().getBoolean(CreateContainerTaskFragmentBase.ARG_CREATE_HIDDEN_VOLUME, false);
    }

    /**
     * Saves the hidden-volume request and forces randomization of the outer free space.
     */
    @Override
    protected void saveValue(boolean value)
    {
        getHostFragment().getState().putBoolean(CreateContainerTaskFragmentBase.ARG_CREATE_HIDDEN_VOLUME, value);
        if (value)
            getHostFragment().getState().putBoolean(CreateContainerTaskFragmentBase.ARG_FILL_FREE_SPACE, true);
    }

    /**
     * Refreshes dependent controls when the hidden-volume request changes.
     */
    @Override
    protected boolean onChecked(boolean isChecked)
    {
        boolean res = super.onChecked(isChecked);
        getHostFragment().changeHiddenVolumeDependentOptions();
        return res;
    }

    /**
     * Returns the container creation fragment that owns this property state.
     */
    protected CreateContainerFragmentBase getHostFragment()
    {
        return (CreateContainerFragmentBase) getHost();
    }
}
