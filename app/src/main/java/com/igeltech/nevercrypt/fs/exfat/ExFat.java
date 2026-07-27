package com.igeltech.nevercrypt.fs.exfat;

import com.igeltech.nevercrypt.fs.FileSystem;
import com.igeltech.nevercrypt.fs.Path;
import com.igeltech.nevercrypt.fs.RandomAccessIO;
import com.igeltech.nevercrypt.fs.VolumeSizeLimiter;
import com.igeltech.nevercrypt.fs.util.FileStat;
import com.igeltech.nevercrypt.fs.util.Util;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

public class ExFat implements FileSystem, VolumeSizeLimiter
{
    private static final byte[] EXFAT_SIGN = new byte[]{'E', 'X', 'F', 'A', 'T', ' ', ' ', ' '};
    private static final int MIN_COMPATIBLE_NATIVE_MODULE_VERSION = 1001;

    static
    {
        System.loadLibrary("fsexfat");
    }

    final Object _sync = new Object();
    private final RandomAccessIO _exfatImageFile;
    private long _exfatPtr;

    public ExFat(RandomAccessIO exFatImage, boolean readOnly) throws IOException
    {
        _exfatImageFile = exFatImage;
        _exfatPtr = openFS(readOnly);
        if (_exfatPtr == 0)
            throw new IOException("Failed opening exfat file system");
    }

    public static boolean isExFATImage(RandomAccessIO img) throws IOException
    {
        byte[] buf = new byte[8];
        img.seek(3);
        return Util.readBytes(img, buf) == buf.length && Arrays.equals(EXFAT_SIGN, buf);
    }

    public static void makeNewFS(RandomAccessIO img) throws IOException
    {
        makeNewFS(img, null, 0, 0, -1);
    }

    public static void makeNewFS(RandomAccessIO img, String label, int volumeSerial, long firstSector, int sectorsPerCluster) throws IOException
    {
        if (makeFS(img, label, volumeSerial, firstSector, sectorsPerCluster) != 0)
            throw new IOException("Failed formatting an ExFAT image");
    }

    private static native int makeFS(RandomAccessIO raio, String label, int volumeSerial, long firstSector, int sectorsPerCluster);

    native static int getVersion();

    @Override
    public Path getRootPath() throws IOException
    {
        return new ExFatPath(this, "/");
    }

    @Override
    public Path getPath(String pathString) throws IOException
    {
        return new ExFatPath(this, pathString);
    }

    @Override
    public void close(boolean force) throws IOException
    {
        synchronized (_sync)
        {
            if (_exfatPtr != 0)
            {
                closeFS();
                _exfatPtr = 0;
            }
        }
    }

    @Override
    public boolean isClosed()
    {
        return _exfatPtr == 0;
    }

    public long getFreeSpaceVolumeStartOffset()
    {
        return getFreeSpaceStartOffset();
    }

    /**
     * Applies the protected writable size used when an outer volume is opened with hidden protection.
     */
    @Override
    public void setVolumeSizeLimit(long volumeSizeLimit) throws IOException
    {
        if (volumeSizeLimit <= 0)
            throw new IllegalArgumentException("volumeSizeLimit <= 0");
        synchronized (_sync)
        {
            // The native layer keeps this as an in-memory cap and returns ENOSPC past the boundary.
            int res = setVolumeSizeLimit(_exfatPtr, volumeSizeLimit);
            if (res != 0)
                throw new IOException("Failed setting exFAT volume size limit. Error code = " + res);
        }
    }

    public void overwriteFreeSpace() throws IOException
    {
        int res = randFreeSpace();
        if (res != 0)
            throw new IOException("Failed overwriting the free space. code " + res);
    }

    native int readDir(String path, Collection<String> files);

    native int getAttr(FileStat stat, String path);

    native int makeDir(String path);

    native int makeFile(String path);

    native long getFreeSpace();

    native long getTotalSpace();

    // In-memory cap used by hidden-volume protection; it does not modify the on-disk exFAT metadata.
    native int setVolumeSizeLimit(long handle, long volumeSizeLimit);

    native int rename(String oldPath, String newPath);

    native int delete(String path);

    native int rmdir(String path);

    native int truncate(long handle, long size);

    native long openFile(String path);

    native int closeFile(long handle);

    native long getSize(long handle);

    native int read(long handle, byte[] buf, int bufOffset, int count, long position);

    native int write(long handle, byte[] buf, int bufOffset, int count, long position);

    native int flush(long handle);

    native long openFS(boolean readOnly);

    native int closeFS();

    native long getFreeSpaceStartOffset();

    native int randFreeSpace();

    native int updateTime(String path, long time);
}
