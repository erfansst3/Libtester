package com.erfansst.libtester;

import android.app.Activity;
import android.os.Bundle;
import android.os.Process;
import android.widget.*;
import java.io.*;

public final class MainActivity{
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
        maps.setOnClickListener(v->out.setText(nativeTest(mapsPath,"all")));
        String[] n={"libc openat","libc open","direct syscall openat","direct syscall openat2","fopen","pread","mmap","ioctl","system cat","execve cat","Root / UID","Run all"};
        for(String x:n){
            Button q=new Button(this);
            q.setText(x);
            box.addView(q);
            q.setOnClickListener(v->{
                if("Run all".equals(x))out.setText(nativeTest(path,"all")+"\n\n"+rootTest());
                else if("Root / UID".equals(x))out.setText(rootTest());
                else out.append("\n>>> "+x+"\n"+nativeTest(path,x)+"\n");
            });
        }
        box.addView(out);
        ScrollView s=new ScrollView(this);
        s.addView(box);
        setContentView(s);
    }
    String catCmd(){
        return "=== CMD CAT ===\n"+cmd("cat "+path);
    }
    String rootTest(){
        return "=== ROOT / UID ===\n"+cmd("which su")+"\n"+cmd("id")+"\n"+cmd("id -u")+"\nPROCESS_UID="+Process.myUid()+"\nSU_ID="+cmd("su -c id");
    }
    String cmd(String c){
        try{
            java.lang.Process p=Runtime.getRuntime().exec(new String[]{"sh","-c",c});
            BufferedReader r=new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader e=new BufferedReader(new InputStreamReader(p.getErrorStream()));
            StringBuilder z=new StringBuilder(),bb=new StringBuilder();
            String x;
            while((x=r.readLine())!=null)z.append(x).append('\n');
            while((x=e.readLine())!=null)bb.append(x).append('\n');
            int n=p.waitFor();
            return c+" [exit="+n+"]\n"+(z.length()>0?z:bb).toString().trim();
        }catch(Throwable e){return c+" [error="+e.getClass().getSimpleName()+"]";}
    }
    static native String nativeTest(String p,String m);
    static{try{System.loadLibrary("tester");}catch(Throwable ignored){}}
}