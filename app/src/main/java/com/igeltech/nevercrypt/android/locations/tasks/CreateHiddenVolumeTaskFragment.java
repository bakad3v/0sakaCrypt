package com.igeltech.nevercrypt.android.locations.tasks;

import android.os.Bundle;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.errors.UserException;
import com.igeltech.nevercrypt.container.ContainerFormatInfo;
import com.igeltech.nevercrypt.container.HiddenVolumeFormatter;
import com.igeltech.nevercrypt.container.LocationFormatter;
import com.igeltech.nevercrypt.crypto.SecureBuffer;
import com.igeltech.nevercrypt.fs.FileSystemInfo;
import com.igeltech.nevercrypt.fs.Path;
import com.igeltech.nevercrypt.locations.Location;
import com.igeltech.nevercrypt.locations.Openable;

public class CreateHiddenVolumeTaskFragment extends CreateContainerTaskFragmentBase
{
    /**
     * Creates the formatter responsible only for adding a hidden volume to an existing outer container.
     */
    @Override
    protected LocationFormatter createFormatter()
    {
        return new HiddenVolumeFormatter();
    }

    /**
     * Applies both the outer-volume credentials and the hidden-volume settings.
     */
    @Override
    protected void initFormatter(TaskState state, LocationFormatter formatter, SecureBuffer password) throws Exception
    {
        super.initFormatter(state, formatter, password);
        Bundle args = getArguments();
        HiddenVolumeFormatter cf = (HiddenVolumeFormatter) formatter;
        cf.setHiddenPassword(args.getParcelable(ARG_HIDDEN_PASSWORD));
        cf.setHiddenVolumeSize(args.getInt(ARG_HIDDEN_SIZE, 0) * 1024L * 1024L);
        cf.setHiddenNumKDFIterations(args.getInt(ARG_HIDDEN_KDF_ITERATIONS, 0));
        FileSystemInfo hiddenFs = args.getParcelable(ARG_HIDDEN_FILE_SYSTEM_TYPE);
        if (hiddenFs != null)
            cf.setHiddenFileSystemType(hiddenFs);
        String encAlgName = args.getString(ARG_HIDDEN_CIPHER_NAME);
        String encModeName = args.getString(ARG_HIDDEN_CIPHER_MODE_NAME);
        if (encAlgName != null && encModeName != null)
            cf.setHiddenEncryptionEngine(encAlgName, encModeName);
        String hashAlgName = args.getString(ARG_HIDDEN_HASHING_ALG);
        if (hashAlgName != null)
            cf.setHiddenHashFunc(hashAlgName);
    }

    /**
     * Writes the hidden volume into the already-created outer container.
     */
    @Override
    protected void createEDSLocation(TaskState state, Location locationLocation) throws Exception
    {
        Bundle args = getArguments();
        SecureBuffer password = args.getParcelable(Openable.PARAM_PASSWORD);
        runContainerOperation(() -> {
            HiddenVolumeFormatter cf = (HiddenVolumeFormatter) createFormatter();
            initFormatter(state, cf, password);
            cf.formatHiddenVolume(locationLocation);
        });
    }

    /**
     * Verifies only the existing outer container before writing the hidden volume.
     */
    @Override
    protected boolean checkParams(TaskState state, Location locationLocation) throws Exception
    {
        Bundle args = getArguments();
        Path path = locationLocation.getCurrentPath();
        ContainerFormatInfo containerFormat = getContainerFormatByName(args.getString(ARG_CONTAINER_FORMAT));
        if (containerFormat == null || !containerFormat.hasHiddenContainerSupport())
            throw new UserException(getActivity(), R.string.err_hidden_volume_is_not_supported);
        if (!path.exists() || !path.isFile())
            throw new UserException(_context, R.string.err_failed_opening_container);
        return true;
    }
}
