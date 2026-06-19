package com.geoattend.model;

import com.google.firebase.Timestamp;
import org.junit.Test;
import static org.junit.Assert.*;

public class AttendanceRecordTest {

    @Test
    public void testAttendanceRecordProperties() {
        Timestamp now = Timestamp.now();
        AttendanceRecord record = new AttendanceRecord(
                "id123", "user456", "John Doe", now, "IN",
                "office789", "Main Office", "http://photo.url"
        );

        assertEquals("id123", record.getId());
        assertEquals("user456", record.getUserId());
        assertEquals("John Doe", record.getUserName());
        assertEquals(now, record.getTimestamp());
        assertEquals("IN", record.getType());
        assertEquals("office789", record.getGeofenceId());
        assertEquals("Main Office", record.getGeofenceName());
    }

    @Test
    public void testSecurityFields() {
        AttendanceRecord record = new AttendanceRecord();
        record.setRiskScore(85);
        record.setStatus("FLAGGED");
        record.setMockLocation(true);

        assertEquals(85, record.getRiskScore());
        assertEquals("FLAGGED", record.getStatus());
        assertTrue(record.isMockLocation());
    }
}
