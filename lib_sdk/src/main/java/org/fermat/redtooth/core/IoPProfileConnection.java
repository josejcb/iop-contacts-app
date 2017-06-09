package org.fermat.redtooth.core;


import org.fermat.redtooth.crypto.CryptoBytes;
import org.fermat.redtooth.crypto.CryptoWrapper;
import org.fermat.redtooth.global.DeviceLocation;
import org.fermat.redtooth.global.GpsLocation;
import org.fermat.redtooth.locnet.Explorer;
import org.fermat.redtooth.locnet.NodeInfo;
import org.fermat.redtooth.profile_server.CantConnectException;
import org.fermat.redtooth.profile_server.CantSendMessageException;
import org.fermat.redtooth.profile_server.ProfileServerConfigurations;
import org.fermat.redtooth.profile_server.SslContextFactory;
import org.fermat.redtooth.profile_server.engine.app_services.AppService;
import org.fermat.redtooth.profile_server.engine.app_services.AppServiceMsg;
import org.fermat.redtooth.profile_server.engine.app_services.CallProfileAppService;
import org.fermat.redtooth.profile_server.engine.app_services.CallsListener;
import org.fermat.redtooth.profile_server.engine.listeners.EngineListener;
import org.fermat.redtooth.profile_server.engine.ProfSerEngine;
import org.fermat.redtooth.profile_server.engine.SearchProfilesQuery;
import org.fermat.redtooth.profile_server.engine.futures.BaseMsgFuture;
import org.fermat.redtooth.profile_server.engine.futures.MsgListenerFuture;
import org.fermat.redtooth.profile_server.engine.futures.SearchMessageFuture;
import org.fermat.redtooth.profile_server.engine.futures.SubsequentSearchMsgListenerFuture;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;
import org.fermat.redtooth.profile_server.model.KeyEd25519;
import org.fermat.redtooth.profile_server.model.ProfServerData;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.profile_server.protocol.IopProfileServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Created by mati on 08/05/17.
 *
 * Core class to manage a single profile connection with the redtooth
 *
 */

public class IoPProfileConnection implements CallsListener {

    private static final Logger logger = LoggerFactory.getLogger(IoPProfileConnection.class);

    /** Context wrapper */
    private IoPConnectContext contextWrapper;
    /** Profile cached */
    private Profile profileCache;
    /** profile server engine */
    private ProfSerEngine profSerEngine;
    /** profile server configurations */
    private ProfileServerConfigurations profileServerConfigurations;
    /** Crypto implmentation dependent on the platform */
    private CryptoWrapper cryptoWrapper;
    /** Ssl context factory */
    private SslContextFactory sslContextFactory;
    /** Location helper dependent on the platform */
    private DeviceLocation deviceLocation;
    /** Engine listener */
    private EngineListener profServerEngineListener;
    /** Open profile app service calls -> remote profile pk -> call in progress */
    private ConcurrentMap<String,CallProfileAppService> openCall = new ConcurrentHashMap<>();
    /**  */
    private ConcurrentMap<String,AppService> openServices = new ConcurrentHashMap<>();

    public IoPProfileConnection(IoPConnectContext contextWrapper, Profile profile, ProfileServerConfigurations profileServerConfigurations, CryptoWrapper cryptoWrapper, SslContextFactory sslContextFactory, DeviceLocation deviceLocation){
        this.contextWrapper = contextWrapper;
        this.profileServerConfigurations = profileServerConfigurations;
        this.cryptoWrapper = cryptoWrapper;
        this.sslContextFactory = sslContextFactory;
        this.profileCache = profile;
        this.deviceLocation = deviceLocation;
    }

