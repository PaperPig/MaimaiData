package com.paperpig.maimaidata.crawler;


import static com.paperpig.maimaidata.crawler.CrawlerCaller.finishUpdate;
import static com.paperpig.maimaidata.crawler.CrawlerCaller.onError;
import static com.paperpig.maimaidata.crawler.CrawlerCaller.startAuth;
import static com.paperpig.maimaidata.crawler.CrawlerCaller.writeLog;

import android.util.Log;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.ConnectionSpec;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.TlsVersion;

public class WechatCrawler {
    // Make this true for Fiddler to capture https request
    private static final boolean IGNORE_CERT = false;

    private static final int MAX_RETRY_COUNT = 4;

    private static final String TAG = "Crawler";

    private static final String WX_WINDOWS_UA =
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/81.0.4044.138 Safari/537.36 NetType/WIFI " +
                    "MicroMessenger/7.0.20.1781(0x6700143B) WindowsWechat(0x6307001e)";


    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final MediaType TEXT = MediaType.parse("text/plain");

    private static final SimpleCookieJar jar = new SimpleCookieJar();

    private static final Map<Integer, String> diffMap = new HashMap<>();

    private static OkHttpClient client = null;

    public WechatCrawler() {
        diffMap.put(0, "Basic");
        diffMap.put(1, "Advance");
        diffMap.put(2, "Expert");
        diffMap.put(3, "Master");
        diffMap.put(4, "Re:Master");
        buildHttpClient(false);
    }

    private static void uploadData(Integer diff, String data, Integer retryCount) {
        Request request = new Request.Builder().url("https://www.diving-fish.com/api/pageparser/page").addHeader("content-type", "text/plain").post(RequestBody.create(data, TEXT)).build();

        Call call = client.newCall((request));

        try {
            Response response = call.execute();
            String result = response.body().string();
            writeLog(diffMap.get(diff) + " 难度数据上传状态：" + result);
        } catch (Exception e) {
            retryUploadData(e, diff, data, retryCount);
        }
    }

    private static void retryUploadData(Exception e, Integer diff, String data, Integer currentRetryCount) {
        writeLog("上传 " + diffMap.get(diff) + " 分数数据至水鱼查分器时出现错误: " + e);
        if (currentRetryCount < MAX_RETRY_COUNT) {
            writeLog("进行第" + currentRetryCount + "次重试");
            uploadData(diff, data, currentRetryCount + 1);
        } else {
            writeLog(diffMap.get(diff) + "难度数据上传失败！");
        }
    }

    private static void fetchAndUploadData(String username, String password, Set<Integer> difficulties) {
        List<CompletableFuture<Object>> tasks = new ArrayList<>();
        for (Integer diff : difficulties) {
            tasks.add(CompletableFuture.supplyAsync(() -> {
                fetchAndUploadData(username, password, diff, 1);
                return null;
            }));
        }
        for (CompletableFuture<Object> task : tasks) {
            task.join();
        }
    }

    private static void fetchAndUploadData(String username, String password, Integer diff, Integer retryCount) {
        writeLog("开始获取 " + diffMap.get(diff) + " 难度的数据");
        Request request = new Request.Builder().url("https://maimai.wahlap.com/maimai-mobile/record/musicGenre/search/?genre=99&diff=" + diff).build();

        Call call = client.newCall(request);
        try {
            Response response = call.execute();
            String data = Objects.requireNonNull(response.body()).string();
//            Matcher matcher = Pattern.compile("<html.*>([\\s\\S]*)</html>").matcher(data);
//            if (matcher.find()) data = Objects.requireNonNull(matcher.group(1));
//            data = Pattern.compile("\\s+").matcher(data).replaceAll(" ");

            // Upload data to maimai-prober
            writeLog(diffMap.get(diff) + " 难度的数据已获取，正在上传至水鱼查分器");
            uploadData(diff, "<login><u>" + username + "</u><p>" + password + "</p></login>" + data, 1);
        } catch (Exception e) {
            retryFetchAndUploadData(e, username, password, diff, retryCount);
        }
    }

    private static void retryFetchAndUploadData(Exception e, String username, String password, Integer diff, Integer currentRetryCount) {
        writeLog("获取 " + diffMap.get(diff) + " 难度数据时出现错误: " + e);
        if (currentRetryCount < MAX_RETRY_COUNT) {
            writeLog("进行第" + currentRetryCount + "次重试");
            fetchAndUploadData(username, password, diff, currentRetryCount + 1);
        } else {
            writeLog(diffMap.get(diff) + "难度数据更新失败！");
        }
    }

