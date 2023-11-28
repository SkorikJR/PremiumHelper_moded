package com.appboosty.blytics;

import android.content.Context;
import android.content.SharedPreferences;

import com.appboosty.blytics.model.Counter;
import com.google.gson.Gson;

/**
 * Created by Sergey B on 11.05.2018.
 */
class GlobalCounterRepository extends CounterRepository {

    private final SharedPreferences preferences;

    public GlobalCounterRepository(Context context) {
        preferences = context.getSharedPreferences("com.appboosty.blytics.counters.global", Context.MODE_PRIVATE);
    }

    @Override
    public Counter getCounter(String eventName, String counterName) {

        if (preferences.contains(Counter.fullName(eventName, counterName))) {
            String json = preferences.getString(Counter.fullName(eventName, counterName), null);
            return new Gson().fromJson(json, Counter.class);
        }

        return null;
    }

    @Override
    protected void storeCounter(Counter c) {
        String json = new Gson().toJson(c);
        preferences.edit().putString(c.getFullName(), json).apply();
    }

}
