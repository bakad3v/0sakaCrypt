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
    // One-shot opening flag used to enable hidden-volume write protection.
    String PARAM_PROTECT_HIDDEN_VOLUME = "com.igeltech.nevercrypt.android.PROTECT_HIDDEN_VOLUME";
    // One-shot hidden-volume password; it stays in SecureBuffer form until the opener consumes it.
    String PARAM_HIDDEN_VOLUME_PASSWORD = "com.igeltech.nevercrypt.android.HIDDEN_VOLUME_PASSWORD";

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
