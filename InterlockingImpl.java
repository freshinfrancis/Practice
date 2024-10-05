
import java.util.*;

// Implementation of the Interlocking interface
public class InterlockingImpl implements Interlocking {
    private final Map<Integer, Section> sections;
    private final Map<String, Train> trains;

    public InterlockingImpl() {
        sections = new HashMap<>();
        for (int i = 1; i <= 11; i++) {
            sections.put(i, new Section(i));
        }
        trains = new HashMap<>();
    }

    @Override
    public void addTrain(String trainName, int entryTrackSection, int destinationTrackSection)
            throws IllegalArgumentException {
        if (trains.containsKey(trainName)) {
            throw new IllegalArgumentException("Train " + trainName + " is already in service.");
        }
        Train newTrain = new Train(trainName, entryTrackSection, destinationTrackSection);
        sections.get(entryTrackSection).addTrain(newTrain);
        trains.put(trainName, newTrain);
    }

    @Override
    public int moveTrains(String[] trainNames) throws IllegalArgumentException {
        int count = 0;

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
        return count;
    }

    private boolean moveTrain(Train train) {
        Section currentSection = sections.get(train.getSection());
        Section nextSection = sections.get(train.getNextSection());

        // Special case: Train501 (from 4 to 3) must wait for all trains from 11 to 3
        if (train.getSection() == 4 && train.getNextSection() == 3 && hasPriorityTrainOnPath(11, 3)) {
            System.out.println("Train " + train.trainName + " is waiting at section 4 for priority trains from 11 to 3.");
            return true; // Do not move Train501 yet
        }

        // Check if train can move
        if (isMovable(train)) {
            currentSection.moveTrain();

            if (nextSection != null) {
                if (nextSection.isOccupied()) {
                    nextSection.addToQueue(train);
                    System.out.println("Train " + train.trainName + " is waiting for section " + nextSection.sectionID);
                } else {
                    nextSection.addTrain(train);
                    System.out.println("Train " + train.trainName + " moved to section " + nextSection.sectionID);
                }
            } else {
                trains.remove(train.trainName);
                System.out.println("Train: " + train.trainName + " has exited the system from section " + train.getSection());
            }
            return true;
        }
        return false;
    }

    private boolean hasPriorityTrainOnPath(int fromSection, int toSection) {
        // Loop through all trains and check if any train is moving from section 'fromSection' to 'toSection'
        for (Train train : trains.values()) {
            if (train.getSection() == fromSection && train.getNextSection() == toSection) {
                return false; // A priority train exists on the path
            }
        }
        return true; // No priority train on the path
    }

    private boolean isMovable(Train train) {
        Section nextSection = sections.get(train.getNextSection());

        if (train.isWaiting()) {
            return false;
        }

        if (nextSection == null) {
            return true;
        }

        return !nextSection.isOccupied();
    }

    @Override
    public String getSection(int trackSection) throws IllegalArgumentException {
        if (!sections.containsKey(trackSection)) {
            throw new IllegalArgumentException("Track section does not exist");
        }
        return sections.get(trackSection).getTrainName();
    }

    @Override
    public int getTrain(String trainName) throws IllegalArgumentException {
        if (!trains.containsKey(trainName)) {
            throw new IllegalArgumentException("Train name does not exist");
        }
        return trains.get(trainName).getSection();
    }

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
    Train currentTrain;
    Queue<Train> trainQueue;

    public Section(int sectionID) {
        this.sectionID = sectionID;
        this.currentTrain = null;
        this.trainQueue = new LinkedList<>();
    }

    public boolean isOccupied() {
        return currentTrain != null;
    }

    public void addTrain(Train train) {
        if (isOccupied()) {
            trainQueue.add(train);
        } else {
            currentTrain = train;
        }
    }

    public void addToQueue(Train train) {
        trainQueue.add(train);
    }

    public void moveTrain() {
        if (this.currentTrain != null) {
            this.currentTrain.move();
            this.currentTrain = null;

            if (!trainQueue.isEmpty()) {
                currentTrain = trainQueue.poll();
                System.out.println("Train " + currentTrain.trainName + " moved from queue to section " + sectionID);
            }
        }
    }

    public String getTrainName() {
        return currentTrain != null ? currentTrain.trainName : null;
    }
}

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
    private boolean isWaiting;

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

class Pair<U, V> {
    public final U first;
    public final V second;

    private Pair(U first, V second) {
        this.first = first;
        this.second = second;
    }

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
        Interlocking network = new InterlockingImpl();

        // Add some trains to the network
        network.addTrain("Train501", 4, 3);
        network.addTrain("Train502", 11, 3);
        network.addTrain("Train503", 11, 3);
        network.addTrain("Train504", 11, 3);

        // Display train details and network state
        System.out.println(network);

        // Move trains and display network state after each move
        network.moveTrains(new String[] { "Train501", "Train502", "Train503", "Train504" });
        System.out.println(network);
        network.moveTrains(new String[] { "Train501", "Train502", "Train503", "Train504" });
        System.out.println(network);
        network.moveTrains(new String[] { "Train501", "Train502", "Train503", "Train504" });
        System.out.println(network);
    }
}
