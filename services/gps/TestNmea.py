#!/usr/bin/env python

import unittest
import NmeaSentences
import NmeaParser
import datetime
from Utc import Utc

class TestNmeaSentence(unittest.TestCase):
    
    def setUp(self):
        self.__nmea = NmeaSentences.NmeaSentence("XYZ")
        
    def test_getType(self):
        self.assertEqual(self.__nmea.getType(), "XYZ")


class TestGpggaSentence(unittest.TestCase):
    
    def setUp(self):
        sentence1 = "GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,"
        self.__gpgga1 = NmeaSentences.GpggaSentence( sentence1.split(',') )
        self.__sentence2 = "GPXXX,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,"
        self.__sentence3 = "GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,"
        
    def test_getType(self):
        self.assertEqual(self.__gpgga1.getType(), "GPGGA")
        
    def test_getTime(self):
        dt = datetime.datetime.now( tz = Utc() )
        dt = dt.replace(hour = 12, minute = 35, second = 19, microsecond = 0)
        self.assertEqual(self.__gpgga1.getTime(), dt)
        
    def test_getLatitude(self):
        self.assertAlmostEqual(self.__gpgga1.getLatitude(), 48.0 + 7.038 / 60.0)
        
    def test_getLongitude(self):
        self.assertAlmostEqual(self.__gpgga1.getLongitude(), 11.0 + 31.000 / 60.0)
        
    def test_getQuality(self):
        self.assertEqual(self.__gpgga1.getQuality(), 1)
        
    def test_getNumSatellites(self):
        self.assertEqual(self.__gpgga1.getNumSatellites(), 8)
        
    def test_getHorizontalDop(self):
        self.assertAlmostEqual(self.__gpgga1.getHorizontalDop(), 0.9)
        
    def test_getAltitude(self):
        self.assertAlmostEqual(self.__gpgga1.getAltitude(), 545.4)
        
    def test_getAltitudeUnits(self):
        self.assertEqual(self.__gpgga1.getAltitudeUnits(), "M")
        
    def test_getGeoidHeight(self):
        self.assertAlmostEqual(self.__gpgga1.getGeoidHeight(), 46.9)
        
    def test_getGeoidHeightUnits(self):
        self.assertEqual(self.__gpgga1.getGeoidHeightUnits(), "M")
        
    def test_getSecondsSinceLastDgpsUpdate(self):
        self.assertEqual(self.__gpgga1.getSecondsSinceLastDgpsUpdate(), 0)
        
    def test_getDgpsStationId(self):
        self.assertEqual(self.__gpgga1.getDgpsStationId(), "")
        
    def test_GpggaSentence1(self):
        self.assertRaises(NmeaSentences.InvalidGpggaSentence, NmeaSentences.GpggaSentence, self.__sentence2)
        
    def test_GpggaSentence2(self):
        self.assertRaises(NmeaSentences.InvalidGpggaSentence, NmeaSentences.GpggaSentence, self.__sentence3)
        
        
class TestNmeaParser(unittest.TestCase):
    
    def setUp(self):
        self.__gpggaRaw1 = "$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47\r\n"
        self.__gpgga1 = NmeaParser.NmeaParser.Parse(self.__gpggaRaw1)
        self.__gpggaRaw2 = "GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47\r\n"
        self.__gpggaRaw3 = "$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47\n\n"
        self.__gpggaRaw4 = "$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47\r\r"
        self.__gpggaRaw5 = "$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,447\r\n"
        self.__gpggaRaw6 = "$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*46\r\n"
        
    def test_Parse1(self):
        self.assertEqual(self.__gpgga1.getType(), "GPGGA")
        dt = datetime.datetime.now( tz = Utc() )
        dt = dt.replace(hour = 12, minute = 35, second = 19, microsecond = 0)
        self.assertEqual(self.__gpgga1.getTime(), dt)
        self.assertAlmostEqual(self.__gpgga1.getLatitude(), 48.0 + 7.038 / 60.0)
        self.assertAlmostEqual(self.__gpgga1.getLongitude(), 11.0 + 31.000 / 60.0)
        self.assertEqual(self.__gpgga1.getQuality(), 1)
        self.assertEqual(self.__gpgga1.getNumSatellites(), 8)
        self.assertAlmostEqual(self.__gpgga1.getHorizontalDop(), 0.9)
        self.assertAlmostEqual(self.__gpgga1.getAltitude(), 545.4)
        self.assertEqual(self.__gpgga1.getAltitudeUnits(), "M")
        self.assertAlmostEqual(self.__gpgga1.getGeoidHeight(), 46.9)
        self.assertEqual(self.__gpgga1.getGeoidHeightUnits(), "M")
        self.assertEqual(self.__gpgga1.getSecondsSinceLastDgpsUpdate(), 0)
        self.assertEqual(self.__gpgga1.getDgpsStationId(), "")
        
    def test_Parse2(self):
        self.assertRaises(NmeaParser.InvalidNmeaSentence, NmeaParser.NmeaParser.Parse, self.__gpggaRaw2)
        
    def test_Parse3(self):
        self.assertRaises(NmeaParser.InvalidNmeaSentence, NmeaParser.NmeaParser.Parse, self.__gpggaRaw3)
        
    def test_Parse4(self):
        self.assertRaises(NmeaParser.InvalidNmeaSentence, NmeaParser.NmeaParser.Parse, self.__gpggaRaw4)
        
    def test_Parse5(self):
        self.assertRaises(NmeaParser.InvalidNmeaSentence, NmeaParser.NmeaParser.Parse, self.__gpggaRaw5)
    
    def test_Parse6(self):
        self.assertRaises(NmeaParser.InvalidNmeaSentence, NmeaParser.NmeaParser.Parse, self.__gpggaRaw6)


if __name__ == '__main__':
	unittest.main()
	