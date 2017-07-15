package io.nadilas.deepstream.orpcjava

/**
 * Created by janosveres on 17.06.17.
 */

internal const val CREATE_SESSION_METHOD_NAME = "createSession"
internal const val DESTROY_SESSION_METHOD_NAME = "destroySession"

/**
 * The interval based on which the SessionManager determines if the session is still alive
 */
internal const val HEARTBEAT_CHECK_INTERVALS: Long = 30000
internal const val HEARTBEAT_CHECK_ADDRESS: String = "heartbeat"