    /**
     * Initialization method.
     *
     * @throws Exception
     */
    public void init(final MsgListenerFuture<Boolean> initFuture) throws ExecutionException, InterruptedException {
        // the flow is:
        // If the profile server main contract is active the connection with the profile server is clear.
        // If the hosting contract is null i have to search for a profile server using the LOC network and start the flow again
        //
        if (profileServerConfigurations.getMainProfileServerContract()!=null){
            //
            ProfServerData profServerData = profileServerConfigurations.getMainProfileServer();
            initProfileServer(profServerData);
        }else {
            // search in LOC for a profile server or use a trusted one from the user.
            // todo: here i have to do the LOC Network flow.
            // Sync explore profile servers around Argentina
            ProfServerData profServerData = null;
            if (false){
                Explorer explorer = new Explorer( NodeInfo.ServiceType.Profile, deviceLocation.getDeviceLocation(), 10000, 10 );
                FutureTask< List<NodeInfo> > task = new FutureTask<>(explorer);
                task.run();
                List<NodeInfo> resultNodes = task.get();
                // chose the first one - closest
                if (!resultNodes.isEmpty()) {
                    NodeInfo selectedNode = resultNodes.get(0);
                    profServerData = new ProfServerData(
                            CryptoBytes.fromHexToBytes(selectedNode.getNodeId()),
                            selectedNode.getContact().getAddress().getHostAddress(),
                            selectedNode.getContact().getPort(),
                            selectedNode.getLocation().getLatitude(),
                            selectedNode.getLocation().getLongitude()
                    );
                }
            }else {
                 profServerData = profileServerConfigurations.getMainProfileServer();
            }
            // Until Istvan push his client i will connect to a single local host server putting a hardcoded local server on the configurations.
            initProfileServer(profServerData);
            //profileServerConfigurations.setHost("localhost");
        }
        MsgListenerFuture<Boolean> initWrapper = new MsgListenerFuture<>();
        initFuture.setListener(new BaseMsgFuture.Listener<Boolean>() {
            @Override
            public void onAction(int messageId, Boolean object) {
                // not that this is initialized, init the app services
                registerApplicationServices();
                initFuture.onMessageReceive(messageId,object);
            }

            @Override
            public void onFail(int messageId, int status, String statusDetail) {
                initFuture.onMsgFail(messageId,status,statusDetail);
            }
        });
        profSerEngine.start(initWrapper);
    }

    public void init() throws Exception {
        init(null);
    }

    /**
     * Initialize the profile server
     * @throws Exception
     */
    private void initProfileServer(ProfServerData profServerData) {
        if (profServerData.getHost()!=null) {
            profSerEngine = new ProfSerEngine(
                    contextWrapper,
                    profileServerConfigurations,
                    profServerData,
                    profileCache,
                    cryptoWrapper,
                    sslContextFactory
            );
            profSerEngine.addEngineListener(profServerEngineListener);
            profSerEngine.setCallListener(this);
        }else {
            throw new IllegalStateException("Profile server not found, please set one first using LOC");
        }
    }

    public void stop() {
        profSerEngine.stop();
    }

    public void setProfServerEngineListener(EngineListener profServerEngineListener) {
        this.profServerEngineListener = profServerEngineListener;
    }

    public ProfileServerConfigurations getProfileServerConfigurations() {
        return profileServerConfigurations;
    }

    /**
     * Register the default app services
     */
    private void registerApplicationServices() {
        // registerConnect application services
        final Profile profile = profSerEngine.getProfNodeConnection().getProfile();
        for (final AppService service : profile.getApplicationServices()) {
            addApplicationService(service);
        }
    }


    public void searchProfileByName(String name, ProfSerMsgListener<List<IopProfileServer.ProfileQueryInformation>> listener) {
        profSerEngine.searchProfileByName(name,listener);
    }

    public SearchMessageFuture<List<IopProfileServer.ProfileQueryInformation>> searchProfiles(SearchProfilesQuery searchProfilesQuery) {
        SearchMessageFuture<List<IopProfileServer.ProfileQueryInformation>> future = new SearchMessageFuture<>(searchProfilesQuery);
        profSerEngine.searchProfiles(searchProfilesQuery,future);
        return future;
    }

    public SubsequentSearchMsgListenerFuture<List<IopProfileServer.ProfileQueryInformation>> searchSubsequentProfiles(SearchProfilesQuery searchProfilesQuery) {
        SubsequentSearchMsgListenerFuture future = new SubsequentSearchMsgListenerFuture(searchProfilesQuery);
        profSerEngine.searchSubsequentProfiles(searchProfilesQuery,future);
        return future;
    }

    public int updateProfile(byte[] version, String name, byte[] img, int latitude, int longitude, String extraData, ProfSerMsgListener msgListener) {
        profileCache.setName(name);
        profileServerConfigurations.setUsername(name);
        return profSerEngine.updateProfile(
                version,
                name,
                img,
                latitude,
                longitude,
                extraData,
                msgListener
        );
    }

