package com.erfansst.libtester;

import android.app.Activity;
import android.os.Bundle;
import android.os.Process;
import android.widget.*;

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

        TextView kTitle=new TextView(this);
        kTitle.setText("\n=== KOSSHER TESTS ===");
        box.addView(kTitle);

        String[] kossher={
            "libc openat",
            "libc open",
            "direct syscall openat",
            "direct syscall openat2",
            "fopen",
            "pread",
            "mmap",
            "ioctl",
            "system cat",
            "execve cat",
            "Root / UID",
            "Run all"
        };

        for(String x:kossher){
            addButton(box,x,path,x);
        }

        TextView mTitle=new TextView(this);
        mTitle.setText("\n=== MAPS TESTS ===");
        box.addView(mTitle);

        String[] mapsTests={
            "libc openat",
            "libc open",
            "direct syscall openat",
            "fopen",
            "pread",
            "mmap",
            "ioctl",
            "system cat",
            "execve cat"
        };

        for(String x:mapsTests){
            addButton(box,"maps "+x,mapsPath,x);
        }

        TextView iTitle=new TextView(this);
        iTitle.setText("\n=== INOTIFY TESTS (15s) ===");
        box.addView(iTitle);

        String[] inotifyTests={"mem","maps","smaps","status","kossher","/proc/self"};
        for(String x:inotifyTests){
            String p=x.startsWith("/")?x:"/proc/"+Process.myPid()+"/"+x;
            Button q=new Button(this);
            q.setText("watch "+p);
            box.addView(q);
            q.setOnClickListener(v->{
                out.setText("Waiting for inotify events...\n"+p+"\n");
                new Thread(()->{
                    String r=nativeTest(p,"inotify");
                    runOnUiThread(()->out.append(r+"\n"));
                }).start();
            });
        }

        out=new TextView(this);
        out.setTextSize(12);
        box.addView(out);

        ScrollView s=new ScrollView(this);
        s.addView(box);
        setContentView(s);
    }

    void addButton(LinearLayout box,String label,String testPath,String mode){
        Button q=new Button(this);
        q.setText(label);
        box.addView(q);

        q.setOnClickListener(v->{
            if("Run all".equals(mode)){
                out.setText(nativeTest(testPath,"all")+"\n\n"+nativeTest(testPath,"root"));
            }else if("Root / UID".equals(mode)){
                out.setText(nativeTest(testPath,"root"));
            }else{
                out.append("\n>>> "+label+"\n"+nativeTest(testPath,mode)+"\n");
            }
        });
    }

    static native String nativeTest(String p,String m);

    static{
        try{
            System.loadLibrary("tester");
        }catch(Throwable ignored){}
    }
}
