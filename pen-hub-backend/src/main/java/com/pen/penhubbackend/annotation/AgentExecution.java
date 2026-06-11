package com.pen.penhubbackend.annotation;


import java.lang.annotation.*;


/**
 * 智能体执行注解
 * 用于标记智能体方法，自动记录执行日志和性能数据
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented// 注解会保留在编译的class文件中，并且能被反射机制所读取
public @interface AgentExecution {

    /**
     * 智能体名称
     * 例如: "agent1_generate_titles", "agent2_generate_outline"
     */
    String value();

    /**
     * 智能体描述
     */
    String description() default "";


}