    public int updateProfile(String name,byte[] img,String extraData,ProfSerMsgListener msgListener){
        return updateProfile(profileCache.getVersion(),name,img,0,0,extraData,msgListener);
    }

    public Profile getProfile() {
        return profileCache;
    }

    /**
     * Method to check if the library is ready to use
     * @return
     */
    public boolean isReady() {
        return profSerEngine.isClConnectionReady();
    }

    /**
     * Add the profile name
     * @param profileName
     */
    public void setProfileName(String profileName) {
        if (profileCache!=null)
            this.profileCache.setName(profileName);
        else {
            this.profileServerConfigurations.setUsername(profileName);
        }
    }

    public void setProfileType(String profileType) {
        if (profileCache!=null)
            this.profileCache.setType(profileType);
        this.profileServerConfigurations.setProfileType(profileType);
    }

    public void setProfileKeys(KeyEd25519 key) {
        if (profileCache!=null) {
            profileCache.setKey(key);
            profileServerConfigurations.saveUserKeys(key);
        }else {
            profileServerConfigurations.saveUserKeys(key);
        }

    }

    /**
     * Add more application services to an active profile
     * @param appService
     * @param appService
     */
    public void addApplicationService(AppService appService) {
        profileCache.addApplicationService(appService);
        profileServerConfigurations.saveProfile(profileCache);
        openServices.put(appService.getName(),appService);
        profSerEngine.addApplicationService(appService);
    }


    /**
     *
     *
     *
     * @param publicKey
     * @param msgProfFuture
     */
    public void getProfileInformation(String publicKey, MsgListenerFuture msgProfFuture) throws CantConnectException, CantSendMessageException {
        getProfileInformation(publicKey,false,false,false,msgProfFuture);
    }

    public void getProfileInformation(String publicKey, boolean includeApplicationServices, ProfSerMsgListener msgProfFuture) throws CantConnectException, CantSendMessageException {
        getProfileInformation(publicKey,false,false,includeApplicationServices,msgProfFuture);
    }

    public void getProfileInformation(String publicKey, boolean includeProfileImage , boolean includeThumbnailImage, boolean includeApplicationServices, ProfSerMsgListener msgProfFuture) throws CantConnectException, CantSendMessageException {
        profSerEngine.getProfileInformation(publicKey,includeProfileImage,includeThumbnailImage,includeApplicationServices,msgProfFuture);
    }

