package com.tellshell.app.shell

import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.sui.Sui
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Shizuku/Sui 执行引擎
 *
 * 权限申请流程（参照 activity-manager）：
 *   Phase 1: 先走 Shizuku binder 通道
 *   Phase 2: Shizuku 无响应时 fallback 到 Sui
 *
 * 命令执行：Shizuku.newProcess("/system/bin/sh", "-c", command)
 */
class ShizukuExecutor {

    companion object {
        private const val TAG = "ShizukuExecutor"
        private const val SHIZUKU_REQUEST_CODE = 1001
    }

    /** 权限状态 */
    sealed class PermissionState {
        data object Idle : PermissionState()
        data object Requesting : PermissionState()
        data class Granted(val source: String) : PermissionState()
        data class Denied(val reason: String) : PermissionState()
    }

    /** 执行结果 */
    data class ExecuteResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String
    )

    private var _permissionState: PermissionState = PermissionState.Idle
    val permissionState: PermissionState get() = _permissionState

    private var permissionListener: Shizuku.OnRequestPermissionResultListener? = null

    /**
     * 请求 Shizuku/Sui 权限
     * Phase 1: Shizuku → Phase 2: Sui fallback
     */
    suspend fun requestPermission(
        onResult: (PermissionState) -> Unit
    ) {
        if (_permissionState is PermissionState.Granted) {
            _permissionState = PermissionState.Idle
        }

        _permissionState = PermissionState.Requesting
        onResult(_permissionState)

        // Phase 1: Shizuku
        val phase1Result = tryPhase1()
        if (phase1Result != null) {
            _permissionState = phase1Result
            onResult(_permissionState)
            return
        }

        // Phase 2: Sui fallback
        val phase2Result = tryPhase2()
        _permissionState = phase2Result
        onResult(_permissionState)
    }

    private suspend fun tryPhase1(): PermissionState? {
        return try {
            val deferred = kotlinx.coroutines.CompletableDeferred<Int?>()

            permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
                if (requestCode == SHIZUKU_REQUEST_CODE) {
                    deferred.complete(grantResult)
                }
            }

            Shizuku.addRequestPermissionResultListener(permissionListener!!)
            Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)

            val result = withContext(Dispatchers.IO) {
                try {
                    kotlinx.coroutines.withTimeout(5000L) {
                        deferred.await()
                    }
                } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                    null
                }
            }

            when {
                result == PackageManager.PERMISSION_GRANTED -> {
                    PermissionState.Granted(source = detectPermissionSource())
                }
                result == null -> {
                    Log.w(TAG, "Shizuku 无响应，尝试 Sui fallback")
                    null // fallback
                }
                else -> {
                    PermissionState.Denied(reason = "Shizuku 权限被拒绝")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Shizuku 不可用: ${e.message}")
            null // fallback
        } finally {
            permissionListener?.let { Shizuku.removeRequestPermissionResultListener(it) }
            permissionListener = null
        }
    }

    private suspend fun tryPhase2(): PermissionState {
        return try {
            // 手动初始化 Sui
            val suiAvailable = withContext(Dispatchers.IO) {
                try {
                    Sui.init("com.tellshell.app")
                } catch (_: Exception) {
                    false
                }
            }

            if (!suiAvailable) {
                return PermissionState.Denied(reason = "Sui 不可用")
            }

            val deferred = kotlinx.coroutines.CompletableDeferred<Int?>()

            permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
                if (requestCode == SHIZUKU_REQUEST_CODE) {
                    deferred.complete(grantResult)
                }
            }

            Shizuku.addRequestPermissionResultListener(permissionListener!!)
            Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)

            val result = withContext(Dispatchers.IO) {
                try {
                    kotlinx.coroutines.withTimeout(5000L) {
                        deferred.await()
                    }
                } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                    null
                }
            }

            when {
                result == PackageManager.PERMISSION_GRANTED -> {
                    PermissionState.Granted(source = detectPermissionSource())
                }
                else -> {
                    PermissionState.Denied(reason = "Sui 权限被拒绝或超时")
                }
            }
        } catch (e: Exception) {
            PermissionState.Denied(reason = "Sui 不可用: ${e.message}")
        } finally {
            permissionListener?.let { Shizuku.removeRequestPermissionResultListener(it) }
            permissionListener = null
        }
    }

    /**
     * 检测当前权限来源
     * 返回如 "sui(root)"、"shizuku(shell)" 等
     */
    private fun detectPermissionSource(): String {
        return try {
            val uid = Shizuku.getUid()
            val level = when (uid) {
                0 -> "root"
                2000 -> "shell"
                else -> "uid=$uid"
            }
            val source = try {
                if (Sui.isSui()) "sui" else "shizuku"
            } catch (_: Exception) {
                "shizuku"
            }
            "$source($level)"
        } catch (_: Exception) {
            "shizuku(?)"
        }
    }

    /**
     * 执行 shell 命令
     * @param command 要执行的命令文本
     * @return ExecuteResult (exitCode, stdout, stderr)
     */
    suspend fun execute(command: String): ExecuteResult = withContext(Dispatchers.IO) {
        try {
            val process = Shizuku.newProcess(
                arrayOf("/system/bin/sh", "-c", command),
                null,
                null
            )

            // 读取 stdout
            val stdout = readStream(process.inputStream)
            val stderr = readStream(process.errorStream)

            val exitCode = process.waitFor()

            ExecuteResult(
                exitCode = exitCode,
                stdout = stdout,
                stderr = stderr
            )
        } catch (e: Exception) {
            ExecuteResult(
                exitCode = -1,
                stdout = "",
                stderr = "Execution error: ${e.message}"
            )
        }
    }

    private fun readStream(inputStream: InputStream): String {
        return try {
            inputStream.bufferedReader().use { it.readText().trim() }
        } catch (_: Exception) {
            ""
        }
    }
}
