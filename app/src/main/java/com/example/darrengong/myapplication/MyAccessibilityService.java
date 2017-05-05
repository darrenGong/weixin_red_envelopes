package com.example.darrengong.myapplication;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Contacts;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.Console;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by darren.gong on 2017/3/26.
 */

public class MyAccessibilityService extends AccessibilityService {
    private static final String TAG = MyAccessibilityService.class.getSimpleName();
    private static final int REQUEST_WAKE_LOCK = 100;
    private static final int MSG_BACK_ONCE = 0;
    private static final int MSG_BACK_HOME = 1;
    private static final String LUCKY_MONEY_RECEIVE_UI = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI";
    private static final String LUCKY_MONEY_DETAIL_UI = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI";
    private static final String OPEN_LUCKY_MONEY_BUTTON_ID = "com.tencent.mm:id/bi3";


    private boolean hasNotify = false;
    private boolean hasLuckyMoney = true;

    private static Method mReflectScreenState;
    private PowerManager.WakeLock mWakelock;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        Log.d("Start", "onAccessibilityEvent: -----------------start-----------------");
        int eventType = accessibilityEvent.getEventType();
        Log.d("PackageName", "onAccessibilityEvent() called with: accessibilityEvent = [" + accessibilityEvent.getPackageName() + "]");
        Log.d("Source", "onAccessibilityEvent: " + accessibilityEvent.getSource());
        Log.d("ClassName", "onAccessibilityEvent: " + accessibilityEvent.getClassName());
        Log.d("EventType", "onAccessibilityEvent - event type(int): " + eventType);

