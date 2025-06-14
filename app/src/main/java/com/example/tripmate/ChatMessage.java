package com.example.tripmate;



import java.util.Date;

public class ChatMessage {
    public String senderId;
    public String senderName;
    public String text;
    public Date timestamp;
    public String id;

    public ChatMessage() {}

    public ChatMessage(String senderId, String senderName, String text, Date timestamp) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
        this.timestamp = timestamp;
    }
}