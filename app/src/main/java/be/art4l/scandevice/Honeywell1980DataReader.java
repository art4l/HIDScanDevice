package be.art4l.scandevice;

import android.util.Log;

/**
 * DataReader for the Honeywell 1980i USB Scanner
 * Decode the datastring and put it on the queue
 * Created by Dirk on 25/01/18.
 */

public class Honeywell1980DataReader extends USBDataReader {


    public Honeywell1980DataReader( ){
        super();

    }



    @Override
    public void run() {

        String scannedValue = "";

        while (!Thread.interrupted()){
            try
            {

                // Read the data as a bulk transfer with the size = MaxPacketSize
                int r = readConnection.bulkTransfer(readEp, bytes, packetSize, 200);


                //Logic for the Honeywell 1980i USB Scanner

                if (r > 0){
                    while (r >= 0 && bytes[0] == 2) {

                        if (r > 0 )scannedValue = scannedValue + composeReturnString(bytes);
                        r = readConnection.bulkTransfer(readEp, bytes, packetSize, 200);
                    }
                }

                if (scannedValue.length() > 0) {
                    USBResult usbResult = new USBResult();
                    usbResult.setMessageType(MessageType.BARCODEMESSAGE);
                    usbResult.setBarcodeMessage(scannedValue);
                    readQueue.add(usbResult);
                }
                scannedValue = "";


            }

            catch (NullPointerException e) {
                Log.d("USBDataReader","Error happened while reading. No device or the connection is busy");
                Log.e("USBDataReader", Log.getStackTraceString(e));
                USBResult usbResult = new USBResult();
                usbResult.setMessageType(MessageType.ERRORMESSAGE);
                usbResult.setBarcodeMessage("Error happened while reading. No device or the connection is busy");
                readQueue.add(usbResult);

                break;
            }


            // Sleep for 10 ms to pause and restart a new cycle
            // TODO No Synchronisation between read and write is done at this moment
            //Write is not implemented yet

            sleep(10);
        }

        Log.d("USBDatareader","USBDataReader is interrupted" );



    }

    /**
     * Create a return string with only valid ASCII value's and the correct length
     */

    @Override
    protected String composeReturnString(byte[] bytes) {

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



}
