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
import java.util.List;

@ReactModule(name = SerialportBluetoothModule.NAME)
public class SerialportBluetoothModule extends ReactContextBaseJavaModule implements EventSender{
  public static final String NAME = "SerialportBluetooth";
  private static final String INTENT_ACTION_GRANT_USB = BuildConfig.LIBRARY_PACKAGE_NAME + ".GRANT_USB";
  private final ReactApplicationContext reactContext;
  private final Map<Integer, SerialportDevice> usbSerialPorts = new HashMap<Integer, SerialportDevice>();

  public SerialportBluetoothModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
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
      List<UsbSerialDriver> usbSerialDriverList = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
      HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
      // for (UsbDevice driver : usbSerialDriverList) {
      //     WritableMap d = Arguments.createMap();
      //     d.putInt("deviceId", findDevice());
      //     d.putInt("vendorId", driver.getVendorId());
      //     d.putInt("productId", driver.getProductId());
      //     devices.pushMap(d);
      // }
      for (UsbSerialDriver driver : usbSerialDriverList) {
        WritableMap deviceData = Arguments.createMap();
        deviceData.putString("name", driver.getDevice().getDeviceName());
        deviceData.putInt("vendorId", driver.getDevice().getVendorId());
        deviceData.putInt("productId", driver.getDevice().getProductId());

        for (Map.Entry<String, UsbDevice> usbDeviceEntry : usbDevices.entrySet()) {
            UsbDevice usbDevice = usbDeviceEntry.getValue();
            if (driver.getDevice().getDeviceName().equals(usbDevice.getDeviceName())) {
              deviceData.putString("type", "usb");
              deviceData.putInt("deviceId", usbDevice.getDeviceId());
              devices.pushMap(deviceData);
            }
        }
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

  public void sendEvent(final String eventName, final WritableMap event) {
    reactContext.runOnUiQueueThread(new Runnable() {
        @Override
        public void run() {
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, event);
        }
    });
  }

  @ReactMethod
  public void open(int deviceId, int baudRate, int dataBits, int stopBits, int parity,int readWaitMillis, int writeWaitMillis, Promise promise) {
      SerialportDevice wrapper = usbSerialPorts.get(deviceId);
      if (wrapper != null) {
          promise.resolve(deviceId);
          return;
      }

      UsbManager usbManager = (UsbManager) getCurrentActivity().getSystemService(Context.USB_SERVICE);
      UsbDevice device = findDevice(deviceId);
      if (device == null) {
          promise.reject("1", "device not found");
          return;
      }

      UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
      if (driver == null) {
          promise.reject("2", "no driver for device");
          return;
      }
      if (driver.getPorts().size() < 0) {
          promise.reject("3", "not enough ports at device");
          return;
      }

      UsbDeviceConnection connection = usbManager.openDevice(driver.getDevice());
      if(connection == null) {
          if (!usbManager.hasPermission(driver.getDevice())) {
              promise.reject("4", "connection failed: permission denied");
          } else {
              promise.reject("5", "connection failed: open failed");
          }
          return;
      }

      UsbSerialPort port = driver.getPorts().get(0);
      try {
          port.open(connection);
          port.setParameters(baudRate, dataBits, stopBits, parity);
      } catch (IOException e) {
          try {
                port.close();
          } catch (IOException ignored) {}
          promise.reject("6", "connection failed", e);
          return;
      }

      wrapper = new SerialportDevice(deviceId, port, readWaitMillis, writeWaitMillis, this);
      usbSerialPorts.put(deviceId, wrapper);
      promise.resolve(deviceId);
  }

  @ReactMethod
  public void send(int deviceId, String hexStr, Promise promise) {
      SerialportDevice wrapper = usbSerialPorts.get(deviceId);
      if (wrapper == null) {
          promise.reject("0", "device not open");
          return;
      }

      byte[] data = hexStringToByteArray(hexStr);
      try {
          wrapper.send(data);
          promise.resolve(null);
      } catch (IOException e) {
          promise.reject("0", "send failed", e);
          return;
      }
  }

  @ReactMethod
  public void close(int deviceId, Promise promise) {
      SerialportDevice wrapper = usbSerialPorts.get(deviceId);
      if (wrapper == null) {
          promise.reject("7", "serial port not open or closed");
          return;
      }

      wrapper.close();
      usbSerialPorts.remove(deviceId);
      promise.resolve(null);
  }

  private UsbDevice findDevice(int deviceId) {
    UsbManager usbManager = (UsbManager) getCurrentActivity().getSystemService(Context.USB_SERVICE);
    for (UsbDevice device : usbManager.getDeviceList().values()) {
        if (device.getDeviceId() == deviceId) {
            return device;
        }
    }

    return null;
  }

  public static byte[] hexStringToByteArray(String s) {
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
            + Character.digit(s.charAt(i + 1), 16));
    }
    return data;
  }

  private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

  public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
  }

}
