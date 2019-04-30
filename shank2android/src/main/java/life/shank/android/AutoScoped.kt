package life.shank.android

import android.app.Activity
import android.content.Intent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import life.shank.Scope
import life.shank.Scoped
import life.shank.android.Helper.scopee
import life.shank.android.ShankActivityLifecycleListener.scopeKey


object Helper {
    @JvmStatic
    inline fun AutoScoped.scopee(): Scope {
        return scopeMap[this] ?: when (this) {
            is View -> findParentScopeForView(this)
            is Activity -> activityScopedCache[this]!!
            is Fragment -> fragmentScopedCache[this]!!
            else -> throw IllegalArgumentException()
        }.also { scopeMap[this] = it.addOnClearAction { scopeMap.remove(this) } }
    }

    inline fun findParentScopeForView(view: View): Scope {
        val parentViewScope = view.findScopeInParentView()
        if (parentViewScope != null) return parentViewScope

        if (view.id == 0) throw IllegalArgumentException("View must have an id $view")
        val activity = view.context as? AppCompatActivity
            ?: throw IllegalArgumentException("View does not have an AppCompatActivity $view")
        val fragmentManager = activity.supportFragmentManager

        val parentFragmentScope = fragmentManager.findScopeInFragmentClosestToTheView(view)
        if (parentFragmentScope != null) return parentFragmentScope

        if (activity is Scoped) return activity.scope

        throw IllegalArgumentException("View does not have any parent scopes $view")
    }

    fun FragmentManager.findScopeInFragmentClosestToTheView(view: View): Scope? {
        val scopedFragment = fragments.firstOrNull {
            val scopedChildFragment = it.childFragmentManager.findScopeInFragmentClosestToTheView(view)
            if (scopedChildFragment != null) return scopedChildFragment

            it.containsView(view) && it is Scoped
        } as? Scoped
        return scopedFragment?.scope
    }

    fun View.findScopeInParentView(): Scope? {
        val parentView = parent as? View
        if (parentView != null) {
            if (parentView is Scoped) return parentView.scope
            return parentView.findScopeInParentView()
        }
        return null
    }

    inline private fun Fragment.containsView(view: View) = view == this.view?.findViewById(view.id)
}


interface AutoScoped : Scoped {
    override val scope: Scope get() = scopee()


    fun Intent.nestedScopeExtra() = putExtra(scopeKey, scope)


    private fun findParentScopeForFragment(fragment: Fragment): Scope {
        val parentFragment = fragment.parentFragment
        if (parentFragment == null) {
            val activity = fragment.activity
            if (activity != null && activity is Scoped) return activity.scope
            throw IllegalArgumentException("Fragment does not have scoped parent $fragment")
        } else {
            if (parentFragment is Scoped) return parentFragment.scope
            return findParentScopeForFragment(parentFragment)
        }
    }
}