package BigPrimes;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Random;

public class WorkerBehavior extends AbstractBehavior<WorkerBehavior.Command> {


    public static class Command implements Serializable {

        private static final long serialVersionUID = 1L;

        private String message;
        private ActorRef<ManagerBehavior.Command> sender;

        public Command(String message, ActorRef<ManagerBehavior.Command> sender) {
            this.message = message;
            this.sender = sender;
        }

        public String getMessage() {
            return message;
        }

        public ActorRef<ManagerBehavior.Command> getSender() {
            return sender;
        }
    }

    private WorkerBehavior(ActorContext<Command> context) {
        super(context);
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(WorkerBehavior::new);
    }


    @Override
    public Receive<Command> createReceive() {
        return firstMessage();
    }

    public Receive<Command> firstMessage() {
        return newReceiveBuilder()
                .onAnyMessage(c -> {
                    getContext().getLog().info("First Message received");
                    Random random = new Random();
                    BigInteger b = new BigInteger(2000, random);
                    BigInteger prime = b.nextProbablePrime();
                    if(random.nextInt(5) < 2) {
                        c.getSender().tell(new ManagerBehavior.ResultCommand(prime));
                    }
                    return secondMessage(prime);
                })
                .build();
    }

    public Receive<Command> secondMessage(BigInteger prime) {
        return newReceiveBuilder()
                .onAnyMessage(c -> {
                    System.out.println("Second Message received");
                    if(new Random().nextInt(5) < 2) {
                        c.getSender().tell(new ManagerBehavior.ResultCommand(prime));
                    }
                    return Behaviors.same();
                })
                .build();
    }
}
