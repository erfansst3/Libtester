package com.erfansst.libtester;

import android.app.Activity;
import android.os.Bundle;
import android.os.Process;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private TextView out;
    private String path;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        path = "/proc/" + Process.myPid() + "/kossher";

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(20,20,20,20);

        TextView title = new TextView(this);
        title.setTextSize(15);
        title.setText("Native /proc tester\n" + path);
        box.addView(title);

        out = new TextView(this);
        out.setTextSize(12);
        out.setPadding(0,20,0,20);

        String[] names = {
            "libc openat", "libc open", "direct syscall openat",
            "direct syscall openat2", "fopen", "pread",
            "mmap", "ioctl", "Run all"
        };
        for (String name : names) {
            Button b = new Button(this);
            b.setText(name);
            box.addView(b);
            if ("Run all".equals(name)) {
                b.setOnClickListener(v -> run("all"));
            } else {
                b.setOnClickListener(v -> run(name));
            }
        }

        box.addView(out);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(box);
        setContentView(scroll);
    }

    private void run(String mode) {
        try {
            if (!"all".equals(mode)) {
                out.append("\n>>> " + mode + "\n");
                out.append(nativeTest(path, mode) + "\n");
            } else {
                out.setText(nativeTest(path, "all"));
            }
        } catch (Throwable e) {
            out.append("\nJAVA ERROR: " + e.getClass().getName() + ": " + String.valueOf(e.getMessage()) + "\n");
        }
    }

    private static native String nativeTest(String path, String mode);

    static {
        try {
            System.loadLibrary("tester");
        } catch (Throwable e) {
            // Defer the visible error to the first button invocation.
        }
    }
}
