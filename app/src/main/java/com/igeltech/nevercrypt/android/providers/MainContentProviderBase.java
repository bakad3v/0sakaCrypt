package com.igeltech.nevercrypt.android.providers;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.igeltech.nevercrypt.android.Logger;
import com.igeltech.nevercrypt.android.filemanager.tasks.LoadPathInfoObservable;
import com.igeltech.nevercrypt.android.helpers.CachedPathInfo;
import com.igeltech.nevercrypt.android.helpers.ExtendedFileInfoLoader;
import com.igeltech.nevercrypt.android.helpers.TempFilesMonitor;
import com.igeltech.nevercrypt.android.helpers.WipeFilesTask;
import com.igeltech.nevercrypt.android.locations.PathsStore;
import com.igeltech.nevercrypt.android.providers.cursor.FSCursor;
import com.igeltech.nevercrypt.android.providers.cursor.SelectionChecker;
import com.igeltech.nevercrypt.android.service.FileOpsService;
import com.igeltech.nevercrypt.android.settings.UserSettings;
import com.igeltech.nevercrypt.fs.FSRecord;
import com.igeltech.nevercrypt.fs.File;
import com.igeltech.nevercrypt.fs.Path;
import com.igeltech.nevercrypt.fs.util.PathUtil;
import com.igeltech.nevercrypt.fs.util.SrcDstSingle;
import com.igeltech.nevercrypt.fs.util.StringPathUtil;
import com.igeltech.nevercrypt.fs.util.Util;
import com.igeltech.nevercrypt.locations.Location;
import com.igeltech.nevercrypt.locations.LocationsManager;
import com.igeltech.nevercrypt.settings.GlobalConfig;
import com.igeltech.nevercrypt.settings.SystemConfig;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

import static com.igeltech.nevercrypt.android.providers.cursor.FSCursorBase.COLUMN_ID;
import static com.igeltech.nevercrypt.android.providers.cursor.FSCursorBase.COLUMN_IS_FOLDER;
import static com.igeltech.nevercrypt.android.providers.cursor.FSCursorBase.COLUMN_LAST_MODIFIED;
import static com.igeltech.nevercrypt.android.providers.cursor.FSCursorBase.COLUMN_NAME;
import static com.igeltech.nevercrypt.android.providers.cursor.FSCursorBase.COLUMN_PATH;
import static com.igeltech.nevercrypt.android.providers.cursor.FSCursorBase.COLUMN_SIZE;
import static com.igeltech.nevercrypt.android.providers.cursor.FSCursorBase.COLUMN_TITLE;

public abstract class MainContentProviderBase extends ContentProvider
{
    public static final String COLUMN_LOCATION = "location";
    public static final String MIME_TYPE_FILE_META = "vnd.android.cursor.item/file";
    public static final String MIME_TYPE_FOLDER_META = "vnd.android.cursor.dir/folder";
    public static final String MIME_TYPE_SELECTION = "vnd.android.cursor.dir/selection";
    public static final String OPTION_OFFSET = "offset";
    public static final String OPTION_NUM_BYTES = "num_bytes";
    protected static final String[] ALL_SELECTION_COLUMNS = {COLUMN_LOCATION};
    protected static final String META_PATH = "fs";
    protected static final int META_PATH_CODE = 10;
    protected static final Uri META_URI = Uri.parse("content://" + MainContentProvider.AUTHORITY + "/" + META_PATH);
    protected static final String CONTENT_PATH = "content";
    protected static final int CONTENT_PATH_CODE = 20;
    protected static final Uri CONTENT_URI = Uri.parse("content://" + MainContentProvider.AUTHORITY + "/" + CONTENT_PATH);
    protected static final String CURRENT_SELECTION_PATH = "selection";

