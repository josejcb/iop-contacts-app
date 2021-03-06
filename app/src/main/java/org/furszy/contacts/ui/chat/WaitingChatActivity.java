package org.furszy.contacts.ui.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.libertaria.world.profile_server.ProfileInformation;
import org.libertaria.world.profile_server.client.AppServiceCallNotAvailableException;
import org.libertaria.world.profile_server.engine.futures.BaseMsgFuture;
import org.libertaria.world.profile_server.engine.futures.MsgListenerFuture;
import org.furszy.contacts.App;
import org.furszy.contacts.BaseActivity;
import org.furszy.contacts.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;

import static world.libertaria.shared.library.services.chat.ChatIntentsConstants.ACTION_ON_CHAT_DISCONNECTED;
import static world.libertaria.shared.library.services.chat.ChatIntentsConstants.EXTRA_INTENT_DETAIL;
import static org.furszy.contacts.App.INTENT_CHAT_REFUSED_BROADCAST;

/**
 * Created by Neoperol on 7/3/17.
 */

public class WaitingChatActivity extends BaseActivity implements View.OnClickListener {

    public static final String REMOTE_PROFILE_PUB_KEY = "remote_prof_pub";
    public static final String IS_CALLING = "is_calling";

    /** Call timeout in minutes */
    private static final long CALL_TIMEOUT = 1;

    private View root;
    private TextView txt_name;
    private CircleImageView img_profile;
    private ProgressBar progressBar;
    private TextView txt_title;
    private ProfileInformation profileInformation;
    private String remotePk;
    private boolean isCalling;
    private ExecutorService executors;

    private ScheduledExecutorService scheduledCallTimeout;

