package com.igeltech.nevercrypt.android.settings.fragments;

import android.os.Bundle;

import com.igeltech.nevercrypt.android.fragments.PropertiesFragmentBase;
import com.igeltech.nevercrypt.android.settings.PropertiesHostWithLocation;
import com.igeltech.nevercrypt.android.settings.PropertiesHostWithStateBundle;
import com.igeltech.nevercrypt.android.settings.UserSettings;
import com.igeltech.nevercrypt.android.settings.container.OpeningEncryptionAlgorithmPropertyEditor;
import com.igeltech.nevercrypt.android.settings.container.OpeningHashingAlgorithmPropertyEditor;
import com.igeltech.nevercrypt.android.settings.container.OpenInReadOnlyModePropertyEditor;
import com.igeltech.nevercrypt.android.settings.container.PIMPropertyEditor;
import com.igeltech.nevercrypt.android.settings.container.UseExternalFileManagerPropertyEditor;
import com.igeltech.nevercrypt.container.Container;
import com.igeltech.nevercrypt.container.ContainerFormatInfo;
import com.igeltech.nevercrypt.locations.ContainerLocation;
import com.igeltech.nevercrypt.locations.CryptoLocation;
import com.igeltech.nevercrypt.locations.Location;
import com.igeltech.nevercrypt.locations.LocationsManager;
import com.igeltech.nevercrypt.locations.Openable;
import com.igeltech.nevercrypt.settings.Settings;

import java.util.Collections;
import java.util.List;

public abstract class OpeningOptionsFragmentBase extends PropertiesFragmentBase implements PropertiesHostWithStateBundle, PropertiesHostWithLocation
{
    private final Bundle _state = new Bundle();
    protected Openable _location;
    protected Settings _settings;

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
     * Uses the saved container format hint to narrow algorithm choices when possible.
     */
    private ContainerFormatInfo getCurrentContainerFormat(ContainerLocation location)
    {
        List<ContainerFormatInfo> supportedFormats = location.getSupportedFormats();
        if (supportedFormats.size() == 1)
            return supportedFormats.get(0);
        return Container.findFormatByName(supportedFormats, location.getExternalSettings().getContainerFormatName());
    }
}
