package com.alibaba.mqtt.server.model;

public class StringPair {
    public String key;
    public String value;

    public StringPair() {
    }

    public StringPair(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public int hashCode() {
        return key.hashCode() + 31 * value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        StringPair that = (StringPair) obj;

        return that.key.equals(this.key) && that.value.equals(this.value);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
