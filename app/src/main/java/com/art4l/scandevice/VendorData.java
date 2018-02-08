package com.art4l.scandevice;

/**
 * Object contains the vendor specific data
 * Created by Dirk on 25/01/18.
 */

public class VendorData {

    private String deviceName;
    private String className;           //name of the driver class
    private int vendorId;
    private int productId;
    private int usbClass;

    public VendorData(){


    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public int getVendorId() {
        return vendorId;
    }

    public void setVendorId(int vendorId) {
        this.vendorId = vendorId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public int getUsbClass() {
        return usbClass;
    }

    public void setUsbClass(int usbClass) {
        this.usbClass = usbClass;
    }
}
