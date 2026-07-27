package com.igeltech.nevercrypt.android.service;

import android.content.Intent;

import com.igeltech.nevercrypt.android.Logger;
import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.helpers.TempFilesMonitor;
import com.igeltech.nevercrypt.android.providers.MainContentProviderBase;
import com.igeltech.nevercrypt.android.settings.UserSettings;
import com.igeltech.nevercrypt.fs.Directory;
import com.igeltech.nevercrypt.fs.File;
import com.igeltech.nevercrypt.fs.Path;
import com.igeltech.nevercrypt.fs.util.SrcDstCollection;
import com.igeltech.nevercrypt.locations.Location;

import java.io.IOException;

class SaveTempFileChangesTask extends CopyFilesTask
{
    private static final String BAK_EXTENSION = ".cryptbak";
    private static final String TMP_EXTENSION = ".crypttmp";

    @Override
    protected int getNotificationMainTextId()
    {
        return R.string.saving_changes;
    }

    @Override
    protected boolean copyFile(SrcDstCollection.SrcDst record) throws IOException
    {
        if (super.copyFile(record))
        {
            Path dstPath = calcDstPath(record.getSrcLocation().getCurrentPath().getFile(), record.getDstLocation().getCurrentPath().getDirectory());
            if (dstPath != null && dstPath.isFile())
            {
                TempFilesMonitor.getMonitor(_context).updateMonitoredInfo(record.getSrcLocation(), dstPath.getFile().getLastModified());
                Location savedLocation = record.getDstLocation().copy();
                savedLocation.setCurrentPath(dstPath);
                MainContentProviderBase.notifyLocationChanged(_context, savedLocation);
            }
            return true;
        }
        else
            return false;
    }

    @Override
    protected boolean copyFile(File srcFile, Directory targetFolder) throws IOException
    {
        try
        {
            Path dstPath = calcDstPath(srcFile, targetFolder);
            if (dstPath != null && dstPath.exists() && shouldCreateBackup(dstPath.getFile()))
                deleteBackupCopy(dstPath.getFile(), targetFolder);
            File tmpFile = copyToTempFile(srcFile, targetFolder);
            if (dstPath != null && dstPath.exists())
                prepareBackupCopy(dstPath.getFile(), targetFolder);
            tmpFile.rename(srcFile.getName());
            return true;
        }
        catch (IOException e)
        {
            throw new IOException(_context.getText(R.string.err_failed_saving_changes).toString(), e);
        }
    }

    protected File copyToTempFile(File srcFile, Directory targetFolder) throws IOException
    {
        String tmpName = srcFile.getName() + TMP_EXTENSION;
        Path tmpPath = calcPath(targetFolder, tmpName);
        if (tmpPath != null && tmpPath.isFile())
            tmpPath.getFile().delete();
        File dstFile = targetFolder.createFile(tmpName);
        boolean copied = false;
        try
        {
            if (!super.copyFile(srcFile, dstFile))
                throw new IOException("Failed copying to temp file");
            copied = true;
            return dstFile;
        }
        finally
        {
            if (!copied)
                deleteTempFile(dstFile);
        }
    }

    protected void prepareBackupCopy(File dstFile, Directory targetFolder) throws IOException
    {
        if (shouldCreateBackup(dstFile))
        {
            deleteBackupCopy(dstFile, targetFolder);
            String bakName = getBackupName(dstFile);
            dstFile.rename(bakName);
        }
        else
            dstFile.delete();
    }

    private boolean shouldCreateBackup(File dstFile) throws IOException
    {
        return !UserSettings.getSettings(_context).disableModifiedFilesBackup() && dstFile.getSize() > 0;
    }

    private void deleteBackupCopy(File dstFile, Directory targetFolder) throws IOException
    {
        Path bakPath = calcPath(targetFolder, getBackupName(dstFile));
        if (bakPath != null && bakPath.isFile())
            bakPath.getFile().delete();
    }

    private String getBackupName(File dstFile) throws IOException
    {
        return dstFile.getName() + BAK_EXTENSION;
    }

    private void deleteTempFile(File tempFile)
    {
        try
        {
            tempFile.delete();
        }
        catch (IOException e)
        {
            Logger.log(e);
        }
    }

    @Override
    protected CopyFilesTaskParam initParam(Intent i)
    {
        return new CopyFilesTaskParam(i)
        {
            @Override
            public boolean forceOverwrite()
            {
                return true;
            }
        };
    }
}
