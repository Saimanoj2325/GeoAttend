package com.geoattend.utils;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class FaceRecognitionProcessorTest {

    @Test
    public void testCalculateDistance_identicalVectors() {
        float[] v1 = {1.0f, 2.0f, 3.0f};
        float[] v2 = {1.0f, 2.0f, 3.0f};
        double distance = FaceRecognitionProcessor.calculateDistance(v1, v2);
        assertEquals(0.0, distance, 0.0001);
    }

    @Test
    public void testCalculateDistance_differentVectors() {
        float[] v1 = {1.0f, 0.0f, 0.0f};
        float[] v2 = {0.0f, 1.0f, 0.0f};
        // sqrt((1-0)^2 + (0-1)^2 + (0-0)^2) = sqrt(1 + 1) = sqrt(2) approx 1.414
        double distance = FaceRecognitionProcessor.calculateDistance(v1, v2);
        assertEquals(Math.sqrt(2), distance, 0.0001);
    }

    @Test
    public void testCalculateDistance_nullInput() {
        assertEquals(Double.MAX_VALUE, FaceRecognitionProcessor.calculateDistance(null, new float[3]), 0.0001);
        assertEquals(Double.MAX_VALUE, FaceRecognitionProcessor.calculateDistance(new float[3], null), 0.0001);
    }

    @Test
    public void testCalculateDistance_mismatchedLength() {
        float[] v1 = {1.0f, 2.0f};
        float[] v2 = {1.0f, 2.0f, 3.0f};
        assertEquals(Double.MAX_VALUE, FaceRecognitionProcessor.calculateDistance(v1, v2), 0.0001);
    }
}
