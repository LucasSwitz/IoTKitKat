package com.iot.switzer.iotdormkitkat.services;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.iot.switzer.iotdormkitkat.communication.DoTHandshakePacket;
import com.iot.switzer.iotdormkitkat.communication.DoTPacket;
import com.iot.switzer.iotdormkitkat.communication.DoTPacketParser;
import com.iot.switzer.iotdormkitkat.devices.IoTBluetoothDeviceController;
import com.iot.switzer.iotdormkitkat.devices.IoTDeviceController;
import com.iot.switzer.iotdormkitkat.network.IoTManager;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

interface HandshakeListener {
    void onHandshakeData(byte[] data);
}

/**
 * Created by Administrator on 6/20/2016.
 */
public class DeviceDiscoveryService extends Service implements Runnable,HandshakeServiceListener {
    public static final String PARAM_IN_MSG = "com.iot.switzer.dormiot.param_n_msg";
    public static final int discoveryPeriod = 30;
    private static final int HANDSHAKE_TIMEOUT = 5;
    private boolean finding = false;
    private long currentRunTime = 0;
    private Set<BluetoothDevice> currentlyHandshakingDevices;

    private BluetoothAdapter bthAdapter;

    public DeviceDiscoveryService() {
        Log.d("BLUETOOTH", "Constructed Service!");
        bthAdapter = BluetoothAdapter.getDefaultAdapter();
        currentlyHandshakingDevices = new HashSet<>();

        if (bthAdapter != null)
            Log.d("BLUETOOTH", "Adapter is alive");
        else
            Log.d("BLUETOOTH", "Adapter is nonexistent");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("DISCOVERY", "On Create!");
    }


    @Override
    public void onDestroy() {
        Log.d("DISCOVERY", "On Destroy!");
        stop();
        super.onDestroy();
    }

    private void stop() {
        finding = false;
    }

    @Override
    public void run() {
        Log.d("DISCOVERY", "Starting find!");
        if (!bthAdapter.isEnabled()) {
            bthAdapter.enable();
        }

        if (bthAdapter.isEnabled()) {
            Log.d("DISCOVERY", "Adapter is enabled!");
        }
        find();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("DISCOVERY", "On Start Command.");
        new Thread(this).start();
        return START_STICKY;
    }

    public void find() {
        long start = System.currentTimeMillis();
        Log.d("DISCOVERY", "Started find for: " + String.valueOf(discoveryPeriod) + " seconds.");
        finding = true;
        while (currentRunTime < discoveryPeriod) {
            currentRunTime = ((System.currentTimeMillis() - start) / 1000);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            attemptConnectionWithAvailableDevices();
        }
        this.stopSelf();
    }

    private void addToCurrentlyHandshakingDevices(BluetoothDevice d) {
        currentlyHandshakingDevices.add(d);
    }

    /**
     * This can be concurrently accessed
     */
    private void removeFromCurrentlyHandshakingDevices(BluetoothDevice deviceToRemove) {

        Iterator<BluetoothDevice> iter = currentlyHandshakingDevices.iterator();

        while (iter.hasNext()){
            if (iter.next() == deviceToRemove)
                iter.remove();
        }
    }

    private void attemptConnectionWithAvailableDevices() {
        Set<BluetoothDevice> pairedDevices = bthAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice b : getUnHandshookDevices(pairedDevices)) {
                addToCurrentlyHandshakingDevices(b);
                HandshakeService s = new HandshakeService(b, HANDSHAKE_TIMEOUT);
                s.setListener(this);
                (new Thread(s)).start();
            }
        }
    }

    private boolean isDeviceAlreadyLive(BluetoothDevice device) {
        boolean match = false;
        for (IoTDeviceController controller : IoTManager.getInstance().getLiveDevices()) {
            match = controller.getDeviceDescription().identifer.equals(device.getAddress());

            if (match)
                break;
        }
        return match;
    }

    private boolean isDeviceCurrentlyHandshaking(BluetoothDevice device) {
        boolean match = false;
        for (BluetoothDevice d : currentlyHandshakingDevices) {
            match = (d.equals(device));

            if (match)
                break;
        }
        return match;
    }

    private Set<BluetoothDevice> getUnHandshookDevices(Set<BluetoothDevice> pairedDevices) {
        Set<BluetoothDevice> out = new HashSet<>();
        for (BluetoothDevice device : pairedDevices) {
            boolean match = isDeviceAlreadyLive(device) || isDeviceCurrentlyHandshaking(device);
            if (!match) {
                out.add(device);
            }
        }
        return out;
    }

    public void onHandshakeSuccess(DoTHandshakePacket packet, BluetoothDevice device, BluetoothSocket socket) {
        Log.d("SUCCESS","handsahke success");
        addDeviceFromHandshake(packet, device, socket);
    }

    @Override
    public void onHandshakeFailure(BluetoothDevice device) {
        removeFromCurrentlyHandshakingDevices(device);
    }

    public void addDeviceFromHandshake(DoTHandshakePacket packet, BluetoothDevice device, BluetoothSocket socket) {
        IoTDeviceController.DeviceDescription description = new IoTDeviceController.DeviceDescription(device.getAddress(), packet.getToken(),
                packet.getHeartbeatInterval(),
                packet.getSubscriptionDescriptions());
        IoTDeviceController controller = new IoTBluetoothDeviceController(description, device, socket);

        IoTManager.getInstance().addDevice(controller);
    }
}

interface HandshakeServiceListener
{

