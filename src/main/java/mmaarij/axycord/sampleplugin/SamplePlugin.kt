package mmaarij.axycord.sampleplugin

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.drawable.ColorDrawable
import android.os.IBinder
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.discord.models.deserialization.gson.InboundGatewayGsonParser
import com.discord.models.message.Message
import com.discord.stores.StoreStream
import mmaarij.axycord.sampleplugin.databinding.SampleProfileSectionBinding
import mmaarij.axycord.sampleplugin.databinding.SampleProfileTabBinding
import mmaarij.axycord.sampleplugin.databinding.SampleMessageContentBinding
import mmaarij.axycord.sampleplugin.databinding.SampleMessageHeaderBinding
import mmaarij.axycord.sampleplugin.databinding.SampleMessageStandaloneBinding
import mmaarij.axycord.sampleplugin.databinding.ViewRawPageBinding
import org.json.JSONObject
import mmaarij.axycord.plugins.api.AxycordPlugin
import mmaarij.axycord.plugins.api.PluginComposerZone
import mmaarij.axycord.plugins.api.PluginScope
import mmaarij.axycord.plugins.api.PluginSettingsPage
import mmaarij.axycord.plugins.api.pluginBottomTabContribution
import mmaarij.axycord.plugins.api.pluginComposerContribution
import mmaarij.axycord.plugins.api.pluginHeaderContribution
import mmaarij.axycord.plugins.api.pluginProfileContribution
import mmaarij.axycord.plugins.api.pluginProfileTabContribution
import mmaarij.axycord.plugins.api.pluginSettings
import mmaarij.axycord.plugins.api.pluginSlashCommand
import mmaarij.axycord.plugins.api.PluginContextMenuSurface
import mmaarij.axycord.plugins.api.PluginMessagePlacement
import mmaarij.axycord.plugins.api.PluginMessageInterceptionType
import mmaarij.axycord.plugins.api.pluginContextMenuItem
import mmaarij.axycord.plugins.api.pluginMessageContribution
import mmaarij.axycord.plugins.api.pluginMessageInterceptor
import rx.Completable

class SamplePlugin : AxycordPlugin {
    private lateinit var scope: PluginScope

    override val settingsSchema = pluginSettings {
        section("Axycord Example Plugin")
        toggle("enabled", "Enable example alerts", default = true)
        text("message", "Alert message", default = "Hello from an external Axycord plugin!")
        choice(
            "style",
            "Message card text size",
            default = "compact",
            options = listOf("compact" to "Compact", "large" to "Large"),
        )
        action("test", "Show alert now") { showAlert("Settings") }
    }

    override val settingsPage = PluginSettingsPage { context, _ ->
        TextView(context).apply {
            text = "Custom settings pages can use arbitrary Android Views or plugin XML."
            setPadding(dp(context, 16), dp(context, 16), dp(context, 16), dp(context, 16))
        }
    }

