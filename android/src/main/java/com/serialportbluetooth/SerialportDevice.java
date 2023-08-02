package com.serialportbluetooth;

import java.io.IOException;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import android.util.Log;

public class SerialportDevice implements SerialInputOutputManager.Listener {
  private static final int WRITE_WAIT_MILLIS = 2000;
  private static final int READ_WAIT_MILLIS = 2000;

  private static final String DataReceivedEvent = "serialportDataReceived";

  private int deviceId;
  private UsbSerialPort port;
  private EventSender sender;
  private boolean closed = false;
  private SerialInputOutputManager ioManager;


  public SerialportDevice(int deviceId, UsbSerialPort port, EventSender sender) {
    this.deviceId = deviceId;
    this.port = port;
    this.sender = sender;
    this.ioManager = new SerialInputOutputManager(port, this);
    ioManager.start();
  }

  public void send(byte[] data) throws IOException {
    this.port.write(data, WRITE_WAIT_MILLIS);
  }

  public void onNewData(byte[] data) {
      WritableMap event = Arguments.createMap();
      String hex = SerialportBluetoothModule.bytesToHex(data);
      event.putInt("deviceId", this.deviceId);
      event.putString("data", hex);
      sender.sendEvent(DataReceivedEvent, event);
  }

  public void onRunError(Exception e) {
      // TODO: implement
  }

  public void close() {
    if (closed) {
        return;
    }

    if(ioManager != null) {
        ioManager.setListener(null);
        ioManager.stop();
    }

    this.closed = true;
    try {
        port.close();
    } catch (IOException e) {
        e.printStackTrace();
    }
  }


  public int getDeviceId() {
      return deviceId;
  }
}
