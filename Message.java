package com.paxos.council;

import java.io.Serializable;

public class Message implements Serializable {
    public enum MessageType {
        PREPARE_REQUEST,
        PROMISE,
        ACCEPT_REQUEST,
        ACCEPTED
    }

    private MessageType type;
    private int proposalNumber;
    private String proposerId;
    private String value;
    private int lastAcceptedProposalNumber;
    private String lastAcceptedValue;
    private String senderId;
    private String receiverId;

    public Message(MessageType type) {
        this.type = type;
    }

    // Getters and Setters
    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public int getProposalNumber() {
        return proposalNumber;
    }

    public void setProposalNumber(int proposalNumber) {
        this.proposalNumber = proposalNumber;
    }

    public String getProposerId() {
        return proposerId;
    }

    public void setProposerId(String proposerId) {
        this.proposerId = proposerId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getLastAcceptedProposalNumber() {
        return lastAcceptedProposalNumber;
    }

    public void setLastAcceptedProposalNumber(int lastAcceptedProposalNumber) {
        this.lastAcceptedProposalNumber = lastAcceptedProposalNumber;
    }

    public String getLastAcceptedValue() {
        return lastAcceptedValue;
    }

    public void setLastAcceptedValue(String lastAcceptedValue) {
        this.lastAcceptedValue = lastAcceptedValue;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }
}



