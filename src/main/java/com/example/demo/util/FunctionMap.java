package com.example.demo.util;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@ConfigurationProperties(prefix = "functionmap")
@Component
public class FunctionMap {
    Map<String, String> map;

    public Map<String, String> getMap() {
        return map;
    }

    public void setMap(Map<String, String> map) {
        this.map = map;
    }


    @Override
    public String toString() {
        return "FunctionMap{" +
                "map=" + map +
                '}';
    }
}
