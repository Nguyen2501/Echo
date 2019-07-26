package com.innovavn.echo;

import android.app.Application;
import android.util.Log;

public class AlexaApplication extends Application {
    // Our Amazon application product ID, this is passed to the server when we authenticate
    public static final String PRODUCT_ID = "interactive_conversation";

    //Our Application instance if we need to reference it directly
    private static AlexaApplication mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        if (BuildConfig.DEBUG) {
//            Log.i("AlexaApplication", "onCreate: ");
        }
    }

    /**
     * Return a reference to our mInstance instance
     * @return our current application instance, created in onCreate()
     */
    public static AlexaApplication getInstance(){
        return mInstance;
    }
}
