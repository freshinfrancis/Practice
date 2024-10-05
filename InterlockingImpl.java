import java.util.*;

// Implementation of the Interlocking interface
public class InterlockingImpl implements Interlocking {
    // Sections represent different parts of the track network
    private final Map<Integer, Section> sections;
    // A map of active trains in the network, with train names as keys
    private final Map<String, Train> trains;

    // Constraints and priority rules for movements between sections
    // Constraints restrict certain train movements based on occupancy
    private final Map<Pair<Integer, Integer>, Set<Pair<Integer, Integer>>> constraints;
    // Priority rules specify which train movements should take precedence
    private final Map<Pair<Integer, Integer>, Set<Pair<Integer, Integer>>> priority;
    // A set that stores all priority train movements
    private final Set<Pair<Integer, Integer>> prioritySet;

    // Constructor to initialize the sections, trains, constraints, and priority rules
    public InterlockingImpl() {
        // Initialize sections (track segments) numbered 1 through 11
        sections = new HashMap<>();
        for (int i = 1; i <= 11; i++) {
            sections.put(i, new Section(i));
        }
        // Initialize the train map
        trains = new HashMap<>();

        // Initialize constraints between track sections
        constraints = new HashMap<>(Map.ofEntries(
            // These constraints specify which train movements cannot happen concurrently
            Map.entry(Pair.of(4, 3), Set.of(Pair.of(3, 4))),
            Map.entry(Pair.of(3, 4), Set.of(Pair.of(4, 3))),
            Map.entry(Pair.of(3, 11), Set.of(Pair.of(11, 3), Pair.of(7, 3))),
            Map.entry(Pair.of(11, 3), Set.of(Pair.of(3, 11), Pair.of(7, 11))),
            Map.entry(Pair.of(1, 9), Set.of(Pair.of(9, 2))),
            Map.entry(Pair.of(9, 2), Set.of(Pair.of(1, 9), Pair.of(5, 9)))
        ));

        // Initialize priority rules
        priority = new HashMap<>(Map.ofEntries(
            // These priority rules specify which movements have priority over others
            Map.entry(Pair.of(3, 4), Set.of(Pair.of(1, 5), Pair.of(6, 2))),
            Map.entry(Pair.of(4, 3), Set.of(Pair.of(1, 5), Pair.of(6, 2))),
            Map.entry(Pair.of(9, 6), Set.of(Pair.of(5, 8), Pair.of(10, 6)))
        ));

        // Populate the priority set with all movements marked as having priority
        prioritySet = new HashSet<>();
        for (Set<Pair<Integer, Integer>> value : priority.values()) {
            prioritySet.addAll(value);
        }
    }

