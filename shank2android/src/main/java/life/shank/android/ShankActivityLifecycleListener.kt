package life.shank.android

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import life.shank.Scope
import java.util.*

//private val activityScopedCache = IdentityHashMap<Activity, Scope>()
//val fragmentScopedCache = IdentityHashMap<Fragment, Scope>()
@JvmField
val scopeMap = IdentityHashMap<Any, Scope>()

object ShankActivityLifecycleListener : Application.ActivityLifecycleCallbacks {
    internal val scopeKey = "shank_scope_key"

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        val scope = (savedInstanceState?.getSerializable(scopeKey) as? Scope
            ?: activity.intent.getSerializableExtra(scopeKey) as? Scope
            ?: Scope(UUID.randomUUID()))

//        activityScopedCallbackCache[activity]?.invoke(scope)
        ScopeObservable.putScope(activity.hashCode(), scope)

        (activity as FragmentActivity)
            .supportFragmentManager
            .registerFragmentLifecycleCallbacks(ShankFragmentLifecycleListener, true)
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (activity.isFinishing) {
            ScopeObservable.getScope(activity.hashCode()) {
                it.clear()
            }
        }
        ScopeObservable.clear(activity.hashCode())
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle): Unit =
        ScopeObservable.getScope(activity.hashCode()) {
            outState.putSerializable(scopeKey, it)
        }

    override fun onActivityPaused(activity: Activity?) = Unit
    override fun onActivityResumed(activity: Activity?) = Unit
    override fun onActivityStarted(activity: Activity?) = Unit
    override fun onActivityStopped(activity: Activity?) = Unit
}

