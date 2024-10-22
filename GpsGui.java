import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import swidgets.*;
import nz.sodium.*;
import java.util.ArrayList;
import java.lang.Math;

/**
 * The GpsGui class represents the main graphical user interface for displaying
 * information from GPS trackers.
 */
public class GpsGui {
    
    /**
     * Checks if a given GPS event is within the specified latitude and longitude range.
     *
     * @param event    The GPS event to check.
     * @param latLower The lower limit for latitude.
     * @param latUpper The upper limit for latitude.
     * @param lonLower The lower limit for longitude.
     * @param lonUpper The upper limit for longitude.
     * @return True if the event is within the specified range, false otherwise.
     */
    public static boolean isWithinRange(GpsEvent event, double latLower, double latUpper, double lonLower, double lonUpper) {
        return event.latitude >= latLower && event.latitude <= latUpper &&
               event.longitude >= lonLower && event.longitude <= lonUpper;
    }

    /**
     * Calculates the Cartesian distance between two GpsEvent points.
     * 
     * @param evenT2 The first GpsEvent representing the starting point.
     * @param evenT3 The second GpsEvent representing the ending point.
     * @return The calculated distance between the two points as a Double value.
     */
    public static Double CalculateDistance(GpsEvent evenT2, GpsEvent evenT3){
        Double lat = evenT3.latitude - evenT2.latitude;
        Double lon = evenT3.longitude - evenT2.longitude;
        Double alt = 0.3048*(evenT3.altitude - evenT2.altitude); 
        Double distTraveled = Math.sqrt(lat*lat + lon*lon + alt*alt);
        return distTraveled;
    }
    
    /**
     * Updates the distance label and calculates the total distance
     * considering events within the specified latitude and longitude range.
     *
     * @param distanceTracker The text field for displaying the distance.
     * @param trackEvents     The list of GPS events.
     * @param latLower        The lower limit for latitude.
     * @param latUpper        The upper limit for latitude.
     * @param lonLower        The lower limit for longitude.
     * @param lonUpper        The upper limit for longitude.
     * @return The total distance calculated for the last 5 minutes.
     */
    public static double updateDistanceLabel(STextField distanceTracker, ArrayList<GpsEvent> trackEvents, double latLower, double latUpper, double lonLower, double lonUpper) {
        double totalDistance = 0.0;
    
        for (int i = 0; i < trackEvents.size() - 1; i++) {
            GpsEvent event1 = trackEvents.get(i);
            GpsEvent event2 = trackEvents.get(i + 1);
    
            if (isWithinRange(event1, latLower, latUpper, lonLower, lonUpper) && isWithinRange(event2, latLower, latUpper, lonLower, lonUpper)) {
                totalDistance += CalculateDistance(event1, event2);
            }
        }
        long roundedDistance = Math.round(totalDistance); 
        distanceTracker.setText(Long.toString(roundedDistance));
        return totalDistance;
    }

    // Text fields for filter values
    private static STextField latLowerLimit = new STextField("-90");
    private static STextField latUpperLimit = new STextField("90");
    private static STextField lonLowerLimit = new STextField("-180");
    private static STextField lonUpperLimit = new STextField("180");

    /**
     * The main method initialises the GUI, sets up event streams, and updates
     * tracker information dynamically.
     */
    public static void main(String[] args){

        // Create the main frame
        JFrame frame = new JFrame("Tracker Information"); 
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(3, 4));

