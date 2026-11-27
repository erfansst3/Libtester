package com.erfansst.libtester;
import android.app.Application;
import com.adivery.sdk.Adivery;
public class App extends Application{
@Override public void onCreate(){super.onCreate();Adivery.setLoggingEnabled(true);Adivery.configure(this,"a2e19f45-f3d5-4abb-800b-3a02ae635736");}
}
