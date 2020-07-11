package ch.fhnw.ds.dht.chord;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ReceiveTimeout;
import ch.fhnw.ds.dht.chord.HashTest.Get;
import ch.fhnw.ds.dht.chord.HashTest.Get2;
import ch.fhnw.ds.dht.chord.HashTest.Partition;
import ch.fhnw.ds.dht.chord.HashTest.PartitionAnswer;
import ch.fhnw.ds.dht.chord.HashTest.Print;
import ch.fhnw.ds.dht.chord.HashTest.Put;
import ch.fhnw.ds.dht.chord.HashTest.Put2;
import ch.fhnw.ds.dht.chord.HashTest.Result;
import scala.concurrent.duration.Duration;

public class HashNode extends AbstractActor {
	private final int id;
	
	private int next;
	private TreeMap<Integer, ActorRef> fingerTable;
	
	private Map<Object, Object> values = new HashMap<>();

	public HashNode(int id) { this.id = id; }

	@Override
	public void preStart() throws Exception {
		getContext().setReceiveTimeout(Duration.create(5, TimeUnit.SECONDS));
		super.preStart();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(TreeMap.class, fingerTable -> {
				this.fingerTable = fingerTable;
			})
			.match(Integer.class, next -> {
				this.next = next;
			})
			// Inserts a value into dht by looking up where to insert it
			.match(Put.class, msg -> {
				int hash = msg.key.hashCode();
				//System.out.printf("Put of %s at %s%n", hash, id);
				if(between(hash, id, next)) {
				    // if value managed by successor -> insert value into successor
					fingerTable.get(next).tell(new Put2(msg.key, msg.value), getSelf());
				} else {
				    // find closted preceding node of value to insert
					var set = fingerTable.navigableKeySet();
					var prev = set.lower(hash);
					if(prev == null) prev = set.last();
					fingerTable.get(prev).tell(msg, getSelf());
				}
			})
			// Inserts the value into this node
			.match(Put2.class, msg -> {
				values.put(msg.key, msg.value);
			})
			// Lookup a value in dht
			.match(Get.class, msg -> {
				int hash = msg.key.hashCode();
				//System.out.printf("Lookup of %s at %s%n", hash, id);
				if(between(hash, id, next)) {
				    // value is managed by successor -> Get2 
					fingerTable.get(next).tell(new Get2(msg.key, msg.counter + 1), getSender());
				} else {
					var set = fingerTable.navigableKeySet();
					var prev = set.lower(hash);
					if(prev == null) prev = set.last();
					fingerTable.get(prev).tell(new Get(msg.key, msg.counter + 1), getSender());
				}
			})
			// Looks up the requested value in this node
			.match(Get2.class, msg -> {
				getSender().tell(new Result(this.id, msg.key, values.get(msg.key), msg.counter), getSelf());
			})
			// If a node receives this message, that means that some values must be transferred to the sender 
			// (sender is a new node that just joined the chord ring)
			.match(Partition.class, msg -> {
				Map<Object, Object> res = new HashMap<>();
				var it = values.entrySet().iterator();
				while(it.hasNext()) {
					var entry = it.next();
					var hash = entry.getKey().hashCode();
					// msg.id contains value of newly inserted node -> all values lower will not be managed by this node anymore.
					if(!between(hash, msg.id, this.id)) {
						res.put(entry.getKey(), entry.getValue());
						it.remove();
					}
				}
				// transfer the values to its newly assigned managing node
				getSender().tell(new PartitionAnswer(res), getSelf());
			})
			// Answer to Partition -> the answer contains the values which are now managed by this node.
			.match(PartitionAnswer.class, msg -> {
				System.out.printf("added %d values to node %d%n", msg.map.size(), id);
				values.putAll(msg.map);
			})
			// Stabiliazion function, part 1: runs periodically
			.match(ReceiveTimeout.class, msg -> {
				// TODO the old entries shoudl be removed
				ActorRef ref = fingerTable.get(next);
				int dx = 1;
				// according to the chord formular successor((n + 2^i) mod 2^m), get the resulting hashes.
				for(int k = 0; k < HashTest.K; k++) {
					final int hash = id+dx;
					// Do a GET request, which's result is delivered to this node.
					ref.tell(new Get(new Object() {
						public int hashCode() { return hash; }
					}), getSelf());
					dx = dx << 1;
				}
			})
			// Stabilization function, part 2
			// Any result received was caused by the Stabilization function part 1.
			// It indicates that the fingertable can be updated 
			.match(Result.class, msg -> {
				fingerTable.put(msg.id, getSender());
			})
			.match(Print.class, msg -> {
			    synchronized(System.out) { 
			        System.out.println("Node " + id);
			        System.out.println("\tnext: " + next);
			        System.out.println("\tfingertable:");
			        for(var e : fingerTable.entrySet()) {
			            System.out.printf("\t\t%s: %s%n", e.getKey(), e.getValue());
			        }
			        for(Object key: values.keySet()) {
			            System.out.printf("\t%s [%d] => %s%n", key, key.hashCode(), values.get(key));
			        }
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