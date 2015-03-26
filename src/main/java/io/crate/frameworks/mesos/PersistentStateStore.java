package io.crate.frameworks.mesos;

import org.apache.mesos.state.Variable;
import org.apache.mesos.state.ZooKeeperState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class PersistentStateStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistentStateStore.class);
    private static final String CRATE_STATE = "crate";

    private final ZooKeeperState zk;
    private final CrateState state;

    private final Future<Variable> zkFuture;
    private Variable stateVariable = null;


    public PersistentStateStore(ZooKeeperState zk, int desiredInstances) {
        this.zk = zk;
        this.zkFuture = zk.fetch(CRATE_STATE);
        this.state = restore();
        int nodeCount = state.desiredInstances().getValue() == CrateState.UNDEFINED_DESIRED_INSTANCES ?
                desiredInstances :
                state.desiredInstances().getValue();
        desiredInstances(nodeCount);
    }

    public CrateState state() {
        return state;
    }

    public synchronized void save() {
        stateVariable = stateVariable.mutate(state.toStream());
        try {
            stateVariable = zk.store(stateVariable).get();
            if (stateVariable == null) {
                LOGGER.error("Couldn't save state in Zookeeper");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private CrateState restore() {
        try {
            stateVariable = zkFuture.get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
        if (stateVariable != null) {
            try {
                CrateState st = CrateState.fromStream(stateVariable.value());
                LOGGER.info("Restored state from Zookeeper: {}", st);
                LOGGER.debug(st.toString());
                return st;
            } catch (IOException e) {
                e.printStackTrace();
                LOGGER.error(e.getMessage());
            }
        }
        return new CrateState();
    }

    public synchronized void desiredInstances(int instances) {
        // set state
        state.desiredInstances(instances);
        save();
    }
}
