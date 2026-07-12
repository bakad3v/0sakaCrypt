package com.igeltech.nevercrypt.android.locations.fragments;

import android.os.Bundle;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.activities.SettingsBaseActivity;
import com.igeltech.nevercrypt.android.locations.tasks.CreateContainerTaskFragmentBase;
import com.igeltech.nevercrypt.android.settings.container.CreateHiddenVolumePropertyEditor;
import com.igeltech.nevercrypt.container.ContainerFormatInfo;

public class CreateContainerFragment extends CreateContainerFragmentBase
{
    /**
     * Updates the outer-volume controls that depend on hidden-volume support.
     */
    @Override
    public void changeHiddenVolumeDependentOptions()
    {
        ContainerFormatInfo info = getCurrentContainerFormatInfo();
        boolean addExisting = _state.getBoolean(ARG_ADD_EXISTING_LOCATION, false);
        boolean containerFormat = info != null && !isEncFsFormat() && !addExisting;
        boolean hiddenSupported = containerFormat && info.hasHiddenContainerSupport();
        if (!hiddenSupported)
            _state.putBoolean(CreateContainerTaskFragmentBase.ARG_CREATE_HIDDEN_VOLUME, false);

        boolean createHidden = hiddenSupported
                && _state.getBoolean(
                        CreateContainerTaskFragmentBase.ARG_CREATE_HIDDEN_VOLUME, false
        );
        if (createHidden)
            _state.putBoolean(CreateContainerTaskFragmentBase.ARG_FILL_FREE_SPACE, true);

        _propertiesView.setPropertyState(R.string.fill_free_space_with_random_data, containerFormat && !createHidden);
        _propertiesView.setPropertyState(R.string.create_hidden_volume, hiddenSupported);
    }

    /**
     * Adds the outer-volume properties and the switch that enables the hidden-volume wizard step.
     */
    @Override
    protected void createContainerProperties()
    {
        super.createContainerProperties();
        _propertiesView.addProperty(new CreateHiddenVolumePropertyEditor(this));
    }

    /**
     * Continues the creation flow with the hidden-volume settings fragment.
     */
    @Override
    protected boolean handleCreateLocationTaskResult(Bundle args, int result) throws Exception
    {
        if (result != CreateContainerTaskFragmentBase.RESULT_REQUEST_HIDDEN_VOLUME_SETTINGS)
            return false;

        _state.putLong(
                CreateContainerTaskFragmentBase.ARG_MAX_HIDDEN_VOLUME_SIZE,
                args.getLong(CreateContainerTaskFragmentBase.ARG_MAX_HIDDEN_VOLUME_SIZE, 0));
        _state.remove(CreateContainerTaskFragmentBase.ARG_HIDDEN_SIZE);
        _state.putBoolean(CreateContainerTaskFragmentBase.ARG_HIDDEN_VOLUME_STAGE, true);

        Bundle nextState = transferStateToNextFragment();
        getParentFragmentManager().
                beginTransaction().
                replace(getId(), CreateHiddenVolumeFragment.newInstance(nextState), SettingsBaseActivity.SETTINGS_FRAGMENT_TAG).
                commit();
        return true;
    }
}
