package com.kmq.kmqsamplebody.position

import android.os.Bundle
import android.os.RemoteException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ainirobot.coreservice.client.Definition
import com.ainirobot.coreservice.client.RobotApi
import com.ainirobot.coreservice.client.StatusListener
import com.ainirobot.coreservice.client.listener.ActionListener
import com.ainirobot.coreservice.client.listener.CommandListener
import com.kmq.kmqsamplebody.R
import org.json.JSONArray

/**
 * 点位导航页面
 *
 * 功能：
 * 1. 自动检测定位状态并展示（STATUS_POSE_ESTIMATE）
 * 2. 获取已配置点位列表，以 Grid 网格形式展示
 * 3. 点击点位卡片，导航到该点位（startNavigation）
 *
 * 导航 API：
 * startNavigation(reqId, destinationName, speed, timeout, listener)
 *   - destinationName: 目标点位名称（与 getPlaceList 返回的 name 对应）
 *   - speed: 导航速度（m/s），推荐 0.5
 *   - timeout: 超时时间（ms），0 表示不超时
 */
class PositionActivity : AppCompatActivity() {

    private var reqId = 0
    private var statusListener: StatusListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_position)

        findViewById<Button>(R.id.btn_back_home).setOnClickListener { finish() }

        val tvPoseStatus = findViewById<TextView>(R.id.tv_pose_status)
        val tvNavStatus = findViewById<TextView>(R.id.tv_nav_status)
        val tvHint = findViewById<TextView>(R.id.tv_positions_hint)
        val btnGetPositions = findViewById<Button>(R.id.btn_get_positions)
        val rvPositions = findViewById<RecyclerView>(R.id.rv_positions)

        rvPositions.layoutManager = GridLayoutManager(this, 3)

        // 注册定位状态监听
        statusListener = object : StatusListener() {
            override fun onStatusUpdate(type: String?, data: String?) {
                runOnUiThread {
                    val statusText = when (data) {
                        "0" -> "未定位"
                        "1" -> "已定位 ✓"
                        else -> "未知（$data）"
                    }
                    tvPoseStatus.text = "定位状态：$statusText"
                }
            }
        }
        RobotApi.getInstance().registerStatusListener(Definition.STATUS_POSE_ESTIMATE, statusListener)
        RobotApi.getInstance().getRobotStatus(Definition.STATUS_POSE_ESTIMATE, statusListener)

        btnGetPositions.setOnClickListener {
            tvNavStatus.text = "正在获取点位列表..."
            RobotApi.getInstance().getPlaceList(reqId++, object : CommandListener() {
                override fun onResult(result: Int, message: String?) {
                    runOnUiThread {
                        try {
                            val jsonArray = JSONArray(message ?: "[]")
                            if (jsonArray.length() == 0) {
                                tvNavStatus.text = "暂无已配置的点位"
                                tvHint.visibility = View.GONE
                                return@runOnUiThread
                            }
                            val positions = mutableListOf<PositionItem>()
                            for (i in 0 until jsonArray.length()) {
                                val json = jsonArray.getJSONObject(i)
                                positions.add(
                                    PositionItem(
                                        name = json.optString("name", "未知"),
                                        x = json.optDouble("x", 0.0),
                                        y = json.optDouble("y", 0.0),
                                        theta = json.optDouble("theta", 0.0)
                                    )
                                )
                            }
                            tvNavStatus.text = "共 ${positions.size} 个点位"
                            tvHint.visibility = View.VISIBLE
                            rvPositions.adapter = PositionAdapter(positions) { item ->
                                navigateTo(item.name, tvNavStatus)
                            }
                        } catch (e: Exception) {
                            tvNavStatus.text = "解析失败: $message"
                        }
                    }
                }
            })
        }
    }

    private fun navigateTo(name: String, tvNavStatus: TextView) {
        tvNavStatus.text = "正在导航到【$name】..."
        RobotApi.getInstance().startNavigation(
            reqId++, name, 0.5, 0L,
            object : ActionListener() {
                @Throws(RemoteException::class)
                override fun onResult(status: Int, responseString: String?) {
                    runOnUiThread {
                        tvNavStatus.text = "导航结果：status=$status $responseString"
                    }
                }

                @Throws(RemoteException::class)
                override fun onStatusUpdate(status: Int, data: String?) {
                    runOnUiThread {
                        tvNavStatus.text = "导航中【$name】status=$status $data"
                    }
                }

                @Throws(RemoteException::class)
                override fun onError(errorCode: Int, errorString: String?) {
                    runOnUiThread {
                        tvNavStatus.text = "导航失败：error=$errorCode $errorString"
                    }
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        statusListener?.let {
            RobotApi.getInstance().unregisterStatusListener(it)
        }
    }
}

data class PositionItem(
    val name: String,
    val x: Double,
    val y: Double,
    val theta: Double
)

class PositionAdapter(
    private val items: List<PositionItem>,
    private val onClick: (PositionItem) -> Unit
) : RecyclerView.Adapter<PositionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_position_name)
        val tvCoords: TextView = view.findViewById(R.id.tv_position_coords)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_position, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvCoords.text = "(${String.format("%.1f", item.x)}, ${String.format("%.1f", item.y)})"
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}
