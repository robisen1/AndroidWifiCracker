#!/usr/bin/env python

import time
from NmeaEvents import NmeaEventPrinter
from NmeaSerialGps import NmeaSerialGps

def main():
    try:
        gps = NmeaSerialGps('/dev/ttyUSB0')
        printer = NmeaEventPrinter()

        gps.registerListener(printer)
        gps.start()
        
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        gps.stop()
        gps.unregisterListener(printer)


if __name__ == '__main__':
    main()