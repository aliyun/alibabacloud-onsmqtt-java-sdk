/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.mqtt.server.model;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.LongString;

import java.util.Map;

public class MessageProperties {
    private static final String PROPERTY_MQTT_SECOND_TOPIC = "mqttSecondTopic";
    private static final String PROPERTY_MQTT_PARENT_TOPIC = "mqttParentTopic";
    private static final String PROPERTY_MQTT_CLIENT = "clientId";
    private static final String PROPERTY_MQTT_BORN_TIME = "bornTime";
    public static final String MQTT5_USER_PROPERTIES = "mqtt5UserProperty";

    private String firstTopic;
    private String secondTopic;
    private String clientId;
    private Long bornTime;
    private String mqtt5ContentType;
    /**
     * unit: second
     */
    private String mqtt5MsgExpireInterval;
    private String mqtt5UserProperty;

    public MessageProperties(AMQP.BasicProperties properties) {
        Map<String, Object> headers = properties.getHeaders();
        if (headers == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : headers.entrySet()) {
            LongString value = (entry.getValue() instanceof LongString) ? (LongString) entry.getValue() : null;
            if (value == null) {
                continue;
            }
            if (PROPERTY_MQTT_PARENT_TOPIC.equals(entry.getKey())) {
                firstTopic = value.toString();
            } else if (PROPERTY_MQTT_SECOND_TOPIC.equals(entry.getKey())) {
                secondTopic = value.toString();
            } else if (PROPERTY_MQTT_CLIENT.equals(entry.getKey())) {
                clientId = value.toString();
            } else if (PROPERTY_MQTT_BORN_TIME.equals(entry.getKey())) {
                bornTime = Long.parseLong(value.toString());
            } else if (MQTT5_USER_PROPERTIES.equals(entry.getKey())) {
                mqtt5UserProperty = value.toString();
            }
        }
        mqtt5ContentType = properties.getContentType();
        mqtt5MsgExpireInterval = properties.getExpiration();
    }

    public String getFirstTopic() {
        return firstTopic;
    }

    public String getSecondTopic() {
        return secondTopic;
    }

    public String getClientId() {
        return clientId;
    }

    public Long getBornTime() {
        return bornTime;
    }

    public String getMqtt5ContentType() {
        return mqtt5ContentType;
    }

    public String getMqtt5MsgExpireInterval() {
        return mqtt5MsgExpireInterval;
    }

    public String getMqtt5UserProperty() {
        return mqtt5UserProperty;
    }
}
