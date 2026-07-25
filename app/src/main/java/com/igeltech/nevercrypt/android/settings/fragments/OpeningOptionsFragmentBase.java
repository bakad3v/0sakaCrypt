package com.igeltech.nevercrypt.android.settings.fragments;

import android.os.Bundle;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.dialogs.PasswordDialog;
import com.igeltech.nevercrypt.android.fragments.PropertiesFragmentBase;
import com.igeltech.nevercrypt.android.settings.PropertyEditor;
import com.igeltech.nevercrypt.android.settings.PropertiesHostWithLocation;
import com.igeltech.nevercrypt.android.settings.PropertiesHostWithStateBundle;
import com.igeltech.nevercrypt.android.settings.UserSettings;
import com.igeltech.nevercrypt.android.settings.container.HiddenVolumePasswordPropertyEditor;
import com.igeltech.nevercrypt.android.settings.container.OpeningEncryptionAlgorithmPropertyEditor;
import com.igeltech.nevercrypt.android.settings.container.OpeningHashingAlgorithmPropertyEditor;
import com.igeltech.nevercrypt.android.settings.container.OpenInReadOnlyModePropertyEditor;
import com.igeltech.nevercrypt.android.settings.container.PIMPropertyEditor;
import com.igeltech.nevercrypt.android.settings.container.ProtectHiddenVolumePropertyEditor;
import com.igeltech.nevercrypt.android.settings.container.UseExternalFileManagerPropertyEditor;
import com.igeltech.nevercrypt.container.Container;
import com.igeltech.nevercrypt.container.ContainerFormatInfo;
import com.igeltech.nevercrypt.locations.ContainerLocation;
import com.igeltech.nevercrypt.locations.CryptoLocation;
import com.igeltech.nevercrypt.locations.Location;
import com.igeltech.nevercrypt.locations.LocationsManager;
import com.igeltech.nevercrypt.locations.Openable;
import com.igeltech.nevercrypt.settings.Settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class OpeningOptionsFragmentBase extends PropertiesFragmentBase implements PropertiesHostWithStateBundle, PropertiesHostWithLocation, PasswordDialog.PasswordReceiver
{
    private final Bundle _state = new Bundle();
    protected Openable _location;
    protected Settings _settings;
    // Stored so the hidden password editor can be enabled only when protection is checked.
    private int _hiddenVolumePasswordPropertyId, _hiddenVolumePIMPropertyId, _hiddenVolumeEncryptionPropertyId, _hiddenVolumeHashPropertyId;

    public void saveExternalSettings()
    {
        _location.saveExternalSettings();
    }

    @Override
    public Bundle getState()
    {
        return _state;
    }

    @Override
    public Location getTargetLocation()
    {
        return _location;
    }

    @Override
    protected void createProperties()
    {
        _location = (Openable) LocationsManager.
                getLocationsManager(getActivity()).
                getFromIntent(getActivity().getIntent(), null);
        if (_location == null)
        {
            getActivity().finish();
            return;
        }
        _settings = UserSettings.getSettings(getActivity());
        _propertiesView.setInstantSave(true);
        Bundle extras = getActivity().getIntent().getExtras();
        if (extras != null)
            _state.putAll(extras);
        createOpenableProperties();
        if (_location instanceof CryptoLocation)
            createLocationProperties();
        if (_location instanceof ContainerLocation)
            createContainerProperties();
    }

    protected void createLocationProperties()
    {
        _propertiesView.addProperty(new OpenInReadOnlyModePropertyEditor(this));
    }

    protected void createOpenableProperties()
    {
        int id = _propertiesView.addProperty(new PIMPropertyEditor(this));
        if (!_location.hasCustomKDFIterations())
            _propertiesView.setPropertyState(id, false);
        id = _propertiesView.addProperty(new UseExternalFileManagerPropertyEditor(this));
        if (_settings.getExternalFileManagerInfo() == null)
            _propertiesView.setPropertyState(id, false);
    }

    protected void createContainerProperties()
    {
        // These options are stored in the activity state and apply only to the current opening attempt.
        _propertiesView.addProperty(new OpeningEncryptionAlgorithmPropertyEditor(this));
        _propertiesView.addProperty(new OpeningHashingAlgorithmPropertyEditor(this));
        if (hasHiddenVolumeProtectionSupport())
        {
            // Show protection only for formats whose layout can contain a hidden volume.
            _propertiesView.addProperty(new ProtectHiddenVolumePropertyEditor(this));
            _hiddenVolumePasswordPropertyId = _propertiesView.addProperty(new HiddenVolumePasswordPropertyEditor(this));
            if (hasHiddenVolumeCustomKDFIterationsSupport())
                _hiddenVolumePIMPropertyId = _propertiesView.addProperty(new PIMPropertyEditor(this, R.string.hidden_volume_kdf_iterations_multiplier, Openable.PARAM_HIDDEN_VOLUME_KDF_ITERATIONS));
            _hiddenVolumeEncryptionPropertyId = _propertiesView.addProperty(new OpeningEncryptionAlgorithmPropertyEditor(
                    this,
                    R.string.hidden_volume_encryption_algorithm,
                    Openable.PARAM_HIDDEN_VOLUME_CIPHER_NAME,
                    Openable.PARAM_HIDDEN_VOLUME_CIPHER_MODE_NAME,
                    true));
            _hiddenVolumeHashPropertyId = _propertiesView.addProperty(new OpeningHashingAlgorithmPropertyEditor(
                    this,
                    R.string.hidden_volume_hash_algorithm,
                    Openable.PARAM_HIDDEN_VOLUME_HASHING_ALG,
                    true));
            updateHiddenVolumeProtectionProperties();
        }
    }

    /**
     * Keeps hidden-volume probe fields disabled until the user opts into protection.
     */
    public void updateHiddenVolumeProtectionProperties()
    {
        boolean enabled = _state.getBoolean(Openable.PARAM_PROTECT_HIDDEN_VOLUME, false);
        if (_hiddenVolumePasswordPropertyId != 0)
            _propertiesView.setPropertyState(_hiddenVolumePasswordPropertyId, enabled);
        if (_hiddenVolumePIMPropertyId != 0)
            _propertiesView.setPropertyState(_hiddenVolumePIMPropertyId, enabled);
        if (_hiddenVolumeEncryptionPropertyId != 0)
            _propertiesView.setPropertyState(_hiddenVolumeEncryptionPropertyId, enabled);
        if (_hiddenVolumeHashPropertyId != 0)
            _propertiesView.setPropertyState(_hiddenVolumeHashPropertyId, enabled);
    }

    @Override
    public void onPasswordEntered(PasswordDialog dlg)
    {
        // The fragment receives dialog callbacks, but the property editor owns the password state.
        int propertyId = dlg.getArguments().getInt(PropertyEditor.ARG_PROPERTY_ID);
        PasswordDialog.PasswordReceiver receiver = (PasswordDialog.PasswordReceiver) getPropertiesView().getPropertyById(propertyId);
        if (receiver != null)
            receiver.onPasswordEntered(dlg);
    }

    @Override
    public void onPasswordNotEntered(PasswordDialog dlg)
    {
        // Mirror successful callback routing so cancellation reaches the editor that opened the dialog.
        int propertyId = dlg.getArguments().getInt(PropertyEditor.ARG_PROPERTY_ID);
        PasswordDialog.PasswordReceiver receiver = (PasswordDialog.PasswordReceiver) getPropertiesView().getPropertyById(propertyId);
        if (receiver != null)
            receiver.onPasswordNotEntered(dlg);
    }

    /**
     * Returns the formats whose algorithms should be offered on the one-shot opening options screen.
     */
    public List<ContainerFormatInfo> getOpeningContainerFormats()
    {
        if (!(_location instanceof ContainerLocation))
            return Collections.emptyList();
        ContainerLocation location = (ContainerLocation) _location;
        ContainerFormatInfo cfi = getCurrentContainerFormat(location);
        return cfi != null ? Collections.singletonList(cfi) : location.getSupportedFormats();
    }

    /**
     * Returns only hidden-capable formats for hidden-header probe hints.
     */
    public List<ContainerFormatInfo> getOpeningHiddenContainerFormats()
    {
        List<ContainerFormatInfo> formats = getOpeningContainerFormats();
        if (formats.isEmpty())
            return formats;
        ArrayList<ContainerFormatInfo> res = new ArrayList<>();
        for (ContainerFormatInfo cfi : formats)
            if (cfi.hasHiddenContainerSupport())
                res.add(cfi);
        return res;
    }

    /**
     * Uses the saved container format hint to narrow algorithm choices when possible.
     */
    private ContainerFormatInfo getCurrentContainerFormat(ContainerLocation location)
    {
        List<ContainerFormatInfo> supportedFormats = location.getSupportedFormats();
        if (supportedFormats.size() == 1)
            return supportedFormats.get(0);
        return Container.findFormatByName(supportedFormats, location.getExternalSettings().getContainerFormatName());
    }

    /**
     * Checks whether any format available for this opener can contain a hidden volume.
     */
    private boolean hasHiddenVolumeProtectionSupport()
    {
        // The option is format-dependent and should not appear for layouts without hidden containers.
        for (ContainerFormatInfo cfi : getOpeningContainerFormats())
            if (cfi.hasHiddenContainerSupport())
                return true;
        return false;
    }

    /**
     * Shows hidden PIM only when at least one hidden-capable candidate format supports custom KDF iterations.
     */
    private boolean hasHiddenVolumeCustomKDFIterationsSupport()
    {
        for (ContainerFormatInfo cfi : getOpeningHiddenContainerFormats())
            if (cfi.hasCustomKDFIterationsSupport())
                return true;
        return false;
    }
}
