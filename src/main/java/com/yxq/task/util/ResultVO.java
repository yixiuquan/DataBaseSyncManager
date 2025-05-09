package com.yxq.task.util;

import lombok.Data;

/**
 * 统一响应结果类
 * @param <T> 数据类型
 */
@Data
public class ResultVO<T> {
    /**
     * 状态码
     */
    private Integer code;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 数据
     */
    private T data;
    
    /**
     * 成功结果
     * @return 成功的响应结果
     */
    public static <T> ResultVO<T> success() {
        return new ResultVO<T>().setCode(200).setMessage("操作成功");
    }
    
    /**
     * 成功结果（带数据）
     * @param data 数据
     * @return 成功的响应结果
     */
    public static <T> ResultVO<T> success(T data) {
        return new ResultVO<T>().setCode(200).setMessage("操作成功").setData(data);
    }
    
    /**
     * 失败结果
     * @param message 错误消息
     * @return 失败的响应结果
     */
    public static <T> ResultVO<T> error(String message) {
        return new ResultVO<T>().setCode(500).setMessage(message);
    }
    
    /**
     * 失败结果（自定义状态码）
     * @param code 状态码
     * @param message 错误消息
     * @return 失败的响应结果
     */
    public static <T> ResultVO<T> error(Integer code, String message) {
        return new ResultVO<T>().setCode(code).setMessage(message);
    }
    
    /**
     * 设置状态码
     * @param code 状态码
     * @return this
     */
    public ResultVO<T> setCode(Integer code) {
        this.code = code;
        return this;
    }
    
    /**
     * 设置消息
     * @param message 消息
     * @return this
     */
    public ResultVO<T> setMessage(String message) {
        this.message = message;
        return this;
    }
    
    /**
     * 设置数据
     * @param data 数据
     * @return this
     */
    public ResultVO<T> setData(T data) {
        this.data = data;
        return this;
    }
} 