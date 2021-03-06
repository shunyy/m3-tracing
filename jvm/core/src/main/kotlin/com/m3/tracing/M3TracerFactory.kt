package com.m3.tracing

import com.m3.tracing.internal.Config
import com.m3.tracing.tracer.logging.M3LoggingTracer
import org.slf4j.LoggerFactory
import javax.annotation.concurrent.ThreadSafe

/**
 * Singleton factory/holder of [M3Tracer] instance.
 *
 * To choose [M3Tracer] implementation, use one of following methods:
 * 1. Set FQCN of [M3Tracer] implementation in system property
 * 2. Set FQCN of [M3Tracer] implementation in environment variable
 */
@ThreadSafe
object M3TracerFactory {
    private const val tracerFQCNConfigName = "m3.tracer.fqcn"

    private val logger = LoggerFactory.getLogger(M3TracerFactory::class.java)

    // Use SYNCHRONIZED mode to prevent double-initialization of tracer SDKs.
    private val tracer: M3Tracer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { createTracer() }

    /**
     * Get instance of [M3Tracer].
     * Always returns same instance.
     */
    fun get() = tracer

    private fun createTracer(): M3Tracer {
        return createTracerByFQCN(Config[tracerFQCNConfigName])
                ?: M3LoggingTracer()
    }
    private fun createTracerByFQCN(fqcn: String?): M3Tracer? {
        if (fqcn == null) return null

        try {
            @Suppress("UNCHECKED_CAST")
            val cls = Class.forName(fqcn) as Class<out M3Tracer>
            if (! M3Tracer::class.java.isAssignableFrom(cls)) {
                logger.error("Ignored invalid tracer FQCN (is not subclass of ${M3Tracer::class.java.name}): \"$fqcn\"")
                return null
            }

            val ctor = cls.getConstructor()
            return ctor.newInstance()
        } catch (e: ReflectiveOperationException) {
            // ClassNotFoundException, NoSuchMethodException, InvocationTargetException, ...
            logger.error("Ignored invalid tracer FQCN (${e.javaClass.simpleName}): \"$fqcn\"", e)
            return null
        }
    }
}