    /*
    public static Uri getCurrentSelectionUri()
    {
        return CURRENT_SELECTION_URI;
    }
    */
    protected static final int CURRENT_SELECTION_PATH_CODE = 30;
    protected static final Uri CURRENT_SELECTION_URI = Uri.parse("content://" + MainContentProvider.AUTHORITY + "/" + CURRENT_SELECTION_PATH);
    private static final UriMatcher _uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final String[] ALL_META_COLUMNS = {COLUMN_ID, COLUMN_NAME, COLUMN_TITLE, COLUMN_SIZE, COLUMN_LAST_MODIFIED, COLUMN_IS_FOLDER, COLUMN_PATH};
    private static final Cursor _emptyMetaCursor = new MatrixCursor(ALL_META_COLUMNS, 0);

    static
    {
        _uriMatcher.addURI(MainContentProvider.AUTHORITY, META_PATH + "/*", META_PATH_CODE);
        _uriMatcher.addURI(MainContentProvider.AUTHORITY, CONTENT_PATH + "/*", CONTENT_PATH_CODE);
        _uriMatcher.addURI(MainContentProvider.AUTHORITY, CURRENT_SELECTION_PATH, CURRENT_SELECTION_PATH_CODE);
    }

    private PathsStore _currentSelection;

    public static boolean hasSelectionInClipboard(ClipboardManager clipboard)
    {
        if (!clipboard.hasPrimaryClip())
        {
            Logger.debug("hasSelectionInClipboard: clipboard doesn't have a primary clip");
            return false;
        }
        ClipData clip = clipboard.getPrimaryClip();
        if (clip != null)
        {
            if (GlobalConfig.isDebug())
                Logger.debug(String.format("hasSelectionInClipboard: clip = %s", clip));
            return clip.getItemCount() > 0 && MainContentProvider.isClipboardUri(clip.getItemAt(0).getUri());
        }
        Logger.debug("hasSelectionInClipboard: clip = null");
        return false;
    }

    public static boolean isClipboardUri(Uri providerUri)
    {
        return providerUri != null && MainContentProvider.AUTHORITY.equals(providerUri.getHost()) && providerUri.getPathSegments().size() >= 2;
    }

    public static Location getLocationFromProviderUri(Context context, Uri providerUri)
    {
        try
        {
            return LocationsManager.getLocationsManager(context, true).
                    getLocation(getLocationUriFromProviderUri(providerUri));
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Wrong location uri", e);
        }
    }

    public static Uri getLocationUriFromProviderUri(Uri providerUri)
    {
        String path = providerUri.getPath();
        if (path == null)
            return null;
        String[] parts = new StringPathUtil(path).getComponents();
        if (parts.length < 2)
            return null;
        String encodedLocationUri = parts[1];
        byte[] locationUriBytes = Base64.decode(encodedLocationUri, Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);
        return Uri.parse(new String(locationUriBytes, Charset.defaultCharset()));
    }

    public static ParcelFileDescriptor getParcelFileDescriptor(final ContentProvider cp, final Location loc, String accessMode, final Bundle opts)
    {
        try
        {
            File.AccessMode am = Util.getAccessModeFromString(accessMode);
            if (!loc.getCurrentPath().isFile() && am == File.AccessMode.Read)
                throw new FileNotFoundException();
            final File f = loc.getCurrentPath().getFile();
            ParcelFileDescriptor fd = f.getFileDescriptor(am);
            if (fd != null)
                return fd;
            if (am == File.AccessMode.Read || am == File.AccessMode.Write)
            {
                UserSettings s = UserSettings.getSettings(cp.getContext());
                if (!s.forceTempFiles())
                {
                    if (am == File.AccessMode.Read)
                    //    return writeToPipe(f, opts == null ? new Bundle() : opts);
                    {
                        //String mime = FileOpsService.getMimeTypeFromExtension(cp.getContext(), loc.getCurrentPath());
                        //return cp.openPipeHelper(srcUri, mime, opts, f, new PipeWriter());
                        return writeToPipe(f, opts == null ? new Bundle() : opts);
                    }
                    else
                        return readFromPipe(cp, loc, f, opts == null ? new Bundle() : opts);
                }
            }
            Path parentPath = loc.getCurrentPath();
            try
            {
                parentPath = parentPath.getParentPath();
            }
            catch (IOException ignored)
            {
            }
            int mode = Util.getParcelFileDescriptorModeFromAccessMode(am);
            final Location tmpLocation = copyFileToTmpLocation(loc, parentPath, f, mode, cp);
            Uri u = tmpLocation.getDeviceAccessibleUri(tmpLocation.getCurrentPath());
            if (u != null && ContentResolver.SCHEME_FILE.equalsIgnoreCase(u.getScheme()))
            {
                java.io.File jf = new java.io.File(u.getPath());
                return mode == ParcelFileDescriptor.MODE_READ_ONLY ? ParcelFileDescriptor.open(jf, mode) : ParcelFileDescriptor.open(jf, mode, new Handler(Looper.getMainLooper()), e -> {
                    if (e != null)
                        Logger.showAndLog(cp.getContext(), e);
                    else
                        FileOpsService.saveChangedFile(cp.getContext(), new SrcDstSingle(tmpLocation, loc));
                });
            }
            return tmpLocation.getCurrentPath().getFile().getFileDescriptor(am);
        }
        catch (IOException e)
        {
            Logger.log(e);
            throw new RuntimeException(e);
        }
    }

