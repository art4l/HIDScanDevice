package be.art4l.scandevice;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.util.Log;

import java.util.concurrent.BlockingQueue;

import be.art4l.scandevice.USBResult;

/**
 * Created by Dirk on 25/01/18.
 */

public class USBDataReader implements Runnable {


    protected BlockingQueue<USBResult> readQueue = null;
    protected UsbDeviceConnection readConnection = null;
    protected int packetSize;
    protected byte[] bytes;
    protected UsbEndpoint readEp;


    public USBDataReader(){

    }

    public BlockingQueue<USBResult> getReadQueue() {
        return readQueue;
    }

    public void setReadQueue(BlockingQueue<USBResult> readQueue) {
        this.readQueue = readQueue;
    }

    public UsbDeviceConnection getReadConnection() {
        return readConnection;
    }

    public void setReadConnection(UsbDeviceConnection readConnection) {
        this.readConnection = readConnection;
    }

    public int getPacketSize() {
        return packetSize;
    }

    public void setPacketSize(int packetSize) {
        this.packetSize = packetSize;
        bytes = new byte[packetSize];
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public UsbEndpoint getReadEp() {
        return readEp;
    }

    public void setReadEp(UsbEndpoint readEp) {
        this.readEp = readEp;
    }

    @Override
    public void run() {

    }

    protected  String composeReturnString(byte[] bytes) {
        return new String(bytes);
    }

    protected void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
