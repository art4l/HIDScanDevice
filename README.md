# USB Scanner Driver for Android Things

This application contains and demonstrated the use of a USB Scanner library for Android Things
The starting app is based on the Android Things USBEnumaration sample app.




## Getting Started

The HIDScanDevice is the main object to use in your Activity
Make sure that your activity receives USB Attach events
            <intent-filter>
                <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
            </intent-filter>

            <meta-data android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"
                       android:resource="@xml/device_filter" />



Create a instance of HIDScanDevice

        mHIDScanDevice = new HIDScanDevice(this);
        if (mHIDScanDevice.openDevice()) {
            mHIDScanDevice.startReadingThread();
            mHIDScanDevice.setMessageListener(new HIDScanDevice.OnMessageReceived() {
                @Override
                public void messageReceived(String device, USBResult message) {
                    printResult("Message Received from: " + type + " Content: " + message.getBarcodeMessage());


                }
            });
        };

Every scan (or error) is send to the MessageListener. The return value is an USBResult object

Handle the connect & disconnect of a USB Device

    /**
     * Broadcast receiver to handle USB disconnect events.
     */
    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    mHIDScanDevice.closeDevice(device))
                }
            }
        }
    };

    register the receiver in onCreate() & unregister in onDestroy()


    /**
    * The method is called by the onNewIntent(Intent) event for USB Connect events
    */


    private void handleIntent(Intent intent) {
        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (device != null) {
            if (mHIDScanDevice.openDevice()) {
                mHIDScanDevice.startReadingThread();
                mHIDScanDevice.setMessageListener(new HIDScanDevice.OnMessageReceived() {
                    @Override
                    public void messageReceived(String type, USBResult message) {
                        printResult("Message Received from: " + type + " Content: " + message.getBarcodeMessage());
                    }
                });
            };
        }
    }








## Expand the libary
To add new USB Scanners following actions are needed
First add new scanner in the usbscandevicex.xml (stored as an xml resource)
    <usb-device>
        <plugin-name>Honeywell1980DataReader</plugin-name>
        <vendor-id>3118</vendor-id>
        <product-id>3175</product-id>
    </usb-device>

Second: create a new DataReader, as an extension of USBDataReader. MAke sure that the classname
is identical to the plugin-name entry in the xml file.

Third: add the vendor & product id also in the device_filter.xml



