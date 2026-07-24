package com.igeltech.nevercrypt.locations;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;

import com.igeltech.nevercrypt.android.Logger;
import com.igeltech.nevercrypt.android.locations.ContainerBasedLocation;
import com.igeltech.nevercrypt.android.locations.ContentResolverLocation;
import com.igeltech.nevercrypt.android.locations.EncFsLocation;
import com.igeltech.nevercrypt.android.locations.EncFsLocationBase;
import com.igeltech.nevercrypt.android.locations.LUKSLocation;
import com.igeltech.nevercrypt.android.locations.TrueCryptLocation;
import com.igeltech.nevercrypt.android.locations.VeraCryptLocation;
import com.igeltech.nevercrypt.android.locations.closer.fragments.OpenableLocationCloserFragment;
import com.igeltech.nevercrypt.android.settings.UserSettings;
import com.igeltech.nevercrypt.android.widgets.CloseAllContainersWidget;
import com.igeltech.nevercrypt.android.widgets.LocationShortcutWidget;
import com.igeltech.nevercrypt.fs.Path;
import com.igeltech.nevercrypt.fs.util.Util;
import com.igeltech.nevercrypt.settings.Settings;

import org.json.JSONException;

import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

public abstract class LocationsManagerBase
{
    public static final String PARAM_LOCATION_URIS = "com.igeltech.nevercrypt.android.LOCATION_URIS";
    public static final String PARAM_PATHS = "com.igeltech.nevercrypt.android.PATHS";
    public static final String PARAM_LOCATION_URI = "com.igeltech.nevercrypt.android.LOCATION_URI";
    public static final String PARAM_EXPORT_FOLDER_URI = "com.igeltech.nevercrypt.android.EXPORT_FOLDER_URI";
    public static final String BROADCAST_LOCATION_CREATED = "com.igeltech.nevercrypt.BROADCAST_LOCATION_CREATED";
    public static final String BROADCAST_LOCATION_REMOVED = "com.igeltech.nevercrypt.BROADCAST_LOCATION_REMOVED";
    public static final String BROADCAST_ALL_CONTAINERS_CLOSED = "com.igeltech.nevercrypt.android.BROADCAST_ALL_CONTAINERS_CLOSED";
    public static final String BROADCAST_CLOSE_ALL = "com.igeltech.nevercrypt.CLOSE_ALL";
    public static final String BROADCAST_LOCATION_CHANGED = "com.igeltech.nevercrypt.android.BROADCAST_LOCATION_CHANGED";

    private static LocationsManagerBase _instance;
    protected final Settings _settings;
    final List<LocationInfo> _currentLocations = new ArrayList<>();
    private final Stack<String> _openedLocationsStack = new Stack<>();
    private final Context _context;

    protected LocationsManagerBase(Context context, Settings settings)
    {
        _settings = settings;
        _context = context.getApplicationContext();
    }

    public static synchronized LocationsManager getLocationsManager(Context context)
    {
        return getLocationsManager(context, true);
    }

    public static synchronized LocationsManager getLocationsManager(Context context, boolean create)
    {
        if (create && _instance == null)
        {
            _instance = new LocationsManager(context.getApplicationContext(), UserSettings.getSettings(context));
            _instance.loadStoredLocations();
        }
        return (LocationsManager) _instance;
    }

    private static synchronized void closeLocationsManager()
    {
        if (_instance != null)
        {
            _instance.close();
            _instance = null;
        }
    }

    public static synchronized void setGlobalLocationsManager(LocationsManagerBase lm)
    {
        if (_instance != null)
            closeLocationsManager();
        _instance = lm;
    }

    public static void storePathsInBundle(Bundle b, Location loc, Collection<? extends Path> paths)
    {
        b.putParcelable(PARAM_LOCATION_URI, loc.getLocationUri());
        if (paths != null)
            b.putStringArrayList(PARAM_PATHS, Util.storePaths(paths));
    }

    public static void storePathsInIntent(Intent i, Location loc, Collection<? extends Path> paths)
    {
        i.setData(loc.getLocationUri());
        i.putExtra(PARAM_LOCATION_URI, loc.getLocationUri());
        if (paths != null)
            i.putStringArrayListExtra(PARAM_PATHS, Util.storePaths(paths));
    }

