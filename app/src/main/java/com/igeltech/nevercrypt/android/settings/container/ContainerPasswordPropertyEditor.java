package com.igeltech.nevercrypt.android.settings.container;

import android.os.Bundle;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.dialogs.PasswordDialog;
import com.igeltech.nevercrypt.android.locations.fragments.CreateLocationFragment;
import com.igeltech.nevercrypt.android.settings.ButtonPropertyEditor;
import com.igeltech.nevercrypt.android.settings.PropertyEditor;
import com.igeltech.nevercrypt.crypto.SecureBuffer;
import com.igeltech.nevercrypt.locations.Openable;

public class ContainerPasswordPropertyEditor extends ButtonPropertyEditor implements PasswordDialog.PasswordReceiver
{
    private final String _passwordKey;

    /**
     * Creates the default password editor for the outer container.
     */
    public ContainerPasswordPropertyEditor(CreateLocationFragment createLocationFragment)
    {
        this(createLocationFragment, R.string.container_password, Openable.PARAM_PASSWORD);
    }

    /**
     * Creates a password editor that stores its value under the supplied state key.
     */
    public ContainerPasswordPropertyEditor(CreateLocationFragment createLocationFragment, int titleResId, String passwordKey)
    {
        super(createLocationFragment, titleResId, 0, R.string.change);
        _passwordKey = passwordKey;
    }

    @Override
    public void onPasswordEntered(PasswordDialog dlg)
    {
        getHostFragment().getState().putParcelable(_passwordKey, new SecureBuffer(dlg.getPassword()));
    }

    @Override
    public void onPasswordNotEntered(PasswordDialog dlg)
    {
    }

    @Override
    protected void onButtonClick()
    {
        Bundle args = new Bundle();
        args.putBoolean(PasswordDialog.ARG_HAS_PASSWORD, true);
        args.putBoolean(PasswordDialog.ARG_VERIFY_PASSWORD, true);
        args.putInt(PropertyEditor.ARG_PROPERTY_ID, getId());
        args.putString(PasswordDialog.ARG_RECEIVER_FRAGMENT_TAG, getHostFragment().getTag());
        PasswordDialog pd = new PasswordDialog();
        pd.setArguments(args);
        pd.show(getHost().getFragmentManager(), PasswordDialog.TAG);
    }

    CreateLocationFragment getHostFragment()
    {
        return (CreateLocationFragment) getHost();
    }
}
