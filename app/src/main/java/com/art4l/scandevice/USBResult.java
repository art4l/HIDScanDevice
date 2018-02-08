package com.art4l.scandevice;

/**
 * Object to store the result of a USB read action from a scanner
 * Created by Dirk on 25/01/18.
 */

public class USBResult {

    private MessageType messageType;
    private String barcodeType;
    private String barcodeMessage;

    public USBResult(){


    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getBarcodeType() {
        return barcodeType;
    }

    public void setBarcodeType(String barcodeType) {
        this.barcodeType = barcodeType;
    }

    public String getBarcodeMessage() {
        return barcodeMessage;
    }

    public void setBarcodeMessage(String barcodeMessage) {
        this.barcodeMessage = barcodeMessage;
    }
}