    void onHandshakeSuccess(DoTHandshakePacket packet, BluetoothDevice device, BluetoothSocket socket);
    void onHandshakeFailure(BluetoothDevice device);
}

class HandshakeService implements Runnable, HandshakeListener {
    int timeout;
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private HandshakeServiceReciever handshakeServiceReciever;
    private HandshakeServiceListener listener;

    public HandshakeService(BluetoothDevice device, int timeout) {
        this.timeout = timeout;
        this.device = device;
    }

    public void setListener(HandshakeServiceListener listener)
    {
        this.listener = listener;
    }

    @Override
    public void run() {
        attemptHandshake();
    }


    private boolean openConnection() {

        try {
            Log.d("DISCOVERY", "Attemping to Handshake: " + device.getName());
            socket = device.createRfcommSocketToServiceRecord(IoTBluetoothDeviceController.DEFAULT_UUID);
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

            try {
                if (!socket.isConnected())
                    Log.d("DISCOVERY", device.getName() + "Socket was not connected..connecting...");

                socket.connect();
                Log.d("DISCOVERY", device.getName() + " :Device successfully opened socket!");

            } catch (IOException e) {
                Log.d("DISCOVERY", device.getName() + ":Error Connecting!");

                return false;
            }
        } catch (IOException e1) {
            Log.d("DISCOVERY", "Unable to open Rfcomm socket");

            return false;
        }
        return true;
    }

    private void attemptHandshake() {

        if (openConnection()) {
            startHandshakeRecieverService();
            sendHandshakeRequest();
            waitForHandshake();
        }
        else
        {
            if(listener != null)
            {
                listener.onHandshakeFailure(device);
            }
        }
    }

    private void startHandshakeRecieverService() {
        InputStream is = null;
        try {
            is = socket.getInputStream();
            handshakeServiceReciever = new HandshakeServiceReciever(is);
            handshakeServiceReciever.addListener(this);
            handshakeServiceReciever.startServiceThread();

        } catch (IOException e) {
            Log.d("DISCOVERY", "Unable to start handshake service", e);
        }
    }

    private void sendHandshakeRequest() {
        OutputStream os = null;
        try {
            os = socket.getOutputStream();
            DoTHandshakePacket packet = new DoTHandshakePacket();
            packet.setHeader(DoTPacket.HANDSHAKE_REQUEST);
            packet.build();

            os.write(packet.asBytes());

            Log.d("DISCOVERY", device.getAddress() + ": Sent Handshake request");
        } catch (IOException e) {
            Log.d("DISCOVERY", device.getAddress() + ": Unabled to write handshake.", e);
        }
    }

    private void waitForHandshake() {
        double start = System.currentTimeMillis();
        double duration;

        while (!handshakeServiceReciever.success()) {
            duration = (System.currentTimeMillis() - start) / 1000;

            if (duration > timeout) {
                handshakeServiceReciever.interuptServiceThread();
                break;
            }
        }
        handleHandshakeResult();
    }

    private void closeHandshakeSocket() {
        try {
            socket.close();
        } catch (IOException e) {
            Log.d("DISCOVERY", device.getAddress() + " :Device hand shake failed with error!");
        }
    }

    private void endHandshakeRecieverService() {
        if (handshakeServiceReciever.isServiceAlive())
            handshakeServiceReciever.interuptServiceThread();
    }

    private void handleHandshakeResult() {
        if (!handshakeServiceReciever.success()) {
            Log.d("DISCOVERY", device.getAddress() + " :Device did not handshake in given time");

            closeHandshakeSocket();
        } else {
            Log.d("DISCOVERY", device.getAddress() + " :Device hand shook!");
        }
        endHandshakeRecieverService();
    }

    @Override
    public void onHandshakeData(byte[] data) {
        DoTPacketParser packetParser = new DoTPacketParser();
        DoTHandshakePacket packet = (DoTHandshakePacket) packetParser.parse(data);

        if(listener != null)
        {
            listener.onHandshakeSuccess(packet,device,socket);
        }
    }

    class HandshakeServiceReciever implements Runnable {
        boolean hasHeader = false;
        boolean hasTail = false;
        byte packet[];
        int packetSize = 0;
        private InputStream is;
        private HandshakeListener listener;
        private Thread serviceThread;

        HandshakeServiceReciever(InputStream is) {
            this.is = new BufferedInputStream(is);
            packet = new byte[1024];
        }

        void startServiceThread() {
            serviceThread = new Thread(this);
            serviceThread.start();
        }

        void interuptServiceThread() {
            serviceThread.interrupt();
        }

        boolean isServiceAlive() {
            return serviceThread.isAlive();
        }

        void addListener(HandshakeListener listener) {
            this.listener = listener;
        }

        void read() {
            byte[] in = new byte[1024];
            int numOfBytes = 0;
            try {
                numOfBytes = is.read(in);
                for (int i = 0; i < numOfBytes; i++) {
                    packet[packetSize] = in[i];
                    packetSize++;
                }

                if (in[0] == DoTPacket.HANDSHAKE_RETURN) {
                    hasHeader = true;
                }

                if (in[numOfBytes - 1] != DoTPacket.PACKET_DELIM) {
                    run();
                } else {
                    hasTail = true;
                    listener.onHandshakeData(packet);
                }

            } catch (IOException e) {
                Log.d("DISCOVERY", "Failure Reading From: " + device.getAddress());
            }
        }

        @Override
        public void run() {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            read();
        }

        public boolean success() {
            return hasHeader && hasTail;
        }
    }
}
