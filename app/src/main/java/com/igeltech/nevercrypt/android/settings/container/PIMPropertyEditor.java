package com.igeltech.nevercrypt.android.settings.container;

import androidx.fragment.app.Fragment;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.settings.IntPropertyEditor;
import com.igeltech.nevercrypt.android.settings.PropertiesHostWithStateBundle;
import com.igeltech.nevercrypt.locations.Openable;

public class PIMPropertyEditor extends IntPropertyEditor
{
    private final String _pimKey;

    /**
     * Creates the default VeraCrypt PIM editor for the outer volume.
     */
    public PIMPropertyEditor(PropertiesHostWithStateBundle hostFragment)
    {
        this(hostFragment, R.string.kdf_iterations_multiplier, Openable.PARAM_KDF_ITERATIONS);
    }

    /**
     * Creates a VeraCrypt PIM editor that stores its value under the supplied state key.
     */
    public PIMPropertyEditor(PropertiesHostWithStateBundle hostFragment, int titleResId, String pimKey)
    {
        super(hostFragment, titleResId, R.string.number_of_kdf_iterations_veracrypt_descr, ((Fragment) hostFragment).getTag());
        _pimKey = pimKey;
    }

    @Override
    public PropertiesHostWithStateBundle getHost()
    {
        return (PropertiesHostWithStateBundle) super.getHost();
    }

    @Override
    protected int loadValue()
    {
        int val = getHost().getState().getInt(_pimKey, 0);
        return val < 0 ? 0 : val;
    }

    @Override
    protected void saveValue(int value)
    {
        if (value < 0)
            value = 0;
        else if (value > 100000)
            value = 100000;
        getHost().getState().putInt(_pimKey, value);
    }
}
