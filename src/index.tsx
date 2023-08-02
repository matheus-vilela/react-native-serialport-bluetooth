import { NativeEventEmitter, NativeModules, Platform } from 'react-native';
import Connect from './connect';
import SerialportBluetooth, { type Device } from './native_module';

export type { EventData, Listener } from './connect';
export type { Connect, Device };

const eventEmitter = new NativeEventEmitter(NativeModules.SerialportBluetooth);

export interface OpenOptions {
  baudRate: number;
  parity: Parity;
  dataBits: number;
  stopBits: number;
}

export enum Parity {
  None = 0,
  Odd,
  Even,
  Mark,
  Space,
}

export interface Manager {
  list(): Promise<Device[]>;
  tryRequestPermission(deviceId: number): Promise<boolean>;
  hasPermission(deviceId: number): Promise<boolean>;
  open(deviceId: number, options: OpenOptions): Promise<Connect>;
}

const defaultManager: Manager = {
  list(): Promise<Device[]> {
    return SerialportBluetooth.list();
  },

  async tryRequestPermission(deviceId: number): Promise<boolean> {
    const result = await SerialportBluetooth.tryRequestPermission(deviceId);
    return result === 1;
  },

  hasPermission(deviceId: number): Promise<boolean> {
    return SerialportBluetooth.hasPermission(deviceId);
  },

  async open(deviceId: number, options: OpenOptions): Promise<Connect> {
    await SerialportBluetooth.open(
      deviceId,
      options.baudRate,
      options.dataBits,
      options.stopBits,
      options.parity
    );
    return new Connect(deviceId, eventEmitter);
  },
};

export const SerialBluetoothManager: Manager =
  Platform.OS === 'android'
    ? defaultManager
    : (new Proxy(
        {},
        {
          get() {
            return () => {
              throw new Error(`Not support ${Platform.OS}`);
            };
          },
        }
      ) as Manager);
