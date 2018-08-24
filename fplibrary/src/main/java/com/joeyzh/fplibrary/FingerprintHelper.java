package com.joeyzh.fplibrary;

import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.util.Log;

/**
 * Created by Joey on 2018/8/22.
 */
@TargetApi(Build.VERSION_CODES.M)
public class FingerprintHelper extends FingerprintManagerCompat.AuthenticationCallback {

    private FingerprintManagerCompat mFingerprintManager;
    private CryptoObjectCreator cryptoObjectCreator;
    private FingerprintHelper helper;
    private FingerprintManagerCompat.CryptoObject mCryptoObject;
    private Handler fpHandler;
    private boolean mSelfCancled = true;
    private CancellationSignal mCancellationSignal;
    private Context mContext;
    private String TAG = getClass().getCanonicalName();


    public FingerprintHelper(Context context) {
        mContext = context;
        mFingerprintManager = FingerprintManagerCompat.from(context);
        cryptoObjectCreator = new CryptoObjectCreator(new CryptoObjectCreator.ICryptoObjectCreateListener() {
            @Override
            public void onDataPrepared(FingerprintManagerCompat.CryptoObject cryptoObject) {
                mCryptoObject = cryptoObject;
            }
        });
        fpHandler = new Handler(Looper.getMainLooper());
    }


    /**
     * Determine if fingerprint hardware is present and functional.
     *
     * @return true if hardware is present and functional, false otherwise.
     */
    public boolean isHardwareDetected() {
        return mFingerprintManager.isHardwareDetected();
    }

    /**
     * @return
     */
    public boolean hasEnrolledFingerprints() {
        return mFingerprintManager.hasEnrolledFingerprints();
    }

    /**
     * 判断手机是否开启了密码识别功能
     *
     * @param context
     * @return
     */
    public boolean isKeyguardSecure(Context context) {
        KeyguardManager keyguardManager = context.getSystemService(KeyguardManager.class);
        return keyguardManager.isKeyguardSecure();

    }

    public void startListening() {
        // 不支持指纹识别
        if (!isHardwareDetected() || !hasEnrolledFingerprints()) {
            return;
        }
        // 秘钥初始化失败
        if (mCryptoObject == null) {
            return;
        }
        mCancellationSignal = new CancellationSignal();
        mSelfCancled = false;
        mFingerprintManager
                .authenticate(mCryptoObject, 0, mCancellationSignal, this, fpHandler);

    }

    public void stopListening() {
        if (mCancellationSignal != null) {
            mSelfCancled = true;
            mCancellationSignal.cancel();
            mCancellationSignal = null;
        }
    }

    /**
     * @param errMsgId
     * @param errString
     */
    @Override
    public void onAuthenticationError(int errMsgId, CharSequence errString) {
        super.onAuthenticationError(errMsgId, errString);
        Log.i(TAG, errMsgId + ":" + errString.toString());
    }

    @Override
    public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
        super.onAuthenticationHelp(helpMsgId, helpString);
        Log.i(TAG, helpMsgId + ":" + helpString.toString());

    }

    @Override
    public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
        super.onAuthenticationSucceeded(result);
        Log.i(TAG, "onAuthenticationSucceeded");

    }

    @Override
    public void onAuthenticationFailed() {
        super.onAuthenticationFailed();
        Log.i(TAG, "onAuthenticationFailed");

    }

    public interface Callback {
        void onAuthenticationError(int errMsgId, CharSequence errString);

        void onAuthenticationHelp(int helpMsgId, CharSequence helpString);

        void onAuthenticationSucceeded();

        void onAuthenticationFailed();
    }


}
