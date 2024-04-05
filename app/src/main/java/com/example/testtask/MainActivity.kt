package com.example.testtask

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.log
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            Surface(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                Greeting()
            }
        }
    }
}

@Composable
fun Greeting() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .paint(
                painterResource(id = R.drawable.bg_main),
                contentScale = ContentScale.FillBounds
            )
    ) {
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f, true)
        ) {
            RadialMenu(
                mainColor = Color(0x80000000),
                fraction = .9f,
            ) {
                addEntry(R.drawable.ic_menu, description = "item1") {
                    addEntry(R.drawable.ic_menu, "Увеличить розыск", "Увеличить розыск")
                    addEntry(R.drawable.ic_menu, "Снять розыск", "Снять розыск")
                    addEntry(R.drawable.ic_give_doc, "Посадить в КПЗ", "Посадить в КПЗ")
                }
                addEntry(R.drawable.ic_three_up, description = "item2") {
                    addEntry(R.drawable.ic_menu, "Увеличить розыск", "Увеличить розыск")
                    addEntry(R.drawable.ic_menu, "Посадить в КПЗ", "Посадить в КПЗ")
                }
                addEntry(R.drawable.ic_one_up, description = "item3") {
                    addEntry(R.drawable.ic_menu, "Увеличить розыск", "Увеличить розыск")
                    addEntry(R.drawable.ic_menu, "Снять розыск", "Снять розыск")
                    addEntry(R.drawable.ic_menu, "Посадить в КПЗ", "Посадить в КПЗ")
                }
                addEntry(R.drawable.ic_give, description = "item4")
                addEntry(R.drawable.ic_ban, description = "item5")
                addEntry(R.drawable.ic_warning, description = "item6")
                addEntry(R.drawable.ic_money_doc, description = "item7")
                addEntry(R.drawable.ic_handcuffs, description = "item8")
            }
        }
    }
}

@Preview(
    device = "spec:width=811dp,height=1691dp,dpi=420,isRound=false,chinSize=0dp,orientation=landscape",
    showSystemUi = true
)
@Composable
fun GreetingPreview() {
    Greeting()
}