package com.igeltech.nevercrypt.android.locations.fragments;

import android.os.Bundle;
import android.view.Menu;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.fragments.TaskFragment;
import com.igeltech.nevercrypt.android.locations.tasks.CreateContainerTaskFragmentBase;
import com.igeltech.nevercrypt.android.locations.tasks.CreateHiddenVolumeTaskFragment;
import com.igeltech.nevercrypt.android.settings.container.ContainerPasswordPropertyEditor;
import com.igeltech.nevercrypt.android.settings.container.EncryptionAlgorithmPropertyEditor;
import com.igeltech.nevercrypt.android.settings.container.FileSystemTypePropertyEditor;
import com.igeltech.nevercrypt.android.settings.container.HashingAlgorithmPropertyEditor;
import com.igeltech.nevercrypt.android.settings.container.HiddenVolumeSizePropertyEditor;
import com.igeltech.nevercrypt.android.settings.container.PIMPropertyEditor;
import com.igeltech.nevercrypt.container.ContainerFormatInfo;

public class CreateHiddenVolumeFragment extends CreateContainerFragmentBase
{
    /**
     * Creates the second wizard step with state copied from the outer-volume step.
     */
    public static CreateHiddenVolumeFragment newInstance(Bundle state)
    {
        CreateHiddenVolumeFragment fragment = new CreateHiddenVolumeFragment();
        fragment.setArguments(new Bundle(state));
        return fragment;
    }

    /**
     * Keeps the confirmation action label specific to hidden-volume creation.
     */
    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.confirm).setTitle(R.string.create_hidden_volume);
    }

    /**
     * Updates the visible properties for the hidden-volume-only step.
     */
    @Override
    public void changeHiddenVolumeDependentOptions()
    {
        ContainerFormatInfo info = getCurrentContainerFormatInfo();
        boolean showHidden = info != null && info.hasHiddenContainerSupport();
        _propertiesView.setPropertyState(R.string.hidden_volume_password, showHidden);
        _propertiesView.setPropertyState(R.string.hidden_volume_size, showHidden);
        _propertiesView.setPropertyState(R.string.hidden_volume_encryption_algorithm, showHidden);
        _propertiesView.setPropertyState(R.string.hidden_volume_hash_algorithm, showHidden);
        _propertiesView.setPropertyState(R.string.hidden_volume_file_system_type, showHidden);
        _propertiesView.setPropertyState(R.string.hidden_volume_kdf_iterations_multiplier, showHidden && info.hasCustomKDFIterationsSupport());
    }

    /**
     * Skips the initial create-or-add-existing selector in the hidden-volume step.
     */
    @Override
    protected void createStartProperties()
    {
    }

    /**
     * Adds only hidden-volume settings to this step.
     */
    @Override
    protected void createNewLocationProperties()
    {
        _propertiesView.addProperty(new ContainerPasswordPropertyEditor(this, R.string.hidden_volume_password, CreateContainerTaskFragmentBase.ARG_HIDDEN_PASSWORD));
        _propertiesView.addProperty(new HiddenVolumeSizePropertyEditor(this));
        _propertiesView.addProperty(new PIMPropertyEditor(this, R.string.hidden_volume_kdf_iterations_multiplier, CreateContainerTaskFragmentBase.ARG_HIDDEN_KDF_ITERATIONS));
        _propertiesView.addProperty(new EncryptionAlgorithmPropertyEditor(
                this,
                R.string.hidden_volume_encryption_algorithm,
                CreateContainerTaskFragmentBase.ARG_HIDDEN_CIPHER_NAME,
                CreateContainerTaskFragmentBase.ARG_HIDDEN_CIPHER_MODE_NAME,
                true));
        _propertiesView.addProperty(new HashingAlgorithmPropertyEditor(
                this,
                R.string.hidden_volume_hash_algorithm,
                CreateContainerTaskFragmentBase.ARG_HIDDEN_HASHING_ALG,
                true));
        _propertiesView.addProperty(new FileSystemTypePropertyEditor(
                this,
                R.string.hidden_volume_file_system_type,
                CreateContainerTaskFragmentBase.ARG_HIDDEN_FILE_SYSTEM_TYPE));
    }

    /**
     * Starts the task that writes the hidden volume into the existing outer container.
     */
    @Override
    protected TaskFragment createCreateLocationTask()
    {
        return new CreateHiddenVolumeTaskFragment();
    }

    /**
     * Shows only hidden-volume settings and preserves the outer-container location.
     */
    @Override
    public void showCreateNewLocationProperties()
    {
        _state.putBoolean(ARG_ADD_EXISTING_LOCATION, false);
        _state.putBoolean(CreateContainerTaskFragmentBase.ARG_CREATE_HIDDEN_VOLUME, true);
        _state.putBoolean(CreateContainerTaskFragmentBase.ARG_HIDDEN_VOLUME_STAGE, true);
        _propertiesView.setPropertiesState(false);
        changeHiddenVolumeDependentOptions();
    }
}
