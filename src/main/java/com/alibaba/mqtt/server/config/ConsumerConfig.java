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

package com.alibaba.mqtt.server.config;

public class ConsumerConfig {
    private int minConsumeThreadNum = 16;
    private int maxConsumeThreadNum = 32;

    public int getMinConsumeThreadNum() {
        return minConsumeThreadNum;
    }

    public void setMinConsumeThreadNum(int minConsumeThreadNum) {
        this.minConsumeThreadNum = minConsumeThreadNum;
    }

    public int getMaxConsumeThreadNum() {
        return maxConsumeThreadNum;
    }

    public void setMaxConsumeThreadNum(int maxConsumeThreadNum) {
        this.maxConsumeThreadNum = maxConsumeThreadNum;
    }
}
