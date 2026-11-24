package com.nutritrackcore.camera;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Busca las IPv4 privadas del PC para armar el enlace que abre el celular.
 * Ignora las interfaces virtuales (VirtualBox, VMware, Hyper-V, Docker).
 */
public final class LocalNetworkAddress {
    private static final String[] VIRTUAL_NAMES = {"virtualbox", "vmware", "vethernet", "hyper-v", "docker", "wsl"};

    private LocalNetworkAddress() {
    }

    /** IPv4 privadas, primero las 192.168.x.x (Wi-Fi de casa o punto de acceso del celular). */
    public static List<String> privateAddresses() {
        List<String> result = new ArrayList<>();
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nic.isUp() || nic.isLoopback() || nic.isVirtual() || isVirtualName(nic)) {
                    continue;
                }
                for (InetAddress address : Collections.list(nic.getInetAddresses())) {
                    if (address instanceof Inet4Address && address.isSiteLocalAddress()) {
                        result.add(address.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            return List.of();
        }
        result.sort(Comparator.comparingInt(LocalNetworkAddress::priority));
        return result;
    }

    /** La IP más probable para el celular, si hay alguna. */
    public static Optional<String> preferred() {
        List<String> all = privateAddresses();
        return all.isEmpty() ? Optional.empty() : Optional.of(all.get(0));
    }

    private static int priority(String ip) {
        if (ip.startsWith("192.168.")) return 0;
        if (ip.startsWith("10.")) return 1;
        return 2;                                  // 172.16.0.0 a 172.31.255.255
    }

    private static boolean isVirtualName(NetworkInterface nic) {
        String name = (nic.getName() + " " + nic.getDisplayName()).toLowerCase(Locale.ROOT);
        for (String virtual : VIRTUAL_NAMES) {
            if (name.contains(virtual)) {
                return true;
            }
        }
        return false;
    }
}