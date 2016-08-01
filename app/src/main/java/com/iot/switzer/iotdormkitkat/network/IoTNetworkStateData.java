package com.iot.switzer.iotdormkitkat.network;

public class IoTNetworkStateData {

    public int state;
    public int progress;
    public String message;
    public IoTNetworkStateData(int state, int progress, String message) {
        this.state = state;
        this.progress = progress;
        this.message = message;
    }
}
