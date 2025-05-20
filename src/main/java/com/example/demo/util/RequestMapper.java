package com.example.demo.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

@RestController
public class RequestMapper {


    @PostMapping("/mapper")
    public ResponseWrapper mapper(@RequestBody RequestWrapper wrapper) {
        String fullyQualifiedMethodName = wrapper.fullyQualifiedName;
        List<Pair<String, String>> params = wrapper.params;
        List<Boolean> isReferenceParams = wrapper.isReferenceParams;
        Object[] realParams = new Object[params.size()];
        Class<?>[] paramTypes = wrapper.paramTypes;

        ObjectFlattener objectFlattener = new ObjectFlattener();

        for (int i = 0; i < params.size(); i++) {
            try {
                Class<?> paramType = Class.forName(params.get(i).getLeft());
                JsonElement paramValue = JsonParser.parseString(params.get(i).getRight());
                Object storedObject = objectFlattener.reconstruct(paramValue, paramType);
                realParams[i] = storedObject;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        try {
            String className = fullyQualifiedMethodName.substring(0, fullyQualifiedMethodName.lastIndexOf('.'));
            Class<?> targetClass = Class.forName(className);
            String methodName = fullyQualifiedMethodName.substring(fullyQualifiedMethodName.lastIndexOf(".") + 1);
            Method targetMethod = targetClass.getMethod(methodName, paramTypes);
            Object serviceInstance = targetClass.getDeclaredConstructor().newInstance();

            Object result = targetMethod.invoke(serviceInstance, realParams);
            ResponseWrapper response = new ResponseWrapper();
            objectFlattener = new ObjectFlattener();
            if (result != null) {
                Type resultType = result.getClass();
                JsonElement resultValue = objectFlattener.flatten(result);
                response.result= new Pair<>(resultType.getTypeName(),resultValue.toString());
            }
            for (int i = 0; i < isReferenceParams.size(); i++) {
                if (isReferenceParams.get(i)) {
                    JsonElement paramValue = objectFlattener.flatten(realParams[i]);
                    response.modifiedReferences.put(i, new Pair<>(paramTypes[i].getTypeName(), paramValue.toString()));
                }
            }
            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ResponseWrapper();
    }
}