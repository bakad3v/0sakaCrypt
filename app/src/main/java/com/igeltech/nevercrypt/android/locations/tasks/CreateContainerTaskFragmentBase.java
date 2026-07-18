package com.igeltech.nevercrypt.android.locations.tasks;

import android.os.Bundle;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.errors.InputOutputException;
import com.igeltech.nevercrypt.android.errors.UserException;
import com.igeltech.nevercrypt.android.errors.WrongPasswordOrBadContainerException;
import com.igeltech.nevercrypt.container.Container;
import com.igeltech.nevercrypt.container.ContainerFormatInfo;
import com.igeltech.nevercrypt.container.ContainerFormatter;
import com.igeltech.nevercrypt.container.ContainerFormatterBase;
import com.igeltech.nevercrypt.container.HiddenVolumeFormatter;
import com.igeltech.nevercrypt.container.LocationFormatter;
import com.igeltech.nevercrypt.crypto.SecureBuffer;
import com.igeltech.nevercrypt.fs.FileSystemInfo;
import com.igeltech.nevercrypt.fs.Path;
import com.igeltech.nevercrypt.fs.errors.WrongImageFormatException;
import com.igeltech.nevercrypt.locations.Location;
import com.igeltech.nevercrypt.locations.Openable;

import java.io.IOException;

public abstract class CreateContainerTaskFragmentBase extends CreateLocationTaskFragment
{
    public static final String ARG_CONTAINER_FORMAT = "com.igeltech.nevercrypt.android.CONTAINER_FORMAT";
    public static final String ARG_CIPHER_MODE_NAME = "com.igeltech.nevercrypt.android.CIPHER_MODE_NAME";
    public static final String ARG_HASHING_ALG = "com.igeltech.nevercrypt.android.HASHING_ALG";
    public static final String ARG_SIZE = "com.igeltech.nevercrypt.android.SIZE";
    public static final String ARG_FILL_FREE_SPACE = "com.igeltech.nevercrypt.android.FILL_FREE_SPACE";
    public static final String ARG_FILE_SYSTEM_TYPE = "com.igeltech.nevercrypt.android.FILE_SYSTEM_TYPE";
    public static final String ARG_SAVE_VOLUME_SETTINGS = "com.igeltech.nevercrypt.android.SAVE_VOLUME_SETTINGS";
    public static final String ARG_CREATE_HIDDEN_VOLUME = "com.igeltech.nevercrypt.android.CREATE_HIDDEN_VOLUME";
    public static final String ARG_HIDDEN_PASSWORD = "com.igeltech.nevercrypt.android.HIDDEN_PASSWORD";
    public static final String ARG_HIDDEN_SIZE = "com.igeltech.nevercrypt.android.HIDDEN_SIZE";
    public static final String ARG_HIDDEN_CIPHER_NAME = "com.igeltech.nevercrypt.android.HIDDEN_CIPHER_NAME";
    public static final String ARG_HIDDEN_CIPHER_MODE_NAME = "com.igeltech.nevercrypt.android.HIDDEN_CIPHER_MODE_NAME";
    public static final String ARG_HIDDEN_HASHING_ALG = "com.igeltech.nevercrypt.android.HIDDEN_HASHING_ALG";
    public static final String ARG_HIDDEN_FILE_SYSTEM_TYPE = "com.igeltech.nevercrypt.android.HIDDEN_FILE_SYSTEM_TYPE";
    public static final String ARG_HIDDEN_KDF_ITERATIONS = "com.igeltech.nevercrypt.android.HIDDEN_KDF_ITERATIONS";
    public static final String ARG_HIDDEN_VOLUME_STAGE = "com.igeltech.nevercrypt.android.HIDDEN_VOLUME_STAGE";
    public static final String ARG_MAX_HIDDEN_VOLUME_SIZE = "com.igeltech.nevercrypt.android.MAX_HIDDEN_VOLUME_SIZE";
    public static final int RESULT_REQUEST_HIDDEN_VOLUME_SETTINGS = 2;

    public static ContainerFormatInfo getContainerFormatByName(String name)
    {
        for (ContainerFormatInfo ci : Container.getSupportedFormats())
            if (ci.getFormatName().equals(name))
                return ci;
        return null;
    }

    @Override
    protected LocationFormatter createFormatter()
    {
        return new ContainerFormatter();
    }

