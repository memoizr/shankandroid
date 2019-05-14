package life.shank.android

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import life.shank.*
import life.shank.android.Helper.xxx


object Helper {
    inline fun AutoScoped.xxx(noinline osr: (Scope) -> Unit) {
        when (this) {
            is LifecycleOwner -> ScopeObservable.getScope(this.hashCode(), osr)
            is View -> findParentScopeForView(this).doScoped(osr)
            else -> throw IllegalArgumentException()
        }
    }

    fun findParentScopeForView(view: View): AutoScoped {

        val parentViewScope = view.findScopeInParentView()
        if (parentViewScope != null) return parentViewScope

        if (view.id == View.NO_ID) throw IllegalArgumentException("View must have an id $view")
        val activity = view.context as? AppCompatActivity
            ?: throw IllegalArgumentException("View does not have an AppCompatActivity $view")
        val fragmentManager = activity.supportFragmentManager

        val parentFragmentScope = fragmentManager.findScopeInFragmentClosestToTheView(view)
        if (parentFragmentScope != null) return parentFragmentScope

        if (activity is AutoScoped) return activity

        throw IllegalArgumentException("View does not have any parent scopes $view")
    }

    fun FragmentManager.findScopeInFragmentClosestToTheView(view: View): AutoScoped? {
        val scopedFragment = fragments.firstOrNull {
            val scopedChildFragment = it.childFragmentManager.findScopeInFragmentClosestToTheView(view)
            if (scopedChildFragment != null) return scopedChildFragment

            it.containsView(view) && it is AutoScoped
        } as? AutoScoped
        return scopedFragment
    }

    fun View.findScopeInParentView(): AutoScoped? {
        val parentView = parent as? View
        if (parentView != null) {
            return if (parentView is AutoScoped) parentView else parentView.findScopeInParentView()
        }
        return null
    }

    inline private fun Fragment.containsView(view: View) = view == this.view?.findViewById(view.id)
}


interface AutoScoped {
    fun doScoped(onScopeReady: (Scope) -> Unit) = xxx(onScopeReady)

    operator fun <T> ScopedProvider0<T>.invoke(o: (T) -> Unit = {}) = doScoped { invoke(it).also { o(it) } }
    operator fun <A, T> ScopedProvider1<A, T>.invoke(a: A, o: (T) -> Unit = {}) = doScoped { invoke(it, a).also { o(it) } }
    operator fun <A, B, T> ScopedProvider2<A, B, T>.invoke(a: A, b: B, o: (T) -> Unit = {}) = doScoped { invoke(it, a, b).also { o(it) } }
    operator fun <A, B, C, T> ScopedProvider3<A, B, C, T>.invoke(a: A, b: B, c: C, o: (T) -> Unit = {}) = doScoped { invoke(it, a, b, c).also { o(it) } }
}