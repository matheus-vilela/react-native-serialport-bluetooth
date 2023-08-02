package com.serialportbluetooth;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ReactModule(name = SerialportBluetoothModule.NAME)
public class SerialportBluetoothModule extends ReactContextBaseJavaModule implements EventSender{
  public static final String NAME = "SerialportBluetooth";
  private static final String INTENT_ACTION_GRANT_USB = BuildConfig.LIBRARY_PACKAGE_NAME + ".GRANT_USB";
  private final ReactApplicationContext reactContext;
  private final Map<Integer, SerialportDevice> usbSerialPorts = new HashMap<Integer, SerialportDevice>();

  public SerialportBluetoothModule(ReactApplicationContext reactContext) {
    super(reactContext);
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }


  @ReactMethod
  public void list(Promise promise) {
      WritableArray devices = Arguments.createArray();
      UsbManager usbManager = (UsbManager) getCurrentActivity().getSystemService(Context.USB_SERVICE);
      for (UsbDevice device : usbManager.getDeviceList().values()) {
          WritableMap d = Arguments.createMap();
          d.putInt("deviceId", device.getDeviceId());
          d.putInt("vendorId", device.getVendorId());
          d.putInt("productId", device.getProductId());
          devices.pushMap(d);
      }
      promise.resolve(devices);
  }

  @ReactMethod
  public void tryRequestPermission(int deviceId, Promise promise) {
      UsbManager usbManager = (UsbManager) getCurrentActivity().getSystemService(Context.USB_SERVICE);
      UsbDevice device = findDevice(deviceId);
      if (device == null) {
          promise.reject("1", "device not found");
          return;
      }

      if (usbManager.hasPermission(device)) {
          System.out.println("has permission");
          promise.resolve(1);
          return;
      }

      PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(getCurrentActivity(), 0, new Intent(INTENT_ACTION_GRANT_USB), 0);
      usbManager.requestPermission(device, usbPermissionIntent);
      promise.resolve(0);
  }

  @ReactMethod
  public void hasPermission(int deviceId, Promise promise) {
      UsbManager usbManager = (UsbManager) getCurrentActivity().getSystemService(Context.USB_SERVICE);
      UsbDevice device = findDevice(deviceId);
      if (device == null) {
          promise.reject("1", "device not found");
          return;
      }

      promise.resolve(usbManager.hasPermission(device));
      return;
  }


}
