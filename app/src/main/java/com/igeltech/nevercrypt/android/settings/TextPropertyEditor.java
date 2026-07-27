package com.igeltech.nevercrypt.android.settings;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.textview.MaterialTextView;
import com.igeltech.nevercrypt.android.Logger;
import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.settings.dialogs.TextEditDialog;

public abstract class TextPropertyEditor extends PropertyEditorBase implements TextEditDialog.TextResultReceiver
{
    private final String _hostFragmentTag;
    protected MaterialTextView _selectedValueTextView;

    public TextPropertyEditor(PropertyEditor.Host host, int titleResId, int descResId, String hostFragmentTag)
    {
        this(host, R.layout.settings_text_editor, titleResId, descResId, hostFragmentTag);
    }

    public TextPropertyEditor(PropertyEditor.Host host, int layoutId, int titleResId, int descResId, String hostFragmentTag)
    {
        super(host, layoutId, titleResId, descResId);
        _hostFragmentTag = hostFragmentTag;
    }

    @Override
    public View createView(ViewGroup parent)
    {
        View view = super.createView(parent);
        _selectedValueTextView = view.findViewById(android.R.id.text1);
        // Dynamic properties can be enabled before loadProperties() reaches them; avoid saving XML placeholders.
        _selectedValueTextView.setText(loadText());
        AppCompatButton selectButton = view.findViewById(android.R.id.button1);
        selectButton.setOnClickListener(view1 -> startChangeValueDialog());
        return view;
    }

    @Override
    public void load()
    {
        _selectedValueTextView.setText(loadText());
    }

    @Override
    public void load(Bundle b)
    {
        if (_selectedValueTextView != null)
        {
            if (isInstantSave())
                load();
            else
                _selectedValueTextView.setText(b.getString(getBundleKey()));
        }
    }

    @Override
    public void save() throws Exception
    {
        saveText(_selectedValueTextView.getText().toString());
    }

    @Override
    public void save(Bundle b)
    {
        if (!isInstantSave() && _selectedValueTextView != null)
            b.putString(getBundleKey(), _selectedValueTextView.getText().toString());
    }

    public void setResult(String value) throws Exception
    {
        onTextChanged(value);
    }

    protected abstract String loadText();

    protected abstract void saveText(String text) throws Exception;

    protected void startChangeValueDialog()
    {
        Bundle args = initDialogArgs();
        AppCompatDialogFragment df = new TextEditDialog();
        df.setArguments(args);
        df.show(getHost().getFragmentManager(), TextEditDialog.TAG);
    }

    protected int getDialogViewResId()
    {
        return R.layout.settings_edit_text;
    }

    protected Bundle initDialogArgs()
    {
        Bundle b = new Bundle();
        b.putString(TextEditDialog.ARG_TEXT, _selectedValueTextView.getText().toString());
        b.putInt(PropertyEditor.ARG_PROPERTY_ID, getId());
        b.putInt(TextEditDialog.ARG_MESSAGE_ID, _titleResId);
        b.putInt(TextEditDialog.ARG_EDIT_TEXT_RES_ID, getDialogViewResId());
        if (_hostFragmentTag != null)
            b.putString(PropertyEditor.ARG_HOST_FRAGMENT_TAG, _hostFragmentTag);
        return b;
    }

    protected void onTextChanged(String newValue)
    {
        _selectedValueTextView.setText(newValue);
        if (!_host.getPropertiesView().isLoadingProperties() && _host.getPropertiesView().isInstantSave())
            try
            {
                save();
            }
            catch (Exception e)
            {
                Logger.showAndLog(getHost().getContext(), e);
            }
    }
}
