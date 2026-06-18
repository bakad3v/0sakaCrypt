package com.igeltech.nevercrypt.veracrypt;

import com.igeltech.nevercrypt.crypto.EncryptionEngine;
import com.igeltech.nevercrypt.crypto.hash.Argon2id;
import com.igeltech.nevercrypt.exceptions.ApplicationException;
import com.igeltech.nevercrypt.truecrypt.StdLayout;

import org.signal.argon2.Argon2;
import org.signal.argon2.Type;
import org.signal.argon2.Version;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;

public class VolumeLayout extends StdLayout
{
    protected static final byte[] SIG = {'V', 'E', 'R', 'A'};
    protected static final short COMPATIBLE_PROGRAM_VERSION = 0x010b;
    protected static final int ARGON2ID_DEFAULT_PIM = 12;
    protected static final int ARGON2ID_HEADER_KDF_OUTPUT_SIZE = 192;
    protected static final int ARGON2ID_PARALLELISM = 1;
    private int _numIterations;

    public static int getKDFIterationsFromPIM(int pim)
    {
        return 15000 + pim * 1000;
    }

    @Override
    public void setNumKDFIterations(int num)
    {
        _numIterations = num;
    }

    @Override
    public void close() throws IOException
    {
        super.close();
        _numIterations = 0;
    }

    @Override
    public List<MessageDigest> getSupportedHashFuncs()
    {
        List<MessageDigest> l = super.getSupportedHashFuncs();
        try
        {
            l.add(MessageDigest.getInstance("SHA256"));
        }
        catch (NoSuchAlgorithmException ignored)
        {
        }
        l.add(new Argon2id());
        return l;
    }

    @Override
    protected byte[] getHeaderSignature()
    {
        return SIG;
    }

    @Override
    protected int getMKKDFNumIterations(MessageDigest hashFunc)
    {
        return _numIterations > 0 ? getKDFIterationsFromPIM(_numIterations) : "ripemd160".equalsIgnoreCase(hashFunc.getAlgorithm()) ? 655331 : 500000;
    }

    @Override
    protected byte[] deriveHeaderKey(EncryptionEngine ee, MessageDigest md, byte[] salt) throws ApplicationException
    {
        if (!isArgon2id(md))
            return super.deriveHeaderKey(ee, md, salt);

        int keySize = ee.getKeySize();
        if (_encEngine == null)
        {
            for (EncryptionEngine eng : getSupportedEncryptionEngines())
                if (eng.getKeySize() > keySize)
                    keySize = eng.getKeySize();
        }
        if (keySize > ARGON2ID_HEADER_KDF_OUTPUT_SIZE)
            throw new ApplicationException("Argon2id header key output is too short for the selected encryption engine");

        byte[] kdfOutput = deriveArgon2idHeaderKey(_password, salt);
        byte[] key = new byte[keySize];
        System.arraycopy(kdfOutput, 0, key, 0, keySize);
        Arrays.fill(kdfOutput, (byte) 0);
        return key;
    }

    protected byte[] deriveArgon2idHeaderKey(byte[] password, byte[] salt) throws ApplicationException
    {
        int pim = getArgon2idPIM();
        if (_openingProgressReporter != null)
        {
            _openingProgressReporter.setProgress(0);
            if (_openingProgressReporter.isCancelled())
                throw new CancellationException();
        }
        try
        {
            byte[] hash = new Argon2.Builder(Version.V13)
                    .type(Type.Argon2id)
                    .memoryCostKiB(getArgon2idMemoryCostKiB(pim))
                    .iterations(getArgon2idTimeCost(pim))
                    .parallelism(ARGON2ID_PARALLELISM)
                    .hashLength(ARGON2ID_HEADER_KDF_OUTPUT_SIZE)
                    .build()
                    .hash(password, salt)
                    .getHash();
            if (_openingProgressReporter != null)
                _openingProgressReporter.setProgress(100);
            return hash;
        }
        catch (CancellationException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ApplicationException("Failed deriving Argon2id header key", e);
        }
    }

    protected int getArgon2idPIM()
    {
        return _numIterations > 0 ? _numIterations : ARGON2ID_DEFAULT_PIM;
    }

    public static int getArgon2idMemoryCostKiB(int pim)
    {
        int memoryMiB = Math.min(64 + (pim - 1) * 32, 1024);
        return memoryMiB * 1024;
    }

    public static int getArgon2idTimeCost(int pim)
    {
        return pim <= 31 ? 3 + (pim - 1) / 3 : 13 + (pim - 31);
    }

    public static boolean isArgon2id(MessageDigest hashFunc)
    {
        return Argon2id.NAME.equalsIgnoreCase(hashFunc.getAlgorithm());
    }

    @Override
    protected short getMinCompatibleProgramVersion()
    {
        return COMPATIBLE_PROGRAM_VERSION;
    }
}
