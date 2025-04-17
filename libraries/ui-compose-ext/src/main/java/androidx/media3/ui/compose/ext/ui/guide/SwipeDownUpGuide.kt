package androidx.media3.ui.compose.ext.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.atlasv.android.mediax.composeui.R

/**
 * Created by weiping on 2024/9/27
 */

/**
 * 提示可以上线滑动查看更多
 */
@Composable
fun SwipeDownUpGuide(
    guideText: String = stringResource(R.string.swipe_tip),
    onClick: () -> Unit = {}
) {
    val starsComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.swipe_down_up))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize()
            .clickable { onClick() }
            .background(color = Color.Black.copy(alpha = 0.7f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LottieAnimation(
            starsComposition,
            modifier = Modifier
                .size(170.dp),
            contentScale = ContentScale.FillWidth,
            iterations = LottieConstants.IterateForever
        )
        Text(
            text = guideText,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(80.dp)) // 整体上下居中，这里加一个底部Space以便内容区上移40dp
    }
}