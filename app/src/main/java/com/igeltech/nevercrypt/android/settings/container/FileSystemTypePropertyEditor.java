package com.igeltech.nevercrypt.android.settings.container;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.locations.fragments.CreateContainerFragmentBase;
import com.igeltech.nevercrypt.android.locations.tasks.CreateContainerTaskFragmentBase;
import com.igeltech.nevercrypt.android.settings.ChoiceDialogPropertyEditor;
import com.igeltech.nevercrypt.fs.FileSystemInfo;

import java.util.ArrayList;
import java.util.List;

public class FileSystemTypePropertyEditor extends ChoiceDialogPropertyEditor
{
    private final String _fileSystemTypeKey;

    /**
     * Creates the default file system editor for the outer volume.
     */
    public FileSystemTypePropertyEditor(CreateContainerFragmentBase createContainerFragment)
    {
        this(createContainerFragment, R.string.file_system_type, CreateContainerTaskFragmentBase.ARG_FILE_SYSTEM_TYPE);
    }

    /**
     * Creates a file system editor that stores its value under the supplied state key.
     */
    public FileSystemTypePropertyEditor(CreateContainerFragmentBase createContainerFragment, int titleResId, String fileSystemTypeKey)
    {
        super(createContainerFragment, titleResId, 0, createContainerFragment.getTag());
        _fileSystemTypeKey = fileSystemTypeKey;
    }

    @Override
    protected int loadValue()
    {
        List<String> names = getEntries();
        FileSystemInfo cur = getHostFragment().
                getState().
                getParcelable(_fileSystemTypeKey);
        if (cur != null)
            return names.indexOf(cur.getFileSystemName());
        else if (!names.isEmpty())
            return 0;
        else
            return -1;
    }

    @Override
    protected void saveValue(int value)
    {
        List<FileSystemInfo> fs = FileSystemInfo.getSupportedFileSystems();
        FileSystemInfo selected = fs.get(value);
        getHostFragment().
                getState().
                putParcelable(_fileSystemTypeKey, selected);
    }

    @Override
    protected ArrayList<String> getEntries()
    {
        ArrayList<String> res = new ArrayList<>();
        List<FileSystemInfo> supportedFS = FileSystemInfo.getSupportedFileSystems();
        if (supportedFS != null)
        {
            for (FileSystemInfo fsInfo : supportedFS)
                res.add(fsInfo.getFileSystemName());
        }
        return res;
    }

    protected CreateContainerFragmentBase getHostFragment()
    {
        return (CreateContainerFragmentBase) getHost();
    }
}
