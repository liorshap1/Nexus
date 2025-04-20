/*
 * Copyright 2025 Lior Shaposhnikov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.nexus.core;

public class Message {
    private String text;
    private String timestamp;
    private String messageDeliverUid;
    private MessageType messageType;

    public Message(String text, String timestamp, MessageType messageType, String messageDeliverUid) {
        this.text = text;
        this.timestamp = timestamp;
        this.messageType = messageType;
        this.messageDeliverUid = messageDeliverUid;
    }

    public String getMessageDeliverUid() {
        return messageDeliverUid;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public enum MessageType {
        SENDER, RECEIVER
    }
}
