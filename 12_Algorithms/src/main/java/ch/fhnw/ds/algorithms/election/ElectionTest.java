package ch.fhnw.ds.algorithms.election;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;

public class ElectionTest {

    static class Start {
    }

    static class Token {
        public final int value;

        public Token(int value) {
            this.value = value;
        }
    }

    static class Reset {
        public final int value;

        public Reset(int value) {
            this.value = value;
        }
    }

    static final int N = 4; // size of the ring, i.e. number of nodes
    static final int K = 4; // number of nodes starting an election

    public static void main(String[] args) throws Exception {
        ActorSystem as = ActorSystem.create();
        Random r = new Random();
        Timeout timeout = new Timeout(5, TimeUnit.SECONDS);

        List<ActorRef> actors = IntStream.range(0, N).mapToObj(n -> as.actorOf(Props.create(ElectionNode.class, 4-n), "Node" + n))
                .collect(Collectors.toList());
        // tell each node its successor
        for (int i = 0; i < actors.size(); i++)
            actors.get(i).tell(actors.get((i + 1) % N), null);

        List<Future<Object>> list = new ArrayList<>();
        list.add(Patterns.ask(actors.get(0), new Start(), timeout));
        list.add(Patterns.ask(actors.get(1), new Start(), timeout));
        list.add(Patterns.ask(actors.get(2), new Start(), timeout));
        list.add(Patterns.ask(actors.get(3), new Start(), timeout));

        Future<Object> res = Futures.firstCompletedOf(list, as.dispatcher());
        for (Future<Object> f : list) {
            Await.result(f, timeout.duration());
        }

        String result = (String) list.get(0).value().get().get();
        System.out.println(result);
        System.out.println("Msg Count: " + ElectionNode.COUNTER);

        as.terminate();
    }

}
