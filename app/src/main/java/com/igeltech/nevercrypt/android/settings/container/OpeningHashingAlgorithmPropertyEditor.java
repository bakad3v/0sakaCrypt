package com.igeltech.nevercrypt.android.settings.container;

import android.view.View;
import android.view.ViewGroup;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.settings.fragments.OpeningOptionsFragmentBase;
import com.igeltech.nevercrypt.container.ContainerFormatInfo;
import com.igeltech.nevercrypt.container.VolumeLayout;
import com.igeltech.nevercrypt.locations.Openable;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Lets the user select a one-shot KDF/base hash hint before opening a container.
 */
public class OpeningHashingAlgorithmPropertyEditor extends HashingAlgorithmPropertyEditorBase
{
    private final String _hashAlgKey;
    private final boolean _hiddenVolume;

    public OpeningHashingAlgorithmPropertyEditor(OpeningOptionsFragmentBase hostFragment)
    {
        this(hostFragment, R.string.hash_algorithm, Openable.PARAM_HASHING_ALG, false);
    }

    /**
     * Creates a one-shot opening hash hint editor for either the outer or hidden header.
     */
    public OpeningHashingAlgorithmPropertyEditor(OpeningOptionsFragmentBase hostFragment, int titleResId, String hashAlgKey, boolean hiddenVolume)
    {
        super(hostFragment, titleResId, R.string.hash_alg_desc, true);
        _hashAlgKey = hashAlgKey;
        _hiddenVolume = hiddenVolume;
    }

    protected OpeningOptionsFragmentBase getHostFragment()
    {
        return (OpeningOptionsFragmentBase) getHost();
    }

    /**
     * Initializes dynamically shown hidden-volume rows immediately; this editor has no load side effects.
     */
    @Override
    public View createView(ViewGroup parent)
    {
        View view = super.createView(parent);
        load();
        return view;
    }

    /**
     * Builds a stable, de-duplicated list of KDF/hash functions supported by candidate formats.
     */
    @Override
    protected List<MessageDigest> getCurrentHashAlgList()
    {
        LinkedHashMap<String, MessageDigest> hashFuncs = new LinkedHashMap<>();
        List<ContainerFormatInfo> formats = _hiddenVolume ? getHostFragment().getOpeningHiddenContainerFormats() : getHostFragment().getOpeningContainerFormats();
        for (ContainerFormatInfo cfi : formats)
        {
            VolumeLayout layout = _hiddenVolume ? cfi.getHiddenVolumeLayout() : cfi.getVolumeLayout();
            if (layout == null)
                continue;
            for (MessageDigest hashFunc : layout.getSupportedHashFuncs())
            {
                String key = hashFunc.getAlgorithm().toLowerCase(Locale.US);
                if (!hashFuncs.containsKey(key))
                    hashFuncs.put(key, hashFunc);
            }
        }
        return new ArrayList<>(hashFuncs.values());
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
        // Store an explicit empty value so the password dialog can clear previous defaults.
        getHostFragment().getState().putString(_hashAlgKey, "");
    }
}
