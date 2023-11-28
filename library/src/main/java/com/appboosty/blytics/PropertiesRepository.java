package com.appboosty.blytics;

import java.util.Map;

/**
 * Created by Sergey B on 17.05.2018.
 */
interface PropertiesRepository {

    String getProperty(String name, String defaultValue);
    <T> void setProperty(String name, T value);

    String getUserProperty(String name, String defaultValue);
    <T> void setUserProperty(String name, T value);

    Map<String, String> getUserProperties();
}