    private static Location copyFileToTmpLocation(Location srcLoc, Path parentPath, File srcFile, int mode, ContentProvider cp) throws IOException
    {
        if (!srcLoc.getCurrentPath().exists() && (mode & ParcelFileDescriptor.MODE_CREATE) == 0)
            throw new IOException("File doesn't exist");
        final Location tmpLocation = TempFilesMonitor.getTmpLocation(srcLoc, parentPath, cp.getContext(), UserSettings.getSettings(cp.getContext()).getWorkDir(), mode != ParcelFileDescriptor.MODE_READ_ONLY);
        File dst = null;
        Path dstFilePath = PathUtil.buildPath(tmpLocation.getCurrentPath(), srcFile.getName());
        if ((mode & ParcelFileDescriptor.MODE_TRUNCATE) == 0)
        {
            if (dstFilePath != null && dstFilePath.isFile())
            {
                Location dstLocation = tmpLocation.copy();
                dstLocation.setCurrentPath(dstFilePath);
                if (!TempFilesMonitor.getMonitor(cp.getContext()).isUpdateRequired(srcLoc, dstLocation))
                    dst = dstFilePath.getFile();
            }
            if (dst == null)
                dst = Util.copyFile(srcFile, tmpLocation.getCurrentPath().getDirectory(), srcFile.getName());
        }
        else
        {
            if (dstFilePath != null && dstFilePath.isFile())
                WipeFilesTask.wipeFileRnd(dstFilePath.getFile());
            dst = tmpLocation.getCurrentPath().getDirectory().createFile(srcFile.getName());
        }
        tmpLocation.setCurrentPath(dst.getPath());
        Location srcFolderLocation = srcLoc.copy();
        srcFolderLocation.setCurrentPath(parentPath);
        Uri u = tmpLocation.getDeviceAccessibleUri(dst.getPath());
        if (u == null || !ContentResolver.SCHEME_FILE.equalsIgnoreCase(u.getScheme()))
            TempFilesMonitor.getMonitor(cp.getContext()).addFileToMonitor(srcLoc, srcFolderLocation, tmpLocation, mode == ParcelFileDescriptor.MODE_READ_ONLY);
        return tmpLocation;
    }

    private static ParcelFileDescriptor readFromPipe(final ContentProvider cp, final Location loc, final File targetFile, final Bundle opts) throws IOException
    {
        final ParcelFileDescriptor[] pfds = ParcelFileDescriptor.createPipe();
        Completable.create(s -> {
            try (FileInputStream fin = new FileInputStream(pfds[0].getFileDescriptor()))
            {
                Util.CancellableProgressInfo pi = new Util.CancellableProgressInfo();
                s.setCancellable(pi);
                Util.copyFileFromInputStream(fin, targetFile, opts.getLong(OPTION_OFFSET, 0), opts.getLong(OPTION_NUM_BYTES, -1), pi);
                notifyLocationChanged(cp.getContext(), loc);
            }
            pfds[0].close();
            s.onComplete();
        }).
                subscribeOn(Schedulers.io()).
                subscribe(() -> {
                }, Logger::log);
        return pfds[1];
    }