    /**
     * If this method is called is supposed that the service already have the ProfileInformation with the included application services
     *
     * @param remoteProfilePublicKey
     * @param appService
     * @param tryWithoutGetInfo -> if the redtooth knows the profile data there is no necesity to get the data again.
     */
    public void callProfileAppService(final String remoteProfilePublicKey, final String appService, boolean tryWithoutGetInfo, final ProfSerMsgListener<CallProfileAppService> profSerMsgListener) {
        logger.info("callProfileAppService from "+remoteProfilePublicKey+" using "+appService);
        final CallProfileAppService callProfileAppService = new CallProfileAppService(appService,profileCache.getHexPublicKey(),remoteProfilePublicKey,profSerEngine);
        try {
            if (!tryWithoutGetInfo) {
                callProfileAppService.setStatus(CallProfileAppService.Status.PENDING_AS_INFO);
                // first if the app doesn't have the profileInformation including the app services i have to request it.
                final MsgListenerFuture<IopProfileServer.GetProfileInformationResponse> getProfileInformationFuture = new MsgListenerFuture<>();
                getProfileInformationFuture.setListener(new BaseMsgFuture.Listener<IopProfileServer.GetProfileInformationResponse>() {
                    @Override
                    public void onAction(int messageId, IopProfileServer.GetProfileInformationResponse getProfileInformationResponse) {
                        logger.info("callProfileAppService getProfileInformation ok");
                        callProfileAppService.setStatus(CallProfileAppService.Status.AS_INFO);
                        try {
                            // todo: save this profile and it's services for future calls.
                            if (!getProfileInformationResponse.getIsOnline()) {
                                // remote profile not online.
                                // todo: launch notification and end the flow here
                                callProfileAppService.setStatus(CallProfileAppService.Status.CALL_FAIL);
                                callProfileAppService.setErrorStatus("Remote profile not online");
                                return;
                            }
                            boolean isServiceSupported = false;
                            for (String supportedAppService : getProfileInformationResponse.getApplicationServicesList()) {
                                if (supportedAppService.equals(appService)) {
                                    logger.info("callProfileAppService getProfileInformation -> profile support app service");
                                    isServiceSupported = true;
                                    break;
                                }
                            }
                            if (!isServiceSupported) {
                                // service not supported
                                // todo: launch notification and end the flow here
                                callProfileAppService.setStatus(CallProfileAppService.Status.CALL_FAIL);
                                callProfileAppService.setErrorStatus("Remote profile not accept service " + appService);
                                return;
                            }

                            // call profile
                            callProfileAppService(callProfileAppService,profSerMsgListener);

                        } catch (CantSendMessageException e) {
                            e.printStackTrace();
                            callProfileAppService.setStatus(CallProfileAppService.Status.CALL_FAIL);
                        } catch (CantConnectException e) {
                            e.printStackTrace();
                            callProfileAppService.setStatus(CallProfileAppService.Status.CALL_FAIL);
                        }
                    }

                    @Override
                    public void onFail(int messageId, int status, String statusDetail) {
                        // todo: launch notification..
                        logger.info("callProfileAppService getProfileInformation fail");
                        callProfileAppService.setStatus(CallProfileAppService.Status.CALL_FAIL);
                        callProfileAppService.setErrorStatus(statusDetail);

                    }
                });
                getProfileInformation(remoteProfilePublicKey, true, getProfileInformationFuture);
            }else {
                callProfileAppService(callProfileAppService,profSerMsgListener);
            }

        } catch (CantSendMessageException e) {
            e.printStackTrace();
            callProfileAppService.setStatus(CallProfileAppService.Status.CALL_FAIL);
        } catch (CantConnectException e) {
            e.printStackTrace();
            callProfileAppService.setStatus(CallProfileAppService.Status.CALL_FAIL);
        }
    }

    /**
     *
     * @param callProfileAppService
     * @throws CantConnectException
     * @throws CantSendMessageException
     */
    private void callProfileAppService(final CallProfileAppService callProfileAppService, final ProfSerMsgListener<CallProfileAppService> profSerMsgListener) throws CantConnectException, CantSendMessageException {
        // call profile
        logger.info("callProfileAppService call profile");
        callProfileAppService.setStatus(CallProfileAppService.Status.PENDING_CALL_AS);
        final MsgListenerFuture<IopProfileServer.CallIdentityApplicationServiceResponse> callProfileFuture = new MsgListenerFuture<>();
        callProfileFuture.setListener(new BaseMsgFuture.Listener<IopProfileServer.CallIdentityApplicationServiceResponse>() {
            @Override
            public void onAction(int messageId, IopProfileServer.CallIdentityApplicationServiceResponse appServiceResponse) {
                logger.info("callProfileAppService accepted");
                try {
                    callProfileAppService.setCallToken(appServiceResponse.getCallerToken().toByteArray());
                    openCall.put(CryptoBytes.toHexString(appServiceResponse.getCallerToken().toByteArray()),callProfileAppService);
                    // setup call app service
                    setupCallAppServiceInitMessage(callProfileAppService,profSerMsgListener);
                } catch (CantSendMessageException e) {
                    e.printStackTrace();
                    callProfileAppService.setStatus(CallProfileAppService.Status.CALL_FAIL);
                } catch (CantConnectException e) {
                    e.printStackTrace();
                    callProfileAppService.setStatus(CallProfileAppService.Status.CALL_FAIL);
                }
            }
            @Override
            public void onFail(int messageId, int status, String statusDetail) {
                logger.info("callProfileAppService rejected, "+statusDetail);
            }
        });
        profSerEngine.callProfileAppService(callProfileAppService.getRemotePubKey(), callProfileAppService.getAppService(), callProfileFuture);
    }


