package com.example.demo.util;

import com.google.gson.*;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;

public class ObjectFlattener {
    private final Map<String, Object> uuidMap = new HashMap<>();
    private final Map<Object, String> reverseMap = new IdentityHashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, Object> reconstructedMap = new HashMap<>();

    public JsonElement flatten(Object obj) {
        return flattenRecursive(obj);
    }

    private JsonElement flattenRecursive(Object obj) {
        if (obj == null) return JsonNull.INSTANCE;

        // 如果是基本类型或包装类型，直接返回其值
        if (obj instanceof Number || obj instanceof Boolean || obj instanceof Character || obj instanceof String) {
            return new JsonPrimitive(obj.toString());
        }

        // 如果已经被映射，返回其 UUID
        if (reverseMap.containsKey(obj)) {
            return new JsonPrimitive(reverseMap.get(obj));
        }

        String uuid = UUID.randomUUID().toString();
        uuidMap.put(uuid, obj);
        reverseMap.put(obj, uuid);

        Class<?> clazz = obj.getClass();

        if (obj instanceof Collection) {
            JsonArray array = new JsonArray();
            for (Object item : (Collection<?>) obj) {
                array.add(flattenRecursive(item));
            }
            return array;
        }

        if (clazz.isArray()) {
            JsonArray array = new JsonArray();
            int len = Array.getLength(obj);
            for (int i = 0; i < len; i++) {
                array.add(flattenRecursive(Array.get(obj, i)));
            }
            return array;
        }

        if (obj instanceof Map) {
            JsonObject mapObj = new JsonObject();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
                mapObj.add(entry.getKey().toString(), flattenRecursive(entry.getValue()));
            }
            return mapObj;
        }

        // 处理普通对象
        JsonObject jsonObj = new JsonObject();
        jsonObj.addProperty("__uuid", uuid);
        jsonObj.addProperty("__type", clazz.getName());
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                jsonObj.add(field.getName(), value == null ? JsonNull.INSTANCE : flattenRecursive(value));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return jsonObj;
    }

    public <T> T reconstruct(JsonElement jsonElement, Class<T> clazz) {
        return (T) reconstructRecursive(jsonElement, clazz);
    }

    private Object reconstructRecursive(JsonElement element, Class<?> clazz) {
        if (element.isJsonPrimitive()) {
            JsonPrimitive prim = element.getAsJsonPrimitive();

            // 是 UUID 引用（间接引用其他对象）
            if (prim.isString() && uuidMap.containsKey(prim.getAsString())) {
                String uuid = prim.getAsString();
                if (reconstructedMap.containsKey(uuid)) return reconstructedMap.get(uuid);
                Object raw = uuidMap.get(uuid);
                Object real = reconstructRecursive(flatten(raw), raw.getClass());
                reconstructedMap.put(uuid, real);
                return real;
            }

            // 否则是基本类型或字符串
            if (clazz == int.class || clazz == Integer.class) return prim.getAsInt();
            if (clazz == long.class || clazz == Long.class) return prim.getAsLong();
            if (clazz == boolean.class || clazz == Boolean.class) return prim.getAsBoolean();
            if (clazz == double.class || clazz == Double.class) return prim.getAsDouble();
            if (clazz == float.class || clazz == Float.class) return prim.getAsFloat();
            if (clazz == short.class || clazz == Short.class) return prim.getAsShort();
            if (clazz == byte.class || clazz == Byte.class) return prim.getAsByte();
            if (clazz == char.class || clazz == Character.class) return prim.getAsCharacter();
            if (clazz == String.class) return prim.getAsString();

            return gson.fromJson(prim, clazz); // 兜底转换
        }

        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            List<Object> list = new ArrayList<>();
            for (JsonElement e : array) {
                list.add(reconstructRecursive(e, Object.class));
            }
            return list;
        }

        if (element.isJsonObject()) {
            JsonObject jsonObj = element.getAsJsonObject();
            if (jsonObj.has("__uuid")) {
                String uuid = jsonObj.get("__uuid").getAsString();
                if (reconstructedMap.containsKey(uuid)) return reconstructedMap.get(uuid);

                // 动态加载类
                if (jsonObj.has("__type")) {
                    String typeName = jsonObj.get("__type").getAsString();
                    try {
                        clazz = Class.forName(typeName);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("无法加载类: " + typeName, e);
                    }
                }

                Object instance;
                try {
                    instance = clazz.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                reconstructedMap.put(uuid, instance);

                for (Map.Entry<String, JsonElement> entry : jsonObj.entrySet()) {
                    String fieldName = entry.getKey();
                    if (fieldName.equals("__uuid") || fieldName.equals("__type")) continue;

                    try {
                        Field field = clazz.getDeclaredField(fieldName);
                        field.setAccessible(true);
                        Object fieldVal = reconstructRecursive(entry.getValue(), field.getType());
                        field.set(instance, fieldVal);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
                return instance;
            }

            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : jsonObj.entrySet()) {
                map.put(entry.getKey(), reconstructRecursive(entry.getValue(), Object.class));
            }
            return map;
        }

        return null;
    }

    public Map<String, Object> getUuidMap() {
        return uuidMap;
    }

    public String toJson(JsonElement element) {
        return gson.toJson(element);
    }
}