package com.atlasv.android.media3.demo.download

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.atlasv.android.media3.demo.download.ui.theme.Androidxmedia3Theme

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

private const val INS_IMAGE1 =
    "https://scontent-lhr8-1.cdninstagram.com/v/t51.29350-15/464648271_1061991415569752_8942922764679332935_n.webp?se=7&stp=dst-jpg_e35&efg=eyJ2ZW5jb2RlX3RhZyI6ImltYWdlX3VybGdlbi4xMDgweDE5MjAuc2RyLmYyOTM1MC5kZWZhdWx0X2ltYWdlIn0&_nc_ht=scontent-lhr8-1.cdninstagram.com&_nc_cat=108&_nc_ohc=tC9qkneVqXAQ7kNvgEgm3Vt&_nc_gid=2483d986144546b19e9751b7241ada2f&edm=ANmP7GQBAAAA&ccb=7-5&ig_cache_key=MzQ4ODU3MTI2MTczNDI0MTg1Ng%3D%3D.3-ccb7-5&oh=00_AYDxfLs5UeGAUQiI0oUfTJrJKC7obOVNNXubP57Q9Qoxfg&oe=672542C8&_nc_sid=982cc7"

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Androidxmedia3Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(16.dp)
                    ) {
                        Greeting(text = "Aliyun", modifier = Modifier, onClick = {
                            viewModel.testDownload(downloadUrl = TEST_URL_VIDEO1)
//                            viewModel.testDownload(downloadUrl = TEST_URL_IMAGE_1)
//                            viewModel.testDownload(downloadUrl = TEST_URL_AUDIO_1)
                        })

                        Greeting(text = "Digital Ocean", modifier = Modifier, onClick = {
                            viewModel.testDownload(downloadUrl = TEST_URL_VIDEO2)
//                            viewModel.testDownload(downloadUrl = TEST_URL_IMAGE_2)
                        })

                        Greeting(text = "Google Storage", modifier = Modifier, onClick = {
                            viewModel.testDownload(downloadUrl = TEST_URL_IMAGE_3)
                        })

                        Greeting(text = "Instagram", modifier = Modifier, onClick = {
                            viewModel.testDownload(downloadUrl = INS_IMAGE1)
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Text(text = text)
    }
}