package com.assignment2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InterlockingImpl implements Interlocking {
    HashMap<Integer, Section> sections;

    HashMap<String, Train> trains;

    //Constraints to be check when calling addTrain method
    HashMap<Pair<Integer, Integer>, Set<Pair<Integer, Integer>>> constraints = new HashMap<>(Map.ofEntries(
            Map.entry(Pair.of(4, 3), Set.of(Pair.of(3, 4))),
            Map.entry(Pair.of(3, 4), Set.of(Pair.of(4, 3))),
            Map.entry(Pair.of(3, 11), Set.of(Pair.of(11, 3), Pair.of(7, 3))),
            Map.entry(Pair.of(11, 3), Set.of(Pair.of(3, 11), Pair.of(7, 11))),
            Map.entry(Pair.of(1, 9), Set.of(Pair.of(9, 2))),
            Map.entry(Pair.of(9, 2), Set.of(Pair.of(1, 9), Pair.of(5, 9)))
    ));

    //Priority to be checked when calling moveTrains method
    HashMap<Pair<Integer, Integer>, Set<Pair<Integer, Integer>>> priority = new HashMap<>(Map.ofEntries(
            Map.entry(Pair.of(3, 4), Set.of(
                    Pair.of(1, 5),
                    Pair.of(6, 2)
            )),
            Map.entry(Pair.of(4, 3), Set.of(
                    Pair.of(1, 5),
                    Pair.of(6, 2)
            )),
            Map.entry(Pair.of(9, 6), Set.of(
                    Pair.of(5, 8),
                    Pair.of(10, 6)
            ))
    ));

    Set<Pair<Integer, Integer>> prioritySet;

    public InterlockingImpl() {
        sections = new HashMap<>();
        for (int i = 1; i < 12; i++) {
            sections.put(i, new Section(i));
        }
        trains = new HashMap<>();
        prioritySet = new HashSet<>();
        for (Set<Pair<Integer, Integer>> item : priority.values()) {
            prioritySet.addAll(item);
        }
    }

    /**
     * Check if there is a priority target to give way to
     * @param key - pair of integers defining current section and section to move to
     * @return true if a priority train is not in the system false otherwise
     */
    private boolean hasPriorityTarget(Pair<Integer, Integer> key) {
        Set<Pair<Integer, Integer>> priorityTarget = priority.get(key);
        if (priorityTarget == null) {
            return false;
        }
        for (Pair<Integer, Integer> target : priorityTarget) {
            Section section = sections.get(target.first);
            int targetDestination = target.second;
            if (!section.isOccupied()) {
                continue;
            }
            if (section.train.getNextSection() == targetDestination) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the current train can be moved.
     *
     * Train can be moved if no train with higher priority is in the system, and if the next section is not occupied
     * @param train train to be moved
     * @return true if train can be moved, false otherwise
     */
    private boolean isMovable(Train train) {
        Section nextSection = sections.get(train.getNextSection());
        Pair<Integer, Integer> key = Pair.of(train.getSection(), train.getNextSection());
        //If nextSection is exit point -> Movable
        if (nextSection == null) {
            return true;
        }
        //If either nextSection is occupied or if a priority target is present -> Not movable
        if (nextSection.isOccupied() || hasPriorityTarget(key)) {
            return false;
        }
        return true;
    }

    /**
     * Move a train object if permitted
     * @param train - train to be moved
     * @return false if train is not movable else true
     */
    private boolean moveTrain(Train train) {
        if (isMovable(train)) {
            Section currentSection = sections.get(train.getSection());
            Section nextSection = sections.get(train.getNextSection());
            currentSection.moveTrain();
            if (nextSection != null) {
                nextSection.addTrain(train);
            }
            return true;
        }
        return false;
    }

    /**
     * Adds a train to the rail corridor.
     *
     * @param trainName               A String that identifies a given train. Cannot be the same as any other train present.
     * @param entryTrackSection       The id number of the track section that the train is entering into.
     * @param destinationTrackSection The id number of the track section that the train should exit from.
     * @throws IllegalArgumentException if the train name is already in use, or there is no valid path from the entry to the destination
     * @throws IllegalStateException    if the entry track is already occupied
     */
    @Override
    public void addTrain(String trainName, int entryTrackSection, int destinationTrackSection)
            throws IllegalArgumentException, IllegalStateException {
        //Check if any constraint is violated
        Pair<Integer, Integer> key = Pair.of(entryTrackSection, destinationTrackSection);
        Set<Pair<Integer, Integer>> value = constraints.get(key);
        if (value != null) {
            for (Pair<Integer, Integer> c : value) {
                Section constraintSection = sections.get(c.first);
                int targetSection = c.second;
                if (constraintSection.isOccupied()) {
                    if (constraintSection.train.getDestination() == targetSection) {
                        throw new IllegalStateException("Constraint not met: trying to add a train heading for " +
                                destinationTrackSection + " from " + entryTrackSection + ", but a train heading for "
                                + targetSection + " is on " + c.first);
                    }
                }
            }
        }

        Train newTrain = new Train(trainName, entryTrackSection, destinationTrackSection);
        sections.get(entryTrackSection).addTrain(newTrain);
        trains.put(trainName, newTrain);
    }

    /**
     * The listed trains proceed to the next track section.
     * Trains only move if they are able to do so, otherwise they remain in their current section.
     * When a train reaches its destination track section, it exits the rail corridor next time it moves.
     *
     * @param trainNames The names of the trains to move.
     * @return The number of trains that have moved.
     * @throws IllegalArgumentException if the train name does not exist or is no longer in the rail corridor
     */
    @Override
    public int moveTrains(String[] trainNames) throws IllegalArgumentException {
        int count = 0;
        List<String> priorityNames = new ArrayList<>();
        List<String> nonPriorityNames = new ArrayList<>();
        //First pass - check illegal exception and prioritise priority sets.
        for (String name : trainNames) {
            //Check if train is in service
            if (!Train.allTrains.contains(name)) {
                throw new IllegalArgumentException("Train " + name + " is not in service.");
            }
            Train train = trains.get(name);
            Pair<Integer, Integer> key = Pair.of(train.getSection(), train.getNextSection());
            if (prioritySet.contains(key)) {
                priorityNames.add(name);
                continue;
            }
            nonPriorityNames.add(name);
        }
        priorityNames.addAll(nonPriorityNames);
        //Second pass - move trains
        for (String name : priorityNames) {
            Train train = trains.get(name);
            boolean isMoved = moveTrain(train);
            if (isMoved) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns the name of the Train currently occupying a given track section
     *
     * @param trackSection The id number of the section of track.
     * @return The name of the train currently in that section, or null if the section is empty/unoccupied.
     * @throws IllegalArgumentException if the track section does not exist
     */
    @Override
    public String getSection(int trackSection) throws IllegalArgumentException {
        if (!sections.containsKey(trackSection)) {
            throw new IllegalArgumentException("Track section does not exist");
        }
        return sections.get(trackSection).getTrain();
    }


    /**
     * Returns the track section that a given train is occupying
     *
     * @param trainName The name of the train.
     * @return The id number of section of track the train is occupying, or -1 if the train is no longer in the rail corridor
     * @throws IllegalArgumentException if the train name does not exist
     */
    @Override
    public int getTrain(String trainName) throws IllegalArgumentException {
        if (!Train.allTrains.contains(trainName)) {
            throw new IllegalArgumentException("Train name does not exist");
        }
        return trains.get(trainName).getSection();
    }

    public String printSection(int trackSection){
        if (getSection(trackSection)==null){
            return " ";
        }
        return getSection(trackSection);
    }

    public static void removeTrain(String trainName){
        Train.removeTrain(trainName);
    }

    /**
     * Print to console the network for debugging purposes
     */
    public String toString() {
        String repr = String.format("%-15s|%-15s|%-15s", " ", " ", " ");
        int underlineLength = repr.length();
        repr = repr + "\n";

        //A dotted line under header
        String underline = "";
        for (int i = 0; i < underlineLength; i++) {
            underline += "-";
        }
        underline += "\n";
        repr = underline;

        //Add Tracks
        repr = repr + String.format("%-15s|%-15s|%-15s",
                " ",
                "4: " + printSection(4),
                "8: " + printSection(8)) + "\n";
        repr = repr + String.format("%-15s|%-15s|%-15s",
                "1: " + printSection(1),
                "5: " + printSection(5),
                "9: " + printSection(9)) + "\n";
        repr = repr + String.format("%-15s|%-15s|%-15s",
                "2: " + printSection(2),
                "6: " + printSection(6),
                "10: " + printSection(10)) + "\n";
        repr = repr + String.format("%-15s|%-15s|%-15s",
                "3: " + printSection(3),
                "7: " + printSection(7),
                "11: " + printSection(11)) + "\n";
        repr = repr + underline;

        return repr;
    }

}

class Section {
    int sectionID;
    public Train train;
    boolean occupied;

    /**
     * Initialise a section
     *
     * @param sectionID name of the train section to add
     */
    public Section(int sectionID) {
        this.sectionID = sectionID;
        this.train = null;
        this.occupied = false;
    }

    /**
     * Check if any train is occupying the track
     *
     * @return
     */
    public boolean isOccupied() {
        return occupied;
    }

    /**
     * Add a train to the current track if permitted
     *
     * @param train Train object
     * @throws IllegalStateException if the train is occupied
     */
    public void addTrain(Train train) throws IllegalStateException {
        if (!isOccupied()) {
            this.train = train;
            this.occupied = true;
            return;
        }
        if (this.train == train) {
            return;
        }
        throw new IllegalStateException("Track " + sectionID + " is currently occupied.");
    }

    /**
     * Get name of the train currently occupying track
     *
     * @return trainName if there is a train on track, otherwise null
     */
    public String getTrain() {
        if (train != null) {
            return train.trainName;
        }
        return null;
    }

    /**
     * Move the train in the current section to the next scheduled section if permitted.
     * Reset track to empty state.
     */
    public void moveTrain() {
        if (this.train != null) {
            this.train.move();
            this.occupied = false;
            this.train = null;
        }
    }

}

class Train {
    public static HashMap<Pair<Integer, Integer>, List<Integer>> allPaths = new HashMap<>(Map.ofEntries(
            Map.entry(Pair.of(1, 8), List.of(1, 5, 8)),
            Map.entry(Pair.of(1, 9), List.of(1, 5, 9)),
            Map.entry(Pair.of(3, 4), List.of(3, 4)),
            Map.entry(Pair.of(4, 3), List.of(4, 3)),
            Map.entry(Pair.of(9, 2), List.of(9, 6, 2)),
            Map.entry(Pair.of(10, 2), List.of(10, 6, 2)),
            Map.entry(Pair.of(3, 11), List.of(3, 7, 11)),
            Map.entry(Pair.of(11, 3), List.of(11, 7, 3))
    ));

    public static HashSet<String> allTrains = new HashSet<>();

    public final String trainName;
    private final int start;
    private final int end;
    private int journeyIndex;

    /**
     * Initialise a new train
     *
     * @param trainName trainName
     * @param start     start section
     * @param end       destination section
     * @throws IllegalArgumentException if trying to initialise a train in service or
     *                                  if there is no path from start to end.
     */
    public Train(String trainName, int start, int end) throws IllegalArgumentException {
        if (!allPaths.containsKey(Pair.of(start, end))) {
            throw new IllegalArgumentException("No path exists between " + start + " and " + end);
        }
        if (allTrains.contains(trainName)) {
            throw new IllegalArgumentException("Train " + trainName + " is in service.");
        }
        this.trainName = trainName;
        this.start = start;
        this.end = end;
        this.journeyIndex = 0;
        allTrains.add(trainName);
    }

    /**
     * Get a list of section ID connecting start to end
     *
     * @return List<Integer> section path
     */
    public List<Integer> getPath() {
        return allPaths.get(Pair.of(this.start, this.end));
    }

    /**
     * Check if the current train is in service given a path index - i.e. on a section in the train network
     *
     * @param index a path index
     * @return true if train is in service
     */
    public boolean isInService(int index) {
        return index < this.getPath().size();
    }

    /**
     * Check if the current train is in service - i.e. on a section in the train network
     *
     * @return true if train is in service else false
     */
    public boolean isInService() {
        return isInService(journeyIndex);
    }

    /**
     * Get track section the train is occupying
     *
     * @return section ID if train is in service, else -1
     */
    public int getSection() {
        if (isInService()) {
            return getPath().get(journeyIndex);
        }
        return -1;
    }

    /**
     * Get the next track section the train is moving to
     *
     * @return section ID if train will be in service, else -1
     */
    public int getNextSection() {
        if (isInService(journeyIndex + 1)) {
            return getPath().get(journeyIndex + 1);
        }
        return -1;
    }

    /**
     * Get the destination of current train
     * @return destination section
     */
    public int getDestination(){
        return end;
    }

    /**
     * Move train to the next track section
     *
     * @throws IllegalArgumentException if trying to move a train not in the network
     */
    public void move() throws IllegalArgumentException {
        if (isInService()) {
            journeyIndex++;
            if (journeyIndex >= getPath().size()) {
                allTrains.remove(trainName);
            }
            return;
        }
        throw new IllegalArgumentException("Trying to move a train not in service.");
    }

    /**
     * Remove train from all trains
     */
    public static void removeTrain(String trainName){
        if (allTrains.contains(trainName)){
            allTrains.remove(trainName);
        }
    }
}


class Pair<U, V> {
    public final U first;       // the first field of a pair
    public final V second;      // the second field of a pair

    // Constructs a new pair with specified values
    public Pair(U first, V second) {
        this.first = first;
        this.second = second;
    }

    // Factory method for creating a typed Pair immutable instance
    public static <U, V> Pair<U, V> of(U a, V b) {
        // calls private constructor
        return new Pair<>(a, b);
    }

    @Override
    // Checks specified object is "equal to" the current object or not
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Pair<?, ?> pair = (Pair<?, ?>) obj;

        // call `equals()` method of the underlying objects
        if (!first.equals(pair.first)) {
            return false;
        }
        return second.equals(pair.second);
    }

    @Override
    // Computes hash code for an object to support hash tables
    public int hashCode() {
        // use hash codes of the underlying objects
        return 31 * first.hashCode() + second.hashCode();
    }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ")";
    }
    
    
    public static void main(String[] args) {
        Interlocking network = new InterlockingImpl();
        network.addTrain("t18",1,8);
        network.addTrain("t102", 10, 2);
        network.addTrain("t34", 3, 4);
        System.out.println(network);
        network.moveTrains(new String[]{"t18","t34","t102"});
        System.out.println(network);
        network.moveTrains(new String[]{"t18","t34","t102"});
        System.out.println(network);
        network.moveTrains(new String[]{"t18","t34","t102"});
        System.out.println(network);
    }
}