package com.paperpig.maimaidata.network.server;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;

public class HttpServerService extends Service {
    private static final String TAG = "HttpServerService";
    private HttpServer httpServer;
    private HttpRedirectServer httpRedirectServer;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("HttpService", "Http service on create");
        try {
            if (this.httpServer != null) this.httpServer.stop();
            if (this.httpRedirectServer != null) this.httpRedirectServer.stop();
            this.httpServer = new HttpServer();
            this.httpRedirectServer = new HttpRedirectServer();
        } catch (IOException e) {
            Log.d(TAG, "Error while create HttpServerService: " + e);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d("HttpService", "Http service on start command");
        try {
            this.httpServer.start();
            this.httpRedirectServer.start();
        } catch (IOException e) {
            Log.d(TAG, "Error while start HttpServerService: " + e);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.httpServer.stop();
        this.httpRedirectServer.stop();
    }
}
