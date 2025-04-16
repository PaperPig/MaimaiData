package com.bakapiano.maimai.updater.ui;

import static com.bakapiano.maimai.updater.Util.copyText;
import static com.bakapiano.maimai.updater.Util.getDifficulties;
import static com.bakapiano.maimai.updater.crawler.CrawlerCaller.writeLog;

import static java.lang.Integer.parseInt;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import java.util.Calendar;
import java.util.Random;

import com.bakapiano.maimai.updater.R;
import com.bakapiano.maimai.updater.crawler.Callback;
import com.bakapiano.maimai.updater.crawler.CrawlerCaller;
import com.bakapiano.maimai.updater.notification.NotificationUtil;
import com.bakapiano.maimai.updater.server.HttpServer;
import com.bakapiano.maimai.updater.server.HttpServerService;
import com.bakapiano.maimai.updater.vpn.core.Constant;
import com.bakapiano.maimai.updater.vpn.core.LocalVpnService;
import com.bakapiano.maimai.updater.vpn.core.ProxyConfig;

public class MainActivity extends AppCompatActivity implements
        OnCheckedChangeListener,
        LocalVpnService.onStatusChangedListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int START_VPN_SERVICE_REQUEST_CODE = 1985;
    private static String GL_HISTORY_LOGS;
    private SwitchCompat switchProxy;
    private TextView textViewLog;
    private ScrollView scrollViewLog;
    private Calendar mCalendar;

    private SharedPreferences mContextSp;

    private void updateTilte() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            if (LocalVpnService.IsRunning) {
                actionBar.setTitle(getString(R.string.connected));
            } else {
                actionBar.setTitle(getString(R.string.disconnected));
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textViewLog = (TextView) findViewById(R.id.textViewLog);

        assert textViewLog != null;
        textViewLog.setText(GL_HISTORY_LOGS);
        textViewLog.setMovementMethod(ScrollingMovementMethod.getInstance());

        mCalendar = Calendar.getInstance();
        LocalVpnService.addOnStatusChangedListener(this);

        mContextSp = this.getSharedPreferences(
                "com.bakapiano.maimai.updater.data",
                Context.MODE_PRIVATE);

        CrawlerCaller.listener = this;

        loadContextData();
    }

    private void inputAddress() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("Http com.bakapiano.maimai.com.bakapiano.maimai.proxy server");
