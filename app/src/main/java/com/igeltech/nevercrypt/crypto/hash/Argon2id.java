package com.igeltech.nevercrypt.crypto.hash;

import java.security.MessageDigest;

public class Argon2id extends MessageDigest
{
    public static final String NAME = "Argon2id";

    public Argon2id()
    {
        super(NAME);
    }

    @Override
    protected int engineGetDigestLength()
    {
        return 0;
    }

    @Override
    protected byte[] engineDigest()
    {
        throw new UnsupportedOperationException(NAME + " is a KDF marker, not a MessageDigest");
    }

    @Override
    protected void engineReset()
    {
    }

    @Override
    protected void engineUpdate(byte input)
    {
        throw new UnsupportedOperationException(NAME + " is a KDF marker, not a MessageDigest");
    }

    @Override
    protected void engineUpdate(byte[] input, int offset, int len)
    {
        throw new UnsupportedOperationException(NAME + " is a KDF marker, not a MessageDigest");
    }
}