    private BroadcastReceiver chatReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(App.INTENT_CHAT_ACCEPTED_BROADCAST)){
                Intent intent1 = new Intent(WaitingChatActivity.this,ChatActivity.class);
                intent1.putExtra(REMOTE_PROFILE_PUB_KEY,intent.getStringExtra(REMOTE_PROFILE_PUB_KEY));
                startActivity(intent1);
                finish();
            }else if(action.equals(INTENT_CHAT_REFUSED_BROADCAST)){
                Toast.makeText(WaitingChatActivity.this,"Chat refused.",Toast.LENGTH_LONG).show();
                onBackPressed();
            }else if (action.equals(ACTION_ON_CHAT_DISCONNECTED)){
                String remotePubKey = intent.getStringExtra(REMOTE_PROFILE_PUB_KEY);
                String reason = intent.getStringExtra(EXTRA_INTENT_DETAIL);
                if (remotePk.equals(remotePubKey)){
                    Toast.makeText(WaitingChatActivity.this,"Chat disconnected",Toast.LENGTH_LONG).show();
                    onBackPressed();
                }
            }
        }
    };

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        super.onCreateView(savedInstanceState, container);
        root = getLayoutInflater().inflate(R.layout.incoming_message, container);
        img_profile = (CircleImageView) root.findViewById(R.id.profile_image);
        txt_name = (TextView) root.findViewById(R.id.txt_name);
        progressBar = (ProgressBar) root.findViewById(R.id.progressBar);
        txt_title = (TextView) root.findViewById(R.id.txt_title);
        remotePk = getIntent().getStringExtra(REMOTE_PROFILE_PUB_KEY);
        if (remotePk==null) throw new IllegalStateException("remote profile key null");
        isCalling = getIntent().hasExtra(IS_CALLING);
        if (isCalling){
            root.findViewById(R.id.single_cancel_container).setVisibility(View.VISIBLE);
            root.findViewById(R.id.btn_cancel_chat_alone).setOnClickListener(this);
            root.findViewById(R.id.container_btns).setVisibility(View.GONE);
            // prepare timer..
            scheduleCallTimeout();
        }else {
            root.findViewById(R.id.single_cancel_container).setVisibility(View.GONE);
            root.findViewById(R.id.btn_open_chat).setOnClickListener(this);
            root.findViewById(R.id.btn_cancel_chat).setOnClickListener(this);
        }
    }

    private void scheduleCallTimeout() {
        scheduledCallTimeout = Executors.newSingleThreadScheduledExecutor();
        scheduledCallTimeout.schedule(new Runnable() {
            @Override
            public void run() {
                executors.submit(new Runnable() {
                    @Override
                    public void run() {
                        refuseChat();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(WaitingChatActivity.this,profileInformation.getName()+" doesn't answer..",Toast.LENGTH_LONG).show();
                                onBackPressed();
                            }
                        });
                    }
                });
            }
        },CALL_TIMEOUT, TimeUnit.MINUTES);
    }

    @Override
    protected void onResume() {
        super.onResume();
        localBroadcastManager.registerReceiver(chatReceiver,new IntentFilter(App.INTENT_CHAT_ACCEPTED_BROADCAST));
        localBroadcastManager.registerReceiver(chatReceiver,new IntentFilter(App.INTENT_CHAT_REFUSED_BROADCAST));
        localBroadcastManager.registerReceiver(chatReceiver,new IntentFilter(ACTION_ON_CHAT_DISCONNECTED));
        if (executors==null)
            executors = Executors.newSingleThreadExecutor();
        executors.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    if (selectedProfPubKey!=null && remotePk!=null) {
                        if (!chatModule.isChatActive(selectedProfPubKey, remotePk)) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(WaitingChatActivity.this, "Chat not active anymore", Toast.LENGTH_LONG).show();
                                    onBackPressed();
                                }
                            });

                        }
                    }else
                        Log.e("WaitingChat","profile pub key is null");
                }catch (Exception e){
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(WaitingChatActivity.this, "Chat not active anymore", Toast.LENGTH_LONG).show();
                            onBackPressed();
                        }
                    });

                }
            }
        });

        profileInformation = profilesModule.getKnownProfile(selectedProfPubKey,remotePk);
        txt_name.setText(profileInformation.getName());
        if (profileInformation.getImg()!=null){
            Bitmap bitmap = BitmapFactory.decodeByteArray(profileInformation.getImg(),0,profileInformation.getImg().length);
            img_profile.setImageBitmap(bitmap);
        }
        if(isCalling){
            txt_title.setText("Waiting for "+profileInformation.getName()+" response...");
        }else {
            txt_title.setText("Call from "+profileInformation.getName());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        localBroadcastManager.unregisterReceiver(chatReceiver);
        if (executors!=null){
            executors.shutdownNow();
            executors = null;
        }
        if (scheduledCallTimeout!=null){
            scheduledCallTimeout.shutdownNow();
        }
        finish();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_open_chat){
            progressBar.setVisibility(View.VISIBLE);
            acceptChatRequest();
        }else if (id == R.id.btn_cancel_chat || id == R.id.btn_cancel_chat_alone){
            // here i have to close the connection refusing the call..
            executors.submit(new Runnable() {
                @Override
                public void run() {
                    refuseChat();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onBackPressed();
                        }
                    });
                }
            });
        }
    }

    private void refuseChat(){
        try{
            chatModule.refuseChatRequest(selectedProfPubKey,profileInformation.getHexPublicKey());
        }catch (Exception e){
            e.printStackTrace();
            // do nothing..
        }
    }

    private void acceptChatRequest() {
        executors.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    // send the ok to the other side
                    MsgListenerFuture<Boolean> future = new MsgListenerFuture<>();
                    future.setListener(new BaseMsgFuture.Listener<Boolean>() {
                        @Override
                        public void onAction(int messageId, Boolean object) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Intent intent = new Intent(WaitingChatActivity.this, ChatActivity.class);
                                    intent.putExtra(REMOTE_PROFILE_PUB_KEY, profileInformation.getHexPublicKey());
                                    startActivity(intent);
                                    finish();
                                }
                            });
                        }

                        @Override
                        public void onFail(int messageId, int status, final String statusDetail) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(WaitingChatActivity.this, "Chat connection fail\n" + statusDetail, Toast.LENGTH_LONG).show();
                                    onBackPressed();
                                }
                            });
                        }
                    });
                    chatModule.acceptChatRequest(selectedProfPubKey,profileInformation.getHexPublicKey(), future);
                } catch (AppServiceCallNotAvailableException e){
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(WaitingChatActivity.this,"Connection is not longer available",Toast.LENGTH_LONG).show();
                            onBackPressed();
                        }
                    });

                } catch (final Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(WaitingChatActivity.this,"Chat connection fail\n"+e.getMessage(),Toast.LENGTH_LONG).show();
                            onBackPressed();
                        }
                    });
                }
            }
        });
    }
}
