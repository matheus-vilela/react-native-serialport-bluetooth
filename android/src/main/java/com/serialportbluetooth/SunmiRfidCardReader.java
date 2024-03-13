package com.serialportbluetooth;

import android.os.Bundle;
import android.util.Log;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.sunmi.pay.hardware.aidl.AidlConstants;
import com.sunmi.pay.hardware.aidlv2.readcard.CheckCardCallbackV2;
import com.sunmi.pay.hardware.aidlv2.readcard.ReadCardOptV2;
import sunmi.paylib.SunmiPayKernel;
import sunmi.paylib.SunmiPayKernel.ConnectCallback;

public class SunmiRfidCardReader {
  private final String tag = "SunmiRfidCardReader";
  private final ReactApplicationContext reactContext;
  private final SunmiPayKernel payKernel;

  public SunmiRfidCardReader(ReactApplicationContext reactContext) {
    this.reactContext = reactContext;
    payKernel = SunmiPayKernel.getInstance();
  }

  public void bindService() {
    payKernel.initPaySDK(reactContext, new ConnectCallback() {
      @Override
      public void onConnectPaySDK() {}

      @Override
      public void onDisconnectPaySDK() {}
    });
  }

  public void searchCard(final Promise promise) {
    Log.d(tag, "Search card!");

    try {
      payKernel.mReadCardOptV2.cancelCheckCard();
      payKernel.mReadCardOptV2.checkCard(
        AidlConstants.CardType.MIFARE.getValue(),
        new CheckCardCallbackV2.Stub() {
          @Override
          public void findMagCard(Bundle bundle) {
            promise.reject("500", "findMagCard");
          }

          @Override
          public void findICCard(String s) {
            promise.reject("500", "findICCard");
          }

          @Override
          public void findRFCard(String s) {
            Log.d(tag, "(findRFCard) Serial Number: '" + s + "'");
            String rfid = resolveRFIDBySerialNumber(s);
            Log.d(tag, "RFID: " + rfid);
            promise.resolve(rfid);
          }

          @Override
          public void onError(int i, String s) {
            promise.reject("500", i + " - " + s);
          }

          @Override
          public void findICCardEx(Bundle bundle) {
            promise.reject("500", "findICCardEx");
          }

          @Override
          public void findRFCardEx(Bundle bundle) {
            promise.reject("500", "findRFCardEx");
          }

          @Override
          public void onErrorEx(Bundle bundle) {
            promise.reject("500", "onErrorEx");
          }
        },
        0
      );
    } catch (Exception e) {
      e.printStackTrace();
      promise.reject(e);
    }
  }

  private String resolveRFIDBySerialNumber(String value) {
    int length = value.length();

    StringBuilder n = new StringBuilder();
    n.append(value.substring(0, 2)).append("-");

    if (length >= 4) {
      n.append(value.substring(2, 4));
      if (length > 4) n.append("-");
    }

    if (length >= 6) {
      n.append(value.substring(4, 6));
      if (length > 6) n.append("-");
    }

    if (length >= 8) {
      n.append(value.substring(6, 8));
      if (length > 8) n.append("-");
    }

    if (length >= 10) {
      n.append(value.substring(6, 10));
    }

    String[] split = n.toString().split("-");
    int splitSize = split.length;
    StringBuilder nova = new StringBuilder();

    switch (splitSize) {
        case 4:
          nova.append(split[3]).append(split[2]).append(split[1]).append(split[0]);
          break;
        case 3:
          nova.append(split[2]).append(split[1]).append(split[0]);
          break;
        case 2:
          nova.append(split[1]).append(split[0]);
          break;
        default:
          break;
    }

    String hexResult = nova.toString();
    if (hexResult == null) hexResult = "FFFFFF";
    long converted = Long.parseLong(hexResult, 16);

    String id = Long.toString(converted);

    if (id.length() < 10) {
      id = "0" + converted;
      if (id.length() < 10) id = "00" + converted;
      if (id.length() < 10) id = "000" + converted;
    }

    return id;
  }
}
