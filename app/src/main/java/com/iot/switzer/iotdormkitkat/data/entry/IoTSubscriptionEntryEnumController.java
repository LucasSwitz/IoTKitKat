package com.iot.switzer.iotdormkitkat.data.entry;

import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.iot.switzer.iotdormkitkat.data.entry.IoTSubscriptionEntry;
import com.iot.switzer.iotdormkitkat.data.entry.IoTUIController;

/**
 * Created by Lucas Switzer on 6/30/2016.
 */
public class IoTSubscriptionEntryEnumController extends IoTUIController {

    private Spinner spinner;
    private int position = 0;

    public IoTSubscriptionEntryEnumController(Context context, IoTSubscriptionEntry entry) {
        super(context,entry);
        spinner = new Spinner(context);

        Integer enums[] = new Integer[entry.getDescription().highLimit+1];

        for (int i = 0; i < enums.length; i++) {
            enums[i] = i;
        }

        ArrayAdapter<Integer> spinnerArrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, enums);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerArrayAdapter);

        disable();
    }

    @Override
    public void postValue() {
       spinner.setSelection(position);
    }

    @Override
    public View getView() {
        return spinner;
    }

    @Override
    public void onValueUpdate(IoTSubscriptionEntry entry) {
        position = entry.getValAsInt();
    }
}
