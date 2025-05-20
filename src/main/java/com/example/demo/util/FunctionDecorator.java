package com.example.demo.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Aspect
@Component
public class FunctionDecorator {

    @Autowired
    FunctionMap functionMap;

    @Autowired
    private RestTemplate restTemplate;

    private static void copyProperties(Object target, Object source) throws Exception {
        if (source == null || target == null) {
            return;
        }

        if (source instanceof List && target instanceof List) {
            List<?> sourceList = (List<?>) source;
            List<Object> targetList = (List<Object>) target;
            targetList.clear();
            for (Object item : sourceList) {
                if (item != null) {
                    Object copiedItem = deepCopy(item);
                    targetList.add(copiedItem);
                } else {
                    targetList.add(null);
                }
            }
        } else if (source instanceof Map && target instanceof Map) {
            Map<?, ?> sourceMap = (Map<?, ?>) source;
            Map<Object, Object> targetMap = (Map<Object, Object>) target;
            targetMap.clear();
            for (Map.Entry<?, ?> entry : sourceMap.entrySet()) {
                Object copiedKey = deepCopy(entry.getKey());
                Object copiedValue = deepCopy(entry.getValue());
                targetMap.put(copiedKey, copiedValue);
            }
        } else {
            Class<?> clazz = source.getClass();
            while (clazz != null && clazz != Object.class) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
                        field.setAccessible(true);
                        Object value = field.get(source);
                        if (value != null) {
                            if (isPrimitiveOrWrapper(value.getClass()) || value instanceof String) {
                                field.set(target, value);
                            } else {
                                Object copiedValue = deepCopy(value);
                                field.set(target, copiedValue);
                            }
                        }
                    }
                }
                clazz = clazz.getSuperclass();
            }
        }
    }

    private static Object deepCopy(Object source) throws Exception {
        if (source == null) {
            return null;
        }
        if (isPrimitiveOrWrapper(source.getClass()) || source instanceof String) {
            return source;
        }
        Object copy = source.getClass().getDeclaredConstructor().newInstance();
        copyProperties(copy, source);
        return copy;
    }

    private static boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() || clazz == Integer.class || clazz == Long.class ||
                clazz == Double.class || clazz == Float.class || clazz == Boolean.class ||
                clazz == Byte.class || clazz == Short.class || clazz == Character.class;
    }

    @Around("@annotation(LogQualifiedName)")
    public Object logMethodName(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        String fullQualifiedName = className + "." + methodName;

        ObjectFlattener flattener = new ObjectFlattener();

        Object[] args = joinPoint.getArgs();
        List<Pair<String, String>> argList = new ArrayList<>();
        List<Boolean> isReferenceParams = new ArrayList<>();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?>[] parameterTypes = method.getParameterTypes();

        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            String argType = arg.getClass().getName();
            JsonElement json = flattener.flatten(arg);
            argList.add(new Pair<>(argType, json.toString()));
            isReferenceParams.add(!parameterTypes[i].isPrimitive() && !(arg instanceof String));
        }

        String remoteUrl = functionMap.getMap().get(fullQualifiedName);
        if (Objects.equals(remoteUrl, "localhost") || remoteUrl == null) {
            return joinPoint.proceed();
        } else {
            RequestWrapper wrapper = new RequestWrapper(fullQualifiedName, argList, isReferenceParams, parameterTypes);
            ResponseWrapper response = restTemplate.postForObject(
                    remoteUrl + "/mapper",
                    wrapper,
                    ResponseWrapper.class
            );

            if (response == null) {
                return null;
            }
            flattener = new ObjectFlattener();
            try {
                Class<?> resultClass = Class.forName(response.result.getLeft());
                JsonElement resultValue = JsonParser.parseString(response.result.getRight());
                Object result = flattener.reconstruct(resultValue, resultClass);
                Map<Integer, Pair<String, String>> modifiedReferences = response.modifiedReferences;
                if (modifiedReferences != null) {
                    for (Map.Entry<Integer, Pair<String, String>> entry : modifiedReferences.entrySet()) {
                        int index = entry.getKey();
                        if (index >= 0 && index < args.length) {
                            Object originalArg = args[index];
                            if (originalArg != null) {
                                String json = entry.getValue().getRight();
                                JsonElement paramValue = JsonParser.parseString(json);
                                System.out.println(paramValue);
                                Object storedObject = flattener.reconstruct(paramValue, Class.forName(entry.getValue().getLeft()));
                                copyProperties(originalArg, storedObject);
                            }
                        }
                    }
                }
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}