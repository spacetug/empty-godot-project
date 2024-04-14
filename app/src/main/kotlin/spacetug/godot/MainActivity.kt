package spacetug.godot

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.godotengine.godot.Godot
import org.godotengine.godot.GodotHost
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Godot(this, Modifier.fillMaxSize())
        }
    }
}

@Composable
fun Godot(activity: MainActivity, modifier: Modifier = Modifier) {
    var container: FrameLayout? by remember { mutableStateOf(null) }

    val action = remember { MutableSharedFlow<Unit>() }

    Column(modifier = modifier) {
        AndroidView(
            factory = { FrameLayout(it) },
            update = { container = it },
            modifier = Modifier.weight(1f),
        )

        val scope = rememberCoroutineScope()

        Button(onClick = { scope.launch { action.emit(Unit) } }) {
            Text(text = "Send Signal")
        }
    }

    container?.let { view ->
        LaunchedEffect(activity, view) {
            val godot = Godot(view.context)
            val signal = SignalInfo("message", ByteArray::class.javaObjectType)
            val plugin = object : GodotPlugin(godot) {
                val bytes = ByteArray(3072 * 3072 * 4)

                override fun getPluginName() = "TestPlugin"
                override fun getPluginSignals() = setOf(signal)

                fun send() {
                    emitSignal(signal.name, bytes)
                }
            }
            val host = object : GodotHost {
                override fun getActivity() = activity
                override fun getGodot() = godot
                override fun getHostPlugins(engine: Godot?) = setOf(plugin)
            }
            godot.onCreate(host)
            godot.onInitNativeLayer(host)
            godot.onInitRenderView(host, view)

            launch(Dispatchers.Default) {
                action.collect {
                    plugin.send()
                }
            }

            try {
                awaitCancellation()
            } finally {
                godot.onDestroy(host)
            }
        }
    }
}
