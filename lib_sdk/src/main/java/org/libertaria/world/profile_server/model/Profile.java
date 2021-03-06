package org.libertaria.world.profile_server.model;

import org.bitcoinj.core.Sha256Hash;
import org.libertaria.world.crypto.CryptoBytes;
import org.libertaria.world.global.DbObject;
import org.libertaria.world.global.Version;
import org.libertaria.world.profile_server.ProfileBase;
import org.libertaria.world.profile_server.Signer;
import org.libertaria.world.profile_server.engine.app_services.AppService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by mati on 06/02/17.
 */

public class Profile implements Signer, ProfileBase, DbObject {

    // internal sdk fields
    private long id;

    // specific fields
    /**
     * 3 bytes version
     */
    private Version version;
    /**
     * Max 64 bytes long.
     */
    private String name;
    /**
     * Max 64 bytes long
     */
    private String type;
    private byte[] img;
    /**
     * SHA-256 hash of the image
     */
    private byte[] imgHash;
    private byte[] thumbnailImg;
    private byte[] thumbnailImgHash;
    private int latitude;
    private int longitude;
    private String extraData;

    private String homeHost;
    private byte[] homeHostId;
    /**
     * Hash map of name-service of available AppServices
     */
    private HashMap<String, AppService> applicationServices = new HashMap<>();
    /**
     * Available app services to load..
     */
    private Set<String> appServices = new HashSet();

    /**
     * Key del profile
     */
    private KeyEd25519 keyEd25519;


    public Profile() {
    }

    public Profile(Version version, String name, byte[] img, int latitude, int longitude, String extraData) {
        this.version = version;
        this.name = name;
        this.img = img;
        if (imgHash != null)
            this.imgHash = Sha256Hash.hash(img);
        this.latitude = latitude;
        this.longitude = longitude;
        this.extraData = extraData;
        this.applicationServices = new HashMap<>();
    }

    public Profile(Version version, String name, String type, String extraData, byte[] img, String homeHost, KeyEd25519 keyEd25519) {
        this.version = version;
        this.name = name;
        this.type = type;
        this.extraData = extraData;
        this.img = img;
        if (imgHash != null)
            this.imgHash = Sha256Hash.hash(img);
        this.homeHost = homeHost;
        this.keyEd25519 = keyEd25519;
        this.applicationServices = new HashMap<>();
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setImg(byte[] img) {
        this.img = img;
        if (imgHash != null)
            this.imgHash = Sha256Hash.hash(img);
    }

    public void setLatitude(int latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(int longitude) {
        this.longitude = longitude;
    }

    public void setExtraData(String extraData) {
        this.extraData = extraData;
    }

    public byte[] getPublicKey() {
        return keyEd25519.getPublicKey();
    }

    public String getHexPublicKey() {
        return keyEd25519.getPublicKeyHex();
    }

    public byte[] getPrivKey() {
        return keyEd25519.getPrivateKey();
    }

    public String getPrivKeyHex() {
        return keyEd25519.getPrivateKeyHex();
    }

    public Version getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean hasService(String serviceName) {
        return applicationServices.containsKey(serviceName);
    }

    public byte[] getImg() {
        return img;
    }

    public int getLatitude() {
        return latitude;
    }

    public int getLongitude() {
        return longitude;
    }

    public String getExtraData() {
        return extraData;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void addApplicationService(AppService service) {
        applicationServices.put(service.getName(), service);
        appServices.add(service.getName());
    }

    public void setKey(KeyEd25519 keyEd25519) {
        this.keyEd25519 = keyEd25519;
    }

    public void setHomeHost(String homeHost) {
        this.homeHost = homeHost;
    }

    public void setHomeHostId(byte[] homeHostId) {
        this.homeHostId = homeHostId;
    }

    public long getId() {
        return id;
    }

    public String getHomeHost() {
        return homeHost;
    }

    public byte[] getHomeHostId() {
        return homeHostId;
    }

    @Override
    public byte[] sign(byte[] message) {
        return keyEd25519.sign(message, keyEd25519.getExpandedPrivateKey());
    }

    @Override
    public boolean verify(byte[] signature, byte[] message) {
        return keyEd25519.verify(signature, message, keyEd25519.getPublicKey());
    }


    public HashMap<String, AppService> getApplicationServices() {
        return applicationServices;
    }

    public <T extends AppService> T getAppService(String name, Class<T> clazz) {
        return (T) applicationServices.get(name);
    }

    public AppService getAppService(String name) {
        return applicationServices.get(name);
    }

    public Object getKey() {
        return keyEd25519;
    }

    public byte[] getNetworkId() {
        return Sha256Hash.hash(getPublicKey());
    }

    public String getNetworkIdHex() {
        return CryptoBytes.toHexString(Sha256Hash.hash(getPublicKey()));
    }

    public byte[] getThumbnailImg() {
        return thumbnailImg;
    }

    public void addAllServices(Set<String> appServices) {
        this.appServices.addAll(appServices);
    }

    public Set<String> getAppServices() {
        return appServices;
    }

    @Override
    public String toString() {
        return "Profile{" +
                "id=" + id +
                ", version=" + version.toString() +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", extraData='" + extraData + '\'' +
                ", homeHost='" + homeHost + '\'' +
                ", applicationServices=" + applicationServices +
                ", keyEd25519=" + keyEd25519 +
                '}';
    }


    public byte[] getImgHash() {
        return imgHash;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean containsAppService(String service) {
        return applicationServices.containsKey(service);
    }
}
