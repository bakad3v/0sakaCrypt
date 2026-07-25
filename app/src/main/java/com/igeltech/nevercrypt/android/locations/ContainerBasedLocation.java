package com.igeltech.nevercrypt.android.locations;

import android.content.Context;
import android.net.Uri;

import com.igeltech.nevercrypt.android.Logger;
import com.igeltech.nevercrypt.android.R;
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
import com.igeltech.nevercrypt.fs.Directory;
import com.igeltech.nevercrypt.fs.FileSystem;
import com.igeltech.nevercrypt.fs.VolumeSizeLimiter;
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
        boolean opened = false;
        try
        {
            cnt.open(pass);
            opened = true;
            // Hidden protection needs a successfully opened outer header before it can probe the hidden header.
            applyHiddenVolumeProtection(cnt);
        }
        catch (WrongFileFormatException e)
        {
            clearContainerAfterOpenFailure(cnt, opened);
            throw new WrongPasswordOrBadContainerException(getContext());
        }
        catch (Exception e)
        {
            clearContainerAfterOpenFailure(cnt, opened);
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
        clearHiddenVolumeProtectionState();
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

    /**
     * Stores hidden-volume protection options for the next open attempt only.
     */
    @Override
    public void setHiddenVolumeProtection(boolean protect, SecureBuffer hiddenPassword)
    {
        SharedData data = getSharedData();
        // This is one-shot state: replace stale password data and reset all derived limits.
        if (data.hiddenVolumePassword != hiddenPassword)
            clearHiddenVolumeProtectionPassword();
        data.protectHiddenVolume = protect;
        data.hiddenVolumePassword = protect ? hiddenPassword : null;
        data.protectedOuterVolumeSize = -1;
        data.hiddenVolumeSize = -1;
        data.fullOuterVolumeSize = -1;
        data.documentProviderFreeSpace = -1;
        data.documentProviderTotalSpace = -1;
        data.hiddenVolumeLimitApplied = false;
        if (!protect && hiddenPassword != null)
            hiddenPassword.close();
    }

    /**
     * Returns provider-visible free space without exposing the protected writable cap.
     */
    @Override
    public long getDocumentProviderFreeSpace(long defaultFreeSpace)
    {
        // DocumentProvider exposes the full outer view so protected hidden-volume size is not observable.
        SharedData data = getSharedData();
        if (data.documentProviderFreeSpace >= 0)
            return data.documentProviderFreeSpace;
        if (data.hiddenVolumeSize > 0)
        {
            long max = data.fullOuterVolumeSize > 0 ? data.fullOuterVolumeSize : Long.MAX_VALUE;
            return Math.min(max, defaultFreeSpace + data.hiddenVolumeSize);
        }
        return defaultFreeSpace;
    }

    /**
     * Returns provider-visible total space without exposing the protected writable cap.
     */
    @Override
    public long getDocumentProviderTotalSpace(long defaultTotalSpace)
    {
        // The mounted FS may be capped, but Android's root metadata should keep the full outer size.
        SharedData data = getSharedData();
        return data.fullOuterVolumeSize > 0 ? data.fullOuterVolumeSize : defaultTotalSpace;
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
        FileSystem fs = getSharedData().container.getEncryptedFS(readOnly);
        // Apply the cap after FS creation so root counters can be cached before they become limited.
        applyProtectedVolumeSizeLimit(fs);
        return fs;
    }

    /**
     * Reads the hidden header and calculates the safe writable portion of the outer volume.
     */
    private void applyHiddenVolumeProtection(Container outerContainer) throws IOException, UserException
    {
        SharedData data = getSharedData();
        if (!data.protectHiddenVolume)
            return;
        SecureBuffer hiddenPassword = data.hiddenVolumePassword;
        if (hiddenPassword == null)
            throw new UserException(getContext(), R.string.err_hidden_volume_protection_failed);
        byte[] hiddenPasswordBytes = null;
        try
        {
            hiddenPasswordBytes = hiddenPassword.getDataArray();
            HiddenVolumeProtectionInfo info = readHiddenVolumeProtectionInfo(outerContainer, hiddenPasswordBytes);
            long outerDataOffset = outerContainer.getVolumeLayout().getEncryptedDataOffset();
            // Outer writes are safe only before the hidden volume data begins.
            long protectedOuterVolumeSize = info.hiddenDataOffset - outerDataOffset;
            if (protectedOuterVolumeSize <= 0 || protectedOuterVolumeSize >= info.fullOuterVolumeSize)
                throw new UserException(getContext(), R.string.err_hidden_volume_protection_failed);
            data.protectedOuterVolumeSize = protectedOuterVolumeSize;
            data.hiddenVolumeSize = info.hiddenVolumeSize;
            data.fullOuterVolumeSize = info.fullOuterVolumeSize;
        }
        finally
        {
            if (hiddenPasswordBytes != null)
                SecureBuffer.eraseData(hiddenPasswordBytes);
            // The hidden password is only needed for this probe and must not survive the open attempt.
            clearHiddenVolumeProtectionPassword();
        }
    }

    /**
     * Opens a temporary hidden container in header-only mode to validate the password and read its layout.
     */
    private HiddenVolumeProtectionInfo readHiddenVolumeProtectionInfo(Container outerContainer, byte[] hiddenPasswordBytes) throws IOException, UserException
    {
        Container hiddenContainer = new Container(getLocation().getCurrentPath());
        hiddenContainer.setContainerFormat(outerContainer.getContainerFormat());
        try
        {
            hiddenContainer.open(hiddenPasswordBytes, true);
            HiddenVolumeProtectionInfo info = new HiddenVolumeProtectionInfo();
            long containerSize = outerContainer.getPathToContainer().getFile().getSize();
            // Store only offsets and sizes; the hidden filesystem is never mounted here.
            info.hiddenDataOffset = hiddenContainer.getVolumeLayout().getEncryptedDataOffset();
            info.hiddenVolumeSize = hiddenContainer.getVolumeLayout().getEncryptedDataSize(containerSize);
            info.fullOuterVolumeSize = outerContainer.getVolumeLayout().getEncryptedDataSize(containerSize);
            return info;
        }
        catch (WrongFileFormatException e)
        {
            throw new UserException(getContext(), R.string.err_hidden_volume_protection_failed, e);
        }
        catch (Exception e)
        {
            throw new UserException(getContext(), R.string.err_hidden_volume_protection_failed, e);
        }
        finally
        {
            try
            {
                hiddenContainer.close();
            }
            catch (Exception e)
            {
                Logger.log(e);
            }
        }
    }

    /**
     * Installs the calculated write cap into the mounted filesystem.
     */
    private void applyProtectedVolumeSizeLimit(FileSystem fs) throws IOException, UserException
    {
        SharedData data = getSharedData();
        if (data.protectedOuterVolumeSize <= 0 || data.hiddenVolumeLimitApplied)
            return;
        cacheDocumentProviderSpace(fs);
        // Unsupported file systems fail closed instead of opening without protection.
        if (!(fs instanceof VolumeSizeLimiter))
            throw new UserException(getContext(), R.string.err_hidden_volume_protection_failed);
        ((VolumeSizeLimiter) fs).setVolumeSizeLimit(data.protectedOuterVolumeSize);
        data.hiddenVolumeLimitApplied = true;
    }

    /**
     * Captures full-size provider counters before the filesystem starts reporting the protected limit.
     */
    private void cacheDocumentProviderSpace(FileSystem fs)
    {
        SharedData data = getSharedData();
        try
        {
            Directory root = fs.getRootPath().getDirectory();
            data.documentProviderFreeSpace = root.getFreeSpace();
            data.documentProviderTotalSpace = root.getTotalSpace();
        }
        catch (IOException e)
        {
            Logger.log(e);
            data.documentProviderTotalSpace = data.fullOuterVolumeSize;
            data.documentProviderFreeSpace = -1;
        }
    }

    /**
     * Rolls back a partially opened outer container when protection setup fails.
     */
    private void clearContainerAfterOpenFailure(Container container, boolean opened)
    {
        if (opened)
        {
            try
            {
                container.close();
            }
            catch (Throwable e)
            {
                Logger.log(e);
            }
        }
        getSharedData().container = null;
        clearHiddenVolumeProtectionState();
    }

    /**
     * Erases and forgets the one-shot hidden-volume password.
     */
    private void clearHiddenVolumeProtectionPassword()
    {
        SharedData data = getSharedData();
        if (data.hiddenVolumePassword != null)
        {
            data.hiddenVolumePassword.close();
            data.hiddenVolumePassword = null;
        }
    }

    /**
     * Drops all derived protection state so later opens start from an unrestricted baseline.
     */
    private void clearHiddenVolumeProtectionState()
    {
        clearHiddenVolumeProtectionPassword();
        SharedData data = getSharedData();
        data.protectHiddenVolume = false;
        data.protectedOuterVolumeSize = -1;
        data.hiddenVolumeSize = -1;
        data.fullOuterVolumeSize = -1;
        data.documentProviderFreeSpace = -1;
        data.documentProviderTotalSpace = -1;
        data.hiddenVolumeLimitApplied = false;
    }

    /**
     * Minimal hidden-layout data needed to protect the outer mount.
     */
    private static class HiddenVolumeProtectionInfo
    {
        long hiddenDataOffset;
        long hiddenVolumeSize;
        long fullOuterVolumeSize;
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
        // One-shot hidden-volume protection state. Negative sizes mean unknown or inactive.
        public boolean protectHiddenVolume, hiddenVolumeLimitApplied;
        public SecureBuffer hiddenVolumePassword;
        public long protectedOuterVolumeSize = -1;
        public long hiddenVolumeSize = -1;
        public long fullOuterVolumeSize = -1;
        public long documentProviderFreeSpace = -1;
        public long documentProviderTotalSpace = -1;

        public SharedData(String id, CryptoLocationBase.InternalSettings settings, Location location, Context context)
        {
            super(id, settings, location, context);
        }
    }
}
