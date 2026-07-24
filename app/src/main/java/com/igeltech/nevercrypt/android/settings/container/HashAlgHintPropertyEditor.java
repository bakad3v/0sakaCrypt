package com.igeltech.nevercrypt.android.settings.container;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.locations.fragments.ContainerSettingsFragment;
import com.igeltech.nevercrypt.android.locations.fragments.ContainerSettingsFragmentBase;
import com.igeltech.nevercrypt.container.ContainerFormatInfo;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class HashAlgHintPropertyEditor extends HashingAlgorithmPropertyEditorBase
{
    public HashAlgHintPropertyEditor(ContainerSettingsFragmentBase containerSettingsFragment)
    {
        super(containerSettingsFragment, R.string.hash_algorithm, R.string.hash_alg_desc, true);
    }

    @Override
    public ContainerSettingsFragment getHost()
    {
        return (ContainerSettingsFragment) super.getHost();
    }

    @Override
    protected List<MessageDigest> getCurrentHashAlgList()
    {
        ContainerFormatInfo cfi = getHost().getCurrentContainerFormat();
        return cfi != null ? cfi.getVolumeLayout().getSupportedHashFuncs() : new ArrayList<MessageDigest>();
    }

    @Override
    protected boolean hasSavedHashFuncSelection()
    {
        String name = getHost().getLocation().getExternalSettings().getHashFuncName();
        return name != null && !name.isEmpty();
    }

    @Override
    protected int findSavedHashFuncIndex(List<MessageDigest> algs)
    {
        return findHashFuncIndexByName(algs, getHost().getLocation().getExternalSettings().getHashFuncName());
    }

    @Override
    protected void saveHashFuncValue(MessageDigest hashFunc)
    {
        getHost().getLocation().getExternalSettings().setHashFuncName(hashFunc.getAlgorithm());
        getHost().saveExternalSettings();
    }

    @Override
    protected void saveAutoDetectValue()
    {
        getHost().getLocation().getExternalSettings().setHashFuncName(null);
        getHost().saveExternalSettings();
    }

    @Override
    protected String getHashFuncEntryName(MessageDigest md)
    {
        return md.getAlgorithm();
    }
}
