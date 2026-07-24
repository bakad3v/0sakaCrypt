package com.igeltech.nevercrypt.locations;

import com.igeltech.nevercrypt.android.helpers.ProgressReporter;
import com.igeltech.nevercrypt.crypto.SecureBuffer;

import java.io.IOException;

public interface Openable extends Location
{
    String PARAM_PASSWORD = "com.igeltech.nevercrypt.android.PASSWORD";
    String PARAM_KDF_ITERATIONS = "com.igeltech.nevercrypt.android.KDF_ITERATIONS";
    // Optional one-shot container opening hints passed through the password/options flow.
    String PARAM_CIPHER_NAME = "com.igeltech.nevercrypt.android.CIPHER_NAME";
    String PARAM_CIPHER_MODE_NAME = "com.igeltech.nevercrypt.android.CIPHER_MODE_NAME";
    String PARAM_HASHING_ALG = "com.igeltech.nevercrypt.android.HASHING_ALG";

    void setPassword(SecureBuffer pass);

    boolean hasPassword();

    boolean requirePassword();

    boolean hasCustomKDFIterations();

    boolean requireCustomKDFIterations();

    void setNumKDFIterations(int num);

    void setOpenReadOnly(boolean readOnly);

    boolean isOpen();

    void open() throws Exception;

    void close(boolean force) throws IOException;

    void setOpeningProgressReporter(ProgressReporter pr);
}
