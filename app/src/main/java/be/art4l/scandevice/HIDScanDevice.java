package be.art4l.scandevice;

/**
 * Created by Dirk on 1/12/17.
 */

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;

/**
 * This class is used for talking to a Scanner in HID Mode, connecting, disconnecting and enumerating the device
 * It is used to receive data from teh Honeywell 1980i USB Scanner
 * @author Dirk Matheussen
 */
public class HIDScanDevice {
    private Context _context;
    private int _productId;
    private int _vendorId;



    // Locker object that is responsible for locking read/write thread. Not used at this moment
    private final Object _locker = new Object();
    private Thread _readingThread = null;
    private boolean _runReadingThread = false;

    private UsbManager _usbManager;
    private UsbDevice _usbDevice;

    // return event
    private OnMessageReceived _messageListener;


    /**
     * Creates a hid bridge to the Scanner. Should be created once.
     * @param context is the UI context of Android.
     * @param productId of the device.
     * @param vendorId of the device.
     */
    public HIDScanDevice(Context context, int productId, int vendorId) {
        _context = context;
        _productId = productId;
        _vendorId = vendorId;
    }


    public void setMessageListener(OnMessageReceived messageListener){
        _messageListener = messageListener;

    }

    /**
     * Searches for the device and opens it if successful
     * @return true, if connection was successful
     */
    public boolean openDevice() {
        _usbManager = (UsbManager) _context.getSystemService(Context.USB_SERVICE);

        HashMap<String, UsbDevice> deviceList = _usbManager.getDeviceList();

        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        _usbDevice = null;

        // Iterate all the available devices and find ours.
        while(deviceIterator.hasNext()){
            UsbDevice device = deviceIterator.next();
            if (device.getProductId() == _productId && device.getVendorId() == _vendorId) {
                _usbDevice = device;
            }
        }

        if (_usbDevice == null) {
            Log("Cannot find the device. Did you forgot to plug it?");
            Log(String.format("\t I search for VendorId: %s and ProductId: %s", _vendorId, _productId));
            return false;
        }

        Log("Found the device");
        return true;
    }

    /**
     * Closes the reading thread of the device.
     */
    public void closeDevice() {
        try
        {
            stopReadingThread();
        }
        catch(RuntimeException e)
        {
            Log("Error happend while closing device. Usb reciever not connected.");
        }
    }

    /**
     * Starts the thread that continuously reads the data from the device.
     * Should be called in order to be able to talk with the device.
     */
    public void startReadingThread() {
        if (_readingThread == null) {
            _runReadingThread = true;
            _readingThread = new Thread(readerReceiver);
            _readingThread.start();
        } else {
            Log("Reading thread already started");
        }
    }

    /**
     * Stops the thread that continuously reads the data from the device.
     * If it is stopped - talking to the device would be impossible.
     */
    public void stopReadingThread() {
        if (_readingThread != null) {
            // Just kill the thread. It is better to do that fast if we need that asap.
            _runReadingThread = false;
            _readingThread = null;
        } else {
            Log("No reading thread to stop");
        }
    }

    /**
     * Write data to the usb hid. Data is written as-is, so calling method is responsible for adding header data.
     * @param bytes is the data to be written.
     * @return true if succeed.
     */
    public boolean writeData(byte[] bytes) {
        try
        {
            UsbInterface writeIntf = _usbDevice.getInterface(0);
            UsbEndpoint writeEp = writeIntf.getEndpoint(1);
            UsbDeviceConnection writeConnection = _usbManager.openDevice(_usbDevice);

            // Lock the usb interface.
            writeConnection.claimInterface(writeIntf, true);

            // Write the data as a bulk transfer with defined data length.
            int r = writeConnection.bulkTransfer(writeEp, bytes, bytes.length, 0);
            if (r != -1) {
                Log(String.format("Written %s bytes to the dongle. Data written: %s", r, bytes));
            } else {
                Log("Error happened while writing data. No ACK");
            }

            // Release the usb interface.
            writeConnection.releaseInterface(writeIntf);
            writeConnection.close();
        } catch(NullPointerException e)
        {
            Log("Error happend while writing. Could not connect to the device or interface is busy?");
            Log.e("HidBridge", Log.getStackTraceString(e));
            return false;
        }
        return true;
    }


