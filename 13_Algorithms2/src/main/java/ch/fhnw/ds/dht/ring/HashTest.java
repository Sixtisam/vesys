package ch.fhnw.ds.dht.ring;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;

public class HashTest {
	
	static class Put {
		public final Object key;
		public final Object value;
		public Put(Object key, Object value) { this.key = key; this.value = value; }
	}
	static class Put2 {
		public final Object key;
		public final Object value;
		public final int previousId;
		public Put2(Object key, Object value, int previousId) { this.key = key; this.value = value; this.previousId = previousId; }
	}
	static class Get {
		public final Object key;
		public Get(Object key) { this.key = key; }
	}
	static class Get2 {
		public final Object key;
		public final int previousId;
		public final int counter;
		public Get2(Object key, int previousId, int counter) { this.key = key; this.previousId = previousId; this.counter = counter; }
	}
	static class Result {
		public final int id;
		public Object value;
		public final int counter;
		public Result(int id, Object value, int counter) { this.id = id; this.value = value; this.counter = counter; }
		public String toString() { return String.format("Result(id = %d, value = %s, counter = %d)", id, value, counter); }
	}
	static class SetNext {
		public final int nextId;
		public final ActorRef next;
		public SetNext(int nextId, ActorRef next) { this.nextId = nextId; this.next = next; }
	}
	static class AddNode {
		public final int newId;
		public final ActorRef newActor;
		public AddNode(int newId, ActorRef newActor) { this.newId = newId; this.newActor = newActor; }
	}
	static class AddNode2 {
		public final int newId;
		public final ActorRef newActor;
		public AddNode2(int newId, ActorRef newActor) { this.newId = newId; this.newActor = newActor; }
	}
	static class Partition { // return all elements <= id
		public final int id;
		public Partition(int id) { this.id = id; }
	}
	static class PartitionAnswer {
		public final Map<Object, Object> map;
		public PartitionAnswer(Map<Object, Object> map) { this.map = map; }
	}
	static class Print {
		public final ActorRef start;
		public Print() { this(null); }
		public Print(ActorRef start) { this.start = start; }
	}
	
	static final int N = 1;	// size of the ring, i.e. number of nodes, N >= 1

	public static void main(String[] args) throws Exception {
		ActorSystem as = ActorSystem.create();
		Random r = new Random();
		
		Set<Integer> keys = new HashSet<>();
		int id = r.nextInt();
		keys.add(id);
		ActorRef a0 = as.actorOf(Props.create(HashNode.class, id), "Node:"+id);
		a0.tell(new SetNext(id, a0), null);
		a0.tell(new PartitionAnswer(Map.of()), null);
		
		for(int i = 1; i < N; i++) {
			do {
				id = r.nextInt();
			} while(keys.contains(id));
			keys.add(id);
			ActorRef a = as.actorOf(Props.create(HashNode.class, id), "Node:"+id);
			a0.tell(new AddNode(id, a), null);
		}
		
		Timeout  timeout = new Timeout(5, TimeUnit.SECONDS);
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		boolean exit = false;
		while (!exit) {
			String input = stdin.readLine();
			String[] cmd = input.split(" ");
			switch(cmd[0]) {
			case "get":
				if(!enoughArguments(cmd, 1)) break;
				Future<Object> res = Patterns.ask(a0, new Get(cmd[1]), timeout);
				Object result = Await.result(res, timeout.duration());
				System.out.println(result);
				break;

			case "put":
				if(!enoughArguments(cmd, 2)) break;
				a0.tell(new Put(cmd[1], cmd[2]), null);
				break;

			case "node": // looks up which node is responsible for a given key
				if(!enoughArguments(cmd, 1)) break;
				res = Patterns.ask(a0, new Get(new Object() {
					public int hashCode() { return Integer.parseInt(cmd[1]); }
				}), timeout);
				result = Await.result(res, timeout.duration());
				System.out.println(result);
				break;
				
			case "add":
				id = r.nextInt();
				if(cmd.length > 1) {
					id = Integer.parseInt(cmd[1]);
				}
				while(keys.contains(id)) id = r.nextInt();
				
				ActorRef newActor = as.actorOf(Props.create(HashNode.class, id), "Node:"+id);
				a0.tell(new AddNode(id, newActor), null);
				keys.add(id);
				System.out.println("new node with id " + id);
				break;
			
			case "print":
				a0.tell(new Print(null), null);
				break;

			case "size":
				System.out.printf("%d nodes%n", keys.size());
				break;

			case "exit":
			case "quit": 
				exit = true;
				break;

			default: 
				System.out.println("Command not understood: " + input);
			}
		}

		as.terminate();
	}

	private static boolean enoughArguments(String[] cmd, int n) {
		if(cmd.length - 1 < n) {
			System.err.printf("Not enougth arguments, %s requires %d args%n", cmd[0], n);
		}
		return cmd.length - 1 >= n;
	}

}
