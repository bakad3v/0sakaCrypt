package com.igeltech.nevercrypt.android.settings.container;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.locations.fragments.CreateContainerFragmentBase;
import com.igeltech.nevercrypt.android.locations.tasks.CreateContainerTaskFragmentBase;
import com.igeltech.nevercrypt.android.settings.CheckBoxPropertyEditor;

public class SaveVolumeSettingsPropertyEditor extends CheckBoxPropertyEditor
{
    /**
     * Creates the checkbox that controls whether opening hints are saved for the new volume.
     */
    public SaveVolumeSettingsPropertyEditor(CreateContainerFragmentBase createContainerFragment)
    {
        super(createContainerFragment, R.string.save_volume_settings, R.string.save_volume_settings_desc);
    }

    /**
     * Loads whether opening hints should be saved for the created volume.
     */
    @Override
    protected boolean loadValue()
    {
        return getHostFragment().getState().getBoolean(CreateContainerTaskFragmentBase.ARG_SAVE_VOLUME_SETTINGS, false);
    }

    /**
     * Saves whether the formatter may persist the volume opening hints.
     */
    @Override
    protected void saveValue(boolean value)
    {
        getHostFragment().getState().putBoolean(CreateContainerTaskFragmentBase.ARG_SAVE_VOLUME_SETTINGS, value);
    }

    /**
     * Returns the container creation fragment that owns this property state.
     */
    protected CreateContainerFragmentBase getHostFragment()
    {
        return (CreateContainerFragmentBase) getHost();
    }
}