    private static ParcelFileDescriptor writeToPipe(final File srcFile, final Bundle opts) throws IOException
    {
        final ParcelFileDescriptor[] pfds = ParcelFileDescriptor.createPipe();
        Completable.create(s -> {
            try (FileOutputStream fout = new FileOutputStream(pfds[1].getFileDescriptor()))
            {
                Util.CancellableProgressInfo pi = new Util.CancellableProgressInfo();
                s.setCancellable(pi);
                Util.copyFileToOutputStream(fout, srcFile, opts.getLong(OPTION_OFFSET, 0), opts.getLong(OPTION_NUM_BYTES, -1), pi);
            }
            catch (IOException e)
            {
                if (!isBrokenPipe(e))
                    throw e;
                Logger.debug("Pipe reader closed before file copy completed");
            }
            finally
            {
                closeQuietly(pfds[1]);
            }
            s.onComplete();
        }).
                subscribeOn(Schedulers.newThread()).
                subscribe(() -> {
                }, Logger::log);
        return pfds[0];
    }

    /**
     * Invalidates cached file metadata and notifies both content and documents Uris.
     * Call this after external writers update a provider-backed file.
     */
    public static void notifyLocationChanged(Context context, Location loc)
    {
        if (context == null || loc == null)
            return;
        try
        {
            ExtendedFileInfoLoader.getInstance().discardCache(loc, loc.getCurrentPath());
            context.getContentResolver().notifyChange(getContentUriFromLocation(loc), null);
            context.getContentResolver().notifyChange(ContainersDocumentProviderBase.getUriFromLocation(loc), null);

            Path parentPath = loc.getCurrentPath().getParentPath();
            if (parentPath != null)
            {
                Location parentLoc = loc.copy();
                parentLoc.setCurrentPath(parentPath);
                context.getContentResolver().notifyChange(getContentUriFromLocation(parentLoc), null);
                context.getContentResolver().notifyChange(ContainersDocumentProviderBase.getUriFromLocation(parentLoc), null);
            }
        }
        catch (IOException e)
        {
            Logger.log(e);
        }
    }

    /**
     * Returns true when a pipe write failed because the reader closed its end.
     * Many clients stop reading early after they have consumed enough data.
     */
    private static boolean isBrokenPipe(Throwable e)
    {
        while (e != null)
        {
            if (e instanceof ErrnoException && ((ErrnoException) e).errno == OsConstants.EPIPE)
                return true;
            String message = e.getMessage();
            if (message != null && message.contains("EPIPE"))
                return true;
            e = e.getCause();
        }
        return false;
    }

    /**
     * Closes a pipe endpoint during cleanup without replacing the original failure.
     */
    private static void closeQuietly(ParcelFileDescriptor pfd)
    {
        try
        {
            pfd.close();
        }
        catch (IOException ignored)
        {
        }
    }

    public static Uri getContentUriFromLocation(Location loc, Path path)
    {
        Location copy = loc.copy();
        copy.setCurrentPath(path);
        return getContentUriFromLocation(copy);
    }

    public static Uri getContentUriFromLocation(Location loc)
    {
        return getContentUriFromLocationUri(loc.getLocationUri());
        /*try
        {
            if (loc.getCurrentPath().isFile())
            {
                Uri.Builder ub = uri.buildUpon();
                ub.appendPath(loc.getCurrentPath().getFile().getName());
                uri = ub.build();
            }
        }
        catch (IOException e)
        {
            Logger.log(e);
        }
        return uri;*/
    }

    public static Uri getContentUriFromLocationUri(Uri locationUri)
    {
        Uri.Builder ub = CONTENT_URI.buildUpon();
        appendLocationUriToProviderUri(ub, locationUri);
        return ub.build();
    }

