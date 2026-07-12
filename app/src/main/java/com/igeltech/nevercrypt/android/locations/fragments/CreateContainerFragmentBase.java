package com.igeltech.nevercrypt.android.locations.fragments;

import android.os.Bundle;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.dialogs.PasswordDialog;
import com.igeltech.nevercrypt.android.dialogs.PasswordDialogBase;
import com.igeltech.nevercrypt.android.fragments.TaskFragment;
import com.igeltech.nevercrypt.android.locations.tasks.AddExistingContainerTaskFragment;
import com.igeltech.nevercrypt.android.locations.tasks.CreateContainerTaskFragment;
import com.igeltech.nevercrypt.android.locations.tasks.CreateContainerTaskFragmentBase;
import com.igeltech.nevercrypt.android.locations.tasks.CreateEncFsTaskFragment;
import com.igeltech.nevercrypt.android.settings.PropertyEditor;
import com.igeltech.nevercrypt.android.settings.UserSettings;
import com.igeltech.nevercrypt.android.settings.container.ContainerFormatPropertyEditor;
import com.igeltech.nevercrypt.android.settings.container.ContainerPasswordPropertyEditor;
import com.igeltech.nevercrypt.android.settings.container.ContainerSizePropertyEditor;
import com.igeltech.nevercrypt.android.settings.container.EncryptionAlgorithmPropertyEditor;
import com.igeltech.nevercrypt.android.settings.container.FileSystemTypePropertyEditor;
import com.igeltech.nevercrypt.android.settings.container.FillFreeSpacePropertyEditor;
import com.igeltech.nevercrypt.android.settings.container.HashingAlgorithmPropertyEditor;
import com.igeltech.nevercrypt.android.settings.container.PIMPropertyEditor;
import com.igeltech.nevercrypt.android.settings.container.PathToContainerPropertyEditor;
import com.igeltech.nevercrypt.android.settings.encfs.BlockSizePropertyEditor;
import com.igeltech.nevercrypt.android.settings.encfs.DataCodecPropertyEditor;
import com.igeltech.nevercrypt.android.settings.encfs.EnableEmptyBlocksPropertyEditor;
import com.igeltech.nevercrypt.android.settings.encfs.ExternalFileIVPropertyEditor;
import com.igeltech.nevercrypt.android.settings.encfs.FilenameIVChainingPropertyEditor;
import com.igeltech.nevercrypt.android.settings.encfs.KeySizePropertyEditor;
import com.igeltech.nevercrypt.android.settings.encfs.MACBytesPerBlockPropertyEditor;
import com.igeltech.nevercrypt.android.settings.encfs.NameCodecPropertyEditor;
import com.igeltech.nevercrypt.android.settings.encfs.NumKDFIterationsPropertyEditor;
import com.igeltech.nevercrypt.android.settings.encfs.RandBytesPerBlockPropertyEditor;
import com.igeltech.nevercrypt.android.settings.encfs.UniqueIVPropertyEditor;
import com.igeltech.nevercrypt.container.Container;
import com.igeltech.nevercrypt.container.ContainerFormatInfo;
import com.igeltech.nevercrypt.container.LocationFormatter;
import com.igeltech.nevercrypt.container.VolumeLayout;
import com.igeltech.nevercrypt.crypto.SecureBuffer;
import com.igeltech.nevercrypt.locations.Openable;

public abstract class CreateContainerFragmentBase extends CreateLocationFragment implements PasswordDialogBase.PasswordReceiver
{
    public void changeUniqueIVDependentOptions()
    {
        boolean show = isEncFsFormat() && !_state.getBoolean(ARG_ADD_EXISTING_LOCATION, false) && _state.getBoolean(CreateEncFsTaskFragment.ARG_UNIQUE_IV, true) && _state.getBoolean(CreateEncFsTaskFragment.ARG_CHAINED_NAME_IV, true);
        _propertiesView.setPropertyState(R.string.enable_filename_to_file_iv_chain, show);
    }

    public boolean isEncFsFormat()
    {
        return LocationFormatter.FORMAT_ENCFS.equals(_state.getString(CreateContainerTaskFragmentBase.ARG_CONTAINER_FORMAT));
    }

    public VolumeLayout getSelectedVolumeLayout()
    {
        ContainerFormatInfo info = getCurrentContainerFormatInfo();
        return info == null ? null : info.getVolumeLayout();
    }

    @Override
    public void onCreate(Bundle state) {
        Bundle args = getArguments();
        if (args != null)
            _state.putAll(args);
        super.onCreate(state);
    }

    @Override
    public void onDestroy() {
        SecureBuffer sb = _state.getParcelable(CreateContainerTaskFragmentBase.ARG_HIDDEN_PASSWORD);
        if (sb != null)
        {
            sb.close();
            _state.remove(CreateContainerTaskFragmentBase.ARG_HIDDEN_PASSWORD);
        }
        super.onDestroy();
    }

