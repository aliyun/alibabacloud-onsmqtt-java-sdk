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

package com.alibaba.mqtt.server.network;

import com.alibaba.mqtt.server.config.ChannelConfig;
import com.alibaba.mqtt.server.util.AliyunCredentialsProvider;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class AbstractChannel {
    protected ConnectionFactory factory = new ConnectionFactory();
    private ChannelConfig channelConfig;
    protected AtomicBoolean started = new AtomicBoolean(false);

    public AbstractChannel(ChannelConfig channelConfig) {
        this.channelConfig = channelConfig;
    }

    public ChannelConfig getChannelConfig() {
        return channelConfig;
    }

    protected void start() throws IOException, TimeoutException {
        long ts = System.currentTimeMillis();
        factory.setHost(channelConfig.getDomain());
        factory.setCredentialsProvider(
                new AliyunCredentialsProvider(channelConfig.getAccessKey(), channelConfig.getSecretKey(), channelConfig.getInstanceId(), ts));
        factory.setAutomaticRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(channelConfig.getNetworkRecoveryInterval());
        factory.setPort(channelConfig.getPort());
        factory.setVirtualHost("MQTT");
        factory.setConnectionTimeout(channelConfig.getConnectionTimeout());
        factory.setHandshakeTimeout(channelConfig.getHandshakeTimeout());
        factory.setRequestedHeartbeat(channelConfig.getRequestedHeartbeat());
        factory.setShutdownTimeout(channelConfig.getShutdownTimeout());

        Map<String, Object> properties = new HashMap<>(1);
        properties.put("signKey", String.valueOf(ts));
        if (channelConfig.isCustomAuth()) {
            properties.put("customAuth", "true");
        }
        factory.setClientProperties(properties);
    }

}
