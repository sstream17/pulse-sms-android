/*
 * Copyright (C) 2020 Luke Klinker
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

package com.stream_suite.link.api.entity;

public class AddScheduledMessageRequest {

    public String accountId;
    public ScheduledMessageBody[] scheduledMessages;

    public AddScheduledMessageRequest(String accountId, ScheduledMessageBody[] scheduledMessages) {
        this.accountId = accountId;
        this.scheduledMessages = scheduledMessages;
    }

    public AddScheduledMessageRequest(String accountId, ScheduledMessageBody scheduledMessage) {
        this.accountId = accountId;
        this.scheduledMessages = new ScheduledMessageBody[1];
        this.scheduledMessages[0] = scheduledMessage;
    }

}
