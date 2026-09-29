package com.erfansst.libtester;

import android.app.Activity;
import android.os.Bundle;
import android.os.Process;
import android.widget.*;
import java.io.*;

public final class MainActivity extends Activity{
    TextView out;
    String path;
    String mapsPath;
    public void onCreate(Bundle b){
        super.onCreate(b);
        path="/proc/"+Process.myPid()+"/kossher";
        mapsPath="/proc/"+Process.myPid()+"/maps";
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(20,20,20,20);
        TextView t=new TextView(this);
        t.setText("Native /proc tester\nKOSSHER: "+path+"\nMAPS: "+mapsPath);
        box.addView(t);
        out=new TextView(this);
        out.setTextSize(12);
        Button maps=new Button(this);
        maps.setText("Test /proc/PID/maps");
        box.addView(maps);
        maps.setOnClickListener(v->out.setText(nativeTest(mapsPath,"maps")));
        String[] n={"libc openat","libc open","direct syscall openat","direct syscall openat2","fopen","pread","mmap","ioctl","system cat","execve cat","Root / UID","Run all"};
        for(String x:n){
            Button q=new Button(this);
            q.setText(x);
            box.addView(q);
            q.setOnClickListener(v->{
                if("Run all".equals(x))out.setText(nativeTest(path,"all")+"\n\n"+nativeTest(path,"root"));
                else if("Root / UID".equals(x))out.setText(nativeTest(path,"root"));
                else out.append("\n>>> "+x+"\n"+nativeTest(path,x)+"\n");
            });
        }
        box.addView(out);
        ScrollView s=new ScrollView(this);
        s.addView(box);
        setContentView(s);
    }
    static native String nativeTest(String p,String m);
    static{try{System.loadLibrary("tester");}catch(Throwable ignored){}}
}