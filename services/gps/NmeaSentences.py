#!/usr/bin/env python

import datetime
import math
from Utc import Utc

"""Classes representing NMEA sentence types"""

class NmeaSentence:
    """A base class for all NMEA sentences"""
    
    def __init__(self, type):
        """Create a new NmeaSentence instance
        
        Arguments:
        type -- a string containing the NMEA sentence type (e.g. GPGGA)
        """
        self.__type = type
        
    def __str__(self):
        '''Return a string representation of the object'''
        return "NmeaSentence{type = %s}" % ( self.getType() )
        
    def getType(self):
        """Return the NMEA message type"""
        return self.__type
        
        
class InvalidGpggaSentence(ValueError):
    """Exception that is raised when an invalid GPGGA sentence is encountered"""
    
    def __init__(self, args):
        ValueError.__init__(self, args)
        
        
class GpggaSentence(NmeaSentence):
    """GPGGA NMEA sentence"""
    
    # The NMEA message type corresponding to GPGGA
    __Type = "GPGGA"
    
    # The number of expected fields in a GPGGA message
    __NumFields = 15
    
    def __init__(self, fields):
        """Create a new GPGGA NMEA sentence instance
        
        Arguments:
        fields -- a list containing the GPGGA NMEA sentence fields
        """
        if (len(fields) != GpggaSentence.__NumFields):
            raise InvalidGpggaSentence, "Incorrect number of fields"
        
        if (fields[0] != GpggaSentence.__Type):
            raise InvalidGpggaSentence, "Type field does not contain GPGGA"
        
        NmeaSentence.__init__(self, GpggaSentence.__Type)
        
        # UTC time the fix was taken.  Format is hhmmss.
        self.__setTime( fields[1] )
        
        # Latitude variables
        self.__setLatitude( fields[2], fields[3] )
        
        # Longitude variables
        self.__setLongitude( fields[4], fields[5] )
        
        # The fix quality.  Possible values are:
        #  0 - invalid
        #  1 - GPS fix
        #  2 - DGPS fix
        #  3 - PPS fix
        #  4 - Real time kinematic (RTK)
        #  5 - Float RTK
        #  6 - Estimated (dead reckoning)
        #  7 - Manual input mode
        #  8 - Simulation mode
        try:
            self.__quality = GpggaSentence.__ToInt( fields[6] )
        except ValueError:
            raise InvalidGpggaSentence("Quality field is invalid")
        
        # The number of satellites being tracked
        try:
            self.__numSatellites = GpggaSentence.__ToInt( fields[7] )
        except ValueError:
            raise InvalidGpggaSentence("Number of satellites field is invalid")
        
        # Horizontal dilution of precision
        try:
            self.__hdop = GpggaSentence.__ToFloat( fields[8] )
        except ValueError:
            raise InvalidGpggaSentence("HDOP field is invalid")
        
        # Altitude above mean sea level
        try:
            self.__altitude = GpggaSentence.__ToFloat( fields[9] )
        except ValueError:
            raise InvalidGpggaSentence("Altitude field is invalid")    
        # Altitude units
        self.__altitudeUnits = fields[10]
        
        # Height of the geoid above mean sea level
        try:
            self.__geoidHeight = GpggaSentence.__ToFloat( fields[11] )
        except ValueError:
            raise InvalidGpggaSentence("Geoid height field is invalid")
        # Geoid height units
        self.__geoidHeightUnits = fields[12]
        
        # Time in seconds since the latest DGPS update
        try:
            self.__secondsSinceLastDgpsUpdate = \
                GpggaSentence.__ToInt( fields[13] )
        except ValueError:
            raise InvalidGpggaSentence("Seconds since last DGPS update " + \
                                       "field is invalid")
        # DGPS station ID number
        self.__dgpsStationId = fields[14]
        
    def __str__(self):
        '''Return a string representation of the object'''
        formatString = "GpggaSentence{time = %s, latitude = %g, " + \
                       "longitude = %g, quality = %d, numSatellites = %d, " + \
                       "hDop = %g, altitude = %g %s, geoidHeight = %g %s, " + \
                       "timeSinceLastDgpsUpdate = %s, dgpsStationId = %s}"
        formatArgs = ( self.getTime().isoformat(), self.getLatitude(), 
                       self.getLongitude(), self.getQuality(), 
                       self.getNumSatellites(), self.getHorizontalDop(), 
                       self.getAltitude(), self.getAltitudeUnits(), 
                       self.getGeoidHeight(), self.getGeoidHeightUnits(), 
                       self.getSecondsSinceLastDgpsUpdate(), 
                       self.getDgpsStationId() )
               
        return formatString % formatArgs
        
    def getTime(self):
        """Return the UTC time the position fix was taken"""
        return self.__time
        
    def getLatitude(self):
        """Return the latitude of the fix"""
        return self.__latitude
        
    def getLongitude(self):
        """Return the longitude of the fix"""
        return self.__longitude
        
    def getQuality(self):
        """Return an integer denoting the quality of the fix.  Possible values 
        are:
        0 - invalid
        1 - GPS fix
        2 - DGPS fix
        3 - PPS fix
        4 - Real time kinematic (RTK)
        5 - Float RTK
        6 - Estimated (dead reckoning)
        7 - Manual input mode
        8 - Simulation mode
        """
        return self.__quality
        
    def getNumSatellites(self):
        """Return an integer denoting the number of satellites tracked for 
        the fix.
        """
        return self.__numSatellites
        
    def getHorizontalDop(self):
        """Return the horizontal dilution of position (DOP)"""
        return self.__hdop
        
    def getAltitude(self):
        """Return the altitude above mean sea level (AMSL)"""
        return self.__altitude
        
    def getAltitudeUnits(self):
        """Return a string denoting the units of the altitude AMSL"""
        return self.__altitudeUnits
        
    def getGeoidHeight(self):
        """Return the height of the geoid above mean sea level (AMSL)"""
        return self.__geoidHeight

    def getGeoidHeightUnits(self):
        """Return a string denoting the units of the height of the geoid 
        AMSL
        """
        return self.__geoidHeightUnits
        
    def getSecondsSinceLastDgpsUpdate(self):
        """Return an integer denoting the time in seconds since the last DGPS 
        update, or None if the field is undefined
        """
        return self.__secondsSinceLastDgpsUpdate
        
    def getDgpsStationId(self):
        """Return a string denoting the DGPS station ID"""
        return self.__dgpsStationId
        
    @staticmethod
    def __ConvertLatOrLon(value):
        """Converts a latitude or longitude value in the form DDMM.MMM to a 
        proper floating point format
        """
        temp = GpggaSentence.__ToFloat(value)
            
        degreesAndMinutes = int(temp)
        
        degrees = degreesAndMinutes / 100
        
        minutes = degreesAndMinutes - degrees * 100
        minutes += temp - degreesAndMinutes
        
        ans = degrees + minutes / 60.0
        
        return ans
        
    @staticmethod
    def __ToInt(field):
        """Converts a string field to an integer.  If the string is empty then 
        a value of 0 is returned
        
        Arguments:
        field -- the field that is to be converted to an integer
        """
        if (field == ""):
            return 0
            
        return int(field)
        
    @staticmethod
    def __ToFloat(field):
        """Converts a string field to a floating point number.  If the string 
        is empty then a value of 0.0 is returned

        Arguments:
        field -- the field that is to be converted to a floating point number
        """
        if (field == ""):
            return 0.0

        return float(field)
        
    def __setTime(self, field):
        """Set the UTC time the position fix was taken
        
        Arguments:
        field -- the string value of the time field in the GPGGA sentence
        """
        try:
            time = GpggaSentence.__ToFloat(field)
        except ValueError, e:
            raise InvalidGpggaSentence("Time field invalid", e)

        hours = int(time // 10000)
        time -= hours * 10000

        minutes = int(time // 100)
        time -= minutes * 100

        seconds = int(time)
        time -= seconds

        microseconds = int(time * 1000000)

        self.__time = datetime.datetime.now( tz = Utc() ).replace(hour = hours, 
                        minute = minutes, second = seconds, 
                        microsecond = microseconds)
                        
    def __setLatitude(self, latField, nsField):
        """Set the latitude of the position fix
        
        Arguments:
        latField -- the string value of the latitude field in the GPGGA 
        sentence
        
        nsField -- the string value of the N/S field in the GPGGA sentence
        """
        try:
            self.__latitude = GpggaSentence.__ConvertLatOrLon(latField)
        except ValueError:
            raise InvalidGpggaSentence("Latitude field is invalid")

        if (nsField.upper() == "S"):
            self.__latitude *= -1.0
            
    def __setLongitude(self, lonField, ewField):
        """Set the latitude of the position fix

        Arguments:
        lonField -- the string value of the longitude field in the GPGGA 
        sentence

        ewField -- the string value of the E/W field in the GPGGA sentence
        """
        try:
            self.__longitude = GpggaSentence.__ConvertLatOrLon(lonField)
        except ValueError:
            raise InvalidGpggaSentence("Longitude field is invalid")

        if (ewField.upper() == "W"):
            self.__longitude *= -1.0
