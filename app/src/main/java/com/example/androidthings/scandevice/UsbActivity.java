/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.androidthings.scandevice;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.text.ParseException;
import java.util.HashMap;
import java.util.regex.PatternSyntaxException;

import com.art4l.barcodeValidator.BarcodeMask;
import com.art4l.barcodeValidator.BarcodeValidator;
import com.art4l.scandevice.ConfigurationDescriptor;
import com.art4l.scandevice.DeviceDescriptor;
import com.art4l.scandevice.HIDScanDevice;
import com.art4l.scandevice.USBResult;
import com.art4l.scandevice.UsbHelper;

import org.json.JSONException;

public class UsbActivity extends Activity {
    private static final String TAG = "UsbEnumerator";

    /* USB system service */
    private UsbManager mUsbManager;

    // ScanDevice Interface
    private HIDScanDevice mHIDScanDevice;

    /* UI elements */
    private TextView mStatusView, mResultView;

    BarcodeValidator mBarcodeValidator;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb);

        mStatusView = findViewById(R.id.text_status);
        mResultView = findViewById(R.id.text_result);


        mUsbManager = getSystemService(UsbManager.class);

        // Detach events are sent as a system-wide broadcast
        IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);

//        handleIntent(getIntent());

        //create the scandevice interface, even of no device is attached
        mHIDScanDevice = new HIDScanDevice(this);

        /*
        if (mHIDScanDevice.openDevice()) {
            mHIDScanDevice.startReadingThread();
            mHIDScanDevice.setMessageListener(new HIDScanDevice.OnMessageReceived() {
                @Override
                public void messageReceived(String type, USBResult message) {
                    printResult("Message Received from: " + type + " Content: " + message.getBarcodeMessage());



                }

                @Override
                public void errorReceived(String type, String message){


                }
            });
        };
*/

        mBarcodeValidator = new BarcodeValidator();
        String barcodeMasks = new String();

        barcodeMasks = "{'barcodemasks': [{'regexString': '3232\\\\d{14}','priority': 1,'barcodeType':CODE_128},"+
                "{'regexString': '3232\\\\d{20}','priority': 2,'barcodeType':CODE_128},"+
                "{'regexString': 'CZ\\\\w{9}BE','priority': 3,'barcodeType':CODE_128},"+
                "{'regexString': 'CZ\\\\w{9}BE','priority': 3,'barcodeType':CODE_39},"+
                "{'regexString': 'C\\\\w{10}DE','priority': 4,'barcodeType':CODE_128},"+
                "{'regexString': '\\\\d{12}','priority': 5,'barcodeType':TWO_O_5I},"+
                "{'regexString': '1J\\\\w{13,30}','priority': 6,'barcodeType':CODE_128},"+
                "{'regexString': 'JJD\\\\w{8}','priority': 7,'barcodeType':CODE_128}," +
                "{'regexString': '00\\\\d{18}','priority': 8,'barcodeType':CODE_128}," +
                "{'regexString': '00\\\\d{18}','priority': 8,'barcodeType':EAN_128}," +
                "{'regexString': 'JJBEA70\\\\w{11}','priority': 9,'barcodeType':CODE_128}," +
                "{'regexString': '1J\\\\w{13,30}','priority': 10,'barcodeType':CODE_128}," +
                "{'regexString': '2J\\\\w{13,30}','priority': 11,'barcodeType':CODE_128}," +
                "{'regexString': '3J\\\\w{13,30}','priority': 12,'barcodeType':CODE_128}," +
                "{'regexString': '4J\\\\w{13,30}','priority': 13,'barcodeType':CODE_128}," +
                "{'regexString': '5J\\\\w{13,30}','priority': 14,'barcodeType':CODE_128}," +
                "{'regexString': '6J\\\\w{13,30}','priority': 15,'barcodeType':CODE_128}," +
                "{'regexString': '7J\\\\w{13,30}','priority': 16,'barcodeType':CODE_128}," +
                "{'regexString': '8J\\\\w{13,30}','priority': 17,'barcodeType':CODE_128}," +
                "{'regexString': '9J\\\\w{13,30}','priority': 18,'barcodeType':CODE_128}," +
                "{'regexString': '10J\\\\w{13,30}','priority': 19,'barcodeType':CODE_128}," +
                "{'regexString': 'J\\\\w{12,30}','priority': 20,'barcodeType':CODE_128}," +
                "{'regexString': '3299\\\\d{20}','priority': 21,'barcodeType':CODE_128}," +
                "{'regexString': 'EE\\\\w{9}BE','priority': 22,'barcodeType':CODE_128}," +
                "{'regexString': 'EE\\\\w{9}BE','priority': 22,'barcodeType':CODE_39}," +
                "{'regexString': 'CD\\\\w{9}BE','priority': 23,'barcodeType':CODE_128}," +
                "{'regexString': 'CD\\\\w{9}BE','priority': 23,'barcodeType':CODE_39}," +
                "{'regexString': 'CE\\\\w{9}BE','priority': 24,'barcodeType':CODE_128}," +
                "{'regexString': 'CE\\\\w{9}BE','priority': 24,'barcodeType':CODE_39}," +
                "{'regexString': '01054128850\\\\d{19}','priority': 25,'barcodeType':CODE_128}," +
                "{'regexString': 'R\\\\w{12}','priority': 26,'barcodeType':CODE_128}," +
                "{'regexString': 'R\\\\w{12}','priority': 26,'barcodeType':CODE_39}," +
                "{'regexString': 'L\\\\w{12}','priority': 27,'barcodeType':CODE_128}," +
                "{'regexString': 'L\\\\w{12}','priority': 27,'barcodeType':CODE_39}," +
                "{'regexString': 'V\\\\w{12}','priority': 28,'barcodeType':CODE_128}," +
                "{'regexString': 'V\\\\w{12}','priority': 28,'barcodeType':CODE_39}," +
                "{'regexString': 'S\\\\w{2}','priority': 99,'barcodeType':CODE_128}," +
                "{'regexString': 'S\\\\w{2}','priority': 99,'barcodeType':CODE_39}," +
                "{'regexString': 'BE\\\\w{60}','priority': 99,'barcodeType':DATAMATRIX}," +
                "{'regexString': 'JJBEA\\\\w{27}','priority': 99,'barcodeType':DATAMATRIX}" +
                "]}";

        try {
            mBarcodeValidator.loadJsonArray(barcodeMasks);
            boolean validBarcode = false;
            validBarcode =  mBarcodeValidator.validateBarcode(BarcodeMask.BarcodeType.TWO_O_5I,"A23456789012");
            Log.d(TAG,"Is barcode valid? " + validBarcode);
            validBarcode =  mBarcodeValidator.validateBarcode(BarcodeMask.BarcodeType.CODE_128,"CZ123456789BE");
            Log.d(TAG,"Is barcode valid? " + validBarcode);
            validBarcode =  mBarcodeValidator.validateBarcode(null,"AJBEA1234474747");
            Log.d(TAG,"Is barcode valid? " + validBarcode);

        } catch (JSONException ex){
            Log.d(TAG,"Json Error: " + ex.getMessage());

        } catch (PatternSyntaxException ex){
            Log.d(TAG,"Regex Error: " + ex.getMessage());
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUsbReceiver);
    }

    /**
     * Broadcast receiver to handle USB disconnect events.
     */
    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    printStatus(getString(R.string.status_removed));
                    if (mHIDScanDevice !=  null && mHIDScanDevice.closeDevice(device))
                        printDeviceDescription(device);
                }
            }
        }
    };

    /**
     * Determine whether to list all devices or query a specific device from
     * the provided intent.
     * @param intent Intent to query.
     */
    private void handleIntent(Intent intent) {
        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (device != null) {
            printStatus(getString(R.string.status_added));
            printDeviceDetails(device);
            if (mHIDScanDevice.openDevice()) {



                mHIDScanDevice.startReadingThread();
                mHIDScanDevice.setMessageListener(new HIDScanDevice.OnMessageReceived() {
                    @Override
                    public void messageReceived(String type, USBResult message) {
                        printResult("Message Received from: " + type + " Content: " + message.getBarcodeMessage());
                       if(!mBarcodeValidator.validateBarcode(null,message.getBarcodeMessage())){
                           mHIDScanDevice.beep();
                           mHIDScanDevice.beep();
                           mHIDScanDevice.beep();

                       } else {
                           mHIDScanDevice.beep();
                       }

                    }

                    @Override
                    public void errorReceived(String type, String message){
                        printResult("Message Received from: " + type + " Content: " + message);
                    }
                });


            };

        } else {
            // List all devices connected to USB host on startup
            printStatus(getString(R.string.status_list));
            printDeviceList();
        }
    }

    /**
     * Print the list of currently visible USB devices.
     */
    private void printDeviceList() {
        HashMap<String, UsbDevice> connectedDevices = mUsbManager.getDeviceList();

        if (connectedDevices.isEmpty()) {
            printResult("No Devices Currently Connected");
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append("Connected Device Count: ");
            builder.append(connectedDevices.size());
            builder.append("\n\n");
            for (UsbDevice device : connectedDevices.values()) {
                //Use the last device detected (if multiple) to open
                builder.append(UsbHelper.readDevice(device));
                builder.append("\n\n");
            }
            printResult(builder.toString());
        }
    }
    /**
     * Print a basic description about a specific USB device.
     * @param device USB device to query.
     */
    private void printDeviceDescription(UsbDevice device) {
        String result = UsbHelper.readDevice(device) + "\n\n";
        printResult(result);
    }

    /**
     * Initiate a control transfer to request the device information
     * from its descriptors.
     *
     * @param device USB device to query.
     */
    private void printDeviceDetails(UsbDevice device) {
        UsbDeviceConnection connection = mUsbManager.openDevice(device);

        String deviceString = "";
        try {
            //Parse the raw device descriptor
            deviceString = DeviceDescriptor.fromDeviceConnection(connection)
                    .toString();
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Invalid device descriptor", e);
        }

        String configString = "";
        try {
            //Parse the raw configuration descriptor
            configString = ConfigurationDescriptor.fromDeviceConnection(connection)
                    .toString();
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Invalid config descriptor", e);
        } catch (ParseException e) {
            Log.w(TAG, "Unable to parse config descriptor", e);
        }

        printResult(deviceString + "\n\n" + configString);
        connection.close();
    }

    /* Helpers to display user content */

    private void printStatus(String status) {
        mStatusView.setText(status);
        Log.i(TAG, status);
    }

    private void printResult(final String result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mResultView.setText(result);
            }
        });
        Log.i(TAG, result);
    }



}
