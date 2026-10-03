package com.devin.uniontalk.base.utils;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import lombok.Getter;
import java.math.BigInteger;
import java.util.UUID;

/**
 * 2026/5/13 12:08.
 *
 * <p>
 * 分布式Id生成器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Getter
public final class IdGenerator {

    /**
     * 节点标识最小值.
     */
    private static final long MIN_NODE_ID = 0L;

    /**
     * 节点标识最大值.
     */
    private static final long MAX_NODE_ID = 31L;

    /**
     * Key最小长度.
     */
    private static final int MIN_KEY_LENGTH = 1;

    /**
     * Key最大长度.
     */
    private static final int MAX_KEY_LENGTH = 32;

    /**
     * 工作机器标识 JVM 参数名.
     */
    private static final String WORKER_ID_PROPERTY = "union.id-generator.worker-id";

    /**
     * 数据中心标识 JVM 参数名.
     */
    private static final String DATACENTER_ID_PROPERTY = "union.id-generator.datacenter-id";

    /**
     * 工作机器标识环境变量名.
     */
    private static final String WORKER_ID_ENV = "UNION_ID_GENERATOR_WORKER_ID";

    /**
     * 数据中心标识环境变量名.
     */
    private static final String DATACENTER_ID_ENV = "UNION_ID_GENERATOR_DATACENTER_ID";

    /**
     * 默认Id生成器.
     */
    private static volatile IdGenerator defaultGenerator;

    /**
     * 工作机器标识.
     */
    private final long workerId;

    /**
     * 数据中心标识.
     */
    private final long datacenterId;

    /**
     * Hutool雪花算法生成器.
     */
    private final Snowflake snowflake;

    /**
     * 创建Id生成器.
     *
     * @param workerId     工作机器Id，取值范围0到31
     * @param datacenterId 数据中心Id，取值范围0到31
     */
    private IdGenerator(final long workerId, final long datacenterId) {
        this.workerId = workerId;
        this.datacenterId = datacenterId;
        this.snowflake = new Snowflake(workerId, datacenterId);
    }

    /**
     * 获取默认Id生成器.
     *
     * <p>
     * 默认生成器从JVM参数或环境变量读取机器标识。JVM参数优先级高于环境变量：
     * union.id-generator.worker-id、union.id-generator.datacenter-id、
     * UNION_ID_GENERATOR_WORKER_ID、UNION_ID_GENERATOR_DATACENTER_ID。
     * 如果均未配置，则使用Hutool基于本机信息推导出的机器标识。
     * </p>
     *
     * @return 默认Id生成器
     */
    public static IdGenerator defaultGenerator() {
        IdGenerator generator = defaultGenerator;
        if (generator == null) {
            synchronized (IdGenerator.class) {
                generator = defaultGenerator;
                if (generator == null) {
                    // 默认实例只在首次使用时创建，
                    // 避免未使用静态方法的场景被配置项阻塞启动。
                    generator = createDefaultGenerator();
                    defaultGenerator = generator;
                }
            }
        }
        return generator;
    }

    /**
     * 创建指定机器标识的Id生成器.
     *
     * @param workerId     工作机器Id，取值范围0到31
     * @param datacenterId 数据中心Id，取值范围0到31
     * @return Id生成器
     */
    public static IdGenerator of(final long workerId, final long datacenterId) {
        validateWorkerId(workerId);
        validateDatacenterId(datacenterId);
        return new IdGenerator(workerId, datacenterId);
    }

    /**
     * 使用默认生成器生成Long类型Id.
     *
     * @return Long类型Id
     */
    public static long nextId() {
        return defaultGenerator().nextLong();
    }

    /**
     * 使用默认生成器生成字符串类型Id.
     *
     * @return 字符串类型Id
     */
    public static String nextIdStr() {
        return defaultGenerator().nextString();
    }

    /**
     * 使用默认生成器生成BigInteger类型Id.
     *
     * @return BigInteger类型Id
     */
    public static BigInteger nextIdBigInteger() {
        return defaultGenerator().nextBigInteger();
    }

    /**
     * 生成Long类型Id.
     *
     * @return Long类型Id
     */
    public long nextLong() {
        return snowflake.nextId();
    }

    /**
     * 生成字符串类型Id.
     *
     * @return 字符串类型Id
     */
    public String nextString() {
        return Long.toString(nextLong());
    }

    /**
     * 生成BigInteger类型Id.
     *
     * @return BigInteger类型Id
     */
    public BigInteger nextBigInteger() {
        return BigInteger.valueOf(nextLong());
    }

    /**
     * 使用默认生成器生成32位随机Key.
     *
     * @return 32位大写十六进制随机Key
     */
    public static String nextKey() {
        return defaultGenerator().nextKeyString();
    }

    /**
     * 使用默认生成器生成指定位数的随机Key.
     *
     * @param length Key长度，取值范围1到32
     * @return 指定位数的大写十六进制随机Key
     */
    public static String nextKey(final int length) {
        return defaultGenerator().nextKeyString(length);
    }

    /**
     * 生成32位随机Key.
     *
     * @return 32位大写十六进制随机Key
     */
    public String nextKeyString() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    /**
     * 生成指定位数的随机Key.
     *
     * @param length Key长度，取值范围1到32
     * @return 指定位数的大写十六进制随机Key
     */
    public String nextKeyString(final int length) {
        AssertUtils.isTrue(
                length >= MIN_KEY_LENGTH && length <= MAX_KEY_LENGTH,
                () -> new IllegalArgumentException(
                        "Key长度范围必须是" + MIN_KEY_LENGTH + "到" + MAX_KEY_LENGTH + "，当前值: " + length
                )
        );
        return nextKeyString().substring(0, length);
    }

    /**
     * 创建默认Id生成器.
     *
     * @return 默认Id生成器
     */
    private static IdGenerator createDefaultGenerator() {
        long resolvedDatacenterId = resolveLong(
                DATACENTER_ID_PROPERTY,
                DATACENTER_ID_ENV,
                IdUtil.getDataCenterId(MAX_NODE_ID)
        );
        long resolvedWorkerId = resolveLong(
                WORKER_ID_PROPERTY,
                WORKER_ID_ENV,
                IdUtil.getWorkerId(resolvedDatacenterId, MAX_NODE_ID)
        );
        return of(resolvedWorkerId, resolvedDatacenterId);
    }

    /**
     * 解析Long配置.
     *
     * @param propertyName JVM参数名
     * @param envName      环境变量名
     * @param defaultValue 默认值
     * @return 配置值
     */
    private static long resolveLong(final String propertyName, final String envName, final long defaultValue) {
        String configuredValue = System.getProperty(propertyName);
        if (isBlank(configuredValue)) {
            configuredValue = System.getenv(envName);
        }
        if (isBlank(configuredValue)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(configuredValue.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException(
                    "Id生成器配置必须是数字: JVM参数[" + propertyName
                            + "]或环境变量[" + envName + "]=" + configuredValue,
                    ex
            );
        }
    }

    /**
     * 校验工作机器Id.
     *
     * @param workerId 工作机器Id
     */
    private static void validateWorkerId(final long workerId) {
        validateNodeId("workerId", workerId);
    }

    /**
     * 校验数据中心Id.
     *
     * @param datacenterId 数据中心Id
     */
    private static void validateDatacenterId(final long datacenterId) {
        validateNodeId("datacenterId", datacenterId);
    }

    /**
     * 校验节点Id范围.
     *
     * @param name  节点名称
     * @param value 节点值
     */
    private static void validateNodeId(final String name, final long value) {
        if (value < MIN_NODE_ID || value > MAX_NODE_ID) {
            throw new IllegalArgumentException(
                    "Id生成器" + name + "取值范围必须是" + MIN_NODE_ID
                            + "到" + MAX_NODE_ID + "，当前值: " + value
            );
        }
    }

    /**
     * 判断字符串是否为空白.
     *
     * @param value 字符串
     * @return true表示为空白
     */
    private static boolean isBlank(final String value) {
        return value == null || value.trim().isEmpty();
    }

}
