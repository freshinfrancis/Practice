import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import swidgets.*;
import nz.sodium.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.time.format.DateTimeFormatter;  
import java.time.LocalDateTime;

/** 
 * An example of how to use the GpsService class
 */
public class GpsGui {
    static List<Cell<String>> trackers = new ArrayList<Cell<String>>();
    static List<JLabel> namesOfTrackers = new ArrayList<JLabel>();
    static List<Timer> timers = new ArrayList<Timer>(10);
    static List<StreamSink<String>> timersStream = new ArrayList<StreamSink<String>>();
    static List<Stream<String>> bufferLatest = new ArrayList<Stream<String>>();
    static List<Stream<String>> combinedFilter = new ArrayList<Stream<String>>();
    static Timer filterFieldTimer = new Timer();
    static StreamSink<String> filterTimerSink = new StreamSink<>();

    public static void main(String[] args){
        JFrame frame = new JFrame("GPS GUI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Set the panel to add labels
        JPanel panel = new JPanel();
        BoxLayout boxlayout = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(boxlayout);
        panel.setBorder(BorderFactory.createEmptyBorder(50,50,50,10));

        JPanel display0 = new JPanel(new FlowLayout(FlowLayout.LEFT, 50, 0));
        JPanel display1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 50, 0));
        JPanel display2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 50, 0));
        JPanel display3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 50, 0));
        JPanel display4 = new JPanel(new FlowLayout(FlowLayout.LEFT, 50, 0));
        JPanel display5 = new JPanel(new FlowLayout(FlowLayout.LEFT, 50, 0));
        JPanel display6 = new JPanel(new FlowLayout(FlowLayout.LEFT, 50, 0));
        JPanel display7 = new JPanel(new FlowLayout(FlowLayout.LEFT, 50, 0));
        JPanel display8 = new JPanel(new FlowLayout(FlowLayout.LEFT, 50, 0));
        JPanel display9 = new JPanel(new FlowLayout(FlowLayout.LEFT, 50, 0));
        JPanel singleDisplay = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        JPanel filterSettings = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

        // initialise timers
        for(int i = 0; i<10; i++){
            timers.add(new Timer());
        }

        // Initialise the GPS Service
        GpsService serv = new GpsService();
        // Retrieve Event Streams
        Stream<GpsEvent>[] streams = serv.getEventStreams();
        // Attach a handler method to each stream
        for(Stream<GpsEvent> s : streams){
            s.listen((GpsEvent ev) -> System.out.println(ev));
            s.listen((GpsEvent ev) -> VisibilityTimer(ev.name));
        }

        // loop over stuff???
        for(int i = 0; i<10; i++){
            // initialise sinks to add timer events into
            timersStream.add(new StreamSink<>());
            // map each event to a string that says its lat and long
            Stream<String> eventbuffer = streams[i].map(u -> "lat: "+ u.latitude + "    long: " + 
            u.longitude);
            // merge streams from timer and the gps together
            trackers.add(eventbuffer.orElse(timersStream.get(i)).hold(""));
        }

        // labels for each tracker
        SLabel label0 = new SLabel(trackers.get(0));
        SLabel label1 = new SLabel(trackers.get(1));
        SLabel label2 = new SLabel(trackers.get(2));
        SLabel label3 = new SLabel(trackers.get(3));
        SLabel label4 = new SLabel(trackers.get(4));
        SLabel label5 = new SLabel(trackers.get(5));
        SLabel label6 = new SLabel(trackers.get(6));
        SLabel label7 = new SLabel(trackers.get(7));
        SLabel label8 = new SLabel(trackers.get(8));
        SLabel label9 = new SLabel(trackers.get(9));

        // create display for latest event
        JLabel latestName = new JLabel("Latest Events");
        for(int i = 0; i<10; i++){
            // initialise sinks to add timer events into
            // timersStream.add(new StreamSink<>());
            // map each event to a string that says its lat and long
            Stream<String> eventbuffer = streams[i].map(u -> u.name + ", " + u.latitude + ", "
            + u.longitude + ", " + u.altitude);
            // merge streams from timer and the gps together
            bufferLatest.add(eventbuffer.orElse(timersStream.get(i)));
        }
        bufferLatest.get(0);
        // combines all streams into one
        Stream<String> latestEvents = Stream.orElse(bufferLatest);
        STextField latestText = new STextField(latestEvents, "");
        latestText.setColumns(30);
        // display time on the side
        SLabel latestTime = new SLabel(latestEvents.map(u -> java.time.LocalTime.now().toString()).hold("") );

        // create frp thing for gps filtering
        // create labels and text field and a button for filtering
        JLabel latName = new JLabel("Max Lat:");
        STextField latField = new STextField("90");
        JLabel longName = new JLabel("Max Long:");
        STextField longField = new STextField("180");
        SButton setter = new SButton("Set");
        // snapshot values in text field and store them as hashmap kv pair
        Stream<HashMap<String, String>> newSets =
            setter.sClicked.snapshot(latField.text, longField.text, (u, lat, longs) -> {
                HashMap<String,String> x = new HashMap<String, String>();
                x.put("lat", lat.toString());
                x.put("long", longs.toString());
                return x;
            });
        // retrieve lat/longs and put in cells
        Cell<String> latOut = newSets.map(u -> u.get("lat")).hold("");
        Cell<String> longOut = newSets.map(u -> u.get("long")).hold("");
        SLabel currentLat = new SLabel(latOut);
        SLabel currentLong = new SLabel(longOut);
        // initialise current starting fitlers
        currentLat.setText("90");
        currentLong.setText("180");

        // create new area to show filtered tracker info
        JLabel filteredName = new JLabel("Filtered Events");
        
        // borrow stream from part 2
        for(int i = 0; i<10; i++){
            // filter from the start, since orelse() may drop some valid events
            Stream<GpsEvent> filteredStream = streams[i].filter(ev -> ev.latitude <= 
            Double.valueOf(currentLat.getText()) && ev.longitude <= Double.valueOf(currentLong.getText()) );
            // use map and merge primitives as normal
            Stream<String> combiner = filteredStream.map(u -> u.name + ", " + u.latitude + ", "
            + u.longitude + ", " + u.altitude);
            // merge streams from timer and the gps together
            combinedFilter.add(combiner);
        }
        
        combinedFilter.get(0);
        // combines all streams into one
        Stream<String> combiner2 = combinedFilter.get(0).orElse(combinedFilter);

        combiner2.listen((String ev) -> SinglesTimer(ev.substring(0, 8)));
        combiner2 = combiner2.orElse(filterTimerSink);

        STextField filtertexter = new STextField(combiner2, "");
        filtertexter.setColumns(30);
        // display time on the side
        SLabel filterTime = new SLabel(combiner2.map(u -> java.time.LocalTime.now().toString()).hold("") );

        // create name label for each tracker
        for(int i = 0; i<10; i++){
            namesOfTrackers.add(new JLabel("Tracker " + i));
        }
        display0.add(namesOfTrackers.get(0));
        display1.add(namesOfTrackers.get(1));
        display2.add(namesOfTrackers.get(2));
        display3.add(namesOfTrackers.get(3));
        display4.add(namesOfTrackers.get(4));
        display5.add(namesOfTrackers.get(5));
        display6.add(namesOfTrackers.get(6));
        display7.add(namesOfTrackers.get(7));
        display8.add(namesOfTrackers.get(8));
        display9.add(namesOfTrackers.get(9));

        // display streams for each tracker
        display0.add(label0);
        display1.add(label1);
        display2.add(label2);
        display3.add(label3);
        display4.add(label4);
        display5.add(label5);
        display6.add(label6);
        display7.add(label7);
        display8.add(label8);
        display9.add(label9);

        // display latest events
        singleDisplay.add(latestName);
        singleDisplay.add(latestText);
        singleDisplay.add(latestTime);

        // display gps filters
        filterSettings.add(latName);
        filterSettings.add(latField);
        filterSettings.add(currentLat);
        filterSettings.add(longName);
        filterSettings.add(longField);
        filterSettings.add(currentLong);
        filterSettings.add(setter);
        filterSettings.add(filteredName);
        filterSettings.add(filtertexter);
        filterSettings.add(filterTime);
        
        panel.add(display0);
        panel.add(Box.createVerticalGlue());
        panel.add(display1);
        panel.add(Box.createVerticalGlue());
        panel.add(display2);
        panel.add(Box.createVerticalGlue());
        panel.add(display3);
        panel.add(Box.createVerticalGlue());
        panel.add(display4);
        panel.add(Box.createVerticalGlue());
        panel.add(display5);
        panel.add(Box.createVerticalGlue());
        panel.add(display6);
        panel.add(Box.createVerticalGlue());
        panel.add(display7);
        panel.add(Box.createVerticalGlue());
        panel.add(display8);
        panel.add(Box.createVerticalGlue());
        panel.add(display9);
        panel.add(Box.createVerticalGlue());
        panel.add(singleDisplay);
        panel.add(Box.createVerticalGlue());
        panel.add(filterSettings);
        
        frame.add(panel);
        frame.setSize(700, 1000);
        frame.setVisible(true);
    }

    static void VisibilityTimer(String tracker){

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                int tindex = Integer.valueOf(tracker.substring(7, 8));
                timersStream.get(tindex).send("empty    time: " + java.time.LocalTime.now());
            }
        };
        // yea this could be better but idc anymore
        if (tracker.equals("Tracker0")){
            timers.get(0).cancel();
            timers.get(0).purge();
            timers.set(0, new Timer());
            timers.get(0).schedule(timerTask, 3000);
        }
        if (tracker.equals("Tracker1")){
            timers.get(1).cancel();
            timers.get(1).purge();
            timers.set(1, new Timer());
            timers.get(1).schedule(timerTask, 3000);
        }
        if (tracker.equals("Tracker2")){
            timers.get(2).cancel();
            timers.get(2).purge();
            timers.set(2, new Timer());
            timers.get(2).schedule(timerTask, 3000);
        }
        if (tracker.equals("Tracker3")){
            timers.get(3).cancel();
            timers.get(3).purge();
            timers.set(3, new Timer());
            timers.get(3).schedule(timerTask, 3000);
        }
        if (tracker.equals("Tracker4")){
            timers.get(4).cancel();
            timers.get(4).purge();
            timers.set(4, new Timer());
            timers.get(4).schedule(timerTask, 3000);
        }
        if (tracker.equals("Tracker5")){
            timers.get(5).cancel();
            timers.get(5).purge();
            timers.set(5, new Timer());
            timers.get(5).schedule(timerTask, 3000);
        }
        if (tracker.equals("Tracker6")){
            timers.get(6).cancel();
            timers.get(6).purge();
            timers.set(6, new Timer());
            timers.get(6).schedule(timerTask, 3000);
        }
        if (tracker.equals("Tracker7")){
            timers.get(7).cancel();
            timers.get(7).purge();
            timers.set(7, new Timer());
            timers.get(7).schedule(timerTask, 3000);
        }
        if (tracker.equals("Tracker8")){
            timers.get(8).cancel();
            timers.get(8).purge();
            timers.set(8, new Timer());
            timers.get(8).schedule(timerTask, 3000);
        }
        if (tracker.equals("Tracker9")){
            timers.get(9).cancel();
            timers.get(9).purge();
            timers.set(9, new Timer());
            timers.get(9).schedule(timerTask, 3000);
        }
    }

    static void SinglesTimer(String tracker){
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                filterTimerSink.send("empty (filter)    time: " + java.time.LocalTime.now());
            }
        };
        filterFieldTimer.cancel();
        filterFieldTimer.purge();
        filterFieldTimer = new Timer();
        filterFieldTimer.schedule(timerTask, 3000);
    }
} 
