package com.igeltech.nevercrypt.settings;

import android.os.Build;

import com.igeltech.nevercrypt.crypto.SecureBuffer;

import java.util.Arrays;
import java.util.List;

public class DefaultSettingsCommon implements SettingsCommon
{
    @Override
    public String getLocationSettingsString(String locationId)
    {
        return "";
    }

    @Override
    public void setLocationSettingsString(String locationId, String data)
    {
    }

    @Override
    public String getStoredLocations()
    {
        return "";
    }

    @Override
    public void setStoredLocations(String locations)
    {
    }

    @Override
    public int getMaxTempFileSize()
    {
        return 100;
    }

    @Override
    public boolean wipeTempFiles()
    {
        return true;
    }

    @Override
    public String getWorkDir()
    {
        return "";
    }

    @Override
    public int getInternalImageViewerMode()
    {
        return USE_INTERNAL_IMAGE_VIEWER_VIRT_FS;
    }

    @Override
    public LocationShortcutWidgetInfo getLocationShortcutWidgetInfo(int widgetId)
    {
        return null;
    }

    @Override
    public void setLocationShortcutWidgetInfo(int widgetId, LocationShortcutWidgetInfo info)
    {
    }

    @Override
    public boolean forceTempFiles()
    {
        return true;
    }

    @Override
    public boolean disableLargeSceenLayouts()
    {
        return false;
    }

    @Override
    public int getFilesSortMode()
    {
        return FB_SORT_FILENAME_ASC;
    }

    @Override
    public int getMaxContainerInactivityTime()
    {
        return -1;
    }

    @Override
    public boolean showPreviews()
    {
        return true;
    }

    @Override
    public String getExtensionsMimeMapString()
    {
        return "";
    }

    @Override
    public boolean isImageViewerFullScreenModeEnabled()
    {
        return false;
    }

    @Override
    public boolean isImageViewerAutoZoomEnabled()
    {
        return false;
    }

    @Override
    public boolean neverSaveHistory()
    {
        return false;
    }

    @Override
    public boolean disableDebugLog()
    {
        return false;
    }

    @Override
    public List<String> getVisitedHintSections()
    {
        return Arrays.asList(new String[0]);
    }

    @Override
    public boolean disableModifiedFilesBackup()
    {
        return false;
    }

    @Override
    public SecureBuffer getSettingsProtectionKey() throws InvalidSettingsPassword
    {
        return null;
    }

    @Override
    public int getCurrentSettingsVersion()
    {
        return Settings.VERSION;
    }

    @Override
    public boolean isFlagSecureEnabled()
    {
        return true;
    }

    @Override
    public boolean alwaysForceClose()
    {
        return false;
    }

    @Override
    public int getCurrentTheme()
    {
        return THEME_DEFAULT;
    }

    @Override
    public ExternalFileManagerInfo getExternalFileManagerInfo()
    {
        return null;
    }

    @Override
    public boolean dontUseContentProvider()
    {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.N;
    }
}
