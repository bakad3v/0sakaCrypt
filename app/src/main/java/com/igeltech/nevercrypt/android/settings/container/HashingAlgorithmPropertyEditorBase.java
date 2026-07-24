package com.igeltech.nevercrypt.android.settings.container;

import androidx.fragment.app.Fragment;

import com.igeltech.nevercrypt.android.settings.ChoiceDialogPropertyEditor;
import com.igeltech.nevercrypt.android.settings.PropertyEditor;
import com.igeltech.nevercrypt.container.VolumeLayoutBase;
import com.igeltech.nevercrypt.crypto.hash.RIPEMD160;
import com.igeltech.nevercrypt.crypto.hash.Whirlpool;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Base choice editor for hash/KDF algorithms shared by creation, saved hints, and opening hints.
 */
public abstract class HashingAlgorithmPropertyEditorBase extends ChoiceDialogPropertyEditor
{
    private final boolean _autoDetectEntry;

    protected HashingAlgorithmPropertyEditorBase(PropertyEditor.Host hostFragment, int titleResId, int descResId, boolean autoDetectEntry)
    {
        super(hostFragment, titleResId, descResId, ((Fragment) hostFragment).getTag());
        _autoDetectEntry = autoDetectEntry;
    }

    public static String getHashFuncName(MessageDigest md)
    {
        if (md instanceof RIPEMD160)
            return "RIPEMD-160";
        if (md instanceof Whirlpool)
            return "Whirlpool";
        return md.getAlgorithm();
    }

    @Override
    protected int loadValue()
    {
        List<MessageDigest> algs = getCurrentHashAlgList();
        if (algs == null || algs.isEmpty())
            return _autoDetectEntry ? 0 : -1;
        if (hasSavedHashFuncSelection())
        {
            int index = findSavedHashFuncIndex(algs);
            if (index >= 0)
                return getEntryIndexForAlgIndex(index);
            return _autoDetectEntry ? 0 : -1;
        }
        return 0;
    }

    @Override
    protected void saveValue(int value)
    {
        if (_autoDetectEntry && value <= 0)
        {
            saveAutoDetectValue();
            return;
        }
        List<MessageDigest> algs = getCurrentHashAlgList();
        int algIndex = getAlgIndexForEntryIndex(value);
        if (algs != null && algIndex >= 0 && algIndex < algs.size())
        {
            MessageDigest md = algs.get(algIndex);
            saveHashFuncValue(md);
        }
    }

    @Override
    protected ArrayList<String> getEntries()
    {
        ArrayList<String> res = new ArrayList<>();
        if (_autoDetectEntry)
            // The first entry keeps the existing auto-detection behavior.
            res.add("-");
        List<MessageDigest> supportedEngines = getCurrentHashAlgList();
        if (supportedEngines != null)
        {
            for (MessageDigest eng : supportedEngines)
                res.add(getHashFuncEntryName(eng));
        }
        return res;
    }

    protected int findHashFuncIndexByName(List<MessageDigest> algs, String name)
    {
        if (name == null || name.isEmpty())
            return -1;
        MessageDigest md = VolumeLayoutBase.findHashFunc(algs, name);
        return algs.indexOf(md);
    }

    protected String getHashFuncEntryName(MessageDigest md)
    {
        return getHashFuncName(md);
    }

    /**
     * Returns the hash/KDF algorithms available in the concrete editor context.
     */
    protected abstract List<MessageDigest> getCurrentHashAlgList();

    /**
     * Returns whether this editor currently has a persisted selection to resolve.
     */
    protected abstract boolean hasSavedHashFuncSelection();

    /**
     * Finds the persisted selection in the supplied hash/KDF list.
     */
    protected abstract int findSavedHashFuncIndex(List<MessageDigest> algs);

    /**
     * Persists a concrete hash/KDF selection.
     */
    protected abstract void saveHashFuncValue(MessageDigest hashFunc);

    /**
     * Persists the auto-detect selection.
     */
    protected abstract void saveAutoDetectValue();

    private int getEntryIndexForAlgIndex(int algIndex)
    {
        return _autoDetectEntry ? algIndex + 1 : algIndex;
    }

    private int getAlgIndexForEntryIndex(int entryIndex)
    {
        return _autoDetectEntry ? entryIndex - 1 : entryIndex;
    }
}
