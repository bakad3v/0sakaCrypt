package com.igeltech.nevercrypt.container;

import static com.igeltech.nevercrypt.container.VolumeLayoutBase.SECTOR_SIZE;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.errors.UserException;
import com.igeltech.nevercrypt.crypto.EncryptedFileWithCache;
import com.igeltech.nevercrypt.crypto.FileEncryptionEngine;
import com.igeltech.nevercrypt.crypto.SecureBuffer;
import com.igeltech.nevercrypt.exceptions.ApplicationException;
import com.igeltech.nevercrypt.fs.FileSystemInfo;
import com.igeltech.nevercrypt.fs.RandomAccessIO;
import com.igeltech.nevercrypt.fs.fat.FATInfo;
import com.igeltech.nevercrypt.locations.Location;
import com.igeltech.nevercrypt.truecrypt.StdLayout;
import com.igeltech.nevercrypt.veracrypt.HiddenVolumeLayout;
import com.igeltech.nevercrypt.veracrypt.HiddenVolumeUtils;

import java.io.IOException;
import java.security.MessageDigest;

public class HiddenVolumeFormatter extends ContainerFormatterBase
{
    protected SecureBuffer _hiddenPassword;
    protected FileSystemInfo _hiddenFileSystemType = new FATInfo();
    protected FileEncryptionEngine _hiddenEncryptionEngine;
    protected MessageDigest _hiddenHashFunc;
    protected long _hiddenVolumeSize;
    protected int _hiddenNumKDFIterations;

    /**
     * Sets the VeraCrypt PIM value used for deriving the hidden-volume header key.
     */
    public void setHiddenNumKDFIterations(int num)
    {
        _hiddenNumKDFIterations = num;
    }

    /**
     * Sets the password that will protect the hidden volume header.
     */
    public void setHiddenPassword(SecureBuffer pass)
    {
        _hiddenPassword = pass;
    }

    /**
     * Sets the requested hidden volume payload size in bytes.
     */
    public void setHiddenVolumeSize(long hiddenVolumeSize)
    {
        _hiddenVolumeSize = hiddenVolumeSize;
    }

    /**
     * Selects the hidden-volume encryption engine by cipher and mode names.
     */
    public void setHiddenEncryptionEngine(String encAlgName, String encModeName)
    {
        setHiddenEncryptionEngine((FileEncryptionEngine) VolumeLayoutBase.findCipher(getHiddenLayout().getSupportedEncryptionEngines(), encAlgName, encModeName));
    }

    /**
     * Sets the hidden-volume encryption engine instance.
     */
    public void setHiddenEncryptionEngine(FileEncryptionEngine engine)
    {
        _hiddenEncryptionEngine = engine;
    }

    /**
     * Selects the hidden-volume password hash function by name.
     */
    public void setHiddenHashFunc(String name)
    {
        setHiddenHashFunc(VolumeLayoutBase.findHashFunc(getHiddenLayout().getSupportedHashFuncs(), name));
    }

    /**
     * Sets the hidden-volume password hash function instance.
     */
    public void setHiddenHashFunc(MessageDigest md)
    {
        _hiddenHashFunc = md;
    }

    /**
     * Sets the file system formatter used for the hidden volume payload.
     */
    public void setHiddenFileSystemType(FileSystemInfo fsInfo)
    {
        _hiddenFileSystemType = fsInfo;
    }

    /**
     * Returns a fresh hidden-volume layout for the selected container format.
     */
    protected VolumeLayout getHiddenLayout()
    {
        return _containerFormat.getHiddenVolumeLayout();
    }

    /**
     * Applies the hidden-volume password to the supplied layout.
     */
    protected void setHiddenVolumeLayoutPassword(VolumeLayout layout) throws IOException
    {
        if (_hiddenPassword != null)
        {
            byte[] pass = _hiddenPassword.getDataArray();
            layout.setPassword(ContainerBase.cutPassword(pass, _containerFormat.getMaxPasswordLength()));
            SecureBuffer.eraseData(pass);
        }
        else
            layout.setPassword(new byte[0]);
    }

    /**
     * Opens the created outer volume and calculates the maximum hidden payload size from its real file system.
     */
    public long calculateMaxHiddenVolumeSize(Location location) throws IOException, ApplicationException, UserException
    {
        try (RandomAccessIO io = getIO(location))
        {
            com.igeltech.nevercrypt.veracrypt.VolumeLayout outerLayout = readOuterVeraCryptLayout(io);
            try
            {
                long maxHiddenSize = getMaxHiddenVolumeSize(io, outerLayout);
                return HiddenVolumeUtils.alignDown(maxHiddenSize, SECTOR_SIZE);
            }
            finally
            {
                outerLayout.close();
            }
        }
    }

