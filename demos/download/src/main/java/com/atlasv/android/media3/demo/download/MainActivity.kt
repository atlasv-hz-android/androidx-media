package com.atlasv.android.media3.demo.download

import android.os.Bundle
import android.text.format.Formatter
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atlasv.android.media3.demo.download.ui.theme.Androidxmedia3Theme
import com.atlasv.android.mediax.downloader.output.DownloadResult

private const val TEST_URL_VIDEO1 =
    "https://mwping-android.oss-cn-hangzhou.aliyuncs.com/video/birds-red-crowned-cranes-cranes-219862_tiny.mp4"
private const val TEST_URL_VIDEO2 =
    "https://richman-media.sfo3.cdn.digitaloceanspaces.com/public/overlay/resources/butterflies-171635.mp4"

private const val TEST_URL_IMAGE_1 =
    "https://mwping-android.oss-cn-hangzhou.aliyuncs.com/image/mountains-7543273.jpg"
private const val TEST_URL_IMAGE_2 =
    "https://richman-media.sfo3.cdn.digitaloceanspaces.com/public/overlay/previews/astronaut-171361.webp"
private const val TEST_URL_IMAGE_3 =
    "https://storage.googleapis.com/public-market-event-files/20240822/banner.webp"

private const val TEST_URL_AUDIO_1 =
    "https://mwping-android.oss-cn-hangzhou.aliyuncs.com/audio/guitar_xushi_aigei_com.mp3"

// Head 请求拿不到Content-Length，code=404
private const val PIXABAY_VIDEO_1 = "https://cdn.pixabay.com/video/2023/01/30/148597-794221559.mp4"

private const val X_VIDEO_1 =
    "https://video.twimg.com/amplify_video/1821285816372514816/vid/avc1/480x270/ghNQV5-RKNnYAfNs.mp4?tag=14"

private const val DO_VIDEO1 =
    "https://downloader-media.nyc3.cdn.digitaloceanspaces.com/public/video/test/148597-794221559_medium.mp4"
private const val DO_VIDEO2 =
    "https://downloader-media.nyc3.cdn.digitaloceanspaces.com/public/video/test/148597-794221559.mp4"

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val downloadItems by viewModel.downloadItems.collectAsStateWithLifecycle()
            Androidxmedia3Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LazyColumn(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Greeting(text = "Aliyun", modifier = Modifier, onClick = {
                                viewModel.testDownload(downloadUrl = TEST_URL_VIDEO1)
//                            viewModel.testDownload(downloadUrl = TEST_URL_IMAGE_1)
//                            viewModel.testDownload(downloadUrl = TEST_URL_AUDIO_1)
                            })

                            Greeting(text = "Digital Ocean", modifier = Modifier, onClick = {
                                viewModel.testDownload(downloadUrl = DO_VIDEO1)
                            })

                            Greeting(text = "Google Storage", modifier = Modifier, onClick = {
                                viewModel.testDownload(downloadUrl = TEST_URL_IMAGE_3)
                            })

                            Greeting(text = "Pixabay", modifier = Modifier, onClick = {
                                viewModel.testDownload(downloadUrl = PIXABAY_VIDEO_1)
                            })

                            Greeting(text = "X", modifier = Modifier, onClick = {
                                viewModel.testDownload(downloadUrl = X_VIDEO_1)
                            })

                            Greeting(text = "同链接下载多次", modifier = Modifier, onClick = {
                                viewModel.testDuplicateDownload(downloadUrl = TEST_URL_VIDEO2)
                            })
                        }
                        items(downloadItems) {
                            ProgressItemView(it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressItemView(progressItemWithDownloadResult: Pair<ProgressItem, DownloadResult?>) {
    val (progressItem, downloadResult) = progressItemWithDownloadResult
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = progressItem.downloadUrl, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            DownloadResultView(downloadResult)
            MainProgress(progressItem)
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            RangeProgress(progressItem)
        }
    }
}

@Composable
private fun DownloadResultView(downloadResult: DownloadResult?) {
    downloadResult ?: return
    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
    Text(
        "下载完成（${downloadResult.contentLength} bytes）\n${downloadResult.outputTarget}",
        fontSize = 12.sp
    )
    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
}

@Composable
private fun MainProgress(progressItem: ProgressItem) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "总进度: ${progressItem.bytesCached}/${progressItem.requestLength}(${(progressItem.progress * 100).toInt()}%)\n下载速度：${
            Formatter.formatFileSize(
                App.app,
                progressItem.speedPerSeconds
            ) + "/s"
        }",
        fontSize = 13.sp
    )
    Spacer(modifier = Modifier.height(4.dp))
    LinearProgressIndicator(
        progress = { progressItem.progress },
        modifier = Modifier.fillMaxWidth(),
        gapSize = 0.dp
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun RangeProgress(progressItem: ProgressItem) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = "分片进度", fontSize = 13.sp)
    Spacer(Modifier.height(4.dp))
    progressItem.specs.forEach { spec ->
        Text(
            text = "${spec.bytesCached}/${spec.requestLength}(${(spec.progress * 100).toInt()}%)",
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { spec.progress },
            modifier = Modifier.fillMaxWidth(),
            gapSize = 0.dp
        )
    }
}

@Composable
fun Greeting(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Text(text = text)
    }
}