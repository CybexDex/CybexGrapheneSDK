package cy.agorise.graphenej.operations;

import cy.agorise.graphenej.AccountOptions;
import cy.agorise.graphenej.AssetAmount;
import cy.agorise.graphenej.Authority;
import cy.agorise.graphenej.UserAccount;

public class CreateAccountOperationBuilder extends BaseOperationBuilder {

    private AssetAmount fee;
    private UserAccount registrar;
    private UserAccount referrer;
    private int referrer_percent;
    private String name;
    private Authority owner;
    private Authority active;
    private AccountOptions options;

    public CreateAccountOperationBuilder setFee(AssetAmount fee) {
        this.fee = fee;
        return this;
    }

    public CreateAccountOperationBuilder setReferrer(UserAccount referrer) {
        this.referrer = referrer;
        return this;
    }

    public CreateAccountOperationBuilder setReferrer_percent(int referrer_percent) {
        this.referrer_percent = referrer_percent;
        return this;
    }

    public CreateAccountOperationBuilder setRegistrar(UserAccount registrar) {
        this.registrar = registrar;
        return this;
    }

    public CreateAccountOperationBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public CreateAccountOperationBuilder setOwner(Authority owner) {
        this.owner = owner;
        return this;
    }

    public CreateAccountOperationBuilder setActive(Authority active) {
        this.active = active;
        return this;
    }

    public CreateAccountOperationBuilder setOptions(AccountOptions options) {
        this.options = options;
        return this;
    }

    @Override
    public CreateAccountOperation build() {
        return new CreateAccountOperation(fee, registrar, referrer, referrer_percent, name, owner, active, options);
    }
}
