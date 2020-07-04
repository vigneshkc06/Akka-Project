package BlockChain.blockchain;

import BlockChain.model.Block;
import BlockChain.model.HashResult;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import static BlockChain.utils.BlockChainUtils.calculateHash;


public class WorkerBehavior extends AbstractBehavior<WorkerBehavior.Command> {

    public static class Command {
        private Block block;
        private int startNonce;
        private int difficulty;
        private ActorRef<ManagerBehavior.Command> ref;

        public Command(Block block, int startNonce, int difficulty,ActorRef<ManagerBehavior.Command> ref) {
            this.block = block;
            this.startNonce = startNonce;
            this.difficulty = difficulty;
            this.ref = ref;
        }

        public Block getBlock() {
            return block;
        }

        public int getStartNonce() {
            return startNonce;
        }

        public int getDifficulty() {
            return difficulty;
        }

        public ActorRef<ManagerBehavior.Command> getRef() {
            return ref;
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
        return newReceiveBuilder()
                .onAnyMessage(m -> {
                	int difficultyLevel = m.getDifficulty();
                    String hash = new String(new char[difficultyLevel]).replace("\0", "X");
                    String target = new String(new char[difficultyLevel]).replace("\0", "0");

                    int nonce = m.getStartNonce();
                    while (!hash.substring(0, difficultyLevel).equals(target) && nonce < m.getStartNonce()+1000) {
                        nonce++;
                        Block block = m.getBlock();
                        String dataToEncode = block.getPreviousHash() +
								Long.toString(block.getTransaction().getTimestamp()) + Integer.toString(nonce) +
								block.getTransaction();
                        hash = calculateHash(dataToEncode);
                    }
                    if (hash.substring(0, difficultyLevel).equals(target)) {
                        HashResult hashResult = new HashResult();
                        hashResult.foundAHash(hash, nonce);
                        m.getRef().tell(new ManagerBehavior.HashResultCommand(hashResult));
						getContext().getLog().debug(hashResult.getNonce()+" : "+hashResult.getHash());
						return Behaviors.same();
                    } else {
                        //return null;
						getContext().getLog().debug("Null");
						return Behaviors.stopped();
                    }

                })
                .build();
    }

}
