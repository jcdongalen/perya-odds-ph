import Foundation
import shared

/// Extension to make Kotlin StateFlow observable from Swift
/// This provides a standard bridge between Kotlin coroutines and Swift async patterns

extension Kotlinx_coroutines_coreStateFlow {
    /// Observes the StateFlow and calls the callback with each new value
    /// - Parameter onValue: Callback invoked on the main thread with each new value
    /// - Returns: A Closeable handle to cancel the observation
    func watch<T>(onValue: @escaping (T) -> Void) -> Closeable {
        let collector = FlowCollector<T>(callback: onValue)
        
        // Collect the flow using a coroutine
        let job = Kotlinx_coroutines_coreKt.launchIn(
            self,
            scope: Kotlinx_coroutines_coreMainScope()
        ) { value in
            collector.emit(value: value)
        }
        
        return CloseableJob(job: job)
    }
}

/// Protocol for closeable resources
protocol Closeable {
    func close()
}

/// Wrapper to make Kotlinx_coroutines_coreJob conform to Closeable
private class CloseableJob: Closeable {
    private var job: Kotlinx_coroutines_coreJob?
    
    init(job: Kotlinx_coroutines_coreJob) {
        self.job = job
    }
    
    func close() {
        job?.cancel(cause: nil)
        job = nil
    }
}

/// Collector that bridges Kotlin Flow values to Swift callbacks
private class FlowCollector<T> {
    private let callback: (T) -> Void
    
    init(callback: @escaping (T) -> Void) {
        self.callback = callback
    }
    
    func emit(value: Any?) {
        if let typedValue = value as? T {
            // Ensure callback is called on main thread
            if Thread.isMainThread {
                callback(typedValue)
            } else {
                DispatchQueue.main.async {
                    self.callback(typedValue)
                }
            }
        }
    }
}

