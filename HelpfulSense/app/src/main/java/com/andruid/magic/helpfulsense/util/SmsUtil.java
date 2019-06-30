package com.andruid.magic.helpfulsense.util;

import android.content.Context;
import android.location.Location;
import android.telephony.SmsManager;

import com.wafflecopter.multicontactpicker.ContactResult;
import com.wafflecopter.multicontactpicker.RxContacts.PhoneNumber;

import java.util.ArrayList;
import java.util.List;

public class SmsUtil {
    public static void sendSMS(Context context, Location location, String message){
        message = message +
                " I am at (" +
                location.getLatitude() +
                ", " +
                location.getLongitude() +
                ").";
        SmsManager smsManager = SmsManager.getDefault();
        ArrayList<String> parts = smsManager.divideMessage(message);
        List<ContactResult> contacts = FileUtil.readContactsFromFile(context);
        for(ContactResult contact : contacts){
            List<PhoneNumber> phoneNumbers = contact.getPhoneNumbers();
            for(PhoneNumber phone : phoneNumbers){
                smsManager.sendMultipartTextMessage(phone.getNumber(), null, parts, null,
                        null);
            }
        }
    }
}