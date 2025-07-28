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


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.nio.charset.StandardCharsets;

public class StatusNotice {
    private String clientId;
    private StatusType statusType;
    private String channelId;
    private String clientIp;
    private long time;
    private String certificateChainSn;
    private String remark;
    public StatusNotice(byte[] payload) {
        JSONObject msgBody = JSON.parseObject(new String(payload, StandardCharsets.UTF_8));
        String eventType = msgBody.getString("eventType");
        if ("connect".equals(eventType)) {
            statusType = StatusType.ONLINE;
        } else {
            statusType = StatusType.OFFLINE;
        }
        this.clientId = msgBody.getString("clientId");
        this.channelId = msgBody.getString("channelId");
        this.clientIp = msgBody.getString("clientIp");
        this.time = msgBody.getLongValue("time");
        this.certificateChainSn = msgBody.getString("certificateChainSn");
        this.remark = msgBody.getString("remark");
    }

    public String getClientId() {
        return clientId;
    }

    public StatusType getStatusType() {
        return statusType;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getClientIp() {
        return clientIp;
    }

    public long getTime() {
        return time;
    }

    public String getCertificateChainSn() {
        return certificateChainSn;
    }

    public String getRemark() {
        return remark;
    }
}
