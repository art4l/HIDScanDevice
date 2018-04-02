package com.art4l.scandevice;

/**
 *
 * Created by Dirk on 1/12/17.
 */

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.example.androidthings.scandevice.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * This class is used for talking to a USB Device - Scanner in HID Mode, connecting, disconnecting and enumerating the device
 *
 * @author Dirk Matheussen
 */
public class HIDScanDevice {
    private Context _context;
    // Locker object that is responsible for locking read/write thread. Not used at this moment

    private VendorData _usbVendor;
    private final Object _locker = new Object();
    private Thread _readingThread = null;
    private boolean _runReadingThread = false;

    private UsbManager _usbManager;
    private UsbDevice _usbDevice;

    private ArrayList<VendorData> vendors = new ArrayList<VendorData>();

    // return event
    private OnMessageReceived _messageListener;


    /**
     * Creates a hid bridge to the Scanner. Should be created once.
     * @param context is the UI context of Android.
     */
    public HIDScanDevice(Context context) {
        _context = context;

        try {
            XmlResourceParser scanDevices = _context.getResources().getXml(R.xml.usbscandevices);
            scanDevices.next();
            int eventType = scanDevices.getEventType();
            String className = null;
            String vendorId = null;
            String productId = null;
            String deviceName = null;
            String usbClass = null;

            while (eventType != XmlPullParser.END_DOCUMENT)
            {

                if (eventType == XmlPullParser.START_TAG && scanDevices.getName().equals("plugin-name")){
                    eventType = scanDevices.next();
                    className = scanDevices.getText();
                }
                if (eventType == XmlPullParser.START_TAG && scanDevices.getName().equals("vendor-id")){
                    eventType = scanDevices.next();
                    vendorId = scanDevices.getText();
                }
                if (eventType == XmlPullParser.START_TAG && scanDevices.getName().equals("product-id")){
                    eventType = scanDevices.next();
                    productId = scanDevices.getText();
                }
                if (eventType == XmlPullParser.START_TAG && scanDevices.getName().equals("device-name")){
                    eventType = scanDevices.next();
                    deviceName = scanDevices.getText();
                }
                if (eventType == XmlPullParser.START_TAG && scanDevices.getName().equals("usb-class")){
                    eventType = scanDevices.next();
                    usbClass = scanDevices.getText();
                }



                if (eventType == XmlPullParser.END_TAG && scanDevices.getName().equals("usb-device")){
                    VendorData vendorData = new VendorData();
                    vendorData.setDeviceName(deviceName);
                    vendorData.setClassName(className);
                    vendorData.setProductId(Integer.valueOf(productId));
                    vendorData.setVendorId(Integer.valueOf(vendorId));
                    vendorData.setUsbClass(Integer.valueOf(usbClass));
                    vendors.add(vendorData);

                }
                eventType = scanDevices.next();


            }

        } catch (IOException | XmlPullParserException ex){

        }


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
            _usbVendor =findMatchingDevice(device);
            if ( _usbVendor !=null){
                _usbDevice = device;
                break;
            }
        }

        if (_usbDevice == null) {
            Log("Cannot find a valid device");
            return false;
        }

