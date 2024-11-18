package com.paxos.council;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Member implements Runnable {
    private String memberId;
    private int port;
    private Map<String, Integer> members; // Map of memberId to port
    private ServerSocket serverSocket;

    // Paxos state variables
    private int proposalNumber = 0;
    private int highestProposalNumberSeen = 0;
    private int highestProposalNumberAccepted = 0;
    private String acceptedValue = null;

    // For collecting promises and accepted messages
    private Map<String, Message> promisesReceived = new ConcurrentHashMap<>();
    private Map<String, Message> acceptedReceived = new ConcurrentHashMap<>();

    // For simulation of delays and failures
    private Random random = new Random();
    private int memberIdNumber;

    // **New Variable to Keep Track of Final Accepted Value**
    private static String finalAcceptedValue = null;

    public Member(String memberId, int port, Map<String, Integer> members) {
        this.memberId = memberId;
        this.port = port;
        this.members = members;
        this.memberIdNumber = Integer.parseInt(memberId.substring(1));
    }

    @Override
    public void run() {
        // Start server to listen for messages
        try {
            serverSocket = new ServerSocket(port);
            while (true) {
                Socket socket = serverSocket.accept();
                // Handle incoming message
                handleIncomingMessage(socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleIncomingMessage(Socket socket) {
        // Read the message
        try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
            Message msg = (Message) ois.readObject();
            // Process the message
            processMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processMessage(Message msg) {
        switch (msg.getType()) {
            case PREPARE_REQUEST:
                handlePrepareRequest(msg);
                break;
            case PROMISE:
                handlePromise(msg);
                break;
            case ACCEPT_REQUEST:
                handleAcceptRequest(msg);
                break;
            case ACCEPTED:
                handleAccepted(msg);
                break;
            default:
                System.out.println("Unknown message type");
        }
    }

    private boolean simulateDelay() {
        // Simulate delays and failures based on member characteristics
        if (memberId.equals("M1")) {
            // M1 responds instantly
            return true;
        } else if (memberId.equals("M2")) {
            // M2 has poor internet
            int rand = random.nextInt(100);
            if (rand < 50) {
                // 50% chance of delay
                try {
                    Thread.sleep(5000); // 5 seconds delay
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return true;
            } else if (rand < 75) {
                // 25% chance of no response
                return false;
            } else {
                return true;
            }
        } else if (memberId.equals("M3")) {
            // M3 sometimes doesn't receive messages
            int rand = random.nextInt(100);
            if (rand < 30) {
                // 30% chance of not processing the message
                return false;
            } else {
                return true;
            }
        } else {
            // M4-M9 have varying response times
            try {
                int delay = random.nextInt(3000); // Up to 3 seconds delay
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return true;
        }
    }

    private void handlePrepareRequest(Message msg) {
        if (!simulateDelay()) {
            return;
        }
        int receivedProposalNumber = msg.getProposalNumber();

        if (receivedProposalNumber > highestProposalNumberSeen) {
            highestProposalNumberSeen = receivedProposalNumber;

            // Logging
            System.out.println("Phase 1 : Acceptor " + memberId + " received PREPARE from " + msg.getSenderId()
                    + " with proposal number " + receivedProposalNumber);

            // Send Promise
            Message promise = new Message(Message.MessageType.PROMISE);
            promise.setProposalNumber(receivedProposalNumber);
            promise.setProposerId(msg.getProposerId());
            promise.setLastAcceptedProposalNumber(highestProposalNumberAccepted);
            promise.setLastAcceptedValue(acceptedValue);
            promise.setSenderId(memberId);

            // Logging
            System.out.println("Phase 1 : Acceptor " + memberId + " sends PROMISE to " + msg.getSenderId());

            sendMessage(msg.getSenderId(), promise);
        } else {
            // Ignore or send negative response
            System.out.println("Phase 1 : Acceptor " + memberId + " ignores PREPARE from " + msg.getSenderId()
                    + " with proposal number " + receivedProposalNumber);
        }
    }

    private void handlePromise(Message msg) {
        String acceptorId = msg.getSenderId();
        promisesReceived.put(acceptorId, msg);

        // Logging
        System.out.println("Phase 2 : Proposer " + memberId + " received PROMISE from " + acceptorId);
    }

    private void handleAcceptRequest(Message msg) {
        if (!simulateDelay()) {
            return;
        }
        int receivedProposalNumber = msg.getProposalNumber();

        if (receivedProposalNumber >= highestProposalNumberSeen) {
            highestProposalNumberSeen = receivedProposalNumber;
            highestProposalNumberAccepted = receivedProposalNumber;
            acceptedValue = msg.getValue();

            // Logging
            System.out.println("Phase 3 : Acceptor " + memberId + " accepts value '" + acceptedValue
                    + "' from proposer " + msg.getProposerId());

            // Send Accepted
            Message accepted = new Message(Message.MessageType.ACCEPTED);
            accepted.setProposalNumber(receivedProposalNumber);
            accepted.setProposerId(msg.getProposerId());
            accepted.setValue(acceptedValue);
            accepted.setSenderId(memberId);

            // Logging
            System.out.println("Phase 3 : Acceptor " + memberId + " sends ACCEPTED to " + msg.getSenderId());

            sendMessage(msg.getSenderId(), accepted);
        } else {
            // Ignore or send negative response
            System.out.println("Phase 3 : Acceptor " + memberId + " rejects ACCEPT_REQUEST from " + msg.getSenderId());
        }
    }

    private void handleAccepted(Message msg) {
        String acceptorId = msg.getSenderId();
        acceptedReceived.put(acceptorId, msg);

        // Logging
        System.out.println("Phase 4 : Proposer " + memberId + " received ACCEPTED from " + acceptorId);
    }

    public void proposeValue(String value) {
        System.out.println("\n--------------- Voting:: " + memberId + " will send proposal. --------------");

        proposalNumber += 1; // Increment round
        int n = proposalNumber * 10 + memberIdNumber;

        // Reset the maps
        promisesReceived.clear();
        acceptedReceived.clear();

        // Logging
        System.out.println("Phase 1 : " + memberId + " starts Phase 1 - Prepare. Sending PREPARE to members with proposal number " + n);

        // Send PrepareRequest to all acceptors
        Message prepareRequest = new Message(Message.MessageType.PREPARE_REQUEST);
        prepareRequest.setProposalNumber(n);
        prepareRequest.setProposerId(memberId);

        for (String member : members.keySet()) {
            if (!member.equals(memberId)) {
                sendMessage(member, prepareRequest);
            }
        }

        // Wait for promises from majority
        int majority = (members.size() / 2) + 1;

        long startTime = System.currentTimeMillis();
        long timeout = 15000; // 15 seconds timeout

        while (promisesReceived.size() < majority && (System.currentTimeMillis() - startTime) < timeout) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Inside the proposeValue method, after receiving PROMISES from majority
        if (promisesReceived.size() >= majority) {
            // Proceed to Accept phase

            // Logging
            System.out.println("Phase 2 : " + memberId + " received PROMISES from majority.");

            String highestValue = null;
            int highestProposalNumber = -1;
            for (Message promise : promisesReceived.values()) {
                if (promise.getLastAcceptedProposalNumber() > highestProposalNumber) {
                    highestProposalNumber = promise.getLastAcceptedProposalNumber();
                    highestValue = promise.getLastAcceptedValue();
                }
            }
            // If any acceptor has already accepted a proposal, we must use that value
            if (highestValue != null) {
                System.out.println("Phase 2 : " + memberId + " learns about previously accepted value '" + highestValue + "' with proposal number " + highestProposalNumber);
                value = highestValue;
            } else {
                System.out.println("Phase 2 : " + memberId + " did not learn about any previously accepted value. Proceeding with own value '" + value + "'");
            }

            // Logging
            System.out.println("Phase 3: " + memberId + " starts Phase 3 - Accept. Sending ACCEPT_REQUEST with value '" + value + "' to members.");


            // Send AcceptRequest to acceptors
            Message acceptRequest = new Message(Message.MessageType.ACCEPT_REQUEST);
            acceptRequest.setProposalNumber(n);
            acceptRequest.setProposerId(memberId);
            acceptRequest.setValue(value);

            for (String member : members.keySet()) {
                if (!member.equals(memberId)) {
                    sendMessage(member, acceptRequest);
                }
            }

            // Wait for Accepted messages from majority
            startTime = System.currentTimeMillis();
            while (acceptedReceived.size() < majority && (System.currentTimeMillis() - startTime) < timeout) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (acceptedReceived.size() >= majority) {
                // Value has been chosen
                finalAcceptedValue = value; // Update the static variable

                System.out.println("\n!!!!!!!!!!!     Final value accepted is " + value + " by proposer " + memberId + " !!!!!!!!!!!!!\n");

                // **New Logging: Announce the winner**
                System.out.println("************     " + value + " has been elected as Council President!     ************\n");
            } else {
                System.out.println("[" + memberId + "] Failed to reach consensus on value: " + value);
            }

        } else {
            System.out.println("[" + memberId + "] Failed to receive promises from majority");
        }
    }

    private void sendMessage(String toMemberId, Message msg) {
        int toPort = members.get(toMemberId);
        msg.setSenderId(memberId);
        msg.setReceiverId(toMemberId);
        try (Socket socket = new Socket("localhost", toPort);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {
            oos.writeObject(msg);
        } catch (IOException e) {
            // e.printStackTrace();
        }
    }
}




