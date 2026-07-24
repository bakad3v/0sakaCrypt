package com.igeltech.nevercrypt.android.settings.container;

import androidx.fragment.app.Fragment;

import com.igeltech.nevercrypt.android.settings.ChoiceDialogPropertyEditor;
import com.igeltech.nevercrypt.android.settings.PropertyEditor;
import com.igeltech.nevercrypt.container.VolumeLayoutBase;
import com.igeltech.nevercrypt.crypto.EncryptionEngine;
import com.igeltech.nevercrypt.truecrypt.EncryptionEnginesRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Base choice editor for encryption algorithms shared by creation, saved hints, and opening hints.
 */
public abstract class EncryptionAlgorithmPropertyEditorBase extends ChoiceDialogPropertyEditor
{
    private final boolean _autoDetectEntry;

    protected EncryptionAlgorithmPropertyEditorBase(PropertyEditor.Host hostFragment, int titleResId, int descResId, boolean autoDetectEntry)
    {
        super(hostFragment, titleResId, descResId, ((Fragment) hostFragment).getTag());
        _autoDetectEntry = autoDetectEntry;
    }

    public static String getEncEngineName(EncryptionEngine eng)
    {
        return EncryptionEnginesRegistry.getEncEngineName(eng);
    }

    @Override
    protected int loadValue()
    {
        List<? extends EncryptionEngine> algs = getCurrentEncAlgList();
        if (algs == null || algs.isEmpty())
            return _autoDetectEntry ? 0 : -1;
        if (hasSavedAlgorithmSelection())
        {
            int index = findSavedAlgorithmIndex(algs);
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
        List<? extends EncryptionEngine> algs = getCurrentEncAlgList();
        int algIndex = getAlgIndexForEntryIndex(value);
        if (algs != null && algIndex >= 0 && algIndex < algs.size())
        {
            EncryptionEngine ee = algs.get(algIndex);
            saveAlgorithmValue(ee);
        }
    }

    @Override
    protected ArrayList<String> getEntries()
    {
        ArrayList<String> res = new ArrayList<>();
        if (_autoDetectEntry)
            // The first entry keeps the existing auto-detection behavior.
            res.add("-");
        List<? extends EncryptionEngine> supportedEngines = getCurrentEncAlgList();
        if (supportedEngines != null)
        {
            for (EncryptionEngine eng : supportedEngines)
                res.add(getEncEngineEntryName(eng));
        }
        return res;
    }

    protected int findEngineIndexByCipherAndMode(List<? extends EncryptionEngine> algs, String cipherName, String modeName)
    {
        if (cipherName == null || cipherName.isEmpty() || modeName == null || modeName.isEmpty())
            return -1;
        EncryptionEngine ee = VolumeLayoutBase.findCipher(algs, cipherName, modeName);
        return algs.indexOf(ee);
    }

    protected int findEngineIndexBySavedName(List<? extends EncryptionEngine> algs, String name)
    {
        if (name == null || name.isEmpty())
            return -1;
        for (int i = 0; i < algs.size(); i++)
        {
            if (name.equalsIgnoreCase(getSavedEncEngineName(algs.get(i))))
                return i;
        }
        return -1;
    }

    protected String getEncEngineEntryName(EncryptionEngine eng)
    {
        return getEncEngineName(eng);
    }

    protected String getSavedEncEngineName(EncryptionEngine eng)
    {
        return String.format("%s-%s", eng.getCipherName(), eng.getCipherModeName());
    }

    /**
     * Returns the encryption algorithms available in the concrete editor context.
     */
    protected abstract List<? extends EncryptionEngine> getCurrentEncAlgList();

    /**
     * Returns whether this editor currently has a persisted selection to resolve.
     */
    protected abstract boolean hasSavedAlgorithmSelection();

    /**
     * Finds the persisted selection in the supplied algorithm list.
     */
    protected abstract int findSavedAlgorithmIndex(List<? extends EncryptionEngine> algs);

    /**
     * Persists a concrete encryption algorithm selection.
     */
    protected abstract void saveAlgorithmValue(EncryptionEngine engine);

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
