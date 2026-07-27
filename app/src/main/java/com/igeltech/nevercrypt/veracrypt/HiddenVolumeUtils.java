package com.igeltech.nevercrypt.veracrypt;

import com.igeltech.nevercrypt.fs.FileSystem;
import com.igeltech.nevercrypt.fs.FileSystemInfo;
import com.igeltech.nevercrypt.fs.RandomAccessIO;
import com.igeltech.nevercrypt.fs.exfat.ExFat;
import com.igeltech.nevercrypt.fs.fat.FatFS;

import java.io.IOException;

public final class HiddenVolumeUtils
{
    /**
     * Prevents instantiation of this stateless utility class.
     */
    private HiddenVolumeUtils()
    {
    }

    /**
     * Opens an already formatted outer volume and calculates the free tail available for a hidden volume.
     */
    public static long getMaxHiddenVolumeSize(RandomAccessIO outerVolume, FileSystemInfo outerFileSystem, long outerDataAreaSize) throws IOException
    {
        FileSystem fs = outerFileSystem.openFileSystem(outerVolume, true);
        try
        {
            return getMaxHiddenVolumeSize(fs, outerDataAreaSize);
        }
        finally
        {
            fs.close(true);
        }
    }

    /**
     * Rounds a value down to the nearest multiple of the supplied block size.
     */
    public static long alignDown(long value, long blockSize)
    {
        return value - value % blockSize;
    }

    /**
     * Returns the size of the contiguous free area at the end of the mounted outer file system.
     */
    private static long getMaxHiddenVolumeSize(FileSystem outerFileSystem, long outerDataAreaSize)
    {
        long freeTailStart;
        if (outerFileSystem instanceof FatFS)
            freeTailStart = getFreeTailStart((FatFS) outerFileSystem, outerDataAreaSize);
        else if (outerFileSystem instanceof ExFat)
            freeTailStart = ((ExFat) outerFileSystem).getFreeSpaceVolumeStartOffset();
        else
            return 0;
        return freeTailStart >= 0 && freeTailStart < outerDataAreaSize ? outerDataAreaSize - freeTailStart : 0;
    }

    /**
     * Finds the first byte after the last allocated FAT cluster in the outer file system.
     */
    private static long getFreeTailStart(FatFS fat, long outerDataAreaSize)
    {
        int[] clusterTable = fat.getClusterTable();
        long clusterSize = (long) fat.getSectorsPerCluster() * fat.getBytesPerSector();
        long freeTailStart = clusterTable.length > 2 ? fat.getClusterOffset(2) : outerDataAreaSize;
        for (int i = 2; i < clusterTable.length; i++)
            if (clusterTable[i] != 0)
                freeTailStart = fat.getClusterOffset(i) + clusterSize;
        return Math.min(freeTailStart, outerDataAreaSize);
    }
}
