package ca.cmpt276.parentapp.timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;

import java.io.IOException;

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        int notification_id = intent.getIntExtra(TimerActivity.NOTIFY_ID,1);

        if (timerService.alarm_sound != null && timerService.alarm_sound.isPlaying()){
            try {
                timerService.alarm_sound.stop();
                timerService.alarm_sound.prepare();
            } catch (IOException e) {
                Toast.makeText(context,"Error in playing alarm",Toast.LENGTH_SHORT).show();
            }
        }

        NotificationManagerCompat notify_manager = NotificationManagerCompat.from(context);
        notify_manager.cancel(notification_id);
    }
}