    @Override
    public void incomingCallNotification(int messageId, IopProfileServer.IncomingCallNotificationRequest message) {
        logger.info("incomingCallNotification");
        try {
            // todo: launch notification to accept the incoming call here.
            String remotePubKey = CryptoBytes.toHexString(message.getCallerPublicKey().toByteArray());
            String callToken = CryptoBytes.toHexString(message.getCalleeToken().toByteArray());
            final CallProfileAppService callProfileAppService = new CallProfileAppService(message.getServiceName(), profileCache.getHexPublicKey(), remotePubKey,profSerEngine);
            callProfileAppService.setCallToken(message.getCalleeToken().toByteArray());

            // accept every single call
            openCall.put(callToken, callProfileAppService);
            profSerEngine.acceptCall(messageId);

            // init setup call message
            setupCallAppServiceInitMessage(callProfileAppService, new ProfSerMsgListener<CallProfileAppService>() {
                @Override
                public void onMessageReceive(int messageId, CallProfileAppService message) {
                    // once everything is correct, launch notification
                    openServices.get(message.getAppService()).onNewCallReceived(callProfileAppService);
                }

                @Override
                public void onMsgFail(int messageId, int statusValue, String details) {
                    logger.info("setupCallAppServiceInitMessage init message fail, "+details);
                }

                @Override
                public String getMessageName() {
                    return "setupCallAppServiceInitMessage";
                }
            });
        } catch (CantSendMessageException e) {
            e.printStackTrace();
        } catch (CantConnectException e) {
            e.printStackTrace();
        }
    }

    private void setupCallAppServiceInitMessage(final CallProfileAppService callProfileAppService, final ProfSerMsgListener<CallProfileAppService> profSerMsgListener) throws CantConnectException, CantSendMessageException {
        callProfileAppService.setStatus(CallProfileAppService.Status.PENDING_INIT_MESSAGE);
        // send init message to setup the call
        MsgListenerFuture<IopProfileServer.ApplicationServiceSendMessageResponse> initMsgFuture = new MsgListenerFuture<>();
        initMsgFuture.setListener(new BaseMsgFuture.Listener<IopProfileServer.ApplicationServiceSendMessageResponse>() {
            @Override
            public void onAction(int messageId, IopProfileServer.ApplicationServiceSendMessageResponse object) {
                logger.info("callProfileAppService setup message accepted");
                callProfileAppService.setStatus(CallProfileAppService.Status.CALL_AS_ESTABLISH);
                profSerMsgListener.onMessageReceive(messageId,callProfileAppService);
            }
            @Override
            public void onFail(int messageId, int status, String statusDetail) {
                logger.info("callProfileAppService init message fail, "+statusDetail);
                profSerMsgListener.onMsgFail(messageId,status,statusDetail);
            }
        });
        profSerEngine.sendAppServiceMsg(callProfileAppService.getCallToken(), null, initMsgFuture);
    }

    @Override
    public void incomingAppServiceMessage(int messageId, AppServiceMsg message) {
        logger.info("incomingAppServiceMessage");
        //logger.info("msg arrived! -> "+message.getMsg());
        // todo: Como puedo saber a donde deberia ir este mensaje..
        // todo: una vez sabido a qué openCall vá, la open call debe tener un listener de los mensajes entrantes (quizás una queue) y debe ir respondiendo con el ReceiveMessageNotificationResponse
        // todo: para notificar al otro lado que todo llegó bien.
        if (openCall.containsKey(message.getCallTokenId())){
            // launch notification
            openCall.get(message.getCallTokenId()).onMessageReceived(message.getMsg());
            // now report the message received to the counter party
            try {
                profSerEngine.respondAppServiceReceiveMsg(message.getCallTokenId(),messageId);
            } catch (CantSendMessageException e) {
                e.printStackTrace();
                logger.warn("cant send responseAppServiceMsgReceived for msg id: "+message);
            } catch (CantConnectException e) {
                logger.warn("cant connect and send responseAppServiceMsgReceived for msg id: "+message);
                e.printStackTrace();
            }
        }else {
            logger.warn("incomingAppServiceMessage -> openCall not found");
        }
    }

    public CallProfileAppService getActiveAppCallService(String remoteProfileKey){
        for (CallProfileAppService callProfileAppService : openCall.values()) {
            if (callProfileAppService.getRemotePubKey().equals(remoteProfileKey)){
                return callProfileAppService;
            }
        }
        return null;
    }

}