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

package com.alibaba.mqtt.server;

import com.alibaba.mqtt.server.callback.MessageListener;
import com.alibaba.mqtt.server.callback.StatusListener;
import com.alibaba.mqtt.server.config.ChannelConfig;
import com.alibaba.mqtt.server.config.ConsumerConfig;
import com.alibaba.mqtt.server.model.MessageProperties;
import com.alibaba.mqtt.server.model.StatusNotice;
import com.alibaba.mqtt.server.network.AbstractChannel;
import com.alibaba.mqtt.server.util.ThreadFactoryImpl;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ServerConsumer extends AbstractChannel {
    private static final int CONNECTION_NUM = 4;
    private Connection[] connections = new Connection[CONNECTION_NUM];
    private Channel[] channels = new Channel[CONNECTION_NUM];
    private ConsumerConfig consumerConfig;
    private ExecutorService msgExecutor;
    private ExecutorService statusExecutor;
    private static ScheduledThreadPoolExecutor scheduler =
            new ScheduledThreadPoolExecutor(1, new ThreadFactoryImpl("scan_server_consumer_callback_"));
    private Map<String, StatusListener> subscribeStatusMap = new ConcurrentHashMap<>();
    private Map<String, MessageListener> subscribeTopicMap = new ConcurrentHashMap<>();

    public ServerConsumer(ChannelConfig channelConfig, ConsumerConfig consumerConfig) {
        super(channelConfig);
        this.consumerConfig = consumerConfig;
    }

    public void start() throws IOException, TimeoutException {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        super.start();
        for (int i = 0; i < CONNECTION_NUM; i++) {
            connections[i] = factory.newConnection();
            channels[i] = connections[i].createChannel();
        }
        msgExecutor = new ThreadPoolExecutor(
                consumerConfig.getMinConsumeThreadNum(),
                consumerConfig.getMaxConsumeThreadNum(),
                1, TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(30000));

        statusExecutor = new ThreadPoolExecutor(
                consumerConfig.getMinConsumeThreadNum(),
                consumerConfig.getMaxConsumeThreadNum(),
                1, TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(30000));

        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < CONNECTION_NUM; i++) {
                        Channel channel = channels[i];
                        if (!channel.isOpen()) {
                            channels[i] = connections[i].createChannel();
                            Channel finalChannel = channels[i];
                            subscribeTopicMap.forEach((topic, messageListener) -> {
                                try {
                                    _subscribeTopic(finalChannel, topic, messageListener);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                            subscribeStatusMap.forEach((mqttGroupId, statusListener) -> {
                                try {
                                    _subscribeStatus(finalChannel, mqttGroupId, statusListener);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    public void stop() throws IOException {
        for (int i = 0; i < CONNECTION_NUM; i++) {
            connections[i].close();
        }
    }

    public void subscribeTopic(String firstTopic, MessageListener messageListener) throws IOException {
        if (firstTopic == null || messageListener == null) {
            return;
        }
        subscribeTopicMap.put(firstTopic, messageListener);
        for (int i = 0; i < CONNECTION_NUM; i++) {
            Channel channel = channels[i];
            _subscribeTopic(channel, firstTopic, messageListener);
        }
    }

    public void _subscribeTopic(Channel channel, String firstTopic, MessageListener messageListener) throws IOException {
        channel.basicConsume(firstTopic, false, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) {
                msgExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            messageListener.process(properties.getMessageId(), new MessageProperties(properties), body);
                            channel.basicAck(envelope.getDeliveryTag(), false);
                        } catch (Throwable t) {
                            try {
                                channel.basicNack(envelope.getDeliveryTag(), false, false);
                            } catch (IOException e) {
                            }
                        }
                    }
                });
            }
        });
    }

    public void subscribeStatus(String mqttGroupId, StatusListener statusListener) throws IOException {
        if (mqttGroupId == null || statusListener == null) {
            return;
        }
        subscribeStatusMap.put(mqttGroupId, statusListener);
        for (int i = 0; i < CONNECTION_NUM; i++) {
            Channel channel = channels[i];
            _subscribeStatus(channel, mqttGroupId, statusListener);
        }
    }

    public void _subscribeStatus(Channel channel, String mqttGroupId, StatusListener statusListener) throws IOException {
        Map<String, Object> arguments = new HashMap<>(4);
        if (getChannelConfig().isCustomAuth() && mqttGroupId == null) {
            mqttGroupId = StringUtils.EMPTY;
        }
        arguments.put("GROUP_ID", mqttGroupId);
        channel.basicConsume("STATUS", false, arguments, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
                statusExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            statusListener.process(new StatusNotice(body));
                            channel.basicAck(envelope.getDeliveryTag(), false);
                        } catch (Throwable t) {
                            try {
                                channel.basicNack(envelope.getDeliveryTag(), false, false);
                            } catch (IOException e) {
                            }
                        }
                    }
                });
            }
        });
    }

}
