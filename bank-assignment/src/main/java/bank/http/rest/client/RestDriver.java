package bank.http.rest.client;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import bank.BankDriver;
import bank.InactiveException;
import bank.OverdrawException;
import bank.http.rest.AccountDTO;
import bank.http.rest.BalanceDTO;
import bank.http.rest.ExceptionDTO;
import bank.http.rest.TransferDTO;

public class RestDriver implements BankDriver {
    private Bank bank = null;
    private Client webClient;
    private WebTarget rootTarget;
    private WebTarget accountsTarget;

    @Override
    public void connect(String[] args) throws UnknownHostException, IOException {
        bank = new Bank();
        webClient = ClientBuilder.newClient();
        rootTarget = webClient.target("http://localhost:8080/bank");
        accountsTarget = rootTarget.path("accounts");
        System.out.println("connected...");
    }

    @Override
    public void disconnect() throws IOException {
        bank = null;
        webClient.close();
    }

    @Override
    public bank.Bank getBank() {
        return bank;
    }

    public void detectExceptions(Response response) throws Exception {
        if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
            ExceptionDTO dto = response.readEntity(ExceptionDTO.class);
            throw (Exception) Class.forName(dto.name).getConstructor(String.class).newInstance(dto.message);
        }
        if (response.getStatus() == Status.GONE.getStatusCode()) {
            throw new InactiveException();
        }
    }

    public class Bank implements bank.Bank {

        @Override
        public Set<String> getAccountNumbers() throws IOException {
            try {
                Response response = accountsTarget
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .buildGet()
                        .invoke();
                detectExceptions(response);
                return response.readEntity(new GenericType<Set<String>>() {
                });
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public bank.Account getAccount(String number) throws IOException {
            try {
                if (number.trim().equals("")) {
                    return null;
                }
                AccountDTO accDto = accountsTarget.path(number.trim())
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .buildGet()
                        .invoke(AccountDTO.class);
                return new Account(accDto.getNumber(), accDto.getOwner());
            } catch (NotFoundException e) {
                return null;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public String createAccount(String owner) throws IOException {
            try {
                Response response = accountsTarget
                        .request()
                        .buildPost(Entity.json(new AccountDTO(owner)))
                        .invoke();
                // could extract number from location header or body answer
                AccountDTO accDto = webClient.target(response.getLink("self"))
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .buildGet()
                        .invoke(AccountDTO.class);
                detectExceptions(response);
                if (response.getStatusInfo().equals(Status.CREATED)) {
                    return accDto.getNumber();
                } else {
                    return null;
                }
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public boolean closeAccount(String number) throws IOException {
            try {
                Response response = accountsTarget.path(number)
                        .request()
                        .buildDelete()
                        .invoke();
                detectExceptions(response);
                return response.getStatusInfo().equals(Status.OK);
            } catch (InactiveException e) {
                return false;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public void transfer(bank.Account from, bank.Account to, double amount)
                throws IOException, OverdrawException, InactiveException {
            try {
                TransferDTO transfer = new TransferDTO(from.getNumber(), to.getNumber(), amount);
                Response response = rootTarget.path("transfers")
                        .request()
                        .buildPost(Entity.json(transfer))
                        .invoke();
                detectExceptions(response);
            } catch (InactiveException | OverdrawException | IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

    }

    private class Account implements bank.Account {
        private final String number;
        private final String owner;
        private double balance;
        private boolean active;
        private String etag;

        private Account(String number, String owner) {
            this.number = number;
            this.owner = owner;
        }

        @Override
        public String getNumber() {
            return number;
        }

        @Override
        public String getOwner() {
            return owner;
        }

        private Response loadAccountResponse() throws IOException {
            Response response = accountsTarget.path(this.number)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .header("If-None-Match", etag)
                    .buildGet()
                    .invoke();
            try {
                detectExceptions(response);
            } catch (Exception e) {
                throw new IOException(e);
            }

            return response;
        }

        private void loadAndCacheAccount() throws IOException {
            Response resp = loadAccountResponse();
            if (resp.getStatusInfo().equals(Status.NOT_MODIFIED) || resp.getStatusInfo().equals(Status.NOT_FOUND)) {
                return;
            }
            AccountDTO acc = resp.readEntity(AccountDTO.class);
            this.etag = resp.getEntityTag().toString();
            this.active = acc.active;
            this.balance = acc.balance;
        }

        @Override
        public double getBalance() throws IOException {
            loadAndCacheAccount();
            return balance;
        }

        @Override
        public boolean isActive() throws IOException {
            loadAndCacheAccount();
            return active;
        }

        @Override
        public void deposit(double amount) throws InactiveException, IOException {
            if (amount < 0)
                throw new IllegalArgumentException();
            try {
                updateBalance(balance -> balance + amount);
            } catch (OverdrawException e) {
                // nop can never happen
            }
        }

        @Override
        public void withdraw(double amount) throws InactiveException, OverdrawException, IOException {
            if (amount < 0)
                throw new IllegalArgumentException();
            updateBalance(balance -> balance - amount);
        }

        /**
         * Updates the balance of the account with the returned value of the passed
         * lambda
         */
        private void updateBalance(DoubleUnaryOperator operator)
                throws InactiveException, OverdrawException, IOException {
            Objects.requireNonNull(operator);
            try {
                while (true) {
                    loadAndCacheAccount();
                    BalanceDTO balanceDto = new BalanceDTO(operator.applyAsDouble(this.balance));
                    if (balanceDto.getBalance() < 0.0) {
                        throw new OverdrawException();
                    }
                    Response updateResp = accountsTarget.path(this.number)
                            .request()
                            .header("If-Match", this.etag)
                            .buildPut(Entity.json(balanceDto))
                            .invoke();

                    detectExceptions(updateResp);
                    if (updateResp.getStatusInfo().equals(Status.OK)) {
                        return;
                    }
                }
            } catch (InactiveException | OverdrawException | IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
    }
}
