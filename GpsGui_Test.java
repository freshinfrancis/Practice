package com.frp.app;

import org.junit.Test;
import static org.junit.Assert.*;

import swidgets.STextField;

import java.util.ArrayList;

public class GpsGui_Test {

    @Test
    public void testIsWithinRange() {
        // Test when the event is within the specified range
        GpsEvent withinRangeEvent = new GpsEvent("TestEvent", 0.0, 0.0, 0.0);
        assertTrue(GpsGui.isWithinRange(withinRangeEvent, -90, 90, -180, 180));

        // Test when the event is outside the specified range
        GpsEvent outsideRangeEvent = new GpsEvent("TestEvent", 100.0, 0.0, 0.0);
        assertFalse(GpsGui.isWithinRange(outsideRangeEvent, -90, 90, -180, 180));
    }

    @Test
    public void testCalculateDistance() {
        // Test the calculation of Cartesian distance between two events
        GpsEvent event1 = new GpsEvent("Event1", 0.0, 0.0, 0.0);
        GpsEvent event2 = new GpsEvent("Event2", 1.0, 1.0, 1.0);
        assertEquals(1.446686918445038, GpsGui.CalculateDistance(event1, event2), 0.0001);
    }

    @Test
    public void testUpdateDistanceLabel() {
        // Test updating the distance label based on a list of track events
        STextField distanceTracker = new STextField("");
        ArrayList<GpsEvent> trackEvents = new ArrayList<>();
        trackEvents.add(new GpsEvent("Event1", 0.0, 0.0, 0.0));
        trackEvents.add(new GpsEvent("Event2", 1.0, 1.0, 1.0));

        double totalDistance = GpsGui.updateDistanceLabel(distanceTracker, trackEvents, -90, 90, -180, 180);

        // Check if the total distance and the rounded distance label are correct
        assertEquals(1.446686918445038, totalDistance, 0.0001);
        assertEquals("1", distanceTracker.getText()); // Rounded up to the nearest integer meter
    }
}