package com.tellshell.app.network

/**
 * 思考深度（reasoning effort）。
 *
 * 取值对齐 DeepSeek 官方 `reasoning_effort` 枚举：
 * `none` / `low` / `high` / `max`。
 * 官方同时接受 `minimal`（映射为 low）与 `medium` / `xhigh`（映射为 high），
 * 但这里只暴露权威枚举，避免用户看到重复语义的档位。
 *
 * @param value   发送给 API 的字面值
 * @param label   UI 显示名
 * @param isThinking 是否启用思考模式（none 为关闭）
 */
enum class ReasoningEffort(
    val value: String,
    val label: String,
    val description: String,
    val isThinking: Boolean
) {
    NONE(
        value = "none",
        label = "关闭",
        description = "不启用思考模式，响应最快、最省 token",
        isThinking = false
    ),
    LOW(
        value = "low",
        label = "低",
        description = "轻量思考，适合简单直接的操作",
        isThinking = true
    ),
    HIGH(
        value = "high",
        label = "高",
        description = "官方默认思考深度，兼顾质量与速度",
        isThinking = true
    ),
    MAX(
        value = "max",
        label = "最高",
        description = "最深思考，适合复杂任务，耗时与消耗最高",
        isThinking = true
    );

    /**
     * OpenCode 网关侧的等价取值。
     *
     * OpenCode 官方 variant 枚举与 DeepSeek 并不完全一致：
     * OpenAI 系为 `none` / `minimal` / `low` / `medium` / `high` / `xhigh`，
     * 且没有 `max`。因此在 OpenCode 兼容开关打开时做一次映射，
     * 把 DeepSeek 的 `max` 降到 OpenCode 可接受的最高档 `xhigh`。
     */
    val opencodeValue: String
        get() = when (this) {
            NONE -> "none"
            LOW -> "low"
            HIGH -> "high"
            MAX -> "xhigh"
        }

    companion object {
        /** 默认档位（官方默认也是 high） */
        val DEFAULT = HIGH

        /**
         * 反序列化：同时兼容历史遗留值。
         *
         * 旧版本把「关闭」存成 `"disabled"`，且枚举里只有
         * disabled/high/max，这里统一迁移到新枚举，避免老用户配置失效。
         */
        fun fromString(value: String?): ReasoningEffort = when (value?.trim()?.lowercase()) {
            null, "" -> DEFAULT
            "disabled", "none", "off", "false" -> NONE
            "minimal", "low" -> LOW
            "medium", "high" -> HIGH
            "xhigh", "max" -> MAX
            else -> DEFAULT
        }
    }
}
