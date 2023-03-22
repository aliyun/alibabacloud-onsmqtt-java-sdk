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

import java.io.IOException;
import java.nio.charset.Charset;

public class Base64Utils {
    public static Charset UTF8 = Charset.forName("UTF-8");
    /**
     * Decode for Base64 string
     *
     *
     * @param str  String need to be decoded
     * @return String decode from the input string
     */
    public static String decode(String str) {
        try {
            return new String(net.iharder.Base64.decode(str), UTF8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Decoding input string exception", e);
        }
    }

    /**
     * Encode a string into Base64 String
     *
     * @param str  String need to be encoded
     * @return     An Base64 string
     */
    public static String encode(String str) {
        return net.iharder.Base64.encodeBytes(str.getBytes(UTF8));
    }

}
