package com.bakapiano.maimai.updater.crawler;

import static com.bakapiano.maimai.updater.Util.getDifficulties;

import android.os.Handler;

import com.bakapiano.maimai.updater.ui.DataContext;
import com.bakapiano.maimai.updater.vpn.core.LocalVpnService;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class CrawlerCaller {
    private static final String TAG = "CrawlerCaller";
    private static final Handler m_Handler = new Handler();
    public static LocalVpnService.onStatusChangedListener listener;

    static public String getWechatAuthUrl() {
        try {
            WechatCrawler crawler = new WechatCrawler();
            String url = crawler.getWechatAuthUrl();
            return url;
        } catch (IOException error) {
            writeLog("获取微信登录url时出现错误:");
            writeLog(error);
            return null;
        }
    }

    static public void writeLog(String text) {
        m_Handler.post(() -> listener.onLogReceived(text));
    }

    static public void writeLog(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();
        m_Handler.post(() -> listener.onLogReceived(exceptionAsString));
    }

    static public void fetchData(String authUrl) {
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                LocalVpnService.IsRunning = false;
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                writeLog(e);
            }

            try {
                WechatCrawler crawler = new WechatCrawler();
                crawler.fetchAndUploadData(DataContext.Username, DataContext.Password, getDifficulties(), authUrl);
            } catch (IOException e) {
                writeLog(e);
            }
        }).start();
    }

    static public void verifyAccount(String username, String password, Callback callback) {
        new Thread(() -> {
            try {
                WechatCrawler crawler = new WechatCrawler();
                Boolean result = crawler.verifyProberAccount(username, password);
                callback.onResponse(result);
            } catch (IOException error) {
                error.printStackTrace();
                callback.onError(error);
            }
        }).start();
    }

    static public void getLatestVersion(Callback callback)
    {
        new Thread(() -> {
            WechatCrawler crawler = new WechatCrawler();
            String result = crawler.getLatestVersion();
            callback.onResponse(result);
        }).start();
    }

}
