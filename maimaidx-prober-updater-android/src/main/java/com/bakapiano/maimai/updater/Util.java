package com.bakapiano.maimai.updater;

import static androidx.core.content.ContextCompat.getSystemService;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import com.bakapiano.maimai.updater.ui.DataContext;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Util {
    public static Set<Integer> getDifficulties() {
        Set<Integer> set = new HashSet<>();
        if (DataContext.BasicEnabled)
            set.add(0);
        if (DataContext.AdvancedEnabled)
            set.add(1);
        if (DataContext.ExpertEnabled)
            set.add(2);
        if (DataContext.MasterEnabled)
            set.add(3);
        if (DataContext.RemasterEnabled)
            set.add(4);
        return set;
    }

    public static void copyText(Context context, String link) {
        ClipboardManager clipboard = Objects.requireNonNull(getSystemService(context, ClipboardManager.class));
        ClipData clip = ClipData.newPlainText("link", link);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "已复制链接，请在微信中粘贴并打开", Toast.LENGTH_SHORT).show();
    }
}