        Log("Found the device");
        return true;
    }

    /**
     * Closes the reading thread of the device.
     */
    public  boolean closeDevice(UsbDevice usbDevice) {

        if (_usbDevice == null) return false;
        if (usbDevice.getDeviceId() == _usbDevice.getDeviceId()) {
            try {
                stopReadingThread();
            } catch (RuntimeException e) {
                Log("Error happend while closing device. Usb reciever not connected.");
            }
            return true;
        }
        return false;

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
            if (isReadonly()){
                _messageListener.errorReceived(_usbDevice.getProductName(),"This device type does not support write actions");
                return false;
            }

            UsbDeviceConnection writeConnection = _usbManager.openDevice(_usbDevice);

            int interfaceNr = getInterfaceIndex(writeConnection,_usbVendor.getUsbClass());
            int endPointNr = getEndpointIndex(writeConnection, interfaceNr, UsbConstants.USB_DIR_OUT);

            if (endPointNr <0) {
                Log("No interface");
                _messageListener.errorReceived(_usbDevice.getProductName(),"No interface found to write to");
                return false;
            }

            UsbInterface writeIntf = _usbDevice.getInterface(interfaceNr);
            UsbEndpoint writeEp = writeIntf.getEndpoint(endPointNr);

            // Lock the usb interface.
            writeConnection.claimInterface(writeIntf, true);

            final byte[] newBuff = new byte[bytes.length];
            System.arraycopy(bytes,0,newBuff,0,bytes.length);
            // Write the data as a bulk transfer with defined data length.
            int r = writeConnection.bulkTransfer(writeEp, newBuff, newBuff.length, 0);
            if (r != -1) {
                Log(String.format("Written %s bytes to the scanner. Data written: %s", r, new String(bytes)));
            } else {
                Log("Error happened while writing data. No ACK");
            }
            // Release the usb interface.
            writeConnection.releaseInterface(writeIntf);
//            writeConnection.close();

        } catch(NullPointerException e)
        {
            Log("Error happend while writing. Could not connect to the device or interface is busy?");
            Log.e("HidScanDevice", Log.getStackTraceString(e));
            return false;
        }
        return true;
    }

    /**
     * Check if device is readonly or not (USB HID is readonly)
     * @return
     */


    public boolean isReadonly(){
        boolean readOnly = true;

        if (_usbVendor.getUsbClass() == UsbConstants.USB_CLASS_CDC_DATA){
            readOnly = false;
        }

        return readOnly;

    }

    /**
     * Play a beep sound on the scanner
     * Scanner must be enabled to receive the BEL character
     *
     */
    public void beep(){
        byte[] bel = new byte[]{7};
        writeData(bel);

    }




    /**
     * Find the matching Device
     * @param device
     * @return
     */

    private VendorData findMatchingDevice(UsbDevice device){

        for (VendorData vendor:vendors){
            if (device.getProductId() == vendor.getProductId() && device.getVendorId() == vendor.getVendorId()) {
                return vendor;
            }

        }
        return null;
    }

    /**
     * Thread that receives data from Scanner (USB Device) and sends it to the subscriber
     * The data is coming from a device specific driver, using a BlockingQueue.
     * This thread is in wait till a new message arrives from the device specific driver.
     *
     */
    private Runnable readerReceiver = new Runnable() {
        public void run() {

            BlockingQueue<USBResult> messageQueue = new ArrayBlockingQueue<USBResult>(1024);     //queue for reception of messages
            Thread usbReader;

            if (_usbDevice == null) {
                Log("No device to read from");
                _messageListener.errorReceived("No device","No device found to read from");

                return;
            }

            UsbEndpoint readEp;
            UsbDeviceConnection readConnection = null;
            UsbInterface readIntf = null;


            try
            {

                readConnection = _usbManager.openDevice(_usbDevice);
                int interfaceNr = getInterfaceIndex(readConnection,_usbVendor.getUsbClass());
                int endPointNr = getEndpointIndex(readConnection, interfaceNr, UsbConstants.USB_DIR_IN);

                if (endPointNr <0) {
                    Log("No interface");
                    _messageListener.errorReceived(_usbDevice.getProductName(),"No interface found to read from");
                    return;
                }

                readIntf = _usbDevice.getInterface(interfaceNr);
                readEp = readIntf.getEndpoint(endPointNr);

//                readIntf = _usbDevice.getInterface(1);
//                readEp = readIntf.getEndpoint(1);


                if (readConnection == null) {
                    Log("Cannot start reader because the user didn't gave me permissions or the device is not present. Retrying in 2 sec...");
                    _messageListener.errorReceived(_usbDevice.getSerialNumber(),"Cannot start reader because the user didn't gave me permissions or the device is not present.");
                    return;

                }

                // Claim and lock the interface in the android system.
                readConnection.claimInterface(readIntf, true);
            }
            catch (SecurityException e) {
                Log("Cannot start reader because the user didn't gave me permissions.");
                _messageListener.errorReceived(_usbDevice.getSerialNumber(),"Cannot start reader because the user didn't gave me permissions.");
                return;
            }

            Log("!!! Reader is started !!!");
            int packetSize = readEp.getMaxPacketSize();

//            String className = "be.art4l.scandevice.drivers."+_usbDriver;
            String className = "com.art4l.scandevice."+_usbVendor.getClassName();

            try {
                USBDataReader USBDataReader = (USBDataReader)Class.forName(className).newInstance();
                // configure the reader
                USBDataReader.setReadQueue(messageQueue);
                USBDataReader.setReadConnection(readConnection);
                USBDataReader.setReadEp(readEp);
                USBDataReader.setPacketSize(packetSize);

                //create a new thread
                usbReader =new Thread(USBDataReader);
                usbReader.start();

            } catch(InstantiationException
                    | IllegalAccessException
                    | ClassNotFoundException e){

                _messageListener.errorReceived(_usbDevice.getSerialNumber(),"No driver class for this device: " + _usbVendor.getClassName());

                throw new IllegalStateException(e);

            }

            //
            while (_runReadingThread) {
                try {

                    // this is a blocking queue
                    USBResult scannedValue = messageQueue.take();
                    _messageListener.messageReceived(_usbDevice.getSerialNumber(), scannedValue);

                } catch (InterruptedException ex) {
                    //TODO not handled yet

                }
                catch (ThreadDeath e) {
                    usbReader.interrupt();
                    readConnection.releaseInterface(readIntf);
                    readConnection.close();
                    throw e;
                }
            }

            //stop the reader
            usbReader.interrupt();
            // Release the interface lock.
            readConnection.releaseInterface(readIntf);
            readConnection.close();

            Log("Thread is stopped");

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
     * Get the correct interface, depends on the type of USB Device
     *
     * @param connection
     * @param usbClass
     * @return
     */

    private int getInterfaceIndex(UsbDeviceConnection connection,int usbClass){

        try {
            ConfigurationDescriptor configurationDescriptor = ConfigurationDescriptor.fromDeviceConnection(connection);

            for (int i = 0; i < configurationDescriptor.interfaces.size();i++) {
                if (configurationDescriptor.interfaces.get(i).interfaceClass == usbClass) {
                    return i;
                }

            }

        } catch (java.text.ParseException ex){


        }
        return -1;
    }

    /**
     * Get the correct endpoint to read from or to write to
     *
     * @param connection
     * @param interfaceNr
     * @param direction
     * @return
     */

    private int getEndpointIndex(UsbDeviceConnection connection,int interfaceNr, int direction){

        if (interfaceNr <0) return -1;

        try {
            ConfigurationDescriptor configurationDescriptor = ConfigurationDescriptor.fromDeviceConnection(connection);

            ConfigurationDescriptor.InterfaceInfo interfaceInfo = configurationDescriptor.interfaces.get(interfaceNr);
            for (int i =  0; i < interfaceInfo.endpoints.size();i++){
                if (interfaceInfo.endpoints.get(i).direction ==  direction) return i;
            }

        } catch (java.text.ParseException ex) {

        }
        return -1;
    }



    /**
     * Logs the message from HIDScanDevice
     * @param message to log.
     */
    private void Log(String message) {
        Log.d("HIDScanDevice: ", message);
    }





    /**
     *
     * Declare the interface to receive data
     *
     *
     */

    public interface OnMessageReceived {
        void messageReceived(String type, USBResult message);
        void errorReceived(String type, String errorMessage);
    }


}