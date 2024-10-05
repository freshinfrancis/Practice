import java.util.*;

// Implementation of the Interlocking interface
public class InterlockingImpl implements Interlocking {
    // Sections represent different parts of the track network
    private final Map<Integer, Section> sections;
    // A map of active trains in the network, with train names as keys
    private final Map<String, Train> trains;

    // Constructor to initialize the sections, trains
    public InterlockingImpl() {
        // Initialize sections (track segments) numbered 1 through 11
        sections = new HashMap<>();
        for (int i = 1; i <= 11; i++) {
            sections.put(i, new Section(i));
        }
        // Initialize the train map
        trains = new HashMap<>();
    }

    /**
     * Adds a train to the rail corridor.
     * @param trainName Name of the train.
     * @param entryTrackSection Starting section of the train.
     * @param destinationTrackSection Destination section of the train.
     * @throws IllegalArgumentException if train name is already in use or path doesn't exist.
     */
    @Override
    public void addTrain(String trainName, int entryTrackSection, int destinationTrackSection)
            throws IllegalArgumentException {
        // Check if a train with this name already exists
        if (trains.containsKey(trainName)) {
            throw new IllegalArgumentException("Train " + trainName + " is already in service.");
        }

        // Add the train to the system
        Train newTrain = new Train(trainName, entryTrackSection, destinationTrackSection);
        sections.get(entryTrackSection).addTrain(newTrain); // Place the train in the entry section
        trains.put(trainName, newTrain); // Register the train in the map
    }

    /**
     * Moves the listed trains to the next track section.
     * @param trainNames List of train names to move.
     * @return Number of trains successfully moved.
     * @throws IllegalArgumentException if a train doesn't exist.
     */
    @Override
    public int moveTrains(String[] trainNames) throws IllegalArgumentException {
        int count = 0; // Counter for successful moves

        for (String name : trainNames) {
            if (!trains.containsKey(name)) {
                throw new IllegalArgumentException("Train " + name + " does not exist or is no longer in the rail corridor.");
            }
            Train train = trains.get(name);
            boolean isMoved = moveTrain(train);
            if (isMoved) {
                count++;
            }
        }
        return count; // Return the number of successful moves
    }

    /**
     * Moves a single train if allowed.
     * @param train The train to move.
     * @return true if the train is moved, false otherwise.
     */
    private boolean moveTrain(Train train) {
        if (isMovable(train)) {
            Section currentSection = sections.get(train.getSection());
            Section nextSection = sections.get(train.getNextSection());

            // Move the train out of the current section
            currentSection.moveTrain();

            // Move the train into the next section, or mark it as waiting if it's at the destination
            if (nextSection != null) {
                nextSection.addTrain(train); // Move the train to the next section
            } else if (!train.isWaiting()) {
                // Mark the train as waiting at its final destination
                train.setWaiting();
            }

            return true; // Train was successfully moved
        }
        return false; // Train could not be moved (next section was occupied)
    }

    /**
     * Checks if a train can be moved based on section occupancy.
     * @param train The train to check.
     * @return true if the train can be moved, false otherwise.
     */
    private boolean isMovable(Train train) {
        Section nextSection = sections.get(train.getNextSection());

        // If the train is at its destination and is waiting, it cannot move further
        if (train.isWaiting()) {
            return false;
        }

        // If the next section is null (exit point), the train can move
        if (nextSection == null) {
            return true;
        }

        // If the next section is occupied, the train cannot move
        if (nextSection.isOccupied()) {
            return false;
        }

        return true; // Train can move
    }

    /**
     * Returns the name of the train occupying the given section.
     * @param trackSection The track section to check.
     * @return The name of the train in the section, or null if empty.
     * @throws IllegalArgumentException if the section does not exist.
     */
    @Override
    public String getSection(int trackSection) throws IllegalArgumentException {
        if (!sections.containsKey(trackSection)) {
            throw new IllegalArgumentException("Track section does not exist");
        }
        return sections.get(trackSection).getTrainName(); // Get the train name in the section
    }

    /**
     * Returns the current section occupied by a given train.
     * @param trainName The name of the train.
     * @return The section the train is occupying.
     * @throws IllegalArgumentException if the train does not exist.
     */
    @Override
    public int getTrain(String trainName) throws IllegalArgumentException {
        if (!trains.containsKey(trainName)) {
            throw new IllegalArgumentException("Train name does not exist");
        }
        return trains.get(trainName).getSection(); // Get the current section of the train
    }

    /**
     * Helper method to print the current state of the track network.
     * Displays the occupancy of each track section.
     */
    @Override
    public String toString() {
        StringBuilder repr = new StringBuilder();
        String format = "%-15s|%-15s|%-15s\n"; // Formatting for output

        // Print the occupancy of sections
        repr.append(String.format(format, " ", "4: " + printSection(4), "8: " + printSection(8)));
        repr.append(String.format(format, "1: " + printSection(1), "5: " + printSection(5), "9: " + printSection(9)));
        repr.append(String.format(format, "2: " + printSection(2), "6: " + printSection(6), "10: " + printSection(10)));
        repr.append(String.format(format, "3: " + printSection(3), "7: " + printSection(7), "11: " + printSection(11)));

        return repr.toString(); // Return the formatted string representation
    }

