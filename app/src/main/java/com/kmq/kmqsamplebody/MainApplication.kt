package com.kmq.kmqsamplebody

import android.app.Application
import android.os.Bundle
import android.util.Log
import com.ainirobot.agent.AppAgent
import com.ainirobot.agent.action.Action
import com.ainirobot.agent.action.Actions
import com.ainirobot.coreservice.client.ApiListener
import com.ainirobot.coreservice.client.RobotApi

class MainApplication : Application() {

    lateinit var appAgent: AppAgent
    var isRobotApiConnected = false
        private set

    companion object {
        private const val TAG = "MainApplication"
    }

    override fun onCreate() {
        super.onCreate()

        appAgent = object : AppAgent(this@MainApplication) {
            override fun onCreate() {
                setPersona("你是科梦奇机器人的示例应用助手，负责展示各项SDK功能。")
                registerAction(Actions.SAY)
            }

            override fun onExecuteAction(action: Action, params: Bundle?): Boolean {
                return false
            }
        }

        try {
            RobotApi.getInstance().connectServer(this, object : ApiListener {
                override fun handleApiConnected() {
                    isRobotApiConnected = true
                    Log.i(TAG, "RobotApi 连接成功")
                }

                override fun handleApiDisconnected() {
                    isRobotApiConnected = false
                    Log.w(TAG, "RobotApi 连接断开")
                }

                override fun handleApiDisabled() {
                    isRobotApiConnected = false
                    Log.w(TAG, "RobotApi 不可用")
                }
            })
        } catch (e: SecurityException) {
            isRobotApiConnected = false
            Log.e(TAG, "RobotApi 连接失败（权限不足，请确认从机器人桌面启动应用）: ${e.message}")
        }
    }
}