    public static void storeLocationsInBundle(Bundle b, Iterable<? extends Location> locations)
    {
        ArrayList<Uri> uris = new ArrayList<>();
        for (Location loc : locations)
            uris.add(loc.getLocationUri());
        b.putParcelableArrayList(PARAM_LOCATION_URIS, uris);
    }

    public static void storeLocationsInIntent(Intent i, Iterable<? extends Location> locations)
    {
        Bundle b = new Bundle();
        storeLocationsInBundle(b, locations);
        i.putExtras(b);
    }

    public static ArrayList<Location> getLocationsFromBundle(LocationsManagerBase lm, Bundle b) throws Exception
    {
        ArrayList<Location> res = new ArrayList<>();
        if (b != null)
        {
            ArrayList<Uri> uris = b.getParcelableArrayList(PARAM_LOCATION_URIS);
            if (uris != null)
                for (Uri uri : uris)
                    res.add(lm.getLocation(uri));
        }
        return res;
    }

    public static ArrayList<Location> getLocationsFromIntent(LocationsManagerBase lm, Intent i) throws Exception
    {
        ArrayList<Location> res = getLocationsFromBundle(lm, i.getExtras());
        if (res.isEmpty() && i.getData() != null)
            res.add(lm.getLocation(i.getData()));
        return res;
    }

    public static Location getFromIntent(Intent i, LocationsManagerBase lm, Collection<Path> pathsHolder)
    {
        try
        {
            if (i.getData() == null)
                return null;
            Location loc = lm.getLocation(i.getData());
            if (pathsHolder != null)
            {
                ArrayList<String> pathStrings = i.getStringArrayListExtra(PARAM_PATHS);
                if (pathStrings != null)
                    pathsHolder.addAll(Util.restorePaths(loc.getFS(), pathStrings));
            }
            return loc;
        }
        catch (Exception e)
        {
            Logger.log(e);
            return null;
        }
    }

    public static Location getFromBundle(Bundle b, LocationsManagerBase lm, Collection<Path> pathsHolder)
    {
        if (b == null || !b.containsKey(PARAM_LOCATION_URI))
            return null;
        try
        {
            Location loc = lm.getLocation(b.getParcelable(PARAM_LOCATION_URI));
            if (pathsHolder != null)
            {
                ArrayList<String> pathStrings = b.getStringArrayList(PARAM_PATHS);
                if (pathStrings != null)
                    pathsHolder.addAll(Util.restorePaths(loc.getFS(), pathStrings));
            }
            return loc;
        }
        catch (Exception e)
        {
            Logger.log(e);
            return null;
        }
    }

    public static IntentFilter getLocationRemovedIntentFilter()
    {
        return new IntentFilter(LocationsManager.BROADCAST_LOCATION_REMOVED);
    }

    public static IntentFilter getLocationAddedIntentFilter()
    {
        return new IntentFilter(LocationsManager.BROADCAST_LOCATION_CREATED);
    }

    public static void broadcastLocationChanged(Context context, Location location)
    {
        Intent i = new Intent(BROADCAST_LOCATION_CHANGED);
        //i.setData(location.getLocationUri());
        i.putExtra(PARAM_LOCATION_URI, location.getLocationUri());
        sendInternalBroadcast(context, i);
        sendExplicitBroadcast(context, i, LocationShortcutWidget.class);
        sendExplicitBroadcast(context, i, CloseAllContainersWidget.class);
    }

    public static void broadcastLocationAdded(Context context, Location location)
    {
        Intent i = new Intent(BROADCAST_LOCATION_CREATED);
        //i.setData(location.getLocationUri());
        if (location != null)
            i.putExtra(PARAM_LOCATION_URI, location.getLocationUri());
        sendInternalBroadcast(context, i);
    }

    public static void broadcastLocationRemoved(Context context, Location location)
    {
        Intent i = new Intent(BROADCAST_LOCATION_REMOVED);
        //i.setData(location.getLocationUri());
        if (location != null)
            i.putExtra(PARAM_LOCATION_URI, location.getLocationUri());
        sendInternalBroadcast(context, i);
    }

    public static void broadcastAllContainersClosed(Context context)
    {
        Intent i = new Intent(BROADCAST_ALL_CONTAINERS_CLOSED);
        sendInternalBroadcast(context, i);
        sendExplicitBroadcast(context, i, CloseAllContainersWidget.class);
    }