    override fun start(scope: PluginScope): Completable = Completable.fromAction {
        this.scope = scope
        val inflater = requireNotNull(scope.resources.inflater)

        scope.ui.registerHeader(
            pluginHeaderContribution(
                "sample_header",
                "Header Alert",
                { inflater.inflate(R.layout.sample_plugin_button, null, false) },
            ) {
                priority = 25
                onClick = { showAlert("Header") }
            },
        )

        scope.ui.registerComposer(
            pluginComposerContribution(
                "sample_composer",
                PluginComposerZone.RIGHT,
                { context -> iconButton(context) { showAlert("Composer") } },
            ) {
                priority = 25
                bind = { view, context -> view.isEnabled = context.canSendMessages }
            },
        )

        scope.ui.registerBottomTab(
            pluginBottomTabContribution(
                "sample_bottom_tab",
                "Example",
                { context -> iconButton(context) {} },
            ) {
                priority = 250
                bind = { view, context -> view.alpha = if (context.buttonsEnabled) 1f else 0.5f }
                onClick = { showAlert("Bottom Tab") }
            },
        )

        scope.ui.registerProfile(
            pluginProfileContribution(
                "sample_profile",
                "Plugin Section",
                { SampleProfileSectionBinding.inflate(inflater).root },
            ) {
                priority = 450
                bind = { view, context ->
                    (view as TextView).apply {
                        text = "Plugin section for user ${context.userId}"
                        setTextColor(context.primaryTextColor)
                        background = ColorDrawable(context.surfaceColor)
                    }
                }
            },
        )

        scope.ui.registerProfileTab(
            pluginProfileTabContribution(
                "sample_profile_tab",
                "Plugin Tab",
                { SampleProfileTabBinding.inflate(inflater).root },
            ) {
                priority = 50
                bind = { view, context ->
                    SampleProfileTabBinding.bind(view).apply {
                        root.setBackgroundColor(context.surfaceColor)
                        profileTabContent.setTextColor(context.primaryTextColor)
                    }
                }
            },
        )

        scope.ui.registerMessage(
            pluginMessageContribution(
                "sample_message_header",
                "Message Header Badge",
                PluginMessagePlacement.HEADER,
                { SampleMessageHeaderBinding.inflate(inflater).root },
            ) {
                priority = 30
                bind = { view, context ->
                    SampleMessageHeaderBinding.bind(view).root.text = "Plugin · ${context.messageId}"
                }
            },
        )

        scope.ui.registerMessage(
            pluginMessageContribution(
                "sample_message_content",
                "Message Content Row",
                PluginMessagePlacement.CONTENT,
                { SampleMessageContentBinding.inflate(inflater).root },
            ) {
                priority = 20
                bind = { view, context ->
                    SampleMessageContentBinding.bind(view).root.apply {
                        text = "Plugin content · ${context.content.length} characters"
                        textSize = if (scope.settings.getString("style", "compact") == "large") 18f else 14f
                    }
                }
            },
        )

        scope.ui.registerMessage(
            pluginMessageContribution(
                "sample_message_standalone",
                "Standalone Message Card",
                PluginMessagePlacement.STANDALONE,
                { SampleMessageStandaloneBinding.inflate(inflater).root },
            ) {
                priority = 10
                bind = { view, context ->
                    SampleMessageStandaloneBinding.bind(view).messageComponentDetail.text =
                        "Message ${context.messageId} in channel ${context.channelId}"
                }
            },
        )

        scope.ui.registerMessageInterceptor(
            pluginMessageInterceptor(
                "sample_message_prefix",
                PluginMessageInterceptionType.SEND,
                PluginMessageInterceptionType.EDIT,
                transform = { message ->
                    val content = message.content
                    if (content?.startsWith("plugin:") == true) {
                        message.copy(content = content.removePrefix("plugin:").trimStart())
                    } else {
                        message
                    }
                },
            ) {
                priority = 10
            },
        )

        scope.ui.registerSlashCommand(
            pluginSlashCommand("echo", "echo", "Echo text into the composer", { command ->
                command.arguments["text"]?.toString().orEmpty().ifBlank {
                    scope.settings.getString("message", "Hello from an external Axycord plugin!")
                }
            }) {
                priority = 25
                string("text", "Text to echo")
            },
        )

        scope.ui.registerContextMenuItem(
            pluginContextMenuItem(
                "inspect_context",
                "Inspect with Axycord Example Plugin",
                *PluginContextMenuSurface.entries.toTypedArray(),
                onClick = { context ->
                    scope.dialogs.alert(
                        "${context.surface} Context",
                        "guild=${context.guildId}, channel=${context.channelId}, message=${context.messageId}, " +
                            "user=${context.userId}, target=${context.targetId}",
                    )
                },
            ) { priority = 25 },
        )

        scope.ui.registerContextMenuItem(
            pluginContextMenuItem(
                "view_raw",
                "View Raw",
                PluginContextMenuSurface.MESSAGE,
                onClick = { context ->
                    val message = context.channelId?.let { channelId ->
                        StoreStream.getMessages().getMessages(channelId)
                            .firstOrNull { it.id == context.messageId }
                    }
                    if (message == null) {
                        scope.dialogs.alert("View Raw", "Message is no longer available in the local cache.")
                    } else {
                        scope.dialogs.page("Raw message by ${message.author?.username ?: "unknown user"}", "View Raw") {
                            rawMessagePage(message)
                        }
                    }
                },
            ) {
                priority = 50
                isVisible = { it.channelId != null && it.messageId != null }
                createIcon = { scope.resources.context?.getDrawable(R.drawable.ic_view_raw) }
            },
        )

        scope.storage.filesDir.resolve("started.txt").writeText("started")
    }

    private fun showAlert(source: String) {
        if (!scope.settings.getBoolean("enabled", true)) return
        scope.dialogs.alert(
            "$source - ${StoreStream.getUsers().getMe().username}",
            scope.settings.getString("message", "Hello from an external Axycord plugin!"),
        )
    }

    private fun iconButton(context: Context, click: () -> Unit) = ImageButton(context).apply {
        setImageDrawable(scope.resources.context?.getDrawable(R.drawable.ic_sample_plugin))
        background = null
        contentDescription = "Example plugin action"
        minimumWidth = dp(context, 48)
        minimumHeight = dp(context, 48)
        setOnClickListener { click() }
    }

    private fun rawMessagePage(message: Message): View {
        val binding = ViewRawPageBinding.inflate(requireNotNull(scope.resources.inflater))
        val content = message.content.orEmpty()
        val rawJson = InboundGatewayGsonParser.toJson(message)
        val prettyRaw = runCatching { JSONObject(rawJson).toString(2) }.getOrDefault(rawJson)
        listOf(
            Triple(binding.copyContent, "Content", content),
            Triple(binding.copyRawData, "Raw Data", prettyRaw),
        ).forEach { (button, label, value) ->
            button.isEnabled = value.isNotEmpty()
            button.alpha = if (button.isEnabled) 1f else 0.5f
            button.setOnClickListener {
                button.context.getSystemService(ClipboardManager::class.java)
                    ?.setPrimaryClip(ClipData.newPlainText(label, value))
                Toast.makeText(button.context, "$label copied", Toast.LENGTH_SHORT).show()
            }
        }
        binding.messageContent.apply {
            text = content
            visibility = if (content.isEmpty()) View.GONE else View.VISIBLE
        }
        binding.rawData.text = scope.text.renderCodeBlock(prettyRaw, "js")
        return binding.root
    }

    private fun dp(context: Context, value: Int) = (value * context.resources.displayMetrics.density).toInt()

}

class SamplePluginDiscoveryService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
