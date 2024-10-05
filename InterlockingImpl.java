import java.util.*;

public class InterlockingImpl implements Interlocking {
    private final Map<Integer, Section> sections;
    private final Map<String, Train> trains;

    // Constraints and priority rules
    private final Map<Pair<Integer, Integer>, Set<Pair<Integer, Integer>>> constraints;
    private final Map<Pair<Integer, Integer>, Set<Pair<Integer, Integer>>> priority;
    private final Set<Pair<Integer, Integer>> prioritySet;

    public InterlockingImpl() {
        sections = new HashMap<>();
        for (int i = 1; i <= 11; i++) {
            sections.put(i, new Section(i));
        }
        trains = new HashMap<>();

        // Initialize constraints
        constraints = new HashMap<>(Map.ofEntries(
                Map.entry(Pair.of(4, 3), Set.of(Pair.of(3, 4))),
                Map.entry(Pair.of(3, 4), Set.of(Pair.of(4, 3))),
                Map.entry(Pair.of(3, 11), Set.of(Pair.of(11, 3), Pair.of(7, 3))),
                Map.entry(Pair.of(11, 3), Set.of(Pair.of(3, 11), Pair.of(7, 11))),
                Map.entry(Pair.of(1, 9), Set.of(Pair.of(9, 2))),
                Map.entry(Pair.of(9, 2), Set.of(Pair.of(1, 9), Pair.of(5, 9)))
        ));

        // Initialize priority rules
        priority = new HashMap<>(Map.ofEntries(
                Map.entry(Pair.of(3, 4), Set.of(Pair.of(1, 5), Pair.of(6, 2))),
                Map.entry(Pair.of(4, 3), Set.of(Pair.of(1, 5), Pair.of(6, 2))),
                Map.entry(Pair.of(9, 6), Set.of(Pair.of(5, 8), Pair.of(10, 6)))
        ));

        // Create a set of all priority movements
        prioritySet = new HashSet<>();
        for (Set<Pair<Integer, Integer>> value : priority.values()) {
            prioritySet.addAll(value);
        }
    }

    /**
     * Adds a train to the rail corridor.
     */
    @Override
    public void addTrain(String trainName, int entryTrackSection, int destinationTrackSection)
            throws IllegalArgumentException, IllegalStateException {
        // Check if train name is already in use
        if (trains.containsKey(trainName)) {
            throw new IllegalArgumentException("Train " + trainName + " is already in service.");
        }

        // Check if any constraint is violated
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
     * Moves the listed trains to the next track section.
     */
    @Override
    public int moveTrains(String[] trainNames) throws IllegalArgumentException {
        int count = 0;
        List<String> priorityNames = new ArrayList<>();
        List<String> nonPriorityNames = new ArrayList<>();

        // First pass - check for exceptions and prioritize trains
        for (String name : trainNames) {
            if (!trains.containsKey(name)) {
                throw new IllegalArgumentException("Train " + name + " does not exist or is no longer in the rail corridor.");
            }
            Train train = trains.get(name);
            Pair<Integer, Integer> key = Pair.of(train.getSection(), train.getNextSection());
            if (prioritySet.contains(key)) {
                priorityNames.add(name);
            } else {
                nonPriorityNames.add(name);
            }
        }
        priorityNames.addAll(nonPriorityNames);

        // Second pass - move trains
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
     * Moves a single train if permitted.
     */
    private boolean moveTrain(Train train) {
        if (isMovable(train)) {
            Section currentSection = sections.get(train.getSection());
            Section nextSection = sections.get(train.getNextSection());

            currentSection.moveTrain();

            if (nextSection != null) {
                nextSection.addTrain(train);
            } else {
                // Train has exited the network
                trains.remove(train.trainName);
            }
            return true;
        }
        return false;
    }

    /**
     * Checks if a train can be moved based on constraints and occupancy.
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
     * Checks if there is a priority target that should be given way to.
     */
    private boolean hasPriorityTarget(Pair<Integer, Integer> key) {
        Set<Pair<Integer, Integer>> priorityTargets = priority.get(key);
        if (priorityTargets == null) {
            return false;
        }
        for (Pair<Integer, Integer> target : priorityTargets) {
            Section section = sections.get(target.first);
            int targetDestination = target.second;
            if (section.isOccupied()) {
                if (section.train.getNextSection() == targetDestination) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the name of the Train currently occupying a given track section.
     */
    @Override
    public String getSection(int trackSection) throws IllegalArgumentException {
        if (!sections.containsKey(trackSection)) {
            throw new IllegalArgumentException("Track section does not exist");
        }
        return sections.get(trackSection).getTrainName();
    }

    /**
     * Returns the track section that a given train is occupying.
     */
    @Override
    public int getTrain(String trainName) throws IllegalArgumentException {
        if (!trains.containsKey(trainName)) {
            throw new IllegalArgumentException("Train name does not exist");
        }
        return trains.get(trainName).getSection();
    }

    /**
     * Helper method to print the current state of the network.
     */
    @Override
    public String toString() {
        StringBuilder repr = new StringBuilder();
        String format = "%-15s|%-15s|%-15s\n";

        repr.append(String.format(format, " ", "4: " + printSection(4), "8: " + printSection(8)));
        repr.append(String.format(format, "1: " + printSection(1), "5: " + printSection(5), "9: " + printSection(9)));
        repr.append(String.format(format, "2: " + printSection(2), "6: " + printSection(6), "10: " + printSection(10)));
        repr.append(String.format(format, "3: " + printSection(3), "7: " + printSection(7), "11: " + printSection(11)));

        return repr.toString();
    }

    private String printSection(int trackSection) {
        String trainName = getSection(trackSection);
        return trainName != null ? trainName : " ";
    }
}

class Section {
    int sectionID;
    Train train;

    public Section(int sectionID) {
        this.sectionID = sectionID;
        this.train = null;
    }

    public boolean isOccupied() {
        return train != null;
    }

    public void addTrain(Train train) {
        if (isOccupied() && this.train != train) {
            throw new IllegalStateException("Track " + sectionID + " is currently occupied.");
        }
        this.train = train;
    }

    public String getTrainName() {
        return train != null ? train.trainName : null;
    }

    public void moveTrain() {
        if (this.train != null) {
            this.train.move();
            this.train = null;
        }
    }
}

class Train {
    // All possible paths in the network
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

    public Train(String trainName, int start, int end) throws IllegalArgumentException {
        if (!allPaths.containsKey(Pair.of(start, end))) {
            throw new IllegalArgumentException("No path exists between " + start + " and " + end);
        }
        this.trainName = trainName;
        this.start = start;
        this.end = end;
        this.journeyIndex = 0;
    }

    public List<Integer> getPath() {
        return allPaths.get(Pair.of(this.start, this.end));
    }

    public boolean isInService() {
        return journeyIndex < getPath().size();
    }

    public int getSection() {
        return isInService() ? getPath().get(journeyIndex) : -1;
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
            throw new IllegalArgumentException("Trying to move a train not in service.");
        }
    }
}

class Pair<U, V> {
    public final U first;
    public final V second;

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
}


