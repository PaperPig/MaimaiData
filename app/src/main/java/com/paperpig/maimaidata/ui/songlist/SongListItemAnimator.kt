package com.paperpig.maimaidata.ui.songlist

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.view.View
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator

class SongListItemAnimator : SimpleItemAnimator() {

    private val interpolator: TimeInterpolator = FastOutSlowInInterpolator()

    init {
        changeDuration = 320L
        moveDuration = 320L
        addDuration = 260L
        removeDuration = 200L
    }

    private data class MoveInfo(
        val holder: RecyclerView.ViewHolder,
        val fromX: Int,
        val fromY: Int,
        val toX: Int,
        val toY: Int
    )

    private data class ChangeInfo(
        var oldHolder: RecyclerView.ViewHolder?,
        var newHolder: RecyclerView.ViewHolder?,
        val fromX: Int,
        val fromY: Int,
        val toX: Int,
        val toY: Int
    )

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

    override fun runPendingAnimations() {
        val removalsPending = pendingRemovals.isNotEmpty()
        val movesPending = pendingMoves.isNotEmpty()
        val changesPending = pendingChanges.isNotEmpty()
        val additionsPending = pendingAdditions.isNotEmpty()

        if (!removalsPending && !movesPending && !additionsPending && !changesPending) {
            return
        }

        // 1. Removals
        for (holder in pendingRemovals) {
            animateRemoveImpl(holder)
        }
        pendingRemovals.clear()

        // 2. Moves
        if (movesPending) {
            val moves = ArrayList(pendingMoves)
            movesList.add(moves)
            pendingMoves.clear()
            val mover = Runnable {
                for (moveInfo in moves) {
                    animateMoveImpl(
                        moveInfo.holder,
                        moveInfo.fromX,
                        moveInfo.fromY,
                        moveInfo.toX,
                        moveInfo.toY
                    )
                }
                moves.clear()
                movesList.remove(moves)
            }
            if (removalsPending) {
                val view = moves[0].holder.itemView
                view.postOnAnimationDelayed(mover, removeDuration)
            } else {
                mover.run()
            }
        }

        // 3. Changes (带波浪式交错延迟动画)
        if (changesPending) {
            val changes = ArrayList(pendingChanges)
            changesList.add(changes)
            pendingChanges.clear()
            val changer = Runnable {
                val sortedChanges = changes.sortedBy { it.fromY }
                for ((index, changeInfo) in sortedChanges.withIndex()) {
                    val delay = (index * 22L).coerceAtMost(110L)
                    animateChangeImpl(changeInfo, delay)
                }
                changes.clear()
                changesList.remove(changes)
            }
            if (removalsPending) {
                val holder = changes[0].oldHolder ?: changes[0].newHolder
                holder?.itemView?.postOnAnimationDelayed(changer, removeDuration)
            } else {
                changer.run()
            }
        }

        // 4. Additions
        if (additionsPending) {
            val additions = ArrayList(pendingAdditions)
            additionsList.add(additions)
            pendingAdditions.clear()
            val adder = Runnable {
                for (holder in additions) {
                    animateAddImpl(holder)
                }
                additions.clear()
                additionsList.remove(additions)
            }
            val totalDelay = when {
                removalsPending || movesPending || changesPending -> {
                    val removeTime = if (removalsPending) removeDuration else 0L
                    val moveTime = if (movesPending) moveDuration else 0L
                    val changeTime = if (changesPending) changeDuration else 0L
                    removeTime + maxOf(moveTime, changeTime)
                }

                else -> 0L
            }
            if (totalDelay > 0) {
                val view = additions[0].itemView
                view.postOnAnimationDelayed(adder, totalDelay)
            } else {
                adder.run()
            }
        }
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
        animation.setDuration(removeDuration)
            .alpha(0f)
            .scaleX(0.92f)
            .scaleY(0.92f)
            .setInterpolator(interpolator)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animator: Animator) {
                    animation.setListener(null)
                    view.alpha = 1f
                    view.scaleX = 1f
                    view.scaleY = 1f
                    dispatchRemoveFinished(holder)
                    removeAnimations.remove(holder)
                    dispatchFinishedWhenDone()
                }
            }).start()
    }

    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        resetAnimation(holder)
        holder.itemView.alpha = 0f
        holder.itemView.scaleX = 0.94f
        holder.itemView.scaleY = 0.94f
        pendingAdditions.add(holder)
        return true
    }

    private fun animateAddImpl(holder: RecyclerView.ViewHolder) {
        val view = holder.itemView
        val animation = view.animate()
        addAnimations.add(holder)
        dispatchAddStarting(holder)
        animation.setDuration(addDuration)
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setInterpolator(interpolator)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animator: Animator) {
                    animation.setListener(null)
                    view.alpha = 1f
                    view.scaleX = 1f
                    view.scaleY = 1f
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

    private fun animateMoveImpl(
        holder: RecyclerView.ViewHolder,
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int
    ) {
        val view = holder.itemView
        val deltaX = toX - fromX
        val deltaY = toY - fromY
        if (deltaX != 0) {
            view.animate().translationX(0f)
        }
        if (deltaY != 0) {
            view.animate().translationY(0f)
        }
        val animation = view.animate()
        moveAnimations.add(holder)
        dispatchMoveStarting(holder)
        animation.setDuration(moveDuration)
            .setInterpolator(interpolator)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animator: Animator) {
                    if (deltaX != 0) view.translationX = 0f
                    if (deltaY != 0) view.translationY = 0f
                }

                override fun onAnimationEnd(animator: Animator) {
                    animation.setListener(null)
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
            newHolder.itemView.scaleX = 0.94f
            newHolder.itemView.scaleY = 0.90f
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
            oldAnim.scaleX(0.94f)
            oldAnim.scaleY(0.90f)
            oldAnim.setInterpolator(interpolator)
            oldAnim.startDelay = delay
            dispatchChangeStarting(changeInfo.oldHolder, true)
            oldAnim.setListener(object : AnimatorListenerAdapter() {
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
