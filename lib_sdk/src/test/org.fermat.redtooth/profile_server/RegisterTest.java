/*
package org.libertaria.world.profile_server;

import org.libertaria.world.core.RedtoothContext;
import org.libertaria.world.core.RedtoothProfileConnection;
import CryptoWrapperJava;
import CryptoWrapper;
import MsgListenerFuture;
import org.libertaria.world.profile_server.utils.ProfileConfigurationImp;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

*
 * Created by furszy on 5/23/17.



public class RegisterTest {

    RedtoothContext redtoothContext = new RedtoothContext() {
        @Override
        public ProfileServerConfigurations createProfSerConfig() {
            return new ProfileConfigurationImp();
        }
    };
    CryptoWrapper cryptoWrapper = new CryptoWrapperJava();
    SslContextFactory sslContextFactory = new org.libertaria.world.profile_server.utils.SslContextFactory();

    @Test
    public void goodRegisterProfileTest() throws Exception {
        ProfileConfigurationImp conf = new ProfileConfigurationImp();
        RedtoothProfileConnection redtoothProfileConnection = new RedtoothProfileConnection(redtoothContext,conf,cryptoWrapper,sslContextFactory);
        redtoothProfileConnection.setProfileName("Matias");
        redtoothProfileConnection.setProfileType("registerTest");
        redtoothProfileConnection.init();
        waitUntilProfileServerIsConnected(redtoothProfileConnection);
        assert redtoothProfileConnection.isReady();
        redtoothProfileConnection.stop();
    }

    @Test
    public void BadProfileTypeRegisterProfileTest() throws Exception {
        ProfileConfigurationImp conf = new ProfileConfigurationImp();
        // profile type hardcoded null
        conf.setProfileType(null);
        RedtoothProfileConnection redtoothProfileConnection = new RedtoothProfileConnection(redtoothContext,conf,cryptoWrapper,sslContextFactory);
        redtoothProfileConnection.setProfileName("Matias");
        MsgListenerFuture<Boolean> initFuture = new MsgListenerFuture<>();
        redtoothProfileConnection.init(initFuture);
        initFuture.get();
        assert !redtoothProfileConnection.isReady() && initFuture.getStatus()==400;
        redtoothProfileConnection.stop();
    }





    private void waitUntilProfileServerIsConnected(RedtoothProfileConnection redtoothProfileConnection) {
        while (!redtoothProfileConnection.isReady()){
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
*/
