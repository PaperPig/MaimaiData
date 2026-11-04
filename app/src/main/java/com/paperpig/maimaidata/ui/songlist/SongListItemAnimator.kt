package com.paperpig.maimaidata.ui.songlist

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.view.View
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.paperpig.maimaidata.utils.toDp

class SongListItemAnimator : SimpleItemAnimator() {

    private val interpolator: TimeInterpolator = FastOutSlowInInterpolator()

    init {
        changeDuration = 300L
        moveDuration = 300L
        addDuration = 300L
        removeDuration = 260L
    }

    private data class MoveInfo(
        val holder: RecyclerView.ViewHolder,
        val fromX: Int,
        val fromY: Int,
        val toX: Int,
        val toY: Int,
        val isAppearance: Boolean = false,
        val isDisappearance: Boolean = false
    )

    private data class ChangeInfo(
        var oldHolder: RecyclerView.ViewHolder?,
        var newHolder: RecyclerView.ViewHolder?,
        val fromX: Int,
        val fromY: Int,
        val toX: Int,
        val toY: Int
    )

    private sealed class ActiveEntry(val sortPosition: Int, val targetY: Int) {
        class Change(val changeInfo: ChangeInfo, pos: Int, y: Int) : ActiveEntry(pos, y)
        class Move(val moveInfo: MoveInfo, pos: Int, y: Int) : ActiveEntry(pos, y)
        class Add(val holder: RecyclerView.ViewHolder, pos: Int, y: Int) : ActiveEntry(pos, y)
    }

    private val pendingRemovals = ArrayList<RecyclerView.ViewHolder>()
    private val pendingAdditions = ArrayList<RecyclerView.ViewHolder>()
    private val pendingMoves = ArrayList<MoveInfo>()
    private val pendingChanges = ArrayList<ChangeInfo>()

    private val movesList = ArrayList<ArrayList<MoveInfo>>()
    private val changesList = ArrayList<ArrayList<ChangeInfo>>()
    private val additionsList = ArrayList<ArrayList<RecyclerView.ViewHolder>>()

    private val removeAnimations = ArrayList<RecyclerView.ViewHolder>()
    private val moveAnimations = ArrayList<RecyclerView.ViewHolder>()
    private val addAnimations = ArrayList<RecyclerView.ViewHolder>()
    private val changeAnimations = ArrayList<RecyclerView.ViewHolder>()

