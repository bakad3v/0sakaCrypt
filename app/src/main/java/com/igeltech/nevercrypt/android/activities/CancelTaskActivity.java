package com.igeltech.nevercrypt.android.activities;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;

import com.igeltech.nevercrypt.android.R;
import com.igeltech.nevercrypt.android.service.FileOpsService;

public class CancelTaskActivity extends AppCompatActivity
{
    public static final String ACTION_CANCEL_TASK = "com.igeltech.nevercrypt.android.CANCEL_TASK";

    public static Intent getCancelTaskIntent(Context context, int taskId)
    {
        Intent i = new Intent(context, CancelTaskActivity.class);
        i.setAction(ACTION_CANCEL_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra(FileOpsService.INTENT_PARAM_TASK_ID, taskId);
        return i;
    }

    public static PendingIntent getCancelTaskPendingIntent(Context context, int taskId)
    {
        return PendingIntent.getActivity(context, taskId, getCancelTaskIntent(context, taskId), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.cancel_task_activity);
    }

    public void onYesClick(View v)
    {
        FileOpsService.cancelTask(this);
    }

    public void onNoClick(View v)
    {
        finish();
    }
}
