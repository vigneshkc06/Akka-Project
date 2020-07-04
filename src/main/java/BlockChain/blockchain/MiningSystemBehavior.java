package BlockChain.blockchain;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.*;

public class MiningSystemBehavior extends AbstractBehavior<ManagerBehavior.Command> {

    private PoolRouter<ManagerBehavior.Command> managerPool;

    private ActorRef<ManagerBehavior.Command> managers;

    private MiningSystemBehavior(ActorContext<ManagerBehavior.Command> context) {
        super(context);
        managerPool = Routers.pool(3,
                Behaviors.supervise(ManagerBehavior.create()).onFailure(SupervisorStrategy.restart()));
        managers = getContext().spawn(managerPool,"managerPool");
    }

    public static Behavior<ManagerBehavior.Command> create(){
        return Behaviors.setup(MiningSystemBehavior::new);
    }

    @Override
    public Receive<ManagerBehavior.Command> createReceive() {
        return newReceiveBuilder()
                .onAnyMessage(m->{
                    managers.tell(m);
                    return Behaviors.same();
                })
                .build();
    }
}
