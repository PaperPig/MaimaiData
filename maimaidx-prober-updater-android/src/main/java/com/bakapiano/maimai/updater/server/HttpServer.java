package com.bakapiano.maimai.updater.server;

import static com.bakapiano.maimai.updater.ui.DataContext.HookHost;

import android.util.Log;

import com.bakapiano.maimai.updater.crawler.CrawlerCaller;
import com.bakapiano.maimai.updater.crawler.WechatCrawler;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;


public class HttpServer extends NanoHTTPD {
    public static int Port = 8284;
    private final static String TAG = "HttpServer";

    protected HttpServer() throws IOException {
        super(Port);
    }

    @Override
    public void start() throws IOException {
        super.start();
        Log.d(TAG, "Http server running on http://localhost:" + Port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        Log.d(TAG, "Serve request: " + session.getUri());
        switch (session.getUri()) {
            case "/auth":
                return redirectToWechatAuthUrl(session);
            default:
                return redirectToAuthUrlWithRandomParm(session);
        }
    }

    // To avoid fu***ing cache of wechat webview client
    private Response redirectToAuthUrlWithRandomParm(IHTTPSession session) {
        Response r = newFixedLengthResponse(Response.Status.REDIRECT, MIME_HTML, "");
        r.addHeader("Location", "http://" + HookHost + "/auth?random=" + System.currentTimeMillis());
        return r;
    }

    private Response redirectToWechatAuthUrl(IHTTPSession session) {
        String url = CrawlerCaller.getWechatAuthUrl();
        if (url == null)
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_HTML, "");
        Log.d(TAG, url);

        Response r = newFixedLengthResponse(Response.Status.REDIRECT, MIME_HTML, "");
        r.addHeader("Location", url);
        r.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        r.addHeader("Pragma", "no-cache");
        r.addHeader("Expires", "0");
        return r;
    }
}
