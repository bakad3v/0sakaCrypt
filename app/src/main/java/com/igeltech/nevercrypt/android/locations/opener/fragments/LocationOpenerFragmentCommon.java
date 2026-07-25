package com.igeltech.nevercrypt.android.locations.opener.fragments;

import android.os.Bundle;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.dialogs.PasswordDialog;
import com.igeltech.nevercrypt.android.dialogs.PasswordDialogBase;
import com.igeltech.nevercrypt.android.errors.WrongPasswordOrBadContainerException;
import com.igeltech.nevercrypt.android.fragments.TaskFragment;
import com.igeltech.nevercrypt.crypto.SecureBuffer;
import com.igeltech.nevercrypt.exceptions.WrongPasswordException;
import com.igeltech.nevercrypt.locations.ContainerLocation;
import com.igeltech.nevercrypt.locations.Location;
import com.igeltech.nevercrypt.locations.LocationsManager;
import com.igeltech.nevercrypt.locations.Openable;

public class LocationOpenerFragmentCommon extends LocationOpenerBaseFragment implements PasswordDialog.PasswordReceiver
{
    @Override
    public void onPasswordEntered(PasswordDialog dlg)
    {
        usePassword(getPasswordDialogResultBundle(dlg));
    }

    @Override
    public void onPasswordNotEntered(PasswordDialog dlg)
    {
        finishOpener(false, getTargetLocation());
    }

    @Override
    protected TaskFragment getOpenLocationTask()
    {
        return new OpenLocationTaskFragment();
    }

    protected Bundle getAskPasswordArgs()
    {
        Bundle args = new Bundle();
        args.putString(PasswordDialogBase.ARG_LABEL, getResources().getString(R.string.opening_container));
        args.putString(PasswordDialogBase.ARG_RECEIVER_FRAGMENT_TAG, getTag());
        Openable loc = getTargetLocation();
        LocationsManager.storePathsInBundle(args, loc, null);
        return args;
    }

    @Override
    protected Openable getTargetLocation()
    {
        return (Openable) super.getTargetLocation();
    }

    @Override
    protected void openLocation()
    {
        Openable ol = getTargetLocation();
        Bundle defaultArgs = getArguments();
        if (defaultArgs == null)
            defaultArgs = new Bundle();
        if (ol.isOpen())
            super.openLocation();
        else if (needPasswordDialog(ol, defaultArgs))
            askPassword();
        else
            startOpeningTask(initOpenLocationTaskParams(getTargetLocation()));
    }

    @Override
    protected Bundle initOpenLocationTaskParams(Location location)
    {
        Bundle args = super.initOpenLocationTaskParams(location);
        Bundle defaultArgs = getArguments();
        if (defaultArgs != null)
        {
            if (defaultArgs.containsKey(Openable.PARAM_PASSWORD) && !args.containsKey(Openable.PARAM_PASSWORD))
            {
                String val = defaultArgs.getString(Openable.PARAM_PASSWORD);
                if (val != null)
                    args.putParcelable(Openable.PARAM_PASSWORD, new SecureBuffer(val.toCharArray()));
            }
            if (defaultArgs.containsKey(Openable.PARAM_KDF_ITERATIONS) && !args.containsKey(Openable.PARAM_KDF_ITERATIONS))
                args.putInt(Openable.PARAM_KDF_ITERATIONS, defaultArgs.getInt(Openable.PARAM_KDF_ITERATIONS));
            // Carry one-shot container hints that were supplied directly to the opener fragment.
            copyStringParam(defaultArgs, args, Openable.PARAM_CIPHER_NAME, false);
            copyStringParam(defaultArgs, args, Openable.PARAM_CIPHER_MODE_NAME, false);
            copyStringParam(defaultArgs, args, Openable.PARAM_HASHING_ALG, false);
            // Hidden-volume protection is also one-shot and must preserve SecureBuffer ownership.
            copyBooleanParam(defaultArgs, args, Openable.PARAM_PROTECT_HIDDEN_VOLUME, false);
            copySecureBufferParam(defaultArgs, args, Openable.PARAM_HIDDEN_VOLUME_PASSWORD, false);
        }
        return args;
    }

    protected void usePassword(Bundle passwordDialogResultBundle)
    {
        Bundle args = initOpenLocationTaskParams(getTargetLocation());
        updateOpenLocationTaskParams(args, passwordDialogResultBundle);
        startOpeningTask(args);
    }

