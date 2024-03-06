import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-serialport-bluetooth' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo managed workflow\n';

export interface Device {
  readonly name: string;
  readonly type: 'bluetooth' | 'usb';
  readonly deviceId: number;
  readonly vendorId: number;
  readonly productId: number;
}

interface SerialportBluetoothAPI {
  list(): Promise<Device[]>;
  tryRequestPermission(deviceId: number): Promise<number>;
  hasPermission(deviceId: number): Promise<boolean>;
  open(
    deviceId: number,
    baudRate: number,
    dataBits: number,
    stopBits: number,
    parity: number,
    readWaitMillis: number,
    writeWaitMillis: number
  ): Promise<number>;
  send(deviceId: number, hexStr: string): Promise<null>;
  close(deviceId: number): Promise<null>;
  readRfidCard(): Promise<string>;
}

const SerialportBluetooth: SerialportBluetoothAPI =
  NativeModules.SerialportBluetooth
    ? NativeModules.SerialportBluetooth
    : new Proxy(
        {},
        {
          get() {
            throw new Error(LINKING_ERROR);
          },
        }
      );

export default SerialportBluetooth;
