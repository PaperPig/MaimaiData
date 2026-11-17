package com.paperpig.maimaidata.ui.maimaidxprober

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.DialogLxnsBindBinding
import com.paperpig.maimaidata.network.lxns.LxnsApiException
import com.paperpig.maimaidata.network.lxns.LxnsClient
import com.paperpig.maimaidata.utils.SpUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 落雪查分器绑定弹窗：通过个人 API 密钥（X-User-Token）校验并绑定账号。
 */
class LxnsBindDialog(
    context: Context,
    private val onBindingUpdated: () -> Unit
) : BottomSheetDialog(context, R.style.Theme_Material3_Light_BottomSheetDialog) {

    private val binding = DialogLxnsBindBinding.inflate(layoutInflater)
    private var bindJob: Job? = null

    init {
        setContentView(binding.root)
        setCanceledOnTouchOutside(true)
        setCancelable(true)
        setupViews()
    }

    private fun setupViews() {
        refreshState()

        binding.lxnsUnbindBtn.setOnClickListener {
            SpUtil.clearLxnsAccount()
            Toast.makeText(context, R.string.lxns_unbind_success, Toast.LENGTH_SHORT).show()
            onBindingUpdated()
            dismiss()
        }

        binding.lxnsBindConfirmBtn.setOnClickListener {
            submitBind()
        }
    }

    fun refreshState() {
        hideError()
        val existingToken = SpUtil.getLxnsUserToken()
        binding.lxnsUserTokenEdit.setText(existingToken)
        binding.lxnsUnbindBtn.visibility = if (existingToken.isNotBlank()) View.VISIBLE else View.GONE
    }

    private fun submitBind() {
        hideError()
        val userToken = binding.lxnsUserTokenEdit.text?.toString()?.trim().orEmpty()
        if (userToken.isBlank()) {
            showError(context.getString(R.string.lxns_invalid_token_input))
            return
        }

        setLoading(true)
        bindJob?.cancel()
        bindJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val player = withContext(Dispatchers.IO) {
                    LxnsClient.instance.fetchUserPlayer(userToken)
                }
                SpUtil.saveLxnsAccount(userToken, player.friendCode, player.name, player.rating)
                Toast.makeText(
                    context,
                    context.getString(R.string.lxns_bind_success, player.name, player.rating),
                    Toast.LENGTH_SHORT
                ).show()
                onBindingUpdated()
                dismiss()
            } catch (e: LxnsApiException) {
                if (e.statusCode == 404) {
                    // 401 鉴权已通过，404 表示该账号尚未同步过舞萌玩家档案，允许保存密钥以便后续首次传分建档
                    SpUtil.saveLxnsAccount(
                        userToken,
                        0L,
                        context.getString(R.string.lxns_pending_sync_name),
                        0
                    )
                    Toast.makeText(
                        context,
                        R.string.lxns_bind_pending_success,
                        Toast.LENGTH_LONG
                    ).show()
                    onBindingUpdated()
                    dismiss()
                } else {
                    showError(e.message ?: "绑定失败，请稍后重试")
                }
            } catch (e: Exception) {
                showError(e.message ?: "绑定失败，请稍后重试")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.lxnsBindProgress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.lxnsBindConfirmBtn.isEnabled = !loading
        binding.lxnsUnbindBtn.isEnabled = !loading
    }

    private fun showError(msg: String) {
        binding.lxnsBindErrorTv.text = msg
        binding.lxnsBindErrorTv.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.lxnsBindErrorTv.visibility = View.GONE
    }

    override fun dismiss() {
        bindJob?.cancel()
        super.dismiss()
    }
}
