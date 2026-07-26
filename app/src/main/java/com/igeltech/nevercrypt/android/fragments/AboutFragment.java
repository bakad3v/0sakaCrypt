package com.igeltech.nevercrypt.android.fragments;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textview.MaterialTextView;
import com.igeltech.nevercrypt.android.Logger;
import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.providers.MainContentProvider;
import com.igeltech.nevercrypt.android.settings.UserSettings;
import com.igeltech.nevercrypt.exceptions.ApplicationException;
import com.igeltech.nevercrypt.fs.util.StringPathUtil;
import com.igeltech.nevercrypt.locations.DeviceBasedLocation;
import com.igeltech.nevercrypt.locations.Location;
import com.igeltech.nevercrypt.settings.GlobalConfig;
import com.igeltech.nevercrypt.util.exec.ExecuteExternalProgram;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;

public class AboutFragment extends Fragment
{
    private static final String SATA_ANDAGI_ASSET_NAME = "sata-andagi.mp3";

    private MediaPlayer _sataAndagiPlayer;

    public static String getVersionName(Context context)
    {
        try
        {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        }
        catch (NameNotFoundException e)
        {
            Logger.log(e);
            return "";
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        String verName = getVersionName(getActivity());
        String aboutMessage = String.format("%s v%s", getResources().getString(R.string.app_name), verName);
        ((MaterialTextView) view.findViewById(R.id.about_text_view)).setText(aboutMessage);
        view.findViewById(R.id.get_program_log).setOnClickListener(view1 -> {
            try
            {
                saveDebugLog();
            }
            catch (Throwable e)
            {
                Logger.showAndLog(getActivity(), e);
            }
        });
        view.findViewById(R.id.check_source_code_button).setOnClickListener(view1 -> openWebPage(GlobalConfig.SOURCE_CODE_URL));
        view.findViewById(R.id.launcher_icon).setOnClickListener(view1 -> playSataAndagi());
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView()
    {
        releaseSataAndagiPlayer();
        super.onDestroyView();
    }

    protected void openWebPage(String url)
    {
        try
        {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
        catch (Exception e)
        {
            Logger.showAndLog(getActivity(), e);
        }
    }

    private void playSataAndagi()
    {
        try
        {
            releaseSataAndagiPlayer();

            Context context = getContext();
            if (context == null)
                return;

            MediaPlayer player = new MediaPlayer();
            _sataAndagiPlayer = player;

            try (AssetFileDescriptor asset = context.getAssets().openFd(SATA_ANDAGI_ASSET_NAME))
            {
                player.setDataSource(asset.getFileDescriptor(), asset.getStartOffset(), asset.getLength());
            }

            player.setOnCompletionListener(mediaPlayer -> {
                if (_sataAndagiPlayer == mediaPlayer)
                    _sataAndagiPlayer = null;
                mediaPlayer.release();
            });
            player.setOnErrorListener((mediaPlayer, what, extra) -> {
                if (_sataAndagiPlayer == mediaPlayer)
                    _sataAndagiPlayer = null;
                mediaPlayer.release();
                return true;
            });
            player.prepare();
            player.start();
        }
        catch (Exception e)
        {
            releaseSataAndagiPlayer();
            Logger.showAndLog(getActivity(), e);
        }
    }

    private void releaseSataAndagiPlayer()
    {
        if (_sataAndagiPlayer != null)
        {
            _sataAndagiPlayer.release();
            _sataAndagiPlayer = null;
        }
    }

    private void saveDebugLog() throws IOException, ApplicationException
    {
        Context ctx = getActivity();
        File out = ctx.getExternalFilesDir(null);
        if (out == null || !out.canWrite())
            out = ctx.getFilesDir();
        Location loc = new DeviceBasedLocation(UserSettings.getSettings(ctx), new StringPathUtil(out.getPath()).combine(String.format(Locale.US, "log-%1$tY%1$tm%1$td%1$tH%1$tM%1$tS.txt", new Date())).toString());
        dumpLog(loc);
        sendLogFile(loc);
    }

    protected void dumpLog(Location logLocation) throws IOException, ApplicationException
    {
        ExecuteExternalProgram.executeAndReadString("logcat", "-df", logLocation.getCurrentPath().getPathString());
    }

    private void sendLogFile(Location logLocation)
    {
        Context ctx = getActivity();
        Uri uri = MainContentProvider.getContentUriFromLocation(logLocation);
        Intent actionIntent = new Intent(Intent.ACTION_SEND);
        actionIntent.setType("text/plain");
        actionIntent.putExtra(Intent.EXTRA_STREAM, uri);
        ClipData cp = ClipData.newUri(ctx.getContentResolver(), ctx.getString(R.string.get_program_log), uri);
        actionIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        actionIntent.setClipData(cp);
        Intent startIntent = Intent.createChooser(actionIntent, ctx.getString(R.string.save_log_file_to));
        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(startIntent);
    }

}
