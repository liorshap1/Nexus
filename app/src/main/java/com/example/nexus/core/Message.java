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

import java.util.Map;

public class Message {
    private final String messageDeliverUid;
    private String text;
    private long timestamp;
    private MessageType messageType;
    private Map<String, String> reactions;
    private String messageKey;

    public Message(String text, long timestamp, MessageType messageType, String messageDeliverUid, Map<String, String> reactions, String messageKey) {
        this.text = text;
        this.timestamp = timestamp;
        this.messageType = messageType;
        this.messageDeliverUid = messageDeliverUid;
        this.reactions = reactions;
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessageDeliverUid() {
        return messageDeliverUid;
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

    public Map<String, String> getReactions() {
        return reactions;
    }

    public void setReactions(Map<String, String> reactions) {
        this.reactions = reactions;
    }
}
