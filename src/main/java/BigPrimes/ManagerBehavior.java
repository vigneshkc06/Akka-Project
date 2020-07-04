package BigPrimes;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.io.Serializable;
import java.math.BigInteger;
import java.time.Duration;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.IntStream;

public class ManagerBehavior extends AbstractBehavior<ManagerBehavior.Command> {

    public interface Command extends Serializable{};

    public static class InstructionCommand implements Command {

        private static final long serialVersionUID = 1L;

        private String message;

        private ActorRef<SortedSet<BigInteger>> sender;

        public InstructionCommand(String message,ActorRef<SortedSet<BigInteger>> sender) {

            this.message = message;
            this.sender = sender;
        }

        public String getMessage() {
            return message;
        }

        public ActorRef<SortedSet<BigInteger>> getSender() {
            return sender;
        }
    }

    public static class ResultCommand implements Command {

        private static final long serialVersionUID = 1L;

        private BigInteger prime;

        public ResultCommand(BigInteger prime) {
            this.prime = prime;
        }

        public BigInteger getPrime() {
            return prime;
        }
    }

    private class FailureCommand implements Command{
        private static final long serialVersionUID = 1L;
        private ActorRef<WorkerBehavior.Command> ref;

        public FailureCommand(ActorRef<WorkerBehavior.Command> ref) {
            this.ref = ref;
        }

        public ActorRef<WorkerBehavior.Command> getRef() {
            return ref;
        }
    }

    private SortedSet<BigInteger> primes = new TreeSet<>();

    private ActorRef<SortedSet<BigInteger>> sender;

    private ManagerBehavior(ActorContext<Command> context) {
        super(context);
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(ManagerBehavior::new);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(InstructionCommand.class,c->{
                    if(c.message.equals("create")){
                        sender = c.getSender();
                        IntStream.rangeClosed(1, 20).forEach(i -> {
                            ActorRef<WorkerBehavior.Command> worker =
                                    getContext().spawn(WorkerBehavior.create(), "Worker" + i);
                            askForPrime(worker);
                        });
                    }
                    return Behaviors.same();
                })
                .onMessage(ResultCommand.class,c->{
                    primes.add(c.getPrime());
                    if(primes.size() == 20){
                        sender.tell(primes);
                    }
                    return Behaviors.same();
                })
                .onMessage(FailureCommand.class, c->{
                    askForPrime(c.getRef());
                    return Behaviors.same();
                })
                .build();
    }

    private void askForPrime(ActorRef<WorkerBehavior.Command> ref){
        getContext().ask(Command.class,ref, Duration.ofSeconds(5),
                (me)-> new WorkerBehavior.Command("start",me),
                (resp,throwable)->{
                    if(resp != null){
                        return resp;
                    }
                    System.out.println("Message not received from "+ref.path());
                    return new FailureCommand(ref);
                });
    }
}
