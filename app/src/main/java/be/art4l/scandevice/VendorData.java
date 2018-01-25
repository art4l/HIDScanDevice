package be.art4l.scandevice;

/**
 * Object contains the vendor specific data
 * Created by Dirk on 25/01/18.
 */

public class VendorData {

    private String className;           //name of the driver class
    private int vendorId;
    private int productId;

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
}
