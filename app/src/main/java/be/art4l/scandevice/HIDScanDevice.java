package be.art4l.scandevice;

/**
 *
 * Created by Dirk on 1/12/17.
 */

import android.content.Context;
import android.content.res.XmlResourceParser;
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

    private String _usbDriver;
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
                    if (eventType == XmlPullParser.END_TAG && scanDevices.getName().equals("usb-device")){
                        Log("From XML: All together");
                        VendorData vendorData = new VendorData();
                        vendorData.setClassName(className);
                        vendorData.setProductId(Integer.valueOf(productId));
                        vendorData.setVendorId(Integer.valueOf(vendorId));
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
     * Find the matching Device
     * @param device
     * @return
     */

    private String findMatchingDevice(UsbDevice device){

        for (VendorData vendor:vendors){
            if (device.getProductId() == vendor.getProductId() && device.getVendorId() == vendor.getVendorId()) {
                return vendor.getClassName();
            }

        }

        return null;
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
            _usbDriver = findMatchingDevice(device);
            if ( _usbDriver !=null){
                _usbDevice = device;
            }
        }

        if (_usbDevice == null) {
            Log("Cannot find the device. Did you forgot to plug it?");
//            Log(String.format("\t I search for VendorId: %s and ProductId: %s", _vendorId, _productId));
            return false;
        }

        Log("Found the device");
        return true;
    }

    /**
     * Closes the reading thread of the device.
     */
    public  boolean closeDevice(UsbDevice usbDevice) {

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


    /**
     * Thread that receives data from Scanner (USB Device) and sends it to the subscriber
     *
     */
    private Runnable readerReceiver = new Runnable() {
        public void run() {

            BlockingQueue<USBResult> messageQueue = new ArrayBlockingQueue<USBResult>(1024);     //queue for reception of messages
            Thread usbReader;

            if (_usbDevice == null) {
                Log("No device to read from");
                USBResult usbResult = new USBResult();
                usbResult.setMessageType(MessageType.ERRORMESSAGE);
                usbResult.setBarcodeMessage("No device to read from");
                _messageListener.messageReceived("no device",usbResult);

                return;
            }

            UsbEndpoint readEp;
            UsbDeviceConnection readConnection = null;
            UsbInterface readIntf = null;

            readIntf = _usbDevice.getInterface(0);
            readEp = readIntf.getEndpoint(0);
            try
            {

                readConnection = _usbManager.openDevice(_usbDevice);

                if (readConnection == null) {
                    Log("Cannot start reader because the user didn't gave me permissions or the device is not present. Retrying in 2 sec...");
                    USBResult usbResult = new USBResult();
                    usbResult.setMessageType(MessageType.ERRORMESSAGE);
                    usbResult.setBarcodeMessage("Cannot start reader because the user didn't gave me permissions or the device is not present.");
                    _messageListener.messageReceived(_usbDevice.getManufacturerName(),usbResult);
                    return;

                }

                // Claim and lock the interface in the android system.
                readConnection.claimInterface(readIntf, true);
            }
            catch (SecurityException e) {
                Log("Cannot start reader because the user didn't gave me permissions.");
                USBResult usbResult = new USBResult();
                usbResult.setMessageType(MessageType.ERRORMESSAGE);
                usbResult.setBarcodeMessage("Cannot start reader because the user didn't gave me permissions.");
                _messageListener.messageReceived(_usbDevice.getManufacturerName(),usbResult);
                return;
            }

            Log("!!! Reader is started !!!");
            int packetSize = readEp.getMaxPacketSize();

//            String className = "be.art4l.scandevice.drivers."+_usbDriver;
            String className = "be.art4l.scandevice."+_usbDriver;

            try {
                USBDataReader USBDataReader = (USBDataReader)Class.forName(className).newInstance();
                USBDataReader.setReadQueue(messageQueue);
                USBDataReader.setReadConnection(readConnection);
                USBDataReader.setReadEp(readEp);
                USBDataReader.setPacketSize(packetSize);


//             USBDataReader USBDataReader = new Honeywell1980DataReader(messageQueue,readConnection,readEp,packetSize);
                usbReader =new Thread(USBDataReader);
                usbReader.start();

            } catch(InstantiationException
                    | IllegalAccessException
                    | ClassNotFoundException e){

                USBResult usbResult = new USBResult();
                usbResult.setMessageType(MessageType.ERRORMESSAGE);
                usbResult.setBarcodeMessage("No driver class for this device: " + _usbDriver);
                _messageListener.messageReceived(_usbDevice.getManufacturerName(),usbResult);

                throw new IllegalStateException(e);

            }

            while (_runReadingThread) {

                try {

                    USBResult scannedValue = messageQueue.take();                                        //when no message arrive, this is blocked
                    _messageListener.messageReceived(_usbDevice.getManufacturerName(), scannedValue);

                } catch (InterruptedException ex) {



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
     * Logs the message from HidBridge.
     * @param message to log.
     */
    private void Log(String message) {
        //LogHandler logHandler = LogHandler.getInstance();
        //logHandler.WriteMessage("HidBridge: " + message, LogHandler.GetNormalColor());
        Log.d("HIDScanDevice: ", message);
    }





    /**
     *
     * Declare the interface to receive data
     *
     *
     */

    public interface OnMessageReceived {
        public void messageReceived(String type, USBResult message);
    }


}