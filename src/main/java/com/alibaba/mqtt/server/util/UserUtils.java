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


import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;


public class UserUtils {
    public static int ACCESS_FROM_USER = 0;

    /***
     * @param ak  aliyun
     * @param instanceId
     * @return
     */
    public static String getUserName(String ak, String instanceId) {
        StringBuffer buf = new StringBuffer();
        return Base64Utils.encode(buf.append(ACCESS_FROM_USER).append(":").append(instanceId).append(":")
                .append(ak).toString());
    }

    /***
     * @param ak  aliyun
     * @param instanceId
     * @param stsToken
     * @return
     */
    public static String getUserName(String ak, String instanceId, String stsToken) {
        StringBuffer buf = new StringBuffer();
        return Base64Utils.encode(buf.append(ACCESS_FROM_USER).
                append(":").append(instanceId)
                .append(":").append(ak).append(":").
                        append(stsToken).toString());
    }

    /***
     *
     * @param sk aliyun secrectKey
     * @return  password
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     */
    public static String getPassord(String sk) throws InvalidKeyException, NoSuchAlgorithmException {
        long timestamp = System.currentTimeMillis();
        StringBuffer buf = new StringBuffer();
        String signature = HmacSHA1Utils.hamcsha1(sk.getBytes(StandardCharsets.UTF_8), String.valueOf(timestamp).getBytes(StandardCharsets.UTF_8));
        return Base64Utils.encode(buf.append(signature).append(":").append(timestamp).toString());
    }
}
