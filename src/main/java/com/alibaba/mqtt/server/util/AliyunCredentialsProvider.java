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

package com.alibaba.mqtt.server.util;

import com.rabbitmq.client.impl.CredentialsProvider;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class AliyunCredentialsProvider implements CredentialsProvider {
    private static Logger logger = LoggerFactory.getLogger(AliyunCredentialsProvider.class);

    /**
     * Access Key ID.
     */
    private final String accessKeyId;
    /**
     * Access Key Secret.
     */
    private final String accessKeySecret;
    /**
     * security temp token. (optional)
     */
    private final String securityToken;

    /**
     * instanceId
     */
    private final String instanceId;
    private final long ts;

    public AliyunCredentialsProvider(final String accessKeyId, final String accessKeySecret,
                                     final String instanceId, final long ts) {
        this(accessKeyId, accessKeySecret, null, instanceId, ts);
    }
    public AliyunCredentialsProvider(final String accessKeyId, final String accessKeySecret,
                                     final String securityToken, final String instanceId, final long ts) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.securityToken = securityToken;
        this.instanceId = instanceId;
        this.ts = ts;
    }
    @Override
    public String getUsername() {
        if(StringUtils.isNotEmpty(securityToken)) {
            return UserUtils.getUserName(accessKeyId, instanceId, securityToken);
        } else {
            return UserUtils.getUserName(accessKeyId, instanceId);
        }
    }

    @Override
    public String getPassword() {
        try {
            return Tools.macSignature(String.valueOf(ts), accessKeySecret);
        } catch (InvalidKeyException e) {
            logger.error("get password error! check your params, NoSuchAlgorithmException:" + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            logger.error("get password error! check your params, invalidKeyException:" + e.getMessage());
        }
        return null;
    }
}
