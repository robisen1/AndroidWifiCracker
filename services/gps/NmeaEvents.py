#!/usr/bin/env python

import NmeaParser
import NmeaSentences

class NmeaEventListener:
    """A base class for any class that wishes to receive NMEA events"""
    
    def gpggaEvent(self, gpgga):
        """Method called when a GPGGA NMEA sentence is encountered
        
        Arguments:
        gpgga -- the GpggaSentence that was encountered
        """
        pass
        
    def invalidNmeaSentenceEvent(self, e):
        """Method called when an invalid NMEA sentence is encountered by the 
        parser
        
        Arguments:
        e -- the NmeaParser.InvalidNmeaSentence exception that was thrown
        """
        pass
        
    def invalidGpggaSentenceEvent(self, e):
        """Method called when an invalid GPGGA sentence is encountered

        Arguments:
        e -- the NmeaParser.InvalidGPGGASentence exception that was thrown
        """
        pass
        
        
class NmeaEventPrinter(NmeaEventListener):
    """A class that prints out NMEA events as they are received"""
    
    def __init__(self):
        """Construct a new NmeaEventPrinter instance"""
        self.__gpggaEventsReceived = 0
        self.__invalidNmeaSentenceEventsReceived = 0
        self.__invalidGpggaSentenceEventsReceived = 0
    
    def gpggaEvent(self, gpgga):
        """Method called when a GPGGA NMEA sentence is encountered
        
        Arguments:
        gpgga -- the GpggaSentence that was encountered
        """
        self.__gpggaEventsReceived += 1
        print "\nGPGGA sentence #%d received\n" % (self.__gpggaEventsReceived)
        print "\t%s\n" % (gpgga)
        
    def invalidNmeaSentenceEvent(self, e):
        """Method called when an invalid NMEA sentence is encountered by the 
        parser

        Arguments:
        e -- the NmeaParser.InvalidNmeaSentence exception that was thrown
        """
        self.__invalidNmeaSentenceEventsReceived += 1
        print "\nInvalid NMEA sentence #%d received\n" % \
              (self.__invalidNmeaSentenceEventsReceived)
        print "\t%s\n" % (e)

    def invalidGpggaSentenceEvent(self, e):
        """Method called when an invalid GPGGA sentence is encountered

        Arguments:
        e -- the NmeaSentences.InvalidGPGGASentence exception that was thrown
        """
        self.__invalidGpggaSentenceEventsReceived += 1
        print "\nInvalid GPGGA sentence #%d received\n" % \
              (self.__invalidGpggaSentenceEventsReceived)
        print "\t%s\n" % (e)
        

class NmeaEventSource:
    """A base class for any class that wishes to generate NMEA events"""
    
    def __init__(self):
        """Construct a new NmeaEventSource instance"""
        self.__listeners = []
    
    def registerListener(self, listener):
        """Register an NmeaEventListener so that it receives all future NMEA 
        events
        
        Arguments:
        listener -- the listener to be registered
        """
        self.__listeners.append(listener)
        
    def unregisterListener(self, listener):
        """Unregister an NmeaEventListener so that it no longer receives NMEA 
        events
        
        Arguments:
        listener -- the listener to be unregistered
        """
        self.__listeners.remove(listener)
        
    def sendGpggaEvent(self, gpgga):
        """Method called to send a GPGGA event to all listeners
        
        Arguments:
        event -- the GPGGASentence that was encountered
        """
        for listener in self.__listeners:
            listener.gpggaEvent(gpgga)
            
    def sendInvalidNmeaSentenceEvent(self, e):
        """Method called to send an invalid NMEA sentence event to all 
        listeners
        
        Arguments:
        e -- the NmeaParser.InvalidNmeaSentence exception that was thrown
        """
        for listener in self.__listeners:
            listener.invalidNmeaSentenceEvent(e)
            
    def sendInvalidGpggaSentenceEvent(self, e):
        """Method called to send an invalid GPGGA sentence event to all 
        listeners

        Arguments:
        e -- the NmeaSentences.InvalidGpggaSentence exception that was thrown
        """
        for listener in self.__listeners:
            listener.invalidGpggaSentenceEvent(e)
