package com.appboosty.blytics.platforms;

import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.appboosty.blytics.AnalyticsPlatform;
import com.appboosty.blytics.model.Session;

import timber.log.Timber;

/**
 * Created by Sergey B on 10.05.2018.
 */
public class TestLogPlatform extends AnalyticsPlatform {

    @Override
    public String getName() {
        return "Test";
    }

    @Override
    public boolean isEnabled(@NonNull Application application) {
        return true;
    }

    @Override
    public void initialize(@NonNull Application application, boolean debug) {
        super.initialize(application, debug);
        Timber.tag("TestLogPlatform").i("Initialized");
    }

    @Override
    public void track(@NonNull String event, @NonNull Bundle params) {
        Timber.tag("TestLogPlatform").d("Event: " + event + " Params: " + params.toString());
    }

    @Override
    public void onSessionStart(Session session) {
        Timber.tag("TestLogPlatform").d("Session start: %s", session.getId());
    }

    @Override
    public void onSessionFinish(Session session) {
        Timber.tag("TestLogPlatform").d("Session finish: %s", session.getId());
    }

    @Override
    public void setUserId(@NonNull String userId) {
        Timber.tag("TestLogPlatform").d("Set user id: %s", userId);
    }

    @Override
    public void setUserProperty(String property, String value) {
        Timber.tag("TestLogPlatform").d("Set user property: " + property + "=" + value);
    }

    @Override
    public int getMaximumParametersCount() {
        return Integer.MAX_VALUE;
    }
}