    protected void updateOpenLocationTaskParams(Bundle args, Bundle passwordDialogResultBundle)
    {
        if (passwordDialogResultBundle.containsKey(Openable.PARAM_PASSWORD))
        {
            SecureBuffer sb = passwordDialogResultBundle.getParcelable(Openable.PARAM_PASSWORD);
            if (sb != null && (sb.length() > 0 || !args.containsKey(Openable.PARAM_PASSWORD)))
                args.putParcelable(Openable.PARAM_PASSWORD, sb);
        }
        if (passwordDialogResultBundle.containsKey(Openable.PARAM_KDF_ITERATIONS))
            args.putInt(Openable.PARAM_KDF_ITERATIONS, passwordDialogResultBundle.getInt(Openable.PARAM_KDF_ITERATIONS));
        // User-selected options from the password dialog must replace any opener defaults.
        copyStringParam(passwordDialogResultBundle, args, Openable.PARAM_CIPHER_NAME, true);
        copyStringParam(passwordDialogResultBundle, args, Openable.PARAM_CIPHER_MODE_NAME, true);
        copyStringParam(passwordDialogResultBundle, args, Openable.PARAM_HASHING_ALG, true);
        // Dialog options override defaults, including protection state and hidden password.
        copyBooleanParam(passwordDialogResultBundle, args, Openable.PARAM_PROTECT_HIDDEN_VOLUME, true);
        copySecureBufferParam(passwordDialogResultBundle, args, Openable.PARAM_HIDDEN_VOLUME_PASSWORD, true);
    }

    /**
     * Copies an optional string parameter while preserving explicit empty values used to clear hints.
     */
    private void copyStringParam(Bundle src, Bundle dst, String key, boolean overwrite)
    {
        if (src.containsKey(key) && (overwrite || !dst.containsKey(key)))
        {
            String val = src.getString(key);
            if (val != null)
                dst.putString(key, val);
            else if (overwrite)
                dst.remove(key);
        }
    }

    /**
     * Copies a one-shot boolean option while respecting the caller's overwrite policy.
     */
    private void copyBooleanParam(Bundle src, Bundle dst, String key, boolean overwrite)
    {
        if (src.containsKey(key) && (overwrite || !dst.containsKey(key)))
            dst.putBoolean(key, src.getBoolean(key));
    }

    /**
     * Copies a SecureBuffer by reference so password bytes are not converted to immutable strings.
     */
    private void copySecureBufferParam(Bundle src, Bundle dst, String key, boolean overwrite)
    {
        if (src.containsKey(key) && (overwrite || !dst.containsKey(key)))
        {
            SecureBuffer val = src.getParcelable(key);
            if (val != null)
                dst.putParcelable(key, val);
            else if (overwrite)
                dst.remove(key);
        }
    }

    protected void askPassword()
    {
        PasswordDialog pd = new PasswordDialog();
        pd.setArguments(getAskPasswordArgs());
        pd.show(getFragmentManager(), PasswordDialog.TAG);
    }

    protected boolean needPasswordDialog(Openable ol, Bundle defaultArgs)
    {
        if (defaultArgs == null)
            defaultArgs = new Bundle();
        return (ol.requirePassword() && !defaultArgs.containsKey(Openable.PARAM_PASSWORD)) || (ol.requireCustomKDFIterations() && !defaultArgs.containsKey(Openable.PARAM_KDF_ITERATIONS));
    }

    protected Bundle getPasswordDialogResultBundle(PasswordDialog pd)
    {
        Bundle res = new Bundle();
        res.putAll(pd.getOptions());
        res.putParcelable(Openable.PARAM_PASSWORD, new SecureBuffer(pd.getPassword()));
        return res;
    }

    public static class OpenLocationTaskFragment extends LocationOpenerBaseFragment.OpenLocationTaskFragment
    {
        @Override
        protected void procLocation(TaskState state, Location location, Bundle param) throws Exception
        {
            try
            {
                openLocation((Openable) location, param);
                regLocation((Openable) location);
            }
            catch (WrongPasswordException e)
            {
                throw new WrongPasswordOrBadContainerException(_context);
            }
            super.procLocation(state, location, param);
        }

        protected void openLocation(Openable location, Bundle param) throws Exception
        {
            if (location.isOpen())
                return;
            location.setOpeningProgressReporter(_openingProgressReporter);
            if (param.containsKey(Openable.PARAM_PASSWORD))
                location.setPassword(param.getParcelable(Openable.PARAM_PASSWORD));
            location.setNumKDFIterations(param.getInt(Openable.PARAM_KDF_ITERATIONS, 0));
            // Container-specific one-shot hints are applied immediately before opening.
            if (location instanceof ContainerLocation)
            {
                ContainerLocation containerLocation = (ContainerLocation) location;
                containerLocation.setOpeningEncryptionEngineHint(param.getString(Openable.PARAM_CIPHER_NAME), param.getString(Openable.PARAM_CIPHER_MODE_NAME));
                containerLocation.setOpeningHashFuncHint(param.getString(Openable.PARAM_HASHING_ALG));
                // Pass protection options immediately before open(); the container location owns cleanup.
                containerLocation.setHiddenVolumeProtection(param.getBoolean(Openable.PARAM_PROTECT_HIDDEN_VOLUME, false), param.getParcelable(Openable.PARAM_HIDDEN_VOLUME_PASSWORD));
            }
            location.open();
        }

        protected void regLocation(Openable location)
        {
            _locationsManager.regOpenedLocation(location);
        }
    }
}
