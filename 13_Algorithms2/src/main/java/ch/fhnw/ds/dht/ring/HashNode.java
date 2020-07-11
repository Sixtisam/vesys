package ch.fhnw.ds.dht.ring;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import ch.fhnw.ds.dht.ring.HashTest.AddNode;
import ch.fhnw.ds.dht.ring.HashTest.AddNode2;
import ch.fhnw.ds.dht.ring.HashTest.Get;
import ch.fhnw.ds.dht.ring.HashTest.Get2;
import ch.fhnw.ds.dht.ring.HashTest.Partition;
import ch.fhnw.ds.dht.ring.HashTest.PartitionAnswer;
import ch.fhnw.ds.dht.ring.HashTest.Print;
import ch.fhnw.ds.dht.ring.HashTest.Put;
import ch.fhnw.ds.dht.ring.HashTest.Put2;
import ch.fhnw.ds.dht.ring.HashTest.Result;
import ch.fhnw.ds.dht.ring.HashTest.SetNext;
import scala.Tuple2;

public class HashNode extends AbstractActor {
	private final int id;
	private ActorRef next;
	private int nextId;

	private boolean ready = false;
	private final Queue<Tuple2<Object, ActorRef>> pendingMessages = new ArrayDeque<>();

	private Map<Object, Object> values = new HashMap<>();

	public HashNode(int id) { this.id = id; }

	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(SetNext.class, msg -> {
				next = msg.next;
				nextId = msg.nextId;
			})
			.match(Put.class, msg -> {
				next.tell(new Put2(msg.key, msg.value, this.id), getSelf());
			})
			.match(Put2.class, msg -> {
				int hash = msg.key.hashCode();
				if(between(hash, msg.previousId, this.id)) {
					if(!ready) {
						pendingMessages.add(new Tuple2<>(msg, getSender()));
					} else {
						values.put(msg.key, msg.value);
					}
				} else {
					next.tell(new Put2(msg.key, msg.value, this.id), getSelf());
				}
			})
			.match(Get.class, msg -> {
				next.tell(new Get2(msg.key, this.id, 1), getSender());
			})
			.match(Get2.class, msg -> {
				int hash = msg.key.hashCode();
				if(between(hash, msg.previousId, this.id)) {
					if(!ready) {
						pendingMessages.add(new Tuple2<>(msg, getSender()));
					} else {
						getSender().tell(new Result(this.id, values.get(msg.key), msg.counter), getSelf());
					}
				} else {
					next.tell(new Get2(msg.key, this.id, msg.counter + 1), getSender());
				}
			})
			.match(AddNode.class, msg -> {
				next.tell(new AddNode2(msg.newId, msg.newActor), getSelf());
			})
			.match(AddNode2.class, msg -> {
				if(between(msg.newId, this.id, this.nextId)) {
					next.tell(new Partition(msg.newId), msg.newActor);
					msg.newActor.tell(new SetNext(this.nextId, this.next), getSelf());
					this.next = msg.newActor;
					this.nextId = msg.newId;
				} else {
					next.tell(new AddNode2(msg.newId, msg.newActor), getSelf());
				}
			})
			.match(Partition.class, msg -> {
				if(!ready) {
					pendingMessages.add(new Tuple2<>(msg, getSender()));
				} else {
					Map<Object, Object> res = new HashMap<>();
					var it = values.entrySet().iterator();
					while(it.hasNext()) {
						var entry = it.next();
						var hash = entry.getKey().hashCode();
						if(!between(hash, msg.id, this.id)) {
							res.put(entry.getKey(), entry.getValue());
							it.remove();
						}
					}
					getSender().tell(new PartitionAnswer(res), getSelf());
				}
			})
			.match(PartitionAnswer.class, msg -> {
				System.out.printf("added %d values to node %d%n", msg.map.size(), this.id);
				values.putAll(msg.map);
				this.ready = true;
				while(!pendingMessages.isEmpty()) {
					var t = pendingMessages.poll();
					getSelf().tell(t._1, t._2);
				}
			})
			.match(Print.class, msg -> {
				if(msg.start != getSelf()) {
					System.out.println("Node " + id);
					System.out.println("\tnext: " + next);
					for(Object key: values.keySet()) {
						System.out.printf("\t%s [%d] => %s%n", key, key.hashCode(), values.get(key));
					}
					if(msg.start == null) next.tell(new Print(getSelf()), getSelf());
					else next.tell(msg, getSelf());
				}
			})
			.matchAny(msg -> {
				System.out.println("UnHandled Message Received");
				unhandled(msg);
			})
			.build();
	}

	// from < key <= to (with modulo arithmetic)
	private boolean between(int key, int from, int to) {
		return ((from < to)  && ((from < key) && (key <= to)))
			|| ((from >= to) && ((from < key) || (key <= to)));	
	}
}
