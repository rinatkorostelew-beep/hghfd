package com.bighead.app

import android.animation.*
import android.os.Bundle
import android.view.*
import android.view.animation.*
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.bighead.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Drag state
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f

    // UI state
    private var panelShown = false
    private var currentMode = Mode.BIGHEAD

    enum class Mode { BIGHEAD, DIGTER }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Keep screen edge-to-edge
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        positionPanelOnLayout()
        setupLaunchButton()
        setupDrag()
        setupModeToggle()
        setupSliders()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Initial positioning
    // ─────────────────────────────────────────────────────────────────────────

    private fun positionPanelOnLayout() {
        binding.rootContainer.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    binding.rootContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    val sw = binding.rootContainer.width.toFloat()
                    val sh = binding.rootContainer.height.toFloat()
                    val pw = binding.draggableGroup.width.toFloat()
                    val ph = binding.draggableGroup.height.toFloat()
                    binding.draggableGroup.x = (sw - pw) / 2f
                    binding.draggableGroup.y = (sh - ph) / 2f - 80f
                }
            })
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Launch button
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupLaunchButton() {
        binding.btnLaunch.setOnClickListener {
            if (!panelShown) showPanel() else hidePanel()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Panel show / hide
    // ─────────────────────────────────────────────────────────────────────────

    private fun showPanel() {
        panelShown = true
        binding.btnLaunch.text = "ЗАКРЫТЬ"
        binding.btnLaunch.setIconResource(R.drawable.ic_close)

        // Animate launch button
        binding.btnLaunch.animate()
            .scaleX(0.9f).scaleY(0.9f)
            .setDuration(100)
            .withEndAction {
                binding.btnLaunch.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }.start()

        with(binding.draggableGroup) {
            visibility = View.VISIBLE
            scaleX = 0.75f
            scaleY = 0.75f
            alpha = 0f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(380)
                .setInterpolator(OvershootInterpolator(1.3f))
                .start()
        }

        // Show the appropriate sub-UI
        when (currentMode) {
            Mode.BIGHEAD -> revealBigheadSlider()
            Mode.DIGTER  -> revealCircleAndSlider()
        }
    }

    private fun hidePanel() {
        panelShown = false
        binding.btnLaunch.text = "ЗАПУСТИТЬ"
        binding.btnLaunch.setIconResource(R.drawable.ic_launch)

        binding.draggableGroup.animate()
            .alpha(0f)
            .scaleX(0.75f)
            .scaleY(0.75f)
            .setDuration(260)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction { binding.draggableGroup.visibility = View.INVISIBLE }
            .start()

        hideCircle()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dragging
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupDrag() {
        binding.panelCard.setOnTouchListener { _, event ->
            val root = binding.rootContainer
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dragOffsetX = event.rawX - binding.draggableGroup.x
                    dragOffsetY = event.rawY - binding.draggableGroup.y
                    // Lift effect
                    binding.draggableGroup.animate()
                        .scaleX(1.04f).scaleY(1.04f)
                        .setDuration(120)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val newX = (event.rawX - dragOffsetX)
                        .coerceIn(0f, (root.width - binding.draggableGroup.width).toFloat())
                    val newY = (event.rawY - dragOffsetY)
                        .coerceIn(0f, (root.height - binding.draggableGroup.height).toFloat())
                    binding.draggableGroup.x = newX
                    binding.draggableGroup.y = newY
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Drop effect
                    binding.draggableGroup.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(200)
                        .setInterpolator(OvershootInterpolator(2f))
                        .start()
                    true
                }
                else -> false
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mode toggle  BIGHEAD ↔ DIGTER
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupModeToggle() {
        binding.btnBigHead.setOnClickListener { if (currentMode != Mode.BIGHEAD) switchMode(Mode.BIGHEAD) }
        binding.btnDigter.setOnClickListener  { if (currentMode != Mode.DIGTER)  switchMode(Mode.DIGTER)  }
        applyToggleVisuals(Mode.BIGHEAD, animated = false)
    }

    private fun switchMode(mode: Mode) {
        currentMode = mode
        applyToggleVisuals(mode, animated = true)

        if (!panelShown) return   // nothing extra to show

        when (mode) {
            Mode.BIGHEAD -> {
                hideCircle()
                revealBigheadSlider()
                hideSingleSlider(binding.digterSliderContainer)
            }
            Mode.DIGTER  -> {
                revealCircleAndSlider()
                hideSingleSlider(binding.bigheadSliderContainer)
            }
        }
    }

    private fun applyToggleVisuals(mode: Mode, animated: Boolean) {
        val selectedBtn   = if (mode == Mode.BIGHEAD) binding.btnBigHead else binding.btnDigter
        val unselectedBtn = if (mode == Mode.BIGHEAD) binding.btnDigter  else binding.btnBigHead

        selectedBtn.setBackgroundResource(R.drawable.toggle_selected)
        selectedBtn.setTextColor(getColor(R.color.white))

        unselectedBtn.setBackgroundResource(R.drawable.toggle_unselected)
        unselectedBtn.setTextColor(getColor(R.color.text_muted))

        if (animated) {
            selectedBtn.animate().scaleX(1.05f).scaleY(1.05f).setDuration(80).withEndAction {
                selectedBtn.animate().scaleX(1f).scaleY(1f).setDuration(120)
                    .setInterpolator(OvershootInterpolator(2f)).start()
            }.start()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Slider helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun revealBigheadSlider() = revealSingleSlider(binding.bigheadSliderContainer)
    private fun revealCircleAndSlider() {
        revealCircle()
        revealSingleSlider(binding.digterSliderContainer)
    }

    private fun revealSingleSlider(container: View) {
        if (container.isVisible && container.alpha == 1f) return
        container.visibility = View.VISIBLE
        container.alpha = 0f
        container.translationY = -20f
        container.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(320)
            .setInterpolator(DecelerateInterpolator(1.4f))
            .start()
    }

    private fun hideSingleSlider(container: View) {
        if (!container.isVisible) return
        container.animate()
            .alpha(0f)
            .translationY(-16f)
            .setDuration(220)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction { container.visibility = View.GONE }
            .start()
    }

    private fun setupSliders() {
        binding.digterSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                binding.circleOverlay.setRadius(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
        // Set initial circle size to match slider default (progress=40)
        binding.circleOverlay.setRadius(40)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Circle  (DIGTER mode)
    // ─────────────────────────────────────────────────────────────────────────

    private fun revealCircle() {
        with(binding.circleOverlay) {
            visibility = View.VISIBLE
            scaleX = 0.3f
            scaleY = 0.3f
            alpha = 0f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(OvershootInterpolator(1.1f))
                .start()
        }
    }

    private fun hideCircle() {
        if (!binding.circleOverlay.isVisible) return
        binding.circleOverlay.animate()
            .alpha(0f)
            .scaleX(0.3f)
            .scaleY(0.3f)
            .setDuration(260)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction { binding.circleOverlay.visibility = View.INVISIBLE }
            .start()
    }
}
