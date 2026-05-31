package com.pen.penhubbackend.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

/**
 * Jackson 工具类
 * 提供统一的 ObjectMapper 实例，避免重复创建
 *
 */
@Slf4j
public class JacksonUtils {

    /**
     * 单例 ObjectMapper 实例
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            // 忽略未知属性
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            // 禁用日期时间戳
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            // 允许空对象序列化
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    private JacksonUtils() {
        // 私有构造函数，防止实例化
    }

    /**
     * 获取 ObjectMapper 实例
     *
     * @return ObjectMapper 实例
     */
    public static ObjectMapper getInstance() {
        return OBJECT_MAPPER;
    }

    /**
     * 对象转 JSON 字符串
     *
     * @param obj 对象
     * @return JSON 字符串
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON 序列化失败, obj={}", obj, e);
            return null;
        }
    }

    /**
     * 对象转 JSON 字符串（格式化输出）
     *
     * @param obj 对象
     * @return 格式化的 JSON 字符串
     */
    public static String toPrettyJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON 序列化失败, obj={}", obj, e);
            return null;
        }
    }

    /**
     * JSON 字符串转对象
     *
     * @param json  JSON 字符串
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 对象实例
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("JSON 解析失败, json={}", json, e);
            return null;
        }
    }

    /**
     * JSON 字符串转对象（支持泛型）
     *
     * @param json         JSON 字符串
     * @param typeReference TypeReference 类型引用
     * @param <T>          泛型类型
     * @return 对象实例
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error("JSON 解析失败, json={}", json, e);
            return null;
        }
    }

    /**
     * JSON 字符串转对象（支持 JavaType）
     *
     * @param json     JSON 字符串
     * @param javaType JavaType 类型
     * @param <T>      泛型类型
     * @return 对象实例
     */
    public static <T> T fromJson(String json, JavaType javaType) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, javaType);
        } catch (JsonProcessingException e) {
            log.error("JSON 解析失败, json={}", json, e);
            return null;
        }
    }

    /**
     * 安全地将 JSON 字符串转为对象，解析失败时返回 null
     *
     * @param json  JSON 字符串
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 对象实例，解析失败返回 null
     */
    public static <T> T fromJsonSafe(String json, Class<T> clazz) {
        return fromJson(json, clazz);
    }

    /**
     * 安全地将 JSON 字符串转为对象（支持泛型），解析失败时返回 null
     *
     * @param json         JSON 字符串
     * @param typeReference TypeReference 类型引用
     * @param <T>          泛型类型
     * @return 对象实例，解析失败返回 null
     */
    public static <T> T fromJsonSafe(String json, TypeReference<T> typeReference) {
        return fromJson(json, typeReference);
    }

    /**
     * 构建泛型类型
     *
     * @param rawType       原始类型
     * @param parameterType 参数类型
     * @return JavaType
     */
    public static JavaType buildGenericType(Class<?> rawType, Class<?> parameterType) {
        return OBJECT_MAPPER.getTypeFactory().constructParametricType(rawType, parameterType);
    }

    /**
     * 构建多参数泛型类型
     *
     * @param rawType        原始类型
     * @param parameterTypes 参数类型数组
     * @return JavaType
     */
    public static JavaType buildGenericType(Class<?> rawType, Class<?>... parameterTypes) {
        JavaType[] javaTypes = new JavaType[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            javaTypes[i] = OBJECT_MAPPER.getTypeFactory().constructType(parameterTypes[i]);
        }
        return OBJECT_MAPPER.getTypeFactory().constructParametricType(rawType, javaTypes);
    }
}
