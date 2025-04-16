package com.bakapiano.maimai.updater.vpn.tunnel.httpconnect;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.util.Locale;

import com.bakapiano.maimai.updater.crawler.WechatCrawler;
import com.bakapiano.maimai.updater.ui.DataContext;
import com.bakapiano.maimai.updater.vpn.core.Constant;
import com.bakapiano.maimai.updater.vpn.core.ProxyConfig;
import com.bakapiano.maimai.updater.vpn.tunnel.Tunnel;

public class HttpConnectTunnel extends Tunnel {
    private static final String TAG = "HttpConnectTunnel";
    private boolean m_TunnelEstablished;
    private boolean m_FirstPacket;
    private HttpConnectConfig m_Config;

    public HttpConnectTunnel(HttpConnectConfig config, Selector selector) throws IOException {
//        super(config.ServerAddress, selector);
        super(new InetSocketAddress(DataContext.ProxyHost, DataContext.ProxyPort), selector);
        m_Config = config;
    }

    @Override
    protected void onConnected(ByteBuffer buffer) throws Exception {
        String request;
//        if (TextUtils.isEmpty(m_Config.UserName) || TextUtils.isEmpty(m_Config.Password)) {
        request = String.format(Locale.ENGLISH, "CONNECT %s:%d HTTP/1.0\r\n" +
                        "Proxy-Connection: keep-alive\r\n" +
                        "User-Agent: %s\r\n" +
                        "X-App-Install-ID: %s" +
                        "\r\n\r\n",
                m_DestAddress.getHostName(),
                m_DestAddress.getPort(),
                ProxyConfig.Instance.getUserAgent(),
                ProxyConfig.AppInstallID);
//        } else {
//            request = String.format(Locale.ENGLISH, "CONNECT %s:%d HTTP/1.0\r\n" +
//                            "Proxy-Authorization: Basic %s\r\n" +
//                            "Proxy-Connection: keep-alive\r\n" +
//                            "User-Agent: %s\r\n" +
//                            "X-App-Install-ID: %s" +
//                            "\r\n\r\n",
//                    m_DestAddress.getHostName(),
//                    m_DestAddress.getPort(),
//                    makeAuthorization(),
//                    ProxyConfig.Instance.getUserAgent(),
//                    ProxyConfig.AppInstallID);
//        }
        Log.i(TAG, "onConnected: " + request);
        buffer.clear();
        buffer.put(request.getBytes());
        buffer.flip();
        if (this.write(buffer, true)) {
            this.beginReceive();
        }
    }

    private String makeAuthorization() {
        return Base64.encodeToString((m_Config.UserName + ":" + m_Config.Password).getBytes(), Base64.DEFAULT).trim();
    }

    @Override
    protected void afterReceived(ByteBuffer buffer) throws Exception {
        if (!m_TunnelEstablished) {
            String response = new String(buffer.array(), buffer.position(), 12);
            Log.i(TAG, m_DestAddress.toString());
            Log.i(TAG, "afterReceived: " + response);
            if (response.matches("^HTTP/1.[01] 200$")) {
                buffer.limit(buffer.position());
            } else {
                throw new Exception(String.format(Locale.ENGLISH, "Proxy server responsed an error: %s", response));
            }

            m_TunnelEstablished = true;
            m_FirstPacket = true;
            super.onTunnelEstablished();
        } else if (m_FirstPacket) {
            // Workaround for mysterious "Content-Length: 0" after handshaking.
            // Possible a bug of golang.
            // Also need to remove "\r\n" afterward.
            String response = new String(buffer.array(), buffer.position(), 17);
            if (response.matches("^Content-Length: 0$")) {
                buffer.position(buffer.position() + 17);
            }
            while (true) {
                response = new String(buffer.array(), buffer.position(), 2);
                if (response.matches("^\r\n$")) {
                    buffer.position(buffer.position() + 2);
                } else {
                    break;
                }
            }
            m_FirstPacket = false;
        }
    }

    @Override
    protected boolean isTunnelEstablished() {
        return m_TunnelEstablished;
    }

    @Override
    protected void beforeSend(ByteBuffer buffer) throws Exception {

    }

    @Override
    protected void onDispose() {
        m_Config = null;
    }


}