    public static Uri getMetaUriFromLocationUri(Uri locationUri)
    {
        Uri.Builder ub = META_URI.buildUpon();
        appendLocationUriToProviderUri(ub, locationUri);
        return ub.build();
    }

    public static void appendLocationUriToProviderUri(Uri.Builder ub, Uri locationUri)
    {
        ub.appendPath(Base64.encodeToString(locationUri.toString().getBytes(), Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE));
    }

    public static Cursor getEmptyMetaCursor()
    {
        return _emptyMetaCursor;
    }

    @Override
    public boolean onCreate()
    {
        SystemConfig.setInstance(new com.igeltech.nevercrypt.android.settings.SystemConfig(getContext()));
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {
        switch (_uriMatcher.match(uri))
        {
            case CONTENT_PATH_CODE:
            case META_PATH_CODE:
                if (sortOrder != null)
                    throw new IllegalArgumentException("Sorting is not supported");
                return queryMeta(uri, projection, selection, selectionArgs);
            case CURRENT_SELECTION_PATH_CODE:
                if (selection != null || selectionArgs != null)
                    throw new IllegalArgumentException("Selection is not supported");
                if (sortOrder != null)
                    throw new IllegalArgumentException("Sorting is not supported");
                return querySelection(projection);
        }
        throw new IllegalArgumentException("Unsupported uri: " + uri);
    }

    @Override
    public String getType(@NonNull Uri uri)
    {
        switch (_uriMatcher.match(uri))
        {
            case META_PATH_CODE:
                return getMetaMimeType(uri);
            case CONTENT_PATH_CODE:
                return getContentMimeType(uri);
            case CURRENT_SELECTION_PATH_CODE:
                return _currentSelection == null ? null : MIME_TYPE_SELECTION;
        }
        throw new IllegalArgumentException("Unsupported uri: " + uri);
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues contentValues)
    {
        switch (_uriMatcher.match(uri))
        {
            case CONTENT_PATH_CODE:
            case META_PATH_CODE:
                return insertMeta(uri, contentValues);
            case CURRENT_SELECTION_PATH_CODE:
                _currentSelection = null;
                setCurrentSelection(contentValues);
                return CURRENT_SELECTION_URI;
        }
        throw new IllegalArgumentException("Unsupported uri: " + uri);
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs)
    {
        switch (_uriMatcher.match(uri))
        {
            case CONTENT_PATH_CODE:
            case META_PATH_CODE:
                return deleteFromFS(uri, selection, selectionArgs);
            case CURRENT_SELECTION_PATH_CODE:
                if (selection != null || selectionArgs != null)
                    throw new IllegalArgumentException("Selection is not supported");
                if (_currentSelection != null)
                {
                    _currentSelection = null;
                    return 1;
                }
                return 0;
        }
        throw new IllegalArgumentException("Unsupported uri: " + uri);
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs)
    {
        switch (_uriMatcher.match(uri))
        {
            case CONTENT_PATH_CODE:
            case META_PATH_CODE:
                return updateFS(uri, values, selection, selectionArgs);
            case CURRENT_SELECTION_PATH_CODE:
                if (selection != null || selectionArgs != null)
                    throw new IllegalArgumentException("Selection is not supported");
                setCurrentSelection(values);
                return 1;
        }
        throw new IllegalArgumentException("Unsupported uri: " + uri);
    }

    @Override
    public String[] getStreamTypes(@NonNull Uri uri, @NonNull String mimeTypeFilter)
    {
        switch (_uriMatcher.match(uri))
        {
            case CONTENT_PATH_CODE:
            case META_PATH_CODE:
                return getContentMimeType(uri, mimeTypeFilter);
            case CURRENT_SELECTION_PATH_CODE:
                return null;
        }
        throw new IllegalArgumentException("Unsupported uri: " + uri);
    }

    @Override
    public AssetFileDescriptor openTypedAssetFile(@NonNull Uri uri, @NonNull String mimeTypeFilter, Bundle opts) throws FileNotFoundException
    {
        Location loc = getLocationFromProviderUri(uri);
        // Checks to see if the MIME type filter matches a supported MIME type.
        String[] mimeTypes = getContentMimeType(loc, mimeTypeFilter);
        // If the MIME type is supported
        if (mimeTypes != null)
            return getAssetFileDescriptor(loc, "r", opts == null ? new Bundle() : opts);
        // If the MIME type is not supported, return a read-only handle to the file.
        return super.openTypedAssetFile(uri, mimeTypeFilter, opts);
    }

    @Override
    public AssetFileDescriptor openAssetFile(@NonNull Uri uri, @NonNull String mode)
    {
        switch (_uriMatcher.match(uri))
        {
            case CONTENT_PATH_CODE:
            case META_PATH_CODE:
                Location loc = getLocationFromProviderUri(uri);
                return getAssetFileDescriptor(loc, mode, new Bundle());
        }
        throw new IllegalArgumentException("Unsupported uri: " + uri);
    }

    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode)
    {
        switch (_uriMatcher.match(uri))
        {
            case CONTENT_PATH_CODE:
            case META_PATH_CODE:
                Location loc = getLocationFromProviderUri(uri);
                return getParcelFileDescriptor(loc, mode, new Bundle());
        }
        throw new IllegalArgumentException("Unsupported uri: " + uri);
    }

