package scripts.kt.api.utility

import org.tribot.api2007.Interfaces
import org.tribot.script.sdk.*
import org.tribot.script.sdk.query.Query
import org.tribot.script.sdk.types.Widget
import org.tribot.script.sdk.types.World
import java.awt.Rectangle
import java.util.function.Supplier


/* Written by IvanEOD 10/24/2022, at 12:07 PM */
object WorldHopping {

    private var randomWorldNumberGenerator: () -> Int = {
        Query.worlds().isMembers
                .isNotDangerous
                .isRequirementsMet
                .isNotAnyType(
                        World.Type.PVP_ARENA,
                        World.Type.BOUNTY,
                        World.Type.TOURNAMENT,
                        World.Type.HIGH_RISK,
                        World.Type.LAST_MAN_STANDING,
                        World.Type.LEAGUE,
                        World.Type.PVP,
                        World.Type.DEADMAN,
                        World.Type.DEADMAN_TOURNAMENT,
                        World.Type.SKILL_TOTAL,
                        World.Type.QUEST_SPEEDRUNNING
                ).worldNumberNotEquals(
                        403, 404, 407, 408, 411, 412, 502, 503, 527, 427, 540, 541, 549, 550, 568, 569, 576, 581
                )
                .isNotCurrentWorld
                .findRandom()
                .orElse(null)?.worldNumber ?: -1
    }

    @JvmStatic
    fun setRandomWorldNumberGenerator(generator: Supplier<Int>) {
        randomWorldNumberGenerator = generator::get
    }


    private fun waitForWidget(timeout: Int = 1000, supplier: () -> Widget?): Widget? {
        var widget = supplier()
        if (widget != null) return widget
        Waiting.waitUntil(timeout) {
            widget = supplier()
            widget != null
        }
        return widget
    }

    private fun getWorldSwitcher(timeout: Int = 1000): Widget? =
            waitForWidget(timeout) { Query.widgets().inIndexPath(182, 3).findFirst().orElse(null) }

    private fun getWorldSwitcherCloseButton(timeout: Int = 1000): Widget? =
            waitForWidget(timeout) { Query.widgets().inIndexPath(69, 3).findFirst().orElse(null) }

    private fun getWorldWidget(number: Int, timeout: Int = 1000): Widget? =
            waitForWidget(timeout) { Query.widgets().inIndexPath(69, 16, number).findFirst().orElse(null) }

    private fun getWorldHopperVisibleFrame(timeout: Int): Widget? =
            waitForWidget(timeout) { Query.widgets().inIndexPath(69, 15).findFirst().orElse(null) }

    private fun getWorldHopperVisibleFrameBounds(timeout: Int): Rectangle? = getWorldHopperVisibleFrame(timeout)?.bounds
    private fun isWidgetHidden(widget: Widget): Boolean {
        val rsInterface = Interfaces.get(widget.indexPath)
        return rsInterface != null && rsInterface.isHidden
    }

    private fun needToScrollToWidget(widget: Widget, timeout: Int = 1000): Boolean {
        val worldWidgetBounds = widget.bounds
        val visibleBounds = getWorldHopperVisibleFrameBounds(timeout)
        if (visibleBounds == null) {
            Log.warn("Failed to find world hopper visible frame bounds, could not hop worlds.")
            return false
        }
        return !visibleBounds.contains(worldWidgetBounds)
    }


    private fun isWorldHopperOpen(timeout: Int = 1000): Boolean {
        val closeButton = getWorldSwitcherCloseButton(timeout)
        if (closeButton == null) {
            Log.warn("Failed to find world hopper close button, could not determine if world hopper is open.")
            return false
        }
        return closeButton.isVisible
    }

    private fun openWorldHopper(): Boolean {
        if (isWorldHopperOpen()) return true
        if (!GameTab.LOGOUT.isOpen) {
            val opened = GameTab.LOGOUT.open()
            Waiting.waitUntil(1000) { GameTab.LOGOUT.isOpen }
            if (!opened) {
                Log.warn("Failed to open logout tab,  could not open world hopper.")
                return false
            }
        }

        if (!isWorldHopperOpen()) {
            val openWorldSwitchButton = getWorldSwitcher(3500)
            if (openWorldSwitchButton == null) {
                Log.warn("Failed to find world switch button, could not open world hopper.")
                return false
            }

            val clicked = openWorldSwitchButton.click("World Switcher")
            if (!clicked) {
                Log.warn("Failed to click world switch button, could not open world hopper.")
                return false
            }
        }

        val success = Waiting.waitUntil(3000) { isWorldHopperOpen(900) }
        if (!success) {
            Log.warn("Failed to open world hopper.")
            return false
        }
        return true
    }

    @JvmStatic
    fun hopWorlds(): Boolean = hopWorlds(randomWorldNumberGenerator())

    @JvmStatic
    fun hopWorlds(number: Int): Boolean {
        if (number == WorldHopper.getCurrentWorld() || number == -1) {
            if (number == -1) {
                Log.warn("Failed to find a world to hop to.")
                return false
            }
            Log.warn("Tried hopping to World #$number, we're already there!")
            return hopWorlds(randomWorldNumberGenerator())
        }
        if (number < 300) throw IndexOutOfBoundsException("World number must be greater than 300.")
        if (number > 600) throw IndexOutOfBoundsException("World number must be less than 600.")
        Log.trace("Hopping to World #$number")
        if (!Login.isLoggedIn()) return WorldHopper.hop(number)

        if (!openWorldHopper()) {
            Log.warn("Failed to open world hopper, could not hop worlds.")
            return false
        }

        Waiting.waitUntil(3000) { isWorldHopperOpen() }

        val worldWidget = getWorldWidget(number, 3000)
        if (worldWidget == null) {
            Log.warn("Failed to find world widget, could not hop worlds.")
            return false
        }


        var isHidden = isWidgetHidden(worldWidget)
        if (isHidden) {

            Waiting.waitNormal(2000, 500)
            isHidden = isWidgetHidden(worldWidget)
            if (isHidden) {
                Log.warn("World widget is hidden, could not hop worlds.")
                return false
            }
        }

        if (needToScrollToWidget(worldWidget)) {
            Log.trace("Scrolling to widget")
            do {
                val scrolled = worldWidget.scrollTo()
                if (!scrolled) Log.warn("Failed scrolling to world widget...")
            } while (needToScrollToWidget(worldWidget))
        }

        if (needToScrollToWidget(worldWidget)) {
            Log.warn("Failed to scroll to world widget, could not hop worlds.")
            return false
        }

        val clicked = worldWidget.click("Switch")

        if (!clicked) {
            Log.warn("Failed to click world widget, could not hop worlds.")
            return false
        }

        val successfullyStarted = Waiting.waitUntil(4000) { GameState.getState() == GameState.State.HOPPING }
        if (!successfullyStarted) {
            Log.warn("Failed to start world hop, could not hop worlds.")
            return false
        }
        val successfullyHopped = Waiting.waitUntil {
            GameState.getState() == GameState.State.LOGGED_IN
        }
        if (!successfullyHopped) {
            Log.warn("Failed to hop worlds.")
            return false
        }
        return true
    }

}