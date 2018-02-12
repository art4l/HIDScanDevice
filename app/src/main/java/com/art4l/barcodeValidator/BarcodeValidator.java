package com.art4l.barcodeValidator;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.PatternSyntaxException;

/**
 * Validates a barcode with a set of regex rules, stored in BarcodeMasks
 *
 * Created by Dirk on 12/02/18.
 */

public class BarcodeValidator {

    private ArrayList<BarcodeMask> mBarcodeMasks = new ArrayList<BarcodeMask>();


    public BarcodeValidator(){


    }

    /**
     * Add a barcodMask to the list of barcodeMasks & sort in priority
     * (1 =highest priority)
     * @param barcodeMask barcodeMask to add
     */

    public void addBarcodeMask(BarcodeMask barcodeMask){

        mBarcodeMasks.add(barcodeMask);

        //sort in priority
        Collections.sort(mBarcodeMasks, new Comparator<BarcodeMask>(){
            public int compare(BarcodeMask mask1, BarcodeMask mask2) {
                return mask1.getPriority() - mask2.getPriority(); // sort order
            }
        });


    }

    /**
     * Validate a barcode with the list of registered barcodeMasks
     * barcodeType is optional (can be null)
     *
     * @param barcodeType: BarcodeMask enumaration of barcode typs
     * @param barcodeValue: Scanned barcode value to check
     * @return true of false
     */


    public boolean validateBarcode(BarcodeMask.BarcodeType barcodeType, String barcodeValue){

        boolean findMatch  = false;
        //loop throug the set of barcodeMasks
        for (BarcodeMask barcodeMask:mBarcodeMasks){
            if (barcodeType != null && barcodeType == barcodeMask.getBarcodeType()){
                findMatch = (checkRegex(barcodeMask, barcodeValue));
            } else if (barcodeType == null){
                findMatch = (checkRegex(barcodeMask, barcodeValue));
            }
            if (findMatch) return true;

        }
        return false;

    }

    /**
     * Load regex descriptions in Json format into the lookup list
     *
     * @param json in BarcodeMask format
     */


    public void loadJsonArray(String json) throws JSONException {

        Gson gsonClass = new Gson();

        JSONObject gson = new JSONObject(json);
        JSONArray jsonArray = gson.getJSONArray("barcodemasks");

        for (int i = 0; i <  jsonArray.length();i++){
            String gsonStr = jsonArray.getString(i);
            BarcodeMask barcodeMask= gsonClass.fromJson(gsonStr, BarcodeMask.class);
            if (barcodeMask != null) addBarcodeMask(barcodeMask);
        }

    }

    /**
     * Apply the validation of a mask
     * This is a runtime  execution version, a pre-compile (Pattern.compile) version to foresee later
     * @param barcodeMask mask for validation
     * @param barcodeValue scanned value to validate
     * @return true or false
     */

    private boolean checkRegex(BarcodeMask barcodeMask, String barcodeValue) throws PatternSyntaxException {
        //apply regex rule
        return barcodeValue.matches(barcodeMask.getRegexString());
        //TODO Use a Pattern.compile version and not the String runtime version.

    }

}



