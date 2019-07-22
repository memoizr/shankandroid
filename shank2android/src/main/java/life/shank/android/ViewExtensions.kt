package life.shank.android

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import life.shank.Scope
import life.shank.Scoped

internal val View.activity: Activity? get() = context.activity
private val Context.activity: Activity?
    get() = when (this) {
        is Activity -> this
        is ContextThemeWrapper -> this.baseContext.activity
        is android.view.ContextThemeWrapper -> this.baseContext.activity
        is ContextWrapper -> this.baseContext.activity
        else -> null
    }

fun View.doOnScopeReady(onScopeReady: (Scope) -> Unit) {
    if (isAttachedToWindow) {
        doOnScopeReadyWhenViewIsAttachedToWindow(this, onScopeReady)
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewDetachedFromWindow(v: View?) {}
            override fun onViewAttachedToWindow(v: View?) {
                doOnScopeReadyWhenViewIsAttachedToWindow(this@doOnScopeReady, onScopeReady)
                removeOnAttachStateChangeListener(this)
            }
        })
    }
}

private fun doOnScopeReadyWhenViewIsAttachedToWindow(view: View, onScopeReady: (Scope) -> Unit) {
    val scopedOrAutoScopedParentView = view.findScopedOrAutoScopedParentView()
    if (scopedOrAutoScopedParentView != null) {
        when (scopedOrAutoScopedParentView) {
            is Scoped -> onScopeReady(scopedOrAutoScopedParentView.scope)
            is AutoScoped -> scopedOrAutoScopedParentView.onScopeReady(onScopeReady)
        }
        return
    }

    if (view.id == View.NO_ID) throw IllegalArgumentException("View must have an id $view")
    val activity = view.activity ?: throw IllegalArgumentException("View does not have an AppCompatActivity $view")

    if (activity is FragmentActivity) {
        val fragmentManager = activity.supportFragmentManager

        val scopedOrAutoScopedFragment = fragmentManager.findScopedOrAutoScopedFragmentClosestToTheView(view)
        if (scopedOrAutoScopedFragment != null) {
            when (scopedOrAutoScopedFragment) {
                is Scoped -> onScopeReady(scopedOrAutoScopedFragment.scope)
                is AutoScoped -> scopedOrAutoScopedFragment.onScopeReady(onScopeReady)
            }
            return
        }
    }

    when (activity) {
        is Scoped -> onScopeReady(activity.scope)
        is AutoScoped -> activity.onScopeReady(onScopeReady)
        else -> throw IllegalArgumentException("View does not have any parent scopes $view")
    }
}

private fun View.findScopedOrAutoScopedParentView(): View? {
    val parentView = parent as? View ?: return null

    return if (parentView is AutoScoped || parentView is Scoped) parentView
    else parentView.findScopedOrAutoScopedParentView()
}

private fun FragmentManager.findScopedOrAutoScopedFragmentClosestToTheView(view: View): Fragment? {
    return fragments.firstOrNull {
        if (!it.isAdded) return@firstOrNull false

        val fragment = it.childFragmentManager.findScopedOrAutoScopedFragmentClosestToTheView(view)
        if (fragment != null) return fragment

        (it is Scoped || it is AutoScoped) && it.containsView(view)
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Fragment.containsView(view: View) = view == this.view?.findViewById(view.id)