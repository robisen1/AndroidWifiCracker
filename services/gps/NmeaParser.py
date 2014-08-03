#!/usr/bin/env python

import NmeaSentences

class InvalidNmeaSentence(ValueError):
    """Exception raised when an NMEA sentence is invalid"""
    
    def __init__(self, args=None):
        ValueError.__init__(self, args)


class NmeaParser:
    """Parser for NMEA sentences"""
    
    # The byte signifying the start of an NMEA sentence
    __NmeaStart = "$"
    
    # The bytes signifying the end of an NMEA sentence
    __NmeaEnd = "\r\n"
    
    # The NMEA sentence delimiter
    __Delimiter = ","
    
    @staticmethod
    def Parse(sentence):
        """Parse an NMEA sentence, returning an NmeaSentence class of the 
        appropriate type
        
        Arguments:
        sentence -- a string containing the NMEA sentence to be parsed
        """
        # Make sure the NMEA sentence begins with $
        if not sentence.startswith(NmeaParser.__NmeaStart):
            raise InvalidNmeaSentence, "NMEA sentence does not start with $"
            
        # Make sure the NMEA sentence ends with \r\n
        if not sentence.endswith(NmeaParser.__NmeaEnd):
            raise InvalidNmeaSentence, "NMEA sentence does not end with \r\n"
        
        # We want to drop the beginning $ as well as the ending \r\n
        sentence = sentence[1:-2]
        
        # Make sure there is an asterisk before the checksum
        if (sentence[-3] != "*"):
            raise InvalidNmeaSentence, "NMEA checksum is not preceeded by *"
        
        # The checksum consists of the last two (hex) characters
        checksum = int(sentence[-2:], 16)
        
        # Now we can drop the checksum and its asterisk as well
        sentence = sentence[:-3]
        
        # Make sure the checksum is valid
        if ( checksum != NmeaParser.__ComputeXorChecksum(sentence) ):
            raise InvalidNmeaSentence, "Invalid checksum"
        
        fields = sentence.split(NmeaParser.__Delimiter)
        
        if (fields[0] == "GPGGA"):
            return NmeaSentences.GpggaSentence(fields)
        else:
            # Skip any other messages
            pass
        
    @staticmethod
    def __ComputeXorChecksum(string):
        """Return the XOR checksum for the given string"""
        checksum = 0
        for c in string:
            checksum ^= ord(c)
            
        return checksum