    /**
     * Moves the current state to the next wizard fragment without letting this fragment close shared secrets.
     */
    protected Bundle transferStateToNextFragment()
    {
        Bundle res = new Bundle(_state);
        _state.remove(Openable.PARAM_PASSWORD);
        _state.remove(CreateContainerTaskFragmentBase.ARG_HIDDEN_PASSWORD);
        return res;
    }

    /**
     * Returns the volume layout used to populate hidden-volume-only option lists.
     */
    public VolumeLayout getSelectedHiddenVolumeLayout()
    {
        ContainerFormatInfo info = getCurrentContainerFormatInfo();
        return info == null || !info.hasHiddenContainerSupport() ? null : info.getHiddenVolumeLayout();
    }

    /**
     * Lets concrete fragments update controls that depend on hidden-volume support.
     */
    public void changeHiddenVolumeDependentOptions()
    {
    }

    @Override
    protected TaskFragment createAddExistingLocationTask()
    {
        return AddExistingContainerTaskFragment.newInstance(_state.getParcelable(CreateContainerTaskFragmentBase.ARG_LOCATION), !UserSettings.getSettings(getActivity()).neverSaveHistory(), _state.getString(CreateContainerTaskFragmentBase.ARG_CONTAINER_FORMAT));
    }

    @Override
    protected TaskFragment createCreateLocationTask()
    {
        return isEncFsFormat() ? new CreateEncFsTaskFragment() : new CreateContainerTaskFragment();
    }

    @Override
    public void showCreateNewLocationProperties()
    {
        super.showCreateNewLocationProperties();
        _propertiesView.setPropertyState(R.string.container_format, true);
        changeHiddenVolumeDependentOptions();
    }

    @Override
    public void onPasswordEntered(PasswordDialog dlg)
    {
        int propertyId = dlg.getArguments().getInt(PropertyEditor.ARG_PROPERTY_ID);
        PasswordDialogBase.PasswordReceiver pr = (PasswordDialogBase.PasswordReceiver) getPropertiesView().getPropertyById(propertyId);
        if (pr != null)
            pr.onPasswordEntered(dlg);
    }

    @Override
    public void onPasswordNotEntered(PasswordDialog dlg)
    {
        int propertyId = dlg.getArguments().getInt(PropertyEditor.ARG_PROPERTY_ID);
        PasswordDialogBase.PasswordReceiver pr = (PasswordDialogBase.PasswordReceiver) getPropertiesView().getPropertyById(propertyId);
        if (pr != null)
            pr.onPasswordNotEntered(dlg);
    }

    @Override
    protected void createProperties()
    {
        super.createProperties();
        if (!_state.containsKey(CreateContainerTaskFragmentBase.ARG_CONTAINER_FORMAT))
            _state.putString(CreateContainerTaskFragmentBase.ARG_CONTAINER_FORMAT, Container.getSupportedFormats().get(1).getFormatName());
    }

    @Override
    protected void createNewLocationProperties()
    {
        _propertiesView.addProperty(new ContainerFormatPropertyEditor(this));
        _propertiesView.addProperty(new PathToContainerPropertyEditor(this));
        _propertiesView.addProperty(new ContainerPasswordPropertyEditor(this));
        createContainerProperties();
        createEncFsProperties();
    }

    protected ContainerFormatInfo getCurrentContainerFormatInfo()
    {
        return Container.findFormatByName(_state.getString(CreateContainerTaskFragmentBase.ARG_CONTAINER_FORMAT));
    }

    protected void createContainerProperties()
    {
        _propertiesView.addProperty(new PIMPropertyEditor(this));
        _propertiesView.addProperty(new ContainerSizePropertyEditor(this));
        _propertiesView.addProperty(new EncryptionAlgorithmPropertyEditor(this));
        _propertiesView.addProperty(new HashingAlgorithmPropertyEditor(this));
        _propertiesView.addProperty(new FileSystemTypePropertyEditor(this));
        _propertiesView.addProperty(new FillFreeSpacePropertyEditor(this));
    }

    private void createEncFsProperties()
    {
        _propertiesView.addProperty(new DataCodecPropertyEditor(this));
        _propertiesView.addProperty(new NameCodecPropertyEditor(this));
        _propertiesView.addProperty(new KeySizePropertyEditor(this));
        _propertiesView.addProperty(new BlockSizePropertyEditor(this));
        _propertiesView.addProperty(new UniqueIVPropertyEditor(this));
        _propertiesView.addProperty(new FilenameIVChainingPropertyEditor(this));
        _propertiesView.addProperty(new ExternalFileIVPropertyEditor(this));
        _propertiesView.addProperty(new EnableEmptyBlocksPropertyEditor(this));
        _propertiesView.addProperty(new MACBytesPerBlockPropertyEditor(this));
        _propertiesView.addProperty(new RandBytesPerBlockPropertyEditor(this));
        _propertiesView.addProperty(new NumKDFIterationsPropertyEditor(this));
    }
}
