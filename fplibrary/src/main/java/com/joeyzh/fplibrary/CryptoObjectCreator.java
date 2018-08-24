package com.joeyzh.fplibrary;

import android.annotation.TargetApi;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * Created by Joey on 2016/11/7.
 * 用于创建指纹识别的秘钥
 */

@TargetApi(Build.VERSION_CODES.M)
public class CryptoObjectCreator {

    private static final String KEY_NAME = "key_not_invalidated";
    static final String DEFAULT_KEY_NAME = "default_key";


    FingerprintManagerCompat.CryptoObject mCryptoObject;
    private KeyStore mKeyStore;
    private KeyGenerator mKeyGenerator;
    private Cipher mCipher;

    public interface ICryptoObjectCreateListener {
        void onDataPrepared(FingerprintManagerCompat.CryptoObject cryptoObject);
    }

    public CryptoObjectCreator(ICryptoObjectCreateListener createListener) {
        mKeyStore = providesKeystore();
        mKeyGenerator = providesKeyGenerator();
        mCipher = createCipher();
        if (mKeyStore != null && mKeyGenerator != null && mCipher != null) {
            mCryptoObject = new FingerprintManagerCompat.CryptoObject(mCipher);
        }
        prepareData(createListener);
    }

    private void prepareData(final ICryptoObjectCreateListener createListener) {
        new Thread("FingerprintLogic:InitThread") {
            @Override
            public void run() {
                try {
                    if (mCryptoObject != null) {
                        createKey(DEFAULT_KEY_NAME,true);
                        createKey(KEY_NAME, false);
                        // Set up the crypto object for later. The object will be authenticated by use
                        // of the fingerprint.
                        if (!initCipher(mCipher, KEY_NAME)) {
                            Log.i(getClass().getSimpleName(), "Failed to init Cipher.");
                        }
                    }
                } catch (Exception e) {
                    Log.i(getClass().getSimpleName(), " Failed to init Cipher, e:" + Log.getStackTraceString(e));
                }
                if (createListener != null) {
                    createListener.onDataPrepared(mCryptoObject);
                }
            }
        }.start();
    }


    /**
     * Creates a symmetric key in the Android Key Store which can only be used after the user has
     * authenticated with fingerprint.
     *
     * @param keyName                          the name of the key to be created
     * @param invalidatedByBiometricEnrollment if {@code false} is passed, the created key will not
     *                                         be invalidated even if a new fingerprint is enrolled.
     *                                         The default value is {@code true}, so passing
     *                                         {@code true} doesn't change the behavior
     *                                         (the key will be invalidated if a new fingerprint is
     *                                         enrolled.). Note that this parameter is only valid if
     *                                         the app works on Android N developer preview.
     */
    public void createKey(String keyName, boolean invalidatedByBiometricEnrollment) {
        // The enrolling flow for fingerprint. This is where you ask the user to set up fingerprint
        // for your flow. Use of keys is necessary if you need to know if the set of
        // enrolled fingerprints has changed.
        try {
            mKeyStore.load(null);
            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder

            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(keyName,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    // Require the user to authenticate with a fingerprint to authorize every use
                    // of the key
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);

            // This is a workaround to avoid crashes on devices whose API level is < 24
            // because KeyGenParameterSpec.Builder#setInvalidatedByBiometricEnrollment is only
            // visible on API level +24.
            // Ideally there should be a compat library for KeyGenParameterSpec.Builder but
            // which isn't available yet.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setInvalidatedByBiometricEnrollment(invalidatedByBiometricEnrollment);
            }
            mKeyGenerator.init(builder.build());
            mKeyGenerator.generateKey();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException
                | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Initialize the {@link Cipher} instance with the created key in the
     * {@link #createKey(String, boolean)} method.
     *
     * @param keyName the key name to init the cipher
     * @return {@code true} if initialization is successful, {@code false} if the lock screen has
     * been disabled or reset after the key was generated, or if a fingerprint got enrolled after
     * the key was generated.
     */
    private boolean initCipher(Cipher cipher, String keyName) {
        try {
            mKeyStore.load(null);
                SecretKey key = (SecretKey) mKeyStore.getKey(keyName, null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            Log.i(getClass().getSimpleName(), " Failed to initCipher, e:" + Log.getStackTraceString(e));
            return false;
        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException e) {
            Log.i(getClass().getSimpleName(), " Failed to initCipher, e :" + Log.getStackTraceString(e));
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }

    public static KeyStore providesKeystore() {
        try {
            return KeyStore.getInstance("AndroidKeyStore");
        } catch (Throwable e) {
            return null;
        }
    }

    public static KeyGenerator providesKeyGenerator() {
        try {
            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * 创建对称加密秘钥
     */
    private Cipher createCipher() {
        Cipher defaultCipher = null;
        try {
            defaultCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("创建Cipher对象失败", e);
        }
        return defaultCipher;

    }

    public FingerprintManagerCompat.CryptoObject getCryptoObject() {
        return mCryptoObject;
    }

    public void onDestroy() {
        mCipher = null;
        mCryptoObject = null;
        mCipher = null;
        mKeyStore = null;
    }
}
