package org.furszy.contacts.ui.home.requests;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.furszy.contacts.R;
import org.furszy.contacts.app_base.BaseAppRecyclerFragment;
import org.furszy.contacts.ui.home.HomeActivity;
import org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener;
import org.libertaria.world.profiles_manager.PairingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import tech.furszy.ui.lib.base.adapter.BaseAdapter;

public class RequestsFragment extends BaseAppRecyclerFragment<PairingRequest> {

    private static final Logger log = LoggerFactory.getLogger(RequestsFragment.class);

    private AtomicBoolean acceptanceFlag = new AtomicBoolean();


    public RequestsFragment() {
        // Required empty public constructor
    }
 
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
 
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = super.onCreateView(inflater,container,savedInstanceState);
        setEmptyText(getResources().getString(R.string.empty_request));
        setEmptyTextColor(Color.parseColor("#4d4d4d"));
        setEmptyView(R.drawable.img_request_empty);
        return root;
    }

    @Override
    protected List<PairingRequest> onLoading() {
        try {
            if (pairingModule!=null)
                return pairingModule.getPairingRequests(selectedProfilePubKey);
            else {
                loadBasics();
                TimeUnit.SECONDS.sleep(1);
                onLoading();
            }
        }catch (Exception e){
            log.info("onLoading",e);
        }
        return null;
    }

    @Override
    protected BaseAdapter initAdapter() {
        RequestAdapter profileAdapter = new RequestAdapter(getActivity(), new RequestAdapter.RequestListener() {
            @Override
            public void onAcceptRequest(final PairingRequest pairingRequest) {
                if (acceptanceFlag.compareAndSet(false,true)) {
                    swipeRefreshLayout.setRefreshing(true);
                    Toast.makeText(getActivity(), "Sending acceptance..\nplease wait some seconds..", Toast.LENGTH_SHORT).show();
                    executor.submit(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                pairingModule.acceptPairingProfile(pairingRequest,new ProfSerMsgListener<Boolean>(){

                                    @Override
                                    public void onMessageReceive(int messageId, Boolean message) {
                                        acceptanceFlag.set(false);
                                        loadRunnable.run();
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                swipeRefreshLayout.setRefreshing(false);
                                            }
                                        });
                                        ((HomeActivity)getActivity()).refreshContacts();
                                    }

                                    @Override
                                    public void onMsgFail(int messageId, int statusValue, final String details) {
                                        acceptanceFlag.set(false);
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                swipeRefreshLayout.setRefreshing(false);
                                                Toast.makeText(getActivity(), "Accept pairing fail\n" + details, Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    }

                                    @Override
                                    public String getMessageName() {
                                        return "acceptance response";
                                    }
                                });
                            } catch (final Exception e) {
                                // todo: show this exception..
                                acceptanceFlag.set(false);
                                e.printStackTrace();
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        swipeRefreshLayout.setRefreshing(false);
                                        Toast.makeText(getActivity(), "Accept pairing fail\n" + e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                    });
                }else {
                    Toast.makeText(getActivity(),"Sending an acceptance, please wait some time before send another one",Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelRequest(PairingRequest pairingRequest) {
                pairingModule.cancelPairingRequest(pairingRequest, true);
                Toast.makeText(getActivity(),"Connection cancelled..",Toast.LENGTH_SHORT).show();
                refresh();
            }
        });
        return profileAdapter;
    }

}