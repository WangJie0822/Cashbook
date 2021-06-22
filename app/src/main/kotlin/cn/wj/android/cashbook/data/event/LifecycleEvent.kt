package cn.wj.android.cashbook.data.event

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer

/**
 * 与生命周期关联的事件
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/22
 */
class LifecycleEvent<T> : LifecycleEventObserver {

    private var observer: Observer<T>? = null

    var value: T? = null
        set(value) {
            field = value
            observer?.onChanged(field)
        }

    fun observe(owner: LifecycleOwner, observer: Observer<T>) {
        owner.lifecycle.addObserver(this)
        this.observer = observer
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_DESTROY) {
            observer = null
            value = null
            source.lifecycle.removeObserver(this)
        }
    }
}