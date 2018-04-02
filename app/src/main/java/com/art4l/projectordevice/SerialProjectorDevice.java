package com.art4l.projectordevice;

/**
 *
 * Created by Dirk on 1/12/17.
 */

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.art4l.projectordevice.usbserial.UsbSerialDevice;
import com.art4l.projectordevice.usbserial.UsbSerialInterface;
import com.art4l.scandevice.MessageType;
import com.art4l.scandevice.USBResult;
import com.art4l.scandevice.VendorData;
import com.example.androidthings.scandevice.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This class is used for talking to a Serial USB Device, linked to a projector
 * Purpose is to send commands to the projector like switch on or switch off
 * @author Dirk Matheussen
 */
public class SerialProjectorDevice {
    private Context _context;
    // Locker object that is responsible for locking read/write thread. Not used at this moment

    private VendorData _usbVendor;


    private UsbManager _usbManager;
    private UsbDevice _usbDevice;
    private UsbDeviceConnection _usbConnection;
    private UsbSerialDevice _projectorDevice;

    private boolean _serialPortConnected = false;

    private ArrayList<VendorData> vendors = new ArrayList<VendorData>();

    // return event
    private OnMessageReceived _messageListener;


    /**
     * Creates a hid bridge to the Scanner. Should be created once.
     * @param context is the UI context of Android.
     */
    public SerialProjectorDevice(Context context) {
        _context = context;

        try {
            XmlResourceParser scanDevices = _context.getResources().getXml(R.xml.serialprojectordevices);
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

        if (_serialPortConnected) return true;

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

        _usbConnection =  _usbManager.openDevice(_usbDevice);

        _projectorDevice = UsbSerialDevice.createUsbSerialDevice(_usbDevice,_usbConnection);

        if (_projectorDevice != null) {
            if (_projectorDevice.open()) {
                _serialPortConnected = true;
                _projectorDevice.setBaudRate(19200);
                _projectorDevice.setDataBits(UsbSerialInterface.DATA_BITS_8);
                _projectorDevice.setStopBits(UsbSerialInterface.STOP_BITS_1);
                _projectorDevice.setParity(UsbSerialInterface.PARITY_NONE);
                _projectorDevice.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);

                _projectorDevice.read(new UsbSerialInterface.UsbReadCallback() {
                    @Override
                    public void onReceivedData(byte[] arg0) {
                        try {
                            String data = new String(arg0, "UTF-8");
                            USBResult usbResult = new USBResult();
                            usbResult.setBarcodeMessage(data);
                            usbResult.setMessageType(MessageType.PROJECTORMESSAGE);
                            _messageListener.messageReceived(_usbDevice.getSerialNumber(),usbResult);
                        } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                        }
                    }
                });

            } else {
                    }
        } else {
        }

        return true;
    }

    /**
     * Closes the reading thread of the device.
     */
    public  boolean closeDevice(UsbDevice usbDevice) {

        _serialPortConnected = false;

        if (_usbDevice == null) return false;
        if (usbDevice.getDeviceId() == _usbDevice.getDeviceId()) {
            try {

            } catch (RuntimeException e) {
                _messageListener.errorReceived(_usbDevice.getSerialNumber(),"Error happend while closing device. Usb reciever not connected.");

                Log("Error happend while closing device. Usb reciever not connected.");
            }
            return true;
        }
        return false;

    }



    /**
     * Write data to the usb hid. Data is written as-is, so calling method is responsible for adding header data.
     * @param bytes is the data to be written.
     * @return true if succeed.
     */
    public boolean writeData(byte[] bytes) {

        if (_projectorDevice !=null) {
            _projectorDevice.write(bytes);
            return true;
        } else {
            return false;
        }
    }



    public void checkState(){
        writeData("(PWR?)".getBytes());            //check if device is switched on or off

    }

    /**
     * Switch on or off the projector
     *
     * @param setOn
     */

    public void powerSwitch(boolean setOn){

        if (setOn){
            writeData("(PWR1)".getBytes());
        } else {
            writeData("(PWR0)".getBytes());
        }

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
     * Logs the message from SerialProjectorDevice
     * @param message to log.
     */
    private void Log(String message) {
        Log.d("SerialProjectorDevice: ", message);
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