    private static void sendInternalBroadcast(Context context, Intent intent)
    {
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }

    private static void sendExplicitBroadcast(Context context, Intent intent, Class<?> receiverClass)
    {
        context.sendBroadcast(new Intent(intent).setClass(context, receiverClass));
    }

    public static ArrayList<Path> getPathsFromLocations(Iterable<? extends Location> locations) throws IOException
    {
        ArrayList<Path> res = new ArrayList<>();
        for (Location loc : locations)
            res.add(loc.getCurrentPath());
        return res;
    }

    public static List<Uri> getStoredLocationUris(Settings settings)
    {
        ArrayList<Uri> res = new ArrayList<>();
        try
        {
            List<String> locationStrings = com.igeltech.nevercrypt.android.helpers.Util.loadStringArrayFromString(settings.getStoredLocations());
            for (String p : locationStrings)
            {
                try
                {
                    res.add(Uri.parse(p));
                }
                catch (Exception e)
                {
                    Logger.log(e);
                }
            }
        }
        catch (JSONException e)
        {
            Logger.log(e);
            return res;
        }
        return res;
    }

    public void close()
    {
        closeAllLocations(true, false);
        clearLocations();
    }

    public Location getFromIntent(Intent i, Collection<Path> pathsHolder)
    {
        return getFromIntent(i, this, pathsHolder);
    }

    public Location getFromBundle(Bundle b, Collection<Path> pathsHolder)
    {
        return getFromBundle(b, this, pathsHolder);
    }

    public ArrayList<Location> getLocationsFromIntent(Intent i) throws Exception
    {
        return getLocationsFromIntent(this, i);
    }

    public ArrayList<Location> getLocationsFromBundle(Bundle b) throws Exception
    {
        return getLocationsFromBundle(this, b);
    }

    public void broadcastAllContainersClosed()
    {
        broadcastAllContainersClosed(_context);
    }

    public void closeAllLocations(Iterable<Location> locations, boolean forceClose, boolean sendBroadcasts)
    {
        //boolean forceClose = _settings.alwaysForceClose();
        for (Location l : locations)
        {
            try
            {
                if (l instanceof Openable)
                {
                    Openable ol = (Openable) l;
                    if (ol.isOpen())
                    {
                        closeLocation(l, forceClose);
                        if (sendBroadcasts)
                            broadcastLocationChanged(_context, l);
                    }
                }
            }
            catch (Exception e)
            {
                Logger.showAndLog(_context, e);
            }
        }
    }

    public Location getLocation(Uri locationUri) throws Exception
    {
        String locId = getLocationIdFromUri(locationUri);
        if (locId != null)
        {
            Location prevLoc = findExistingLocation(locId);
            if (prevLoc != null)
            {
                Location loc = prevLoc.copy();
                loc.loadFromUri(locationUri);
                return loc;
            }
        }
        Location loc = createLocationFromUri(locationUri);
        if (loc == null)
            throw new IllegalArgumentException("Unsupported location uri: " + locationUri);
        if (findExistingLocation(loc.getId()) == null)
            addNewLocation(loc, false);
        return loc;
    }

    public ArrayList<Location> getLocations(Collection<String> uriStrings)
    {
        ArrayList<Location> res = new ArrayList<>();
        if (uriStrings == null)
            return res;
        for (String uriString : uriStrings)
        {
            Uri uri = Uri.parse(uriString);
            try
            {
                res.add(getLocation(uri));
            }
            catch (Exception ignored)
            {
            }
        }
        return res;
    }

    public void addNewLocation(Location loc, boolean store)
    {
        synchronized (_currentLocations)
        {
            _currentLocations.add(new LocationInfo(loc, store));
            if (store)
                saveCurrentLocationLinks();
        }
    }

    private void removeLocation(String locationId)
    {
        synchronized (_currentLocations)
        {
            LocationInfo li = findExistingLocationInfo(locationId);
            if (li != null)
            {
                _currentLocations.remove(li);
                if (li.store)
                    saveCurrentLocationLinks();
            }
        }
    }

    public void removeLocation(Location loc)
    {
        removeLocation(loc.getId());
    }

