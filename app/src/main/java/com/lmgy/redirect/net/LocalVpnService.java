package com.lmgy.redirect.net;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.VpnService;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.lmgy.redirect.R;
import com.lmgy.redirect.bean.HostData;
import com.lmgy.redirect.receiver.NetworkReceiver;
import com.lmgy.redirect.utils.DnsUtils;
import com.lmgy.redirect.utils.SPUtils;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.Selector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * Created by lmgy on 15/8/2019
 */
public class LocalVpnService extends VpnService {
    private static final String TAG = "LocalVpnService";
    private static final String VPN_ADDRESS = "10.1.10.1";//本地代理服务器IP地址，必要，建议用A类IP地址，防止冲突
    private static final String VPN_ADDRESS6 = "fe80:49b1:7e4f:def2:e91f:95bf:fbb6:1111";
//    private static final String VPN_ROUTE = "0.0.0.0"; // Intercept everything
//    private static final String VPN_ROUTE6 = "::"; // Intercept everything
    private static final int VPN_ADDRESS_MASK = 32;
    private static final int VPN_ADDRESS6_MASK = 128;
    private static final int VPN_DNS4_MASK = 32;
    private static final int VPN_DNS6_MASK = 128;
    private static final int VPN_ROUTE_MASK = 0;
    private static final int VPN_MTU = 4096;
    private static String VPN_DNS4 = "8.8.8.8";
    private static String VPN_DNS6 = "2001:4860:4860::8888";

    public static final String BROADCAST_VPN_STATE = LocalVpnService.class.getName() + ".VPN_STATE";
    public static final String ACTION_CONNECT = LocalVpnService.class.getName() + ".START";
    public static final String ACTION_DISCONNECT = LocalVpnService.class.getName() + ".STOP";

    private static boolean isRunning = false;
    private static Thread threadHandleHosts = null;
    private ParcelFileDescriptor vpnInterface = null;

    private PendingIntent pendingIntent;

    private ConcurrentLinkedQueue<Packet> deviceToNetworkUDPQueue;
    private ConcurrentLinkedQueue<Packet> deviceToNetworkTCPQueue;
    private ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue;
    private ExecutorService executorService;

    private Selector udpSelector;
    private Selector tcpSelector;
    private NetworkReceiver netStateReceiver;
    private static boolean isOAndBoot = false;


