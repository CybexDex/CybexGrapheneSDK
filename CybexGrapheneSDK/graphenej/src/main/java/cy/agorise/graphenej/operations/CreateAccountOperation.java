package cy.agorise.graphenej.operations;

import com.google.common.primitives.Bytes;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

import cy.agorise.graphenej.AccountOptions;
import cy.agorise.graphenej.AssetAmount;
import cy.agorise.graphenej.Authority;
import cy.agorise.graphenej.BaseOperation;
import cy.agorise.graphenej.OperationType;
import cy.agorise.graphenej.UserAccount;
import cy.agorise.graphenej.Util;

public class CreateAccountOperation extends BaseOperation {
    public static final String KEY_REGISTRAR = "registrar";
    public static final String KEY_REFERRER = "referrer";
    public static final String KEY_NAME = "name";
    public static final String KEY_OWNER = "owner";
    public static final String KEY_ACTIVE = "active";
    public static final String KEY_OPTIONS = "options";
    public static final String KEY_REFERRER_PERCENT = "referrer_percent";

    private AssetAmount fee;
    private UserAccount registrar;
    private UserAccount referrer;
    private int referrer_percent;
    private String name;
    private Authority owner;
    private Authority active;
    private AccountOptions options;

    public CreateAccountOperation(AssetAmount fee, UserAccount registrar, UserAccount referrer, int referrer_percent, String name, Authority owner,
                                  Authority active, AccountOptions options) {
        super(OperationType.ACCOUNT_CREATE_OPERATION);
        this.fee = fee;
        this.registrar = registrar;
        this.referrer = referrer;
        this.referrer_percent = referrer_percent;
        this.name = name;
        this.owner = owner;
        this.active = active;
        this.options = options;

    }

    public AccountOptions getOptions() {
        return options;
    }

    public void setOptions(AccountOptions options) {
        this.options = options;
    }

    public AssetAmount getFee() {
        return fee;
    }

    public Authority getActive() {
        return active;
    }

    public void setActive(Authority active) {
        this.active = active;
    }

    public Authority getOwner() {
        return owner;
    }

    public void setOwner(Authority owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UserAccount getRegistrar() {
        return registrar;
    }

    public void setRegistrar(UserAccount registrar) {
        this.registrar = registrar;
    }

    public int getReferrer_percent() {
        return referrer_percent;
    }

    public void setReferrer_percent(int referrer_percent) {
        this.referrer_percent = referrer_percent;
    }

    public UserAccount getReferrer() {
        return referrer;
    }

    public void setReferrer(UserAccount referrer) {
        this.referrer = referrer;
    }

    @Override
    public void setFee(AssetAmount assetAmount) {
        this.fee = assetAmount;
    }


    @Override
    public byte[] toBytes() {
        byte[] feeBytes = fee.toBytes();
        byte[] registrarBytes = registrar.toBytes();
        byte[] referrerBytes = referrer.toBytes();
        byte[] referrer_percentBytes = Util.revertShort((short)referrer_percent);
        byte[] nameLength = Util.serializeLongToBytes(name.length());
        byte[] nameBytes = Util.hexlify(name);
        byte[] ownerBytes = owner.toBytes();
        byte[] activeBytes = active.toBytes();
        byte[] optionsBytes = options.toBytes();
        return Bytes.concat(feeBytes, registrarBytes, referrerBytes, referrer_percentBytes, nameLength, nameBytes, ownerBytes, activeBytes, optionsBytes);
    }

    @Override
    public String toJsonString() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(CreateAccountOperation.class, new CreateAccountOperation.CreateAccountSerializer());
        return gsonBuilder.create().toJson(this);
    }

    public static class CreateAccountSerializer implements JsonSerializer<CreateAccountOperation> {

        @Override
        public JsonElement serialize(CreateAccountOperation transfer, Type type, JsonSerializationContext jsonSerializationContext) {
            return transfer.toJsonObject();
        }
    }

    @Override
    public JsonElement toJsonObject() {
        JsonArray array = new JsonArray();
        array.add(this.getId());
        JsonObject jsonObject = new JsonObject();
        if (fee != null)
            jsonObject.add(KEY_FEE, fee.toJsonObject());
        jsonObject.addProperty(KEY_REFERRER, referrer.getObjectId());
        jsonObject.addProperty(KEY_REGISTRAR, registrar.getObjectId());
        jsonObject.addProperty(KEY_REFERRER_PERCENT, referrer_percent);
        jsonObject.addProperty(KEY_NAME, name);
        jsonObject.add(KEY_ACTIVE, active.toJsonObject());
        jsonObject.add(KEY_OWNER, owner.toJsonObject());
        jsonObject.add(KEY_OPTIONS, options.toJsonObject());

        array.add(jsonObject);
        return array;
    }
}
