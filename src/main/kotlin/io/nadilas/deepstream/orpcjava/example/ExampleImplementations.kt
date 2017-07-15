package io.nadilas.deepstream.orpcjava.example

import io.deepstream.ConnectionState
import io.deepstream.DeepstreamClient
import io.deepstream.RpcRequestedListener
import org.slf4j.LoggerFactory
import org.springframework.util.StringUtils
import java.lang.reflect.Method
import java.util.*
import kotlin.reflect.KFunction0

/**
 * Created by janosveres on 17.06.17.
 */


/**
 * Example custom implementation of a {@link SessionProvider}
 *
 * This example is using deestream's record list instead of the internal SessionManager to store the sessions
 * belonging to this client in deepstream.
 */
class CustomSessionProvider(dsClient: DeepstreamClient, clientMode: ClientMode, vararg providers: KFunction0<IServiceProvider>) : SessionProvider(dsClient, clientMode, *providers) {
    private val sessionIdRecordList = dsClient.uid + "/sessions"

    /**
     * @see SessionProvider.destroySession
     */
    override fun destroySession(input: Any): ISession? {
        val inputSession = input as UserSession // serializable object
        //val gson = Gson()

//        try {
            val (sessionId, authToken, userId) = inputSession // gson.fromJson(input as String, UserSession::class.java)
            if(!StringUtils.isEmpty(sessionId) && !StringUtils.isEmpty(userId) && !StringUtils.isEmpty(authToken)) {
                logger.info("destroySession was called for sessionId: $sessionId")
                if(dsClient.recordHandler.getList(sessionIdRecordList).entries.contains(sessionId)) {
                    // here the base functionality is used to return the session as it gets stored upon creation anyways
                    // there's no need for a custom session manager implementation
                    val session: UserSession? = SessionManager.instance.find(sessionId)

                    // some validation to see if the session's authtoken corresponds to the input one
                    if(session != null && authToken == session.accessToken)
                    {
                        logger.info("destroySession ($sessionId): allow")
                        return session
                    }
                    else if(session != null)
                    {
                        val accessToken = session.accessToken
                        throw InputMismatchException("Session ($sessionId) cannot be closed, provided AuthToken ($authToken) doesn't match stored version ($accessToken).")
                    }
                }
            }
//        } catch(e: JsonSyntaxException) {
//            logger.error("input data was not in the correct format.", e)
//        }
        return null
    }

    /**
     * @see SessionProvider.cleanupSession
     */
    override fun cleanupSession(session: ISession) {
        val sid = session.sessionUuid
        logger.info("cleanup session was called for $sid")
        dsClient.recordHandler.getList(sessionIdRecordList).removeEntry(sid)
    }

    private val logger = LoggerFactory.getLogger(DefaultSessionProvider::class.java)

    /**
     * @see SessionProvider.createSession
     */
    override fun createSession(input: Any): ISession {
        // let's say we need a userId as input
        val userid = input as String
        val sessionid = UUID.randomUUID().toString()
        // save session
        val mySessions = dsClient.recordHandler.getList(sessionIdRecordList)
        mySessions.addEntry(sessionid)

        return UserSession(sessionid, "someAccessToken", userid)
    }
}

/**
 * Example custom ISession implementation of {@link ISession} interace
 *
 * This example is using two additional properties for access validation: accessToken and userId. Also this class provides
 * a helper method to find a UserSession by its userId
 */
data class UserSession(override val sessionUuid: String, val accessToken: String, val userId: String) : ISession {
    override fun getMethodPath(qualifiedMethodName: String): String {
        return "$sessionUuid/$qualifiedMethodName"
    }

    companion object {
        /**
         * A shorthand extension method to find a DefaultSession by its userId
         * @param userId the string userId the session has been initialized with
         */
        fun findByUserId(userId: String): UserSession? = SessionManager.instance.sessions.find { it is UserSession && it.userId == userId } as UserSession
    }
}

class CustomProtoRpcHandlerImplementation(override val dsClient: DeepstreamClient, override val clientMode: ClientMode, override var sessionProvider: SessionProvider) : IProtoRpcHandler {
    override fun closeSession() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun registerCallback(defaultHeartbeatCallbackImpl: ISessionServiceCallback) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <R : ISession> startSession(vararg sessionCallbackImplementations: ISessionServiceCallback): R? {
        val rpcResult = dsClient.rpcHandler.make(CREATE_SESSION_METHOD_NAME, "myUserId")
        if(rpcResult.success())
        {
            val (sessionUuid, accessToken, userId) = rpcResult.data as UserSession
            println("success, session started: $sessionUuid for $userId with token: $accessToken")
        }

        return null // no session could be created
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun register(vararg providers: KFunction0<IServiceProvider>): IProtoRpcHandler {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun unregister() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun registerProvider(providerFactory: KFunction0<IServiceProvider>): IProtoRpcHandler {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun unregisterProvider(provider: IServiceProvider): IProtoRpcHandler {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun requestedListener(methodName: String, method: Method, provider: IServiceProvider): RpcRequestedListener {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun detailedRequestedListener(methodName: String, method: Method, provider: IServiceProvider): RpcRequestedListener {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}