    @Override
    public void onCreate() {
        registerNetReceiver();
        super.onCreate();
        if (isOAndBoot) {
            //android 8.0 boot
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("vhosts_channel_id", "System", NotificationManager.IMPORTANCE_NONE);
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                manager.createNotificationChannel(channel);
                Notification notification = new Notification.Builder(this, "vhosts_channel_id")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Virtual Hosts Running")
                        .build();
                startForeground(1, notification);
            }
            isOAndBoot=false;
        }
        setupHostFile();
        setupVPN();
        if (vpnInterface == null) {
            Log.d(TAG, "unknow error");
            stopVService();
            return;
        }
        isRunning = true;
        try {
            udpSelector = Selector.open();
            tcpSelector = Selector.open();
            deviceToNetworkUDPQueue = new ConcurrentLinkedQueue<>();
            deviceToNetworkTCPQueue = new ConcurrentLinkedQueue<>();
            networkToDeviceQueue = new ConcurrentLinkedQueue<>();
            executorService = Executors.newFixedThreadPool(5);
            executorService.submit(new UDPInput(networkToDeviceQueue, udpSelector));
            executorService.submit(new UDPOutput(deviceToNetworkUDPQueue, networkToDeviceQueue, udpSelector, this));
            executorService.submit(new TCPInput(networkToDeviceQueue, tcpSelector));
            executorService.submit(new TCPOutput(deviceToNetworkTCPQueue, networkToDeviceQueue, tcpSelector, this));
            executorService.submit(new VPNRunnable(vpnInterface.getFileDescriptor(),
                    deviceToNetworkUDPQueue, deviceToNetworkTCPQueue, networkToDeviceQueue));
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_VPN_STATE).putExtra("running", true));
            Log.i(TAG, "Started");
        } catch (Exception e) {
            // TODO: Here and elsewhere, we should explicitly notify the user of any errors
            // and suggest that they stop the service, since we can't do it ourselves
            Log.e(TAG, "Error starting service", e);
            stopVService();
        }
    }


    private void setupHostFile() {
        try {
            new Thread(){
                @Override
                public void run() {
                    DnsUtils.handle_hosts(SPUtils.getDataList(getApplicationContext(), "hostList", HostData.class));
                }
            }.start();
        } catch (Exception e) {
            Log.e(TAG, "error setup host file service", e);
        }
    }

    private void setupVPN() {
        if (vpnInterface == null) {
            Builder builder = new Builder();
            builder.addAddress(VPN_ADDRESS, VPN_ADDRESS_MASK);
            builder.addAddress(VPN_ADDRESS6, VPN_ADDRESS6_MASK);
            builder.addRoute(VPN_DNS4, VPN_DNS4_MASK);
            builder.addRoute(VPN_DNS6, VPN_DNS6_MASK);
//            builder.addRoute(VPN_ROUTE,VPN_ROUTE_MASK);
//            builder.addRoute(VPN_ROUTE6,VPN_ROUTE_MASK);
            builder.setMtu(VPN_MTU);
            builder.addDnsServer(VPN_DNS4);
            builder.addDnsServer(VPN_DNS6);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                String[] whiteList = {"com.android.vending", "com.google.android.apps.docs", "com.google.android.apps.photos", "com.google.android.gm", "com.google.android.apps.translate"};
                for (String white : whiteList) {
                    try {
                        builder.addDisallowedApplication(white);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
            vpnInterface = builder.setSession(getString(R.string.app_name)).setConfigureIntent(pendingIntent).establish();
        }
    }

    private void registerNetReceiver() {
        //wifi 4G state
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        netStateReceiver = new NetworkReceiver();
        registerReceiver(netStateReceiver, filter);

    }

    private void unregisterNetReceiver() {
        if (netStateReceiver != null) {
            unregisterReceiver(netStateReceiver);
            netStateReceiver = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (ACTION_DISCONNECT.equals(intent.getAction())) {
                stopVService();
                return START_NOT_STICKY;
            }
        }
        return START_STICKY;
    }

    public static boolean isRunning() {
        return isRunning;
    }

    private void stopVService() {
        if (threadHandleHosts != null) threadHandleHosts.interrupt();
        unregisterNetReceiver();
        if (executorService != null) executorService.shutdownNow();
        isRunning = false;
        cleanup();
        stopSelf();
        Log.d(TAG, "Stopping");
    }

    @Override
    public void onRevoke() {
        stopVService();
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        stopVService();
        super.onDestroy();
    }

    private void cleanup() {
        deviceToNetworkTCPQueue = null;
        deviceToNetworkUDPQueue = null;
        networkToDeviceQueue = null;
        ByteBufferPool.clear();
        closeResources(udpSelector, tcpSelector, vpnInterface);
    }

    private static void closeResources(Closeable... resources) {
        for (Closeable resource : resources) {
            try {
                resource.close();
            } catch (Exception e) {
                Log.e(TAG, e.toString(), e);
            }
        }
    }

    private static class VPNRunnable implements Runnable {
        private static final String TAG = VPNRunnable.class.getSimpleName();

        private FileDescriptor vpnFileDescriptor;

        private ConcurrentLinkedQueue<Packet> deviceToNetworkUDPQueue;
        private ConcurrentLinkedQueue<Packet> deviceToNetworkTCPQueue;
        private ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue;

        public VPNRunnable(FileDescriptor vpnFileDescriptor,
                           ConcurrentLinkedQueue<Packet> deviceToNetworkUDPQueue,
                           ConcurrentLinkedQueue<Packet> deviceToNetworkTCPQueue,
                           ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue) {
            this.vpnFileDescriptor = vpnFileDescriptor;
            this.deviceToNetworkUDPQueue = deviceToNetworkUDPQueue;
            this.deviceToNetworkTCPQueue = deviceToNetworkTCPQueue;
            this.networkToDeviceQueue = networkToDeviceQueue;
        }

        @Override
        public void run() {
            Log.i(TAG, "Started");

            FileChannel vpnInput = new FileInputStream(vpnFileDescriptor).getChannel();
            FileChannel vpnOutput = new FileOutputStream(vpnFileDescriptor).getChannel();
            try {
                ByteBuffer bufferToNetwork = null;
                boolean dataSent = true;
                boolean dataReceived;
                while (!Thread.interrupted()) {
                    if (dataSent)
                        bufferToNetwork = ByteBufferPool.acquire();
                    else
                        bufferToNetwork.clear();

                    // TODO: Block when not connected
                    int readBytes = vpnInput.read(bufferToNetwork);
                    if (readBytes > 0) {
                        dataSent = true;
                        bufferToNetwork.flip();
                        Packet packet = new Packet(bufferToNetwork);
                        if (packet.isUDP()) {
                            deviceToNetworkUDPQueue.offer(packet);
                        } else if (packet.isTCP()) {
                            deviceToNetworkTCPQueue.offer(packet);
                        } else {
                            Log.w(TAG, "Unknown packet type");
                            dataSent = false;
                        }
                    } else {
                        dataSent = false;
                    }
                    ByteBuffer bufferFromNetwork = networkToDeviceQueue.poll();
                    if (bufferFromNetwork != null) {
                        bufferFromNetwork.flip();
                        while (bufferFromNetwork.hasRemaining())
                            try {
                                vpnOutput.write(bufferFromNetwork);
                            } catch (Exception e) {
                                Log.e(TAG, e.toString(), e);
                                break;
                            }
                        dataReceived = true;
                        ByteBufferPool.release(bufferFromNetwork);
                    } else {
                        dataReceived = false;
                    }

                    // TODO: Sleep-looping is not very battery-friendly, consider blocking instead
                    // Confirm if throughput with ConcurrentQueue is really higher compared to BlockingQueue
                    if (!dataSent && !dataReceived)
                        Thread.sleep(11);
                }
            } catch (InterruptedException e) {
                Log.i(TAG, "Stopping");
            } catch (IOException e) {
                Log.w(TAG, e.toString(), e);
            } finally {
                closeResources(vpnInput, vpnOutput);
            }
        }
    }
}
