package com.example.chiokojakharjkardam.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Observes network connectivity.
 * Call NetworkMonitor.init(context) once in App.onCreate().
 * Observe NetworkMonitor.getInstance().isConnected() in Activities/Fragments.
 */
public class NetworkMonitor {

    private static volatile NetworkMonitor instance;

    private final MutableLiveData<Boolean> connected = new MutableLiveData<>(false);
    private boolean currentlyConnected = false;

    private NetworkMonitor(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        // Seed initial state
        if (cm != null) {
            Network active = cm.getActiveNetwork();
            if (active != null) {
                NetworkCapabilities caps = cm.getNetworkCapabilities(active);
                currentlyConnected = caps != null &&
                        (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                         caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                         caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            }
            connected.setValue(currentlyConnected);

            // Register callback for changes
            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();

            cm.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    currentlyConnected = true;
                    connected.postValue(true);
                }

                @Override
                public void onLost(@NonNull Network network) {
                    // Check if another network is still active
                    Network still = cm.getActiveNetwork();
                    currentlyConnected = (still != null);
                    connected.postValue(currentlyConnected);
                }
            });
        }
    }

    public static void init(Context context) {
        if (instance == null) {
            synchronized (NetworkMonitor.class) {
                if (instance == null) instance = new NetworkMonitor(context);
            }
        }
    }

    public static NetworkMonitor getInstance() {
        if (instance == null) throw new IllegalStateException("NetworkMonitor not initialised");
        return instance;
    }

    /** LiveData — true when device has an internet connection. */
    public LiveData<Boolean> isConnected() { return connected; }

    /** Synchronous check (for use outside observe context). */
    public boolean isOnline() { return currentlyConnected; }
}

