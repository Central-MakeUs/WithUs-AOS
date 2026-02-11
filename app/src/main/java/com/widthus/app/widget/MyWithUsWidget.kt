package com.widthus.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.widget.RemoteViews
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.withus.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyWithUsWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        // FCM에서 보낸 이미지가 있는 경우 처리
        val imageUrl = intent.getStringExtra("EXTRA_IMAGE_URL")
        val title = intent.getStringExtra("EXTRA_IMAGE_TITLE")
        if (!imageUrl.isNullOrBlank()) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MyWithUsWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, imageUrl, title)
            }
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, imageUrl: String?, title: String?) {        val views = RemoteViews(context.packageName, R.layout.my_with_us_widget_layout)

        if (!title.isNullOrBlank()) {
            views.setTextViewText(R.id.widget_title_text, title)
        }

        if (imageUrl != null) {
            // [중요] 비동기로 이미지를 가져와야 하므로 Coroutine이나 WorkManager 사용 권장
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val loader = ImageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .allowHardware(false) // 위젯 비트맵은 하드웨어 가속 끄는 게 안정적임
                        .build()

                    val result = (loader.execute(request) as? SuccessResult)?.drawable
                    val bitmap = (result as? BitmapDrawable)?.bitmap

                    withContext(Dispatchers.Main) {
                        if (bitmap != null) {
                            views.setImageViewBitmap(R.id.widget_image_view, bitmap)
                        }
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}