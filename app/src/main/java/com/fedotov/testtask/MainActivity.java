package com.fedotov.testtask;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.FileUtils;
import android.text.format.Formatter;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button button;
    private static KeyPair keyPair;
    private boolean isSocketsWork;
    @SuppressLint("StaticFieldLeak")
    public static Context currentContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.isSocketsWork = false;
        currentContext = this;
        setContentView(R.layout.activity_main);
        this.button = findViewById(R.id.button1);

        try {
            setKeys();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
            Log.d("errors", "error in setKeys");
        }

        this.button.setOnClickListener(this);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onClick(View v) {
        TextView textView = findViewById(R.id.ipText);
        if(!isSocketsWork) {
            try {
                startService(new Intent(this, FirstSocket.class));
                startService(new Intent(this, SecondSocket.class));
                isSocketsWork = true;

                String text = getLocalAddress().toString() +
                        "\nPort1: " + FirstSocket.port+
                        "\nPort2: " + SecondSocket.port;
                textView.setText(text);

            } catch (Exception e) {
                textView.setText("Error! Try again");
            }
            button.setText("stop");
        }else {
            stopService(new Intent(this, FirstSocket.class));
            stopService(new Intent(this, SecondSocket.class));
            isSocketsWork = false;
            String text = "";
            textView.setText(text);
            button.setText("start");
        }
    }

    private void setKeys() throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeySpecException {

        SharedPreferences sharedPreferences = this.getSharedPreferences("RSAKeys", Context.MODE_PRIVATE);

        if(!(sharedPreferences.contains("private") && sharedPreferences.contains("public"))) {
            //Generate RSA keys
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024);
            KeyPair keyPair = generator.generateKeyPair();

            //Save keys to memory
            String Private = Base64.encodeToString(keyPair.getPrivate().getEncoded(), Base64.DEFAULT);
            String Public = Base64.encodeToString(keyPair.getPublic().getEncoded(), Base64.DEFAULT);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("private", Private);
            editor.putString("public", Public);
            editor.apply();
        }

        //init keys
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        //get public key from file
        String publicK = sharedPreferences.getString("public", "");
        byte[] keyBytes = Base64.decode(publicK.getBytes("utf-8"), Base64.DEFAULT);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        PublicKey publicKey = (PublicKey) keyFactory.generatePublic(spec);
        //get private key from file
        String privateK = sharedPreferences.getString("private", "");
        keyBytes = Base64.decode(privateK.getBytes("utf-8"), Base64.DEFAULT);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        keyPair = new KeyPair(publicKey, privateKey);

    }

    public InetAddress getLocalAddress() throws Exception {
        List<NetworkInterface> netInts = Collections.list(NetworkInterface.getNetworkInterfaces());

        if (netInts.size() == 1) {
            return InetAddress.getLocalHost();
        }

        for (NetworkInterface net : netInts) {
            if (!net.isLoopback() && !net.isVirtual() && net.isUp()) {
                Enumeration<InetAddress> addrEnum = net.getInetAddresses();
                while (addrEnum.hasMoreElements()) {
                    InetAddress addr = addrEnum.nextElement();
                    if ( !addr.isLoopbackAddress() && !addr.isAnyLocalAddress()
                            && !addr.isLinkLocalAddress() && !addr.isMulticastAddress()
                    ) {
                        return addr;
                    }
                }
            }
        }
        return null;
    }

    public static KeyPair getKeyPair() {
        return keyPair;
    }


}