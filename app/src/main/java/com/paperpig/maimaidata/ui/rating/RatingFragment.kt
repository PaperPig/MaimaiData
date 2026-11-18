package com.paperpig.maimaidata.ui.rating

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.button.MaterialButton
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.crawler.CrawlerCaller
import com.paperpig.maimaidata.crawler.LxnsHtmlUploader
import com.paperpig.maimaidata.crawler.WechatCrawlerListener
import com.paperpig.maimaidata.databinding.FragmentRatingBinding
import com.paperpig.maimaidata.glide.GlideApp
import com.paperpig.maimaidata.model.PlayerProfile
import com.paperpig.maimaidata.model.PlayerProfileFormatter
import com.paperpig.maimaidata.network.lxns.LxnsClient
import com.paperpig.maimaidata.network.server.HttpServerService
import com.paperpig.maimaidata.network.vpn.core.LocalVpnService
import com.paperpig.maimaidata.repository.PlayerProfileRepository
import com.paperpig.maimaidata.ui.BaseFragment
import com.paperpig.maimaidata.ui.MainActivity
import com.paperpig.maimaidata.ui.about.SettingsActivity
import com.paperpig.maimaidata.ui.checklist.LevelCheckActivity
import com.paperpig.maimaidata.ui.checklist.VersionCheckActivity
import com.paperpig.maimaidata.ui.finaletodx.FinaleToDxActivity
import com.paperpig.maimaidata.ui.maimaidxprober.LoginActivity
import com.paperpig.maimaidata.ui.maimaidxprober.LxnsBindDialog
import com.paperpig.maimaidata.ui.maimaidxprober.ProberActivity
import com.paperpig.maimaidata.utils.SpUtil
import com.paperpig.maimaidata.utils.toDp
import com.paperpig.maimaidata.widgets.ProberUploadTarget
import com.paperpig.maimaidata.widgets.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.random.Random

