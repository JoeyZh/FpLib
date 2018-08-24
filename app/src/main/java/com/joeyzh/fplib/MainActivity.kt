package com.joeyzh.fplib

import android.os.Bundle
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.joeyzh.fplibrary.FingerprintHelper
import kotlinx.android.synthetic.main.fp_activity_main.*

/**
 * Created by Joey on 2018/8/22.
 */
class MainActivity : AppCompatActivity() {

    var fingerprintHelper: FingerprintHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fp_activity_main)
        fingerprintHelper = FingerprintHelper(this@MainActivity)

        if (!fingerprintHelper!!.isHardwareDetected) {
            showDialog("硬件不支持！")
            return
        }
        if (!fingerprintHelper!!.isKeyguardSecure(this)) {
            showDialog("请您开启手机的密码锁功能！")
            return

        }
        if (!fingerprintHelper!!.hasEnrolledFingerprints()) {
            showDialog("请您现在设置中添加一个指纹，用于指纹识别")
            return
        }
        btn_start.isEnabled = true
        btn_stop.isEnabled = true
    }

    fun onStart(view: View) {
        tv_notice.text = "开始识别！"
        fingerprintHelper!!.startListening()
    }

    fun onStop(view: View) {
        tv_notice.text = "结束识别！"
        fingerprintHelper!!.stopListening()
    }

    fun showDialog(msg: String) {
        AlertDialog.Builder(this)
                .setTitle("温馨提示")
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok,null)
                .setNegativeButton(android.R.string.cancel,null)
                .setCancelable(false).create().show()
    }
}