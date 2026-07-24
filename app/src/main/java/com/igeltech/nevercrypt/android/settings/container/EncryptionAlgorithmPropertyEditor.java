package com.igeltech.nevercrypt.android.settings.container;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.locations.fragments.CreateContainerFragmentBase;
import com.igeltech.nevercrypt.android.locations.tasks.CreateContainerTaskFragmentBase;
import com.igeltech.nevercrypt.android.locations.tasks.CreateLocationTaskFragmentBase;
import com.igeltech.nevercrypt.container.VolumeLayout;
import com.igeltech.nevercrypt.crypto.EncryptionEngine;
import com.igeltech.nevercrypt.crypto.FileEncryptionEngine;
import com.igeltech.nevercrypt.locations.Openable;

import java.util.Collections;
import java.util.List;

public class EncryptionAlgorithmPropertyEditor extends EncryptionAlgorithmPropertyEditorBase
{
    private final String _cipherNameKey;
    private final String _cipherModeNameKey;
    private final boolean _hiddenVolume;

    /**
     * Creates the default encryption algorithm editor for the outer volume.
     */
    public EncryptionAlgorithmPropertyEditor(CreateContainerFragmentBase createContainerFragment)
    {
        this(createContainerFragment, R.string.encryption_algorithm, Openable.PARAM_CIPHER_NAME, Openable.PARAM_CIPHER_MODE_NAME, false);
    }

    /**
     * Creates an encryption algorithm editor bound to either outer or hidden volume state keys.
     */
    public EncryptionAlgorithmPropertyEditor(CreateContainerFragmentBase createContainerFragment, int titleResId, String cipherNameKey, String cipherModeNameKey, boolean hiddenVolume)
    {
        super(createContainerFragment, titleResId, 0, false);
        _cipherNameKey = cipherNameKey;
        _cipherModeNameKey = cipherModeNameKey;
        _hiddenVolume = hiddenVolume;
    }

    protected CreateContainerFragmentBase getHostFragment()
    {
        return (CreateContainerFragmentBase) getHost();
    }

    /**
     * Returns the encryption algorithms supported by the currently selected outer or hidden layout.
     */
    @Override
    protected List<? extends EncryptionEngine> getCurrentEncAlgList()
    {
        VolumeLayout vl = _hiddenVolume ? getHostFragment().getSelectedHiddenVolumeLayout() : getHostFragment().getSelectedVolumeLayout();
        return vl != null ? vl.getSupportedEncryptionEngines() : Collections.<FileEncryptionEngine>emptyList();
    }

    @Override
    protected boolean hasSavedAlgorithmSelection()
    {
        return getHostFragment().getState().containsKey(_cipherNameKey) || getHostFragment().getState().containsKey(_cipherModeNameKey);
    }

    @Override
    protected int findSavedAlgorithmIndex(List<? extends EncryptionEngine> algs)
    {
        return findEngineIndexByCipherAndMode(algs, getHostFragment().getState().getString(_cipherNameKey), getHostFragment().getState().getString(_cipherModeNameKey));
    }

    @Override
    protected void saveAlgorithmValue(EncryptionEngine engine)
    {
        getHostFragment().getState().putString(_cipherNameKey, engine.getCipherName());
        getHostFragment().getState().putString(_cipherModeNameKey, engine.getCipherModeName());
    }

    @Override
    protected void saveAutoDetectValue()
    {
    }
}
