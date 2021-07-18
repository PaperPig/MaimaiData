package com.paperpig.maimaidata.glide;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;
import com.paperpig.maimaidata.network.OkHttpUrlLoader;
import com.paperpig.maimaidata.network.UnsafeOkHttpClient;

import org.jetbrains.annotations.NotNull;

import java.io.InputStream;

import io.reactivex.annotations.NonNull;
import okhttp3.OkHttpClient;

@GlideModule
public class MyGlideModule extends AppGlideModule {

    @Override
    public void applyOptions(@NotNull Context context, @NotNull GlideBuilder builder) {
        super.applyOptions(context, builder);
    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        OkHttpClient okHttpClient = UnsafeOkHttpClient.getUnsafeOkHttpClient().build();
        registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(okHttpClient));
    }
}