    public void replaceLocation(Location oldLoc, Location newLoc, boolean store)
    {
        synchronized (_currentLocations)
        {
            removeLocation(oldLoc);
            addNewLocation(newLoc, store);
        }
    }

    private void clearLocations()
    {
        synchronized (_currentLocations)
        {
            _currentLocations.clear();
        }
    }

    public void loadStoredLocations()
    {
        synchronized (_currentLocations)
        {
            for (Uri u : getStoredLocationUris(_settings))
            {
                try
                {
                    Location loc = createLocationFromUri(u);
                    if (loc == null)
                        throw new IllegalArgumentException("Unsupported location uri: " + u);
                    _currentLocations.add(new LocationInfo(loc, true));
                }
                catch (Exception e)
                {
                    Logger.log(e);
                }
            }
        }
    }

    public Location findExistingLocation(Location loc) throws Exception
    {
        return findExistingLocation(loc.getLocationUri());
    }

    public Location findExistingLocation(String locationId)
    {
        synchronized (_currentLocations)
        {
            LocationInfo li = findExistingLocationInfo(locationId);
            return li == null ? null : li.location;
        }
    }

    public boolean isStoredLocation(String locationId)
    {
        synchronized (_currentLocations)
        {
            LocationInfo li = findExistingLocationInfo(locationId);
            return li != null && li.store;
        }
    }

    public Location findExistingLocation(Uri locationUri) throws Exception
    {
        Location t = createLocationFromUri(locationUri);
        if (t == null)
            throw new IllegalArgumentException("Unsupported location uri: " + locationUri);
        return findExistingLocation(t.getId());
    }

    public List<Location> getLoadedLocations(final boolean onlyVisible)
    {
        synchronized (_currentLocations)
        {
            return new ArrayList<>(new FilteredList<Location>()
            {
                @Override
                protected boolean isValid(Location l)
                {
                    return !onlyVisible || l.getExternalSettings().isVisibleToUser();
                }
            });
        }
    }

    public List<CryptoLocation> getLoadedCryptoLocations(final boolean onlyVisible)
    {
        synchronized (_currentLocations)
        {
            return new ArrayList<>(new FilteredList<CryptoLocation>()
            {
                @Override
                protected boolean isValid(Location l)
                {
                    return l instanceof CryptoLocation && (!onlyVisible || l.getExternalSettings().isVisibleToUser());
                }
            });
        }
    }

    public boolean hasOpenLocations()
    {
        synchronized (_currentLocations)
        {
            for (LocationInfo loc : _currentLocations)
                if ((loc.location instanceof Openable && ((Openable) loc.location).isOpen()))
                    return true;
            return false;
        }
    }

    public Location getDefaultLocationFromPath(String path) throws Exception
    {
        Uri u = Uri.parse(path);
        return getLocation(u);
    }

    public void saveCurrentLocationLinks()
    {
        ArrayList<String> links = new ArrayList<>();
        synchronized (_currentLocations)
        {
            for (LocationInfo li : _currentLocations)
                if (li.store)
                    links.add(li.location.getLocationUri().toString());
        }
        _settings.setStoredLocations(com.igeltech.nevercrypt.android.helpers.Util.storeElementsToString(links));
    }

    public Iterable<Location> getLocationsClosingOrder()
    {
        ArrayList<Location> locs = new ArrayList<>();
        for (int i = _openedLocationsStack.size() - 1; i >= 0; i--)
        {
            Location loc = findExistingLocation(_openedLocationsStack.get(i));
            if (loc != null)
                locs.add(loc);
        }
        return locs;
    }

    public void regOpenedLocation(Location loc)
    {
        _openedLocationsStack.push(loc.getId());
    }

    public void unregOpenedLocation(Location loc)
    {
        String id = loc.getId();
        while (_openedLocationsStack.contains(id))
            _openedLocationsStack.remove(id);
    }

    public void closeAllLocations(boolean forceClose, boolean sendBroadcasts)
    {
        closeAllLocations(getLocationsClosingOrder(), forceClose, sendBroadcasts);
        closeAllLocations(new ArrayList<>(getLoadedLocations(false)), forceClose, sendBroadcasts);
    }

    public void closeLocation(Location loc, boolean forceClose) throws Exception
    {
        loc.closeFileSystem(forceClose);
        if (loc instanceof Openable)
            OpenableLocationCloserFragment.closeLocation(_context, (Openable) loc, forceClose);
    }

