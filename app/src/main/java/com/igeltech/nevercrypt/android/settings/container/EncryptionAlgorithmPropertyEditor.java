package com.igeltech.nevercrypt.android.settings.container;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.locations.fragments.CreateContainerFragmentBase;
import com.igeltech.nevercrypt.android.locations.tasks.CreateContainerTaskFragmentBase;
import com.igeltech.nevercrypt.android.locations.tasks.CreateLocationTaskFragmentBase;
import com.igeltech.nevercrypt.android.settings.ChoiceDialogPropertyEditor;
import com.igeltech.nevercrypt.container.VolumeLayout;
import com.igeltech.nevercrypt.container.VolumeLayoutBase;
import com.igeltech.nevercrypt.crypto.EncryptionEngine;
import com.igeltech.nevercrypt.crypto.FileEncryptionEngine;
import com.igeltech.nevercrypt.truecrypt.EncryptionEnginesRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EncryptionAlgorithmPropertyEditor extends ChoiceDialogPropertyEditor
{
    private final String _cipherNameKey;
    private final String _cipherModeNameKey;
    private final boolean _hiddenVolume;

    /**
     * Creates the default encryption algorithm editor for the outer volume.
     */
    public EncryptionAlgorithmPropertyEditor(CreateContainerFragmentBase createContainerFragment)
    {
        this(createContainerFragment, R.string.encryption_algorithm, CreateLocationTaskFragmentBase.ARG_CIPHER_NAME, CreateContainerTaskFragmentBase.ARG_CIPHER_MODE_NAME, false);
    }

    /**
     * Creates an encryption algorithm editor bound to either outer or hidden volume state keys.
     */
    public EncryptionAlgorithmPropertyEditor(CreateContainerFragmentBase createContainerFragment, int titleResId, String cipherNameKey, String cipherModeNameKey, boolean hiddenVolume)
    {
        super(createContainerFragment, titleResId, 0, createContainerFragment.getTag());
        _cipherNameKey = cipherNameKey;
        _cipherModeNameKey = cipherModeNameKey;
        _hiddenVolume = hiddenVolume;
    }

    public static String getEncEngineName(EncryptionEngine eng)
    {
        return EncryptionEnginesRegistry.getEncEngineName(eng);
    }

    @Override
    protected int loadValue()
    {
        List<? extends EncryptionEngine> algs = getCurrentEncAlgList();
        String encAlgName = getHostFragment().getState().getString(_cipherNameKey);
        String encModeName = getHostFragment().getState().getString(_cipherModeNameKey);
        if (encAlgName != null && encModeName != null)
        {
            EncryptionEngine ee = VolumeLayoutBase.findCipher(algs, encAlgName, encModeName);
            return algs.indexOf(ee);
        }
        else if (!algs.isEmpty())
            return 0;
        else
            return -1;
    }

    @Override
    protected void saveValue(int value)
    {
        List<? extends EncryptionEngine> algs = getCurrentEncAlgList();
        EncryptionEngine ee = algs.get(value);
        getHostFragment().getState().putString(_cipherNameKey, ee.getCipherName());
        getHostFragment().getState().putString(_cipherModeNameKey, ee.getCipherModeName());
    }

    @Override
    protected ArrayList<String> getEntries()
    {
        ArrayList<String> res = new ArrayList<>();
        List<? extends EncryptionEngine> supportedEngines = getCurrentEncAlgList();
        if (supportedEngines != null)
        {
            for (EncryptionEngine eng : supportedEngines)
                res.add(getEncEngineName(eng));
        }
        return res;
    }

    protected CreateContainerFragmentBase getHostFragment()
    {
        return (CreateContainerFragmentBase) getHost();
    }

    /**
     * Returns the encryption algorithms supported by the currently selected outer or hidden layout.
     */
    private List<? extends EncryptionEngine> getCurrentEncAlgList()
    {
        VolumeLayout vl = _hiddenVolume ? getHostFragment().getSelectedHiddenVolumeLayout() : getHostFragment().getSelectedVolumeLayout();
        return vl != null ? vl.getSupportedEncryptionEngines() : Collections.<FileEncryptionEngine>emptyList();
    }
}
