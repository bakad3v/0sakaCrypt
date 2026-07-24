package com.igeltech.nevercrypt.android.settings.container;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.locations.fragments.ContainerSettingsFragment;
import com.igeltech.nevercrypt.android.locations.fragments.LocationSettingsFragment;
import com.igeltech.nevercrypt.container.ContainerFormatInfo;
import com.igeltech.nevercrypt.container.VolumeLayoutBase;
import com.igeltech.nevercrypt.crypto.EncryptionEngine;

import java.util.Collections;
import java.util.List;

public class EncEngineHintPropertyEditor extends EncryptionAlgorithmPropertyEditorBase
{
    public EncEngineHintPropertyEditor(LocationSettingsFragment containerSettingsFragment)
    {
        super(containerSettingsFragment, R.string.encryption_algorithm, R.string.encryption_alg_desc, true);
    }

    @Override
    public ContainerSettingsFragment getHost()
    {
        return (ContainerSettingsFragment) super.getHost();
    }

    @Override
    protected List<? extends EncryptionEngine> getCurrentEncAlgList()
    {
        ContainerFormatInfo cfi = getHost().getCurrentContainerFormat();
        return cfi != null ? cfi.getVolumeLayout().getSupportedEncryptionEngines() : Collections.emptyList();
    }

    @Override
    protected boolean hasSavedAlgorithmSelection()
    {
        String name = getHost().getLocation().getExternalSettings().getEncEngineName();
        return name != null && !name.isEmpty();
    }

    @Override
    protected int findSavedAlgorithmIndex(List<? extends EncryptionEngine> algs)
    {
        return findEngineIndexBySavedName(algs, getHost().getLocation().getExternalSettings().getEncEngineName());
    }

    @Override
    protected void saveAlgorithmValue(EncryptionEngine engine)
    {
        getHost().getLocation().getExternalSettings().setEncEngineName(getSavedEncEngineName(engine));
        getHost().saveExternalSettings();
    }

    @Override
    protected void saveAutoDetectValue()
    {
        getHost().getLocation().getExternalSettings().setEncEngineName(null);
        getHost().saveExternalSettings();
    }

    @Override
    protected String getEncEngineEntryName(EncryptionEngine eng)
    {
        return VolumeLayoutBase.getEncEngineName(eng);
    }

    @Override
    protected String getSavedEncEngineName(EncryptionEngine eng)
    {
        return VolumeLayoutBase.getEncEngineName(eng);
    }
}
