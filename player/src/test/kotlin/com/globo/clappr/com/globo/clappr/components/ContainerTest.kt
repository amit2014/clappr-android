package com.globo.clappr.base

import android.os.Bundle
import com.globo.clappr.BuildConfig
import com.globo.clappr.components.Container
import com.globo.clappr.components.Playback
import com.globo.clappr.components.PlaybackSupportInterface
import com.globo.clappr.playback.NoOpPlayback
import com.globo.clappr.plugin.container.ContainerPlugin
import com.globo.clappr.plugin.Loader
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, sdk = intArrayOf(23))
open class ContainerTest {

    class MP4Playback(source: String, mimeType: String?, options: Options) : Playback(source, mimeType, options) {
        companion object: PlaybackSupportInterface {
            override fun supportsSource(source: String, mimeType: String?): Boolean {
                return source.endsWith(".mp4")
            }

            override val name: String?
                get() = "mp4"
        }
    }

    @Before
    fun setup() {
        BaseObject.context = ShadowApplication.getInstance().applicationContext
        Loader.clearPlugins()
        Loader.clearPlaybacks()
    }

    @Test
    fun shouldLoadPlugins() {
        Loader.registerPlugin(ContainerPlugin::class)
        val container = Container(Loader(), Options())

        assertTrue("no plugins", container.plugins.isNotEmpty())
    }

    @Test
    fun shouldLoadPlaybackForSupportedSource() {
        Loader.registerPlayback(MP4Playback::class)
        val container = Container(Loader(), Options("some_source.mp4"))

        assertNotNull("should have created playback", container.playback)
        assertEquals("should have created mp4 playback", container.playback?.name, MP4Playback.name)
    }

    @Test
    fun shouldLoadNoOpPlaybackForUnsupportedSource() {
        Loader.registerPlayback(NoOpPlayback::class)
        Loader.registerPlayback(MP4Playback::class)
        val container = Container(Loader(), Options("some_unknown_source.mp0"))

        assertNotNull("should have created playback", container.playback)
        assertEquals("should have created no-op playback", container.playback?.name, NoOpPlayback.name)
    }

    @Test
    fun shouldNotTriggerPlaybackChangedWhenSameNullPlayback() {
        Loader.registerPlayback(MP4Playback::class)
        val container = Container(Loader(), Options("some_unknown_source.mp0"))

        var callbackWasCalled = false
        container.on(InternalEvent.WILL_CHANGE_PLAYBACK.value, Callback.wrap { bundle: Bundle? -> callbackWasCalled = true})
        container.on(InternalEvent.DID_CHANGE_PLAYBACK.value, Callback.wrap { bundle: Bundle? -> callbackWasCalled = true })

        container.load(source = "some_unknown_source.mp0")
        assertFalse("CHANGE_PLAYBACK triggered " + container.playback, callbackWasCalled)
    }

    @Test
    fun shouldTriggerPlaybackChangedWhenDifferentPlayback() {
        Loader.registerPlayback(MP4Playback::class)
        val container = Container(Loader(), Options("some_unknown_source.mp0"))

        val previousPlayback: Playback? = container.playback
        var callbackWasCalled = false
        container.on(InternalEvent.WILL_CHANGE_PLAYBACK.value, Callback.wrap { bundle: Bundle? ->
            assertFalse("DID_CHANGE_PLAYBACK triggered before WILL_CHANGE_PLAYBACK", callbackWasCalled)
            assertEquals("playback already changed", previousPlayback, container.playback)
        })
        container.on(InternalEvent.DID_CHANGE_PLAYBACK.value, Callback.wrap { bundle: Bundle? -> callbackWasCalled = true })

        container.load(source = "some_source.mp4")
        assertTrue("DID_CHANGE_PLAYBACK not triggered", callbackWasCalled)
    }

    @Test
    fun shouldNotTriggerPlaybackChangedWhenSamePlayback() {
        Loader.registerPlayback(MP4Playback::class)
        val container = Container(Loader(), Options("some_unknown_source.mp4"))

        var callbackWasCalled = false
        container.on(InternalEvent.WILL_CHANGE_PLAYBACK.value, Callback.wrap { bundle: Bundle? -> callbackWasCalled = true})
        container.on(InternalEvent.DID_CHANGE_PLAYBACK.value, Callback.wrap { bundle: Bundle? -> callbackWasCalled = true })

        container.load(source = "some_unknown_source.mp4")
        assertFalse("CHANGE_PLAYBACK triggered", callbackWasCalled)
    }
}