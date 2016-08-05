package com.iot.switzer.iotdormkitkat.communication;

import java.util.ArrayList;

/**
 * Created by Lucas on 7/31/2016.
 */
public abstract class DoTPacket {
    public static final byte HEARTBEAT_HEADER = 0;
    public static final byte HEARTBEAT_REQUEST = 0x01;
    public static final byte HEARTBEAT_RETURN = 0x02;

    public static final byte HANDSHAKE_HEADER = 1;
    public static final byte HANDSHAKE_REQUEST = 0x11;
    public static final byte HANDSHAKE_RETURN = 0x12;

    public static final byte SUBSCRIPTION_HEADER = 3;
    public static final byte SUBSCRIPTION_UPDATE = 0x31;

    public static final byte UNI_DELIM = ',';
    public static final byte PACKET_DELIM = (char) 13;
    ArrayList<Byte> packet;


    abstract protected void _build();
    public DoTPacket() {
        packet = new ArrayList<>();
    }

    public byte[] asBytes() {
        byte[] out = new byte[packet.size()];

        for (int i = 0; i < packet.size(); i++) {
            out[i] = packet.get(i);
        }

        return out;
    }

    public void appendEelement(byte[] data) {
        appendBytes(data);
    }

    public void build() {


        close();
    }

    public final byte getHeader() {
        return packet.get(0);
    }

    public void setHeader(byte header) {
        packet.add(0, header);
    }

    protected void appendBytes(byte[] data) {
        for (byte b : data) {
            packet.add(b);
        }
    }

    protected void appendByte(int i, byte b) {
        packet.add(i, b);
    }

    protected void delim() {
        packet.add(UNI_DELIM);
    }

    protected void close() {
        packet.add(PACKET_DELIM);
    }

}
