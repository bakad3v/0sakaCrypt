package com.igeltech.nevercrypt.android.locations;

import android.content.Context;
import android.net.Uri;

import com.igeltech.nevercrypt.android.Logger;
import com.igeltech.nevercrypt.android.errors.UserException;
import com.igeltech.nevercrypt.android.errors.WrongPasswordOrBadContainerException;
import com.igeltech.nevercrypt.android.helpers.ContainerOpeningProgressReporter;
import com.igeltech.nevercrypt.android.settings.UserSettings;
import com.igeltech.nevercrypt.container.Container;
import com.igeltech.nevercrypt.container.ContainerFormatInfo;
import com.igeltech.nevercrypt.container.VolumeLayout;
import com.igeltech.nevercrypt.container.VolumeLayoutBase;
import com.igeltech.nevercrypt.crypto.FileEncryptionEngine;
import com.igeltech.nevercrypt.crypto.SecureBuffer;
import com.igeltech.nevercrypt.crypto.SimpleCrypto;
import com.igeltech.nevercrypt.exceptions.WrongFileFormatException;
import com.igeltech.nevercrypt.fs.FileSystem;
import com.igeltech.nevercrypt.locations.ContainerLocation;
import com.igeltech.nevercrypt.locations.Location;
import com.igeltech.nevercrypt.locations.LocationsManagerBase;
import com.igeltech.nevercrypt.settings.Settings;
import com.igeltech.nevercrypt.settings.SettingsCommon;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ContainerBasedLocation extends CryptoLocationBase implements ContainerLocation
{
    public static final String URI_SCHEME = "crypto-container";
    public static final int MAX_PASSWORD_LENGTH = 64;

    public ContainerBasedLocation(Uri uri, LocationsManagerBase lm, Context context, Settings settings) throws Exception
    {
        this(getContainerLocationFromUri(uri, lm), null, context, settings);
        loadFromUri(uri);
    }

    public ContainerBasedLocation(ContainerBasedLocation sibling)
    {
        super(sibling);
    }

    public ContainerBasedLocation(Location containerLocation, Context context) throws IOException
    {
        this(containerLocation, null, context, UserSettings.getSettings(context));
    }

    public ContainerBasedLocation(Location containerLocation, Container cont, Context context, Settings settings)
    {
        super(settings, new SharedData(getLocationId(containerLocation), createInternalSettings(), containerLocation, context));
        getSharedData().container = cont;
    }

    public static String getLocationId(LocationsManagerBase lm, Uri locationUri) throws Exception
    {
        Location containerLocation = getContainerLocationFromUri(locationUri, lm);
        return getLocationId(containerLocation);
    }

    public static String getLocationId(Location containerLocation)
    {
        return SimpleCrypto.calcStringMD5(containerLocation.getLocationUri().toString());
    }

    @Override
    public void loadFromUri(Uri uri)
    {
        super.loadFromUri(uri);
        _currentPathString = uri.getPath();
    }

    @Override
    public void open() throws Exception
    {
        if (isOpenOrMounted())
            return;
        Container cnt = getCryptoContainer();
        cnt.setContainerFormat(null);
        cnt.setEncryptionEngineHint(null);
        cnt.setHashFuncHint(null);
        cnt.setNumKDFIterations(0);
        if (_openingProgressReporter != null)
            cnt.setProgressReporter((ContainerOpeningProgressReporter) _openingProgressReporter);
        ContainerFormatInfo cfi = getContainerFormatInfo();
        if (cfi != null)
            cnt.setContainerFormat(cfi);
        // One-shot opening hints from the password options override persistent saved hints.
        String cipherName = getSharedData().openingCipherName;
        String cipherModeName = getSharedData().openingCipherModeName;
        if (cipherName != null && cipherModeName != null)
            cnt.setEncryptionEngineHint(cipherName, cipherModeName);
        else
        {
            String name = getExternalSettings().getEncEngineName();
            FileEncryptionEngine encEngineHint = findEncryptionEngineByName(cfi, name);
            if (encEngineHint != null)
                cnt.setEncryptionEngineHint(encEngineHint);
        }
        // The temporary KDF/hash hint falls back to the saved container hint when it is not set.
        String hashFuncName = getSharedData().openingHashFuncName;
        if (hashFuncName == null || hashFuncName.isEmpty())
            hashFuncName = getExternalSettings().getHashFuncName();
        MessageDigest hashFuncHint = findHashFuncByName(cfi, hashFuncName);
        if (hashFuncHint != null)
            cnt.setHashFuncHint(hashFuncHint);
        int numKDFIterations = getSelectedKDFIterations();
        if (numKDFIterations > 0)
            cnt.setNumKDFIterations(numKDFIterations);
        byte[] pass = getFinalPassword();
        try
        {
            cnt.open(pass);
        }
        catch (WrongFileFormatException e)
        {
            getSharedData().container = null;
            throw new WrongPasswordOrBadContainerException(getContext());
        }
        catch (Exception e)
        {
            getSharedData().container = null;
            throw e;
        }
        finally
        {
            if (pass != null)
                Arrays.fill(pass, (byte) 0);
        }
    }

    @Override
    public Uri getLocationUri()
    {
        return makeUri(URI_SCHEME).build();
    }

    @Override
    public ExternalSettings getExternalSettings()
    {
        return (ExternalSettings) super.getExternalSettings();
    }

    @Override
    public boolean hasCustomKDFIterations()
    {
        ContainerFormatInfo cfi = getContainerFormatInfo();
        return cfi == null || cfi.hasCustomKDFIterationsSupport();
    }

    @Override
    public void close(boolean force) throws IOException
    {
        com.igeltech.nevercrypt.android.Logger.debug("Closing container at " + getLocation().getLocationUri());
        super.close(force);
        if (isOpen())
        {
            try
            {
                getSharedData().container.close();
            }
            catch (Throwable e)
            {
                if (!force)
                    throw new IOException(e);
                else
                    Logger.log(e);
            }
            getSharedData().container = null;
        }
        com.igeltech.nevercrypt.android.Logger.debug("Container has been closed");
    }

    @Override
    public boolean isOpen()
    {
        return getSharedData().container != null && getSharedData().container.getVolumeLayout() != null;
    }

    @Override
    public ContainerBasedLocation copy()
    {
        return new ContainerBasedLocation(this);
    }

    @Override
    public synchronized Container getCryptoContainer() throws IOException
    {
        Container cnt = getSharedData().container;
        if (cnt == null)
        {
            cnt = initEdsContainer();
            getSharedData().container = cnt;
        }
        return cnt;
    }

    @Override
    public List<ContainerFormatInfo> getSupportedFormats()
    {
        return Container.getSupportedFormats();
    }

    /**
     * Stores a temporary encryption engine hint used only by the next open() call.
     */
    @Override
    public void setOpeningEncryptionEngineHint(String cipherName, String cipherModeName)
    {
        getSharedData().openingCipherName = cipherName == null || cipherName.isEmpty() || cipherModeName == null || cipherModeName.isEmpty()
                ? null
                : cipherName;
        getSharedData().openingCipherModeName = getSharedData().openingCipherName == null ? null : cipherModeName;
    }

    /**
     * Stores a temporary KDF/hash hint used only by the next open() call.
     */
    @Override
    public void setOpeningHashFuncHint(String hashFuncName)
    {
        getSharedData().openingHashFuncName = hashFuncName == null || hashFuncName.isEmpty() ? null : hashFuncName;
    }

    @Override
    protected SharedData getSharedData()
    {
        return (SharedData) super.getSharedData();
    }

    protected Container initEdsContainer() throws IOException
    {
        return new Container(getLocation().getCurrentPath());
    }

    protected ContainerFormatInfo getContainerFormatInfo()
    {
        String name = getExternalSettings().getContainerFormatName();
        return name != null ? Container.findFormatByName(name) : null;
    }

    /**
     * Resolves a saved hint against the selected format, or all supported formats when format is unknown.
     */
    protected FileEncryptionEngine findEncryptionEngineByName(ContainerFormatInfo selectedFormat, String encEngineName)
    {
        if (encEngineName == null || encEngineName.isEmpty())
            return null;
        for (ContainerFormatInfo cfi : getCandidateFormats(selectedFormat))
        {
            VolumeLayout vl = cfi.getVolumeLayout();
            FileEncryptionEngine ee = (FileEncryptionEngine) VolumeLayoutBase.findEncEngineByName(vl.getSupportedEncryptionEngines(), encEngineName);
            if (ee != null)
                return ee;
        }
        return null;
    }

    /**
     * Resolves a saved or one-shot KDF/hash hint against candidate container formats.
     */
    protected MessageDigest findHashFuncByName(ContainerFormatInfo selectedFormat, String hashFuncName)
    {
        if (hashFuncName == null || hashFuncName.isEmpty())
            return null;
        for (ContainerFormatInfo cfi : getCandidateFormats(selectedFormat))
        {
            MessageDigest hashFunc = VolumeLayoutBase.findHashFunc(cfi.getVolumeLayout().getSupportedHashFuncs(), hashFuncName);
            if (hashFunc != null)
                return hashFunc;
        }
        return null;
    }

    /**
     * Limits hint resolution to the known container format when the user has selected one.
     */
    protected List<ContainerFormatInfo> getCandidateFormats(ContainerFormatInfo selectedFormat)
    {
        return selectedFormat != null ? Collections.singletonList(selectedFormat) : getSupportedFormats();
    }

    @Override
    protected byte[] getSelectedPassword()
    {
        byte[] pass = super.getSelectedPassword();
        if (pass != null && pass.length > MAX_PASSWORD_LENGTH)
        {
            byte[] tmp = pass;
            pass = new byte[MAX_PASSWORD_LENGTH];
            System.arraycopy(tmp, 0, pass, 0, MAX_PASSWORD_LENGTH);
            SecureBuffer.eraseData(tmp);
        }
        return pass;
    }

    @Override
    protected ExternalSettings loadExternalSettings()
    {
        ExternalSettings res = new ExternalSettings();
        res.setProtectionKeyProvider(() -> {
            try
            {
                return UserSettings.getSettings(getContext()).getSettingsProtectionKey();
            }
            catch (SettingsCommon.InvalidSettingsPassword invalidSettingsPassword)
            {
                return null;
            }
        });
        res.load(_globalSettings, getId());
        return res;
    }

    @Override
    protected FileSystem createBaseFS(boolean readOnly) throws IOException, UserException
    {
        return getSharedData().container.getEncryptedFS(readOnly);
    }

    public static class ExternalSettings extends CryptoLocationBase.ExternalSettings implements ContainerLocation.ExternalSettings
    {
        private static final String SETTINGS_CONTAINER_FORMAT = "container_format";
        private static final String SETTINGS_ENC_ENGINE = "encryption_engine";
        private static final String SETTINGS_HASH_FUNC = "hash_func";
        private String _containerFormatName, _hashFuncName, _encEngineName;

        public ExternalSettings()
        {
        }

        @Override
        public String getContainerFormatName()
        {
            return _containerFormatName;
        }

        @Override
        public void setContainerFormatName(String containerFormatName)
        {
            _containerFormatName = containerFormatName;
        }

        @Override
        public String getEncEngineName()
        {
            return _encEngineName;
        }

        @Override
        public void setEncEngineName(String encEngineName)
        {
            _encEngineName = encEngineName;
        }

        @Override
        public String getHashFuncName()
        {
            return _hashFuncName;
        }

        @Override
        public void setHashFuncName(String hashFuncName)
        {
            _hashFuncName = hashFuncName;
        }

        @Override
        public void saveToJSONObject(JSONObject jo) throws JSONException
        {
            super.saveToJSONObject(jo);
            jo.put(SETTINGS_CONTAINER_FORMAT, _containerFormatName);
            jo.put(SETTINGS_ENC_ENGINE, _encEngineName);
            jo.put(SETTINGS_HASH_FUNC, _hashFuncName);
        }

        @Override
        public void loadFromJSONOjbect(JSONObject jo) throws JSONException
        {
            super.loadFromJSONOjbect(jo);
            _containerFormatName = jo.optString(SETTINGS_CONTAINER_FORMAT, null);
            _encEngineName = jo.optString(SETTINGS_ENC_ENGINE, null);
            _hashFuncName = jo.optString(SETTINGS_HASH_FUNC, null);
        }
    }

    protected static class SharedData extends CryptoLocationBase.SharedData
    {
        public Container container;
        // Temporary hints populated from the opening options screen for the next open() call.
        public String openingCipherName, openingCipherModeName, openingHashFuncName;

        public SharedData(String id, CryptoLocationBase.InternalSettings settings, Location location, Context context)
        {
            super(id, settings, location, context);
        }
    }
}
