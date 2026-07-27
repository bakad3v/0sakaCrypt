package com.igeltech.nevercrypt.android.settings.container;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.locations.fragments.CreateContainerFragmentBase;
import com.igeltech.nevercrypt.android.locations.tasks.CreateContainerTaskFragmentBase;
import com.igeltech.nevercrypt.container.VolumeLayout;
import com.igeltech.nevercrypt.locations.Openable;

import java.security.MessageDigest;
import java.util.List;

public class HashingAlgorithmPropertyEditor extends HashingAlgorithmPropertyEditorBase
{
    private final String _hashAlgKey;
    private final boolean _hiddenVolume;

    /**
     * Creates the default password hash editor for the outer volume.
     */
    public HashingAlgorithmPropertyEditor(CreateContainerFragmentBase createContainerFragment)
    {
        this(createContainerFragment, R.string.hash_algorithm, Openable.PARAM_HASHING_ALG, false);
    }

    /**
     * Creates a password hash editor bound to either outer or hidden volume state keys.
     */
    public HashingAlgorithmPropertyEditor(CreateContainerFragmentBase createContainerFragment, int titleResId, String hashAlgKey, boolean hiddenVolume)
    {
        super(createContainerFragment, titleResId, 0, false);
        _hashAlgKey = hashAlgKey;
        _hiddenVolume = hiddenVolume;
    }

    protected CreateContainerFragmentBase getHostFragment()
    {
        return (CreateContainerFragmentBase) getHost();
    }

    /**
     * Returns the hash algorithms supported by the currently selected outer or hidden layout.
     */
    @Override
    protected List<MessageDigest> getCurrentHashAlgList()
    {
        VolumeLayout vl = _hiddenVolume ? getHostFragment().getSelectedHiddenVolumeLayout() : getHostFragment().getSelectedVolumeLayout();
        return vl != null ? vl.getSupportedHashFuncs() : null;
    }

    @Override
    protected boolean hasSavedHashFuncSelection()
    {
        return getHostFragment().getState().containsKey(_hashAlgKey);
    }

    @Override
    protected int findSavedHashFuncIndex(List<MessageDigest> algs)
    {
        return findHashFuncIndexByName(algs, getHostFragment().getState().getString(_hashAlgKey));
    }

    @Override
    protected void saveHashFuncValue(MessageDigest hashFunc)
    {
        getHostFragment().getState().putString(_hashAlgKey, hashFunc.getAlgorithm());
    }

    @Override
    protected void saveAutoDetectValue()
    {
    }
}
