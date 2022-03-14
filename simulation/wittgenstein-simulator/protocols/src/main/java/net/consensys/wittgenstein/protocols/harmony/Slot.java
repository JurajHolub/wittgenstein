package net.consensys.wittgenstein.protocols.harmony;

import java.util.List;
import java.util.Objects;

public class Slot {
    private Integer slot;
    private Integer shard;

    public Slot(Integer key1, Integer key2) {
        this.slot = key1;
        this.shard = key2;
    }


    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Slot) {
            Slot s = (Slot)obj;
            return Objects.equals(slot, s.slot) && Objects.equals(shard, s.shard);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return List.of(slot, shard).hashCode();
    }
}