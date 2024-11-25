# a1942887

## Paxos Council Election (Assignment 3)
Project Overview
This Java project simulates the Paxos consensus algorithm through a mock council election scenario. It demonstrates how multiple nodes (members) in a distributed system can agree on a single value, despite the presence of network delays, failures, and concurrent operations. This project is ideal for understanding the fundamental mechanics of the Paxos protocol in distributed computing.

### Key Components and Their Roles

##### PaxosElection.java

Description: This is the main driver class for the simulation. It sets up the simulation environment, initializes all council members, and executes various test scenarios to demonstrate how the Paxos algorithm handles simultaneous proposals, immediate responses, and varied response scenarios including artificial delays and member failures.
Functionality:
Initializes member instances with unique identifiers and network ports.
Simulates three primary test scenarios: simultaneous proposals, immediate unanimous responses, and responses with varied delays or failures.
Manages the lifecycle of the simulation including the setup, execution of test cases, and orderly shutdown of member instances.

##### Member.java

Description: Represents a council member in the Paxos election. Each member can act as a proposer, acceptor, and learner, which are roles defined within the Paxos protocol to facilitate the consensus process.
Functionality:
Handles incoming network connections and messages related to the Paxos protocol phases: prepare, promise, accept request, and accepted.
Maintains internal state to track the highest proposal numbers seen and accepted, as well as the value associated with those proposals.
Manages network communications to send and receive Paxos-related messages to and from other members.
Can simulate network delays and failures to test the robustness of the consensus process under adverse conditions.

##### Message.java

Description: Defines the structure of messages exchanged between members during the Paxos consensus process. Each message type corresponds to a step in the Paxos protocol.
Functionality:
Encapsulates data fields such as message type, proposal number, proposer ID, and the value being proposed or accepted.
Provides serialization capabilities to facilitate easy transmission of messages over network sockets.

# How to Run Project
## Running Project in Eclipse
Prerequisites
Java Development Kit (JDK): Ensure you have JDK installed on your system. You can download it from Oracle's website or other JDK providers like AdoptOpenJDK.
Eclipse IDE: Install Eclipse IDE for Java Developers from Eclipse Downloads. Make sure it includes support for Maven; most Java bundles include this by default.
Maven: Eclipse's embedded Maven should suffice, but you can also install Maven separately on your system if required.
Importing the Project into Eclipse
Open Eclipse and select a workspace.
Import the Maven project:
Go to File > Import...
Select Existing Maven Projects under the Maven folder.
Click Next.
Browse to the root directory of your Maven project where the pom.xml file is located.
Ensure that the pom.xml file is selected, then click Finish.
Running the Project
Through Eclipse
Run the project directly from Eclipse:

Right-click on the project in the Project Explorer.
Go to Run As > Maven build...
In the Goals field, type clean install to clean and build the project.
Click Run to execute the build. This will compile the code, run any tests, and package the application (if applicable).
Running a specific class with a main method:

Right-click on the class in the Project Explorer.
Go to Run As > Java Application.
Using Maven Commands in Eclipse
Open the Maven Console in Eclipse:
Go to Window > Show View > Console.
Click the Open Console drop-down in the Console view.
Select Maven Console.
Run Maven commands:
Right-click on the project in the Project Explorer.
Select Run As > Maven build...
Enter any Maven commands (e.g., clean, compile, test, package) in the Goals field.
Click Run.
Command Line Usage
You can also run Maven commands directly from the command line outside of Eclipse:

bash
Copy code
#### Navigate to the project directory
cd /path/to/your/project

#### Clean the project
mvn clean

#### Compile the project
mvn compile

#### Run tests
mvn test

#### Package the project
mvn package

#### Install the project to your local Maven repository
mvn install
