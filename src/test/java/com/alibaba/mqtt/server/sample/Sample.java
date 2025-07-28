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

package com.alibaba.mqtt.server.sample;

import com.alibaba.fastjson.JSON;
import com.alibaba.mqtt.server.ServerConsumer;
import com.alibaba.mqtt.server.ServerProducer;
import com.alibaba.mqtt.server.callback.MessageListener;
import com.alibaba.mqtt.server.callback.SendCallback;
import com.alibaba.mqtt.server.callback.StatusListener;
import com.alibaba.mqtt.server.config.ChannelConfig;
import com.alibaba.mqtt.server.config.ConsumerConfig;
import com.alibaba.mqtt.server.config.ProducerConfig;
import com.alibaba.mqtt.server.model.MessageProperties;
import com.alibaba.mqtt.server.model.StatusNotice;
import com.alibaba.mqtt.server.model.StringPair;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

public class Sample {

    @Test
    public void test() throws InterruptedException, IOException, TimeoutException {
        String domain = System.getProperty("domain");
        int port = Integer.parseInt(System.getProperty("port"));
        String instanceId = System.getProperty("instanceId");
        String accessKey = System.getProperty("accessKey");
        String secretKey = System.getProperty("secretKey");

        String firstTopic = System.getProperty("firstTopic");
        String secondTopic = System.getProperty("secondTopic");
        String mqttGroupId = System.getProperty("mqttGroupId");

        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.setDomain(domain);
        channelConfig.setPort(port);
        channelConfig.setInstanceId(instanceId);
        channelConfig.setAccessKey(accessKey);
        channelConfig.setSecretKey(secretKey);
        ServerConsumer serverConsumer = new ServerConsumer(channelConfig, new ConsumerConfig());
        serverConsumer.start();
        serverConsumer.subscribeTopic(firstTopic, new MessageListener() {
            @Override
            public void process(String msgId, MessageProperties messageProperties, byte[] payload) {
                System.out.println("Receive:" + msgId + "," + new String(payload) + ","+ JSON.toJSONString(messageProperties));
            }
        });

        serverConsumer.subscribeStatus(mqttGroupId, new StatusListener() {
            @Override
            public void process(StatusNotice statusNotice) {
                System.out.println(JSON.toJSONString(statusNotice));
            }
        });

        ServerProducer serverProducer = new ServerProducer(channelConfig, new ProducerConfig());
        serverProducer.start();
        for (int i = 0; i < 10000; i++) {
            Thread.sleep(3000);
            String mqttTopic = firstTopic + "/" + secondTopic;
            serverProducer.sendMessage(mqttTopic, ("hello " + i).getBytes(StandardCharsets.UTF_8), new SendCallback() {
                @Override
                public void onSuccess(String msgId) {
                    System.out.println("SendSuccess " + msgId);
                }

                @Override
                public void onFail() {
                    System.out.println("SendFail ");
                }
            }, "600", "text/plain", Arrays.asList(new StringPair("k1", "v1")));
        }

        Thread.sleep(10000000);
    }
}
