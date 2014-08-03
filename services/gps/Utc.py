from datetime import timedelta
from datetime import tzinfo

class Utc(tzinfo):
    """Class representing a timezone corresponding to UTC"""

    def utcoffset(self, dt):
        return timedelta(0)

    def tzname(self, dt):
        return "UTC"

    def dst(self, dt):
        return timedelta(0)
