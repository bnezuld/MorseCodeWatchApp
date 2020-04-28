/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.morsecodewatchapp;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class GattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String UART_SERVICE_UUID = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    public static String RX_CHARACTERISTIC = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    public static String TX_CHARACTERISTIC = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";

    public static String ALERT_NOTIFICATION_SERVICE_UUID = "00001811-0000-1000-8000-00805f9b34fb";
    public static String UNREAD_CHARACTERISTIC = "00002a45-0000-1000-8000-00805f9b34fb";
    public static String NEW_ALERT_CHARACTERISTIC = "00002a46-0000-1000-8000-00805f9b34fb";

    public static String UNREAD_CATAGORY = "00002a47-0000-1000-8000-00805f9b34fb";
    public static String NEW_ALERT_CATAGORY = "00002a48-0000-1000-8000-00805f9b34fb";

    public static String ALERT_NOTIFICATION_CONTROL = "00002a44-0000-1000-8000-00805f9b34fb";

    public static String CLIENT_CHARACTERISTIC_CONFIGURATION = "00002902-0000-1000-8000-00805f9b34fb";


    static {
        // Sample Services.
        attributes.put(UART_SERVICE_UUID, "UART service");
        //attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        // Sample Characteristics.
        attributes.put(TX_CHARACTERISTIC, "Transmit characteristic");
        attributes.put(RX_CHARACTERISTIC, "Receive characteristic");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
