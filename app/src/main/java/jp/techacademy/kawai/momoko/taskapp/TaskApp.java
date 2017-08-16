package jp.techacademy.kawai.momoko.taskapp;

import android.app.Application;

import io.realm.Realm;

/**
 * Created by momon on 2017/08/15.
 */

public class TaskApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Realm.init(this);
    }
}
