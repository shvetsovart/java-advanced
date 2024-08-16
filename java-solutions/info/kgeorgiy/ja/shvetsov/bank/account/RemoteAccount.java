package info.kgeorgiy.ja.shvetsov.bank.account;

public class RemoteAccount implements Account {
    private final String id;
    private int amount;

    /**
     * Constructor out of id.
     * @param id id of the account
     */
    public RemoteAccount(final String id) {
        this.id = id;
        amount = 0;
    }

    /**
     * Constructor out of other {@link RemoteAccount}.
     * @param other {@link RemoteAccount} account
     */
    public RemoteAccount(RemoteAccount other) {
        this.id = other.getId();
        this.amount = other.getAmount();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public synchronized int getAmount() {
        System.out.println("Getting amount of money for account " + id);

        return amount;
    }

    @Override
    public synchronized void setAmount(final int amount) {
        System.out.println("Setting amount of money for account " + id);

        this.amount = amount;
    }
}
