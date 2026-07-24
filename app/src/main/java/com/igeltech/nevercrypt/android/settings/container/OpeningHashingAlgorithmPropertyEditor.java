package com.igeltech.nevercrypt.android.settings.container;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.settings.fragments.OpeningOptionsFragmentBase;
import com.igeltech.nevercrypt.container.ContainerFormatInfo;
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
    public OpeningHashingAlgorithmPropertyEditor(OpeningOptionsFragmentBase hostFragment)
    {
        super(hostFragment, R.string.hash_algorithm, R.string.hash_alg_desc, true);
    }

    protected OpeningOptionsFragmentBase getHostFragment()
    {
        return (OpeningOptionsFragmentBase) getHost();
    }

    /**
     * Builds a stable, de-duplicated list of KDF/hash functions supported by candidate formats.
     */
    @Override
    protected List<MessageDigest> getCurrentHashAlgList()
    {
        LinkedHashMap<String, MessageDigest> hashFuncs = new LinkedHashMap<>();
        for (ContainerFormatInfo cfi : getHostFragment().getOpeningContainerFormats())
        {
            for (MessageDigest hashFunc : cfi.getVolumeLayout().getSupportedHashFuncs())
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
        return getHostFragment().getState().containsKey(Openable.PARAM_HASHING_ALG);
    }

    @Override
    protected int findSavedHashFuncIndex(List<MessageDigest> algs)
    {
        return findHashFuncIndexByName(algs, getHostFragment().getState().getString(Openable.PARAM_HASHING_ALG));
    }

    @Override
    protected void saveHashFuncValue(MessageDigest hashFunc)
    {
        getHostFragment().getState().putString(Openable.PARAM_HASHING_ALG, hashFunc.getAlgorithm());
    }

    @Override
    protected void saveAutoDetectValue()
    {
        // Store an explicit empty value so the password dialog can clear previous defaults.
        getHostFragment().getState().putString(Openable.PARAM_HASHING_ALG, "");
    }
}