        switch (eventType) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED: //通知栏事件
                Log.d(TAG, "onAccessibilityEvent: TYPE_NOTIFICATION_STATE_CHANGED");
                openAppByNotification(accessibilityEvent);
                hasNotify = true;
                break;
           // case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED://窗体状态改变
            //    Log.d("EventType", "event type:TYPE_WINDOW_STATE_CHANGED");
             //   break;
            case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED://View获取到焦点
                Log.d("EventType", "event type:TYPE_VIEW_ACCESSIBILITY_FOCUSED");
                break;
            case AccessibilityEvent.TYPE_GESTURE_DETECTION_START:
                Log.d("EventType", "event type:TYPE_VIEW_ACCESSIBILITY_FOCUSED");
                break;
            case AccessibilityEvent.TYPE_GESTURE_DETECTION_END:
                Log.d("EventType", "event type:TYPE_GESTURE_DETECTION_END");
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                Log.d("EventType", "event type:TYPE_WINDOW_CONTENT_CHANGED");
                break;
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                Log.d("EventType", "event type:TYPE_VIEW_CLICKED");
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                Log.d("EventType", "event type:TYPE_VIEW_TEXT_CHANGED");
                break;
            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                Log.d("EventType", "event type:TYPE_VIEW_SCROLLED");
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED:
                Log.d("EventType", "event type:TYPE_VIEW_TEXT_SELECTION_CHANGED");
                break;
            default:
                Log.d(TAG, "Default EventType:" + eventType);
                defaultAction(accessibilityEvent);
                break;
        }

        for (CharSequence txt : accessibilityEvent.getText()) {
            Log.d("text: " + txt, "onAccessibilityEvent");
        }

        Log.d("Finish Event", "onAccessibilityEvent - Event type:  " +  accessibilityEvent.getEventType());
    }

    @Override
    public void onInterrupt() {

    }

    /**
     *
     *  default action
     * */
    private void defaultAction(AccessibilityEvent event) {
        Log.i(TAG, "Default Action");
        if(hasNotify) {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            clickLuckyMoney(rootNode); // 点击红包

            String className = event.getClassName().toString();
            Log.d(TAG, "defaultAction: className: " + className);
            if (className.equals(LUCKY_MONEY_RECEIVE_UI)) { //红包接收界面
                if(!openLuckyMoney()) { // 如果红包被抢光了，就返回主界面
                    backToHome();
                    hasNotify = false;
                }
                hasLuckyMoney = true;
            } else if (className.equals(LUCKY_MONEY_DETAIL_UI)) { // 抢到红包
                backToHome();
                hasNotify = false;
                hasLuckyMoney = true;
            } else { // 处理没红包的情况，直接返回主界面
                if(!hasLuckyMoney) {
                    handler.sendEmptyMessage(MSG_BACK_ONCE);
                    hasLuckyMoney = true;   // 防止后退多次
                }
            }
        }
    }

    /**
     * 搜索并点击红包
     */
    private void clickLuckyMoney(AccessibilityNodeInfo rootNode) {
        if(rootNode != null) {
            int count = rootNode.getChildCount();
            for (int i = count - 1; i >= 0; i--) {  // 倒序查找最新的红包
                AccessibilityNodeInfo node = rootNode.getChild(i);
                if (node == null)
                    continue;

                CharSequence text = node.getText();

                Log.d(TAG, "clickLuckyMoney: " + text);
                if (text != null && text.toString().equals("领取红包")) {
                    AccessibilityNodeInfo parent = node.getParent();
                    while (parent != null) {
                        if (parent.isClickable()) {
                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            Log.d(TAG, "clickLuckyMoney: start click");
                            break;
                        }
                        parent = parent.getParent();
                    }
                }

                clickLuckyMoney(node);
            }
        }
    }

    /**
     * 打开红包
     */
    private boolean openLuckyMoney() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        Log.d(TAG, "openLuckyMoney: " + rootNode.toString());
        if(rootNode != null) {
            List<AccessibilityNodeInfo> nodes =
                    rootNode.findAccessibilityNodeInfosByViewId(OPEN_LUCKY_MONEY_BUTTON_ID);
            Log.d(TAG, "openLuckyMoney: nodes length: " + nodes.size());
            for(AccessibilityNodeInfo node : nodes) {
                if(node.isClickable()) {
                    Log.i(TAG, "open LuckyMoney");
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return true;
                }
            }
        }

        return false;
    }

    /***
     *
     *  open money the second
     *
     */
    private boolean OpenMoneySecondWay() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        Log.d(TAG, "openLuckyMoney: " + rootNode.toString());
        if(rootNode != null) {
            List<AccessibilityNodeInfo> infoKais = rootNode.findAccessibilityNodeInfosByText("给你发了一个红包");
            Log.d(TAG, "OpenMoneySecondWay: send a luck money, size: " + infoKais.size());
            if (infoKais != null && infoKais.size() > 0) {
                AccessibilityNodeInfo accessibilityNodeInfo = infoKais.get(infoKais.size() - 1);
                int size = accessibilityNodeInfo.getParent().getChildCount();
                Log.d(TAG, "OpenMoneySecondWay  size -->" + size);
            //for (AccessibilityNodeInfo info : infoKais) {
            //    info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            //}
                for (int i = 0; i < accessibilityNodeInfo.getParent().getChildCount(); i++) {
                    boolean isClick = accessibilityNodeInfo.getParent().getChild(i).performAction(
                            AccessibilityNodeInfo.ACTION_CLICK);
                    Log.d(TAG, "OpenMoneySecondWay: isClick: " + isClick);
                    if (isClick) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void backToHome() {
        if(handler.hasMessages(MSG_BACK_HOME)) {
            handler.removeMessages(MSG_BACK_HOME);
        }
        handler.sendEmptyMessage(MSG_BACK_HOME);
    }

    /**
     *  打开屏幕
     * */
    private void openScreen() {
        boolean isScreenOn = false;
        try {
            mReflectScreenState = PowerManager.class.getMethod("isScreenOn", new Class[] {});
            PowerManager pm = (PowerManager) getSystemService(Activity.POWER_SERVICE);
            isScreenOn= (Boolean) mReflectScreenState.invoke(pm);
            mWakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "target");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!isScreenOn) {
            Log.d(TAG, "onAccessibilityEvent: Screen is closed");
            // mWakelock.acquire();
        } else {
            Log.d(TAG, "onAccessibilityEvent: Screen is not closed");
        }
    }

    /**
     * 打开微信
     * @param event 事件
     */
    private void openAppByNotification(AccessibilityEvent event) {
        if (event.getParcelableData() != null  && event.getParcelableData() instanceof Notification) {
            Notification notification = (Notification) event.getParcelableData();
            try {
                PendingIntent pendingIntent = notification.contentIntent;
                pendingIntent.send();

                Log.d(TAG, "openAppByNotification: Open");
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == MSG_BACK_HOME) {
                performGlobalAction(GLOBAL_ACTION_BACK);
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        performGlobalAction(GLOBAL_ACTION_BACK);
                        hasLuckyMoney = false;
                    }
                }, 1500);
            } else if(msg.what == MSG_BACK_ONCE) {
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "click back");
                        performGlobalAction(GLOBAL_ACTION_BACK);
                        hasLuckyMoney = false;
                        hasNotify = false;
                    }
                }, 1500);
            }
        }
    };
}
