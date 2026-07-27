package com.igeltech.nevercrypt.locations;

import com.igeltech.nevercrypt.container.Container;
import com.igeltech.nevercrypt.container.ContainerFormatInfo;
import com.igeltech.nevercrypt.crypto.SecureBuffer;

import java.io.IOException;
import java.util.List;

public interface ContainerLocation extends CryptoLocation
{
    @Override
    ExternalSettings getExternalSettings();

    Container getCryptoContainer() throws IOException;

    List<ContainerFormatInfo> getSupportedFormats();

    /**
     * Sets a temporary encryption engine hint for the next container opening attempt.
     */
    void setOpeningEncryptionEngineHint(String cipherName, String cipherModeName);

    /**
     * Sets a temporary KDF/hash hint for the next container opening attempt.
     */
    void setOpeningHashFuncHint(String hashFuncName);

    /**
     * Sets one-shot hidden volume protection for the next container opening attempt.
     * Implementations take ownership of hiddenPassword and must wipe it after probing the hidden header.
     */
    void setHiddenVolumeProtection(
            boolean protect,
            SecureBuffer hiddenPassword,
            int hiddenNumKDFIterations,
            String hiddenCipherName,
            String hiddenCipherModeName,
            String hiddenHashFuncName);

    /**
     * Returns the free space value exposed through DocumentProvider.
     * Protected mounts override regular FS accounting so the hidden volume size is not leaked.
     */
    long getDocumentProviderFreeSpace(long defaultFreeSpace);

    /**
     * Returns the total space value exposed through DocumentProvider.
     * This may differ from the writable FS size when hidden-volume protection is active.
     */
    long getDocumentProviderTotalSpace(long defaultTotalSpace);

    interface ExternalSettings extends CryptoLocation.ExternalSettings
    {
        String getContainerFormatName();

        void setContainerFormatName(String containerFormatName);

        String getEncEngineName();

        void setEncEngineName(String encEngineName);

        String getHashFuncName();

        void setHashFuncName(String hashFuncName);
    }
}
