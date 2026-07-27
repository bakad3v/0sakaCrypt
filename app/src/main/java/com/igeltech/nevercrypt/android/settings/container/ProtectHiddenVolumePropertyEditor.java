package com.igeltech.nevercrypt.android.settings.container;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.settings.CheckBoxPropertyEditor;
import com.igeltech.nevercrypt.android.settings.fragments.OpeningOptionsFragmentBase;
import com.igeltech.nevercrypt.crypto.SecureBuffer;
import com.igeltech.nevercrypt.locations.Openable;

/**
 * One-shot checkbox that enables outer-volume write limiting before opening the container.
 */
public class ProtectHiddenVolumePropertyEditor extends CheckBoxPropertyEditor
{
    public ProtectHiddenVolumePropertyEditor(OpeningOptionsFragmentBase hostFragment)
    {
        super(hostFragment, R.string.protect_hidden_volume, R.string.protect_hidden_volume_desc);
    }

    @Override
    public OpeningOptionsFragmentBase getHost()
    {
        return (OpeningOptionsFragmentBase) super.getHost();
    }

    @Override
    protected boolean loadValue()
    {
        return getHost().getState().getBoolean(Openable.PARAM_PROTECT_HIDDEN_VOLUME, false);
    }

    @Override
    protected void saveValue(boolean value)
    {
        getHost().getState().putBoolean(Openable.PARAM_PROTECT_HIDDEN_VOLUME, value);
        if (!value)
        {
            // Disabling protection cancels the hidden password immediately.
            SecureBuffer hiddenPassword = getHost().getState().getParcelable(Openable.PARAM_HIDDEN_VOLUME_PASSWORD);
            if (hiddenPassword != null)
                hiddenPassword.close();
            getHost().getState().remove(Openable.PARAM_HIDDEN_VOLUME_PASSWORD);
            getHost().getState().remove(Openable.PARAM_HIDDEN_VOLUME_KDF_ITERATIONS);
            getHost().getState().remove(Openable.PARAM_HIDDEN_VOLUME_CIPHER_NAME);
            getHost().getState().remove(Openable.PARAM_HIDDEN_VOLUME_CIPHER_MODE_NAME);
            getHost().getState().remove(Openable.PARAM_HIDDEN_VOLUME_HASHING_ALG);
        }
    }

    @Override
    protected void onChecked(boolean isChecked)
    {
        super.onChecked(isChecked);
        getHost().updateHiddenVolumeProtectionProperties();
    }
}
