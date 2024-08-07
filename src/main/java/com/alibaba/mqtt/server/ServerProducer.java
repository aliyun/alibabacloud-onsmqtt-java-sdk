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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.mqtt.server.callback.SendCallback;
import com.alibaba.mqtt.server.common.SendResult;
import com.alibaba.mqtt.server.config.ChannelConfig;
import com.alibaba.mqtt.server.config.ProducerConfig;
import com.alibaba.mqtt.server.model.MessageProperties;
import com.alibaba.mqtt.server.model.StringPair;
import com.alibaba.mqtt.server.network.AbstractChannel;
import com.alibaba.mqtt.server.util.ThreadFactoryImpl;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import com.rabbitmq.client.Connection;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ServerProducer extends AbstractChannel {
    private Map<Long, SendCallbackWrapper> sendCallbackMap = new ConcurrentHashMap<>(32);
    private ProducerConfig producerConfig;
    private Connection connection;
    private Channel channel;
    private static ScheduledThreadPoolExecutor scheduler =
            new ScheduledThreadPoolExecutor(1, new ThreadFactoryImpl("scan_server_producer_callback_"));


    public ServerProducer(ChannelConfig channelConfig, ProducerConfig producerConfig) {
        super(channelConfig);
        this.producerConfig = producerConfig;
    }

    public void start() throws IOException, TimeoutException {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        super.start();
        this.connection = factory.newConnection();
        this.channel = connection.createChannel();
        confirmChannel(channel);
        scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<Long, SendCallbackWrapper> entry : sendCallbackMap.entrySet()) {
                    SendCallbackWrapper sendCallbackWrapper = entry.getValue();
                    if (System.currentTimeMillis() - sendCallbackWrapper.timestamp >
                            producerConfig.getSendTimeoutMills() + 1000L) {
                        sendCallbackWrapper.sendCallback.onFail();
                        sendCallbackMap.remove(entry.getKey());
                    }
                }
                try {
                    if (!channel.isOpen()) {
                        Channel newChannel = connection.createChannel();
                        confirmChannel(newChannel);
                        channel = newChannel;
                    }
                } catch (Exception e) {
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public void stop() throws IOException {
        connection.close();
    }

    private void confirmChannel(Channel channel) throws IOException {
        channel.confirmSelect();
        channel.addConfirmListener(new ConfirmListener() {
            @Override
            public void handleAck(long deliveryTag, boolean multiple) throws IOException {
                SendCallbackWrapper sendCallbackWrapper = sendCallbackMap.remove(deliveryTag);
                if (sendCallbackWrapper != null && sendCallbackWrapper.sendCallback != null) {
                    sendCallbackWrapper.sendCallback.onSuccess(sendCallbackWrapper.msgId);
                }
            }

            @Override
            public void handleNack(long deliveryTag, boolean multiple) throws IOException {
                SendCallbackWrapper sendCallbackWrapper = sendCallbackMap.remove(deliveryTag);
                if (sendCallbackWrapper != null && sendCallbackWrapper.sendCallback != null) {
                    sendCallbackWrapper.sendCallback.onFail();
                }
            }
        });
    }

    public void sendMessage(String mqttTopic, byte[] payload, SendCallback sendCallback) throws IOException {
        sendMessage(mqttTopic, payload, sendCallback, null, null, null);
    }

    /**
     * @param mqttTopic
     * @param payload
     * @param sendCallback
     * @param mqtt5MsgExpireInterval  unit:second
     * @param mqtt5ContentType
     * @param mqtt5UserProperty
     * @throws IOException
     */
    public void sendMessage(String mqttTopic,
                             byte[] payload,
                             SendCallback sendCallback,
                             String mqtt5MsgExpireInterval,
                             String mqtt5ContentType,
                             List<StringPair> mqtt5UserProperty) throws IOException {
        String msgId = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
        synchronized (channel) {
            long publishSeqNo = channel.getNextPublishSeqNo();
            AMQP.BasicProperties props = buildAMQPProps(msgId, publishSeqNo, mqtt5MsgExpireInterval, mqtt5ContentType, mqtt5UserProperty);
            if (sendCallback != null) {
                sendCallbackMap.put(publishSeqNo, new SendCallbackWrapper(sendCallback, msgId));
            }
            channel.basicPublish(mqttTopic, mqttTopic, true, props, payload);
        }
    }

    public SendResult sendMessage(String mqttTopic, byte[] payload) throws IOException {
        return sendMessage(mqttTopic, payload, null, null, null);
    }

    /**
     * @param mqttTopic
     * @param payload
     * @param mqtt5MsgExpireInterval unit: second
     * @param mqtt5ContentType
     * @param mqtt5UserProperty
     * @return
     * @throws IOException
     */
    public SendResult sendMessage(String mqttTopic,
                                   byte[] payload,
                                   String mqtt5MsgExpireInterval,
                                   String mqtt5ContentType,
                                   List<StringPair> mqtt5UserProperty) throws IOException {
        String msgId = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        SendResult sendResult = new SendResult(false);
        SyncSendCallBack syncSendCallBack = new SyncSendCallBack(sendResult, countDownLatch);
        synchronized (channel) {
            long publishSeqNo = channel.getNextPublishSeqNo();
            AMQP.BasicProperties props = buildAMQPProps(msgId, publishSeqNo, mqtt5MsgExpireInterval, mqtt5ContentType, mqtt5UserProperty);
            sendCallbackMap.put(publishSeqNo, new SendCallbackWrapper(syncSendCallBack, msgId));
            channel.basicPublish(mqttTopic, mqttTopic, true, props, payload);
        }
        try {
            countDownLatch.await(producerConfig.getSendTimeoutMills() + 1000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return sendResult;
        }
        return sendResult;
    }

    private AMQP.BasicProperties buildAMQPProps(String msgId,
                                                long publishSeqNo,
                                                String mqtt5MsgExpireInterval,
                                                String mqtt5ContentType,
                                                List<StringPair> mqtt5UserProperty) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("seqId", publishSeqNo);
        if (mqtt5UserProperty != null && !mqtt5UserProperty.isEmpty()) {
            headers.put(MessageProperties.MQTT5_USER_PROPERTIES, JSON.toJSONString(mqtt5UserProperty));
        }
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .headers(headers)
                .messageId(msgId)
                .contentType(mqtt5ContentType)
                .expiration(mqtt5MsgExpireInterval)
                .build();
        return props;
    }

    private class SendCallbackWrapper {
        private SendCallback sendCallback;
        private String msgId;
        private long timestamp = System.currentTimeMillis();

        public SendCallbackWrapper(SendCallback sendCallback, String msgId) {
            this.sendCallback = sendCallback;
            this.msgId = msgId;
        }
    }

    private class SyncSendCallBack implements SendCallback {
        private SendResult sendResult;
        private CountDownLatch countDownLatch;

        public SyncSendCallBack(SendResult sendResult, CountDownLatch countDownLatch) {
            this.sendResult = sendResult;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void onSuccess(String msgId) {
            sendResult.setMsgId(msgId);
            sendResult.setSuccess(true);
            countDownLatch.countDown();
        }

        @Override
        public void onFail() {
            countDownLatch.countDown();
        }
    }

}
