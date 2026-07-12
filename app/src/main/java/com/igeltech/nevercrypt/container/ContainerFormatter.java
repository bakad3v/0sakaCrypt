package com.igeltech.nevercrypt.container;

import com.igeltech.nevercrypt.locations.CryptoLocation;

import java.io.IOException;

public class ContainerFormatter extends ContainerFormatterBase
{
    private boolean _createOuterVolumeForHiddenVolume;

    /**
     * Marks the outer volume as a hidden-volume host and forces randomization of free space.
     */
    public void setCreateOuterVolumeForHiddenVolume(boolean val)
    {
        _createOuterVolumeForHiddenVolume = val;
        if (val)
            _randFreeSpace = true;
    }

    /**
     * Skips writing application metadata into outer volumes that are intended to host hidden data.
     */
    @Override
    protected void writeInternalContainerSettings(CryptoLocation loc) throws IOException
    {
        if (_createOuterVolumeForHiddenVolume)
            return;
        super.writeInternalContainerSettings(loc);
    }
}
