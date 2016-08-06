package com.iot.switzer.iotdormkitkat.communication;

import android.util.Log;

/**
 * Created by Lucas on 7/31/2016.
 */

public class DoTPacketParser implements DoTParser {

    public DoTPacket parse(byte[] data) {
        DoTPacket packet = null;
        switch (data[0] / 16) {
            case DoTPacket.HEARTBEAT_HEADER:
                break;
            case DoTPacket.SUBSCRIPTION_HEADER:
                switch (data[0]) {
                    case DoTPacket.SUBSCRIPTION_UPDATE:
                        packet = new DoTSubscriptionUpdateParser().parse(data);
                        break;
                }
                break;
            case DoTPacket.HANDSHAKE_HEADER:
                switch (data[0])
                {
                    case DoTPacket.HANDSHAKE_RETURN:
                        packet = new DoTHandshakeParser().parse(data);
                        break;
                }
        }
        return packet;
    }
}
