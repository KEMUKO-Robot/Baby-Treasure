package com.kmq.kmqsamplebody.charging

import android.os.Bundle
import android.os.RemoteException
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ainirobot.coreservice.client.RobotApi
import com.ainirobot.coreservice.client.listener.ActionListener
import com.kmq.kmqsamplebody.R

class ChargingActivity : AppCompatActivity() {

    private var reqId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_charging)

        val tvStatus = findViewById<TextView>(R.id.tv_charging_status)
        val btnGoCharge = findViewById<Button>(R.id.btn_go_charge)
        val btnStopCharge = findViewById<Button>(R.id.btn_stop_charge)

        btnGoCharge.setOnClickListener {
            tvStatus.text = "前往充电桩..."
            RobotApi.getInstance().startNaviToAutoChargeAction(reqId++, 60, object : ActionListener() {
                @Throws(RemoteException::class)
                override fun onResult(status: Int, responseString: String?) {
                    runOnUiThread { tvStatus.text = "充电结果: status=$status $responseString" }
                }

                @Throws(RemoteException::class)
                override fun onStatusUpdate(status: Int, data: String?) {
                    runOnUiThread { tvStatus.text = "状态更新: status=$status $data" }
                }
            })
        }

        btnStopCharge.setOnClickListener {
            RobotApi.getInstance().stopAutoChargeAction(reqId++, true)
            tvStatus.text = "已停止充电"
        }
    }
}