    // Helper method to get the train occupying a given section, or a blank if empty
    private String printSection(int trackSection) {
        String trainName = getSection(trackSection);
        return trainName != null ? trainName : " ";
    }
}

// Represents a section of the track where a train can be located
class Section {
    int sectionID; // Unique ID for the section
    Train currentTrain; // The train currently in this section, if any
    Queue<Train> trainQueue; // Queue to handle multiple trains passing through this section

    public Section(int sectionID) {
        this.sectionID = sectionID;
        this.currentTrain = null; // Section starts empty
        this.trainQueue = new LinkedList<>(); // Initialize the queue for handling multiple trains
    }

    // Checks if the section is occupied by a train
    public boolean isOccupied() {
        return currentTrain != null;
    }

    // Adds a train to this section's queue if the section is occupied, otherwise places the train directly
    public void addTrain(Train train) {
        if (isOccupied()) {
            // If the section is occupied, add the train to the queue
            trainQueue.add(train);
        } else {
            // If the section is not occupied, place the train in the section
            currentTrain = train;
        }
    }

    // Moves the current train out of this section and checks if another train is in the queue
    public void moveTrain() {
        if (this.currentTrain != null) {
            this.currentTrain.move(); // Move the train to the next section
            this.currentTrain = null; // Empty this section

            // Check if there are any trains waiting in the queue
            if (!trainQueue.isEmpty()) {
                currentTrain = trainQueue.poll(); // Move the next train from the queue into the section
            }
        }
    }

    // Gets the name of the train in this section, or null if none
    public String getTrainName() {
        return currentTrain != null ? currentTrain.trainName : null;
    }
}

// Represents a train in the network
class Train {
    public static final Map<Pair<Integer, Integer>, List<Integer>> allPaths = Map.ofEntries(
        Map.entry(Pair.of(1, 8), List.of(1, 5, 8)),
        Map.entry(Pair.of(1, 9), List.of(1, 5, 9)),
        Map.entry(Pair.of(3, 4), List.of(3, 4)),
        Map.entry(Pair.of(4, 3), List.of(4, 3)),
        Map.entry(Pair.of(9, 2), List.of(9, 6, 2)),
        Map.entry(Pair.of(10, 2), List.of(10, 6, 2)),
        Map.entry(Pair.of(3, 11), List.of(3, 7, 11)),
        Map.entry(Pair.of(11, 3), List.of(11, 7, 3))
    );

    public final String trainName;
    private final int start;
    private final int end;
    private int journeyIndex;
    private boolean isWaiting; // Indicates whether the train is waiting at its destination

    public Train(String trainName, int start, int end) throws IllegalArgumentException {
        if (!allPaths.containsKey(Pair.of(start, end))) {
            throw new IllegalArgumentException("No path exists between " + start + " and " + end);
        }
        this.trainName = trainName;
        this.start = start;
        this.end = end;
        this.journeyIndex = 0;
        this.isWaiting = false;
    }

    public List<Integer> getPath() {
        return allPaths.get(Pair.of(this.start, this.end));
    }

    public boolean isInService() {
        return journeyIndex < getPath().size();
    }

    public void setWaiting() {
        isWaiting = true;
    }

    public boolean isWaiting() {
        return isWaiting;
    }

    public int getSection() {
        return isInService() ? getPath().get(journeyIndex) : end;
    }

    public int getNextSection() {
        return (journeyIndex + 1) < getPath().size() ? getPath().get(journeyIndex + 1) : -1;
    }

    public int getDestination() {
        return end;
    }

    public void move() {
        if (isInService()) {
            journeyIndex++;
        } else {
            setWaiting();
        }
    }
}

// Utility class for creating and managing pairs of values
class Pair<U, V> {
    public final U first; // First value in the pair
    public final V second; // Second value in the pair

    // Constructs a new pair with specified values
    private Pair(U first, V second) {
        this.first = first;
        this.second = second;
    }

    // Factory method for creating a typed Pair immutable instance
    public static <U, V> Pair<U, V> of(U a, V b) {
        return new Pair<>(a, b);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) obj;
        if (!first.equals(pair.first)) return false;
        return second.equals(pair.second);
    }

    @Override
    public int hashCode() {
        return 31 * first.hashCode() + second.hashCode();
    }

    public static void main(String[] args) {
        // Create a new interlocking network
        Interlocking network = new InterlockingImpl();

        // Add some trains to the network
        network.addTrain("Train163", 10, 2);
        network.addTrain("Train164", 10, 2);
        network.addTrain("Train165", 10, 2);

        // Display train details and network state
        System.out.println(network);

        // Move trains and display network state after each move
        network.moveTrains(new String[] { "Train163", "Train164", "Train165" });
        System.out.println(network);
        network.moveTrains(new String[] { "Train163", "Train164", "Train165" });
        System.out.println(network);
        network.moveTrains(new String[] { "Train163", "Train164", "Train165" });
        System.out.println(network);
    }
}