class RatingFragment : BaseFragment<FragmentRatingBinding>(), WechatCrawlerListener,
    LocalVpnService.onStatusChangedListener {

    private lateinit var binding: FragmentRatingBinding

    private val proberUpdateDialog by lazy { ProberUpdateDialog(requireContext()) }
    private val lxnsBindDialog by lazy {
        LxnsBindDialog(requireContext()) {
            refreshAccountStates()
        }
    }
    private val profileRepository by lazy { PlayerProfileRepository.getInstance() }
    private lateinit var proberUpdateTipsDialog: MaterialDialog


    private val httpServiceIntent by lazy {
        Intent(requireContext(), HttpServerService::class.java)
    }

    private val vpnActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startProxyServices()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            RatingFragment()

        const val TAG = "RatingFragment"

    }

    override fun getViewBinding(container: ViewGroup?): FragmentRatingBinding {
        binding = FragmentRatingBinding.inflate(layoutInflater, container, false)
        return binding
    }

    override fun onResume() {
        super.onResume()
        refreshAccountStates()
    }

    private var isUpdatingDifficultyButtons = false
    private var isDifficultyExpanded = false

    private val difficultyButtons: List<Pair<MaterialButton, Int>>
        get() = listOf(
            binding.btnDiffBasic to 0,
            binding.btnDiffAdvanced to 1,
            binding.btnDiffExpert to 2,
            binding.btnDiffMaster to 3,
            binding.btnDiffRemaster to 4,
            binding.btnDiffUtage to LxnsHtmlUploader.UTAGE_DIFFICULTY
        )

    private fun refreshAccountStates() {
        if (SpUtil.getUserName().isEmpty()) {
            binding.accountText.setText(R.string.no_logged_in)
            applyAccountTextStyle(binding.accountText, bound = false)
            binding.proberQueryBtn.visibility = View.GONE
            binding.proberLoginBtn.setText(R.string.login)
        } else {
            binding.accountText.text = SpUtil.getUserName()
            applyAccountTextStyle(binding.accountText, bound = true)
            binding.proberQueryBtn.visibility = View.VISIBLE
            binding.proberLoginBtn.setText(R.string.change_account)
        }

        if (SpUtil.hasLxnsBound()) {
            binding.lxnsAccountText.text = getString(
                R.string.lxns_bound_summary,
                SpUtil.getLxnsPlayerName().ifBlank { SpUtil.getLxnsFriendCode().toString() },
                SpUtil.getLxnsRating()
            )
            applyAccountTextStyle(binding.lxnsAccountText, bound = true)
            binding.lxnsQueryBtn.visibility = View.VISIBLE
            binding.lxnsBindBtn.setText(R.string.lxns_rebind)
        } else {
            binding.lxnsAccountText.setText(R.string.lxns_not_bound)
            applyAccountTextStyle(binding.lxnsAccountText, bound = false)
            binding.lxnsQueryBtn.visibility = View.GONE
            binding.lxnsBindBtn.setText(R.string.lxns_bind)
        }

        val targetButtonId = when (Settings.getProberUploadTarget()) {
            ProberUploadTarget.DIVING_FISH -> R.id.target_diving_fish_btn
            ProberUploadTarget.LXNS -> R.id.target_lxns_btn
            ProberUploadTarget.BOTH -> R.id.target_both_btn
        }
        if (binding.proberUploadTargetGroup.checkedButtonId != targetButtonId) {
            binding.proberUploadTargetGroup.check(targetButtonId)
        }

        val selectedDiffs = Settings.getUpdateDifficulty()
        isUpdatingDifficultyButtons = true
        difficultyButtons.forEach { (button, diff) ->
            button.isChecked = selectedDiffs.contains(diff)
        }
        isUpdatingDifficultyButtons = false
        updateDifficultySummary(selectedDiffs)
    }

    private fun updateDifficultySummary(selectedDiffs: Set<Int>) {
        val selectedButtons = difficultyButtons.filter { selectedDiffs.contains(it.second) }
        binding.proberDiffSummary.text = when {
            selectedButtons.isEmpty() -> getString(R.string.prober_upload_difficulty_none)
            selectedButtons.size == difficultyButtons.size ->
                getString(R.string.prober_upload_difficulty_all, difficultyButtons.size)
            else -> selectedButtons.joinToString(" · ") { it.first.text }
        }
    }

    private fun applyDifficultyExpandedState(expanded: Boolean, animate: Boolean) {
        binding.proberDifficultyGrid.visibility = if (expanded) View.VISIBLE else View.GONE
        val targetRotation = if (expanded) 180f else 0f
        if (animate) {
            binding.proberDiffArrow.animate()
                .rotation(targetRotation)
                .setDuration(150L)
                .start()
        } else {
            binding.proberDiffArrow.rotation = targetRotation
        }
    }

    private fun applyAccountTextStyle(textView: TextView, bound: Boolean) {
        val colorRes = if (bound) R.color.textColor else R.color.gray
        textView.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
        textView.setTypeface(null, if (bound) Typeface.BOLD else Typeface.NORMAL)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.let {
            updateBottomPadding(it.getBottomContentPaddingPx())
        }
        binding.root.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                if (isHidden) return@OnScrollChangeListener
                val dy = scrollY - oldScrollY
                (activity as? MainActivity)?.onContentScrolled(
                    dy = dy,
                    canScrollUp = scrollY > 0
                )
            }
        )

        binding.proberLoginBtn.setOnClickListener {
            startActivity(Intent(context, LoginActivity::class.java))
        }

        binding.proberQueryBtn.setOnClickListener {
            startActivity(Intent(context, ProberActivity::class.java))
        }

        binding.proberLevelCheckBtn.setOnClickListener {
            startActivity(Intent(context, LevelCheckActivity::class.java))
        }

        binding.proberVersionCheckBtn.setOnClickListener {
            startActivity(Intent(context, VersionCheckActivity::class.java))
        }

        binding.proberFinaleToDxBtn.setOnClickListener {
            startActivity(Intent(context, FinaleToDxActivity::class.java))
        }

        binding.proberRatingCalcBtn.setOnClickListener {
            startActivity(Intent(context, RatingCalculatorActivity::class.java))
        }

        binding.lxnsBindBtn.setOnClickListener {
            lxnsBindDialog.refreshState()
            lxnsBindDialog.show()
        }

        binding.lxnsQueryBtn.setOnClickListener {
            ProberActivity.actionStart(requireContext(), ProberActivity.SOURCE_LXNS)
        }

        profileRepository.getProfileLiveData().observe(viewLifecycleOwner) { profile ->
            renderPlayerProfileCard(profile)
        }

        binding.proberUploadTargetGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val target = when (checkedId) {
                R.id.target_lxns_btn -> ProberUploadTarget.LXNS
                R.id.target_both_btn -> ProberUploadTarget.BOTH
                else -> ProberUploadTarget.DIVING_FISH
            }
            Settings.setProberUploadTarget(target)
        }

        applyDifficultyExpandedState(isDifficultyExpanded, animate = false)
        binding.proberDiffToggleRow.setOnClickListener {
            isDifficultyExpanded = !isDifficultyExpanded
            applyDifficultyExpandedState(isDifficultyExpanded, animate = true)
        }

        difficultyButtons.forEach { (button, _) ->
            button.addOnCheckedChangeListener { _, _ ->
                if (isUpdatingDifficultyButtons) return@addOnCheckedChangeListener
                val selected = difficultyButtons
                    .filter { it.first.isChecked }
                    .map { it.second }
                    .toSet()
                Settings.setUpdateDifficulty(selected)
                updateDifficultySummary(selected)
            }
        }

        CrawlerCaller.setOnWechatCrawlerListener(this)
        LocalVpnService.addOnStatusChangedListener(this)

        binding.proberProxySimpleText.setOnClickListener {
            proberUpdateDialog.show()
        }

        binding.proberProxyUpdateBtn.setOnClickListener {
            if (!LocalVpnService.IsRunning) {
                val intent: Intent? = LocalVpnService.prepare(context)
                if (intent == null) {
                    startProxyServices()
                } else {
                    vpnActivityResultLauncher.launch(intent)
                }
            } else {
                LocalVpnService.IsRunning = false
                stopHttpService()
            }
        }

        binding.proberProxyUpdateHelpIv.setOnClickListener {
            showHelpDialog()
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                menu.findItem(R.id.settings).isVisible = !isHidden
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.about_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.settings -> {
                        startActivity(Intent(requireContext(), SettingsActivity::class.java))
                        return true
                    }
                }
                return false
            }

        })
    }

    private fun startProxyServices() {
        val target = Settings.getProberUploadTarget()
        val missingDivingFish = target.includesDivingFish() && SpUtil.getUserName().isEmpty()
        val missingLxns = target.includesLxns() && !SpUtil.hasLxnsBound()

        when {
            missingDivingFish && missingLxns -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.vpn_please_login_both),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            missingDivingFish -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.vpn_please_login),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            missingLxns -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.vpn_please_bind_lxns),
                    Toast.LENGTH_SHORT
                ).show()
                lxnsBindDialog.refreshState()
                lxnsBindDialog.show()
                return
            }
        }
        startVPNService()
        startHttpService()
        createLinkUrl()
        getWechatApi()
    }


    private fun createLinkUrl() {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val randomChar = (1..10)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")

        val link = "http://127.0.0.2:8284/$randomChar"

        val mClipData = ClipData.newPlainText("copyText", link)
        (requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
            mClipData
        )

        Toast.makeText(
            requireContext(),
            "已复制链接，请在微信中粘贴并打开",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun getWechatApi() {
        try {
            val intent = Intent(Intent.ACTION_MAIN)
            val cmp = ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI")
            intent.apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                component = cmp
            }
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
        }
    }

    private fun startVPNService() {
        requireContext().startService(Intent(requireContext(), LocalVpnService::class.java))
    }

    private fun startHttpService() {
        requireContext().startService(httpServiceIntent)
    }

    private fun stopHttpService() {
        requireContext().stopService(httpServiceIntent)
    }

    override fun onStatusChanged(status: String, isRunning: Boolean) {
        binding.proberProxyUpdateBtn.setText(if (isRunning) R.string.stop_proxy else R.string.start_proxy)
    }

    @SuppressLint("SetTextI18n")
    override fun onLogReceived(logString: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            .format(System.currentTimeMillis())
        proberUpdateDialog.appendText("[$timestamp] $logString\n")
    }

    @SuppressLint("SetTextI18n")
    override fun onMessageReceived(logString: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            .format(System.currentTimeMillis())
        proberUpdateDialog.appendText("[$timestamp] $logString\n")
        binding.proberProxySimpleText.text = "[$timestamp] $logString"
    }

    override fun onStartAuth() {
        binding.proberProxySimpleText.text = ""
        binding.proberProxyIndicator.isIndeterminate = true
        binding.proberProxyStatusGroup.visibility = View.VISIBLE
    }

    override fun onFinishUpdate() {
        binding.proberProxyIndicator.visibility = View.INVISIBLE
        stopHttpService()
        refreshLxnsPlayerProfileIfNeeded()
    }

    private fun renderPlayerProfileCard(profile: PlayerProfile?) {
        binding.playerProfileCard.visibility = if (profile != null) View.VISIBLE else View.GONE
        if (profile == null) return

        val trophyName = profile.trophy?.name.orEmpty()
        if (trophyName.isNotBlank()) {
            binding.profileCardTrophyTv.visibility = View.VISIBLE
            binding.profileCardTrophyTv.text = trophyName
            applyTrophyStyle(binding.profileCardTrophyTv, profile.trophy?.color)
        } else {
            binding.profileCardTrophyTv.visibility = View.GONE
        }

        binding.profileCardNameTv.text = profile.name.ifBlank { "-" }

        binding.profileCardRatingTv.text = "DX ${profile.rating}"
        applyRatingPillStyle(binding.profileCardRatingTv, profile.rating)

        binding.profileCardCourseTv.text = PlayerProfileFormatter.formatCourseRank(profile.courseRank)
        applyCourseBadgeStyle(binding.profileCardCourseTv, profile.courseRank)

        binding.profileCardClassTv.text = PlayerProfileFormatter.formatClassRank(profile.classRank)
        applyClassBadgeStyle(binding.profileCardClassTv, profile.classRank)

        binding.profileCardStarTv.text = "×${profile.star}"

        binding.profileCardIconIv.clipToOutline = true
        val iconUrl = profile.iconUrl?.takeIf { it.isNotBlank() }
        if (iconUrl != null) {
            GlideApp.with(this)
                .load(iconUrl)
                .placeholder(R.drawable.bg_profile_avatar_placeholder)
                .into(binding.profileCardIconIv)
        } else {
            binding.profileCardIconIv.setImageDrawable(null)
        }
    }

    private fun applyRatingPillStyle(textView: TextView, rating: Int) {
        val colors = when (rating) {
            in 0..999 -> intArrayOf(Color.parseColor("#64748B"), Color.parseColor("#64748B"))
            in 1000..1999 -> intArrayOf(Color.parseColor("#3B82F6"), Color.parseColor("#3B82F6"))
            in 2000..3999 -> intArrayOf(Color.parseColor("#22C55E"), Color.parseColor("#22C55E"))
            in 4000..6999 -> intArrayOf(Color.parseColor("#F97316"), Color.parseColor("#F97316"))
            in 7000..9999 -> intArrayOf(Color.parseColor("#EF4444"), Color.parseColor("#EF4444"))
            in 10000..11999 -> intArrayOf(Color.parseColor("#9F51DC"), Color.parseColor("#9F51DC"))
            in 12000..12999 -> intArrayOf(Color.parseColor("#B87333"), Color.parseColor("#B87333"))
            in 13000..13999 -> intArrayOf(Color.parseColor("#78909C"), Color.parseColor("#78909C"))
            in 14000..14499 -> intArrayOf(Color.parseColor("#D99B00"), Color.parseColor("#D99B00"))
            in 14500..14999 -> intArrayOf(Color.parseColor("#C79E28"), Color.parseColor("#E6BA3C"))
            else -> intArrayOf(
                Color.parseColor("#FA3582"),
                Color.parseColor("#9F51DC"),
                Color.parseColor("#3B82F6")
            )
        }
        textView.background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors).apply {
            cornerRadius = 8.toDp()
        }
    }

    private fun applyTrophyStyle(textView: TextView, trophyColor: String?) {
        if (trophyColor.equals("rainbow", ignoreCase = true)) {
            applyGradientChipStyle(
                textView = textView,
                fillColorsHex = listOf("#FFF0F6", "#F5EEFF", "#EEF6FF"),
                strokeColorHex = "#E6C4EE",
                textColorHex = "#B82570"
            )
            return
        }
        val (fillHex, strokeHex, textHex) = when (trophyColor?.lowercase(Locale.ROOT)) {
            "gold" -> Triple("#FFF8E1", "#F0D478", "#9A6B00")
            "silver" -> Triple("#F3F5F8", "#D0D7E0", "#4A5568")
            "bronze" -> Triple("#FDF2EA", "#E4BF9F", "#8C4B1F")
            else -> Triple("#F7F8FA", "#E6E8EC", "#333333")
        }
        applyTintedChipStyle(textView, fillHex, strokeHex, textHex)
    }

    private fun applyCourseBadgeStyle(textView: TextView, courseRank: Int) {
        when (courseRank) {
            // 初学者：白底红框红字
            0 -> applyTintedChipStyle(textView, "#FFF6F6", "#FF4D4F", "#E53935")
            // 初段~五段：铜棕色
            in 1..5 -> applyTintedChipStyle(textView, "#FBECE1", "#D9824C", "#9A481B")
            // 六段~十段、真传：深赤铜底 + 深赤红字
            in 6..11 -> applyTintedChipStyle(textView, "#FCE4DC", "#B93B22", "#9E1C13")
            // 真初段~真五段：洋红粉紫渐变
            in 12..16 -> applyGradientChipStyle(
                textView = textView,
                fillColorsHex = listOf("#FBE8FF", "#F0E2FF"),
                strokeColorHex = "#C840E9",
                textColorHex = "#6A1B9A"
            )
            // 真六段~真十段：浅虹彩底 + 粉紫框
            in 17..21 -> applyGradientChipStyle(
                textView = textView,
                fillColorsHex = listOf("#FFEAF4", "#FFF8DC", "#E8FAFF"),
                strokeColorHex = "#D65DB1",
                textColorHex = "#4A25AA"
            )
            // 真皆传：樱粉虹金
            22 -> applyGradientChipStyle(
                textView = textView,
                fillColorsHex = listOf("#FFE8F2", "#FFF3CC", "#E3F9FF"),
                strokeColorHex = "#E28A2B",
                textColorHex = "#8E1B74"
            )
            // 里皆传：金黄底 + 紫金框 + 深紫红字
            23 -> applyGradientChipStyle(
                textView = textView,
                fillColorsHex = listOf("#FFF3B0", "#FFE082"),
                strokeColorHex = "#9C27B0",
                textColorHex = "#880E4F"
            )
            else -> applyTintedChipStyle(textView, "#F7F8FA", "#E6E8EC", "#4A5568")
        }
    }

    private fun applyClassBadgeStyle(textView: TextView, classRank: Int) {
        when (classRank) {
            // B5~B1：绿色
            in 0..4 -> applyTintedChipStyle(textView, "#E8F8EC", "#28A745", "#167930")
            // A5~A1：绯红色
            in 5..9 -> applyTintedChipStyle(textView, "#FFECEE", "#E03E4E", "#B71C2C")
            // S5~S1：橙铜金棕
            in 10..14 -> applyTintedChipStyle(textView, "#FFF2E0", "#D4801F", "#8A4B08")
            // SS5~SS1：银蓝藏青
            in 15..19 -> applyGradientChipStyle(
                textView = textView,
                fillColorsHex = listOf("#EDF2FA", "#E1E9F5"),
                strokeColorHex = "#2B4C8C",
                textColorHex = "#1D3566"
            )
            // SSS5~SSS1：金红皇冠
            in 20..24 -> applyGradientChipStyle(
                textView = textView,
                fillColorsHex = listOf("#FFF6D6", "#FFE9A3"),
                strokeColorHex = "#C67C00",
                textColorHex = "#8C2323"
            )
            // LEGEND：紫金虹彩
            25 -> applyGradientChipStyle(
                textView = textView,
                fillColorsHex = listOf("#F6E8FF", "#FFF2CC", "#E3F5FF"),
                strokeColorHex = "#8E24AA",
                textColorHex = "#6A1B9A"
            )
            else -> applyTintedChipStyle(textView, "#F7F8FA", "#E6E8EC", "#4A5568")
        }
    }

    private fun applyTintedChipStyle(
        textView: TextView,
        fillColorHex: String,
        strokeColorHex: String,
        textColorHex: String
    ) {
        textView.setTextColor(Color.parseColor(textColorHex))
        textView.background = GradientDrawable().apply {
            cornerRadius = 8.toDp()
            setColor(Color.parseColor(fillColorHex))
            setStroke(1.toDp().toInt().coerceAtLeast(1), Color.parseColor(strokeColorHex))
        }
    }

    private fun applyGradientChipStyle(
        textView: TextView,
        fillColorsHex: List<String>,
        strokeColorHex: String,
        textColorHex: String
    ) {
        textView.setTextColor(Color.parseColor(textColorHex))
        textView.background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            fillColorsHex.map { Color.parseColor(it) }.toIntArray()
        ).apply {
            cornerRadius = 8.toDp()
            setStroke(1.toDp().toInt().coerceAtLeast(1), Color.parseColor(strokeColorHex))
        }
    }

    private fun refreshLxnsPlayerProfileIfNeeded() {
        if (!isAdded || view == null) return
        val token = SpUtil.getLxnsUserToken()
        if (!Settings.getProberUploadTarget().includesLxns() || token.isBlank()) return
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    LxnsClient.instance.fetchUserPlayer(token)
                }
            }.onSuccess { player ->
                SpUtil.saveLxnsAccount(token, player.friendCode, player.name, player.rating)
                if (isAdded) {
                    refreshAccountStates()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onError(e: Exception) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            .format(System.currentTimeMillis())
        val message = e.message?.takeIf { it.isNotBlank() } ?: e.toString()
        proberUpdateDialog.appendText("[$timestamp] $message\n")
        binding.proberProxySimpleText.text = "[$timestamp] $message"
        binding.proberProxyStatusGroup.visibility = View.VISIBLE
        binding.proberProxyIndicator.visibility = View.INVISIBLE
        stopHttpService()
    }

    override fun onDestroy() {
        super.onDestroy()
        CrawlerCaller.removeOnWechatCrawlerListener()
        LocalVpnService.removeOnStatusChangedListener(this)
    }

    fun updateBottomPadding(contentPaddingBottom: Int) {
        if (!isAdded) return
        binding.root.updatePadding(bottom = contentPaddingBottom)
    }

    /**
     * 再次点击底栏当前 Tab：平滑滚动回顶部
     */
    fun onTabReselected() {
        if (!isAdded) return
        binding.root.smoothScrollTo(0, 0)
        (activity as? MainActivity)?.setBottomBarVisible(true)
    }

    private fun showHelpDialog() {
        if (::proberUpdateTipsDialog.isInitialized) {
            proberUpdateTipsDialog.show()
        } else {
            var helpStringBuilder = ""
            helpStringBuilder = getString(R.string.prober_update_content_step1) + "\n" +
                    getString(R.string.prober_update_content_step2) + "\n" +
                    getString(R.string.prober_update_content_step3) + "\n" +
                    getString(R.string.prober_update_content_step4) + "\n"

            proberUpdateTipsDialog = MaterialDialog.Builder(requireContext())
                .title(getString(R.string.prober_update_help))
                .content(helpStringBuilder)
                .build()
            proberUpdateTipsDialog.show()
        }
    }
}
