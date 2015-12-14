package org.looseend.director;

import android.app.Application;

import timber.log.Timber;

/**
 * Created by jsinglet on 08/12/2015.
 */
public class DirectorApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }
}
