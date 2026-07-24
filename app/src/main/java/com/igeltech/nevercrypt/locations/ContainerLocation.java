package com.igeltech.nevercrypt.locations;

import com.igeltech.nevercrypt.container.Container;
import com.igeltech.nevercrypt.container.ContainerFormatInfo;

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