    override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder): Boolean = false

    override fun animateAppearance(
        viewHolder: RecyclerView.ViewHolder,
        preLayoutInfo: ItemHolderInfo?,
        postLayoutInfo: ItemHolderInfo
    ): Boolean {
        if (preLayoutInfo != null && (preLayoutInfo.left != postLayoutInfo.left || preLayoutInfo.top != postLayoutInfo.top)) {
            var x = preLayoutInfo.left
            var y = preLayoutInfo.top
            val view = viewHolder.itemView
            x += view.translationX.toInt()
            y += view.translationY.toInt()
            resetAnimation(viewHolder)
            val deltaX = postLayoutInfo.left - x
            val deltaY = postLayoutInfo.top - y
            if (deltaX == 0 && deltaY == 0) {
                dispatchMoveFinished(viewHolder)
                return false
            }
            if (deltaX != 0) {
                view.translationX = -deltaX.toFloat()
            }
            if (deltaY != 0) {
                view.translationY = -deltaY.toFloat()
            }
            // 与 Change/Add 视觉一致：透明度与微缩起步
            view.alpha = 0f
            view.scaleX = 0.95f
            view.scaleY = 0.92f
            pendingMoves.add(
                MoveInfo(
                    viewHolder,
                    x,
                    y,
                    postLayoutInfo.left,
                    postLayoutInfo.top,
                    isAppearance = true
                )
            )
            return true
        } else {
            return animateAdd(viewHolder)
        }
    }

    override fun animateDisappearance(
        viewHolder: RecyclerView.ViewHolder,
        preLayoutInfo: ItemHolderInfo,
        postLayoutInfo: ItemHolderInfo?
    ): Boolean {
        val oldLeft = preLayoutInfo.left
        val oldTop = preLayoutInfo.top
        val disappearingItemView = viewHolder.itemView
        val newLeft = postLayoutInfo?.left ?: disappearingItemView.left
        val newTop = postLayoutInfo?.top ?: (disappearingItemView.top + 70.toDp().toInt())
        val isRemoved = viewHolder.bindingAdapterPosition == RecyclerView.NO_POSITION
        if (!isRemoved && (oldLeft != newLeft || oldTop != newTop)) {
            disappearingItemView.layout(
                newLeft, newTop,
                newLeft + disappearingItemView.width,
                newTop + disappearingItemView.height
            )
            var x = oldLeft
            var y = oldTop
            x += disappearingItemView.translationX.toInt()
            y += disappearingItemView.translationY.toInt()
            resetAnimation(viewHolder)
            val deltaX = newLeft - x
            val deltaY = newTop - y
            if (deltaX != 0) {
                disappearingItemView.translationX = -deltaX.toFloat()
            }
            if (deltaY != 0) {
                disappearingItemView.translationY = -deltaY.toFloat()
            }
            pendingMoves.add(
                MoveInfo(
                    viewHolder,
                    x,
                    y,
                    newLeft,
                    newTop,
                    isDisappearance = true
                )
            )
            return true
        } else {
            return animateRemove(viewHolder)
        }
    }

    override fun runPendingAnimations() {
        val removalsPending = pendingRemovals.isNotEmpty()
        val movesPending = pendingMoves.isNotEmpty()
        val changesPending = pendingChanges.isNotEmpty()
        val additionsPending = pendingAdditions.isNotEmpty()

        if (!removalsPending && !movesPending && !additionsPending && !changesPending) {
            return
        }

        // 1. Removals（并发向下平滑滑动淡出）
        for (holder in pendingRemovals) {
            animateRemoveImpl(holder)
        }
        pendingRemovals.clear()

        // 2. 将所有在视口内展示/入场的 item 汇聚成同一序列，按屏幕逻辑顺序统一计算瀑布流延时
        val activeEntries = ArrayList<ActiveEntry>()

        if (changesPending) {
            val changes = ArrayList(pendingChanges)
            changesList.add(changes)
            for (changeInfo in changes) {
                val holder = changeInfo.newHolder ?: changeInfo.oldHolder
                val pos = holder?.bindingAdapterPosition
                    ?.takeIf { it != RecyclerView.NO_POSITION }
                    ?: holder?.absoluteAdapterPosition
                    ?.takeIf { it != RecyclerView.NO_POSITION }
                    ?: 0
                activeEntries.add(ActiveEntry.Change(changeInfo, pos, changeInfo.toY))
            }
            pendingChanges.clear()
        }

        if (movesPending) {
            val moves = ArrayList(pendingMoves)
            movesList.add(moves)
            for (moveInfo in moves) {
                val pos = moveInfo.holder.bindingAdapterPosition
                    .takeIf { it != RecyclerView.NO_POSITION }
                    ?: moveInfo.holder.absoluteAdapterPosition
                    .takeIf { it != RecyclerView.NO_POSITION }
                    ?: 0
                activeEntries.add(ActiveEntry.Move(moveInfo, pos, moveInfo.toY))
            }
            pendingMoves.clear()
        }

        if (additionsPending) {
            val additions = ArrayList(pendingAdditions)
            additionsList.add(additions)
            for (holder in additions) {
                val pos = holder.bindingAdapterPosition
                    .takeIf { it != RecyclerView.NO_POSITION }
                    ?: holder.absoluteAdapterPosition
                    .takeIf { it != RecyclerView.NO_POSITION }
                    ?: 0
                activeEntries.add(ActiveEntry.Add(holder, pos, holder.itemView.top))
            }
            pendingAdditions.clear()
        }

        // 按 Adapter 物理位置升序排列（确保从视口顶部到视口底部绝对单调递增）
        activeEntries.sortWith(compareBy({ it.sortPosition }, { it.targetY }))

        // 是否属于卡片收缩/展开/进场等多卡片联动
        val isCoordinatedTransition = changesPending || additionsPending || movesPending

        for ((index, entry) in activeEntries.withIndex()) {
            val delay = if (isCoordinatedTransition) {
                (index * 22L).coerceAtMost(160L)
            } else {
                0L
            }
            when (entry) {
                is ActiveEntry.Change -> animateChangeImpl(entry.changeInfo, delay)
                is ActiveEntry.Move -> animateMoveImpl(entry.moveInfo, delay)
                is ActiveEntry.Add -> animateAddImpl(entry.holder, delay)
            }
        }
        changesList.clear()
        movesList.clear()
        additionsList.clear()
    }

    override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
        resetAnimation(holder)
        pendingRemovals.add(holder)
        return true
    }

    private fun animateRemoveImpl(holder: RecyclerView.ViewHolder) {
        val view = holder.itemView
        val animation = view.animate()
        removeAnimations.add(holder)
        dispatchRemoveStarting(holder)
        val slideDistance = 60.toDp()
        animation.setDuration(removeDuration)
            .alpha(0f)
            .scaleX(0.95f)
            .scaleY(0.92f)
            .translationYBy(slideDistance)
            .setInterpolator(interpolator)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animator: Animator) {
                    view.alpha = 1f
                    view.scaleX = 1f
                    view.scaleY = 1f
                    view.translationY = 0f
                }

                override fun onAnimationEnd(animator: Animator) {
                    animation.setListener(null)
                    view.alpha = 1f
                    view.scaleX = 1f
                    view.scaleY = 1f
                    view.translationY = 0f
                    dispatchRemoveFinished(holder)
                    removeAnimations.remove(holder)
                    dispatchFinishedWhenDone()
                }
            }).start()
    }

    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        resetAnimation(holder)
        val view = holder.itemView
        view.alpha = 0f
        view.scaleX = 0.95f
        view.scaleY = 0.92f
        // 初始位移与收缩靠拢时的动势相吻合（从下方 60dp 处顺滑上浮展开）
        view.translationY = 60.toDp()
        pendingAdditions.add(holder)
        return true
    }

    private fun animateAddImpl(holder: RecyclerView.ViewHolder, delay: Long = 0L) {
        val view = holder.itemView
        val animation = view.animate()
        addAnimations.add(holder)
        dispatchAddStarting(holder)
        animation.setDuration(addDuration)
            .setStartDelay(delay)
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .translationX(0f)
            .setInterpolator(interpolator)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animator: Animator) {
                    view.alpha = 1f
                    view.scaleX = 1f
                    view.scaleY = 1f
                    view.translationY = 0f
                    view.translationX = 0f
                }

                override fun onAnimationEnd(animator: Animator) {
                    animation.setListener(null)
                    view.alpha = 1f
                    view.scaleX = 1f
                    view.scaleY = 1f
                    view.translationY = 0f
                    view.translationX = 0f
                    dispatchAddFinished(holder)
                    addAnimations.remove(holder)
                    dispatchFinishedWhenDone()
                }
            }).start()
    }

    override fun animateMove(
        holder: RecyclerView.ViewHolder,
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int
    ): Boolean {
        var x = fromX
        var y = fromY
        val view = holder.itemView
        x += view.translationX.toInt()
        y += view.translationY.toInt()
        resetAnimation(holder)
        val deltaX = toX - x
        val deltaY = toY - y
        if (deltaX == 0 && deltaY == 0) {
            dispatchMoveFinished(holder)
            return false
        }
        if (deltaX != 0) {
            view.translationX = -deltaX.toFloat()
        }
        if (deltaY != 0) {
            view.translationY = -deltaY.toFloat()
        }
        pendingMoves.add(MoveInfo(holder, x, y, toX, toY))
        return true
    }

    private fun animateMoveImpl(moveInfo: MoveInfo, delay: Long = 0L) {
        val holder = moveInfo.holder
        val view = holder.itemView
        val deltaX = moveInfo.toX - moveInfo.fromX
        val deltaY = moveInfo.toY - moveInfo.fromY
        if (deltaX != 0) {
            view.animate().translationX(0f)
        }
        if (deltaY != 0) {
            view.animate().translationY(0f)
        }
        val animation = view.animate()
        moveAnimations.add(holder)
        dispatchMoveStarting(holder)

        if (moveInfo.isAppearance) {
            animation.alpha(1f).scaleX(1f).scaleY(1f)
        } else if (moveInfo.isDisappearance) {
            animation.alpha(0f).scaleX(0.95f).scaleY(0.92f)
        }

        animation.setDuration(moveDuration)
            .setStartDelay(delay)
            .setInterpolator(interpolator)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animator: Animator) {
                    if (deltaX != 0) view.translationX = 0f
                    if (deltaY != 0) view.translationY = 0f
                    view.alpha = 1f
                    view.scaleX = 1f
                    view.scaleY = 1f
                }

                override fun onAnimationEnd(animator: Animator) {
                    animation.setListener(null)
                    view.alpha = 1f
                    view.scaleX = 1f
                    view.scaleY = 1f
                    view.translationX = 0f
                    view.translationY = 0f
                    dispatchMoveFinished(holder)
                    moveAnimations.remove(holder)
                    dispatchFinishedWhenDone()
                }
            }).start()
    }

    override fun animateChange(
        oldHolder: RecyclerView.ViewHolder,
        newHolder: RecyclerView.ViewHolder?,
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int
    ): Boolean {
        if (oldHolder === newHolder) {
            return animateMove(oldHolder, fromX, fromY, toX, toY)
        }
        val prevTranslationX = oldHolder.itemView.translationX
        val prevTranslationY = oldHolder.itemView.translationY
        val prevAlpha = oldHolder.itemView.alpha
        resetAnimation(oldHolder)
        val deltaX = (toX - fromX - prevTranslationX).toInt()
        val deltaY = (toY - fromY - prevTranslationY).toInt()
        oldHolder.itemView.translationX = prevTranslationX
        oldHolder.itemView.translationY = prevTranslationY
        oldHolder.itemView.alpha = prevAlpha
        if (newHolder != null) {
            resetAnimation(newHolder)
            newHolder.itemView.translationX = -deltaX.toFloat()
            newHolder.itemView.translationY = -deltaY.toFloat()
            newHolder.itemView.alpha = 0f
            newHolder.itemView.scaleX = 0.95f
            newHolder.itemView.scaleY = 0.92f
        }
        pendingChanges.add(ChangeInfo(oldHolder, newHolder, fromX, fromY, toX, toY))
        return true
    }

    private fun animateChangeImpl(changeInfo: ChangeInfo, delay: Long) {
        val oldHolder = changeInfo.oldHolder
        val oldView = oldHolder?.itemView
        val newHolder = changeInfo.newHolder
        val newView = newHolder?.itemView

        if (oldView != null) {
            val oldAnim = oldView.animate().setDuration(changeDuration)
            changeAnimations.add(changeInfo.oldHolder!!)
            oldAnim.translationX((changeInfo.toX - changeInfo.fromX).toFloat())
            oldAnim.translationY((changeInfo.toY - changeInfo.fromY).toFloat())
            oldAnim.alpha(0f)
            oldAnim.scaleX(0.95f)
            oldAnim.scaleY(0.92f)
            oldAnim.setInterpolator(interpolator)
            oldAnim.startDelay = delay
            dispatchChangeStarting(changeInfo.oldHolder, true)
            oldAnim.setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animator: Animator) {
                    oldView.alpha = 1f
                    oldView.scaleX = 1f
                    oldView.scaleY = 1f
                    oldView.translationX = 0f
                    oldView.translationY = 0f
                }

                override fun onAnimationEnd(animator: Animator) {
                    oldAnim.setListener(null)
                    oldView.alpha = 1f
                    oldView.scaleX = 1f
                    oldView.scaleY = 1f
                    oldView.translationX = 0f
                    oldView.translationY = 0f
                    dispatchChangeFinished(changeInfo.oldHolder, true)
                    changeAnimations.remove(changeInfo.oldHolder)
                    dispatchFinishedWhenDone()
                }
            }).start()
        }

        if (newView != null) {
            val newAnim = newView.animate()
            changeAnimations.add(changeInfo.newHolder!!)
            dispatchChangeStarting(changeInfo.newHolder, false)
            newAnim.translationX(0f)
                .translationY(0f)
                .setDuration(changeDuration)
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setInterpolator(interpolator)
                .setStartDelay(delay)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationCancel(animator: Animator) {
                        newView.alpha = 1f
                        newView.scaleX = 1f
                        newView.scaleY = 1f
                        newView.translationX = 0f
                        newView.translationY = 0f
                    }

                    override fun onAnimationEnd(animator: Animator) {
                        newAnim.setListener(null)
                        newView.alpha = 1f
                        newView.scaleX = 1f
                        newView.scaleY = 1f
                        newView.translationX = 0f
                        newView.translationY = 0f
                        dispatchChangeFinished(changeInfo.newHolder, false)
                        changeAnimations.remove(changeInfo.newHolder)
                        dispatchFinishedWhenDone()
                    }
                }).start()
        }
    }

    private fun endChangeAnimation(infoList: MutableList<ChangeInfo>, item: RecyclerView.ViewHolder) {
        val iterator = infoList.iterator()
        while (iterator.hasNext()) {
            val changeInfo = iterator.next()
            if (endChangeAnimationIfNecessary(changeInfo, item)) {
                if (changeInfo.oldHolder == null && changeInfo.newHolder == null) {
                    iterator.remove()
                }
            }
        }
    }

    private fun endChangeAnimationIfNecessary(changeInfo: ChangeInfo) {
        if (changeInfo.oldHolder != null) {
            endChangeAnimationIfNecessary(changeInfo, changeInfo.oldHolder!!)
        }
        if (changeInfo.newHolder != null) {
            endChangeAnimationIfNecessary(changeInfo, changeInfo.newHolder!!)
        }
    }

    private fun endChangeAnimationIfNecessary(
        changeInfo: ChangeInfo,
        item: RecyclerView.ViewHolder
    ): Boolean {
        var oldItem = false
        if (changeInfo.newHolder === item) {
            changeInfo.newHolder = null
        } else if (changeInfo.oldHolder === item) {
            changeInfo.oldHolder = null
            oldItem = true
        } else {
            return false
        }
        resetAnimation(item)
        dispatchChangeFinished(item, oldItem)
        return true
    }

    override fun endAnimation(item: RecyclerView.ViewHolder) {
        val view = item.itemView
        view.animate().cancel()

        for (i in pendingMoves.indices.reversed()) {
            val moveInfo = pendingMoves[i]
            if (moveInfo.holder === item) {
                view.translationY = 0f
                view.translationX = 0f
                dispatchMoveFinished(item)
                pendingMoves.removeAt(i)
            }
        }
        endChangeAnimation(pendingChanges, item)
        if (pendingRemovals.remove(item)) {
            resetAnimation(item)
            dispatchRemoveFinished(item)
        }
        if (pendingAdditions.remove(item)) {
            resetAnimation(item)
            dispatchAddFinished(item)
        }

        for (i in changesList.indices.reversed()) {
            val changes = changesList[i]
            endChangeAnimation(changes, item)
            if (changes.isEmpty()) {
                changesList.removeAt(i)
            }
        }
        for (i in movesList.indices.reversed()) {
            val moves = movesList[i]
            for (j in moves.indices.reversed()) {
                val moveInfo = moves[j]
                if (moveInfo.holder === item) {
                    view.translationY = 0f
                    view.translationX = 0f
                    dispatchMoveFinished(item)
                    moves.removeAt(j)
                    if (moves.isEmpty()) {
                        movesList.removeAt(i)
                    }
                    break
                }
            }
        }
        for (i in additionsList.indices.reversed()) {
            val additions = additionsList[i]
            if (additions.remove(item)) {
                resetAnimation(item)
                dispatchAddFinished(item)
                if (additions.isEmpty()) {
                    additionsList.removeAt(i)
                }
            }
        }

        removeAnimations.remove(item)
        addAnimations.remove(item)
        changeAnimations.remove(item)
        moveAnimations.remove(item)

        dispatchFinishedWhenDone()
    }

    private fun resetAnimation(holder: RecyclerView.ViewHolder) {
        val view = holder.itemView
        view.animate().cancel()
        view.alpha = 1f
        view.scaleX = 1f
        view.scaleY = 1f
        view.translationX = 0f
        view.translationY = 0f
    }

    override fun endAnimations() {
        var count = pendingMoves.size
        for (i in count - 1 downTo 0) {
            val item = pendingMoves[i]
            val view = item.holder.itemView
            view.translationY = 0f
            view.translationX = 0f
            dispatchMoveFinished(item.holder)
            pendingMoves.removeAt(i)
        }
        count = pendingRemovals.size
        for (i in count - 1 downTo 0) {
            val item = pendingRemovals[i]
            dispatchRemoveFinished(item)
            pendingRemovals.removeAt(i)
        }
        count = pendingAdditions.size
        for (i in count - 1 downTo 0) {
            val item = pendingAdditions[i]
            resetAnimation(item)
            dispatchAddFinished(item)
            pendingAdditions.removeAt(i)
        }
        count = pendingChanges.size
        for (i in count - 1 downTo 0) {
            endChangeAnimationIfNecessary(pendingChanges[i])
        }
        pendingChanges.clear()
        if (!isRunning) {
            return
        }

        var listCount = movesList.size
        for (i in listCount - 1 downTo 0) {
            val moves = movesList[i]
            count = moves.size
            for (j in count - 1 downTo 0) {
                val moveInfo = moves[j]
                val item = moveInfo.holder
                val view = item.itemView
                view.translationY = 0f
                view.translationX = 0f
                dispatchMoveFinished(moveInfo.holder)
                moves.removeAt(j)
            }
            movesList.removeAt(i)
        }
        listCount = additionsList.size
        for (i in listCount - 1 downTo 0) {
            val additions = additionsList[i]
            count = additions.size
            for (j in count - 1 downTo 0) {
                val item = additions[j]
                resetAnimation(item)
                dispatchAddFinished(item)
                additions.removeAt(j)
            }
            additionsList.removeAt(i)
        }
        listCount = changesList.size
        for (i in listCount - 1 downTo 0) {
            val changes = changesList[i]
            count = changes.size
            for (j in count - 1 downTo 0) {
                endChangeAnimationIfNecessary(changes[j])
            }
            changesList.removeAt(i)
        }

        cancelAll(removeAnimations)
        cancelAll(moveAnimations)
        cancelAll(addAnimations)
        cancelAll(changeAnimations)

        dispatchAnimationsFinished()
    }

    private fun cancelAll(viewHolders: List<RecyclerView.ViewHolder>) {
        for (i in viewHolders.indices.reversed()) {
            viewHolders[i].itemView.animate().cancel()
        }
    }

    override fun isRunning(): Boolean {
        return pendingAdditions.isNotEmpty() ||
                pendingChanges.isNotEmpty() ||
                pendingMoves.isNotEmpty() ||
                pendingRemovals.isNotEmpty() ||
                moveAnimations.isNotEmpty() ||
                removeAnimations.isNotEmpty() ||
                addAnimations.isNotEmpty() ||
                changeAnimations.isNotEmpty() ||
                movesList.isNotEmpty() ||
                additionsList.isNotEmpty() ||
                changesList.isNotEmpty()
    }

    private fun dispatchFinishedWhenDone() {
        if (!isRunning) {
            dispatchAnimationsFinished()
        }
    }
}
