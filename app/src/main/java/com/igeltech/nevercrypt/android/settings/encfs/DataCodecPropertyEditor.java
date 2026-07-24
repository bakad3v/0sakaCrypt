package com.igeltech.nevercrypt.android.settings.encfs;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.locations.fragments.CreateLocationFragment;
import com.igeltech.nevercrypt.android.settings.views.PropertiesView;
import com.igeltech.nevercrypt.fs.encfs.AlgInfo;
import com.igeltech.nevercrypt.fs.encfs.FS;
import com.igeltech.nevercrypt.locations.Openable;

public class DataCodecPropertyEditor extends CodecInfoPropertyEditor
{
    public static final int ID = PropertiesView.newId();

    public DataCodecPropertyEditor(CreateLocationFragment hostFragment)
    {
        super(hostFragment, R.string.encryption_algorithm, 0);
        setId(ID);
    }

    @Override
    protected Iterable<? extends AlgInfo> getCodecs()
    {
        return FS.getSupportedDataCodecs();
    }

    @Override
    protected String getParamName()
    {
        return Openable.PARAM_CIPHER_NAME;
    }
}
