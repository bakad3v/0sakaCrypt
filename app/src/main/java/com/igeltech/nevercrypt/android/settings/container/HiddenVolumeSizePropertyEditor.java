package com.igeltech.nevercrypt.android.settings.container;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.locations.fragments.CreateContainerFragmentBase;
import com.igeltech.nevercrypt.android.locations.tasks.CreateContainerTaskFragmentBase;
import com.igeltech.nevercrypt.android.settings.IntPropertyEditor;

public class HiddenVolumeSizePropertyEditor extends IntPropertyEditor
{
    /**
     * Creates the hidden-volume size editor for the second wizard step.
     */
    public HiddenVolumeSizePropertyEditor(CreateContainerFragmentBase createContainerFragment)
    {
        super(createContainerFragment, R.string.hidden_volume_size, R.string.hidden_volume_size_desc, createContainerFragment.getTag());
    }

    /**
     * Loads the requested hidden size or defaults to the real maximum calculated from the outer volume.
     */
    @Override
    protected int loadValue()
    {
        if (getHostFragment().getState().containsKey(CreateContainerTaskFragmentBase.ARG_HIDDEN_SIZE))
            return getHostFragment().getState().getInt(CreateContainerTaskFragmentBase.ARG_HIDDEN_SIZE, 0);
        return getDefaultMaxSizeMiB();
    }

    /**
     * Saves the requested hidden volume size in mebibytes.
     */
    @Override
    protected void saveValue(int value)
    {
        getHostFragment().getState().putInt(CreateContainerTaskFragmentBase.ARG_HIDDEN_SIZE, Math.max(value, 0));
    }

    /**
     * Returns the container creation fragment that owns this property state.
     */
    protected CreateContainerFragmentBase getHostFragment()
    {
        return (CreateContainerFragmentBase) getHost();
    }

    /**
     * Returns the calculated maximum hidden volume size as a mebibyte integer for the editor.
     */
    private int getDefaultMaxSizeMiB()
    {
        long maxSize = getHostFragment().getState().getLong(CreateContainerTaskFragmentBase.ARG_MAX_HIDDEN_VOLUME_SIZE, 0);
        long maxMiB = maxSize / (1024L * 1024L);
        return maxMiB > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) maxMiB;
    }
}
