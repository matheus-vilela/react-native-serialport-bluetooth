import EventEmitter from 'react-native/Libraries/vendor/emitter/EventEmitter';
import SerialportBluetooth from './native_module';

const DataReceivedEvent = 'serialportDataReceived';

export interface EventData {
  deviceId: number;
  data: string;
}

export type Listener = (data: EventData) => void;

export default class Connect {
  deviceId: number;
  private eventEmitter: EventEmitter;
  private listeners: Listener[];
  buffer = '';

  constructor(deviceId: number, eventEmitter: EventEmitter) {
    this.deviceId = deviceId;
    this.eventEmitter = eventEmitter;
    this.listeners = [];
  }

  send(hexStr: string): Promise<null> {
    return SerialportBluetooth.send(this.deviceId, hexStr);
  }

  onReceived(listener: Listener) {
    const listenerProxy = (event: EventData) => {
      listener(event);
    };

    this.listeners.push(listenerProxy);
    return this.eventEmitter.addListener(DataReceivedEvent, listenerProxy);
  }

  close(): Promise<any> {
    this.eventEmitter.removeAllListeners(DataReceivedEvent);
    return SerialportBluetooth.close(this.deviceId);
  }
}
