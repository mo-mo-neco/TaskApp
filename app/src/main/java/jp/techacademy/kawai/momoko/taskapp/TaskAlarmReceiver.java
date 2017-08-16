package jp.techacademy.kawai.momoko.taskapp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import io.realm.Realm;

/**
 * Created by momon on 2017/08/15.
 */

public class TaskAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("TaskApp", "onReceive");

        // 通知の設定を行う
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.small_icon);
        builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.large_icon));
        builder.setWhen(System.currentTimeMillis());
        builder.setDefaults(Notification.DEFAULT_ALL);
        builder.setAutoCancel(true);

        // EXTRA_TASK から Task の id を取得して、 id から Task のインスタンスを取得する
        int taskId = intent.getIntExtra(MainActivity.EXTRA_TASK, -1);
        Realm realm = Realm.getDefaultInstance();
        Task task = realm.where(Task.class).equalTo("id", taskId).findFirst();
        realm.close();

        // タスクの情報を設定する
        builder.setTicker(task.getTitle()); // 5.0以降は表示されない
        builder.setContentTitle(task.getTitle());
        builder.setContentText(task.getContents());

        // 通知をタップしたらアプリを起動するようにする
        Intent startAppIntent = new Intent(context, MainActivity.class);
        startAppIntent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, startAppIntent, 0);
        builder.setContentIntent(pendingIntent);

        // 通知を表示する
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(task.getId(), builder.build());
    }
}