    /**
     * Adds a hidden volume to an existing outer VeraCrypt container.
     */
    public void formatHiddenVolume(Location location) throws IOException, ApplicationException, UserException
    {
        try (RandomAccessIO io = getIO(location))
        {
            com.igeltech.nevercrypt.veracrypt.VolumeLayout outerLayout = readOuterVeraCryptLayout(io);
            try
            {
                long maxHiddenSize = getMaxHiddenVolumeSize(io, outerLayout);
                formatHiddenVolume(io, io.length(), maxHiddenSize);
            }
            finally
            {
                outerLayout.close();
            }
        }
    }

    /**
     * Reads and decrypts the outer VeraCrypt header using the current outer-volume settings.
     */
    protected com.igeltech.nevercrypt.veracrypt.VolumeLayout readOuterVeraCryptLayout(RandomAccessIO io) throws IOException, ApplicationException, UserException
    {
        if (!_containerFormat.hasHiddenContainerSupport())
            throw new UserException(getContext(), R.string.err_hidden_volume_is_not_supported);

        VolumeLayout layout = getLayout();
        if (!(layout instanceof com.igeltech.nevercrypt.veracrypt.VolumeLayout))
            throw new UserException(getContext(), R.string.err_hidden_volume_is_not_supported);

        com.igeltech.nevercrypt.veracrypt.VolumeLayout outerLayout = (com.igeltech.nevercrypt.veracrypt.VolumeLayout) layout;
        setVolumeLayoutPassword(outerLayout);
        if (_numKDFIterations > 0)
            outerLayout.setNumKDFIterations(_numKDFIterations);
        if (_encryptionEngine != null)
            outerLayout.setEngine(_encryptionEngine);
        if (_hashFunc != null)
            outerLayout.setHashFunc(_hashFunc);
        try
        {
            if (!outerLayout.readHeader(io))
                throw new UserException(getContext(), R.string.bad_container_file_or_wrong_password);
            return outerLayout;
        }
        catch (IOException | ApplicationException | UserException e)
        {
            outerLayout.close();
            throw e;
        }
    }

    /**
     * Formats the hidden file system first and writes hidden headers only after that succeeds.
     */
    protected void formatHiddenVolume(RandomAccessIO io, long containerSize, long maxHiddenSize) throws IOException, ApplicationException, UserException
    {
        long hiddenSize = _hiddenVolumeSize > 0 ? HiddenVolumeUtils.alignDown(_hiddenVolumeSize, SECTOR_SIZE) : maxHiddenSize;
        if (hiddenSize <= 0 || hiddenSize > maxHiddenSize)
        {
            long maxMiB = maxHiddenSize / (1024L * 1024L);
            throw new UserException("Hidden volume size is too large. Maximum size is " + maxMiB + " MiB.", R.string.err_hidden_volume_size_is_too_large, maxMiB);
        }

        HiddenVolumeLayout hiddenLayout = (HiddenVolumeLayout) getHiddenLayout();
        setHiddenVolumeLayoutPassword(hiddenLayout);
        if (_hiddenNumKDFIterations > 0)
            hiddenLayout.setNumKDFIterations(_hiddenNumKDFIterations);
        if (_hiddenEncryptionEngine != null)
            hiddenLayout.setEngine(_hiddenEncryptionEngine);
        if (_hiddenHashFunc != null)
            hiddenLayout.setHashFunc(_hiddenHashFunc);
        hiddenLayout.setContainerSize(containerSize, hiddenSize);
        hiddenLayout.initNew();
        hiddenLayout.preparePayloadEncryptionEngine();
        try
        {
            EncryptedFileWithCache hiddenVolume = null;
            try
            {
                hiddenVolume = new EncryptedFileWithCache(io, hiddenLayout);
                hiddenLayout.formatFS(hiddenVolume, _hiddenFileSystemType);
                hiddenVolume.close(false);
                hiddenVolume = null;
            }
            catch (Exception e)
            {
                throw new UserException(getContext(), R.string.err_hidden_volume_file_system_is_too_small, e);
            }
            finally
            {
                if (hiddenVolume != null)
                    try
                    {
                        hiddenVolume.close(false);
                    }
                    catch (IOException ignored)
                    {
                    }
            }
            hiddenLayout.writeHeader(io);
        }
        finally
        {
            hiddenLayout.close();
        }
    }

    /**
     * Calculates the contiguous free tail inside the mounted outer volume payload.
     */
    protected long getMaxHiddenVolumeSize(RandomAccessIO io, StdLayout outerLayout) throws IOException
    {
        RandomAccessIO outerVolume = new EncryptedFileWithCache(io, outerLayout);
        try
        {
            long outerDataSize = outerLayout.getEncryptedDataSize(io.length());
            return HiddenVolumeUtils.alignDown(HiddenVolumeUtils.getMaxHiddenVolumeSize(outerVolume, _fileSystemType, outerDataSize), SECTOR_SIZE);
        }
        finally
        {
            ((EncryptedFileWithCache) outerVolume).close(false);
        }
    }
}
