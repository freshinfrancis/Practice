package com.paxos.council;

import java.util.HashMap;
import java.util.Map;

import java.util.HashMap;
import java.util.Map;

import java.util.HashMap;
import java.util.Map;

public class PaxosElection {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("------------- Start Council Election ----------------");

        // Map of memberId to port
        Map<String, Integer> members = new HashMap<>();
        members.put("M1", 5001);
        members.put("M2", 5002);
        members.put("M3", 5003);
        members.put("M4", 5004);
        members.put("M5", 5005);
        members.put("M6", 5006);
        members.put("M7", 5007);
        members.put("M8", 5008);
        members.put("M9", 5009);

        Map<String, Member> memberInstances = new HashMap<>();

        // Create and start member threads
        for (Map.Entry<String, Integer> entry : members.entrySet()) {
            Member member = new Member(entry.getKey(), entry.getValue(), members);
            Thread thread = new Thread(member);
            thread.start();
            memberInstances.put(entry.getKey(), member);
        }

        // Let the members start up
        Thread.sleep(2000);

        // Simulate M1 proposing
        Member m1 = memberInstances.get("M1");
        new Thread(() -> m1.proposeValue("M1")).start();

        // Let the simulation run for some time
        Thread.sleep(20000);

        // Simulate M2 going offline after sending proposal
        System.out.println("\n-------------- M2 will be offline after sending proposal -------------\n");
        Member m2 = memberInstances.get("M2");
        new Thread(() -> m2.proposeValue("M2")).start();
        // Assuming M2 goes offline

        // Let the simulation run for some time
        Thread.sleep(20000);

        // Simulate M3 going offline after sending proposal
        System.out.println("\n-------------- M3 will be offline after sending proposal -------------\n");
        Member m3 = memberInstances.get("M3");
        new Thread(() -> m3.proposeValue("M3")).start();
        // Assuming M3 goes offline

        // Let the simulation run for some time
        Thread.sleep(20000);

        // Shutdown the members (in practice, implement a proper shutdown)
        System.exit(0);
    }
}