    protected String getWechatAuthUrl() throws IOException {
        this.buildHttpClient(true);

        Request request = new Request.Builder().addHeader("Host", "tgk-wcaime.wahlap.com").addHeader("Upgrade-Insecure-Requests", "1").addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 12; IN2010 Build/RKQ1.211119.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/86.0.4240.99 XWEB/4317 MMWEBSDK/20220903 Mobile Safari/537.36 MMWEBID/363 MicroMessenger/8.0.28.2240(0x28001C57) WeChat/arm64 Weixin NetType/WIFI Language/zh_CN ABI/arm64").addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/wxpic,image/tpg,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9").addHeader("X-Requested-With", "com.tencent.mm").addHeader("Sec-Fetch-Site", "none").addHeader("Sec-Fetch-Mode", "navigate").addHeader("Sec-Fetch-User", "?1").addHeader("Sec-Fetch-Dest", "document").addHeader("Accept-Encoding", "gzip, deflate").addHeader("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7").url("https://tgk-wcaime.wahlap.com/wc_auth/oauth/authorize/maimai-dx").build();

        Call call = client.newCall(request);
        Response response = call.execute();
        String url = response.request().url().toString().replace("redirect_uri=https", "redirect_uri=http");

        Log.d(TAG, "Auth url:" + url);
        return url;
    }

    public void fetchAndUploadData(String username, String password, Set<Integer> difficulties, String wechatAuthUrl) throws IOException {
        if (wechatAuthUrl.startsWith("http"))
            wechatAuthUrl = wechatAuthUrl.replaceFirst("http", "https");

        jar.clearCookieStroe();

        // Login wechat
        try {
            startAuth();
            writeLog("开始登录net，请稍后...");
            this.loginWechat(wechatAuthUrl);
            writeLog("登陆完成");
        } catch (Exception error) {
            writeLog("登陆时出现错误:\n");
            onError(error);
            return;
        }

        // Fetch maimai data
        try {
            this.fetchMaimaiData(username, password, difficulties);
            writeLog("maimai 数据更新完成");
            finishUpdate();
        } catch (Exception error) {
            writeLog("maimai 数据更新时出现错误:");
            onError(error);
        }
    }


    private void loginWechat(String wechatAuthUrl) throws Exception {
        this.buildHttpClient(true);

        Log.d(TAG, wechatAuthUrl);

        Headers headers = new Headers.Builder()
                .add("Connection", "keep-alive")
                .add("Upgrade-Insecure-Requests", "1")
                .add("User-Agent", WX_WINDOWS_UA)
                .add(
                        "Accept",
                        "text/html,application/xhtml+xml,application/xml;q=0.9," +
                                "image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
                )
                .add("Sec-Fetch-Site", "none")
                .add("Sec-Fetch-Mode", "navigate")
                .add("Sec-Fetch-User", "?1")
                .add("Sec-Fetch-Dest", "document")
                .add("Accept-Encoding", "gzip, deflate, br")
                .add("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7")
                .build();

        Request request = new Request.Builder()
                .headers(headers)
                .get()
                .url(wechatAuthUrl)
                .build();

        Call call = client.newCall(request);
        Response response = call.execute();

        try {
            String responseBody = response.body().string();
            Log.d(TAG, responseBody);
        } catch (NullPointerException error) {
            onError(error);
        }

        int code = response.code();
        writeLog(String.valueOf(code));
        if (code >= 400) {
            Exception exception = new Exception("登陆时出现错误，请重试！");
            onError(exception);
            throw new Exception(exception);
        }

        // Handle redirect manually
        String location = response.headers().get("Location");
        if (response.code() >= 300 && response.code() < 400 && location != null) {
            request = new Request.Builder().url(location).get().build();
            call = client.newCall(request);
            call.execute().close();
        }
    }

    private void fetchMaimaiData(String username, String password, Set<Integer> difficulties) throws IOException {
        this.buildHttpClient(false);
        fetchAndUploadData(username, password, difficulties);
    }

    private void buildHttpClient(boolean followRedirect) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        if (IGNORE_CERT) ignoreCertBuilder(builder);

        builder.connectTimeout(120, TimeUnit.SECONDS);
        builder.readTimeout(120, TimeUnit.SECONDS);
        builder.writeTimeout(120, TimeUnit.SECONDS);
        builder.followRedirects(followRedirect);
        builder.followSslRedirects(followRedirect);

        builder.cookieJar(jar);

        // No cache for http request
        builder.cache(null);
        Interceptor noCacheInterceptor = chain -> {
            Request request = chain.request();
            Request.Builder builder1 = request.newBuilder().addHeader("Cache-Control", "no-cache");
            request = builder1.build();
            return chain.proceed(request);
        };
        builder.addInterceptor(noCacheInterceptor);

        // Fix SSL handle shake error
        ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.COMPATIBLE_TLS).tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_1, TlsVersion.TLS_1_0).allEnabledCipherSuites().build();
        // 兼容http接口
        ConnectionSpec spec1 = new ConnectionSpec.Builder(ConnectionSpec.CLEARTEXT).build();
        builder.connectionSpecs(Arrays.asList(spec, spec1));

        builder.pingInterval(3, TimeUnit.SECONDS);

        client = builder.build();
    }

    private void ignoreCertBuilder(OkHttpClient.Builder builder) {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[]{};
                }
            }};
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        } catch (Exception ignored) {

        }
    }
}
