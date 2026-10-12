package com.mi.onextbox.lsp.compat

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.Consumer

/**
 * A one-way, platform-only handoff for package lifecycle events that race module retirement.
 *
 * Export only the child returned by [retire]. It has no reference to the retired receiver or
 * its parent. A callback already holding the retired receiver must use [defer] from its paused
 * gate fallback; it will then find either the unbound child or the current successor receiver.
 */
internal object HotReloadDispatchMailbox {
    private const val QUEUE = 0
    private const val NEXT = 1
    private const val RECEIVER = 2

    fun create(): Array<Any?> = arrayOf(ConcurrentLinkedQueue<Array<Any>>(), null, null)

    fun submit(mailbox: Array<Any?>, event: Array<Any>) {
        route(mailbox, null, event)
    }

    /** Do not call the same paused receiver recursively; a newer receiver may already be ready. */
    fun defer(mailbox: Array<Any?>, expectedReceiver: Consumer<Array<Any>>, event: Array<Any>) {
        route(mailbox, expectedReceiver, event)
    }

    private fun route(mailbox: Array<Any?>, pausedReceiver: Consumer<Array<Any>>?, event: Array<Any>) {
        val receiver = atLeaf(mailbox) { leaf ->
            val current = receiver(leaf)
            if (current == null || current === pausedReceiver) {
                queue(leaf).add(event)
                null
            } else current
        }
        // Never invoke module code while holding a mailbox monitor.
        receiver?.accept(event)
    }

    /** Call after quiescing this generation, before taking the exported target snapshot. */
    fun retire(mailbox: Array<Any?>): Array<Any?> = atLeaf(mailbox) { leaf ->
        val child = create()
        drain(queue(leaf), queue(child))
        leaf[RECEIVER] = null
        leaf[NEXT] = child
        child
    }

    /** Call only after the receiving generation's gate is open, including preparation rollback. */
    fun bind(mailbox: Array<Any?>, receiver: Consumer<Array<Any>>) {
        val pending = atLeaf(mailbox) { leaf ->
            leaf[RECEIVER] = receiver
            ArrayList<Array<Any>>().also { drain(queue(leaf), it) }
        }
        var failure: Throwable? = null
        pending.forEach { event ->
            try {
                receiver.accept(event)
            } catch (error: Throwable) {
                // One failed dispatch must not silently discard the other queued packages.
                if (failure == null) failure = error else if (failure !== error) failure?.addSuppressed(error)
            }
        }
        failure?.let { throw it }
    }

    private inline fun <T> atLeaf(mailbox: Array<Any?>, action: (Array<Any?>) -> T): T {
        var current = mailbox
        while (true) {
            synchronized(current) {
                @Suppress("UNCHECKED_CAST")
                val next = current[NEXT] as? Array<Any?>
                if (next == null) return action(current)
                current = next
            }
        }
    }

    private fun drain(source: ConcurrentLinkedQueue<Array<Any>>, destination: MutableCollection<Array<Any>>) {
        while (true) destination.add(source.poll() ?: return)
    }

    @Suppress("UNCHECKED_CAST")
    private fun queue(mailbox: Array<Any?>) = mailbox[QUEUE] as ConcurrentLinkedQueue<Array<Any>>

    @Suppress("UNCHECKED_CAST")
    private fun receiver(mailbox: Array<Any?>) = mailbox[RECEIVER] as? Consumer<Array<Any>>
}