    @Override
    protected void initFormatter(TaskState state, LocationFormatter formatter, SecureBuffer password) throws Exception
    {
        super.initFormatter(state, formatter, password);
        Bundle args = getArguments();
        ContainerFormatterBase cf = (ContainerFormatterBase) formatter;
        cf.setContainerFormat(getContainerFormatByName(args.getString(ARG_CONTAINER_FORMAT)));
        cf.setContainerSize(args.getInt(ARG_SIZE) * 1024L * 1024L);
        cf.setNumKDFIterations(args.getInt(Openable.PARAM_KDF_ITERATIONS, 0));
        FileSystemInfo fst = args.getParcelable(ARG_FILE_SYSTEM_TYPE);
        if (fst != null)
            cf.setFileSystemType(fst);
        String encAlgName = args.getString(ARG_CIPHER_NAME);
        String encModeName = args.getString(ARG_CIPHER_MODE_NAME);
        if (encAlgName != null && encModeName != null)
            cf.setEncryptionEngine(encAlgName, encModeName);
        String hashAlgName = args.getString(ARG_HASHING_ALG);
        if (hashAlgName != null)
            cf.setHashFunc(hashAlgName);
        boolean createHiddenVolume = args.getBoolean(ARG_CREATE_HIDDEN_VOLUME, false);
        cf.setSaveVolumeSettings(args.getBoolean(ARG_SAVE_VOLUME_SETTINGS, false));
        if (cf instanceof ContainerFormatter)
            ((ContainerFormatter) cf).setCreateOuterVolumeForHiddenVolume(createHiddenVolume);
        cf.enableFreeSpaceRand(args.getBoolean(ARG_FILL_FREE_SPACE) || createHiddenVolume);
    }

    /**
     * Creates the outer volume and requests the hidden-volume settings step when needed.
     */
    @Override
    protected void createEDSLocation(TaskState state, Location locationLocation) throws Exception
    {
        Bundle args = getArguments();
        SecureBuffer password = args.getParcelable(Openable.PARAM_PASSWORD);
        runContainerOperation(() -> {
            ContainerFormatter cf = (ContainerFormatter) createFormatter();
            initFormatter(state, cf, password);
            cf.format(locationLocation);
            if (args.getBoolean(ARG_CREATE_HIDDEN_VOLUME, false))
            {
                HiddenVolumeFormatter maxSizeFormatter = new HiddenVolumeFormatter();
                initFormatter(state, maxSizeFormatter, password);
                long maxHiddenVolumeSize = maxSizeFormatter.calculateMaxHiddenVolumeSize(locationLocation);
                args.putLong(ARG_MAX_HIDDEN_VOLUME_SIZE, maxHiddenVolumeSize);
                args.remove(ARG_HIDDEN_SIZE);
                state.setResult(RESULT_REQUEST_HIDDEN_VOLUME_SETTINGS);
            }
        });
    }

    /**
     * Converts formatter errors to the user-facing exceptions expected by the task UI.
     */
    protected void runContainerOperation(ContainerOperation operation) throws Exception
    {
        try
        {
            operation.run();
        }
        catch (UserException e)
        {
            throw e;
        }
        catch (WrongImageFormatException e)
        {
            WrongPasswordOrBadContainerException e1 = new WrongPasswordOrBadContainerException(_context);
            e1.initCause(e);
            throw e1;
        }
        catch (IOException e)
        {
            throw new InputOutputException(_context, e);
        }
        catch (Exception e)
        {
            throw new UserException(_context, R.string.err_failed_creating_container, e);
        }
    }

    @Override
    protected boolean checkParams(TaskState state, Location locationLocation) throws Exception
    {
        Bundle args = getArguments();
        Path path = locationLocation.getCurrentPath();
        if (path.exists() && path.isDirectory())
            throw new UserException(_context, R.string.container_file_name_is_not_specified);
        ContainerFormatInfo containerFormat = getContainerFormatByName(args.getString(ARG_CONTAINER_FORMAT));
        if (args.getBoolean(ARG_CREATE_HIDDEN_VOLUME, false) && (containerFormat == null || !containerFormat.hasHiddenContainerSupport()))
            throw new UserException(getActivity(), R.string.err_hidden_volume_is_not_supported);
        if (args.getInt(ARG_SIZE) < 1)
            throw new UserException(getActivity(), R.string.err_container_size_is_too_small);
        if (!getArguments().getBoolean(ARG_OVERWRITE, false))
        {
            if (path.exists() && path.isFile() && path.getFile().getSize() > 0)
            {
                state.setResult(RESULT_REQUEST_OVERWRITE);
                return false;
            }
        }
        return true;
    }

    protected interface ContainerOperation
    {
        void run() throws Exception;
    }
}