        // Panels for data display
        JPanel firstPanel = new JPanel();
        firstPanel.setLayout(new FlowLayout());
        firstPanel.setPreferredSize(new Dimension(250, 120));
        firstPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 3)); // Gray border

        JPanel secondPanel = new JPanel();
        secondPanel.setLayout(new FlowLayout());
        secondPanel.setPreferredSize(new Dimension(250, 120));
        secondPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 3)); // Gray border

        JPanel thirdPanel = new JPanel();
        thirdPanel.setLayout(new FlowLayout());
        thirdPanel.setPreferredSize(new Dimension(250, 120));
        thirdPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 3)); // Gray border

        JPanel fourthPanel = new JPanel();
        fourthPanel.setLayout(new FlowLayout()); 
        fourthPanel.setPreferredSize(new Dimension(250, 120));
        fourthPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 3)); // Gray border

        JPanel fifthPanel = new JPanel();
        fifthPanel.setLayout(new FlowLayout());
        fifthPanel.setPreferredSize(new Dimension(250, 120));
        fifthPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 3)); // Gray border

        JPanel sixthPanel = new JPanel();
        sixthPanel.setLayout(new FlowLayout());
        sixthPanel.setPreferredSize(new Dimension(250, 120));
        sixthPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 3)); // Gray border

        JPanel seventhPanel = new JPanel();
        seventhPanel.setLayout(new FlowLayout());
        seventhPanel.setPreferredSize(new Dimension(250, 120));
        seventhPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 3)); // Gray border

        JPanel eighthPanel = new JPanel();
        eighthPanel.setLayout(new FlowLayout());
        eighthPanel.setPreferredSize(new Dimension(250, 120));
        eighthPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 3)); // Gray border

        JPanel ninethPanel = new JPanel();
        ninethPanel.setLayout(new FlowLayout());
        ninethPanel.setPreferredSize(new Dimension(250, 120));
        ninethPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 3)); // Gray border

        JPanel tenthPanel = new JPanel();
        tenthPanel.setLayout(new FlowLayout());
        tenthPanel.setPreferredSize(new Dimension(250, 120));
        tenthPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 3)); // Gray border

        JPanel incomingPanel = new JPanel();
        incomingPanel.setLayout(new FlowLayout());
        incomingPanel.setPreferredSize(new Dimension(250, 120));
        incomingPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 3));

        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new FlowLayout());

        Cell<String> latLowerWord = new Cell<>("Latitude Lower:");
        SLabel latLowerLabel = new SLabel(latLowerWord);

        Cell<String>  latUpperWord = new Cell<>("Latitude Upper:");
        SLabel latUpperLabel = new SLabel(latUpperWord);

        Cell<String> lonLowerWord = new Cell<>("Longitude Lower:");
        SLabel lonLowerLabel = new SLabel(lonLowerWord);

        Cell<String> lonUpperUpper = new Cell<>("Longitude Upper:");
        SLabel lonUpperLabel = new SLabel(lonUpperUpper);

        JPanel filterButtonPanel = new JPanel();
        filterButtonPanel.setLayout(new FlowLayout());

        // Adding panels to the frame
        frame.add(firstPanel);
        frame.add(secondPanel);
        frame.add(thirdPanel);
        frame.add(fourthPanel);
        frame.add(fifthPanel);
        frame.add(sixthPanel);
        frame.add(seventhPanel);
        frame.add(eighthPanel);
        frame.add(ninethPanel);
        frame.add(tenthPanel);
        frame.add(incomingPanel);
        frame.add(filterPanel);
        frame.add(filterButtonPanel);

        // Setting default close operation and layout for the frame
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new FlowLayout());

        // Initialise the GPS Service
        GpsService serv = new GpsService();

        // Retrieve Event Streams
        Stream<GpsEvent>[] streams = serv.getEventStreams();

        // Define the array of GPS events for all trackers
        ArrayList<GpsEvent> trackEvents1 = new ArrayList<GpsEvent>();
        ArrayList<GpsEvent> trackEvents2 = new ArrayList<GpsEvent>();
        ArrayList<GpsEvent> trackEvents3 = new ArrayList<GpsEvent>();
        ArrayList<GpsEvent> trackEvents4 = new ArrayList<GpsEvent>();
        ArrayList<GpsEvent> trackEvents5 = new ArrayList<GpsEvent>();
        ArrayList<GpsEvent> trackEvents6 = new ArrayList<GpsEvent>();
        ArrayList<GpsEvent> trackEvents7 = new ArrayList<GpsEvent>();
        ArrayList<GpsEvent> trackEvents8 = new ArrayList<GpsEvent>();
        ArrayList<GpsEvent> trackEvents9 = new ArrayList<GpsEvent>();
        ArrayList<GpsEvent> trackEvents10 = new ArrayList<GpsEvent>();

        // Creating Swing components for tracker information
        STextField distanceTracker1 = new STextField("0");
        Cell<String> distanceCellTracker1 = distanceTracker1.sUserChanges.hold(distanceTracker1.getText());
        SLabel distanceLabelTracker1 = new SLabel(distanceCellTracker1);

        STextField distanceTracker2 = new STextField("0");
        Cell<String> distanceCellTracker2 = distanceTracker2.sUserChanges.hold(distanceTracker2.getText());
        SLabel distanceLabelTracker2 = new SLabel(distanceCellTracker2);

        STextField distanceTracker3 = new STextField("0");
        Cell<String> distanceCellTracker3 = distanceTracker3.sUserChanges.hold(distanceTracker3.getText());
        SLabel distanceLabelTracker3 = new SLabel(distanceCellTracker3);

        STextField distanceTracker4 = new STextField("0");
        Cell<String> distanceCellTracker4 = distanceTracker4.sUserChanges.hold(distanceTracker4.getText());
        SLabel distanceLabelTracker4 = new SLabel(distanceCellTracker4);

        STextField distanceTracker5 = new STextField("0");
        Cell<String> distanceCellTracker5 = distanceTracker5.sUserChanges.hold(distanceTracker5.getText());
        SLabel distanceLabelTracker5 = new SLabel(distanceCellTracker5);

        STextField distanceTracker6 = new STextField("0");
        Cell<String> distanceCellTracker6 = distanceTracker6.sUserChanges.hold(distanceTracker6.getText());
        SLabel distanceLabelTracker6 = new SLabel(distanceCellTracker6);

        STextField distanceTracker7 = new STextField("0");
        Cell<String> distanceCellTracker7 = distanceTracker7.sUserChanges.hold(distanceTracker7.getText());
        SLabel distanceLabelTracker7 = new SLabel(distanceCellTracker7);

        STextField distanceTracker8 = new STextField("0");
        Cell<String> distanceCellTracker8 = distanceTracker8.sUserChanges.hold(distanceTracker8.getText());
        SLabel distanceLabelTracker8 = new SLabel(distanceCellTracker8);

        STextField distanceTracker9 = new STextField("0");
        Cell<String> distanceCellTracker9 = distanceTracker9.sUserChanges.hold(distanceTracker9.getText());
        SLabel distanceLabelTracker9 = new SLabel(distanceCellTracker9);

        STextField distanceTracker10 = new STextField("0");
        Cell<String> distanceCellTracker10 = distanceTracker10.sUserChanges.hold(distanceTracker10.getText());
        SLabel distanceLabelTracker10 = new SLabel(distanceCellTracker10);

        Cell<String> distWord = new Cell<>("| Distance Travelled:");
        SLabel distLabelTracker1= new SLabel(distWord);
        SLabel distLabelTracker2= new SLabel(distWord);
        SLabel distLabelTracker3= new SLabel(distWord);
        SLabel distLabelTracker4= new SLabel(distWord);
        SLabel distLabelTracker5= new SLabel(distWord);
        SLabel distLabelTracker6= new SLabel(distWord);
        SLabel distLabelTracker7= new SLabel(distWord);
        SLabel distLabelTracker8= new SLabel(distWord);
        SLabel distLabelTracker9= new SLabel(distWord);
        SLabel distLabelTracker10= new SLabel(distWord);
    

        // Creating the latitude labels for each tracker
        Cell<String> lat = new Cell<>("| Latitude:");
        SLabel latLabelTracker1 = new SLabel(lat);
        SLabel latLabelTracker2 = new SLabel(lat);
        SLabel latLabelTracker3 = new SLabel(lat);
        SLabel latLabelTracker4 = new SLabel(lat);
        SLabel latLabelTracker5 = new SLabel(lat);
        SLabel latLabelTracker6 = new SLabel(lat);
        SLabel latLabelTracker7 = new SLabel(lat);
        SLabel latLabelTracker8 = new SLabel(lat);
        SLabel latLabelTracker9 = new SLabel(lat);
        SLabel latLabelTracker10 = new SLabel(lat);
        SLabel latIncomingTracker = new SLabel(lat);

        // Creating the longitude labels for each tracker
        Cell<String> lon = new Cell<>("| Longitude:");
        SLabel lonLabelTracker1 = new SLabel(lon);
        SLabel lonLabelTracker2 = new SLabel(lon);
        SLabel lonLabelTracker3 = new SLabel(lon);
        SLabel lonLabelTracker4 = new SLabel(lon);
        SLabel lonLabelTracker5 = new SLabel(lon);
        SLabel lonLabelTracker6 = new SLabel(lon);
        SLabel lonLabelTracker7 = new SLabel(lon);
        SLabel lonLabelTracker8 = new SLabel(lon);
        SLabel lonLabelTracker9 = new SLabel(lon);
        SLabel lonLabelTracker10 = new SLabel(lon);
        SLabel lonincomingTracker = new SLabel(lon);

        // Creating the altitude labels for each tracker
        Cell<String> alt = new Cell<>("| Altitude:");
        SLabel altLabelTracker1 = new SLabel(alt);
        SLabel altLabelTracker2 = new SLabel(alt);
        SLabel altLabelTracker3 = new SLabel(alt);
        SLabel altLabelTracker4 = new SLabel(alt);
        SLabel altLabelTracker5 = new SLabel(alt);
        SLabel altLabelTracker6 = new SLabel(alt);
        SLabel altLabelTracker7 = new SLabel(alt);
        SLabel altLabelTracker8 = new SLabel(alt);
        SLabel altLabelTracker9 = new SLabel(alt);
        SLabel altLabelTracker10 = new SLabel(alt);
        SLabel altincomingTracker = new SLabel(alt);

        Cell<String> incomingEvents = new Cell<>("Incoming Events:");
        SLabel incomingEventsLabel = new SLabel(incomingEvents);
        SButton applyFilter = new SButton("Apply Filter");

        applyFilter.sClicked.listen(unit -> {
            // Get the filter values from the snapshot
            double latLower = Double.parseDouble(latLowerLimit.getText());
            double latUpper = Double.parseDouble(latUpperLimit.getText());
            double lonLower = Double.parseDouble(lonLowerLimit.getText());
            double lonUpper = Double.parseDouble(lonUpperLimit.getText());

            updateDistanceLabel(distanceTracker1, trackEvents1, latLower, latUpper, lonLower, lonUpper);
            updateDistanceLabel(distanceTracker2, trackEvents2, latLower, latUpper, lonLower, lonUpper);
            updateDistanceLabel(distanceTracker3, trackEvents3, latLower, latUpper, lonLower, lonUpper);
            updateDistanceLabel(distanceTracker4, trackEvents4, latLower, latUpper, lonLower, lonUpper);
            updateDistanceLabel(distanceTracker5, trackEvents5, latLower, latUpper, lonLower, lonUpper);
            updateDistanceLabel(distanceTracker6, trackEvents6, latLower, latUpper, lonLower, lonUpper);
            updateDistanceLabel(distanceTracker7, trackEvents7, latLower, latUpper, lonLower, lonUpper);
            updateDistanceLabel(distanceTracker8, trackEvents8, latLower, latUpper, lonLower, lonUpper);
            updateDistanceLabel(distanceTracker9, trackEvents8, latLower, latUpper, lonLower, lonUpper);
        });
    
        // Tracker 1 Display fields
        STextField nameTracker1 = new STextField("");
        Cell<String> nameCellTracker1 = nameTracker1.sUserChanges.hold(nameTracker1.getText());
        SLabel nameLabelTracker1 = new SLabel(nameCellTracker1);

        STextField latTracker1 = new STextField("");
        Cell<String> latCellTracker1 = latTracker1.sUserChanges.hold(latTracker1.getText());
        SLabel latOutputTracker1 = new SLabel(latCellTracker1);

        STextField lonTracker1 = new STextField("");
        Cell<String> lonCellTracker1 = lonTracker1.sUserChanges.hold(lonTracker1.getText());
        SLabel lonOutputTracker1 = new SLabel(lonCellTracker1);

        STextField altTracker1 = new STextField("");
        Cell<String> altCellTracker1 = altTracker1.sUserChanges.hold(altTracker1.getText());
        SLabel altOutputTracker1 = new SLabel(altCellTracker1);

        //  Tracker 2 Display fields
        STextField nameTracker2 = new STextField("");
        Cell<String> nameCellTracker2 = nameTracker2.sUserChanges.hold(nameTracker2.getText());
        SLabel nameLabelTracker2 = new SLabel(nameCellTracker2);

        STextField latTracker2 = new STextField("");
        Cell<String> latCellTracker2 = latTracker2.sUserChanges.hold(latTracker2.getText());
        SLabel latOutputTracker2 = new SLabel(latCellTracker2);

        STextField lonTracker2 = new STextField("");
        Cell<String> lonCellTracker2 = lonTracker2.sUserChanges.hold(lonTracker2.getText());
        SLabel lonOutputTracker2 = new SLabel(lonCellTracker2);

        STextField altTracker2 = new STextField("");
        Cell<String> altCellTracker2 = altTracker2.sUserChanges.hold(altTracker2.getText());
        SLabel altOutputTracker2 = new SLabel(altCellTracker2);
        
        //  Tracker 3 Display fields
        STextField nameTracker3 = new STextField("");
        Cell<String> nameCellTracker3 = nameTracker3.sUserChanges.hold(nameTracker3.getText());
        SLabel nameLabelTracker3 = new SLabel(nameCellTracker3);

        STextField latTracker3 = new STextField("");
        Cell<String> latCellTracker3 = latTracker3.sUserChanges.hold(latTracker3.getText());
        SLabel latOutputTracker3 = new SLabel(latCellTracker3);

        STextField lonTracker3 = new STextField("");
        Cell<String> lonCellTracker3 = lonTracker3.sUserChanges.hold(lonTracker3.getText());
        SLabel lonOutputTracker3 = new SLabel(lonCellTracker3);

        STextField altTracker3 = new STextField("");
        Cell<String> altCellTracker3 = altTracker3.sUserChanges.hold(altTracker3.getText());
        SLabel altOutputTracker3 = new SLabel(altCellTracker3);
 
        //  Tracker 4 Display fields
        STextField nameTracker4 = new STextField("");
        Cell<String> nameCellTracker4 = nameTracker4.sUserChanges.hold(nameTracker4.getText());
        SLabel nameLabelTracker4 = new SLabel(nameCellTracker4);

        STextField latTracker4 = new STextField("");
        Cell<String> latCellTracker4 = latTracker4.sUserChanges.hold(latTracker4.getText());
        SLabel latOutputTracker4 = new SLabel(latCellTracker4);

        STextField lonTracker4 = new STextField("");
        Cell<String> lonCellTracker4 = lonTracker4.sUserChanges.hold(lonTracker4.getText());
        SLabel lonOutputTracker4 = new SLabel(lonCellTracker4);

        STextField altTracker4 = new STextField("");
        Cell<String> altCellTracker4 = altTracker4.sUserChanges.hold(altTracker4.getText());
        SLabel altOutputTracker4 = new SLabel(altCellTracker4);

        //  Tracker 5 Display fields
        STextField nameTracker5 = new STextField("");
        Cell<String> nameCellTracker5 = nameTracker5.sUserChanges.hold(nameTracker5.getText());
        SLabel nameLabelTracker5 = new SLabel(nameCellTracker5);

        STextField latTracker5 = new STextField("");
        Cell<String> latCellTracker5 = latTracker5.sUserChanges.hold(latTracker5.getText());
        SLabel latOutputTracker5 = new SLabel(latCellTracker5);

        STextField lonTracker5 = new STextField("");
        Cell<String> lonCellTracker5 = lonTracker5.sUserChanges.hold(lonTracker5.getText());
        SLabel lonOutputTracker5 = new SLabel(lonCellTracker5);

        STextField altTracker5 = new STextField("");
        Cell<String> altCellTracker5 = altTracker5.sUserChanges.hold(altTracker5.getText());
        SLabel altOutputTracker5 = new SLabel(altCellTracker5);
        
        //  Tracker 6 Display fields
        STextField nameTracker6 = new STextField("");
        Cell<String> nameCellTracker6 = nameTracker6.sUserChanges.hold(nameTracker6.getText());
        SLabel nameLabelTracker6 = new SLabel(nameCellTracker6);

        STextField latTracker6 = new STextField("");
        Cell<String> latCellTracker6 = latTracker6.sUserChanges.hold(latTracker6.getText());
        SLabel latOutputTracker6 = new SLabel(latCellTracker6);

        STextField lonTracker6 = new STextField("");
        Cell<String> lonCellTracker6 = lonTracker6.sUserChanges.hold(lonTracker6.getText());
        SLabel lonOutputTracker6 = new SLabel(lonCellTracker6);

        STextField altTracker6 = new STextField("");
        Cell<String> altCellTracker6 = altTracker6.sUserChanges.hold(altTracker6.getText());
        SLabel altOutputTracker6 = new SLabel(altCellTracker6);
        
        //  Tracker 7 Display fields
        STextField nameTracker7 = new STextField("");
        Cell<String> nameCellTracker7 = nameTracker7.sUserChanges.hold(nameTracker7.getText());
        SLabel nameLabelTracker7 = new SLabel(nameCellTracker7);

        STextField latTracker7 = new STextField("");
        Cell<String> latCellTracker7 = latTracker7.sUserChanges.hold(latTracker7.getText());
        SLabel latOutputTracker7 = new SLabel(latCellTracker7);

        STextField lonTracker7 = new STextField("");
        Cell<String> lonCellTracker7 = lonTracker7.sUserChanges.hold(lonTracker7.getText());
        SLabel lonOutputTracker7 = new SLabel(lonCellTracker7);

        STextField altTracker7 = new STextField("");
        Cell<String> altCellTracker7 = altTracker7.sUserChanges.hold(altTracker7.getText());
        SLabel altOutputTracker7 = new SLabel(altCellTracker7);

        //  Tracker 8 Display fields
        STextField nameTracker8 = new STextField("");
        Cell<String> nameCellTracker8 = nameTracker8.sUserChanges.hold(nameTracker8.getText());
        SLabel nameLabelTracker8 = new SLabel(nameCellTracker8);

        STextField latTracker8 = new STextField("");
        Cell<String> latCellTracker8 = latTracker8.sUserChanges.hold(latTracker8.getText());
        SLabel latOutputTracker8 = new SLabel(latCellTracker8);

        STextField lonTracker8 = new STextField("");
        Cell<String> lonCellTracker8 = lonTracker8.sUserChanges.hold(lonTracker8.getText());
        SLabel lonOutputTracker8 = new SLabel(lonCellTracker8);

        STextField altTracker8 = new STextField("");
        Cell<String> altCellTracker8 = altTracker8.sUserChanges.hold(altTracker8.getText());
        SLabel altOutputTracker8 = new SLabel(altCellTracker8);

        //  Tracker 9 Display fields
        STextField nameTracker9 = new STextField("");
        Cell<String> nameCellTracker9 = nameTracker9.sUserChanges.hold(nameTracker9.getText());
        SLabel nameLabelTracker9 = new SLabel(nameCellTracker9);

        STextField latTracker9 = new STextField("");
        Cell<String> latCellTracker9 = latTracker9.sUserChanges.hold(latTracker9.getText());
        SLabel latOutputTracker9 = new SLabel(latCellTracker9);

        STextField lonTracker9 = new STextField("");
        Cell<String> lonCellTracker9 = lonTracker9.sUserChanges.hold(lonTracker9.getText());
        SLabel lonOutputTracker9 = new SLabel(lonCellTracker9);

        STextField altTracker9 = new STextField("");
        Cell<String> altCellTracker9 = altTracker9.sUserChanges.hold(altTracker9.getText());
        SLabel altOutputTracker9 = new SLabel(altCellTracker9);

        //  Tracker 10 Display fields
        STextField nameTracker10 = new STextField("");
        Cell<String> nameCellTracker10 = nameTracker10.sUserChanges.hold(nameTracker10.getText());
        SLabel nameLabelTracker10 = new SLabel(nameCellTracker10);

        STextField latTracker10 = new STextField("");
        Cell<String> latCellTracker10 = latTracker10.sUserChanges.hold(latTracker10.getText());
        SLabel latOutputTracker10 = new SLabel(latCellTracker10);

        STextField lonTracker10 = new STextField("");
        Cell<String> lonCellTracker10 = lonTracker10.sUserChanges.hold(lonTracker10.getText());
        SLabel lonOutputTracker10 = new SLabel(lonCellTracker10);

        STextField altTracker10 = new STextField("");
        Cell<String> altCellTracker10 = altTracker10.sUserChanges.hold(altTracker10.getText());
        SLabel altOutputTracker10 = new SLabel(altCellTracker10);

        STextField filterTrackerName  = new STextField("");

        //  Incoming Display fields
        STextField incomingTrackerName = new STextField("");
        Cell<String> incomingTrackerCell = incomingTrackerName.sUserChanges.hold(incomingTrackerName.getText());
        SLabel incomingTrackerLabel = new SLabel(incomingTrackerCell);

        STextField incomingTrackerLat = new STextField("");
        Cell<String> incomingTrackerLatCell = incomingTrackerLat.sUserChanges.hold(incomingTrackerLat.getText());
        SLabel incomingTrackerLatOutput = new SLabel(incomingTrackerLatCell);

        STextField incomingTrackerLon = new STextField("");
        Cell<String> incomingTrackerLonCell = incomingTrackerLon.sUserChanges.hold(incomingTrackerLon.getText());
        SLabel incomingTrackerLonOutput = new SLabel(incomingTrackerLonCell);

        STextField incomingTrackerAlt = new STextField("");
        Cell<String> incomingTrackerAltCell = incomingTrackerAlt.sUserChanges.hold(incomingTrackerAlt.getText());
        SLabel incomingTrackerAltOutput = new SLabel(incomingTrackerAltCell);


        // Declare a Timer for each field
        Timer clearTracker1 = new Timer(3000, (ActionEvent e) -> {
            latTracker1.setText("0");
            lonTracker1.setText("0");
            altTracker1.setText("0");
            distanceTracker1.setText("0");
        });

        Timer clearTracker2 = new Timer(3000, (ActionEvent e) -> {
            latTracker2.setText("0");
            lonTracker2.setText("0");
            altTracker2.setText("0");
            distanceTracker2.setText("0");
        });

        Timer clearTracker3 = new Timer(3000, (ActionEvent e) -> {
            latTracker3.setText("0");
            lonTracker3.setText("0");
            altTracker3.setText("0");
            distanceTracker3.setText("0");
        });

        Timer clearTracker4 = new Timer(3000, (ActionEvent e) -> {
            latTracker4.setText("0");
            lonTracker4.setText("0");
            altTracker4.setText("0");
            distanceTracker4.setText("0");
        });

        Timer clearTracker5 = new Timer(3000, (ActionEvent e) -> {
            latTracker5.setText("0");
            lonTracker5.setText("0");
            altTracker5.setText("0");
            distanceTracker5.setText("0");
        });

        Timer clearTracker6 = new Timer(3000, (ActionEvent e) -> {
            latTracker6.setText("0");
            lonTracker6.setText("0");
            altTracker6.setText("0");
            distanceTracker6.setText("0");
        });

        Timer clearTracker7 = new Timer(3000, (ActionEvent e) -> {
            latTracker7.setText("0");
            lonTracker7.setText("0");
            altTracker7.setText("0");
            distanceTracker7.setText("0");
        });
        
        Timer clearTracker8 = new Timer(3000, (ActionEvent e) -> {
            latTracker8.setText("0");
            lonTracker8.setText("0");
            altTracker8.setText("0");
            distanceTracker8.setText("0");
        });

        Timer clearTracker9 = new Timer(3000, (ActionEvent e) -> {
            latTracker9.setText("0");
            lonTracker9.setText("0");
            altTracker9.setText("0");
            distanceTracker9.setText("0");
        });

        Timer clearTracker10 = new Timer(3000, (ActionEvent e) -> {
            latTracker10.setText("0");
            lonTracker10.setText("0");
            altTracker10.setText("0");
            distanceTracker10.setText("0");
        });


        for(Stream<GpsEvent> s : streams){    
            // Dynamically update tracker information based on received events
            s.listen((GpsEvent ev) -> {
                filterTrackerName.setText(ev.toString());

                if (isWithinRange(ev, Double.parseDouble(latLowerLimit.getText()), Double.parseDouble(latUpperLimit.getText()), Double.parseDouble(lonLowerLimit.getText()), Double.parseDouble(lonUpperLimit.getText()))) {
                    incomingTrackerName.setText(ev.name);
                    incomingTrackerLat.setText(Double.toString(ev.latitude));
                    incomingTrackerLon.setText(Double.toString(ev.longitude));
                    incomingTrackerAlt.setText(Double.toString(ev.altitude));

                    SwingUtilities.invokeLater(() -> {
                        switch (ev.name) {
                            case "Tracker0":
                                clearTracker1.restart();
                                break;
                            case "Tracker1":
                                clearTracker2.restart();
                                break;
                            case "Tracker2":
                                clearTracker3.restart();
                                break;
                            case "Tracker3":
                                clearTracker4.restart();
                                break;
                            case "Tracker4":
                                clearTracker5.restart();
                                break;
                            case "Tracker5":
                                clearTracker6.restart();
                                break;
                            case "Tracker6":
                                clearTracker7.restart();
                                break;
                            case "Tracker7":
                                clearTracker8.restart();
                                break;
                            case "Tracker8":
                                clearTracker9.restart();
                                break;
                            case "Tracker9":
                                clearTracker10.restart();
                                break;
                    };

                    if (ev.name.equals("Tracker0")) {   
                        nameTracker1.setText(ev.name);
                        latTracker1.setText(Double.toString(ev.latitude));
                        lonTracker1.setText(Double.toString(ev.longitude));
                        altTracker1.setText(Double.toString(ev.altitude));
                        double totalDistance1 = updateDistanceLabel(distanceTracker1, trackEvents1, Double.parseDouble(latLowerLimit.getText()), Double.parseDouble(latUpperLimit.getText()), Double.parseDouble(lonLowerLimit.getText()), Double.parseDouble(lonUpperLimit.getText()));
                        distanceTracker1.setText(Double.toString(totalDistance1));
                        trackEvents1.add(ev);
                    }
                    else if (ev.name.equals("Tracker1")){
                        nameTracker2.setText(ev.name);
                        latTracker2.setText(Double.toString(ev.latitude));
                        lonTracker2.setText(Double.toString(ev.longitude));
                        altTracker2.setText(Double.toString(ev.altitude));
                        double totalDistance2 = updateDistanceLabel(distanceTracker2, trackEvents2, Double.parseDouble(latLowerLimit.getText()), Double.parseDouble(latUpperLimit.getText()), Double.parseDouble(lonLowerLimit.getText()), Double.parseDouble(lonUpperLimit.getText()));
                        distanceTracker2.setText(Double.toString(totalDistance2));
                        trackEvents2.add(ev);
                    }
                    else if (ev.name.equals("Tracker2")) {
                        nameTracker3.setText(ev.name);
                        latTracker3.setText(Double.toString(ev.latitude));
                        lonTracker3.setText(Double.toString(ev.longitude));
                        altTracker3.setText(Double.toString(ev.altitude));
                        double totalDistance3 = updateDistanceLabel(distanceTracker3, trackEvents3, Double.parseDouble(latLowerLimit.getText()), Double.parseDouble(latUpperLimit.getText()), Double.parseDouble(lonLowerLimit.getText()), Double.parseDouble(lonUpperLimit.getText()));
                        distanceTracker3.setText(Double.toString(totalDistance3));
                        trackEvents3.add(ev);
                    }
                    else if (ev.name.equals("Tracker3")) {
                        nameTracker4.setText(ev.name);
                        latTracker4.setText(Double.toString(ev.latitude));
                        lonTracker4.setText(Double.toString(ev.longitude));
                        altTracker4.setText(Double.toString(ev.altitude));
                        double totalDistance4 = updateDistanceLabel(distanceTracker4, trackEvents4, Double.parseDouble(latLowerLimit.getText()), Double.parseDouble(latUpperLimit.getText()), Double.parseDouble(lonLowerLimit.getText()), Double.parseDouble(lonUpperLimit.getText()));
                        distanceTracker4.setText(Double.toString(totalDistance4));
                        trackEvents4.add(ev);
                    }
                    else if (ev.name.equals("Tracker4")) {
                        nameTracker5.setText(ev.name);
                        latTracker5.setText(Double.toString(ev.latitude));
                        lonTracker5.setText(Double.toString(ev.longitude));
                        altTracker5.setText(Double.toString(ev.altitude));
                        double totalDistance5 = updateDistanceLabel(distanceTracker5, trackEvents5, Double.parseDouble(latLowerLimit.getText()), Double.parseDouble(latUpperLimit.getText()), Double.parseDouble(lonLowerLimit.getText()), Double.parseDouble(lonUpperLimit.getText()));
                        distanceTracker5.setText(Double.toString(totalDistance5));
                        trackEvents5.add(ev);
                    }
                    else if (ev.name.equals("Tracker5")) {
                        nameTracker6.setText(ev.name);
                        latTracker6.setText(Double.toString(ev.latitude));
                        lonTracker6.setText(Double.toString(ev.longitude));
                        altTracker6.setText(Double.toString(ev.altitude));
                        double totalDistance6 = updateDistanceLabel(distanceTracker6, trackEvents6, Double.parseDouble(latLowerLimit.getText()), Double.parseDouble(latUpperLimit.getText()), Double.parseDouble(lonLowerLimit.getText()), Double.parseDouble(lonUpperLimit.getText()));
                        distanceTracker6.setText(Double.toString(totalDistance6));
                        trackEvents6.add(ev);
                    }
                    else if (ev.name.equals("Tracker6")) {
                        nameTracker7.setText(ev.name);
                        latTracker7.setText(Double.toString(ev.latitude));
                        lonTracker7.setText(Double.toString(ev.longitude));
                        altTracker7.setText(Double.toString(ev.altitude));
                        double totalDistance7 = updateDistanceLabel(distanceTracker7, trackEvents7, Double.parseDouble(latLowerLimit.getText()), Double.parseDouble(latUpperLimit.getText()), Double.parseDouble(lonLowerLimit.getText()), Double.parseDouble(lonUpperLimit.getText()));
                        distanceTracker7.setText(Double.toString(totalDistance7));
                        trackEvents7.add(ev);
                    }
                    else if (ev.name.equals("Tracker7")) {
                        nameTracker8.setText(ev.name);
                        latTracker8.setText(Double.toString(ev.latitude));
                        lonTracker8.setText(Double.toString(ev.longitude));
                        altTracker8.setText(Double.toString(ev.altitude));
                        double totalDistance8 = updateDistanceLabel(distanceTracker8, trackEvents8, Double.parseDouble(latLowerLimit.getText()), Double.parseDouble(latUpperLimit.getText()), Double.parseDouble(lonLowerLimit.getText()), Double.parseDouble(lonUpperLimit.getText()));
                        distanceTracker8.setText(Double.toString(totalDistance8));
                        trackEvents8.add(ev);
                    }
                    else if (ev.name.equals("Tracker8")) {
                        nameTracker9.setText(ev.name);
                        latTracker9.setText(Double.toString(ev.latitude));
                        lonTracker9.setText(Double.toString(ev.longitude));
                        altTracker9.setText(Double.toString(ev.altitude));
                        double totalDistance9 = updateDistanceLabel(distanceTracker9, trackEvents9, Double.parseDouble(latLowerLimit.getText()), Double.parseDouble(latUpperLimit.getText()), Double.parseDouble(lonLowerLimit.getText()), Double.parseDouble(lonUpperLimit.getText()));
                        distanceTracker9.setText(Double.toString(totalDistance9));
                        trackEvents9.add(ev);
                    }
                    else if (ev.name.equals("Tracker9")) {
                        nameTracker10.setText(ev.name);
                        latTracker10.setText(Double.toString(ev.latitude));
                        lonTracker10.setText(Double.toString(ev.longitude));
                        altTracker10.setText(Double.toString(ev.altitude));
                        double totalDistance10 = updateDistanceLabel(distanceTracker10, trackEvents10, Double.parseDouble(latLowerLimit.getText()), Double.parseDouble(latUpperLimit.getText()), Double.parseDouble(lonLowerLimit.getText()), Double.parseDouble(lonUpperLimit.getText()));
                        distanceTracker10.setText(Double.toString(totalDistance10));
                        trackEvents10.add(ev);
                    }
                });     
            }     
        });
    }
        // Adding Swing components to panels
        firstPanel.add(nameLabelTracker1);
        firstPanel.add(latLabelTracker1);
        firstPanel.add(latOutputTracker1);
        firstPanel.add(lonLabelTracker1);
        firstPanel.add(lonOutputTracker1);
        firstPanel.add(altLabelTracker1);
        firstPanel.add(altOutputTracker1);
        firstPanel.add(distLabelTracker1);
        firstPanel.add(distanceLabelTracker1);

        secondPanel.add(nameLabelTracker2);
        secondPanel.add(latLabelTracker2);
        secondPanel.add(latOutputTracker2);
        secondPanel.add(lonLabelTracker2);
        secondPanel.add(lonOutputTracker2);
        secondPanel.add(altLabelTracker2);
        secondPanel.add(altOutputTracker2);
        secondPanel.add(distLabelTracker2);
        secondPanel.add(distanceLabelTracker2);

        thirdPanel.add(nameLabelTracker3);
        thirdPanel.add(latLabelTracker3);
        thirdPanel.add(latOutputTracker3);
        thirdPanel.add(lonLabelTracker3);
        thirdPanel.add(lonOutputTracker3);
        thirdPanel.add(altLabelTracker3);
        thirdPanel.add(altOutputTracker3);
        thirdPanel.add(distLabelTracker3);
        thirdPanel.add(distanceLabelTracker3);

        fourthPanel.add(nameLabelTracker4);
        fourthPanel.add(latLabelTracker4);
        fourthPanel.add(latOutputTracker4);
        fourthPanel.add(lonLabelTracker4);
        fourthPanel.add(lonOutputTracker4);
        fourthPanel.add(altLabelTracker4);
        fourthPanel.add(altOutputTracker4);
        fourthPanel.add(distLabelTracker4);
        fourthPanel.add(distanceLabelTracker4);

        fifthPanel.add(nameLabelTracker5);
        fifthPanel.add(latLabelTracker5);
        fifthPanel.add(latOutputTracker5);
        fifthPanel.add(lonLabelTracker5);
        fifthPanel.add(lonOutputTracker5);
        fifthPanel.add(altLabelTracker5);
        fifthPanel.add(altOutputTracker5);
        fifthPanel.add(distLabelTracker5);
        fifthPanel.add(distanceLabelTracker5);

        sixthPanel.add(nameLabelTracker6);
        sixthPanel.add(latLabelTracker6);
        sixthPanel.add(latOutputTracker6);
        sixthPanel.add(lonLabelTracker6);
        sixthPanel.add(lonOutputTracker6);
        sixthPanel.add(altLabelTracker6);
        sixthPanel.add(altOutputTracker6);
        sixthPanel.add(distLabelTracker6);
        sixthPanel.add(distanceLabelTracker6);

        seventhPanel.add(nameLabelTracker7);
        seventhPanel.add(latLabelTracker7);
        seventhPanel.add(latOutputTracker7);
        seventhPanel.add(lonLabelTracker7);
        seventhPanel.add(lonOutputTracker7);
        seventhPanel.add(altLabelTracker7);
        seventhPanel.add(altOutputTracker7);
        seventhPanel.add(distLabelTracker7);
        seventhPanel.add(distanceLabelTracker7);

        eighthPanel.add(nameLabelTracker8);
        eighthPanel.add(latLabelTracker8);
        eighthPanel.add(latOutputTracker8);
        eighthPanel.add(lonLabelTracker8);
        eighthPanel.add(lonOutputTracker8);
        eighthPanel.add(altLabelTracker8);
        eighthPanel.add(altOutputTracker8);
        eighthPanel.add(distLabelTracker8);
        eighthPanel.add(distanceLabelTracker8);

        ninethPanel.add(nameLabelTracker9);
        ninethPanel.add(latLabelTracker9);
        ninethPanel.add(latOutputTracker9);
        ninethPanel.add(lonLabelTracker9);
        ninethPanel.add(lonOutputTracker9);
        ninethPanel.add(altLabelTracker9);
        ninethPanel.add(altOutputTracker9);
        ninethPanel.add(distLabelTracker9);
        ninethPanel.add(distanceLabelTracker9);

        tenthPanel.add(nameLabelTracker10);
        tenthPanel.add(latLabelTracker10);
        tenthPanel.add(latOutputTracker10);
        tenthPanel.add(lonLabelTracker10);
        tenthPanel.add(lonOutputTracker10);
        tenthPanel.add(altLabelTracker10);
        tenthPanel.add(altOutputTracker10);
        tenthPanel.add(distLabelTracker10);
        tenthPanel.add(distanceLabelTracker10);

        incomingPanel.add(incomingEventsLabel);
        incomingPanel.add(incomingTrackerLabel);
        incomingPanel.add(latIncomingTracker);
        incomingPanel.add(incomingTrackerLatOutput);
        incomingPanel.add(lonincomingTracker);
        incomingPanel.add(incomingTrackerLonOutput);
        incomingPanel.add(altincomingTracker);
        incomingPanel.add(incomingTrackerAltOutput);

        filterPanel.add(latLowerLabel);
        filterPanel.add(latLowerLimit);
        filterPanel.add(latUpperLabel);
        filterPanel.add(latUpperLimit);
        filterPanel.add(lonLowerLabel);
        filterPanel.add(lonLowerLimit);
        filterPanel.add(lonUpperLabel);
        filterPanel.add(lonUpperLimit);
        filterButtonPanel.add(applyFilter);
        
        frame.setSize(1500, 600);
        frame.setVisible(true);
    }
}