    protected AssetFileDescriptor getAssetFileDescriptor(Location loc, String accessMode, Bundle opts)
    {
        ParcelFileDescriptor pfd = getParcelFileDescriptor(loc, accessMode, opts);
        return new AssetFileDescriptor(pfd, opts.getLong(OPTION_OFFSET, 0), opts.getLong(OPTION_NUM_BYTES, AssetFileDescriptor.UNKNOWN_LENGTH));
    }

    protected ParcelFileDescriptor getParcelFileDescriptor(final Location loc, String accessMode, Bundle opts)
    {
        return Single.<ParcelFileDescriptor>create(s -> s.onSuccess(getParcelFileDescriptor(this, loc, accessMode, opts))).
                subscribeOn(Schedulers.io()).blockingGet();
    }

    protected int updateFS(Uri uri, ContentValues values, String selection, String[] selectionArgs)
    {
        Location loc = getLocationFromProviderUri(uri);
        return LoadPathInfoObservable.create(loc).
                filter(new SelectionChecker(loc, selection, selectionArgs)).
                map(cpi -> {
                    if (cpi.isFile())
                    {
                        cpi.getPath().getFile().rename(values.getAsString(COLUMN_NAME));
                        return true;
                    }
                    else if (cpi.isDirectory())
                    {
                        cpi.getPath().getDirectory().rename(values.getAsString(COLUMN_NAME));
                        return true;
                    }
                    return false;
                }).subscribeOn(Schedulers.io()).blockingGet() ? 1 : 0;
    }

    protected int deleteFromFS(Uri uri, String selection, String[] selectionArgs)
    {
        Location loc = getLocationFromProviderUri(uri);
        return LoadPathInfoObservable.create(loc).
                filter(new SelectionChecker(loc, selection, selectionArgs)).
                map(cpi -> {
                    if (cpi.isFile())
                    {
                        cpi.getPath().getFile().delete();
                        return true;
                    }
                    else if (cpi.isDirectory())
                    {
                        cpi.getPath().getDirectory().delete();
                        return true;
                    }
                    return false;
                }).subscribeOn(Schedulers.io()).blockingGet() ? 1 : 0;
    }

    protected Uri insertMeta(Uri uri, ContentValues contentValues)
    {
        return Single.<Uri>create(em -> {
            Location loc = getLocationFromProviderUri(uri);
            Path basePath = loc.getCurrentPath();
            if (!basePath.isDirectory())
                throw new IllegalArgumentException("Wrong parent folder: " + basePath);
            String name = contentValues.getAsString(COLUMN_NAME);
            FSRecord res = contentValues.getAsBoolean(COLUMN_IS_FOLDER) ? basePath.getDirectory().createDirectory(name) : basePath.getDirectory().createFile(name);
            loc.setCurrentPath(res.getPath());
            em.onSuccess(loc.getLocationUri());
        }).subscribeOn(Schedulers.io()).blockingGet();
    }

