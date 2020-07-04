package BlockChain.blockchain;

import BlockChain.model.Block;
import BlockChain.model.HashResult;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.stream.IntStream;

public class ManagerBehavior extends AbstractBehavior<ManagerBehavior.Command> {

    private StashBuffer<Command> stashBuffer;

    private ManagerBehavior(ActorContext<Command> context,StashBuffer<Command> stash) {
        super(context);
        this.stashBuffer = stash;
    }

    public static Behavior<Command> create(){
        return Behaviors.withStash(10,stash->{
            return Behaviors.setup(context ->{
                return new ManagerBehavior(context,stash);
            });
        });
    }

    public static class HashResultCommand implements Command{
        private static final long serialVersionUID = 1L;

        private HashResult result;

        public HashResultCommand(HashResult result) {
            this.result = result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            HashResultCommand that = (HashResultCommand) o;

            return Objects.equals(result, that.result);
        }

        @Override
        public int hashCode() {
            return result != null ? result.hashCode() : 0;
        }

        public HashResult getResult() {
            return result;
        }


    }
    public static class MineBlockCommand implements Command{
        private static final long serialVersionUID = 1L;

        private Block block;
        private ActorRef<HashResult> sender;
        private int difficulty;

        public MineBlockCommand(Block block, ActorRef<HashResult> sender, int difficulty) {
            this.block = block;
            this.sender = sender;
            this.difficulty = difficulty;
        }

        public Block getBlock() {
            return block;
        }

        public ActorRef<HashResult> getSender() {
            return sender;
        }

        public int getDifficulty() {
            return difficulty;
        }
    }

    private ActorRef<HashResult> sender;
    private Block block;
    private int difficulty;
    private int currentNonce = 0;
    private boolean currentlyMining;


    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onSignal(Terminated.class,m->{
                    setNewWorker();
                    return Behaviors.same();
                })
                .onMessage(MineBlockCommand.class,m->{
                    if(currentlyMining)
                        return handleSecondMiningCommand();
                    this.block = m.getBlock();
                    this.difficulty = m.getDifficulty();
                    this.sender = m.getSender();
                    this.currentlyMining = true;
                    IntStream.rangeClosed(1,10).forEach(i->{
                        setNewWorker();
                    });
                    return Behaviors.same();
                })
                .onMessage(HashResultCommand.class,m->{
                    getContext().getChildren().forEach(c->{
                        getContext().stop(c);
                    });
                    currentlyMining=false;
                    sender.tell(m.getResult());
                    return Behaviors.same();
                })
                .build();
    }

    public Receive<Command> handleSecondMiningCommand(){
        return newReceiveBuilder()
                .onMessage(MineBlockCommand.class,m->{
                    return Behaviors.ignore();
                })
                .build();
    }

    public interface Command extends Serializable {

    }

    private void setNewWorker(){
        if(currentlyMining) {
            System.out.println("Creating workers starting nonce " + currentNonce * 1000);
            Behavior<WorkerBehavior.Command> behavior = Behaviors.supervise(WorkerBehavior.create())
                    .onFailure(SupervisorStrategy.restart());
            ActorRef<WorkerBehavior.Command> worker = getContext().
                    spawn(behavior, "worker" + currentNonce);
            getContext().watch(worker);
            worker.tell(new WorkerBehavior.Command(block, currentNonce * 1000, difficulty, getContext().getSelf()));
            currentNonce++;
        }
    }
}
