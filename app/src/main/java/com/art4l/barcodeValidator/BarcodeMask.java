package com.art4l.barcodeValidator;

/**
 * Class for storing a barcode mask
 * Created by Dirk on 12/02/18.
 */

public class BarcodeMask {

    private String regexString;
    private int priority;
    private BarcodeType barcodeType;


    public enum BarcodeType {EAN_8,EAN_13,TWO_O_5I,CODE_39,CODE_128, EAN_128, QR,DATAMATRIX,PDF417}

    public BarcodeMask(){
        priority = 999;         //by default the lowest priority

    }

    public String getRegexString() {
        return regexString;
    }

    public void setRegexString(String regexString) {
        this.regexString = regexString;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public BarcodeType getBarcodeType() {
        return barcodeType;
    }

    public void setBarcodeType(BarcodeType barcodeType) {
        this.barcodeType = barcodeType;
    }


}