    protected void setCurrentSelection(ContentValues contentValues)
    {
        LocationsManager lm = LocationsManager.getLocationsManager(getContext(), true);
        PathsStore selection = _currentSelection;
        if (selection == null)
            selection = new PathsStore(lm);
        if (contentValues.containsKey(COLUMN_LOCATION))
            try
            {
                selection.setLocation(lm.getLocation(Uri.parse(contentValues.getAsString(COLUMN_LOCATION))));
            }
            catch (Exception e)
            {
                throw new IllegalArgumentException("Failed getting location from uri");
            }
        if (contentValues.containsKey(COLUMN_PATH))
        {
            if (selection.getLocation() == null)
                throw new IllegalArgumentException("Location is not set");
            try
            {
                selection.getPathsStore().add(selection.getLocation().getFS().getPath(contentValues.getAsString(COLUMN_PATH)));
            }
            catch (Exception e)
            {
                throw new IllegalArgumentException("Failed getting location from uri");
            }
        }
        _currentSelection = selection;
    }

    protected Cursor queryMeta(Uri uri, String[] projection, String selection, String[] selectionArgs)
    {
        Location loc = getLocationFromProviderUri(uri);
        return new FSCursor(getContext(), loc, projection == null ? ALL_META_COLUMNS : projection, selection, selectionArgs, true);
    }

    protected Cursor querySelection(String[] projection)
    {
        checkProjection(projection, ALL_SELECTION_COLUMNS);
        if (projection == null)
            projection = ALL_SELECTION_COLUMNS;
        PathsStore selection = _currentSelection;
        MatrixCursor res = new MatrixCursor(projection);
        if (selection == null)
            return res;
        Location loc = selection.getLocation();
        for (Path p : selection.getPathsStore())
        {
            Location copy = loc.copy();
            copy.setCurrentPath(p);
            res.addRow(Collections.singletonList(copy.getLocationUri().toString()));
        }
        return res;
    }

    protected String[] getContentMimeType(Uri uri, String requestedMime)
    {
        Location loc = getLocationFromProviderUri(uri);
        return getContentMimeType(loc, requestedMime);
    }

    protected String getContentMimeType(Uri uri)
    {
        Location loc = getLocationFromProviderUri(uri);
        return getContentMimeType(loc);
    }

    protected String getContentMimeType(Location loc)
    {
        try
        {
            CachedPathInfo cpi = LoadPathInfoObservable.
                    create(loc).
                    subscribeOn(Schedulers.io()).
                    blockingGet();
            return cpi == null ? null : cpi.isFile() ? FileOpsService.getMimeTypeFromExtension(getContext(), loc.getCurrentPath()) : null;
        }
        catch (IOException e)
        {
            Logger.log(e);
            return null;
        }
    }

    protected String[] getContentMimeType(Location loc, String requestedMime)
    {
        ClipDescription cd = new ClipDescription(null, new String[]{getContentMimeType(loc)});
        return cd.filterMimeTypes(requestedMime);
    }

    protected String getMetaMimeType(Uri uri)
    {
        Location loc = getLocationFromProviderUri(uri);
        CachedPathInfo cpi = LoadPathInfoObservable.create(loc).blockingGet();
        return cpi == null ? null : cpi.isFile() ? MIME_TYPE_FILE_META : cpi.isDirectory() ? MIME_TYPE_FOLDER_META : null;
    }

    protected void checkProjection(String[] projection, String[] columns)
    {
        if (projection != null)
        {
            for (String col : projection)
                if (!Arrays.asList(columns).contains(col))
                    throw new IllegalArgumentException("Wrong projection column: " + col);
        }
    }

    protected Location getLocationFromProviderUri(Uri providerUri)
    {
        return getLocationFromProviderUri(getContext(), providerUri);
    }
}
