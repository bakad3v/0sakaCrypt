package com.igeltech.nevercrypt.android.settings.container;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.settings.fragments.OpeningOptionsFragmentBase;
import com.igeltech.nevercrypt.container.ContainerFormatInfo;
import com.igeltech.nevercrypt.container.VolumeLayout;
import com.igeltech.nevercrypt.container.VolumeLayoutBase;
import com.igeltech.nevercrypt.crypto.EncryptionEngine;
import com.igeltech.nevercrypt.crypto.FileEncryptionEngine;
import com.igeltech.nevercrypt.locations.Openable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Lets the user select a one-shot encryption algorithm hint before opening a container.
 */
public class OpeningEncryptionAlgorithmPropertyEditor extends EncryptionAlgorithmPropertyEditorBase
{
    private final String _cipherNameKey;
    private final String _cipherModeNameKey;
    private final boolean _hiddenVolume;

    public OpeningEncryptionAlgorithmPropertyEditor(OpeningOptionsFragmentBase hostFragment)
    {
        this(hostFragment, R.string.encryption_algorithm, Openable.PARAM_CIPHER_NAME, Openable.PARAM_CIPHER_MODE_NAME, false);
    }

    /**
     * Creates a one-shot opening encryption hint editor for either the outer or hidden header.
     */
    public OpeningEncryptionAlgorithmPropertyEditor(OpeningOptionsFragmentBase hostFragment, int titleResId, String cipherNameKey, String cipherModeNameKey, boolean hiddenVolume)
    {
        super(hostFragment, titleResId, R.string.encryption_alg_desc, true);
        _cipherNameKey = cipherNameKey;
        _cipherModeNameKey = cipherModeNameKey;
        _hiddenVolume = hiddenVolume;
    }

    protected OpeningOptionsFragmentBase getHostFragment()
    {
        return (OpeningOptionsFragmentBase) getHost();
    }

    /**
     * Builds a stable, de-duplicated list of engines supported by candidate formats.
     */
    @Override
    protected List<? extends EncryptionEngine> getCurrentEncAlgList()
    {
        LinkedHashMap<String, FileEncryptionEngine> engines = new LinkedHashMap<>();
        List<ContainerFormatInfo> formats = _hiddenVolume ? getHostFragment().getOpeningHiddenContainerFormats() : getHostFragment().getOpeningContainerFormats();
        for (ContainerFormatInfo cfi : formats)
        {
            VolumeLayout layout = _hiddenVolume ? cfi.getHiddenVolumeLayout() : cfi.getVolumeLayout();
            if (layout == null)
                continue;
            for (FileEncryptionEngine engine : layout.getSupportedEncryptionEngines())
            {
                String key = VolumeLayoutBase.getEncEngineName(engine).toLowerCase(Locale.US);
                if (!engines.containsKey(key))
                    engines.put(key, engine);
            }
        }
        return new ArrayList<>(engines.values());
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
        // Store explicit empty values so the password dialog can clear previous defaults.
        getHostFragment().getState().putString(_cipherNameKey, "");
        getHostFragment().getState().putString(_cipherModeNameKey, "");
    }
}
