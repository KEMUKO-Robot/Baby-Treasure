package com.kmq.kmqsamplebody.systeminfo

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ainirobot.coreservice.client.Definition
import com.ainirobot.coreservice.client.RobotApi
import com.ainirobot.coreservice.client.StatusListener
import com.ainirobot.coreservice.client.listener.CommandListener
import com.ainirobot.coreservice.client.robotsetting.RobotSettingApi
import com.kmq.kmqsamplebody.R

/**
 * 系统信息页面 — 展示机器人基本信息和电池状态
 *
 * ## 功能说明
 *
 * | 功能         | API                                                              | 类型   |
 * |-------------|------------------------------------------------------------------|--------|
 * | 系统版本号   | RobotApi.getInstance().getVersion()                              | 同步   |
 * | 设备 SN     | RobotApi.getInstance().getRobotSn(CommandListener)               | 异步   |
 * | 电池快照     | RobotSettingApi.getInstance().getRobotString(BATTERY_INFO)       | 同步   |
 * | 电池实时监听 | RobotApi.registerStatusListener(STATUS_BATTERY, StatusListener)  | 持续回调 |
 *
 * ## 相关系统 API（本页面未使用，但属于同类能力，详见 README 5.4）
 *
 * - **禁用急停画面**：RobotApi.getInstance().disableEmergency()
 *     系统不再接管急停事件，可用于自定义急停 UI。急停下底盘 API 全部不可用。
 *
 * - **禁用电池界面**：RobotApi.getInstance().disableBattery()
 *     充电时不弹出系统充电画面，APP 可使用除底盘外任何能力。
 *     推荐在 handleApiConnected 后立即调用。也是 leaveChargingPile 的前置条件。
 *
 * - **禁用功能键**：RobotApi.getInstance().disableFunctionKey()
 *     禁用机器人头部后面的物理按钮。
 *
 * - **休眠**：RobotApi.getInstance().robotStandby(reqId, listener)
 *     低功耗待机。需要权限 com.ainirobot.coreservice.robotSettingProvider。
 *     停止休眠：RobotApi.getInstance().robotStandbyEnd(reqId)
 *
 * - **唤醒**：RobotApi.getInstance().wakeUp(reqId, angle, ActionListener)
 *     根据声源定位角度转动到用户方向。
 *     小秘策略：<45° 仅转云台，>45° 云台+底盘联动。
 *     mini策略：仅转动底盘。
 *     停止唤醒：RobotApi.getInstance().stopWakeUp(reqId)
 *
 * - **自动回充**：RobotApi.getInstance().startNaviToAutoChargeAction(reqId, timeout, ActionListener)
 *     导航至充电桩并开始充电。timeout 为导航超时（ms）。
 *     停止回充：RobotApi.getInstance().stopAutoChargeAction(reqId, true)
 *
 * - **脱离充电桩**：RobotApi.getInstance().leaveChargingPile(reqId, speed, distance, CommandListener)
 *     必须先调用 disableBattery()。speed 默认 0.7，distance 默认 0.2 米。
 *     线充方式无法使用。离桩后充电状态更新有延迟。
 */
class SystemInfoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SystemInfoActivity"
    }

    private var reqId = 0

    /**
     * 电池状态监听器，在 onCreate 中注册，onDestroy 中反注册。
     * 通过 STATUS_BATTERY 持续接收电池状态变化（充电开始/结束、电量变化、低电量报警等）。
     */
    private var batteryListener: StatusListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_system_info)

        findViewById<Button>(R.id.btn_back_home).setOnClickListener { finish() }

        val tvVersion = findViewById<TextView>(R.id.tv_version)
        val tvSn = findViewById<TextView>(R.id.tv_sn)
        val tvBattery = findViewById<TextView>(R.id.tv_battery)
        val tvBatteryLive = findViewById<TextView>(R.id.tv_battery_live)
        val btnRefresh = findViewById<Button>(R.id.btn_refresh_all)

        btnRefresh.setOnClickListener {
            fetchVersion(tvVersion)
            fetchSn(tvSn)
            fetchBattery(tvBattery)
        }

        fetchVersion(tvVersion)
        fetchSn(tvSn)
        fetchBattery(tvBattery)

        // STATUS_BATTERY 监听：电池状态变化时持续回调
        // data 为 JSON 字符串，包含 isCharging, percent, lowBattery 等字段
        batteryListener = object : StatusListener() {
            override fun onStatusUpdate(type: String?, data: String?) {
                runOnUiThread {
                    tvBatteryLive.text = "电池实时状态：$data"
                }
            }
        }
        try {
            RobotApi.getInstance().registerStatusListener(Definition.STATUS_BATTERY, batteryListener)
        } catch (e: Exception) {
            tvBatteryLive.text = "电池监听注册失败: ${e.message}"
            Log.e(TAG, "registerStatusListener error", e)
        }
    }

    /**
     * 获取系统版本号（同步方法）
     *
     * API: RobotApi.getInstance().getVersion()
     * 返回值: 版本号字符串，如 "11.3.2"；获取失败时返回 null
     */
    private fun fetchVersion(tv: TextView) {
        try {
            val version = RobotApi.getInstance().version
            tv.text = if (!version.isNullOrEmpty()) "系统版本：$version" else "系统版本：获取失败"
        } catch (e: Exception) {
            tv.text = "系统版本：获取失败（${e.message}）"
            Log.e(TAG, "getVersion error", e)
        }
    }

    /**
     * 获取设备 SN 序列号（异步方法）
     *
     * API: RobotApi.getInstance().getRobotSn(CommandListener)
     * 回调: onResult(result, message)
     *   - result == Definition.RESULT_OK (1) 时 message 为 SN 字符串
     *   - result == -1 时表示命令未执行
     */
    private fun fetchSn(tv: TextView) {
        tv.text = "SN：获取中..."
        try {
            RobotApi.getInstance().getRobotSn(object : CommandListener() {
                override fun onResult(result: Int, message: String?) {
                    runOnUiThread {
                        if (result == Definition.RESULT_OK && !message.isNullOrEmpty()) {
                            tv.text = "SN：$message"
                        } else {
                            tv.text = "SN：获取失败（result=$result）"
                        }
                    }
                }
            })
        } catch (e: Exception) {
            tv.text = "SN：获取失败（${e.message}）"
            Log.e(TAG, "getRobotSn error", e)
        }
    }

    /**
     * 获取电池信息快照（同步方法）
     *
     * API: RobotSettingApi.getInstance().getRobotString(Definition.ROBOT_SETTINGS_BATTERY_INFO)
     * 返回值: JSON 字符串，包含电量百分比、是否正在充电等信息
     * 权限: 需要 com.ainirobot.coreservice.robotSettingProvider
     */
    private fun fetchBattery(tv: TextView) {
        try {
            val batteryInfo = RobotSettingApi.getInstance()
                .getRobotString(Definition.ROBOT_SETTINGS_BATTERY_INFO)
            tv.text = if (!batteryInfo.isNullOrEmpty()) "电池信息：$batteryInfo" else "电池信息：获取失败"
        } catch (e: Exception) {
            tv.text = "电池信息：获取失败（${e.message}）"
            Log.e(TAG, "getBatteryInfo error", e)
        }
    }

    /**
     * 页面销毁时反注册电池监听，防止内存泄漏。
     * 所有通过 registerStatusListener 注册的监听器，
     * 都必须在不再需要时调用 unregisterStatusListener 取消。
     */
    override fun onDestroy() {
        super.onDestroy()
        batteryListener?.let {
            try {
                RobotApi.getInstance().unregisterStatusListener(it)
            } catch (e: Exception) {
                Log.e(TAG, "unregisterStatusListener error", e)
            }
        }
    }
}
