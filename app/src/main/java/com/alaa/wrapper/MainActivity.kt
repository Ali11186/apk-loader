package com.alaa.wrapper

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.alaa.wrapper.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnGo.setOnClickListener {
            val pw = b.password.text?.toString() ?: ""
            if (pw.isEmpty()) {
                toast("ادخل كلمة السر")
                return@setOnClickListener
            }
            run(pw)
        }
    }

    private fun run(password: String) {
        b.btnGo.isEnabled = false
        b.btnGo.text = "جاري التحضير…"
        b.progress.visibility = android.view.View.VISIBLE

        scope.launch {
            try {
                val apk = withContext(Dispatchers.IO) {
                    WrapperCrypto.decryptFromAssets(
                        context = applicationContext,
                        password = password,
                        outputFile = File(filesDir, "payload.apk")
                    )
                }
                withContext(Dispatchers.Main) {
                    b.status.text = "تم فك التشفير — جاري التثبيت…"
                    install(apk)
                }
            } catch (t: Throwable) {
                b.btnGo.isEnabled = true
                b.btnGo.text = "افتح"
                b.progress.visibility = android.view.View.GONE
                b.status.text = "فشل: كلمة السر غلط أو الملف تالف"
                toast("فشل فك التشفير")
            }
        }
    }

    private fun install(apk: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !packageManager.canRequestPackageInstalls()) {
            startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:$packageName")))
            toast("اسمح بتثبيت التطبيقات من المصدر ده وأعد المحاولة")
            b.btnGo.isEnabled = true
            b.btnGo.text = "افتح"
            b.progress.visibility = android.view.View.GONE
            return
        }

        val uri = FileProvider.getUriForFile(
            this, "$packageName.fileprovider", apk
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        b.status.text = "اتفتح مُثبّت أندرويد — كمّل التثبيت"
        b.btnGo.isEnabled = true
        b.btnGo.text = "افتح"
        b.progress.visibility = android.view.View.GONE
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
