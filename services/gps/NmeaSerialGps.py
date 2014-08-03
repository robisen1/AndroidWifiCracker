#!/usr/bin/env python

from serial import Serial
from threading import Thread
import time
from NmeaEvents import NmeaEventSource
import NmeaParser
import NmeaSentences

class NmeaSerialGps(NmeaEventSource):
    """A NMEA GPS device that is connected via a serial connection"""
    
    def __init__(self, port, baudrate = 4800):
        """Create a new NMEA serial GPS
        
        Arguments:
        port -- the device that is the GPS serial port (i.e. /dev/ttyS0)
        
        baudrate -- the baud rate of the serial connection.  Set to 4800 by 
        default, since this is the baud rate specified by the NMEA 
        specification
        """
        NmeaEventSource.__init__(self)
        
        # Configure the serial port
        self.__serialPort = Serial(port = port, baudrate = baudrate)
        
        # Create a thread to read from the serial port
        self.__thread = Thread(target = self.__threadLoop, 
                               name = "NmeaSerialGps thread")
        
        # Set a variable that lets us know when we have been requested to stop 
        self.__stopRequested = False
        
    def __del__(self):
        """Destroy an NMEA serial GPS instance"""
        self.__serialPort.close()
        
    def start(self):
        """Start capturing data from the serial GPS device"""
        self.__stopRequested = False
        self.__thread.start()
        
    def stop(self):
        """Stop capturing data from the serial GPS device"""
        self.__stopRequested = True
        self.__thread.join()
        
    def __threadLoop(self):
        """Retrieve NMEA sentences from the device"""
        while not self.__stopRequested:
            nextSentence = self.__serialPort.readline()
            
            if (nextSentence != None):
                nmea = None
                try:
                    # nmea will be None if the latest message is a type we 
                    # don't understand
                    nmea = NmeaParser.NmeaParser.Parse(nextSentence)
                except NmeaParser.InvalidNmeaSentence, e:
                    # Send an invalid NMEA sentence event
                    self.sendInvalidNmeaSentenceEvent(e)
                except NmeaSentences.InvalidGpggaSentence, e:
                    # Send an invalid GPGGA sentence event
                    self.sendInvalidGpggaSentenceEvent(e)
                    
                if (nmea != None):
                    if isinstance(nmea, NmeaSentences.GpggaSentence):
                        # Send a GPGGA event
                        self.sendGpggaEvent(nmea)
