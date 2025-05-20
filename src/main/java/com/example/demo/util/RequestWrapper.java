package com.example.demo.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestWrapper {
    public String fullyQualifiedName;
    public List<Pair<String, String>> params;
    public List<Boolean> isReferenceParams;
    public Class<?>[] paramTypes;

    public RequestWrapper(String fullyQualifiedName, List<Pair<String, String>> params, List<Boolean> isReferenceParams, Class<?>[] paramTypes) {
        this.fullyQualifiedName = fullyQualifiedName;
        this.params = params;
        this.isReferenceParams = isReferenceParams;
        this.paramTypes = paramTypes;
    }
}

class ResponseWrapper {
    public Pair<String, String> result;  // 返回值，第一个是类型，第二个是值的json
    public Map<Integer, Pair<String, String>> modifiedReferences; // 修改过的引用参数，第一个是参数的index，第二个是类型和json

    public ResponseWrapper() {
        this.modifiedReferences = new HashMap<>();
    }

    public ResponseWrapper(Pair<String, String> result, Map<Integer, Pair<String, String>> modifiedReferences) {
        this.result = result;
        this.modifiedReferences = modifiedReferences;
    }
}

class Pair<L, R> {
    private final L left;
    private final R right;

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public L getLeft() { return left; }
    public R getRight() { return right; }

}