    // The thread that continuously receives data from the scanner and and send back.
    private Runnable readerReceiver = new Runnable() {
        public void run() {
            if (_usbDevice == null) {
                Log("No device to read from");
                return;
            }

            UsbEndpoint readEp;
            UsbDeviceConnection readConnection = null;
            UsbInterface readIntf = null;
            boolean readerStartedMsgWasShown = false;



            readIntf = _usbDevice.getInterface(0);
            readEp = readIntf.getEndpoint(0);
            try
            {

                readConnection = _usbManager.openDevice(_usbDevice);

                if (readConnection == null) {
                    Log("Cannot start reader because the user didn't gave me permissions or the device is not present. Retrying in 2 sec...");
                }

                // Claim and lock the interface in the android system.
                readConnection.claimInterface(readIntf, true);
            }
            catch (SecurityException e) {
                Log("Cannot start reader because the user didn't gave me permissions. Retrying in 2 sec...");
            }

            // Show the reader started message once.
            if (!readerStartedMsgWasShown) {
                Log("!!! Reader was started !!!");
                readerStartedMsgWasShown = true;
            }

            String scannedValue = "";

            // We will continuously ask for the data from the device and store it in the queue.
            //TODO Rewrite logic with blocking read
            while (_runReadingThread) {
                try
                    {

                        // Read the data as a bulk transfer with the size = MaxPacketSize
                        int packetSize = readEp.getMaxPacketSize();
                        byte[] bytes = new byte[packetSize];
                        int r = readConnection.bulkTransfer(readEp, bytes, packetSize, 200);

                        //Logic for the Honeywell 1980i USB Scanner
                        //TODO Make generic/ Multiscanner aware, depending on Vendor/Product Id

                        if (r > 0){
                            while (r >= 0 && bytes[0] == 2) {

                                if (r > 0 )scannedValue = scannedValue + composeReturnString(bytes);
                                r = readConnection.bulkTransfer(readEp, bytes, packetSize, 200);
                            }
                        }

                        if (scannedValue.length() > 0) _messageListener.messageReceived(_usbDevice.getDeviceName(), scannedValue);
                        scannedValue = "";


                    }

                    catch (NullPointerException e) {
                        Log("Error happened while reading. No device or the connection is busy");
                        Log.e("HidBridge", Log.getStackTraceString(e));
                    }
                    catch (ThreadDeath e) {
                        if (readConnection != null) {
                            readConnection.releaseInterface(readIntf);
                            readConnection.close();
                        }

                        throw e;
                    }


                // Sleep for 10 ms to pause and restart a new cycle
                // TODO No Synchronisation between read and write is done at this moment
                sleep(10);
            }
            // Release the interface lock.
            readConnection.releaseInterface(readIntf);
            readConnection.close();

        }
    };

    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }



    /**
     * Logs the message from HidBridge.
     * @param message to log.
     */
    private void Log(String message) {
        //LogHandler logHandler = LogHandler.getInstance();
        //logHandler.WriteMessage("HidBridge: " + message, LogHandler.GetNormalColor());
        Log.d("HIDScanDevice: ", message);
    }

    /**
     * Create a return byte array with only valid ASCII value and the correct length
     */
    private static String composeReturnString(byte[] bytes) {

        int size = bytes[1];

        if (size == 0) return null;

        byte[] newBuffer = new byte[size];
        int count = 0;
        for (int i=5;i<bytes.length;i++){
            if (count < size) {
                newBuffer[count] = bytes[i];
                count++;
            }
        }

        return new String(newBuffer);
    }



    /**
     *
     * Declare the interface to receive data
     *
     *
     */

    public interface OnMessageReceived {
        public void messageReceived(String type, String message);
    }


}