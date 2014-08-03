#!/usr/bin/env python

import time
from pysqlite2 import dbapi2 as sqlite
from NmeaEvents import NmeaEventListener
from NmeaSerialGps import NmeaSerialGps

class NmeaDBListener:
    """A class that receives NMEA events and writes them to a sqlite 
    database
    """
    
    def __init__(self, dbName):
        """Construct a new NmeaDBListener instance"""
        self.__dbName = dbName
        self.__executeSql("CREATE TABLE IF NOT EXISTS gpgga (" + \
                          "time integer, latitude real, longitude real, " + \
                          "quality integer, num_satellites integer, " + \
                          "hdop real, altitude real, altitude_units text, " + \
                          "geoid_height real, geoid_height_units text)" )
        
    def gpggaEvent(self, gpgga):
        """Method called when a GPGGA NMEA sentence is encountered
        
        Arguments:
        gpgga -- the GpggaSentence that was encountered
        """
        if (gpgga.getQuality() != 0):
            sqlString = "INSERT INTO gpgga VALUES (\'%s\', %g, %g, %d, " + \
                        "%d, %g, %g, \'%s\', %g, \'%s\')"
            sqlArgs = ( gpgga.getTime().isoformat(), gpgga.getLatitude(), 
                        gpgga.getLongitude(), gpgga.getQuality(), 
                        gpgga.getNumSatellites(), gpgga.getHorizontalDop(), 
                        gpgga.getAltitude(), gpgga.getAltitudeUnits(), 
                        gpgga.getGeoidHeight(), gpgga.getGeoidHeightUnits() )
                 
            self.__executeSql(sqlString % sqlArgs)
        
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
        
    def __executeSql(self, sql):
        """Execute the SQL in the string sql.  We have to reconnect to the 
        database every time we write to it because Connection and Cursor 
        objects cannot be shared across threads.
        
        Arguments:
        sql -- the SQL statement to be executed
        """
        connection = sqlite.connect(self.__dbName)
        cursor = connection.cursor()
        cursor.execute(sql)
        connection.commit()
        connection.close()

def main():
    try:
        gps = NmeaSerialGps('/dev/ttyUSB0')
        listener = NmeaDBListener("/gps/gps.db")

        gps.registerListener(listener)
        gps.start()
        
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        gps.stop()
        gps.unregisterListener(listener)


if __name__ == '__main__':
    main()
