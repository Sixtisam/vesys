package bank.akka.client;

import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.pattern.Patterns;
import akka.util.Timeout;
import bank.BankDriver2;
import bank.commands.AccountChangedAnswer;
import bank.commands.BankAnswer;
import bank.commands.BankAnswer.BankExceptionAnswer;
import bank.commands.BankCommand;
import bank.commands.CommandBank;
import scala.concurrent.Await;
import scala.concurrent.Future;

public class AkkaDriver implements BankDriver2 {
    private final List<UpdateHandler> updateHandlers = new ArrayList<>();

    private ActorSelection serverActor;

    private ActorSystem system;
    private Bank bank;

    @Override
    public void connect(String[] args) throws IOException {
        bank = new Bank();

        Config config = ConfigFactory.load().getConfig("BankClient");
        system = ActorSystem.create("Bank", config);

        ActorRef clientActor = system.actorOf(Props.create(ClientNotificationActor.class, updateHandlers));

        serverActor = system.actorSelection("akka://Bank@127.0.0.1:25520/user/BankServer");
        // subscribe to server
        serverActor.tell(clientActor, clientActor);
    }

    /**
     * Handles any {@link AccountChangedAnswer} objects
     *
     */
    public static class ClientNotificationActor extends AbstractActor {
        private final List<UpdateHandler> updateHandlers;

        public ClientNotificationActor(List<UpdateHandler> updateHandlers) {
            super();
            this.updateHandlers = updateHandlers;
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(AccountChangedAnswer.class, msg -> {
                        updateHandlers.forEach(h -> {
                            try {
                                h.accountChanged(msg.getData());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    })
                    .matchAny(msg -> {
                        System.out.println("Unhandled Message Received");
                        unhandled(msg);
                    })
                    .build();
        }
       
    }

    @Override
    public void disconnect() throws IOException {
        system.terminate();
    }

    @Override
    public Bank getBank() {
        return bank;
    }

    @Override
    public void registerUpdateHandler(UpdateHandler handler) throws IOException {
        updateHandlers.add(handler);
    }

    public class Bank extends CommandBank {
        @Override
        protected <T extends BankCommand<? extends R>, R extends BankAnswer<? extends Serializable>> R remoteCall(T command, Class<R> answerType)
                throws Exception {
            Timeout timeout = Timeout.create(Duration.ofSeconds(2));
            // ask the server actor for result
            Future<Object> future = Patterns.ask(serverActor, command, timeout);
            @SuppressWarnings("unchecked")
            R answer = (R) Await.result(future, timeout.duration());

            // decide if exception or valid result
            if (answer instanceof BankExceptionAnswer) {
                throw ((BankExceptionAnswer) answer).getData();
            } else if (answer instanceof BankAnswer) {
                return answerType.cast(answer);
            } else {
                throw new IOException("unexpected type " + answer.getClass().getSimpleName());
            }
        }
    }

}
