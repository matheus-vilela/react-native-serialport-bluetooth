package com.serialportbluetooth;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;

public class SerialportDevice implements SerialInputOutputManager.Listener {
  private static final String DataReceivedEvent = "serialportDataReceived";

  private int WRITE_WAIT_MILLIS = 200;
  private int READ_WAIT_MILLIS = 200;
  private int deviceId;
  private UsbSerialPort port;
  private EventSender sender;
  private boolean closed = false;
  private SerialInputOutputManager ioManager;

  public SerialportDevice(int deviceId, UsbSerialPort port,int readWaitMillis, int writeWaitMillis, EventSender sender) {
    this.deviceId = deviceId;
    this.port = port;
    this.sender = sender;
    this.ioManager = new SerialInputOutputManager(port, this);
    this.READ_WAIT_MILLIS = readWaitMillis;
    this.WRITE_WAIT_MILLIS = writeWaitMillis;
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
