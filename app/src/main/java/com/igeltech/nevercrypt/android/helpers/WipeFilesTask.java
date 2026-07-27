package com.igeltech.nevercrypt.android.helpers;

import com.igeltech.nevercrypt.fs.File;
import com.igeltech.nevercrypt.fs.Path;
import com.igeltech.nevercrypt.fs.util.SrcDstCollection;
import com.igeltech.nevercrypt.fs.util.SrcDstCollection.SrcDst;

import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;

public class WipeFilesTask
{
    protected final boolean _wipe;

    public WipeFilesTask(boolean wipe)
    {
        _wipe = wipe;
    }

    static public void wipeFileRnd(File file) throws IOException
    {
        wipeFileRnd(file, null);
    }

    public static void wipeFileRnd(File file, ITask task) throws IOException
    {
        SecureRandom rg = new SecureRandom();
        try
        {
            byte[] buf = new byte[4 * 1024];
            long l = file.getSize();
            try (OutputStream s = file.getOutputStream())
            {
                for (long i = 0; i < l; i += buf.length)
                {
                    if (task != null && task.cancel())
                        return;
                    rg.nextBytes(buf);
                    s.write(buf);
                    long tmp = l - i - buf.length;
                    updStatus(task, tmp < 0 ? buf.length + tmp : buf.length);
                }
                s.flush();
            }
        }
        finally
        {
            file.delete();
        }
    }

    public static void wipeFilesRnd(ITask task, Object syncer, boolean wipe, SrcDstCollection... records) throws IOException
    {
        for (SrcDstCollection col : records)
        {
            if (col != null)
            {
                for (SrcDst rec : col)
                {
                    if (task != null && task.cancel())
                        return;
                    Path p = rec.getSrcLocation().getCurrentPath();
                    if (p.isFile())
                    {
                        if (syncer != null)
                        {
                            synchronized (syncer)
                            {
                                wipeFile(p.getFile(), wipe, task);
                            }
                        }
                        else
                            wipeFile(p.getFile(), wipe, task);
                    }
                    else if (p.isDirectory())
                        p.getDirectory().delete();
                }
            }
        }
    }

    public static void wipeFile(File file, boolean wipe, ITask task) throws IOException
    {
        if (wipe)
            wipeFileRnd(file, task);
        else
        {
            file.delete();
            updStatus(task, 0);
        }
    }

    protected static void updStatus(ITask task, long sizeInc)
    {
        if (task == null)
            return;
        task.progress((int) sizeInc);
    }

    protected ITask getITask()
    {
        return new ITask()
        {
            @Override
            public boolean cancel()
            {
                return false;
            }

            @Override
            public void progress(int sizeInc)
            {
            }
        };
    }

    protected void doWork(SrcDstCollection... records) throws Exception
    {
        wipeFilesRnd(getITask(), null, _wipe, records);
    }

    public interface ITask
    {
        boolean cancel();

        void progress(int sizeInc);
    }
}
