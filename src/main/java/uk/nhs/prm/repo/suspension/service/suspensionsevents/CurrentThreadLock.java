package uk.nhs.prm.repo.suspension.service.suspensionsevents;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class CurrentThreadLock {

    private final Set<String> lockedKeys = new HashSet<>();

    public void lock(String key) {
        try {
            synchronized (lockedKeys) {
                while (!lockedKeys.add(key)) {
                    log.info("Multiple threads processing the same NHS number. Locking threads.");
                    lockedKeys.wait();
                }
            }
        } catch (InterruptedException e) {
            log.error("Lock operation failed: " + e.getMessage());
        }

    }

    public void unlock(String key) {
        synchronized (lockedKeys) {
            lockedKeys.remove(key);
            lockedKeys.notifyAll();
        }
    }

}
