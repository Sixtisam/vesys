package ch.fhnw.ds.dht.complete;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import ch.fhnw.ds.dht.complete.HashTest.AddNode;
import ch.fhnw.ds.dht.complete.HashTest.Get;
import ch.fhnw.ds.dht.complete.HashTest.Get2;
import ch.fhnw.ds.dht.complete.HashTest.Partition;
import ch.fhnw.ds.dht.complete.HashTest.PartitionAnswer;
import ch.fhnw.ds.dht.complete.HashTest.Print;
import ch.fhnw.ds.dht.complete.HashTest.Put;
import ch.fhnw.ds.dht.complete.HashTest.Put2;
import ch.fhnw.ds.dht.complete.HashTest.Result;

public class HashNode extends AbstractActor {
	private final int id;
	private TreeMap<Integer, ActorRef> actors;
	
	private Map<Object, Object> values = new HashMap<>();

	public HashNode(int id) { this.id = id; }

	@SuppressWarnings("unchecked")
	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(TreeMap.class, actors -> {
				this.actors = actors;
			})
			.match(Put.class, msg -> {
				var keys = actors.navigableKeySet();
				var key = keys.ceiling(msg.key.hashCode());
				if(key == null) key = keys.first();
				actors.get(key).tell(new Put2(msg.key, msg.value), getSender());
			})
			.match(Put2.class, msg -> {
				values.put(msg.key, msg.value);
			})
			.match(Get.class, msg -> {
				var keys = actors.navigableKeySet();
				var key = keys.ceiling(msg.key.hashCode());
				if(key == null) key = keys.first();
				actors.get(key).tell(new Get2(msg.key), getSender());
			})
			.match(Get2.class, msg -> {
				getSender().tell(new Result(this.id, values.get(msg.key)), getSelf());
			})
			.match(AddNode.class, msg -> {
				actors.put(msg.id, msg.actor);
			})
			.match(Partition.class, msg -> {
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
			})
			.match(PartitionAnswer.class, msg -> {
				System.out.printf("added %d values to node %d%n", msg.map.size(), id);
				values.putAll(msg.map);
			})
			.match(Print.class, msg -> {
				synchronized(System.out) { 
					System.out.println("Node " + id);
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