    public Location createLocationFromUri(Uri locationUri) throws Exception
    {
        String scheme = locationUri.getScheme();
        if (scheme == null)
            return createDeviceLocation(locationUri);
        switch (locationUri.getScheme())
        {
            case ContainerBasedLocation.URI_SCHEME:
                return createContainerLocation(locationUri);
            case DeviceBasedLocation.URI_SCHEME:
                return createDeviceLocation(locationUri);
            case TrueCryptLocation.URI_SCHEME:
                return createTrueCryptLocation(locationUri);
            case VeraCryptLocation.URI_SCHEME:
                return createVeraCryptLocation(locationUri);
            case EncFsLocationBase.URI_SCHEME:
                return createEncFsLocation(locationUri);
            case LUKSLocation.URI_SCHEME:
                return createLUKSLocation(locationUri);
            case ContentResolver.SCHEME_CONTENT:
                return createContentResolverLocation(locationUri);
            default:
                return null;
        }
    }

    protected Context getContext()
    {
        return _context;
    }

    private LocationInfo findExistingLocationInfo(String locationId)
    {
        for (LocationInfo li : _currentLocations)
            if (li.location.getId().equals(locationId))
                return li;
        return null;
    }

    protected String getLocationIdFromUri(Uri locationUri) throws Exception
    {
        String scheme = locationUri.getScheme();
        if (scheme == null)
            return null;
        switch (locationUri.getScheme())
        {
            case ContainerBasedLocation.URI_SCHEME:
                return ContainerBasedLocation.getLocationId(this, locationUri);
            case DeviceBasedLocation.URI_SCHEME:
                return DeviceBasedLocation.getLocationId(locationUri);
            case TrueCryptLocation.URI_SCHEME:
                return TrueCryptLocation.getLocationId(this, locationUri);
            case VeraCryptLocation.URI_SCHEME:
                return VeraCryptLocation.getLocationId(this, locationUri);
            case EncFsLocationBase.URI_SCHEME:
                return EncFsLocationBase.getLocationId(this, locationUri);
            case LUKSLocation.URI_SCHEME:
                return LUKSLocation.getLocationId(this, locationUri);
            case ContentResolver.SCHEME_CONTENT:
                return ContentResolverLocation.getLocationId();
            default:
                return null;
        }
    }

    private Location createContainerLocation(Uri locationUri) throws Exception
    {
        return new ContainerBasedLocation(locationUri, this, getContext(), _settings);
    }

    private Location createTrueCryptLocation(Uri locationUri) throws Exception
    {
        return new TrueCryptLocation(locationUri, this, getContext(), _settings);
    }

    private Location createVeraCryptLocation(Uri locationUri) throws Exception
    {
        return new VeraCryptLocation(locationUri, this, getContext(), _settings);
    }

    private Location createEncFsLocation(Uri locationUri) throws Exception
    {
        return new EncFsLocation(locationUri, this, getContext(), _settings);
    }

    private Location createLUKSLocation(Uri locationUri) throws Exception
    {
        return new LUKSLocation(locationUri, this, getContext(), _settings);
    }

    private Location createDeviceLocation(Uri locationUri) throws IOException
    {
        return new DeviceBasedLocation(_settings, locationUri);
    }

    private Location createContentResolverLocation(Uri locationUri) throws Exception
    {
        return new ContentResolverLocation(_context, locationUri);
    }

    protected static class LocationInfo
    {
        Location location;
        boolean store;

        LocationInfo(Location location, boolean store)
        {
            this.location = location;
            this.store = store;
        }
    }

    class FilteredList<E> extends AbstractList<E>
    {
        @Override
        public int size()
        {
            synchronized (_currentLocations)
            {
                int res = 0;
                for (LocationInfo li : _currentLocations)
                    if (isValid(li.location))
                        res++;
                return res;
            }
        }

        @Override
        public E get(int location)
        {
            int res = 0;
            synchronized (_currentLocations)
            {
                for (LocationInfo li : _currentLocations)
                {
                    if (isValid(li.location))
                    {
                        if (res == location)
                            return (E) li.location;
                        res++;
                    }
                }
            }
            throw new IndexOutOfBoundsException();
        }

        protected boolean isValid(Location l)
        {
            return true;
        }
    }
}
