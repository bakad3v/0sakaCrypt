package com.igeltech.nevercrypt.android.settings.views;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;

import com.igeltech.nevercrypt.android.settings.PropertyEditor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PropertiesView extends LinearLayout
{
    private static int _ID_COUNTER = 1;
    private final Map<Integer, PropertyInfo> _properties = new HashMap<>();
    private final List<PropertyEditor> _propertiesToLoad = new ArrayList<>();
    private final Comparator<PropertyEditor> _comparator = (lhs, rhs) -> Integer.valueOf(lhs.getStartPosition()).compareTo(rhs.getStartPosition());
    private boolean _isLoadingProperties, _instantSave;

    public PropertiesView(Context context)
    {
        super(context);
    }

    public PropertiesView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public PropertiesView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    public static synchronized int newId()
    {
        return ++_ID_COUNTER;
    }

    public static PropertyEditor.Host getHost(Fragment f)
    {
        Bundle args = f.getArguments();
        if (args != null && args.containsKey(PropertyEditor.ARG_HOST_FRAGMENT_TAG))
        {
            String tag = args.getString(PropertyEditor.ARG_HOST_FRAGMENT_TAG);
            Fragment host = f.getParentFragmentManager().findFragmentByTag(tag);
            if (host == null && f.getFragmentManager() != null)
                host = f.getFragmentManager().findFragmentByTag(tag);
            if (host != null)
                return (PropertyEditor.Host) host;
        }
        return (PropertyEditor.Host) f.getActivity();
    }

    public static PropertyEditor getProperty(Fragment f)
    {
        PropertyEditor.Host host = getHost(f);
        return host == null || f.getArguments() == null || !f.getArguments().containsKey(PropertyEditor.ARG_PROPERTY_ID) ? null : host.getPropertiesView().getPropertyById(f.getArguments().getInt(PropertyEditor.ARG_PROPERTY_ID));
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data)
    {
        for (PropertyEditor pe : listDisplayedProperties())
        {
            if (pe.onActivityResult(requestCode, resultCode, data))
                return true;
        }
        return false;
    }

    public int addProperty(PropertyEditor pe)
    {
        if (pe.getId() == 0)
            pe.setId(_properties.size());
        PropertyInfo pi = new PropertyInfo();
        pi.isEnabled = true;
        pi.property = pe;
        _properties.put(pe.getId(), pi);
        if (pe.getStartPosition() == 0)
            pe.setStartPosition(_properties.size());
        displayProperty(pe);
        return pe.getId();
    }

    public void saveProperties() throws Exception
    {
        for (PropertyEditor pe : listDisplayedProperties())
            pe.save();
    }

    public void saveProperties(Bundle b)
    {
        for (PropertyInfo pe : _properties.values())
            pe.property.save(b);
    }

    public void loadProperties()
    {
        loadProperties(getCurrentlyEnabledProperties(), null);
    }

    public void loadProperties(Bundle b)
    {
        loadProperties(getCurrentlyEnabledProperties(), b);
    }

    public void beginUpdate()
    {
        _isLoadingProperties = true;
    }

    public void endUpdate(Bundle b)
    {
        _isLoadingProperties = false;
        commitProperties();
        if (!_propertiesToLoad.isEmpty())
        {
            ArrayList<PropertyEditor> next = new ArrayList<>(_propertiesToLoad);
            _propertiesToLoad.clear();
            loadProperties(next, b);
        }
    }

    public void loadProperties(Iterable<Integer> ids, Bundle b)
    {
        ArrayList<PropertyEditor> props = new ArrayList<>();
        for (int id : ids)
            props.add(getPropertyById(id));
        loadProperties(props, b);
    }

    public void loadProperties(Collection<PropertyEditor> properties, Bundle b)
    {
        if (_isLoadingProperties)
            return;
        beginUpdate();
        try
        {
            for (PropertyEditor pe : properties)
            {
                View v = pe.getView(this);
                v.setId(pe.getId());
                if (b == null)
                    pe.load();
                else
                    pe.load(b);
            }
        }
        finally
        {
            endUpdate(b);
        }
    }

    public boolean isPropertyEnabled(int propertyId)
    {
        PropertyInfo pi = _properties.get(propertyId);
        return pi != null && pi.isEnabled;
    }

    public synchronized PropertyEditor getEnabledPropertyById(int propertyId)
    {
        PropertyInfo pi = _properties.get(propertyId);
        return pi != null && pi.isEnabled ? pi.property : null;
    }

    public synchronized PropertyEditor getPropertyById(int propertyId)
    {
        PropertyInfo pi = _properties.get(propertyId);
        return pi != null ? pi.property : null;
    }

    public synchronized PropertyEditor getPropertyByType(Class<? extends PropertyEditor> type)
    {
        for (PropertyInfo pi : _properties.values())
            if (pi.property.getClass().equals(type))
                return pi.property;
        return null;
    }

    public synchronized void setPropertyState(int propertyId, boolean enabled)
    {
        PropertyInfo pi = _properties.get(propertyId);
        if (pi == null)
            throw new IllegalArgumentException("Property not found. id=" + propertyId);
        setPropertyState(pi, enabled);
        if (!_isLoadingProperties)
            commitProperties();
    }

    public void removeProperty(int propertyId)
    {
        PropertyInfo pi = _properties.get(propertyId);
        if (pi == null)
            throw new IllegalArgumentException("Property not found. id=" + propertyId);
        removeView(pi.property.getView(this));
        _properties.remove(propertyId);
    }

    public synchronized void setPropertiesState(Iterable<Integer> ids, boolean enabled)
    {
        for (int id : ids)
        {
            PropertyInfo pi = _properties.get(id);
            if (pi == null)
                throw new IllegalArgumentException("Property not found. id=" + id);
            setPropertyState(pi, enabled);
        }
        if (!_isLoadingProperties)
            commitProperties();
    }

    public synchronized void setPropertiesState(boolean enabled)
    {
        for (PropertyInfo pi : _properties.values())
            setPropertyState(pi, enabled);
        if (!_isLoadingProperties)
            commitProperties();
    }

    public boolean isInstantSave()
    {
        return _instantSave;
    }

    public void setInstantSave(boolean val)
    {
        _instantSave = val;
    }

    public boolean isLoadingProperties()
    {
        return _isLoadingProperties;
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container)
    {
        if (container != null)
        {
            Bundle b = new Bundle();
            saveProperties(b);
            container.put(getId(), b);
        }
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container)
    {
        if (container != null)
        {
            Bundle b = (Bundle) container.get(getId());
            if (b != null)
                loadProperties(b);
        }
    }

    private void setPropertyState(PropertyInfo pi, boolean enabled)
    {
        pi.isEnabled = enabled;
        if (_isLoadingProperties)
        {
            _propertiesToLoad.remove(pi.property);
            _propertiesToLoad.add(pi.property);
        }
    }

    private void commitProperties()
    {
        ArrayList<PropertyEditor> toRemove = new ArrayList<>();
        Set<Integer> added = new HashSet<>();
        for (PropertyEditor pe : listDisplayedProperties())
        {
            if (!_properties.get(pe.getId()).isEnabled)
                toRemove.add(pe);
            added.add(pe.getId());
        }
        for (PropertyEditor pe : toRemove)
            removeView(pe.getView(this));
        for (PropertyInfo pi : _properties.values())
            if (pi.isEnabled && !added.contains(pi.property.getId()))
                displayProperty(pi.property);
    }

    private synchronized ArrayList<PropertyEditor> getCurrentlyEnabledProperties()
    {
        ArrayList<PropertyEditor> res = new ArrayList<>();
        for (PropertyInfo pi : _properties.values())
            if (pi.isEnabled)
                res.add(pi.property);
        return res;
    }

    private List<PropertyEditor> listDisplayedProperties()
    {
        ArrayList<PropertyEditor> pes = new ArrayList<>();
        for (int i = 0; i < getChildCount(); i++)
            pes.add((PropertyEditor) getChildAt(i).getTag());
        return pes;
    }

    private void displayProperty(final PropertyEditor pe)
    {
        List<PropertyEditor> curList = listDisplayedProperties();
        int pos = Collections.binarySearch(curList, pe, _comparator);
        if (pos < 0)
            pos = -pos - 1;
        View v = pe.getView(this);
        v.setTag(pe);
        v.setOnClickListener(v1 -> pe.onClick());
        addView(v, pos);
    }

    private static class PropertyInfo
    {
        PropertyEditor property;
        boolean isEnabled;
    }
}