//        final EditText input = new EditText(this);
//        input.setText(ProxyConfig.getHttpProxyServer(this));
//        builder.setView(input);
//        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                String text = input.getText().toString();
//                ProxyConfig.Instance.setProxy(text);
//                ProxyConfig.setHttpProxyServer(MainActivity.this, text);
//            }
//        });
//        builder.setCancelable(false);
//        builder.show();
        @SuppressLint("AuthLeak") String text = "http://user:pass@127.0.0.1:8848";
        ProxyConfig.Instance.setProxy(text);
        ProxyConfig.setHttpProxyServer(MainActivity.this, text);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateTilte();
    }

    String getVersionName() {
        PackageManager packageManager = getPackageManager();
        if (packageManager == null) {
            Log.e(TAG, "null package manager is impossible");
            return null;
        }

        try {
            return packageManager.getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "package not found is impossible", e);
            return null;
        }
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onLogReceived(String logString) {
        mCalendar.setTimeInMillis(System.currentTimeMillis());
        logString = String.format("[%1$02d:%2$02d:%3$02d] %4$s\n",
                mCalendar.get(Calendar.HOUR_OF_DAY),
                mCalendar.get(Calendar.MINUTE),
                mCalendar.get(Calendar.SECOND),
                logString);

        Log.d(Constant.TAG, logString);

        textViewLog.append(logString);
        GL_HISTORY_LOGS = textViewLog.getText() == null ? "" : textViewLog.getText().toString();
    }

    @Override
    public void onStatusChanged(String status, Boolean isRunning) {
        switchProxy.setEnabled(true);
        switchProxy.setChecked(isRunning);
        onLogReceived(status);
        updateTilte();
        Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
    }

    private final Object switchLock = new Object();

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (!switchProxy.isEnabled()) return;
        if (!switchProxy.isPressed()) return;
        saveOptions();
        saveDifficulties();

        if (getDifficulties().isEmpty()) {
            if (isChecked) {
                writeLog("请至少勾选一个难度!");
            }
            switchProxy.setChecked(false);
            return;
        }
        Context context = this;
        if (LocalVpnService.IsRunning != isChecked) {
            switchProxy.setEnabled(false);
            if (isChecked) {
                NotificationUtil.getINSTANCE().setContext(this).startNotification();
                checkProberAccount(result -> {
                    this.runOnUiThread(() -> {
                        if ((Boolean) result) {
//                            getAuthLink(link -> {
                            if (DataContext.CopyUrl) {
                                String link = DataContext.WebHost;
                                // Use local auth server if web host is not set
                                if (link.length() == 0) {
                                    link = "http://127.0.0.2:" + HttpServer.Port + "/" + getRandomString(10);
                                }
                                String finalLink = link;
                                this.runOnUiThread(() -> copyText(context, finalLink));
                            }

                            // Start vpn service
                            Intent intent = LocalVpnService.prepare(context);
                            if (intent == null) {
                                startVPNService();
                                // Jump to wechat app
                                if (DataContext.AutoLaunch) {
                                    getWechatApi();
                                }
                            } else {
                                startActivityForResult(intent, START_VPN_SERVICE_REQUEST_CODE);
                            }
                            // Start http service
                            startHttpService();
//                            });
                        } else {
                            switchProxy.setChecked(false);
                            switchProxy.setEnabled(true);
                        }
                    });
                });
            } else {
                LocalVpnService.IsRunning = false;
                stopHttpService();
            }
        }
    }

    private void startHttpService() {
        startService(new Intent(this, HttpServerService.class));
    }

    private void stopHttpService() {
        stopService(new Intent(this, HttpServerService.class));
    }

    private void startVPNService() {
        textViewLog.setText("");
        GL_HISTORY_LOGS = null;
        onLogReceived("starting...");
        startService(new Intent(this, LocalVpnService.class));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == START_VPN_SERVICE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                startVPNService();
                // Jump to wechat app
                getWechatApi();
            } else {
                switchProxy.setChecked(false);
                switchProxy.setEnabled(true);
                onLogReceived("canceled.");
            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_actions, menu);

        MenuItem menuItem = menu.findItem(R.id.menu_item_switch);
        if (menuItem == null) {
            return false;
        }

        switchProxy = (SwitchCompat) menuItem.getActionView();
        if (switchProxy == null) {
            return false;
        }

        switchProxy.setChecked(LocalVpnService.IsRunning);
        switchProxy.setOnCheckedChangeListener(this);

        if (!switchProxy.isChecked()) {
            inputAddress();
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_item_check_version) {// 检查最新版本
            getLatestVersion(result -> {
                Context context = this;
                String latest = (String) result;
                String current = getVersionName().trim();
                if (latest != null) {
                    if (latest.equals(current)) {
                        this.runOnUiThread(() -> {
                            new AlertDialog.Builder(context)
                                    .setTitle(getString(R.string.app_name) + " " + current)
                                    .setMessage("已经是最新版本~")
                                    .setPositiveButton(R.string.btn_ok, null)
                                    .show();
                        });
                    } else {
                        this.runOnUiThread(() -> {
                            new AlertDialog.Builder(context)
                                    .setTitle(getString(R.string.app_name) + " " + current)
                                    .setMessage("当前版本：" + current + "\n" + "最新版本：" + latest + "\n" + "是否前往网站下载最新版？")
                                    .setPositiveButton("更新", (dialog, which) -> openWebLink("https://maimaidx-prober-updater-android.bakapiano.com/"))
                                    .setNegativeButton("取消", null)
                                    .show();
                        });
                    }
                } else {
                    this.runOnUiThread(() -> {
                        new AlertDialog.Builder(context)
                                .setTitle(getString(R.string.app_name) + " " + current)
                                .setMessage("获取最新版本号时出现错误！")
                                .setPositiveButton("OK", null)
                                .show();
                    });
                }
            });
            return true;
        } else if (itemId == R.id.menu_item_about) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.app_name) + " " + getVersionName())
                    .setMessage(R.string.about_info)
                    .setPositiveButton(R.string.btn_ok, null)
                    .show();
            return true;
        } else if (itemId == R.id.menu_item_proxy) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("代理设置");

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);

            // 创建3个EditText输入框，并将其添加到LinearLayout中
            TextView textView1 = new TextView(this);
            textView1.setText("登录链接获取地址：");
            EditText editText1 = new EditText(this);
            editText1.setText(DataContext.WebHost);
                /*
                    TextView textView2 = new TextView(this);
                    textView2.setText("代理地址：");
                    EditText editText2 = new EditText(this);
                    editText2.setText(DataContext.ProxyHost);
                    TextView textView3 = new TextView(this);
                    textView3.setText("代理端口：");
                    EditText editText3 = new EditText(this);
                    editText3.setText(DataContext.ProxyPort+"");
                 */
            layout.addView(textView1);
            layout.addView(editText1);
                /*
                    layout.addView(textView2);
                    layout.addView(editText2);
                    layout.addView(textView3);
                    layout.addView(editText3);
                */
            builder.setView(layout);

            builder.setPositiveButton("确定", (dialog, which) -> {
                // 点击确定按钮的处理逻辑
                String input1 = editText1.getText().toString();
                // String input2 = editText2.getText().toString();
                // int input3 = parseInt(editText3.getText().toString());

                DataContext.WebHost = input1;
                // DataContext.ProxyHost = input2;
                // DataContext.ProxyPort = input3;

                mContextSp.edit()
                        .putString("webHost", input1)
                        // .putString("proxyHost",input2)
                        // .putInt("proxyPort",input3)
                        .apply();

                dialog.dismiss();
            });

            builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

            builder.setNeutralButton("恢复默认", (dialog, which) -> {
                String input1 = "https://maimai.bakapiano.com/shortcut?username=bakapiano666&password=114514";
                String input2 = "proxy.bakapiano.com";
                int input3 = 2569;

                DataContext.WebHost = input1;
                DataContext.ProxyHost = input2;
                DataContext.ProxyPort = input3;

                mContextSp.edit()
                        .putString("webHost", input1)
                        .putString("proxyHost", input2)
                        .putInt("proxyPort", input3)
                        .apply();

                dialog.dismiss();
            });

            AlertDialog dialog = builder.create();
            dialog.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        LocalVpnService.removeOnStatusChangedListener(this);
        super.onDestroy();
    }

    public void saveText(View view) {
        Context context = this;
        checkProberAccount(result -> {
            if ((Boolean) result) {
                saveContextData();
                this.runOnUiThread(() -> {
                    new AlertDialog.Builder(context)
                            .setTitle(getString(R.string.app_name) + " " + getVersionName())
                            .setMessage("查分器账户保存成功")
                            .setPositiveButton(R.string.btn_ok, null)
                            .show();
                });
            }
        });
    }

    private void showInvalidAccountDialog() {
        this.runOnUiThread(() -> {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.app_name) + " " + getVersionName())
                    .setMessage("查分账户信息无效")
                    .setPositiveButton(R.string.btn_ok, null)
                    .show();
        });
    }

    private void openWebLink(String url) {
        Intent intent = new Intent();
        intent.setData(Uri.parse(url));
        intent.setAction(Intent.ACTION_VIEW);
        this.startActivity(intent);
    }

    private void getAuthLink(Callback callback) {
        new Thread() {
            public void run() {
                String link = CrawlerCaller.getWechatAuthUrl();
                callback.onResponse(link);
            }
        }.start();
    }

    private void getLatestVersion(Callback callback) {
        CrawlerCaller.getLatestVersion(result -> {
            String version = (String)result;
            callback.onResponse(version);
        });
    }

    private void checkProberAccount(Callback callback) {
        DataContext.Username = ((TextView) findViewById(R.id.username)).getText().toString();
        DataContext.Password = ((TextView) findViewById(R.id.password)).getText().toString();

        saveOptions();

        saveDifficulties();

        if (DataContext.Username == null || DataContext.Password == null) {
            showInvalidAccountDialog();
            callback.onResponse(false);
            return;
        }
        CrawlerCaller.verifyAccount(DataContext.Username, DataContext.Password, result -> {
            if (!(Boolean) result) showInvalidAccountDialog();
            callback.onResponse(result);
        });
    }

    private void saveDifficulties() {
        DataContext.BasicEnabled = ((CheckBox) findViewById(R.id.basic)).isChecked();
        DataContext.AdvancedEnabled = ((CheckBox) findViewById(R.id.advanced)).isChecked();
        DataContext.ExpertEnabled = ((CheckBox) findViewById(R.id.expert)).isChecked();
        DataContext.MasterEnabled = ((CheckBox) findViewById(R.id.master)).isChecked();
        DataContext.RemasterEnabled = ((CheckBox) findViewById(R.id.remaster)).isChecked();
    }

    private void saveOptions() {
        DataContext.CopyUrl = ((Switch) findViewById(R.id.copyUrl)).isChecked();
        DataContext.AutoLaunch = ((Switch) findViewById(R.id.autoLaunch)).isChecked();
    }


    private void loadContextData() {
        String username = mContextSp.getString("username", null);
        String password = mContextSp.getString("password", null);

        boolean copyUrl = mContextSp.getBoolean("copyUrl", true);
        boolean autoLaunch = mContextSp.getBoolean("autoLaunch", true);

        boolean basicEnabled = mContextSp.getBoolean("basicEnabled", false);
        boolean advancedEnabled = mContextSp.getBoolean("advancedEnabled", false);
        boolean expertEnabled = mContextSp.getBoolean("expertEnabled", true);
        boolean masterEnabled = mContextSp.getBoolean("masterEnabled", true);
        boolean remasterEnabled = mContextSp.getBoolean("remasterEnabled", true);

        String proxyHost = mContextSp.getString("porxyHost","proxy.bakapiano.com");
        String webHost = mContextSp.getString("webHost","");
        int proxyPort = mContextSp.getInt("porxyPort",2569);


        ((TextView) findViewById(R.id.username)).setText(username);
        ((TextView) findViewById(R.id.password)).setText(password);

        ((Switch) findViewById(R.id.copyUrl)).setChecked(copyUrl);
        ((Switch) findViewById(R.id.autoLaunch)).setChecked(autoLaunch);

        ((CheckBox) findViewById(R.id.basic)).setChecked(basicEnabled);
        ((CheckBox) findViewById(R.id.advanced)).setChecked(advancedEnabled);
        ((CheckBox) findViewById(R.id.expert)).setChecked(expertEnabled);
        ((CheckBox) findViewById(R.id.master)).setChecked(masterEnabled);
        ((CheckBox) findViewById(R.id.remaster)).setChecked(remasterEnabled);


        DataContext.Username = username;
        DataContext.Password = password;

        DataContext.CopyUrl = copyUrl;
        DataContext.AutoLaunch = autoLaunch;

        DataContext.BasicEnabled = basicEnabled;
        DataContext.AdvancedEnabled = advancedEnabled;
        DataContext.ExpertEnabled = expertEnabled;
        DataContext.MasterEnabled = masterEnabled;
        DataContext.RemasterEnabled = remasterEnabled;

        DataContext.ProxyPort = proxyPort;
        DataContext.ProxyHost = proxyHost;
        DataContext.WebHost = webHost;
    }

    private void saveContextData() {
        SharedPreferences.Editor editor = mContextSp.edit();
        saveAccountContextData(editor);
        saveOptionsContextData(editor);
        saveDifficultiesContextData(editor);
        editor.apply();
    }

    private static void saveDifficultiesContextData(SharedPreferences.Editor editor) {
        editor.putBoolean("basicEnabled", DataContext.BasicEnabled);
        editor.putBoolean("advancedEnabled", DataContext.AdvancedEnabled);
        editor.putBoolean("expertEnabled", DataContext.ExpertEnabled);
        editor.putBoolean("masterEnabled", DataContext.MasterEnabled);
        editor.putBoolean("remasterEnabled", DataContext.RemasterEnabled);
    }

    private static void saveOptionsContextData(SharedPreferences.Editor editor) {
        editor.putBoolean("copyUrl", DataContext.CopyUrl);
        editor.putBoolean("autoLaunch", DataContext.AutoLaunch);
    }

    private static void saveAccountContextData(SharedPreferences.Editor editor) {
        editor.putString("username", DataContext.Username);
        editor.putString("password", DataContext.Password);
    }

    private void getWechatApi() {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            ComponentName cmp = new ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI");
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setComponent(cmp);
            startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
        }
    }

    public static String getRandomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(62);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }
}
