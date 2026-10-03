package com.devin.uniontalk.cache.utils;

import cn.hutool.extra.spring.SpringUtil;
import com.devin.uniontalk.base.cursor.enums.CursorOrderEnum;
import com.devin.uniontalk.base.cursor.model.CursorPageQuery;
import com.devin.uniontalk.base.cursor.model.CursorPageResult;
import com.devin.uniontalk.base.cursor.model.RedisScanCursorQuery;
import com.devin.uniontalk.base.cursor.model.RedisScanCursorResult;
import com.devin.uniontalk.base.cursor.utils.CursorPageUtils;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.util.StringUtils;

/**
 * 2026/05/20 13:05.
 *
 * <p>
 * Redis 游标分页工具类
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public final class RedisCursorPageUtils {

    /**
     * Redis Scan 初始游标.
     */
    private static final String SCAN_INIT_CURSOR = "0";

    /**
     * Redis Scan 完成游标.
     */
    private static final String SCAN_FINISHED_CURSOR = "0";

    /**
     * Redis MATCH 参数.
     */
    private static final String SCAN_MATCH = "MATCH";

    /**
     * Redis COUNT 参数.
     */
    private static final String SCAN_COUNT = "COUNT";

    /**
     * Redis SCAN 命令.
     */
    private static final String SCAN_COMMAND = "SCAN";

    /**
     * Redis HSCAN 命令.
     */
    private static final String HSCAN_COMMAND = "HSCAN";

    /**
     * Redis SSCAN 命令.
     */
    private static final String SSCAN_COMMAND = "SSCAN";

    /**
     * Redis ZSCAN 命令.
     */
    private static final String ZSCAN_COMMAND = "ZSCAN";

    /**
     * Redis Lex 固定分值.
     */
    private static final double LEX_SCORE = 0D;

    /**
     * 雪花算法游标长度.
     */
    private static final int SNOWFLAKE_CURSOR_LENGTH = 19;

    /**
     * 游标补位字符.
     */
    private static final char CURSOR_PADDING_CHAR = '0';

    /**
     * StringRedisTemplate 实例.
     */
    private static StringRedisTemplate redisTemplate;

    private RedisCursorPageUtils() {
    }

    /**
     * 获取 StringRedisTemplate.
     *
     * @return StringRedisTemplate
     */
    private static StringRedisTemplate getRedisTemplate() {
        if (Objects.isNull(redisTemplate)) {
            redisTemplate = SpringUtil.getBean(StringRedisTemplate.class);
        }
        return redisTemplate;
    }

    /**
     * 格式化雪花算法id为可排序游标.
     *
     * @param id 雪花算法id
     * @return 可排序游标
     */
    public static String formatSnowflakeCursor(final BigInteger id) {
        Objects.requireNonNull(id, "雪花算法id不能为空");
        String idText = id.toString();
        if (idText.length() >= SNOWFLAKE_CURSOR_LENGTH) {
            return idText;
        }
        return String.valueOf(CURSOR_PADDING_CHAR).repeat(SNOWFLAKE_CURSOR_LENGTH - idText.length()) + idText;
    }

    /**
     * 添加 Lex 有序集合成员.
     *
     * @param key   缓存Key
     * @param value 可排序成员值
     * @return 添加结果
     */
    public static Boolean zAddLexValue(final String key, final String value) {
        Objects.requireNonNull(key, "缓存Key不能为空");
        Objects.requireNonNull(value, "可排序成员值不能为空");
        return getRedisTemplate().opsForZSet().add(key, value, LEX_SCORE);
    }

    /**
     * 添加雪花算法 Lex 有序集合成员.
     *
     * @param key 缓存Key
     * @param id  雪花算法id
     * @return 添加结果
     */
    public static Boolean zAddSnowflakeLexValue(final String key, final BigInteger id) {
        return zAddLexValue(key, formatSnowflakeCursor(id));
    }

    /**
     * 使用 ZSet Lex 执行游标分页.
     *
     * @param key         缓存Key
     * @param cursorQuery 游标分页查询参数
     * @return 游标分页响应结果
     */
    public static CursorPageResult<String, String> zSetLexPage(
            final String key,
            final CursorPageQuery<String> cursorQuery
    ) {
        Objects.requireNonNull(key, "缓存Key不能为空");
        CursorPageQuery<String> safeCursorQuery = safeCursorQuery(cursorQuery);
        CursorOrderEnum order = CursorPageUtils.normalizeOrder(safeCursorQuery.getOrder());
        Range<String> range = buildLexRange(safeCursorQuery.getCursorValue(), order);
        Limit limit = Limit.limit()
                .offset(0)
                .count(CursorPageUtils.limitSize(safeCursorQuery.getPageSize()));
        Set<String> sourceSet = order.isAsc()
                ? getRedisTemplate().opsForZSet().rangeByLex(key, range, limit)
                : getRedisTemplate().opsForZSet().reverseRangeByLex(key, range, limit);
        List<String> sourceList = Objects.nonNull(sourceSet) ? new ArrayList<>(sourceSet) : List.of();
        return CursorPageUtils.buildPageResult(
                sourceList,
                safeCursorQuery.getPageSize(),
                Function.identity(),
                null
        );
    }

    /**
     * 使用 SCAN 执行 Key 游标扫描.
     *
     * @param scanQuery Redis Scan 游标查询参数
     * @return Redis Scan 游标响应结果
     */
    public static RedisScanCursorResult<String> scan(final RedisScanCursorQuery scanQuery) {
        return scanCommand(SCAN_COMMAND, null, scanQuery, RedisCursorPageUtils::appendStringValues);
    }

    /**
     * 使用 HSCAN 执行 Hash 游标扫描.
     *
     * @param key       缓存Key
     * @param scanQuery Redis Scan 游标查询参数
     * @return Redis Scan 游标响应结果
     */
    public static RedisScanCursorResult<Map.Entry<String, String>> hScan(
            final String key,
            final RedisScanCursorQuery scanQuery
    ) {
        Objects.requireNonNull(key, "缓存Key不能为空");
        return scanCommand(HSCAN_COMMAND, key, scanQuery, RedisCursorPageUtils::appendHashValues);
    }

    /**
     * 使用 SSCAN 执行 Set 游标扫描.
     *
     * @param key       缓存Key
     * @param scanQuery Redis Scan 游标查询参数
     * @return Redis Scan 游标响应结果
     */
    public static RedisScanCursorResult<String> sScan(final String key, final RedisScanCursorQuery scanQuery) {
        Objects.requireNonNull(key, "缓存Key不能为空");
        return scanCommand(SSCAN_COMMAND, key, scanQuery, RedisCursorPageUtils::appendStringValues);
    }

    /**
     * 使用 ZSCAN 执行 ZSet 游标扫描.
     *
     * @param key       缓存Key
     * @param scanQuery Redis Scan 游标查询参数
     * @return Redis Scan 游标响应结果
     */
    public static RedisScanCursorResult<ZSetOperations.TypedTuple<String>> zScan(
            final String key,
            final RedisScanCursorQuery scanQuery
    ) {
        Objects.requireNonNull(key, "缓存Key不能为空");
        return scanCommand(ZSCAN_COMMAND, key, scanQuery, RedisCursorPageUtils::appendZSetValues);
    }

    /**
     * 获取安全游标查询参数.
     *
     * @param cursorQuery 游标分页查询参数
     * @return 游标分页查询参数
     */
    private static CursorPageQuery<String> safeCursorQuery(final CursorPageQuery<String> cursorQuery) {
        return Objects.nonNull(cursorQuery) ? cursorQuery : new CursorPageQuery<>();
    }

    /**
     * 构建 Lex 查询范围.
     *
     * @param cursorValue 游标值
     * @param order       排序方向
     * @return Lex 查询范围
     */
    private static Range<String> buildLexRange(final String cursorValue, final CursorOrderEnum order) {
        if (!StringUtils.hasLength(cursorValue)) {
            return Range.unbounded();
        }
        Range.Bound<String> cursorBound = Range.Bound.exclusive(cursorValue);
        return order.isAsc() ? Range.rightUnbounded(cursorBound) : Range.leftUnbounded(cursorBound);
    }

    /**
     * 执行 Redis Scan 命令.
     *
     * @param command   Redis Scan 命令
     * @param key       缓存Key
     * @param scanQuery Redis Scan 游标查询参数
     * @param appender  扫描结果追加器
     * @param <T>       扫描结果类型
     * @return Redis Scan 游标响应结果
     */
    private static <T> RedisScanCursorResult<T> scanCommand(
            final String command,
            final String key,
            final RedisScanCursorQuery scanQuery,
            final ScanValueAppender<T> appender
    ) {
        RedisScanCursorQuery safeScanQuery = safeScanQuery(scanQuery);
        int pageSize = CursorPageUtils.normalizePageSize(safeScanQuery.getPageSize());
        List<T> resultList = new ArrayList<>(pageSize);
        String nextCursor = normalizeScanCursor(safeScanQuery.getCursor());
        boolean finished;
        do {
            int remainSize = pageSize - resultList.size();
            ScanCommandResult commandResult = executeScanCommand(
                    command,
                    key,
                    nextCursor,
                    safeScanQuery.getPattern(),
                    remainSize
            );
            nextCursor = commandResult.nextCursor();
            appender.append(resultList, commandResult.valueList());
            finished = SCAN_FINISHED_CURSOR.equals(nextCursor);
        } while (resultList.size() < pageSize && !finished);

        return RedisScanCursorResult.<T>builder()
                .list(resultList)
                .nextCursor(nextCursor)
                .finished(finished)
                .build();
    }

    /**
     * 获取安全 Scan 查询参数.
     *
     * @param scanQuery Redis Scan 游标查询参数
     * @return Redis Scan 游标查询参数
     */
    private static RedisScanCursorQuery safeScanQuery(final RedisScanCursorQuery scanQuery) {
        return Objects.nonNull(scanQuery) ? scanQuery : new RedisScanCursorQuery();
    }

    /**
     * 归一化 Redis Scan 游标.
     *
     * @param cursor Redis Scan 游标
     * @return Redis Scan 游标
     */
    private static String normalizeScanCursor(final String cursor) {
        return StringUtils.hasLength(cursor) ? cursor : SCAN_INIT_CURSOR;
    }

    /**
     * 执行原生 Redis Scan 命令.
     *
     * @param command Redis Scan 命令
     * @param key     缓存Key
     * @param cursor  Redis Scan 游标
     * @param pattern 匹配表达式
     * @param count   单次扫描数量
     * @return Redis Scan 命令响应
     */
    private static ScanCommandResult executeScanCommand(
            final String command,
            final String key,
            final String cursor,
            final String pattern,
            final int count
    ) {
        List<byte[]> argList = buildScanArgList(key, cursor, pattern, count);
        Object response = getRedisTemplate().execute((RedisCallback<Object>) connection ->
                connection.execute(command, argList.toArray(new byte[0][])));
        return parseScanCommandResult(response);
    }

    /**
     * 构建 Redis Scan 命令参数.
     *
     * @param key     缓存Key
     * @param cursor  Redis Scan 游标
     * @param pattern 匹配表达式
     * @param count   单次扫描数量
     * @return Redis Scan 命令参数
     */
    private static List<byte[]> buildScanArgList(
            final String key,
            final String cursor,
            final String pattern,
            final int count
    ) {
        List<byte[]> argList = new ArrayList<>();
        if (StringUtils.hasLength(key)) {
            argList.add(toBytes(key));
        }
        argList.add(toBytes(cursor));
        if (StringUtils.hasLength(pattern)) {
            argList.add(toBytes(SCAN_MATCH));
            argList.add(toBytes(pattern));
        }
        argList.add(toBytes(SCAN_COUNT));
        argList.add(toBytes(String.valueOf(count)));
        return argList;
    }

    /**
     * 解析 Redis Scan 命令响应.
     *
     * @param response Redis Scan 命令响应
     * @return Redis Scan 命令响应
     */
    private static ScanCommandResult parseScanCommandResult(final Object response) {
        List<Object> responseList = toObjectList(response);
        if (responseList.size() < 2) {
            return new ScanCommandResult(SCAN_FINISHED_CURSOR, List.of());
        }
        return new ScanCommandResult(toText(responseList.getFirst()), toObjectList(responseList.get(1)));
    }

    /**
     * 追加字符串扫描结果.
     *
     * @param resultList 扫描结果列表
     * @param valueList  Redis 原始结果列表
     */
    private static void appendStringValues(
            final List<String> resultList,
            final List<Object> valueList
    ) {
        for (Object value : valueList) {
            resultList.add(toText(value));
        }
    }

    /**
     * 追加 Hash 扫描结果.
     *
     * @param resultList 扫描结果列表
     * @param valueList  Redis 原始结果列表
     */
    private static void appendHashValues(
            final List<Map.Entry<String, String>> resultList,
            final List<Object> valueList
    ) {
        for (int index = 0; index < valueList.size();) {
            Object value = valueList.get(index);
            if (value instanceof Map.Entry<?, ?> entry) {
                resultList.add(Map.entry(Objects.requireNonNull(toText(entry.getKey())), Objects.requireNonNull(toText(entry.getValue()))));
                index++;
                continue;
            }
            List<Object> pairList = toObjectList(value);
            if (pairList.size() >= 2) {
                resultList.add(Map.entry(Objects.requireNonNull(toText(pairList.get(0))), Objects.requireNonNull(toText(pairList.get(1)))));
                index++;
                continue;
            }
            if (index + 1 >= valueList.size()) {
                return;
            }
            resultList.add(Map.entry(Objects.requireNonNull(toText(valueList.get(index))), Objects.requireNonNull(toText(valueList.get(index + 1)))));
            index += 2;
        }
    }

    /**
     * 追加 ZSet 扫描结果.
     *
     * @param resultList 扫描结果列表
     * @param valueList  Redis 原始结果列表
     */
    private static void appendZSetValues(
            final List<ZSetOperations.TypedTuple<String>> resultList,
            final List<Object> valueList
    ) {
        for (int index = 0; index < valueList.size();) {
            Object value = valueList.get(index);
            List<Object> pairList = toObjectList(value);
            if (pairList.size() >= 2) {
                addZSetValue(resultList, pairList.get(0), pairList.get(1));
                index++;
                continue;
            }
            if (index + 1 >= valueList.size()) {
                return;
            }
            addZSetValue(resultList, valueList.get(index), valueList.get(index + 1));
            index += 2;
        }
    }

    /**
     * 添加 ZSet 扫描结果.
     *
     * @param resultList 扫描结果列表
     * @param value      成员值
     * @param score      分值
     */
    private static void addZSetValue(
            final List<ZSetOperations.TypedTuple<String>> resultList,
            final Object value,
            final Object score
    ) {
        Double parsedScore = parseScore(score);
        if (Objects.isNull(parsedScore)) {
            return;
        }
        resultList.add(new DefaultTypedTuple<>(toText(value), parsedScore));
    }

    /**
     * 解析 ZSet 分值.
     *
     * @param score 分值
     * @return ZSet 分值
     */
    private static Double parseScore(final Object score) {
        try {
            return Double.valueOf(Objects.requireNonNull(toText(score)));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 转换为对象列表.
     *
     * @param value 原始对象
     * @return 对象列表
     */
    private static List<Object> toObjectList(final Object value) {
        if (value instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        if (value instanceof Object[] array) {
            return new ArrayList<>(Arrays.asList(array));
        }
        if (value instanceof Set<?> set) {
            return new ArrayList<>(set);
        }
        return List.of();
    }

    /**
     * 转换为字符串.
     *
     * @param value 原始对象
     * @return 字符串
     */
    private static String toText(final Object value) {
        if (Objects.isNull(value)) {
            return null;
        }
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return value.toString();
    }

    /**
     * 转换为字节数组.
     *
     * @param value 字符串
     * @return 字节数组
     */
    private static byte[] toBytes(final String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Scan 扫描结果追加器.
     *
     * @param <T> 扫描结果类型
     */
    @FunctionalInterface
    private interface ScanValueAppender<T> {

        /**
         * 追加扫描结果.
         *
         * @param resultList 扫描结果列表
         * @param valueList  Redis 原始结果列表
         */
        void append(List<T> resultList, List<Object> valueList);
    }

    /**
     * Redis Scan 命令响应.
     *
     * @param nextCursor 下一次扫描游标.
     * @param valueList  Redis 原始结果列表.
     */
    private record ScanCommandResult(String nextCursor, List<Object> valueList) {

        /**
         * 创建 Redis Scan 命令响应.
         *
         * @param nextCursor 下一次扫描游标
         * @param valueList  Redis 原始结果列表
         */
        private ScanCommandResult {
        }

        /**
         * 获取下一次扫描游标.
         *
         * @return 下一次扫描游标
         */
        @Override
        public String nextCursor() {
            return nextCursor;
        }

        /**
         * 获取 Redis 原始结果列表.
         *
         * @return Redis 原始结果列表
         */
        @Override
        public List<Object> valueList() {
            return valueList;
        }
    }
}