    /**
     * Adds a train to the rail corridor.
     * @param trainName Name of the train.
     * @param entryTrackSection Starting section of the train.
     * @param destinationTrackSection Destination section of the train.
     * @throws IllegalArgumentException if train name is already in use or path doesn't exist.
     * @throws IllegalStateException if constraints are violated by adding this train.
     */
    @Override
    public void addTrain(String trainName, int entryTrackSection, int destinationTrackSection)
            throws IllegalArgumentException, IllegalStateException {
        // Check if a train with this name already exists
        if (trains.containsKey(trainName)) {
            throw new IllegalArgumentException("Train " + trainName + " is already in service.");
        }

        // Check if any constraints are violated by this new train
        Pair<Integer, Integer> key = Pair.of(entryTrackSection, destinationTrackSection);
        Set<Pair<Integer, Integer>> value = constraints.get(key);
        if (value != null) {
            for (Pair<Integer, Integer> c : value) {
                Section constraintSection = sections.get(c.first);
                int targetSection = c.second;
                // Check if the constrained section is occupied by a train headed for the same target
                if (constraintSection.currentTrain != null && 
                    constraintSection.currentTrain.getDestination() == targetSection) {
                    throw new IllegalStateException("Constraint not met: trying to add a train heading for " +
                            destinationTrackSection + " from " + entryTrackSection + ", but a train heading for "
                            + targetSection + " is on " + c.first);
                }
            }
        }

        // If all checks pass, add the train to the system
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
        List<String> priorityNames = new ArrayList<>();
        List<String> nonPriorityNames = new ArrayList<>();

        // First pass: check exceptions and separate priority from non-priority trains
        for (String name : trainNames) {
            if (!trains.containsKey(name)) {
                throw new IllegalArgumentException("Train " + name + " does not exist or is no longer in the rail corridor.");
            }
            Train train = trains.get(name);
            Pair<Integer, Integer> key = Pair.of(train.getSection(), train.getNextSection());
            // Check if the train movement is a priority movement
            if (prioritySet.contains(key)) {
                priorityNames.add(name);
            } else {
                nonPriorityNames.add(name);
            }
        }
        // Prioritize trains in the movement order
        priorityNames.addAll(nonPriorityNames);

        // Second pass: actually move the trains
        for (String name : priorityNames) {
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
        // Check if the train can be moved
        if (isMovable(train)) {
            // Get current and next sections for the train
            Section currentSection = sections.get(train.getSection());
            Section nextSection = sections.get(train.getNextSection());

            // Move the train out of the current section
            currentSection.moveTrain();

            // Move the train into the next section, or remove it if it's at the end of the path
            if (nextSection != null) {
                nextSection.addTrain(train);
            } else {
                // Train has reached its destination and exited the network
                trains.remove(train.trainName);
            }
            return true; // Train was successfully moved
        }
        return false; // Train could not be moved
    }

    /**
     * Checks if a train can be moved based on constraints and section occupancy.
     * @param train The train to check.
     * @return true if the train can be moved, false otherwise.
     */
    private boolean isMovable(Train train) {
        Section nextSection = sections.get(train.getNextSection());
        Pair<Integer, Integer> key = Pair.of(train.getSection(), train.getNextSection());
    
        // If nextSection is the exit point
        if (nextSection == null) {
            return true;
        }
    
        // If nextSection is occupied or if a priority target is present
        if (nextSection.isOccupied() || hasPriorityTarget(key)) {
            return false;
        }
        return true;
    }

    /**
     * Checks if a higher priority train is present, blocking the movement.
     * @param key The current movement key (current section -> next section).
     * @return true if a priority conflict exists, false otherwise.
     */
    private boolean hasPriorityTarget(Pair<Integer, Integer> key) {
        Set<Pair<Integer, Integer>> priorityTargets = priority.get(key);
        if (priorityTargets == null) {
            return false;
        }
        // Check if any priority trains are blocking the movement
        for (Pair<Integer, Integer> target : priorityTargets) {
            Section section = sections.get(target.first);
            int targetDestination = target.second;
            // Use currentTrain instead of train
            if (section.currentTrain != null) {
                if (section.currentTrain.getNextSection() == targetDestination) {
                    return true; // A priority train is blocking the movement
                }
            }
        }
        return false; // No priority conflict
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
    // Static map defining all possible paths between track sections
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

    public final String trainName; // Unique name for the train
    private final int start; // Starting section of the train
    private final int end; // Destination section of the train
    private int journeyIndex; // Index tracking the train's position on its path

    public Train(String trainName, int start, int end) throws IllegalArgumentException {
        // Ensure the start and end sections are connected by a valid path
        if (!allPaths.containsKey(Pair.of(start, end))) {
            throw new IllegalArgumentException("No path exists between " + start + " and " + end);
        }
        this.trainName = trainName;
        this.start = start;
        this.end = end;
        this.journeyIndex = 0; // Start at the beginning of the journey
    }

    // Returns the full path the train will follow
    public List<Integer> getPath() {
        return allPaths.get(Pair.of(this.start, this.end));
    }

    // Checks if the train is still in service (hasn't completed its journey)
    public boolean isInService() {
        return journeyIndex < getPath().size();
    }

    // Gets the current section the train is in
    public int getSection() {
        return isInService() ? getPath().get(journeyIndex) : -1;
    }

    // Gets the next section the train will move to
    public int getNextSection() {
        return (journeyIndex + 1) < getPath().size() ? getPath().get(journeyIndex + 1) : -1;
    }

    // Gets the final destination section of the train
    public int getDestination() {
        return end;
    }

    // Moves the train to the next section in its path
    public void move() {
        if (isInService()) {
            journeyIndex++; // Move to the next section
        } else {
            throw new IllegalArgumentException("Trying to move a train not in service.");
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
        network.addTrain("t420",1,8);
        network.addTrain("t421", 1, 9);
        network.addTrain("t422", 10, 2);
        network.addTrain("t423", 11, 3);
        network.addTrain("t424", 11, 3);



        // Display train details and network state
        System.out.println(network.getTrain("t420"));
        System.out.println(network.getSection(1));
        System.out.println(network);

        // Move trains and display network state after each move
        network.moveTrains(new String[]{"t420","t421","t422","t423","t424"});
        System.out.println(network);
        network.moveTrains(new String[]{"t420","t421","t422","t423","t424"});
        System.out.println(network);
        network.moveTrains(new String[]{"t420","t421","t422","t423","t424"});
        System.out.println(network);
    }
}
