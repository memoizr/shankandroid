package memoizrlabs.com.shankandroid

import life.shank.AutoDetachable
import life.shank.DetachAware

abstract class Presenter<V : Presenter.View> {
    abstract fun onAttach(v: V)
    abstract fun onDetach(v: V)

    interface View
}

abstract class PresenterAdapter<V>: Presenter<V>(), DetachAware<V> where V: AutoDetachable, V: Presenter.View {
    override fun attach(v: V) {
        onAttach(v)
    }

    override fun detach(v: V) {
        onDetach(v)
    }

    interface View : Presenter.View, AutoDetachable
}
