package com.moquality.android;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.moquality.android.TestConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

class SocketIOHandlerThread extends HandlerThread {

    private Socket socket;
    private Handler mWorkerHandler;

    private static final String TAG = SocketIOHandlerThread.class.getSimpleName();
    private Callback mCallback;
    private boolean isAlive = true;
    private boolean isConnected = false;
    private String deviceId;
    private int botMode = TestConstants.DEFAULT_MODE;

    public interface Callback {
        void onSocketTaskCompleted(int taskId);
        void onSocketEventReceived(String eventName, String method, List<Class> classArgs, List<String> stringArgs);
    }

    SocketIOHandlerThread(Callback callback, String deviceId) {
        super(TAG);
        mCallback = callback;
        this.deviceId = deviceId;
    }

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.i(TAG, "Android bot connected "+ deviceId);
            isConnected = true;
            JSONObject data = new JSONObject();
            try {
                data.put("deviceId", deviceId);
            } catch (JSONException e) {
                Log.i("EVENT_CONNECT", "JSON error occurred");
            }
            socket.emit("status", data.toString());
        }
    };

    private Emitter.Listener onConnecting = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.i("SOCKET_EVENT", d("connecting"));
        }
    };

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.i("SOCKET_EVENT", "disconnected");
            isConnected = false;
        }
    };

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.i("SOCKET_EVENT", d("connect error", args));
        }
    };

    private Emitter.Listener onMessage = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.i("SOCKET_EVENT", d("message"));
        }
    };

    private Emitter.Listener onPing = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.i("SOCKET_EVENT", "ping");
        }
    };

    private Emitter.Listener onPong = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.i("SOCKET_EVENT", "pong");
        }
    };

    private Emitter.Listener onReconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.i("SOCKET_EVENT", d("reconnecting"));
        }
    };

    private Emitter.Listener onReconnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.i("SOCKET_EVENT", d("reconnect error"));
        }
    };

    private Emitter.Listener onReconnecting = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.i("SOCKET_EVENT", d("reconnecting"));
        }
    };

    private Emitter.Listener onStatus = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.i("SOCKET_EVENT", "received status command");
        }
    };

    private Emitter.Listener onReturn = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.i("SOCKET_EVENT", "received return command");
        }
    };

    private Emitter.Listener onCall = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.i("SOCKET_EVENT", "received call command");
            String command = args[0].toString();
            //if (deviceId.equalsIgnoreCase("device1")) {
            //    botMode = TestConstants.REFLECT_MODE;
            //}
            Log.i("call", command + " bot mode = " + botMode);

            try {
                JSONObject obj = new JSONObject(command);

                String targetDeviceId = obj.getString("deviceId");
                if(deviceId.equals(targetDeviceId)) {

                    String cmd = obj.get("cmd").toString();
                    JSONArray cmdArgs = obj.getJSONArray("args");

                    List<Class> classArgs = new ArrayList<>();
                    List<String> stringArgs = new ArrayList<>();
                    for (int i = 0; i < cmdArgs.length(); i++) {
                        classArgs.add(String.class);
                        stringArgs.add(cmdArgs.getString(i));
                    }

                    Log.i(TAG, "cmdArgs" + cmdArgs.toString());
                    Log.i(TAG, "Parsed classArgs " + classArgs.toString());
                    Log.i(TAG, "Parsed stringArgs " + stringArgs.toString());

                    JSONObject msg = new JSONObject();
                    msg.put("id", obj.getInt("id"));
                    msg.put("type", obj.getString("type"));
                    msg.put("result", "OK");

                    mCallback.onSocketEventReceived(TestConstants.SOCKET_EVENT_CALL, cmd, classArgs, stringArgs);
                    socket.emit("return", msg.toString());
                } else {
                    if(botMode == TestConstants.REFLECT_MODE ){
                        Class[] cArg = new Class[1];
                        cArg[0] = String.class;
                        JSONArray cmdArgs = obj.getJSONArray("args");
                        List<Class> classArgs = new ArrayList<>();
                        List<String> stringArgs = new ArrayList<>();
                        for (int i = 0; i < cmdArgs.length(); i++) {
                            classArgs.add(String.class);
                            stringArgs.add(cmdArgs.getString(i));
                        }

                        String cmd = "reflect";

                        JSONObject msg = new JSONObject();
                        msg.put("id", obj.getInt("id"));
                        msg.put("type", obj.getString("type"));
                        msg.put("result", "OK");

                        Log.i(TAG, "REFLECTING MESSAGE ***********************");
                        //msg.put("return", "{}");
                        mCallback.onSocketEventReceived(TestConstants.SOCKET_EVENT_CALL, cmd, classArgs, stringArgs);
                        socket.emit("return", msg.toString());
                    } else {

                        Log.i(TAG, "Ignored message id:" + obj.getInt("id"));
                    }
                }
            } catch (JSONException e) {
                Log.i("EVENT_CONNECT", "JSON error occured");
            }
        }
    };

    void queueTask(int taskId) {
        Log.i(TAG, "Task added to the queue");
        mWorkerHandler.obtainMessage(taskId)
                .sendToTarget();
    }

    void prepareHandler() {
        mWorkerHandler = new Handler(getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handleRequest(msg.what);
                return true;
            }
        });
    }

    boolean isThreadAlive(){return isAlive;}

    private void handleRequest(final int taskId) {
        switch(taskId){
            case TestConstants.SOCKET_IO_START:
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e){
                    Log.e("SOCKET_IO_START", "Thread interrupted");
                }

                Log.i("SOCKET_EVENT", "STARTING");
                isAlive = true;
                try {
                    IO.Options opts = new IO.Options();
                    opts.forceNew = true;
                    socket = IO.socket(TestConstants.CHAT_SERVER_URL + TestConstants.SOCKET_IO_NAMESPACE, opts );
                    socket.connect();
                } catch (URISyntaxException e) {
                    Log.i("SOCKET ERROR", String.format("URI %s is not valid", TestConstants.CHAT_SERVER_URL));
                }

                socket.on(Socket.EVENT_CONNECT, onConnect);
                socket.on(TestConstants.SOCKET_EVENT_STATUS, onStatus);
                socket.on(TestConstants.SOCKET_EVENT_RETURN, onReturn);
                socket.on(TestConstants.SOCKET_EVENT_CALL, onCall);
                socket.on(Socket.EVENT_DISCONNECT, onDisconnect);
                socket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
                socket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
                socket.on(Socket.EVENT_CONNECTING, onConnecting);
                socket.on(Socket.EVENT_MESSAGE, onMessage);
                socket.on(Socket.EVENT_PING, onPing);
                socket.on(Socket.EVENT_PONG, onPong);
                socket.on(Socket.EVENT_RECONNECT, onReconnect);
                socket.on(Socket.EVENT_RECONNECT_ATTEMPT, onReconnectError);
                socket.on(Socket.EVENT_RECONNECTING, onReconnecting);
                socket.on(Socket.EVENT_RECONNECT_ERROR,onReconnectError);
                socket.on(Socket.EVENT_RECONNECT_FAILED, onReconnectError);
                socket.on(Socket.EVENT_RECONNECT_ATTEMPT, onReconnect);

                break;
            case TestConstants.DEFAULT_MODE:
                Log.i(TAG, "DEFAULT bot mode now set");
                botMode = TestConstants.DEFAULT_MODE;

                break;
            case TestConstants.REFLECT_MODE:
                botMode = TestConstants.REFLECT_MODE;
                Log.i(TAG, "REFLECT bot mode now set");

                break;
            case TestConstants.SOCKET_IO_STOP:
                try {
                    Thread.sleep(1000);
                    socket.disconnect();
                    isAlive= false;
                    isConnected=false;
                    socket.off(Socket.EVENT_CONNECT, onConnect);
                    socket.off(TestConstants.SOCKET_EVENT_STATUS, onStatus);
                    socket.off(TestConstants.SOCKET_EVENT_RETURN, onReturn);
                    socket.off(TestConstants.SOCKET_EVENT_CALL, onCall);
                    socket.off(Socket.EVENT_DISCONNECT, onDisconnect);
                    socket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
                    socket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
                    socket.off(Socket.EVENT_CONNECTING, onConnecting);
                    socket.off(Socket.EVENT_MESSAGE, onMessage);
                    socket.off(Socket.EVENT_PING, onPing);
                    socket.off(Socket.EVENT_PONG, onPong);
                    socket.off(Socket.EVENT_RECONNECT, onReconnect);
                    socket.off(Socket.EVENT_RECONNECT_ATTEMPT, onReconnectError);
                    socket.off(Socket.EVENT_RECONNECTING, onReconnecting);
                    socket.off(Socket.EVENT_RECONNECT_ERROR,onReconnectError);
                    socket.off(Socket.EVENT_RECONNECT_FAILED, onReconnectError);
                    socket.off(Socket.EVENT_RECONNECT_ATTEMPT, onReconnect);
                } catch (InterruptedException e) {
                    Log.i("SOCKET_IO_STOP", "Thread interrupted");
                }

                break;
            default:
                break;
        }
    }

    private String d(String msg, Object... args){
        StringBuilder sb = new StringBuilder();
        sb.append(msg);
        sb.append("\n");
        sb.append(args.getClass().getName());
        sb.append("-");
        sb.append(args);
        sb.append("\n");
        for(int i=0; i<args.length;i++) {
            sb.append(" - ");
            sb.append(args[i]);
            sb.append("\n");
        }
        return sb.toString();
    }
}
