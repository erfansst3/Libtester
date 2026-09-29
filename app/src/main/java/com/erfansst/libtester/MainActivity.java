package com.erfansst.libtester;
import android.app.Activity;
import android.os.Bundle;
import android.os.Process;
import android.widget.TextView;
public final class MainActivity extends Activity {
 static { System.loadLibrary("tester"); }
 @Override public void onCreate(Bundle state){super.onCreate(state); TextView tv=new TextView(this); tv.setTextSize(13); tv.setPadding(24,24,24,24); tv.setText(nativeTest("/proc/"+Process.myPid()+"/kossher")); setContentView(tv);}
 private native String nativeTest(String path);
}
