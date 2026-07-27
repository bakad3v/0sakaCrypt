package com.igeltech.nevercrypt.android.settings.container;

import android.os.Bundle;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.dialogs.PasswordDialog;
import com.igeltech.nevercrypt.android.settings.ButtonPropertyEditor;
import com.igeltech.nevercrypt.android.settings.PropertyEditor;
import com.igeltech.nevercrypt.android.settings.fragments.OpeningOptionsFragmentBase;
import com.igeltech.nevercrypt.crypto.SecureBuffer;
import com.igeltech.nevercrypt.locations.Openable;

/**
 * One-shot editor for the hidden-volume password used only to calculate the protected outer size.
 */
public class HiddenVolumePasswordPropertyEditor extends ButtonPropertyEditor implements PasswordDialog.PasswordReceiver
{
    public HiddenVolumePasswordPropertyEditor(OpeningOptionsFragmentBase hostFragment)
    {
        super(hostFragment, R.string.hidden_volume_password, 0, R.string.change);
    }

    @Override
    public OpeningOptionsFragmentBase getHost()
    {
        return (OpeningOptionsFragmentBase) super.getHost();
    }

    @Override
    public void onPasswordEntered(PasswordDialog dlg)
    {
        // Replace and erase any previous value so stale hidden passwords do not remain in memory.
        SecureBuffer oldPassword = getHost().getState().getParcelable(Openable.PARAM_HIDDEN_VOLUME_PASSWORD);
        if (oldPassword != null)
            oldPassword.close();
        getHost().getState().putParcelable(Openable.PARAM_HIDDEN_VOLUME_PASSWORD, new SecureBuffer(dlg.getPassword()));
    }

    @Override
    public void onPasswordNotEntered(PasswordDialog dlg)
    {
    }

    @Override
    public void save(Bundle b)
    {
        // Keep the SecureBuffer reference in the one-shot state; the opener will consume and wipe it.
        SecureBuffer hiddenPassword = getHost().getState().getParcelable(Openable.PARAM_HIDDEN_VOLUME_PASSWORD);
        if (hiddenPassword != null)
            b.putParcelable(Openable.PARAM_HIDDEN_VOLUME_PASSWORD, hiddenPassword);
    }

    @Override
    public void load(Bundle b)
    {
        // Restore only the in-memory SecureBuffer, never serialize the hidden password as text.
        SecureBuffer hiddenPassword = b.getParcelable(Openable.PARAM_HIDDEN_VOLUME_PASSWORD);
        if (hiddenPassword != null)
            getHost().getState().putParcelable(Openable.PARAM_HIDDEN_VOLUME_PASSWORD, hiddenPassword);
    }

    @Override
    protected void onButtonClick()
    {
        // Route PasswordDialog callbacks back to this property editor through the host fragment.
        Bundle args = new Bundle();
        args.putBoolean(PasswordDialog.ARG_HAS_PASSWORD, true);
        args.putInt(PropertyEditor.ARG_PROPERTY_ID, getId());
        args.putString(PasswordDialog.ARG_LABEL, getHost().getContext().getString(R.string.hidden_volume_password));
        args.putString(PasswordDialog.ARG_RECEIVER_FRAGMENT_TAG, getHost().getTag());
        PasswordDialog pd = new PasswordDialog();
        pd.setArguments(args);
        pd.show(getHost().getFragmentManager(), PasswordDialog.TAG);
    }
}
