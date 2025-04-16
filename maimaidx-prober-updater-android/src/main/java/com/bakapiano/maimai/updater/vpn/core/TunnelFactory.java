package com.bakapiano.maimai.updater.vpn.core;

import android.util.Log;

import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import com.bakapiano.maimai.updater.server.HttpRedirectServer;
import com.bakapiano.maimai.updater.server.HttpServer;
import com.bakapiano.maimai.updater.ui.DataContext;
import com.bakapiano.maimai.updater.vpn.tunnel.Config;
import com.bakapiano.maimai.updater.vpn.tunnel.HttpCapturerTunnel;
import com.bakapiano.maimai.updater.vpn.tunnel.RawTunnel;
import com.bakapiano.maimai.updater.vpn.tunnel.Tunnel;
import com.bakapiano.maimai.updater.vpn.tunnel.httpconnect.HttpConnectConfig;
import com.bakapiano.maimai.updater.vpn.tunnel.httpconnect.HttpConnectTunnel;

public class TunnelFactory {
    private final static String TAG = "TunnelFactory";

    public static Tunnel wrap(SocketChannel channel, Selector selector) throws Exception {
        return new RawTunnel(channel, selector);
    }

    public static Tunnel createTunnelByConfig(InetSocketAddress destAddress, Selector selector) throws Exception {
        Log.d(TAG, destAddress.getHostName() + ":" + destAddress.getPort());
        if (destAddress.getAddress() != null)
        {
            Log.d(TAG, destAddress.getAddress().toString());
        }
        // Use online service
        if (DataContext.CompatibleMode) {
            if (destAddress.getHostName().endsWith("wahlap.com") && destAddress.getPort() == 80) {
                Log.d(TAG, "Request for wahlap.com caught");
                return new HttpCapturerTunnel(
                        new InetSocketAddress("127.0.0.1", HttpRedirectServer.Port), selector);
            } else {
//                Config config = ProxyConfig.Instance.getDefaultTunnelConfig(destAddress);
//                return new HttpConnectTunnel((HttpConnectConfig) config, selector);
                if (destAddress.isUnresolved())
                    return new RawTunnel(new InetSocketAddress(destAddress.getHostName(), destAddress.getPort()), selector);
                else
                    return new RawTunnel(destAddress, selector);
            }
//            else if (destAddress.isUnresolved())
//                return new RawTunnel(new InetSocketAddress(destAddress.getHostName(), destAddress.getPort()), selector);
//            else
//                return new RawTunnel(destAddress, selector);
        }
        // Use local service
        else {
//            if (destAddress.getHostName().endsWith(DataContext.HookHost) ||
//                    (destAddress.getAddress() != null && destAddress.getAddress().toString().equals(DataContext.HookHost))) {
//                Log.d(TAG, "Request to" + DataContext.HookHost + " caught");
//                return new RawTunnel(
//                        new InetSocketAddress("127.0.0.1", HttpServer.Port), selector);
//            } else
                if (destAddress.getHostName().endsWith("wahlap.com") && destAddress.getPort() == 80) {
                Log.d(TAG, "Request for wahlap.com caught");
                return new HttpCapturerTunnel(
                        new InetSocketAddress("127.0.0.1", HttpRedirectServer.Port), selector);
            }
//        else if (destAddress.getHostName().endsWith("wahlap.com") && destAddress.getPort() != 80)
//        {
//            Config config = ProxyConfig.Instance.getDefaultTunnelConfig(destAddress);
//            return new HttpConnectTunnel((HttpConnectConfig) config, selector);
//        }
            else if (destAddress.isUnresolved())
                return new RawTunnel(new InetSocketAddress(destAddress.getHostName(), destAddress.getPort()), selector);
            else
                return new RawTunnel(destAddress, selector);
        }

    }

}
