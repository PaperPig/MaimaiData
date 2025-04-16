package com.bakapiano.maimai.updater.vpn.tunnel;

import android.util.Log;

import com.bakapiano.maimai.updater.crawler.CrawlerCaller;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Locale;

public class HttpCapturerTunnel extends Tunnel {
    private static final String TAG = "HttpCapturerTunnel";

    public HttpCapturerTunnel(InetSocketAddress serverAddress, Selector selector) throws Exception {
        super(serverAddress, selector);
    }

    public HttpCapturerTunnel(SocketChannel innerChannel, Selector selector) throws Exception {
        super(innerChannel, selector);
    }

    @Override
    protected void onConnected(ByteBuffer buffer) throws Exception {
        onTunnelEstablished();
    }

    @Override
    protected void beforeSend(ByteBuffer buffer) throws Exception {
        String body = new String(buffer.array());
        if (!body.contains("HTTP")) return;

        // Extract http target from http packet
        String[] lines = body.split("\r\n");
        String path = lines[0].split(" ")[1];
        String host = "";
        for (String line : lines) {
            if (line.toLowerCase(Locale.ROOT).startsWith("host")) {
                host = line.substring(4);
                while (host.startsWith(":") || host.startsWith(" ")) {
                    host = host.substring(1);
                }
                while (host.endsWith("\n") || host.endsWith("\r") || host.endsWith(" ")) {
                    host = host.substring(0, host.length() - 1);
                }
            }
        }
        if (!path.startsWith("/")) path = "/" + path;

        String url = "http://" + host + path;
        Log.d(TAG, "HTTP url: " + url);

        // If it's a auth redirect request, catch it
        if (url.startsWith("http://tgk-wcaime.wahlap.com/wc_auth/oauth/callback/maimai-dx")) {
            Log.d(TAG, "Auth request caught!");
            CrawlerCaller.fetchData(url);
        }
    }

    @Override
    protected void afterReceived(ByteBuffer buffer) {
    }

    @Override
    protected boolean isTunnelEstablished() {
        return true;
    }

    @Override
    protected void onDispose() {
        // TODO Auto-generated method stub

    }
}
