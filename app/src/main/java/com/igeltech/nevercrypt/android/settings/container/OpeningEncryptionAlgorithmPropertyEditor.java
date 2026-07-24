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
    public OpeningEncryptionAlgorithmPropertyEditor(OpeningOptionsFragmentBase hostFragment)
    {
        super(hostFragment, R.string.encryption_algorithm, R.string.encryption_alg_desc, true);
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
        for (ContainerFormatInfo cfi : getHostFragment().getOpeningContainerFormats())
        {
            VolumeLayout layout = cfi.getVolumeLayout();
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
        return getHostFragment().getState().containsKey(Openable.PARAM_CIPHER_NAME) || getHostFragment().getState().containsKey(Openable.PARAM_CIPHER_MODE_NAME);
    }

    @Override
    protected int findSavedAlgorithmIndex(List<? extends EncryptionEngine> algs)
    {
        return findEngineIndexByCipherAndMode(algs, getHostFragment().getState().getString(Openable.PARAM_CIPHER_NAME), getHostFragment().getState().getString(Openable.PARAM_CIPHER_MODE_NAME));
    }

    @Override
    protected void saveAlgorithmValue(EncryptionEngine engine)
    {
        getHostFragment().getState().putString(Openable.PARAM_CIPHER_NAME, engine.getCipherName());
        getHostFragment().getState().putString(Openable.PARAM_CIPHER_MODE_NAME, engine.getCipherModeName());
    }

    @Override
    protected void saveAutoDetectValue()
    {
        // Store explicit empty values so the password dialog can clear previous defaults.
        getHostFragment().getState().putString(Openable.PARAM_CIPHER_NAME, "");
        getHostFragment().getState().putString(Openable.PARAM_CIPHER_MODE_NAME, "");
    }
}
